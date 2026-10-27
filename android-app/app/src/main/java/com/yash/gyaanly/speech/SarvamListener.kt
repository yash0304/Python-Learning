package com.yash.gyaanly.speech

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import com.yash.gyaanly.data.ai.SarvamSpeech
import com.yash.gyaanly.domain.practice.SpeechDetector
import kotlin.concurrent.thread

/**
 * Records your answer on the phone, detects when you've finished (pauses are allowed), then sends
 * the audio to Sarvam speech-to-text, which is tuned for Indian accents. Needs internet and a key.
 */
class SarvamListener(private val stt: SarvamSpeech) : SpeechInput {

    private val main = Handler(Looper.getMainLooper())
    @Volatile private var session = 0
    @Volatile private var detector: SpeechDetector? = null

    override val available: Boolean get() = true

    @SuppressLint("MissingPermission") // the conversation screen asks for the microphone first
    override fun listen(language: String, onPartial: (String) -> Unit, onDone: (Heard) -> Unit, onError: (String) -> Unit) {
        val my = ++session
        val det = SpeechDetector(sampleRate = RATE)
        detector = det
        thread(name = "sarvam-listen") {
            val minBuf = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val rec = try {
                AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, FRAME * 8))
            } catch (e: Exception) {
                null
            }
            if (rec == null || rec.state != AudioRecord.STATE_INITIALIZED) {
                rec?.release()
                main.post { if (my == session) onError("Couldn't open the microphone. Close other apps using it and try again.") }
                return@thread
            }
            val frame = ShortArray(FRAME)
            var announced = false
            try {
                rec.startRecording()
                while (my == session && det.state != SpeechDetector.State.DONE) {
                    val n = rec.read(frame, 0, frame.size)
                    if (n <= 0) continue
                    det.feed(frame.copyOf(n))
                    if (!announced && det.state == SpeechDetector.State.SPEAKING) {
                        announced = true
                        main.post { if (my == session) onPartial("🎙️ Listening… (pause for 2 seconds when you're done)") }
                    }
                }
            } finally {
                runCatching { rec.stop() }
                rec.release()
            }
            if (my != session) return@thread
            val audio = det.audio()
            if (audio.isEmpty()) {
                main.post { if (my == session) onDone(Heard("", null, null)) }
                return@thread
            }
            main.post { if (my == session) onPartial("✍️ Understanding what you said…") }
            val result = runCatching { stt.transcribe(audio, RATE, language) }
            main.post {
                if (my != session) return@post
                result.onSuccess { onDone(Heard(it, det.speechMs / 1000.0, null)) }
                    .onFailure { onError(it.message ?: "Sarvam couldn't transcribe that.") }
            }
        }
    }

    override fun finishNow() {
        detector?.finish()
    }

    override fun cancel() {
        session++
    }

    override fun destroy() = cancel()

    private companion object {
        const val RATE = 16_000
        const val FRAME = 320 // 20 ms
    }
}
