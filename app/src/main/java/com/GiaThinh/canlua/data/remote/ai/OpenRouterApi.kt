package com.GiaThinh.canlua.data.remote.ai

/**
 * OpenRouter chat completion request — OpenAI-compatible.
 * Doc: https://openrouter.ai/docs
 */
data class OpenRouterRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.6,
    // Tăng từ 800 → 1500 vì system prompt giờ dài (RAG + KB + weather + prices)
    // và output Markdown 4 mục (analyzeSeason) dễ vượt 800 → bị cắt giữa câu.
    val max_tokens: Int = 1500
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
