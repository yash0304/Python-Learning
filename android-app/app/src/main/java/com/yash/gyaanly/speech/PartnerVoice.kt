package com.yash.gyaanly.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.yash.gyaanly.data.ai.SarvamVoice
import com.yash.gyaanly.domain.BookBuilder
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** The conversation partner's voice. */
interface PartnerVoice {
    /** Speaks [text]; [onDone] runs on the main thread when it ends. Returns false if it can't start. */
    fun say(text: String, onDone: () -> Unit): Boolean

    /** Gets a line ready in advance so it plays without a delay (no-op for the phone voice). */
    fun prefetch(text: String) = Unit

    fun stop()

    fun shutdown()
}

/** The phone's built-in text-to-speech. */
class PhoneVoice(private val speaker: Speaker, private val lang: String) : PartnerVoice {
    override fun say(text: String, onDone: () -> Unit) = speaker.read(listOf(BookBuilder.Speech(text, lang))) { onDone() }
    override fun stop() = speaker.stop()
    override fun shutdown() = Unit // the screen owns the Speaker
}

/**
 * Sarvam Bulbul voices: natural, human-sounding Indian voices. Lines are fetched over the network
 * (and prefetched while you answer); if Sarvam fails, the phone voice says the line instead and
 * [onFallback] reports why.
 */
class BulbulVoice(
    context: Context,
    private val tts: SarvamVoice,
    private val fallback: PartnerVoice,
    private val onFallback: (String) -> Unit,
) : PartnerVoice {

    private val main = Handler(Looper.getMainLooper())
    private val pool = Executors.newFixedThreadPool(2)
    private val dir = File(context.cacheDir, "bulbul").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
    private val pending = ConcurrentHashMap<String, Future<File>>()
    private var player: MediaPlayer? = null
    private var token = 0

    private fun fetch(text: String): Future<File> = pending.getOrPut(text) {
        pool.submit<File> {
            File(dir, "line-${text.hashCode()}.wav").also { f -> if (!f.exists()) f.writeBytes(tts.synthesize(text)) }
        }
    }

    override fun prefetch(text: String) {
        if (text.isNotBlank()) fetch(text)
    }

    override fun say(text: String, onDone: () -> Unit): Boolean {
        val my = ++token
        stopPlayer()
        val future = fetch(text)
        pool.submit {
            val file = runCatching { future.get() }
            main.post {
                if (my != token) return@post
                file.onSuccess { play(it, my, onDone) }.onFailure { e ->
                    pending.remove(text)
                    onFallback((e.cause ?: e).message ?: "Sarvam voice failed")
                    fallback.say(text) { if (my == token) onDone() }
                }
            }
        }
        return true
    }

    private fun play(file: File, my: Int, onDone: () -> Unit) {
        val p = MediaPlayer()
        player = p
        try {
            p.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            p.setDataSource(file.absolutePath)
            p.setOnCompletionListener { if (my == token) { stopPlayer(); onDone() } }
            p.setOnErrorListener { _, _, _ -> if (my == token) { stopPlayer(); onDone() }; true }
            p.prepare()
            p.start()
        } catch (e: Exception) {
            stopPlayer()
            onFallback("Couldn't play the Sarvam voice")
            onDone()
        }
    }

    private fun stopPlayer() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
    }

    override fun stop() {
        token++
        stopPlayer()
        fallback.stop()
    }

    override fun shutdown() {
        stop()
        pool.shutdownNow()
    }
}
