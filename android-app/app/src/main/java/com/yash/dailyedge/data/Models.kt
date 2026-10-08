package com.yash.dailyedge.data

enum class Track(val key: String, val title: String, val tagline: String) {
    AI_EDGE("ai", "Human Edge in AI", "Stay irreplaceable in the age of AI"),
    ENGLISH("english", "English Speaking", "From A1 to confident corporate speaker"),
    FINANCE("finance", "Corporate Finance", "One concept, term and calculation at a time");

    companion object {
        fun fromKey(key: String): Track = entries.first { it.key == key }
    }
}

data class Section(val heading: String, val body: String)

data class KeyTerm(val word: String, val meaning: String)

data class Calculation(val problem: String, val steps: List<String>, val answer: String)

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
    /** Sentences to listen to and repeat (English track). */
    val speak: List<String>,
    /** One concrete thing to do today (Human Edge track). */
    val action: String?,
    /** Id of a related finance calculator, if any. */
    val calculatorId: String?,
    val quiz: List<QuizQuestion>,
)

data class TrackContent(val track: Track, val levels: List<String>, val lessons: List<Lesson>)
