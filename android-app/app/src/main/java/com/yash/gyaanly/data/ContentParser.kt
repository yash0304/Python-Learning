package com.yash.gyaanly.data

import org.json.JSONArray
import org.json.JSONObject

/** Parses a lesson pack (assets/content/<track>.json). Kept free of Android APIs so it is unit-testable. */
object ContentParser {

    fun parse(json: String): TrackContent {
        val root = JSONObject(json)
        val levels = root.getJSONArray("levels").strings()
        val track = parseTrack(root.getJSONObject("track"), levels)
        val lessons = root.getJSONArray("lessons").objects().map { parseLesson(it, track) }
        return TrackContent(track, lessons)
    }

    private fun parseTrack(o: JSONObject, levels: List<String>) = Track(
        key = o.getString("key"),
        title = o.getString("title"),
        tagline = o.optString("tagline"),
        emoji = o.optString("emoji", "📘"),
        color = parseColor(o.optString("color", "#6C3BFF")),
        order = o.optInt("order", 100),
        levelPicker = o.optBoolean("levelPicker", false),
        tools = o.optJSONArray("tools")?.strings().orEmpty(),
        levels = levels,
    )

    private fun parseLesson(o: JSONObject, track: Track): Lesson = Lesson(
        id = o.getString("id"),
        track = track,
        level = o.getString("level"),
        title = o.getString("title"),
        summary = o.optString("summary"),
        sections = o.optJSONArray("sections")?.objects()?.map {
            Section(it.getString("h"), it.getString("b"))
        }.orEmpty(),
        keyTerm = o.optJSONObject("term")?.let { KeyTerm(it.getString("word"), it.getString("meaning")) },
        example = o.optStringOrNull("example"),
        calculation = o.optJSONObject("calc")?.let {
            Calculation(it.getString("problem"), it.getJSONArray("steps").strings(), it.getString("answer"))
        },
        speak = o.optJSONArray("speak")?.strings().orEmpty(),
        speakLang = o.optString("speakLang", "en-US"),
        action = o.optStringOrNull("action"),
        calculatorId = o.optStringOrNull("calculator"),
        quiz = o.optJSONArray("quiz")?.objects()?.map {
            QuizQuestion(
                question = it.getString("q"),
                options = it.getJSONArray("options").strings(),
                answerIndex = it.getInt("answer"),
                explanation = it.optString("why"),
            )
        }.orEmpty(),
        link = o.optJSONObject("link")?.let { LessonLink(it.getString("label"), it.getString("url")) },
    )

    /** Parses "#RRGGBB" or "#AARRGGBB" into an ARGB long. */
    fun parseBook(json: String): Book {
        val o = JSONObject(json)
        return Book(
            key = o.getString("key"),
            title = o.getString("title"),
            subtitle = o.optString("subtitle"),
            author = o.optString("author"),
            notice = o.optString("notice"),
            chapters = o.getJSONArray("chapters").objects().map { c ->
                BookChapter(c.getString("title"), c.getJSONArray("blocks").objects().map { BookBlock(it.getString("t"), it.getString("x")) })
            },
        )
    }

    fun parseColor(hex: String): Long {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        return if (clean.length == 6) 0xFF000000L or value else value
    }

    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null
}
