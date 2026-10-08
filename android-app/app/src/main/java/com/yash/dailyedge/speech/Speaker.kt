package com.yash.dailyedge.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Thin wrapper over TextToSpeech with an adjustable speaking rate for shadowing practice. */
class Speaker(context: Context) {
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) tts.language = Locale.US
    }

    fun say(text: String, slow: Boolean = false) {
        if (!ready) return
        tts.setSpeechRate(if (slow) 0.7f else 1.0f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    fun shutdown() = tts.shutdown()
}
