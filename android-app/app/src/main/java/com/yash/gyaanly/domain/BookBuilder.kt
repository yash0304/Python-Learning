package com.yash.gyaanly.domain

import com.yash.gyaanly.data.Book
import com.yash.gyaanly.data.BookBlock
import com.yash.gyaanly.data.BookChapter
import com.yash.gyaanly.data.Lesson
import com.yash.gyaanly.data.TrackContent

/** Turns lessons into book chapters, shareable text and read-aloud segments. Pure Kotlin for testing. */
object BookBuilder {

    /** One piece of read-aloud text and the BCP-47 language to speak it in. */
    data class Speech(val text: String, val lang: String)

    /** A book of the given levels (default: all), one chapter per level, ending with a quick self-check. */
    fun fromTrack(pack: TrackContent, levels: List<String> = pack.track.levels, author: String = "Gyaanly"): Book {
        val track = pack.track
        val chapters = levels.mapIndexedNotNull { ci, level ->
            val lessons = pack.lessons.filter { it.level == level }
            if (lessons.isEmpty()) return@mapIndexedNotNull null
            val blocks = mutableListOf<BookBlock>()
            lessons.forEachIndexed { li, lesson -> blocks += lessonBlocks(lesson, "${ci + 1}.${li + 1}  ") }
            val checks = lessons.mapNotNull { it.quiz.firstOrNull() }
            if (checks.isNotEmpty()) {
                blocks += BookBlock("h", "Quick self-check")
                checks.forEachIndexed { i, q ->
                    blocks += BookBlock("p", "${i + 1}. ${q.question}  " + q.options.mapIndexed { o, opt -> "(${LETTERS[o]}) $opt" }.joinToString("  "))
                }
                blocks += BookBlock("em", "Answers: " + checks.mapIndexed { i, q -> "${i + 1}: ${LETTERS[q.answerIndex]}" }.joinToString(", "))
            }
            BookChapter("Chapter ${ci + 1}: $level", blocks)
        }
        val scope = if (levels.size == 1) levels.first() else "${pack.lessons.count { it.level in levels }} lessons"
        return Book(
            key = if (levels == track.levels) track.key else "${track.key}-${slug(levels.joinToString("-"))}",
            title = track.title,
            subtitle = "${track.tagline} · $scope",
            author = author,
            notice = "Made with Gyaanly. Lessons are general learning material, not professional advice.",
            chapters = chapters,
        )
    }

    fun lessonBlocks(lesson: Lesson, number: String = ""): List<BookBlock> = buildList {
        add(BookBlock("h", number + lesson.title))
        if (lesson.summary.isNotBlank()) add(BookBlock("em", lesson.summary))
        lesson.keyTerm?.let { add(BookBlock("h3", "Key term")); add(BookBlock("p", "${it.word}: ${it.meaning}")) }
        lesson.sections.forEach { s ->
            add(BookBlock("h3", s.heading))
            addAll(paragraphs(s.body))
        }
        lesson.example?.let { add(BookBlock("h3", "Example")); it.lines().filter(String::isNotBlank).forEach { line -> add(BookBlock("q", line.trim())) } }
        lesson.calculation?.let { c ->
            add(BookBlock("h3", "Try it"))
            add(BookBlock("p", c.problem))
            c.steps.forEach { add(BookBlock("li", it)) }
            add(BookBlock("p", "Answer: ${c.answer}"))
        }
        if (lesson.speak.isNotEmpty()) {
            add(BookBlock("h3", "Say it aloud"))
            lesson.speak.forEach { add(BookBlock("li", it)) }
        }
        lesson.action?.let { add(BookBlock("h3", "Today's mission")); add(BookBlock("p", it)) }
    }

    /** The lesson as plain text, for sharing to WhatsApp, email, notes and so on. */
    fun plainText(lesson: Lesson): String = buildString {
        appendLine("${lesson.track.emoji} ${lesson.title}")
        lessonBlocks(lesson).drop(1).forEach { b ->
            when (b.type) {
                "h3" -> { appendLine(); appendLine(b.text.uppercase()) }
                "li" -> appendLine("• ${b.text}")
                else -> appendLine(b.text)
            }
        }
        appendLine()
        append("Learned with Gyaanly · ${lesson.track.title}")
    }

    /**
     * The whole lesson as read-aloud segments. Explanations use [explainLang]; example dialogues and
     * speaking drills use the lesson's own language, so Spanish lines are read by a Spanish voice.
     */
    fun speech(lesson: Lesson, explainLang: String): List<Speech> {
        val native = lesson.speakLang
        val out = mutableListOf<Speech>()
        fun say(text: String, lang: String = explainLang) = spoken(text).takeIf { it.isNotBlank() }?.let { out += Speech(it, lang) }
        say(lesson.title + ".")
        say(lesson.summary)
        lesson.keyTerm?.let { say("Key term: ${it.word}. ${it.meaning}") }
        lesson.sections.forEach { s -> say(s.heading + "."); s.body.lines().forEach { say(it) } }
        lesson.example?.let { ex ->
            say("Example.")
            ex.lines().forEach { say(it, if (native != explainLang && !native.startsWith("en")) native else explainLang) }
        }
        lesson.calculation?.let { c -> say("Try it. ${c.problem}"); c.steps.forEach { say(it) }; say("Answer: ${c.answer}") }
        if (lesson.speak.isNotEmpty()) { say("Say it aloud."); lesson.speak.forEach { say(it, native) } }
        lesson.action?.let { say("Today's mission. $it") }
        return out
    }

    /** Read-aloud segments for a book chapter (all in one language). */
    fun speech(chapter: BookChapter, lang: String): List<Speech> =
        (listOf(chapter.title) + chapter.blocks.map { it.text }).map(::spoken).filter { it.isNotBlank() }.map { Speech(it, lang) }

    // Drop symbols that voices read out literally ("bullet", "arrow") and dialogue speaker labels.
    private fun spoken(text: String): String = text
        .replace(Regex("^\\s*[•\\-*]\\s*"), "")
        .replace(Regex("^[A-Z]:\\s+"), "")
        .replace("→", " to ").replace("·", ",").replace("…", "...")
        .trim()

    private fun paragraphs(body: String): List<BookBlock> = body.lines().filter(String::isNotBlank).map { line ->
        val t = line.trim()
        if (t.startsWith("• ")) BookBlock("li", t.removePrefix("• ")) else BookBlock("p", t)
    }

    private fun slug(s: String) = s.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(40)

    private const val LETTERS = "ABCDEFGH"
}
