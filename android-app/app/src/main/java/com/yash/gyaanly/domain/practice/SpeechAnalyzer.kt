package com.yash.gyaanly.domain.practice

import kotlin.math.roundToInt

/** A shortcoming found in the conversation, with an example from what was said and a better way. */
data class Finding(val title: String, val detail: String, val example: String? = null, val better: String? = null)

data class TurnReview(
    val prompt: String,
    val said: String,
    val sample: String?,
    val covered: List<String>,
    val missed: List<String>,
    val wordsPerMinute: Int?,
    val fillers: List<String>,
)

data class Report(
    val overall: Int,
    val fluency: Int,
    val content: Int?,
    val grammar: Int,
    val delivery: Int,
    val wordsPerMinute: Int?,
    val fillerCount: Int,
    val totalWords: Int,
    val strengths: List<String>,
    val shortcomings: List<Finding>,
    val turns: List<TurnReview>,
)

/**
 * Offline feedback on a spoken conversation: pace, filler words, hedging, answer length, key points
 * covered, common grammar slips (especially in Indian English at global workplaces) and, for sales
 * and interviews, whether you asked questions and closed with a next step.
 */
object SpeechAnalyzer {

    private val SINGLE_FILLERS = setOf("um", "umm", "uh", "uhh", "er", "erm", "hmm", "basically", "actually", "literally")
    private val MULTI_FILLERS = listOf("you know", "i mean", "kind of", "sort of", "what to say")
    private val HEDGES = listOf("i think", "maybe", "i guess", "probably", "sorry", "just", "i'm not sure", "i am not sure")
    private val QUESTION_STARTS = listOf(
        "what", "how", "why", "when", "where", "who", "which", "could you", "would you", "can you", "do you", "are you",
        "is there", "have you", "may i", "shall we", "would it",
    )
    private val NEXT_STEP = listOf("meeting", "call", "demo", "schedul", "calendar", "tomorrow", "next week", "follow up", "follow-up", "invite", "slot", "time")

    private data class Slip(val pattern: Regex, val title: String, val better: String)

    private val SLIPS = listOf(
        Slip(Regex("\\bdiscuss(ed|ing)? about\\b"), "\"Discuss about\"", "Say \"discuss the plan\" (no \"about\")."),
        Slip(Regex("\\brevert(ed|ing)? back\\b"), "\"Revert back\"", "Say \"I'll get back to you\" or \"I'll reply\"."),
        Slip(Regex("\\breturn(ed|ing)? back\\b"), "\"Return back\"", "Say \"return\"; \"back\" is already included."),
        Slip(Regex("\\bdo the needful\\b"), "\"Do the needful\"", "Say \"please could you handle this\" or name the action."),
        Slip(Regex("\\bsince (\\d+|one|two|three|four|five|six|seven|eight|nine|ten|many|several) (years?|months?|weeks?|days?)\\b"), "\"Since 5 years\"", "Use \"for\" with a duration: \"for five years\"; \"since\" is for a start point: \"since 2019\"."),
        Slip(Regex("\\bi am having (\\w+ )?(experience|doubt|knowledge|idea)\\b"), "\"I am having experience\"", "Say \"I have five years of experience\"."),
        Slip(Regex("\\bi have (a |one )?doubt\\b"), "\"I have a doubt\"", "Globally, say \"I have a question\"."),
        Slip(Regex("\\bcope up\\b"), "\"Cope up\"", "Say \"cope with\"."),
        Slip(Regex("\\bprepone\\b"), "\"Prepone\"", "Outside India say \"bring forward\" or \"move it earlier\"."),
        Slip(Regex("^myself \\w+"), "\"Myself Yash\"", "Say \"I'm Yash\" or \"My name is Yash\"."),
        Slip(Regex("\\bout of station\\b"), "\"Out of station\"", "Say \"out of town\" or \"travelling\"."),
        Slip(Regex("\\bpassed out (from|of|in)\\b"), "\"Passed out from college\"", "Say \"graduated from\"; \"passed out\" means fainted."),
        Slip(Regex("\\bupdation\\b"), "\"Updation\"", "Say \"update\" or \"updating\"."),
        Slip(Regex("\\bexperiences in\\b|\\byears of experiences\\b"), "\"Experiences\" for work history", "Say \"experience\" (uncountable) for work background."),
        Slip(Regex("\\b(today|now|tomorrow) itself\\b"), "\"Today itself\"", "Say \"today\", \"right away\" or \"by the end of today\"."),
        Slip(Regex("\\baccording to me\\b"), "\"According to me\"", "Say \"in my view\" or \"I think\"."),
        Slip(Regex("\\bexplain me\\b"), "\"Explain me\"", "Say \"explain to me\" or \"could you explain\"."),
        Slip(Regex("\\bi am (understanding|knowing|wanting|liking)\\b"), "\"I am understanding\"", "Use simple present for these verbs: \"I understand\"."),
        Slip(Regex("\\bwhat is your good name\\b"), "\"What is your good name\"", "Say \"May I have your name?\""),
        Slip(Regex("\\b(more better|more easier|more faster)\\b"), "Double comparative", "Say \"better\", \"easier\", \"faster\"."),
        Slip(Regex("\\bhe don't\\b|\\bshe don't\\b|\\bit don't\\b"), "\"He don't\"", "Say \"he doesn't\"."),
        Slip(Regex("\\bkindly\\b.*\\bkindly\\b"), "Too many \"kindly\"s", "Use \"please\" or \"could you\"; \"kindly\" sounds stiff when repeated."),
    )

