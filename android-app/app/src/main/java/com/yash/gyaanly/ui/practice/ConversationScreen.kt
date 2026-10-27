package com.yash.gyaanly.ui.practice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yash.gyaanly.domain.practice.AiCoach
import com.yash.gyaanly.domain.practice.AiFeedback
import com.yash.gyaanly.domain.practice.Answer
import com.yash.gyaanly.domain.practice.ChatMessage
import com.yash.gyaanly.domain.practice.Finding
import com.yash.gyaanly.domain.practice.Report
import com.yash.gyaanly.domain.practice.Scenario
import com.yash.gyaanly.domain.practice.ScenarioSource
import com.yash.gyaanly.domain.practice.ScriptTurn
import com.yash.gyaanly.domain.practice.SpeechAnalyzer
import com.yash.gyaanly.speech.Heard
import com.yash.gyaanly.speech.BulbulVoice
import com.yash.gyaanly.speech.Listener
import com.yash.gyaanly.speech.PartnerVoice
import com.yash.gyaanly.speech.PhoneVoice
import com.yash.gyaanly.speech.SarvamListener
import com.yash.gyaanly.speech.SpeechInput
import com.yash.gyaanly.speech.Speaker
import com.yash.gyaanly.ui.screens.BackBar
import com.yash.gyaanly.ui.screens.GradientCard
import com.yash.gyaanly.ui.screens.InfoCard
import com.yash.gyaanly.ui.screens.PopButton
import com.yash.gyaanly.ui.screens.SectionTitle
import com.yash.gyaanly.ui.theme.Pop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PARTNER_LANG = "en-US"
private const val LEARNER_LANG = "en-IN"
private const val MAX_AI_TURNS = 8
private const val SORRY = "Sorry, I didn't catch that. Could you say that again?"

enum class Phase { INTRO, PARTNER, LISTENING, THINKING, PAUSED, REPORT }

data class Bubble(val partner: Boolean, val text: String)

/**
 * Drives one spoken conversation: the partner speaks, then the app listens automatically, then the
 * partner continues: either the next scripted line or, with AI, a reply to what you actually said.
 */
class Conversation(
    private val scenario: Scenario,
    private val voice: PartnerVoice,
    private var listener: SpeechInput,
    private val coach: AiCoach?,
    private val scope: CoroutineScope,
) {
    var phase by mutableStateOf(Phase.INTRO)
    var smart by mutableStateOf(coach != null && scenario.source == ScenarioSource.AI)
    var live by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
    var report by mutableStateOf<Report?>(null)
    val bubbles = mutableStateListOf<Bubble>()
    val answers = mutableListOf<Answer>()

    private var index = 0
    private var prompt = ""
    private var promptTurn: ScriptTurn? = null
    private var retried = false
    private var token = 0
    private var resume: () -> Unit = {}
    private val history = mutableListOf<ChatMessage>()

    fun start() {
        bubbles.clear(); answers.clear(); history.clear(); index = 0; report = null; error = null
        if (!smart) scenario.turns.take(2).forEach { voice.prefetch(it.say) }
        voice.prefetch(SORRY)
        if (smart) aiNext() else scripted(0)
    }

    private fun scripted(i: Int) {
        val t = scenario.turns.getOrNull(i) ?: return finish()
        if (t.say.isBlank()) { prompt = ""; promptTurn = t; listen(); return }
        partnerSays(t.say, t) { listen() }
    }

    private fun partnerSays(text: String, turn: ScriptTurn?, addBubble: Boolean = true, then: () -> Unit) {
        val my = ++token
        if (addBubble) bubbles += Bubble(true, text)
        prompt = text; promptTurn = turn
        phase = Phase.PARTNER
        resume = { partnerSays(text, turn, addBubble = false, then) }
        val started = voice.say(text) { if (my == token) then() }
        if (!started) { error = "The voice isn't ready yet. Tap Resume in a moment."; phase = Phase.PAUSED }
    }

    private fun listen() {
        val my = ++token
        // While you answer, get the partner's next scripted line ready so it plays without a wait.
        if (!smart) voice.prefetch(scenario.turns.getOrNull(index + 1)?.say ?: scenario.closing)
        phase = Phase.LISTENING
        live = ""
        resume = { listen() }
        listener.listen(LEARNER_LANG, onPartial = { if (my == token) live = it }, onDone = { if (my == token) heard(it) }, onError = {
            if (my == token) { error = it; phase = Phase.PAUSED }
        })
    }

    /** Swaps the recogniser mid-conversation (e.g. Sarvam failed, so use the phone) and listens again. */
    fun useListener(input: SpeechInput) {
        token++
        listener.cancel()
        listener = input
        error = null
        listen()
    }

    private fun heard(h: Heard) {
        if (h.text.isBlank() && !retried) {
            retried = true
            val original = prompt; val turn = promptTurn
            partnerSays(SORRY, turn, addBubble = false) { prompt = original; listen() }
            return
        }
        retried = false
        answers += Answer(prompt, h.text, h.seconds, h.confidence, promptTurn)
        bubbles += Bubble(false, h.text.ifBlank { "(no answer)" })
        live = ""
        if (smart) {
            history += ChatMessage(false, prompt)
            history += ChatMessage(true, h.text.ifBlank { "(silence)" })
            aiNext()
        } else {
            index++
            scripted(index)
        }
    }

    private fun aiNext() {
        val c = coach ?: return finish()
        val my = ++token
        phase = Phase.THINKING
        resume = { aiNext() }
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { c.partner(scenario, history, MAX_AI_TURNS - 1) } }
            if (my != token) return@launch
            result.onSuccess { line ->
                val last = line.ended || answers.size >= MAX_AI_TURNS
                partnerSays(line.text, null) { if (last) analyse() else listen() }
            }.onFailure { error = it.message ?: "The AI partner didn't respond."; phase = Phase.PAUSED }
        }
    }

    private fun finish() = partnerSays(scenario.closing, null) { analyse() }

    fun pause() {
        token++
        voice.stop(); listener.cancel()
        phase = Phase.PAUSED
    }

    fun resumeNow() { error = null; resume() }

    /** Replays the partner's last line, then listens again. */
    fun repeat() {
        token++
        listener.cancel()
        partnerSays(prompt.ifBlank { scenario.closing }, promptTurn, addBubble = false) { listen() }
    }

    fun doneSpeaking() = listener.finishNow()

    fun endNow() {
        token++
        voice.stop(); listener.cancel()
        analyse()
    }

    private fun analyse() {
        token++
        report = SpeechAnalyzer.analyze(scenario, answers)
        phase = Phase.REPORT
    }
}

