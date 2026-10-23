package com.yash.gyaanly.data

/**
 * A learning track, described entirely by its JSON pack (assets/content/<key>.json).
 * Adding a new track means adding a new JSON file — no code changes.
 */
data class Track(
    val key: String,
    val title: String,
    val tagline: String,
    val emoji: String,
    /** ARGB colour, e.g. parsed from "#7C3AED". */
    val color: Long,
    /** Position in lists; lower first. */
    val order: Int,
    /** Lets the learner choose the level today's lesson starts from (e.g. CEFR levels). */
    val levelPicker: Boolean,
    /** Optional tools linked from the track, e.g. "calculators". */
    val tools: List<String>,
    val levels: List<String>,
    /** Today's lesson is a random unfinished one instead of the next in order. */
    val shuffle: Boolean = false,
    /** Optional target date (yyyy-MM-dd) to finish the track by; the app shows the pace needed. */
    val deadline: String? = null,
)

data class Section(val heading: String, val body: String)

data class KeyTerm(val word: String, val meaning: String)

data class Calculation(val problem: String, val steps: List<String>, val answer: String)

/** An external resource for the lesson, e.g. a YouTube channel to watch and shadow. */
data class LessonLink(val label: String, val url: String)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val answerIndex: Int,
    val explanation: String,
)

data class Lesson(
    val id: String,
    val track: Track,
    val level: String,
    val title: String,
    val summary: String,
    val sections: List<Section>,
    val keyTerm: KeyTerm?,
    val example: String?,
    val calculation: Calculation?,
    /** Sentences to listen to and repeat (pronunciation drill). */
    val speak: List<String>,
    /** Language the speak sentences are in (BCP-47, e.g. "en-US", "es-ES"). */
    val speakLang: String,
    /** One concrete thing to do today. */
    val action: String?,
    /** Id of a related finance calculator, if any. */
    val calculatorId: String?,
    val quiz: List<QuizQuestion>,
    val link: LessonLink? = null,
)

data class TrackContent(val track: Track, val lessons: List<Lesson>)

data class Progress(
    val completed: Set<String> = emptySet(),
    /** ISO dates (yyyy-MM-dd) on which at least one lesson was completed. */
    val activeDays: Set<String> = emptySet(),
    /** Best quiz score per lesson id, as "correct/total". */
    val quizScores: Map<String, String> = emptyMap(),
    /** Lesson picked for each track on [planDate]; keeps "today" stable for the whole day. */
    val planDate: String = "",
    val plan: Map<String, String> = emptyMap(),
    /** Chosen starting level per track key (for tracks with a level picker). */
    val levels: Map<String, String> = emptyMap(),
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    /** Courses switched off in Settings. */
    val hiddenTracks: Set<String> = emptySet(),
    /** Topics switched off in Settings, as [levelId] strings. */
    val hiddenLevels: Set<String> = emptySet(),
) {
    fun showsTrack(key: String) = key !in hiddenTracks

    fun showsLevel(key: String, level: String) = levelId(key, level) !in hiddenLevels

    companion object {
        fun levelId(key: String, level: String) = "$key|$level"
    }

    /** 10 XP per lesson finished + 5 XP per best correct quiz answer. */
    val xp: Int
        get() = completed.size * 10 + quizScores.values.sumOf { it.substringBefore("/").toIntOrNull() ?: 0 } * 5
}

/** A book that goes with a track (assets/books/<track>.json). Block types: h, h3, p, em, q (quote), li. */
data class BookBlock(val type: String, val text: String)

data class BookChapter(val title: String, val blocks: List<BookBlock>)

data class Book(
    val key: String,
    val title: String,
    val subtitle: String,
    val author: String,
    val notice: String,
    val chapters: List<BookChapter>,
)
