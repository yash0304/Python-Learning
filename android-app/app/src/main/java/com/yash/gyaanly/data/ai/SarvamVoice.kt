package com.yash.gyaanly.data.ai

import com.yash.gyaanly.domain.practice.AiException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * Sarvam AI Bulbul text-to-speech (POST /text-to-speech, `api-subscription-key` header): natural
 * Indian voices for the conversation partner. Returns the decoded audio file bytes (WAV by default).
 */
class SarvamVoice(
    private val apiKey: String,
    private val speaker: String = DEFAULT_SPEAKER,
    private val pace: Double = 1.0,
    private val model: String = DEFAULT_MODEL,
    private val baseUrl: String = "https://api.sarvam.ai",
) {
    /** The docs have used both names for the language field; remember which one this account accepts. */
    @Volatile private var languageField = "target_language_code"

    /** Blocking: call off the main thread. */
    fun synthesize(text: String, languageCode: String = "en-IN"): ByteArray {
        val clipped = text.take(MAX_CHARS)
        return try {
            send(clipped, languageCode)
        } catch (e: HttpError) {
            if (e.code in 400..422 && "language" in e.body.lowercase()) {
                languageField = if (languageField == "target_language_code") "language_code" else "target_language_code"
                try { send(clipped, languageCode) } catch (again: HttpError) { throw again.toAi() }
            } else {
                throw e.toAi()
            }
        }
    }

    private fun send(text: String, languageCode: String): ByteArray {
        val body = JSONObject()
            .put("text", text)
            .put(languageField, languageCode)
            .put("speaker", speaker)
            .put("model", model)
            .put("pace", pace)
        val conn = try {
            (URL("$baseUrl/text-to-speech").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 45_000
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
            val reply = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw HttpError(code, reply)
            val audios = JSONObject(reply).optJSONArray("audios")
            val b64 = audios?.optString(0).orEmpty()
            if (b64.isBlank()) throw AiException("Sarvam returned no audio.")
            return Base64.getDecoder().decode(b64)
        } catch (e: IOException) {
            throw AiException("Couldn't reach Sarvam. Check your internet connection.", e)
        } finally {
            conn.disconnect()
        }
    }

    private inner class HttpError(val code: Int, val body: String) : Exception("HTTP $code") {
        fun toAi(): AiException {
            val detail = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
            return AiException(
                when (code) {
                    401, 403 -> "Sarvam rejected the API key. Check it in Settings."
                    402 -> "Your Sarvam credits seem to be used up."
                    429 -> "Sarvam is rate-limiting requests."
                    else -> "Sarvam voice error $code${if (detail.isNotBlank()) ": $detail" else ""}. Check the voice name (\"$speaker\") in Settings."
                },
            )
        }
    }

    companion object {
        const val DEFAULT_MODEL = "bulbul:v3"
        const val DEFAULT_SPEAKER = "shubh"
        const val MAX_CHARS = 2500
    }
}