    fun stem(word: String): String = word.lowercase().let { w ->
        when {
            w.length > 6 && w.endsWith("ing") -> w.dropLast(3)
            w.length > 5 && w.endsWith("ed") -> w.dropLast(2)
            w.length > 5 && w.endsWith("es") -> w.dropLast(2)
            w.length > 4 && w.endsWith("s") -> w.dropLast(1)
            else -> w
        }
    }

    fun words(text: String): List<String> = text.lowercase().split(Regex("[^\\p{L}\\p{N}'%]+")).filter { it.isNotBlank() }

    /** True when the answer mentions any keyword of the point (word-prefix match, or phrase match). */
    fun covers(text: String, point: KeyPoint): Boolean {
        val lower = text.lowercase()
        val tokens = words(text)
        return point.keywords.any { k ->
            when {
                k.any { !it.isLetterOrDigit() && it != '\'' } -> k in lower // phrases and symbols: "on time", "%", "wi-fi"
                k.length <= 3 -> k in tokens // short stems must be whole words, or "ai" would match "explain"
                else -> tokens.any { it.startsWith(k) }
            }
        }
    }

    fun fillersIn(text: String): List<String> {
        val tokens = words(text)
        val found = tokens.filter { it in SINGLE_FILLERS }.toMutableList()
        val lower = " " + tokens.joinToString(" ") + " "
        MULTI_FILLERS.forEach { f -> found += List(Regex("\\b$f\\b").findAll(lower).count()) { f } }
        return found
    }

