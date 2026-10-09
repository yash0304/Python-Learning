package com.yash.gyaanly.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Thin wrapper over TextToSpeech with per-sentence language and a slow mode for shadowing practice. */
class Speaker(context: Context) {
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
    }

    fun say(text: String, languageTag: String = "en-US", slow: Boolean = false) {
        if (!ready) return
        tts.language = Locale.forLanguageTag(languageTag)
        tts.setSpeechRate(if (slow) 0.7f else 1.0f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    fun shutdown() = tts.shutdown()
}
