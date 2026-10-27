package com.yash.gyaanly.data.ai

import com.yash.gyaanly.domain.practice.AiException
import com.yash.gyaanly.domain.practice.AiTask
import com.yash.gyaanly.domain.practice.ChatEngine
import com.yash.gyaanly.domain.practice.ChatMessage
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sarvam AI chat completions (https://docs.sarvam.ai): POST /v1/chat/completions with the
 * `api-subscription-key` header. Sarvam renames and retires models often, so the model id is a setting.
 */
class SarvamEngine(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    private val baseUrl: String = "https://api.sarvam.ai",
) : ChatEngine {

    override fun reply(system: String, messages: List<ChatMessage>, task: AiTask): String {
        val effort = if (task == AiTask.PARTNER) "low" else "medium"
        val raw = try {
            send(body(system, messages, task, effort))
        } catch (e: HttpError) {
            // Some Sarvam models don't take reasoning_effort; retry once without it.
            if (e.code == 400 && "reasoning" in e.body.lowercase()) {
                try { send(body(system, messages, task, null)) } catch (again: HttpError) { throw again.toAi(model) }
            } else {
                throw e.toAi(model)
            }
        }
        return stripThinking(raw).ifBlank { throw AiException("Sarvam returned an empty reply. Please try again.") }
    }

    private fun body(system: String, messages: List<ChatMessage>, task: AiTask, effort: String?): JSONObject {
        val list = JSONArray().put(JSONObject().put("role", "system").put("content", system))
        messages.forEach { list.put(JSONObject().put("role", if (it.fromLearner) "user" else "assistant").put("content", it.text)) }
        return JSONObject()
            .put("model", model)
            .put("messages", list)
            // Reasoning tokens count against max_tokens on Sarvam's reasoning models, so leave room.
            .put("max_tokens", if (task == AiTask.PARTNER) 2048 else 6000)
            .apply { if (effort != null) put("reasoning_effort", effort) }
    }

    private fun send(body: JSONObject): String {
        val conn = try {
            (URL("$baseUrl/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 120_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("api-subscription-key", apiKey)
            }
        } catch (e: IOException) {
            throw AiException("Couldn't reach Sarvam. Check your internet connection.", e)
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw HttpError(code, text)
            val message = JSONObject(text).getJSONArray("choices").getJSONObject(0).getJSONObject("message")
            return message.optString("content")
        } catch (e: IOException) {
            throw AiException("Couldn't reach Sarvam. Check your internet connection.", e)
        } finally {
            conn.disconnect()
        }
    }

    private class HttpError(val code: Int, val body: String) : Exception("HTTP $code") {
        fun toAi(model: String): AiException {
            val detail = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
            return AiException(
                when (code) {
                    401, 403 -> "Sarvam rejected the API key. Check it in Settings."
                    402 -> "Your Sarvam credits seem to be used up."
                    429 -> "Sarvam is rate-limiting requests. Wait a moment and try again."
                    404 -> "Sarvam doesn't recognise the model \"$model\". Update the model name in Settings."
                    else -> "Sarvam error $code${if (detail.isNotBlank()) ": $detail" else ""}"
                },
            )
        }
    }

    private fun stripThinking(text: String) = text.replace(Regex("(?s)<think>.*?</think>"), "").trim()

    companion object {
        const val DEFAULT_MODEL = "sarvam-105b"
    }
}
