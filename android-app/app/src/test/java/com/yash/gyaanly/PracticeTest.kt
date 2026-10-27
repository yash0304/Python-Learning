package com.yash.gyaanly

import com.yash.gyaanly.domain.practice.AiCoach
import com.yash.gyaanly.domain.practice.AiTask
import com.yash.gyaanly.domain.practice.Answer
import com.yash.gyaanly.domain.practice.ChatEngine
import com.yash.gyaanly.domain.practice.ChatMessage
import com.yash.gyaanly.domain.practice.KeyPoint
import com.yash.gyaanly.domain.practice.PracticeParser
import com.yash.gyaanly.domain.practice.ScenarioSource
import com.yash.gyaanly.domain.practice.ScriptParser
import com.yash.gyaanly.domain.practice.SpeechAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PracticeTest {

    private val scenarios = PracticeParser.parseAll(File("src/main/assets/practice/scenarios.json").readText())

    @Test
    fun builtInRolePlaysCoverAllFourCategories() {
        assertEquals(40, scenarios.size)
        assertEquals(scenarios.size, scenarios.map { it.id }.toSet().size)
        listOf("Daily life", "Office", "Interview", "Sales calls").forEach { c -> assertEquals(c, 10, scenarios.count { it.category == c }) }
        scenarios.forEach { s ->
            assertTrue("${s.id}: too few turns", s.turns.size >= 4)
            s.turns.forEach { t ->
                assertTrue("${s.id}: empty line", t.say.isNotBlank())
                assertTrue("${s.id}: no key points", t.points.isNotEmpty())
                assertNotNull("${s.id}: no model answer", t.sample)
            }
        }
    }

    @Test
    fun everyModelAnswerCoversItsOwnKeyPoints() {
        scenarios.forEach { s ->
            s.turns.forEach { t -> t.points.forEach { p -> assertTrue("${s.id}: sample misses '${p.idea}'", SpeechAnalyzer.covers(t.sample!!, p)) } }
        }
    }

    @Test
    fun shortKeywordsMatchWholeWordsOnly() {
        val ai = KeyPoint("mention AI", listOf("ai"))
        assertFalse(SpeechAnalyzer.covers("let me explain the process", ai))
        assertTrue(SpeechAnalyzer.covers("we use AI for matching", ai))
        assertTrue(SpeechAnalyzer.covers("I delivered on time", KeyPoint("on time", listOf("on time"))))
        assertTrue(SpeechAnalyzer.covers("we reduced errors", KeyPoint("result", listOf("reduc"))))
    }

    @Test
    fun analyzerFlagsFillersSlipsAndMissingNextStep() {
        val sales = scenarios.first { it.category == "Sales calls" }
        val answers = sales.turns.map { t ->
            Answer(t.say, "um basically I am having experience of um five years and we will discuss about it since two years", 12.0, 0.9f, t)
        }
        val report = SpeechAnalyzer.analyze(sales, answers)
        val titles = report.shortcomings.map { it.title }
        assertTrue(titles.toString(), titles.any { it.startsWith("Too many filler words") })
        assertTrue(titles.toString(), titles.any { "Discuss about" in it })
        assertTrue(titles.toString(), titles.any { "Since 5 years" in it })
        assertTrue(titles.toString(), titles.any { it == "No clear next step" })
        assertTrue(titles.toString(), titles.any { it == "You didn't ask any questions" })
        assertTrue(report.overall in 0..100)
    }

    @Test
    fun strongAnswersScoreHigherThanWeakOnes() {
        val s = scenarios.first { it.category == "Interview" }
        val good = SpeechAnalyzer.analyze(s, s.turns.map { Answer(it.say, it.sample!!, it.sample!!.split(" ").size / 2.3, 0.92f, it) })
        val weak = SpeechAnalyzer.analyze(s, s.turns.map { Answer(it.say, "um yes", 2.5, 0.5f, it) })
        assertTrue("good ${good.overall} vs weak ${weak.overall}", good.overall > weak.overall + 20)
        assertEquals(100, good.content)
    }

    @Test
    fun pastedScriptBecomesARolePlay() {
        val s = ScriptParser.parse(
            """
            Them: Good morning, thanks for calling Acme Bank. How can I help?
            Me: Hi, I'd like to dispute a duplicate charge on my credit card statement.
            Them: I'm sorry to hear that. Can you tell me the date and amount?
            Me: It was on the fifth of October, for 4,500 rupees.
            Them: Thank you. I've raised a dispute. Anything else?
            """.trimIndent(),
            "Bank call",
        )!!
        assertEquals(ScenarioSource.MY_SCRIPT, s.source)
        assertEquals(2, s.turns.size)
        assertEquals("Thank you. I've raised a dispute. Anything else?", s.closing)
        assertTrue(s.turns[0].sample!!.contains("duplicate charge"))
        assertTrue(s.turns[0].points.isNotEmpty())
        assertNull(ScriptParser.parse("Me: just me talking"))
    }

    private class FakeEngine(val reply: String) : ChatEngine {
        var lastSystem = ""
        var lastMessages = emptyList<ChatMessage>()
        override fun reply(system: String, messages: List<ChatMessage>, task: AiTask): String {
            lastSystem = system; lastMessages = messages; return reply
        }
    }

    @Test
    fun aiPartnerLinesAreCleanedAndEndIsDetected() {
        val engine = FakeEngine("<think>plan</think>**Interviewer:** Thanks, that's all for today. [END]")
        val line = AiCoach(engine).partner(scenarios.first(), listOf(ChatMessage(false, "Hi"), ChatMessage(true, "Hello")))
        assertTrue(line.ended)
        assertEquals("Thanks, that's all for today.", line.text)
        assertTrue(engine.lastMessages.first().fromLearner) // conversations start with a user message
        assertEquals(3, engine.lastMessages.size)
    }

    @Test
    fun aiFeedbackAndScenarioJsonAreParsed() {
        val fb = AiCoach(FakeEngine("```json\n{\"summary\":\"Good\",\"score\":72,\"strengths\":[\"clear\"],\"improvements\":[{\"issue\":\"Fillers\",\"youSaid\":\"um\",\"better\":\"pause\",\"why\":\"sounds unsure\"}],\"phrases\":[\"Let me check\"],\"nextPractice\":\"Pause instead of um\"}\n```"))
            .feedback(scenarios.first(), emptyList(), SpeechAnalyzer.analyze(scenarios.first(), emptyList()))
        assertEquals(72, fb.score)
        assertEquals("pause", fb.improvements.single().better)

        val s = AiCoach(FakeEngine("""{"title":"Pitch","emoji":"📞","partner":"CFO","you":"Seller","goal":"Book a demo","turns":[{"say":"Who is this?","points":[{"idea":"introduce","keywords":["name","calling"]}],"sample":"Hi, my name is Yash."}],"closing":"Bye","focus":["Be brief"]}"""))
            .generate("cold call", "Sales calls")
        assertEquals("Sales calls", s.category)
        assertEquals(ScenarioSource.AI, s.source)
        assertEquals("Who is this?", s.turns.single().say)
    }
}