    fun analyze(scenario: Scenario, answers: List<Answer>): Report {
        val spoken = answers.filter { it.text.isNotBlank() }
        val reviews = answers.map { a ->
            val points = a.turn?.points.orEmpty()
            val covered = points.filter { covers(a.text, it) }.map { it.idea }
            val n = words(a.text).size
            TurnReview(
                prompt = a.prompt,
                said = a.text,
                sample = a.turn?.sample,
                covered = covered,
                missed = points.map { it.idea } - covered.toSet(),
                wordsPerMinute = a.seconds?.takeIf { it >= 2.0 && n >= 4 }?.let { (n / (it / 60.0)).roundToInt() },
                fillers = fillersIn(a.text),
            )
        }
        val totalWords = spoken.sumOf { words(it.text).size }
        val fillerCount = reviews.sumOf { it.fillers.size }
        val timed = spoken.filter { it.seconds != null && it.seconds >= 2.0 }
        val wpm = timed.sumOf { it.seconds!! }.takeIf { it > 0 }?.let { secs ->
            (timed.sumOf { words(it.text).size } / (secs / 60.0)).roundToInt()
        }
        val findings = mutableListOf<Finding>()
        val strengths = mutableListOf<String>()

        // Silence
        val silent = answers.count { it.text.isBlank() }
        if (silent > 0) findings += Finding(
            "No answer on $silent turn${if (silent > 1) "s" else ""}",
            "In a real call, silence feels awkward. Buy time with a phrase instead of staying quiet.",
            better = "\"That's a good question; let me think for a second.\"",
        )

        // Fillers
        val per100 = if (totalWords == 0) 0.0 else fillerCount * 100.0 / totalWords
        when {
            totalWords == 0 -> Unit
            per100 >= 4 -> {
                val top = reviews.flatMap { it.fillers }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(3)
                findings += Finding(
                    "Too many filler words ($fillerCount)",
                    "Fillers like ${top.joinToString { "\"${it.key}\" ×${it.value}" }} make you sound unsure. Replace them with a short pause.",
                    example = reviews.firstOrNull { it.fillers.isNotEmpty() }?.said,
                    better = "Pause silently for a second; pauses sound confident.",
                )
            }
            per100 <= 1.5 -> strengths += "Clean speech: very few filler words."
        }

        // Pace
        when {
            wpm == null -> Unit
            wpm < 95 -> findings += Finding("Speaking slowly ($wpm words/min)", "Natural conversation is about 120–160 words a minute. Long gaps can sound hesitant; practise the key phrases until they flow.")
            wpm > 175 -> findings += Finding("Speaking fast ($wpm words/min)", "Above ~170 words a minute, listeners (especially on calls) miss things. Slow down and pause between ideas.")
            else -> strengths += "Good pace: $wpm words a minute."
        }

        // Hedging
        val hedges = spoken.flatMap { a -> HEDGES.filter { h -> Regex("\\b${Regex.escape(h)}\\b").containsMatchIn(a.text.lowercase()) } }
        if (hedges.size >= 3) findings += Finding(
            "Hedging (${hedges.distinct().joinToString { "\"$it\"" }})",
            "Too many softeners weaken your message. State facts directly and keep \"sorry\" for real mistakes.",
            better = "Instead of \"I think maybe we can try\", say \"I recommend we try\".",
        )

        // Length
        val short = reviews.filter { it.said.isNotBlank() && words(it.said).size < 6 }
        val long = reviews.filter { words(it.said).size > 110 }
        if (short.size >= 2) findings += Finding(
            "Very short answers (${short.size})",
            "One- or two-word replies sound disengaged. Add a reason or an example.",
            example = short.first().said,
            better = short.first().sample,
        )
        if (long.isNotEmpty()) findings += Finding(
            "Long, rambling answers (${long.size})",
            "Answers over ~45 seconds lose the listener. Lead with the main point, add one example, then stop.",
        )
        if (short.size < 2 && long.isEmpty() && spoken.isNotEmpty()) strengths += "Answers were a good length."

        // Content
        val withPoints = reviews.filter { r -> (r.covered.size + r.missed.size) > 0 }
        val content = if (withPoints.isEmpty()) null
        else (withPoints.sumOf { it.covered.size.toDouble() / (it.covered.size + it.missed.size) } / withPoints.size * 100).roundToInt()
        if (content != null) {
            if (content >= 75) strengths += "Covered most of the key points ($content%)."
            else {
                val worst = withPoints.filter { it.missed.isNotEmpty() }.maxByOrNull { it.missed.size }
                findings += Finding(
                    "Missed key points (covered $content%)",
                    "Some answers didn't include what the other person was looking for: ${worst?.missed?.joinToString("; ")}.",
                    example = worst?.said?.ifBlank { null },
                    better = worst?.sample,
                )
            }
        }

        // Grammar slips
        val slipHits = spoken.flatMap { a -> SLIPS.filter { it.pattern.containsMatchIn(a.text.lowercase()) }.map { it to a.text } }
        slipHits.distinctBy { it.first.title }.forEach { (slip, said) ->
            findings += Finding("Phrase to fix: ${slip.title}", "Common in Indian English, but it can confuse global colleagues.", example = said, better = slip.better)
        }
        if (slipHits.isEmpty() && spoken.isNotEmpty()) strengths += "No common grammar slips spotted."

        // Sales and interviews: questions and next steps
        val asked = spoken.count { a -> QUESTION_STARTS.any { q -> Regex("(^|[.?!,]\\s*)${Regex.escape(q)}\\b").containsMatchIn(a.text.lowercase()) } || '?' in a.text }
        if (scenario.category in setOf("Sales calls", "Interview")) {
            if (asked == 0) findings += Finding(
                "You didn't ask any questions",
                if (scenario.category == "Sales calls") "Great sellers ask about the prospect's pain before pitching." else "Asking a thoughtful question shows interest and confidence.",
                better = if (scenario.category == "Sales calls") "\"How does your team handle reconciliations at month end today?\"" else "\"What would success look like in this role in the first six months?\"",
            ) else strengths += "You asked $asked question${if (asked > 1) "s" else ""}."
        }
        if (scenario.category == "Sales calls") {
            val closed = spoken.takeLast(2).any { a -> NEXT_STEP.any { covers(a.text, KeyPoint("next step", listOf(it))) } }
            if (!closed) findings += Finding(
                "No clear next step",
                "Every sales call should end by asking for a specific next step.",
                better = "\"Could we book 20 minutes on Thursday to show you how it works with your data?\"",
            ) else strengths += "You asked for a next step."
        }

        // Recogniser confidence as a rough clarity signal
        val conf = spoken.mapNotNull { it.confidence }.filter { it > 0f }
        val avgConf = conf.takeIf { it.isNotEmpty() }?.average()
        if (avgConf != null && avgConf < 0.6) findings += Finding(
            "Some words were hard to recognise",
            "The phone's recogniser struggled with parts of your speech. Open your mouth a little more, finish word endings, and avoid trailing off.",
        )

        val fluency = (100 - per100 * 6 - when {
            wpm == null -> 0
            wpm < 95 -> (95 - wpm) / 2
            wpm > 175 -> (wpm - 175) / 2
            else -> 0
        } - hedges.size * 2).roundToInt().coerceIn(20, 100)
        val grammar = (100 - slipHits.distinctBy { it.first.title }.size * 12).coerceIn(30, 100)
        val delivery = (100 - silent * 15 - short.size * 8 - long.size * 8 - (if (avgConf != null && avgConf < 0.6) 15 else 0)).coerceIn(20, 100)
        val parts = listOfNotNull(fluency to 3, grammar to 2, delivery to 2, content?.let { it to 3 })
        val overall = if (spoken.isEmpty()) 0 else (parts.sumOf { it.first * it.second }.toDouble() / parts.sumOf { it.second }).roundToInt()

        return Report(overall, fluency, content, grammar, delivery, wpm, fillerCount, totalWords, strengths, findings, reviews)
    }
}
