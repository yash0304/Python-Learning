package com.yash.gyaanly.domain.practice

import org.json.JSONArray
import org.json.JSONObject

/** Something a good answer should mention; matched when any keyword stem appears in what was said. */
data class KeyPoint(val idea: String, val keywords: List<String>)

/** One exchange: the partner says [say], the learner answers in their own words. */
data class ScriptTurn(val say: String, val points: List<KeyPoint> = emptyList(), val sample: String? = null)

enum class ScenarioSource { BUILT_IN, MY_SCRIPT, AI }

/** A spoken role-play (assets/practice/scenarios.json, a pasted script, or AI-generated). */
data class Scenario(
    val id: String,
    val category: String,
    val title: String,
    val emoji: String,
    val partner: String,
    val you: String,
    val goal: String,
    val turns: List<ScriptTurn>,
    val closing: String,
    val focus: List<String> = emptyList(),
    val source: ScenarioSource = ScenarioSource.BUILT_IN,
)

/** What the learner said in one turn, with timing from the recogniser. */
data class Answer(
    val prompt: String,
    val text: String,
    /** Seconds between the learner starting and stopping speaking; null if unknown. */
    val seconds: Double? = null,
    /** Recogniser confidence 0..1 when the engine reports it. */
    val confidence: Float? = null,
    val turn: ScriptTurn? = null,
)

object PracticeParser {

    fun parseAll(json: String): List<Scenario> =
        JSONObject(json).getJSONArray("scenarios").let { a -> (0 until a.length()).map { parse(a.getJSONObject(it)) } }

    fun parse(o: JSONObject, source: ScenarioSource = ScenarioSource.BUILT_IN) = Scenario(
        id = o.optString("id", "ai-" + o.optString("title").hashCode()),
        category = o.optString("category", "Custom"),
        title = o.getString("title"),
        emoji = o.optString("emoji", "🎙️"),
        partner = o.optString("partner"),
        you = o.optString("you"),
        goal = o.optString("goal"),
        turns = o.getJSONArray("turns").objects().map { t ->
            ScriptTurn(
                say = t.getString("say"),
                points = t.optJSONArray("points")?.objects()?.map { p ->
                    KeyPoint(p.getString("idea"), p.getJSONArray("keywords").strings().map { it.lowercase() })
                }.orEmpty(),
                sample = t.optString("sample").ifBlank { null },
            )
        },
        closing = o.optString("closing", "Thanks, that's all for today. Goodbye!"),
        focus = o.optJSONArray("focus")?.strings().orEmpty(),
        source = source,
    )

    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
    private fun JSONArray.strings() = (0 until length()).map { getString(it) }
}

/**
 * Turns a pasted dialogue into a role-play. Lines look like "Them: …" / "Me: …"; any speaker
 * that isn't Me/You/I is played by the app. Your own lines become the model answers, so during
 * the conversation you speak freely instead of reading them.
 */
object ScriptParser {

    private val LABEL = Regex("^\\s*([\\p{L} .'-]{1,24}?)\\s*[:\\-–]\\s*(.+)$")
    private val ME = setOf("me", "you", "i", "myself", "learner", "candidate", "student")
    private val STOP = setOf(
        "about", "after", "again", "also", "because", "been", "before", "being", "could", "doing", "from", "have",
        "here", "just", "really", "should", "some", "than", "that", "their", "them", "then", "there", "these",
        "they", "this", "thank", "thanks", "very", "want", "were", "what", "when", "where", "which", "while",
        "will", "with", "would", "your", "yours", "sure", "okay", "great", "please", "into", "like", "know",
    )

    fun parse(text: String, title: String = "My script"): Scenario? {
        val lines = mutableListOf<Pair<Boolean, String>>() // (isMe, text)
        text.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { raw ->
            val m = LABEL.matchEntire(raw)
            if (m != null) {
                lines += (m.groupValues[1].trim().lowercase() in ME) to m.groupValues[2].trim()
            } else if (lines.isNotEmpty()) {
                val (me, prev) = lines.removeAt(lines.lastIndex)
                lines += me to "$prev $raw"
            } else {
                lines += false to raw
            }
        }
        // No "Me:" labels at all: alternate, starting with the other person.
        if (lines.isNotEmpty() && lines.none { it.first }) {
            lines.indices.forEach { i -> lines[i] = (i % 2 == 1) to lines[i].second }
        }
        if (lines.none { !it.first }) return null

        val turns = mutableListOf<ScriptTurn>()
        var i = 0
        if (lines.first().first) {
            // The learner speaks first: start with a neutral prompt.
            val mine = lines.takeWhile { it.first }.joinToString(" ") { it.second }
            turns += turnFor("Go ahead, please.", mine)
            i = lines.indexOfFirst { !it.first }
        }
        var closing = "Thank you. That's all for now."
        while (i < lines.size) {
            val say = lines.drop(i).takeWhile { !it.first }.joinToString(" ") { it.second }
            i += lines.drop(i).takeWhile { !it.first }.size
            val mine = lines.drop(i).takeWhile { it.first }.joinToString(" ") { it.second }
            i += lines.drop(i).takeWhile { it.first }.size
            if (mine.isBlank() && i >= lines.size && turns.isNotEmpty()) closing = say else turns += turnFor(say, mine.ifBlank { null })
        }
        return Scenario(
            id = "script-" + text.hashCode(),
            category = "My scripts",
            title = title,
            emoji = "📝",
            partner = "The other person in your script",
            you = "You",
            goal = "Say your part in your own words, naturally, without reading it.",
            turns = turns,
            closing = closing,
            focus = listOf("Don't memorise word for word: cover the same ideas in your own words."),
            source = ScenarioSource.MY_SCRIPT,
        )
    }

    /** Key points for a scripted answer: its most distinctive words. */
    private fun turnFor(say: String, mine: String?): ScriptTurn {
        val words = mine.orEmpty().lowercase().split(Regex("[^\\p{L}\\p{N}%]+")).filter { it.length >= 4 && it !in STOP }
        val picked = words.distinct().sortedByDescending { it.length }.take(3)
        val points = picked.map { w -> KeyPoint("Mention \"$w\"", listOf(SpeechAnalyzer.stem(w))) }
        return ScriptTurn(say, points, mine)
    }
}
