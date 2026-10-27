package com.yash.gyaanly.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Hands-free listening for conversations: no dialog, partial text while you speak, and pauses
 * inside an answer are allowed. After each pause it keeps listening briefly; if you carry on, the
 * new words are appended, and if you stay quiet the answer is complete.
 */
class Listener(context: Context) : SpeechInput {

    private val main = Handler(Looper.getMainLooper())
    private val recognizer: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null

    override val available: Boolean get() = recognizer != null

    private var session = 0
    private val parts = mutableListOf<String>()
    private val confidences = mutableListOf<Float>()
    private var spokenMs = 0L
    private var speechStart = 0L
    private var onPartial: (String) -> Unit = {}
    private var onDone: (Heard) -> Unit = {}
    private var onError: (String) -> Unit = {}
    private var lang = "en-IN"
    private var heardSpeech = false
    private var answerStart = 0L

    /** Starts listening for one answer. Callbacks run on the main thread. */
    override fun listen(language: String, onPartial: (String) -> Unit, onDone: (Heard) -> Unit, onError: (String) -> Unit) {
        val r = recognizer ?: return onError("Speech recognition isn't available on this phone. Install or update the Google app.")
        session++
        parts.clear(); confidences.clear(); spokenMs = 0
        lang = language
        answerStart = SystemClock.elapsedRealtime()
        this.onPartial = onPartial; this.onDone = onDone; this.onError = onError
        start(r, continuation = false)
    }

    /** Ends the current answer now, keeping what was heard so far. */
    override fun finishNow() {
        recognizer?.stopListening()
    }

    override fun cancel() {
        session++
        main.removeCallbacksAndMessages(null)
        recognizer?.cancel()
    }

    override fun destroy() {
        cancel()
        recognizer?.destroy()
    }

    private fun start(r: SpeechRecognizer, continuation: Boolean) {
        val mySession = session
        main.removeCallbacksAndMessages(null) // drop timers from the previous listening round
        heardSpeech = false
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() {
                heardSpeech = true
                speechStart = SystemClock.elapsedRealtime()
            }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                if (speechStart > 0) spokenMs += SystemClock.elapsedRealtime() - speechStart
                speechStart = 0
            }
            override fun onPartialResults(partialResults: Bundle?) {
                if (mySession != session) return
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                onPartial((parts + partial).joinToString(" ").trim())
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onResults(results: Bundle?) {
                if (mySession != session) return
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)?.firstOrNull()?.let { if (it >= 0f) confidences += it }
                if (text.isNotBlank()) parts += text
                onPartial(parts.joinToString(" "))
                // Keep listening in case this was only a pause in the middle of the answer.
                main.post { if (mySession == session) start(r, continuation = true) }
            }

            override fun onError(error: Int) {
                if (mySession != session) return
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        // Still thinking about the first word: keep waiting up to FIRST_WORD_WAIT_MS.
                        if (!continuation && parts.isEmpty() && SystemClock.elapsedRealtime() - answerStart < FIRST_WORD_WAIT_MS) {
                            main.post { if (mySession == session) start(r, continuation = false) }
                        } else {
                            complete()
                        }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> main.postDelayed({ if (mySession == session) { r.cancel(); start(r, continuation) } }, 300)
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> onError("Allow microphone access to practise speaking.")
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER ->
                        if (parts.isNotEmpty()) complete() else onError("Speech recognition needs internet, or download the offline English pack: Settings → Google → Voice → Offline speech recognition.")
                    else -> if (parts.isNotEmpty()) complete() else onError("The microphone stopped unexpectedly (code $error). Tap the mic to try again.")
                }
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        r.startListening(intent)
        if (continuation) {
            // A continuation only waits briefly: if you don't start speaking again, the answer is done.
            main.postDelayed({ if (mySession == session && !heardSpeech) { r.cancel(); complete() } }, CONTINUE_WAIT_MS)
        } else {
            // Give the learner time to think before the first word, then treat silence as no answer.
            val left = FIRST_WORD_WAIT_MS - (SystemClock.elapsedRealtime() - answerStart)
            main.postDelayed({ if (mySession == session && !heardSpeech && parts.isEmpty()) { r.cancel(); complete() } }, left.coerceAtLeast(500))
        }
    }

    private fun complete() {
        session++ // ignore late callbacks from this answer
        main.removeCallbacksAndMessages(null)
        val heard = Heard(
            text = parts.joinToString(" ").trim(),
            seconds = spokenMs.takeIf { it > 0 }?.div(1000.0),
            confidence = confidences.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
        )
        main.post { onDone(heard) }
    }

    private companion object {
        const val CONTINUE_WAIT_MS = 1800L
        const val FIRST_WORD_WAIT_MS = 12_000L
    }
}
