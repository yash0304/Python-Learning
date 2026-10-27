package com.yash.gyaanly.data.ai

import com.yash.gyaanly.domain.practice.AiException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * Sarvam AI speech-to-text (POST /speech-to-text, multipart, `api-subscription-key` header). Trained
 * on Indian speech, so it usually understands Indian-accented English better than the phone's
 * recogniser. The REST endpoint takes up to 30 seconds per file, so long answers are sent in pieces.
 */
class SarvamSpeech(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    private val baseUrl: String = "https://api.sarvam.ai",
) {

    /** Transcribes 16 kHz mono 16-bit PCM. Blocking: call it off the main thread. */
    fun transcribe(pcm: ShortArray, sampleRate: Int = 16_000, languageCode: String = "en-IN"): String {
        if (pcm.isEmpty()) return ""
        val chunk = sampleRate * MAX_CHUNK_SECONDS
        return (pcm.indices step chunk).joinToString(" ") { start ->
            send(wav(pcm.copyOfRange(start, minOf(start + chunk, pcm.size)), sampleRate), languageCode)
        }.replace(Regex("\\s+"), " ").trim()
    }

    private fun send(wav: ByteArray, languageCode: String): String {
        val boundary = "gyaanly-" + UUID.randomUUID()
        val body = ByteArrayOutputStream().apply {
            fun field(name: String, value: String) =
                write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n".toByteArray())
            field("model", model)
            field("language_code", languageCode)
            // "mode" is documented for the saaras:v3 family only.
            if (model.startsWith("saaras:v3")) field("mode", "transcribe")
            write("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"answer.wav\"\r\nContent-Type: audio/wav\r\n\r\n".toByteArray())
            write(wav)
            write("\r\n--$boundary--\r\n".toByteArray())
        }.toByteArray()

        val conn = try {
            (URL("$baseUrl/speech-to-text").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("api-subscription-key", apiKey)
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }
        } catch (e: IOException) {
            throw AiException("Couldn't reach Sarvam. Check your internet connection.", e)
        }
        try {
            conn.outputStream.use { it.write(body) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val detail = runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
                throw AiException(
                    when (code) {
                        401, 403 -> "Sarvam rejected the API key. Check it in Settings."
                        402 -> "Your Sarvam credits seem to be used up."
                        429 -> "Sarvam is rate-limiting requests. Wait a moment and try again."
                        else -> "Sarvam speech-to-text error $code${if (detail.isNotBlank()) ": $detail" else ""}. Check the model name (\"$model\") in Settings."
                    },
                )
            }
            return JSONObject(text).optString("transcript")
        } catch (e: IOException) {
            throw AiException("Couldn't reach Sarvam. Check your internet connection.", e)
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        const val DEFAULT_MODEL = "saaras:v3"
        const val MAX_CHUNK_SECONDS = 28

        /** A 16-bit mono PCM WAV file. */
        fun wav(pcm: ShortArray, sampleRate: Int): ByteArray {
            val data = pcm.size * 2
            return ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray()); putInt(36 + data); put("WAVE".toByteArray())
                put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
                putInt(sampleRate); putInt(sampleRate * 2); putShort(2); putShort(16)
                put("data".toByteArray()); putInt(data)
                pcm.forEach { putShort(it) }
            }.array()
        }
    }
}
