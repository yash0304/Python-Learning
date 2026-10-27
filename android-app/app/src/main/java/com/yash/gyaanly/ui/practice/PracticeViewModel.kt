package com.yash.gyaanly.ui.practice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yash.gyaanly.GyaanlyApp
import com.yash.gyaanly.data.AiSettings
import com.yash.gyaanly.data.PracticeResult
import com.yash.gyaanly.domain.practice.AiCoach
import com.yash.gyaanly.domain.practice.AiException
import com.yash.gyaanly.domain.practice.Scenario
import com.yash.gyaanly.domain.practice.ScriptParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Scenarios, your pasted scripts, AI generation and practice history for the Talk tab. */
class PracticeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GyaanlyApp
    private val store = app.practice

    val builtIn: List<Scenario> = app.scenarios
    val ai: StateFlow<AiSettings> = store.ai
    val history: StateFlow<List<PracticeResult>> = store.history
    val scripts: StateFlow<List<Pair<String, String>>> = store.scripts

    // Scenarios made in this session (AI-generated); scripts are rebuilt from their saved text.
    private val generated = mutableMapOf<String, Scenario>()

    fun scenario(id: String): Scenario? =
        builtIn.firstOrNull { it.id == id } ?: generated[id] ?: myScripts().firstOrNull { it.id == id }

    fun myScripts(): List<Scenario> = scripts.value.mapNotNull { (title, text) -> ScriptParser.parse(text, title) }

    /** Parses and saves a pasted script; returns its id, or null if it had no lines for the partner. */
    fun addScript(title: String, text: String): String? {
        val s = ScriptParser.parse(text, title.ifBlank { "My script" }) ?: return null
        store.saveScript(s.title, text)
        return s.id
    }

    fun deleteScript(title: String) = store.deleteScript(title)

    fun coach(): AiCoach? = ai.value.engine()?.let(::AiCoach)

    /** Asks the AI for a new role-play; calls back on the main thread with its id or an error message. */
    fun generate(description: String, category: String, done: (Result<String>) -> Unit) {
        val coach = coach() ?: return done(Result.failure(AiException("Add an AI key in Settings first.")))
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { coach.generate(description, category) } }
            done(result.map { s -> generated[s.id] = s; s.id })
        }
    }

    fun updateAi(transform: (AiSettings) -> AiSettings) = store.updateAi(transform)

    fun saveResult(s: Scenario, score: Int) = store.addResult(PracticeResult(LocalDate.now().toString(), s.title, s.category, score))
}
