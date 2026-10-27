package com.yash.gyaanly.data.ai

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.NotFoundException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.yash.gyaanly.domain.practice.AiException
import com.yash.gyaanly.domain.practice.AiTask
import com.yash.gyaanly.domain.practice.ChatEngine
import com.yash.gyaanly.domain.practice.ChatMessage
import kotlin.jvm.optionals.getOrNull

/** Claude via the official Anthropic Java SDK. */
class ClaudeEngine(apiKey: String, private val model: String = DEFAULT_MODEL) : ChatEngine {

    private val client: AnthropicClient = AnthropicOkHttpClient.builder().apiKey(apiKey).build()

    override fun reply(system: String, messages: List<ChatMessage>, task: AiTask): String {
        val params = MessageCreateParams.builder()
            .model(model)
            // Thinking is always on for this model; effort keeps conversation turns quick and feedback thorough.
            .maxTokens(if (task == AiTask.PARTNER) 4000L else 16000L)
            .outputConfig(OutputConfig.builder().effort(if (task == AiTask.PARTNER) OutputConfig.Effort.LOW else OutputConfig.Effort.MEDIUM).build())
            .system(system)
            // If a safety classifier declines, let the API retry on the model it recommends.
            .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
            .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
            .apply { messages.forEach { if (it.fromLearner) addUserMessage(it.text) else addAssistantMessage(it.text) } }
            .build()
        val response = try {
            client.messages().create(params)
        } catch (e: UnauthorizedException) {
            throw AiException("Claude rejected the API key. Check it in Settings.", e)
        } catch (e: PermissionDeniedException) {
            throw AiException("This API key can't use $model.", e)
        } catch (e: NotFoundException) {
            throw AiException("Claude doesn't recognise the model \"$model\".", e)
        } catch (e: RateLimitException) {
            throw AiException("Claude is rate-limiting requests. Wait a moment and try again.", e)
        } catch (e: AnthropicServiceException) {
            throw AiException("Claude error ${e.statusCode()}. Please try again.", e)
        } catch (e: AnthropicIoException) {
            throw AiException("Couldn't reach Claude. Check your internet connection.", e)
        }
        if (response.stopReason().getOrNull() == StopReason.REFUSAL) {
            throw AiException("Claude declined this request. Try rewording the scenario.")
        }
        return response.content().mapNotNull { it.text().getOrNull()?.text() }.joinToString("").trim()
            .ifBlank { throw AiException("Claude returned an empty reply. Please try again.") }
    }

    companion object {
        const val DEFAULT_MODEL = "claude-opus-5-5"
    }
}
