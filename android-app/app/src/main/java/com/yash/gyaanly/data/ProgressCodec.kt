package com.yash.gyaanly.data

import org.json.JSONArray
import org.json.JSONObject

/** Converts progress to and from a portable JSON backup (kept free of Android APIs for testing). */
object ProgressCodec {
    private const val FORMAT = "gyaanly-progress"
    private const val VERSION = 1

    fun encode(p: Progress): String = JSONObject()
        .put("format", FORMAT)
        .put("version", VERSION)
        .put("completed", JSONArray(p.completed.sorted()))
        .put("activeDays", JSONArray(p.activeDays.sorted()))
        .put("quizScores", JSONObject(p.quizScores))
        .put("levels", JSONObject(p.levels))
        .put("reminderEnabled", p.reminderEnabled)
        .put("reminderHour", p.reminderHour)
        .put("reminderMinute", p.reminderMinute)
        .put("hiddenTracks", JSONArray(p.hiddenTracks.sorted()))
        .put("hiddenLevels", JSONArray(p.hiddenLevels.sorted()))
        .toString(2)

    /** Restores learning data from a backup; today's plan is rebuilt by the app afterwards. */
    fun decode(json: String): Progress {
        val o = JSONObject(json)
        require(o.optString("format") == FORMAT) { "Not a Gyaanly backup file" }
        return Progress(
            completed = o.getJSONArray("completed").strings().toSet(),
            activeDays = o.getJSONArray("activeDays").strings().toSet(),
            quizScores = o.getJSONObject("quizScores").stringMap(),
            levels = o.optJSONObject("levels")?.stringMap().orEmpty(),
            reminderEnabled = o.optBoolean("reminderEnabled", false),
            reminderHour = o.optInt("reminderHour", 8),
            reminderMinute = o.optInt("reminderMinute", 0),
            hiddenTracks = o.optJSONArray("hiddenTracks")?.strings()?.toSet().orEmpty(),
            hiddenLevels = o.optJSONArray("hiddenLevels")?.strings()?.toSet().orEmpty(),
        )
    }

    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
    private fun JSONObject.stringMap(): Map<String, String> = keys().asSequence().associateWith { getString(it) }
}
