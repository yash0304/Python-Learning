package com.yash.gyaanly.data

import android.content.Context
import com.yash.gyaanly.data.ai.ClaudeEngine
import com.yash.gyaanly.data.ai.SarvamEngine
import com.yash.gyaanly.data.ai.SarvamSpeech
import com.yash.gyaanly.data.ai.SarvamVoice
import com.yash.gyaanly.domain.practice.ChatEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class AiProvider(val label: String) { OFF("Off (offline only)"), SARVAM("Sarvam AI"), CLAUDE("Claude") }

data class AiSettings(
    val provider: AiProvider = AiProvider.OFF,
    val sarvamKey: String = "",
    val sarvamModel: String = SarvamEngine.DEFAULT_MODEL,
    val claudeKey: String = "",
    /** Use Sarvam speech-to-text instead of the phone's recogniser (better with Indian accents). */
    val sarvamSpeech: Boolean = false,
    val sttModel: String = SarvamSpeech.DEFAULT_MODEL,
    /** Partner speaks with a Sarvam Bulbul voice instead of the phone's text-to-speech. */
    val bulbul: Boolean = false,
    val bulbulSpeaker: String = SarvamVoice.DEFAULT_SPEAKER,
    val bulbulPace: Double = 1.0,
) {
    fun voice(): SarvamVoice? =
        if (bulbul && sarvamKey.isNotBlank()) SarvamVoice(sarvamKey.trim(), bulbulSpeaker.trim().ifBlank { SarvamVoice.DEFAULT_SPEAKER }, bulbulPace) else null

    val speechReady: Boolean get() = sarvamSpeech && sarvamKey.isNotBlank()

    fun speech(): SarvamSpeech? = if (speechReady) SarvamSpeech(sarvamKey.trim(), sttModel.trim().ifBlank { SarvamSpeech.DEFAULT_MODEL }) else null

    val ready: Boolean get() = when (provider) {
        AiProvider.OFF -> false
        AiProvider.SARVAM -> sarvamKey.isNotBlank()
        AiProvider.CLAUDE -> claudeKey.isNotBlank()
    }

    fun engine(): ChatEngine? = when {
        !ready -> null
        provider == AiProvider.SARVAM -> SarvamEngine(sarvamKey.trim(), sarvamModel.trim().ifBlank { SarvamEngine.DEFAULT_MODEL })
        else -> ClaudeEngine(claudeKey.trim())
    }
}

/** One finished practice session, kept so you can see your speaking scores improve. */
data class PracticeResult(val date: String, val title: String, val category: String, val score: Int)

/**
 * Speaking-practice settings and history, in their own SharedPreferences file. API keys stay on
 * this phone: they are never written to progress backups.
 */
class PracticeStore(context: Context) {
    private val prefs = context.getSharedPreferences("practice", Context.MODE_PRIVATE)

    private val _ai = MutableStateFlow(loadAi())
    val ai: StateFlow<AiSettings> = _ai.asStateFlow()

    private val _history = MutableStateFlow(loadHistory())
    val history: StateFlow<List<PracticeResult>> = _history.asStateFlow()

    private val _scripts = MutableStateFlow(loadScripts())
    /** Scripts you pasted, as (title, text). */
    val scripts: StateFlow<List<Pair<String, String>>> = _scripts.asStateFlow()

    fun updateAi(transform: (AiSettings) -> AiSettings) {
        val next = transform(_ai.value)
        prefs.edit()
            .putString("ai_provider", next.provider.name)
            .putString("sarvam_key", next.sarvamKey)
            .putString("sarvam_model", next.sarvamModel)
            .putString("claude_key", next.claudeKey)
            .putBoolean("sarvam_speech", next.sarvamSpeech)
            .putString("stt_model", next.sttModel)
            .putBoolean("bulbul", next.bulbul)
            .putString("bulbul_speaker", next.bulbulSpeaker)
            .putFloat("bulbul_pace", next.bulbulPace.toFloat())
            .apply()
        _ai.value = next
    }

    fun addResult(r: PracticeResult) {
        val next = (listOf(r) + _history.value).take(MAX_HISTORY)
        val arr = JSONArray()
        next.forEach { arr.put(JSONObject().put("date", it.date).put("title", it.title).put("category", it.category).put("score", it.score)) }
        prefs.edit().putString("history", arr.toString()).apply()
        _history.value = next
    }

    fun saveScript(title: String, text: String) {
        val next = (listOf(title to text) + _scripts.value.filter { it.first != title }).take(MAX_SCRIPTS)
        writeScripts(next)
    }

    fun deleteScript(title: String) = writeScripts(_scripts.value.filter { it.first != title })

    private fun writeScripts(list: List<Pair<String, String>>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("title", it.first).put("text", it.second)) }
        prefs.edit().putString("scripts", arr.toString()).apply()
        _scripts.value = list
    }

    private fun loadAi() = AiSettings(
        provider = runCatching { AiProvider.valueOf(prefs.getString("ai_provider", "OFF")!!) }.getOrDefault(AiProvider.OFF),
        sarvamKey = prefs.getString("sarvam_key", "")!!,
        sarvamModel = prefs.getString("sarvam_model", SarvamEngine.DEFAULT_MODEL)!!,
        claudeKey = prefs.getString("claude_key", "")!!,
        sarvamSpeech = prefs.getBoolean("sarvam_speech", false),
        sttModel = prefs.getString("stt_model", SarvamSpeech.DEFAULT_MODEL)!!,
        bulbul = prefs.getBoolean("bulbul", false),
        bulbulSpeaker = prefs.getString("bulbul_speaker", SarvamVoice.DEFAULT_SPEAKER)!!,
        bulbulPace = prefs.getFloat("bulbul_pace", 1.0f).toDouble(),
    )

    private fun loadHistory(): List<PracticeResult> = runCatching {
        val arr = JSONArray(prefs.getString("history", "[]"))
        (0 until arr.length()).map { arr.getJSONObject(it) }.map {
            PracticeResult(it.getString("date"), it.getString("title"), it.getString("category"), it.getInt("score"))
        }
    }.getOrDefault(emptyList())

    private fun loadScripts(): List<Pair<String, String>> = runCatching {
        val arr = JSONArray(prefs.getString("scripts", "[]"))
        (0 until arr.length()).map { arr.getJSONObject(it) }.map { it.getString("title") to it.getString("text") }
    }.getOrDefault(emptyList())

    private companion object {
        const val MAX_HISTORY = 60
        const val MAX_SCRIPTS = 30
    }
}
