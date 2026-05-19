package com.GiaThinh.canlua.data.remote.ai

/**
 * OpenRouter chat completion request — OpenAI-compatible.
 * Doc: https://openrouter.ai/docs
 */
data class OpenRouterRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.6,
    val max_tokens: Int = 800
)

data class ChatMessage(val role: String, val content: String)

data class OpenRouterResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<Choice> = emptyList(),
    val error: OpenRouterError? = null
) {
    data class Choice(val message: ChatMessage? = null, val finish_reason: String? = null)
    data class OpenRouterError(val message: String? = null, val code: Int? = null)
}