@Composable
fun ConversationScreen(vm: PracticeViewModel, scenarioId: String, onBack: () -> Unit) {
    val scenario = remember(scenarioId) { vm.scenario(scenarioId) } ?: run { BackBar("Scenario not found", onBack); return }
    val context = LocalContext.current
    val ai by vm.ai.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val speaker = remember { Speaker(context) }
    // Chosen once per conversation: Sarvam speech-to-text when switched on in Settings, else the phone.
    val sarvam = remember { vm.ai.value.speech() }
    val listener: SpeechInput = remember { sarvam?.let(::SarvamListener) ?: Listener(context) }
    var phoneFallback by remember { mutableStateOf<Listener?>(null) }
    val coach = remember(ai) { vm.coach() }
    var voiceNotice by remember { mutableStateOf<String?>(null) }
    val voice: PartnerVoice = remember {
        val phone = PhoneVoice(speaker, PARTNER_LANG)
        vm.ai.value.voice()?.let { BulbulVoice(context, it, phone) { why -> voiceNotice = "Using the phone voice for that line: $why" } } ?: phone
    }
    val convo = remember(scenarioId, coach) { Conversation(scenario, voice, listener, coach, scope) }
    var showText by remember { mutableStateOf(false) }
    var micDenied by remember { mutableStateOf(false) }
    val view = LocalView.current

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false; voice.shutdown(); speaker.shutdown(); listener.destroy(); phoneFallback?.destroy() }
    }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) convo.start() else micDenied = true
    }
    fun begin() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) convo.start()
        else askMic.launch(Manifest.permission.RECORD_AUDIO)
    }
    val color = categoryColor(scenario.category)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { BackBar("${scenario.emoji}  ${scenario.title}", onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (convo.phase) {
                Phase.INTRO -> Intro(scenario, convo, coach != null, micDenied, color, ::begin)
                Phase.REPORT -> ReportView(vm, scenario, convo, coach, color, onAgain = { convo.start() }, onDone = onBack)
                else -> Talking(convo, showText, { showText = it }, color, voiceNotice, canFallBack = sarvam != null && phoneFallback == null) {
                    phoneFallback = Listener(context).also(convo::useListener)
                }
            }
        }
    }
}

