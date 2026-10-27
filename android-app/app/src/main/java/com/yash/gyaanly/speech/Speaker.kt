package com.yash.gyaanly.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.yash.gyaanly.domain.BookBuilder
import java.util.Locale

/** Voice for explanations; Speaker falls back to US English when an Indian English voice isn't installed. */
const val EXPLAIN_LANG = "en-IN"

/**
 * Thin wrapper over TextToSpeech with per-sentence language, a slow mode for shadowing practice,
 * and [read] for listening to a whole lesson or chapter.
 */
class Speaker(context: Context) {
    private var ready = false
    private val main = Handler(Looper.getMainLooper())
    private var lastId: String? = null
    private var onFinished: (() -> Unit)? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
    }.apply {
        setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                if (utteranceId == lastId) finish()
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId == lastId) finish()
            }
        })
    }

    val isReady: Boolean get() = ready

    fun say(text: String, languageTag: String = "en-US", slow: Boolean = false) {
        if (!ready) return
        finish() // a drill phrase interrupts any lesson being read aloud
        setLanguage(languageTag)
        tts.setSpeechRate(if (slow) 0.7f else 1.0f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    /** Reads every segment in order; [onDone] runs on the main thread when it ends or is stopped. */
    fun read(segments: List<BookBuilder.Speech>, rate: Float = 1.0f, onDone: () -> Unit): Boolean {
        if (!ready || segments.isEmpty()) return false
        stop()
        onFinished = onDone
        tts.setSpeechRate(rate)
        segments.forEachIndexed { i, s ->
            setLanguage(s.lang)
            // Long paragraphs are split; engines reject text over their maximum input length.
            s.text.chunked(MAX_CHUNK).forEachIndexed { j, chunk ->
                val id = "read-$i-$j"
                lastId = id
                tts.speak(chunk, if (i == 0 && j == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, id)
            }
        }
        return true
    }

    fun stop() {
        if (ready) tts.stop()
        finish()
    }

    private fun finish() {
        val callback = onFinished ?: return
        onFinished = null
        lastId = null
        main.post(callback)
    }

    // Prefer the requested voice; fall back to US English when it isn't installed (e.g. en-IN).
    private fun setLanguage(tag: String) {
        val result = tts.setLanguage(Locale.forLanguageTag(tag))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) tts.language = Locale.US
    }

    fun shutdown() {
        onFinished = null
        tts.shutdown()
    }

    private companion object {
        const val MAX_CHUNK = 3000
    }
}
