package com.yash.gyaanly.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yash.gyaanly.data.Lesson
import com.yash.gyaanly.data.QuizQuestion
import com.yash.gyaanly.domain.SpeechMatch
import com.yash.gyaanly.speech.Speaker
import com.yash.gyaanly.ui.AppViewModel
import com.yash.gyaanly.ui.theme.Pop
import com.yash.gyaanly.ui.theme.accent
import com.yash.gyaanly.ui.theme.gradient
import kotlinx.coroutines.launch

private data class Celebration(val xp: Int, val correct: Int, val total: Int)

@Composable
fun LessonScreen(vm: AppViewModel, lessonId: String, onBack: () -> Unit, openCalculator: (String) -> Unit) {
    val lesson = vm.lesson(lessonId) ?: run { BackBar("Not found", onBack); return }
    val progress by vm.progress.collectAsStateWithLifecycle()
    val color = lesson.track.accent
    val answers = remember(lessonId) { mutableStateMapOf<Int, Int>() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var celebration by remember { mutableStateOf<Celebration?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { BackBar("${lesson.track.emoji}  ${lesson.track.title}", onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GradientCard(lesson.track.gradient) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(lesson.level.uppercase(), Color.White)
                    Text(lesson.title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Text(lesson.summary, color = Color.White.copy(alpha = 0.92f), fontStyle = FontStyle.Italic)
                }
            }

            lesson.keyTerm?.let { term ->
                InfoCard("Word of the day", "🔑", color) {
                    Column {
                        Text(term.word, style = MaterialTheme.typography.titleLarge)
                        Text(term.meaning, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            lesson.sections.forEachIndexed { i, section ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(28.dp).background(color, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text("${i + 1}", color = Color.White, fontWeight = FontWeight.Black) }
                        Text("  ${section.heading}", style = MaterialTheme.typography.titleMedium, color = color)
                    }
                    Text(section.body, style = MaterialTheme.typography.bodyLarge)
                }
            }

            lesson.example?.let { InfoCard("Example", "💡", Pop.Sky) { Text(it, style = MaterialTheme.typography.bodyLarge) } }

            lesson.calculation?.let { calc ->
                var showSteps by remember(lessonId) { mutableStateOf(false) }
                InfoCard("Your turn: calculate", "🧮", Pop.Coral) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(calc.problem, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        if (showSteps) {
                            calc.steps.forEachIndexed { i, step -> Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium) }
                            Text("✅  Answer: ${calc.answer}", fontWeight = FontWeight.Black, color = Pop.Good)
                        } else {
                            Text("Grab a pen and try it first ✍️", style = MaterialTheme.typography.bodySmall)
                            PopButton("Reveal the steps", Pop.Coral, onClick = { showSteps = true })
                        }
                    }
                }
            }

            lesson.calculatorId?.let { id ->
                PopButton("🧮  Play with the calculator", Pop.Sun, onClick = { openCalculator(id) }, contentColor = Color(0xFF3B2600))
            }

            if (lesson.speak.isNotEmpty()) {
                SpeakingPractice(lesson, onError = { scope.launch { snackbar.showSnackbar(it) } })
            }

            lesson.action?.let { InfoCard("Today's mission", "🚀", Pop.Mint) { Text(it, style = MaterialTheme.typography.bodyLarge) } }

            if (lesson.quiz.isNotEmpty()) {
                SectionTitle("Quick quiz", "🧠")
                lesson.quiz.forEachIndexed { qi, q ->
                    QuizBlock(qi + 1, q, selected = answers[qi], onSelect = { if (qi !in answers) answers[qi] = it })
                }
            }

            val correct = lesson.quiz.indices.count { answers[it] == lesson.quiz[it].answerIndex }
            val allAnswered = answers.size == lesson.quiz.size
            val alreadyDone = lesson.id in progress.completed
            PopButton(
                text = when {
                    !allAnswered -> "Answer ${lesson.quiz.size - answers.size} more to finish"
                    alreadyDone -> "Finish again ($correct/${lesson.quiz.size})"
                    else -> "Finish lesson 🎉"
                },
                color = if (allAnswered) Pop.Good else color,
                enabled = allAnswered,
                onClick = {
                    val xp = vm.complete(lesson.id, correct, lesson.quiz.size)
                    celebration = Celebration(xp, correct, lesson.quiz.size)
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    celebration?.let { c ->
        CelebrationDialog(c, lesson) { celebration = null; onBack() }
    }
}

@Composable
private fun CelebrationDialog(c: Celebration, lesson: Lesson, onContinue: () -> Unit) {
    val perfect = c.total > 0 && c.correct == c.total
    val (emoji, headline) = when {
        perfect -> "🏆" to "Perfect score!"
        c.total == 0 || c.correct * 2 >= c.total -> "🎉" to "Lesson complete!"
        else -> "💪" to "Done! Review and try again"
    }
    Dialog(onDismissRequest = onContinue) {
        GradientCard(lesson.track.gradient) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(emoji, style = MaterialTheme.typography.displayLarge)
                Text(headline, color = Color.White, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                if (c.total > 0) Text("${c.correct} / ${c.total} correct", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (c.xp > 0) "+${c.xp} XP ⚡" else "Already earned: beat your best score for more XP",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                PopButton("Continue", Color.White, onClick = onContinue, contentColor = lesson.track.accent)
            }
        }
    }
}

@Composable
private fun SpeakingPractice(lesson: Lesson, onError: (String) -> Unit) {
    val context = LocalContext.current
    val speaker = remember { Speaker(context) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }

    var target by remember { mutableIntStateOf(-1) }
    val results = remember(lesson.id) { mutableStateMapOf<Int, Pair<String, SpeechMatch.Result>>() }

    val recognizer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (res.resultCode == Activity.RESULT_OK && spoken != null && target >= 0) {
            results[target] = spoken to SpeechMatch.compare(lesson.speak[target], spoken)
        }
    }

    InfoCard("Say it out loud", "🎤", Pop.Pink) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Listen 🔊, try slow 🐢, then tap the mic and repeat.", style = MaterialTheme.typography.bodySmall)
            lesson.speak.forEachIndexed { i, sentence ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(sentence, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoundAction(Pop.Sky, { speaker.say(sentence, lesson.speakLang) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen")
                        }
                        RoundAction(Pop.Mint, { speaker.say(sentence, lesson.speakLang, slow = true) }) {
                            Icon(Icons.Filled.SlowMotionVideo, contentDescription = "Listen slowly")
                        }
                        RoundAction(Pop.Pink, {
                            target = i
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, lesson.speakLang)
                                .putExtra(RecognizerIntent.EXTRA_PROMPT, sentence)
                            try {
                                recognizer.launch(intent)
                            } catch (e: ActivityNotFoundException) {
                                onError("No speech recognizer found. Install or enable Google speech services.")
                            }
                        }) {
                            Icon(Icons.Filled.Mic, contentDescription = "Speak")
                        }
                        results[i]?.let { (_, r) ->
                            val (badge, tint) = when {
                                r.score >= 85 -> "🌟 ${r.score}%" to Pop.Good
                                r.score >= 60 -> "👍 ${r.score}%" to Pop.Flame
                                else -> "🔁 ${r.score}%" to Pop.Bad
                            }
                            Chip(badge, tint, filled = true)
                        }
                    }
                    results[i]?.let { (spoken, r) ->
                        Text("You said: \"$spoken\"", style = MaterialTheme.typography.bodySmall)
                        if (r.missed.isNotEmpty()) {
                            Text("Practise: ${r.missed.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = Pop.Bad)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundAction(color: Color, onClick: () -> Unit, icon: @Composable () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = color, contentColor = Color.White),
    ) { icon() }
}

@Composable
private fun QuizBlock(number: Int, q: QuizQuestion, selected: Int?, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Q$number. ${q.question}", style = MaterialTheme.typography.titleMedium)
        q.options.forEachIndexed { i, option ->
            val state = when {
                selected == null -> OptionState.Idle
                i == q.answerIndex -> OptionState.Correct
                i == selected -> OptionState.Wrong
                else -> OptionState.Dimmed
            }
            OptionTile(option, state, onClick = { onSelect(i) })
        }
        if (selected != null) {
            val right = selected == q.answerIndex
            Text(
                (if (right) "🎯 Correct! " else "🤔 Not quite. ") + q.explanation,
                color = if (right) Pop.Good else Pop.Bad,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