@Composable
private fun Intro(s: Scenario, convo: Conversation, aiAvailable: Boolean, micDenied: Boolean, color: Color, begin: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        // The details scroll; the Start button stays pinned at the bottom so it's always reachable.
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GradientCard(androidx.compose.ui.graphics.Brush.linearGradient(listOf(color, Pop.Grape))) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(s.title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Text("🎭 The app plays: ${s.partner}", color = Color.White)
                    Text("🙋 You are: ${s.you}", color = Color.White)
                    Text("🎯 ${s.goal}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            if (s.focus.isNotEmpty()) InfoCard("Keep in mind", "💡", color) {
                Column { s.focus.forEach { Text("• $it") } }
            }
            InfoCard("How it works", "🎧", Pop.Sky) {
                Text(
                    "Hands-free: listen, then simply start talking when they finish. Short pauses are fine; stay quiet for about two seconds when you're done. " +
                        "Speak in your own words: there's nothing to read. Use headphones in a noisy place.",
                )
            }
            if (aiAvailable && s.source != ScenarioSource.MY_SCRIPT) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("🤖 Smart partner", fontWeight = FontWeight.Bold)
                        Text("Replies to what you actually say instead of following the script.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = convo.smart, onCheckedChange = { convo.smart = it })
                }
            }
            if (micDenied) Text("Microphone access is needed for speaking practice. Allow it in the app's Android settings.", color = Pop.Bad)
        }
        PopButton("🎙️  Start the conversation", color, onClick = begin, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun Talking(
    convo: Conversation,
    showText: Boolean,
    setShowText: (Boolean) -> Unit,
    color: Color,
    voiceNotice: String?,
    canFallBack: Boolean,
    fallBack: () -> Unit,
) {
    val list = rememberLazyListState()
    LaunchedEffect(convo.bubbles.size, convo.live) {
        val count = convo.bubbles.size + (if (convo.live.isNotBlank()) 1 else 0)
        if (count > 0) list.animateScrollToItem(count - 1)
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (convo.phase) {
                    Phase.PARTNER -> "🔊 Listen…"
                    Phase.LISTENING -> "🎙️ Your turn: speak now"
                    Phase.THINKING -> "💭 They're thinking…"
                    else -> "⏸ Paused"
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (convo.phase == Phase.LISTENING) Pop.Bad else color,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
            )
            Text("Show text", style = MaterialTheme.typography.labelMedium)
            Switch(checked = showText, onCheckedChange = setShowText)
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = list,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(convo.bubbles) { b -> BubbleView(b, showText, color) }
            if (convo.live.isNotBlank()) item { BubbleView(Bubble(false, convo.live + " …"), true, color) }
        }
        voiceNotice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp)) }
        convo.error?.let {
            Text(it, color = Pop.Bad, modifier = Modifier.padding(horizontal = 16.dp))
            if (canFallBack) TextButton(onClick = fallBack, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text("📱 Use the phone's recogniser instead", fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (convo.phase) {
                Phase.LISTENING -> PopButton("✋  I'm done speaking", Pop.Bad, onClick = convo::doneSpeaking)
                Phase.PAUSED -> PopButton("▶  Resume", color, onClick = convo::resumeNow)
                Phase.THINKING -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.padding(end = 12.dp)); Text("Waiting for the AI partner…")
                }
                else -> Unit
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PopButton("🔁 Repeat", Pop.Sky, onClick = convo::repeat, modifier = Modifier.weight(1f))
                PopButton("⏸ Pause", Pop.Sun, onClick = convo::pause, modifier = Modifier.weight(1f), contentColor = Color(0xFF3B2600))
                PopButton("🏁 End", Pop.Grape, onClick = convo::endNow, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BubbleView(b: Bubble, showText: Boolean, color: Color) {
    var reveal by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (b.partner) Arrangement.Start else Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (b.partner) MaterialTheme.colorScheme.surface else color.copy(alpha = 0.85f),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 300.dp).clickable(enabled = b.partner) { reveal = !reveal },
        ) {
            Text(
                if (b.partner && !showText && !reveal) "🔊 ··· (tap to see the words)" else b.text,
                modifier = Modifier.padding(12.dp),
                color = if (b.partner) MaterialTheme.colorScheme.onSurface else Color.White,
                fontStyle = if (b.partner && !showText && !reveal) FontStyle.Italic else FontStyle.Normal,
            )
        }
    }
}

