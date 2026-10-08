package com.yash.dailyedge.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yash.dailyedge.data.Lesson
import com.yash.dailyedge.data.QuizQuestion
import com.yash.dailyedge.domain.SpeechMatch
import com.yash.dailyedge.speech.Speaker
import com.yash.dailyedge.ui.AppViewModel
import com.yash.dailyedge.ui.theme.color
import kotlinx.coroutines.launch

private val Good = Color(0xFF16A34A)
private val Bad = Color(0xFFDC2626)

@Composable
fun LessonScreen(vm: AppViewModel, lessonId: String, onBack: () -> Unit, openCalculator: (String) -> Unit) {
    val lesson = vm.lesson(lessonId) ?: run { BackBar("Not found", onBack); return }
    val progress by vm.progress.collectAsStateWithLifecycle()
    val color = lesson.track.color
    val answers = remember(lessonId) { mutableStateMapOf<Int, Int>() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { BackBar(lesson.track.title, onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Chip(lesson.level, color)
            Text(lesson.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(lesson.summary, style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)

            lesson.keyTerm?.let { term ->
                InfoCard("Word of the day", color) {
                    Column {
                        Text(term.word, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(term.meaning, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            lesson.sections.forEach { section ->
                Column {
                    Text(section.heading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                    Text(section.body, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
                }
            }

            lesson.example?.let { InfoCard("Example", color) { Text(it, style = MaterialTheme.typography.bodyMedium) } }

            lesson.calculation?.let { calc ->
                var showSteps by remember(lessonId) { mutableStateOf(false) }
                InfoCard("Try the calculation", color) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(calc.problem, style = MaterialTheme.typography.bodyMedium)
                        if (showSteps) {
                            calc.steps.forEachIndexed { i, step -> Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium) }
                            Text("Answer: ${calc.answer}", fontWeight = FontWeight.Bold, color = color)
                        } else {
                            TextButton(onClick = { showSteps = true }) { Text("Solve it yourself, then reveal the steps") }
                        }
                    }
                }
            }

            lesson.calculatorId?.let { id ->
                OutlinedButton(onClick = { openCalculator(id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open the calculator for this topic")
                }
            }

            if (lesson.speak.isNotEmpty()) {
                SpeakingPractice(lesson, onError = { scope.launch { snackbar.showSnackbar(it) } })
            }

            lesson.action?.let { InfoCard("Today's action", color) { Text(it, style = MaterialTheme.typography.bodyMedium) } }

            if (lesson.quiz.isNotEmpty()) {
                Text("Quick check", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                lesson.quiz.forEachIndexed { qi, q ->
                    QuizCard(q, selected = answers[qi], onSelect = { if (qi !in answers) answers[qi] = it })
                }
            }

            val correct = lesson.quiz.indices.count { answers[it] == lesson.quiz[it].answerIndex }
            val allAnswered = answers.size == lesson.quiz.size
            val alreadyDone = lesson.id in progress.completed
            Button(
                onClick = {
                    vm.complete(lesson.id, correct, lesson.quiz.size)
                    onBack()
                },
                enabled = allAnswered,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = color),
            ) {
                Text(
                    when {
                        !allAnswered -> "Answer the quiz to finish"
                        lesson.quiz.isEmpty() -> if (alreadyDone) "Done again" else "Mark as done"
                        else -> "Finish · $correct/${lesson.quiz.size} correct"
                    },
                )
            }
        }
    }
}

@Composable
private fun SpeakingPractice(lesson: Lesson, onError: (String) -> Unit) {
    val context = LocalContext.current
    val color = lesson.track.color
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

    InfoCard("Speak it: listen, then repeat", color) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            lesson.speak.forEachIndexed { i, sentence ->
                Column {
                    Text(sentence, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { speaker.say(sentence) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen")
                        }
                        IconButton(onClick = { speaker.say(sentence, slow = true) }) {
                            Icon(Icons.Filled.SlowMotionVideo, contentDescription = "Listen slowly")
                        }
                        IconButton(onClick = {
                            target = i
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                                .putExtra(RecognizerIntent.EXTRA_PROMPT, sentence)
                            try {
                                recognizer.launch(intent)
                            } catch (e: ActivityNotFoundException) {
                                onError("No speech recognizer found. Install or enable Google speech services.")
                            }
                        }) {
                            Icon(Icons.Filled.Mic, contentDescription = "Speak", tint = color)
                        }
                        results[i]?.let { (_, r) ->
                            Text(
                                "${r.score}% match",
                                color = if (r.score >= 80) Good else if (r.score >= 50) Color(0xFFD97706) else Bad,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    results[i]?.let { (spoken, r) ->
                        Text("You said: \"$spoken\"", style = MaterialTheme.typography.bodySmall)
                        if (r.missed.isNotEmpty()) {
                            Text("Work on: ${r.missed.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = Bad)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizCard(q: QuizQuestion, selected: Int?, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(q.question, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        q.options.forEachIndexed { i, option ->
            val border = when {
                selected == null -> null
                i == q.answerIndex -> BorderStroke(2.dp, Good)
                i == selected -> BorderStroke(2.dp, Bad)
                else -> null
            }
            OutlinedButton(
                onClick = { onSelect(i) },
                modifier = Modifier.fillMaxWidth(),
                border = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Text(option, modifier = Modifier.fillMaxWidth())
            }
        }
        if (selected != null) {
            Text(
                (if (selected == q.answerIndex) "Correct! " else "Not quite. ") + q.explanation,
                color = if (selected == q.answerIndex) Good else Bad,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
