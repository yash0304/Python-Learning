package com.yash.gyaanly.domain.practice

import org.json.JSONArray
import org.json.JSONObject

/** One message in an AI conversation: from the learner (user) or from the role-play partner. */
data class ChatMessage(val fromLearner: Boolean, val text: String)

enum class AiTask { PARTNER, FEEDBACK, GENERATE }

/** A provider that turns a system prompt plus history into the next reply (Sarvam, Claude…). */
interface ChatEngine {
    /** Blocking network call; run it off the main thread. Throws [AiException] with a readable message. */
    fun reply(system: String, messages: List<ChatMessage>, task: AiTask): String
}

class AiException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class PartnerLine(val text: String, val ended: Boolean)

data class AiFeedback(
    val summary: String,
    val score: Int?,
    val strengths: List<String>,
    val improvements: List<Finding>,
    val phrases: List<String>,
    val nextPractice: String?,
)

/** Provider-neutral prompts and parsing for the AI conversation partner and coach. */
class AiCoach(private val engine: ChatEngine) {

    private fun partnerSystem(s: Scenario, maxTurns: Int) = """
        You are the conversation partner in a spoken English practice session. Everything you write is read aloud by a phone voice.
        You play: ${s.partner}
        The learner plays: ${s.you}
        The learner's goal: ${s.goal}
        Scenario: ${s.title} (${s.category})

        Speak like a real person in this situation: one to three short sentences, plain spoken English, no lists, no markdown, no emojis, no stage directions, no translations.
        React to what the learner actually said and ask natural follow-up questions. Be realistic: interviewers probe for specifics, prospects on sales calls are busy and raise objections, colleagues have their own priorities. Stay polite.
        Never correct the learner's English and never step out of character; feedback comes later.
        The learner's words come from speech recognition and may contain recognition errors. Interpret them charitably.
        After about $maxTurns of your turns, or when the conversation reaches a natural end, say a short closing line and finish with the token [END].
    """.trimIndent()

    /** The partner's next line. Pass the conversation so far (empty to get the opening line). */
    fun partner(s: Scenario, history: List<ChatMessage>, maxTurns: Int = 7): PartnerLine {
        val messages = listOf(ChatMessage(true, "[The conversation begins now. Say your first line in character.]")) + history
        val raw = cleanSpoken(engine.reply(partnerSystem(s, maxTurns), messages, AiTask.PARTNER))
        val ended = "[END]" in raw
        return PartnerLine(raw.replace("[END]", "").trim().ifBlank { s.closing }, ended)
    }

    /** Detailed coaching on the whole conversation, informed by the offline measurements. */
    fun feedback(s: Scenario, answers: List<Answer>, offline: Report): AiFeedback {
        val transcript = answers.joinToString("\n") { a ->
            val timing = a.seconds?.let { " (spoke for %.0fs)".format(it) }.orEmpty()
            "PARTNER: ${a.prompt}\nLEARNER$timing: ${a.text.ifBlank { "[no answer]" }}"
        }
        val system = """
            You are an expert English speaking coach for Indian professionals who work with global teams.
            Review a spoken role-play. The learner's lines are speech-recognition transcripts, so ignore missing punctuation and obvious recognition errors; judge what they said and how.
            Be specific and kind but honest. Quote the learner's actual words when you point out a problem, and give a better way to say it.
            Reply with only a JSON object, no other text, in this shape:
            {"summary": "2-3 sentences", "score": 0-100,
             "strengths": ["..."],
             "improvements": [{"issue": "short title", "youSaid": "exact words", "better": "improved version", "why": "one sentence"}],
             "phrases": ["useful phrases for next time"],
             "nextPractice": "one concrete thing to practise next"}
            Give 3 to 6 improvements, covering content, structure, grammar, word choice, tone and confidence as relevant.
        """.trimIndent()
        val prompt = """
            Scenario: ${s.title} (${s.category}). The learner played: ${s.you}. Goal: ${s.goal}
            Measured automatically: overall ${offline.overall}/100, pace ${offline.wordsPerMinute ?: "unknown"} words per minute, ${offline.fillerCount} filler words, key-point coverage ${offline.content?.let { "$it%" } ?: "n/a"}.

            Transcript:
            $transcript
        """.trimIndent()
        val o = jsonObject(engine.reply(system, listOf(ChatMessage(true, prompt)), AiTask.FEEDBACK))
        return AiFeedback(
            summary = o.optString("summary"),
            score = o.optInt("score", -1).takeIf { it in 0..100 },
            strengths = o.optJSONArray("strengths").strings(),
            improvements = o.optJSONArray("improvements").objects().map {
                Finding(it.optString("issue"), it.optString("why"), it.optString("youSaid").ifBlank { null }, it.optString("better").ifBlank { null })
            },
            phrases = o.optJSONArray("phrases").strings(),
            nextPractice = o.optString("nextPractice").ifBlank { null },
        )
    }

    /** A new role-play from a description, e.g. "cold call to sell payroll software to an HR head". */
    fun generate(description: String, category: String): Scenario {
        val system = """
            You write spoken English role-play scenarios for a practice app used by Indian professionals.
            Reply with only a JSON object, no other text:
            {"title": "...", "emoji": "one emoji", "partner": "who the app plays", "you": "the learner's role", "goal": "one sentence",
             "turns": [{"say": "partner line, 1-3 short spoken sentences ending in something the learner must answer",
                        "points": [{"idea": "what a good answer includes", "keywords": ["lowercase word stems a good answer would contain"]}],
                        "sample": "a natural model answer"}],
             "closing": "partner's final line", "focus": ["2-3 coaching tips"]}
            Write 5 to 7 turns. Each turn's line must still make sense whatever the learner said before. Give 1-3 points per turn with 3-6 keywords each, including synonyms.
        """.trimIndent()
        val o = jsonObject(engine.reply(system, listOf(ChatMessage(true, "Category: $category\nScenario: $description")), AiTask.GENERATE))
        o.put("category", category)
        o.put("id", "ai-" + (description + System.currentTimeMillis()).hashCode())
        return runCatching { PracticeParser.parse(o, ScenarioSource.AI) }.getOrElse { throw AiException("The AI returned a scenario I couldn't read. Please try again.", it) }
    }

    companion object {
        /** Extracts the JSON object from a reply that may include code fences or stray text. */
        fun jsonObject(text: String): JSONObject {
            val start = text.indexOf('{')
            val end = text.lastIndexOf('}')
            if (start < 0 || end <= start) throw AiException("The AI reply wasn't in the expected format. Please try again.")
            return runCatching { JSONObject(text.substring(start, end + 1)) }
                .getOrElse { throw AiException("The AI reply wasn't in the expected format. Please try again.", it) }
        }

        /** Removes reasoning tags, markdown and stage directions so the voice reads only the line. */
        fun cleanSpoken(text: String): String = text
            .replace(Regex("(?s)<think>.*?</think>"), "")
            .replace(Regex("\\*\\*?([^*]+)\\*\\*?"), "$1")
            .replace(Regex("\\([^)]*(pause|smil|laugh|sigh)[^)]*\\)", RegexOption.IGNORE_CASE), "")
            .lines().joinToString(" ") { it.trim().removePrefix("-").trim() }
            .replace(Regex("^\\s*[\\p{L} ]{1,20}:\\s+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        private fun JSONArray?.strings() = if (this == null) emptyList() else (0 until length()).map { optString(it) }.filter { it.isNotBlank() }
        private fun JSONArray?.objects() = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
    }
}
