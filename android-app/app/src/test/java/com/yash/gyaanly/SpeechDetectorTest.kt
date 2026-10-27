package com.yash.gyaanly

import com.yash.gyaanly.data.ai.SarvamSpeech
import com.yash.gyaanly.domain.practice.SpeechDetector
import com.yash.gyaanly.domain.practice.SpeechDetector.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class SpeechDetectorTest {

    private val rng = Random(7)
    private fun silence(ms: Int) = List(ms / 20) { ShortArray(320) { (rng.nextInt(-60, 60)).toShort() } }
    private fun voice(ms: Int) = List(ms / 20) { f -> ShortArray(320) { i -> (3000 * sin(2 * PI * 220 * (f * 320 + i) / 16000.0)).toInt().toShort() } }

    private fun run(det: SpeechDetector, frames: List<ShortArray>): State = frames.fold(det.state) { _, f -> det.feed(f) }

    @Test
    fun endsAfterTwoSecondsOfSilenceButNotDuringAShortPause() {
        val det = SpeechDetector()
        assertEquals(State.WAITING, run(det, silence(1000)))
        assertEquals(State.SPEAKING, run(det, voice(1500)))
        assertEquals(State.SPEAKING, run(det, silence(1200))) // a pause mid-answer
        assertEquals(State.SPEAKING, run(det, voice(1000)))
        assertEquals(State.DONE, run(det, silence(2100)))
        assertTrue("speech ${det.speechMs}", det.speechMs in 3500.0..3800.0)
        // Audio keeps the speech plus short pre-roll and tail, not the full trailing silence.
        val seconds = det.audio().size / 16000.0
        assertTrue("audio $seconds s", seconds in 3.6..4.4)
    }

    @Test
    fun givesUpWhenNobodySpeaks() {
        val det = SpeechDetector(maxWaitMs = 3000)
        assertEquals(State.DONE, run(det, silence(3100)))
        assertFalse(det.heardSpeech)
        assertEquals(0, det.audio().size)
    }

    @Test
    fun aSingleClickIsNotSpeech() {
        val det = SpeechDetector()
        run(det, silence(500))
        det.feed(ShortArray(320) { 8000 })
        run(det, silence(500))
        assertEquals(State.WAITING, det.state)
    }

    @Test
    fun finishNowStopsImmediately() {
        val det = SpeechDetector()
        run(det, voice(600))
        det.finish()
        assertEquals(State.DONE, det.state)
        assertTrue(det.audio().isNotEmpty())
    }

    @Test
    fun wavHeaderDescribes16kMonoPcm() {
        val wav = SarvamSpeech.wav(ShortArray(16000) { 1 }, 16000)
        assertEquals(44 + 32000, wav.size)
        assertEquals("RIFF", String(wav, 0, 4))
        assertEquals("WAVE", String(wav, 8, 4))
        val b = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(1, b.getShort(22).toInt()) // mono
        assertEquals(16000, b.getInt(24))
        assertEquals(16, b.getShort(34).toInt())
        assertEquals(32000, b.getInt(40))
    }
}
