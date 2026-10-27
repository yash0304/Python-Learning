package com.yash.gyaanly.domain.practice

import kotlin.math.max
import kotlin.math.sqrt

/**
 * Energy-based voice activity detection for recorded answers. Feed 16-bit PCM frames; it waits for
 * you to start, keeps recording through short pauses, and reports DONE after [endSilenceMs] of
 * silence (or [maxWaitMs] with no speech at all). Pure Kotlin so it can be unit-tested.
 */
class SpeechDetector(
    private val sampleRate: Int = 16_000,
    private val endSilenceMs: Int = 2_000,
    private val maxWaitMs: Int = 12_000,
    private val maxSpeechMs: Int = 120_000,
    private val preRollMs: Int = 300,
) {
    enum class State { WAITING, SPEAKING, DONE }

    var state = State.WAITING
        private set

    private val captured = ArrayList<ShortArray>()
    private var elapsedMs = 0.0
    private var firstVoiceMs = -1.0
    private var lastVoiceMs = -1.0
    private var noiseFloor = INITIAL_NOISE_RMS
    private var lastVoiceFrame = -1
    private var voicedStreak = 0

    /** Milliseconds from the first to the last voiced frame; 0 if nobody spoke. */
    val speechMs: Double get() = if (firstVoiceMs < 0) 0.0 else lastVoiceMs - firstVoiceMs

    val heardSpeech: Boolean get() = firstVoiceMs >= 0

    fun feed(frame: ShortArray): State {
        if (state == State.DONE || frame.isEmpty()) return state
        val frameMs = frame.size * 1000.0 / sampleRate
        val level = rms(frame)
        val voiced = level > max(noiseFloor * 3.0, MIN_VOICE_RMS)
        if (!voiced) noiseFloor = noiseFloor * 0.95 + level * 0.05

        captured += frame
        elapsedMs += frameMs
        when (state) {
            State.WAITING -> {
                voicedStreak = if (voiced) voicedStreak + 1 else 0
                // Three voiced frames in a row (~60 ms) so a single click or tap doesn't count as speech.
                if (voicedStreak >= START_FRAMES) {
                    state = State.SPEAKING
                    firstVoiceMs = elapsedMs - frameMs * START_FRAMES
                    lastVoiceMs = elapsedMs
                    // Keep a little audio from just before you started so the first word isn't clipped.
                    val keep = (preRollMs / frameMs).toInt() + 1
                    while (captured.size > keep) captured.removeAt(0)
                    lastVoiceFrame = captured.lastIndex
                } else {
                    val keep = (preRollMs / frameMs).toInt() + 1
                    while (captured.size > keep) captured.removeAt(0)
                    if (elapsedMs >= maxWaitMs) state = State.DONE
                }
            }
            State.SPEAKING -> {
                if (voiced) {
                    lastVoiceMs = elapsedMs
                    lastVoiceFrame = captured.lastIndex
                }
                if (elapsedMs - lastVoiceMs >= endSilenceMs || speechMs >= maxSpeechMs) state = State.DONE
            }
            State.DONE -> Unit
        }
        return state
    }

    /** Stops now (e.g. "I'm done speaking"). */
    fun finish() {
        state = State.DONE
    }

    /** The recorded answer, with long trailing silence trimmed; empty if nobody spoke. */
    fun audio(): ShortArray {
        if (!heardSpeech) return ShortArray(0)
        val end = (lastVoiceFrame + TAIL_FRAMES).coerceAtMost(captured.lastIndex)
        val frames = captured.subList(0, end + 1)
        val out = ShortArray(frames.sumOf { it.size })
        var pos = 0
        frames.forEach { f -> f.copyInto(out, pos); pos += f.size }
        return out
    }

    private fun rms(frame: ShortArray): Double {
        var sum = 0.0
        for (s in frame) sum += s.toDouble() * s
        return sqrt(sum / frame.size)
    }

    private companion object {
        /** Below this RMS (16-bit scale) a frame is never speech, however quiet the room. */
        const val MIN_VOICE_RMS = 400.0
        /** Starting guess for a quiet room; adapts within a second or so. */
        const val INITIAL_NOISE_RMS = 150.0
        const val START_FRAMES = 3
        const val TAIL_FRAMES = 15 // ~300 ms of silence kept after the last word
    }
}
