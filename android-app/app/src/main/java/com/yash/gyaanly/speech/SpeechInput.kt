package com.yash.gyaanly.speech

/** A finished spoken answer: the text, seconds spent speaking, and recogniser confidence if known. */
data class Heard(val text: String, val seconds: Double?, val confidence: Float?)

/** Hands-free listening for one answer at a time: the phone's recogniser or Sarvam speech-to-text. */
interface SpeechInput {
    val available: Boolean

    /** Listens for one answer. All callbacks run on the main thread. */
    fun listen(language: String, onPartial: (String) -> Unit, onDone: (Heard) -> Unit, onError: (String) -> Unit)

    /** Ends the current answer now, keeping what was heard. */
    fun finishNow()

    fun cancel()

    fun destroy()
}
