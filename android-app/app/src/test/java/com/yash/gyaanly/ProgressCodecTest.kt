package com.yash.gyaanly

import com.yash.gyaanly.data.Progress
import com.yash.gyaanly.data.ProgressCodec
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressCodecTest {

    @Test
    fun backupRoundTripKeepsLearningData() {
        val original = Progress(
            completed = setOf("ai-01", "en-int-03", "es-12"),
            activeDays = setOf("2026-10-11", "2026-10-12"),
            quizScores = mapOf("ai-01" to "3/3", "es-12" to "2/3"),
            planDate = "2026-10-12",
            plan = mapOf("ai" to "ai-02"),
            levels = mapOf("english" to "B2", "spanish" to "A1"),
            reminderEnabled = true,
            reminderHour = 7,
            reminderMinute = 30,
        )
        // Today's plan isn't part of the backup; the app rebuilds it after restoring.
        assertEquals(original.copy(planDate = "", plan = emptyMap()), ProgressCodec.decode(ProgressCodec.encode(original)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOtherJsonFiles() {
        ProgressCodec.decode("""{"track": {"key": "ai"}}""")
    }
}