@Composable
private fun ReportView(
    vm: PracticeViewModel,
    s: Scenario,
    convo: Conversation,
    coach: AiCoach?,
    color: Color,
    onAgain: () -> Unit,
    onDone: () -> Unit,
) {
    val report = convo.report ?: return
    var feedback by remember(report) { mutableStateOf<AiFeedback?>(null) }
    var aiError by remember(report) { mutableStateOf<String?>(null) }
    val answers = remember(report) { convo.answers.toList() }
    LaunchedEffect(report) {
        if (answers.any { it.text.isNotBlank() }) vm.saveResult(s, report.overall)
        if (coach != null && answers.any { it.text.isNotBlank() }) {
            withContext(Dispatchers.IO) { runCatching { coach.feedback(s, answers, report) } }
                .onSuccess { feedback = it }.onFailure { aiError = it.message }
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GradientCard(androidx.compose.ui.graphics.Brush.linearGradient(listOf(color, Pop.Grape))) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Your speaking report", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
                    Text("${report.overall} / 100", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                    Text(
                        listOfNotNull(
                            "Fluency ${report.fluency}",
                            report.content?.let { "Content $it" },
                            "Grammar ${report.grammar}",
                            "Delivery ${report.delivery}",
                        ).joinToString("  ·  "),
                        color = Color.White,
                    )
                    Text(
                        "${report.totalWords} words · ${report.wordsPerMinute?.let { "$it words/min" } ?: "pace unknown"} · ${report.fillerCount} fillers",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (answers.none { it.text.isNotBlank() }) item {
            InfoCard("Nothing to analyse yet", "🤫", Pop.Sun) { Text("I didn't hear any answers. Check the microphone and try again.") }
        }
        if (report.strengths.isNotEmpty()) item {
            InfoCard("What went well", "🌟", Pop.Good) { Column { report.strengths.forEach { Text("✓ $it") } } }
        }
        if (report.shortcomings.isNotEmpty()) {
            item { SectionTitle("What to improve", "🛠️") }
            items(report.shortcomings) { FindingCard(it) }
        }
        if (coach != null && answers.any { it.text.isNotBlank() }) item {
            InfoCard("AI coach", "🤖", Pop.Grape) {
                val fb = feedback
                when {
                    fb != null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(fb.summary)
                        fb.strengths.forEach { Text("✓ $it") }
                        fb.improvements.forEach { FindingCard(it) }
                        if (fb.phrases.isNotEmpty()) {
                            Text("Phrases to use next time", fontWeight = FontWeight.Bold)
                            fb.phrases.forEach { Text("• $it") }
                        }
                        fb.nextPractice?.let { Text("Next: $it", fontWeight = FontWeight.Bold) }
                    }
                    aiError != null -> Text(aiError!!, color = Pop.Bad)
                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.padding(end = 12.dp)); Text("Your coach is reviewing the conversation…")
                    }
                }
            }
        }
        if (report.turns.isNotEmpty()) item { SectionTitle("Turn by turn", "🗣️") }
        items(report.turns) { t ->
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                Column(Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (t.prompt.isNotBlank()) Text("They: ${t.prompt}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("You: ${t.said.ifBlank { "(no answer)" }}", fontWeight = FontWeight.SemiBold)
                    t.covered.forEach { Text("✓ $it", color = Pop.Good, style = MaterialTheme.typography.bodySmall) }
                    t.missed.forEach { Text("✗ $it", color = Pop.Bad, style = MaterialTheme.typography.bodySmall) }
                    if (t.fillers.isNotEmpty()) Text("Fillers: ${t.fillers.joinToString()}", style = MaterialTheme.typography.bodySmall, color = Pop.Coral)
                    t.wordsPerMinute?.let { Text("Pace: $it words/min", style = MaterialTheme.typography.bodySmall) }
                    t.sample?.let {
                        Box(Modifier.fillMaxWidth().background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)).padding(10.dp)) {
                            Text("💬 A strong answer: $it", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PopButton("🔁 Practise again", color, onClick = onAgain, modifier = Modifier.weight(1f))
                PopButton("✓ Done", Pop.Grape, onClick = onDone, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FindingCard(f: Finding) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Column(Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(f.title, fontWeight = FontWeight.Bold)
            if (f.detail.isNotBlank()) Text(f.detail, style = MaterialTheme.typography.bodySmall)
            f.example?.let { Text("You said: \"$it\"", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = Pop.Coral) }
            f.better?.let { Text("Better: $it", style = MaterialTheme.typography.bodySmall, color = Pop.Good, fontWeight = FontWeight.SemiBold) }
        }
    }
}
