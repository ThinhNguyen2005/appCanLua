package com.GiaThinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GiaThinh.canlua.data.model.Profile
import com.GiaThinh.canlua.data.model.RicePrice
import com.GiaThinh.canlua.data.model.WeatherInfo
import com.GiaThinh.canlua.data.remote.ai.ChatMessage
import com.GiaThinh.canlua.repository.AiChatRepository
import com.GiaThinh.canlua.repository.FirestoreRepository
import com.GiaThinh.canlua.repository.KnowledgeBaseRepository
import com.GiaThinh.canlua.repository.MarketRepository
import com.GiaThinh.canlua.repository.ProfileRepository
import com.GiaThinh.canlua.repository.WeatherRepository
import com.GiaThinh.canlua.repository.WeatherState
import com.GiaThinh.canlua.util.SpeechRecognizerHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tin nhắn hiển thị trên UI.
 *
 * `id` được sinh bằng UUID để tránh collision khi 2 message gửi cùng millisecond.
 */
data class UiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String,            // "user" hoặc "assistant"
    val content: String,
    val isError: Boolean = false
)

data class VoiceState(
    val state: SpeechRecognizerHelper.State = SpeechRecognizerHelper.State.IDLE,
    val partial: String = "",
    val error: String? = null,
    val available: Boolean = true
)

data class AiChatUiState(
    val messages: List<UiMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val input: String = "",
    val errorMessage: String? = null,
    val rateLimitSeconds: Int = 0
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiChatRepository: AiChatRepository,
    private val firestoreRepository: FirestoreRepository,
    marketRepository: MarketRepository,
    profileRepository: ProfileRepository,
    weatherRepository: WeatherRepository,
    private val knowledgeBase: KnowledgeBaseRepository,
    private val stt: SpeechRecognizerHelper
) : ViewModel() {

    private val _state = MutableStateFlow(AiChatUiState())
    val state: StateFlow<AiChatUiState> = _state.asStateFlow()

    /** Rate limiting: max 10 messages per 60 seconds window, min 1s between sends. */
    private val rateLimitMs = 1_000L
    private val windowMs = 60_000L
    private val maxMessages = 10
    private val recentTimestamps = mutableListOf<Long>()
    private var lastSentTime = 0L

    private fun canSend(): Boolean {
        val now = System.currentTimeMillis()
        recentTimestamps.removeAll { now - it > windowMs }
        val withinBurst = recentTimestamps.size < maxMessages
        val withinThrottle = now - lastSentTime >= rateLimitMs
        return withinBurst && withinThrottle
    }

    private fun recordSend() {
        val now = System.currentTimeMillis()
        recentTimestamps.add(now)
        lastSentTime = now
    }

    val voiceState: StateFlow<VoiceState> = combine(
        stt.state,
        stt.partialText,
        stt.errorMessage
    ) { s, partial, err ->
        VoiceState(state = s, partial = partial, error = err, available = stt.isAvailable)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VoiceState(available = stt.isAvailable)
    )

    /** Cache giá lúa làm context RAG. Tự cập nhật khi Room đổi. */
    private val ricePrices: StateFlow<List<RicePrice>> = marketRepository.getAllPrices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Hồ sơ user (Room). */
    private val profile: StateFlow<Profile?> = profileRepository.latestProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /**
     * Snapshot thời tiết hiện tại — chỉ map từ Data state, ignore Loading/Error
     * để giữ giá trị cũ làm context khi mạng tạm gián đoạn.
     */
    private val weather: StateFlow<WeatherInfo?> = weatherRepository.observeWeather()
        .map { st -> if (st is WeatherState.Data) st.info else null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            profile.collect { prof ->
                if (_state.value.messages.isEmpty()) {
                    val audience = if (prof?.role.equals("TRADER", ignoreCase = true))
                        KnowledgeBaseRepository.Audience.TRADER
                    else
                        KnowledgeBaseRepository.Audience.FARMER
                    _state.value = _state.value.copy(
                        messages = welcomeMessages(audience)
                    )
                }
            }
        }
    }

    private fun currentAudience(): KnowledgeBaseRepository.Audience =
        if (profile.value?.role.equals("TRADER", ignoreCase = true))
            KnowledgeBaseRepository.Audience.TRADER
        else
            KnowledgeBaseRepository.Audience.FARMER

    fun onInputChange(text: String) {
        _state.value = _state.value.copy(input = text, errorMessage = null)
    }

    fun send() {
        val text = _state.value.input.trim()
        if (text.isEmpty() || _state.value.isStreaming) return

        if (!canSend()) {
            val cooldownSec = ((rateLimitMs - (System.currentTimeMillis() - lastSentTime)) / 1000).toInt().coerceAtLeast(1)
            _state.value = _state.value.copy(rateLimitSeconds = cooldownSec.coerceAtLeast(1))
            viewModelScope.launch {
                repeat(cooldownSec.coerceAtLeast(1)) { i ->
                    delay(1_000)
                    _state.value = _state.value.copy(rateLimitSeconds = cooldownSec - i - 1)
                }
                _state.value = _state.value.copy(rateLimitSeconds = 0)
            }
            return
        }
        recordSend()

        val userMsg = UiMessage(role = "user", content = text)
        val currentMessages = _state.value.messages + userMsg

        _state.value = _state.value.copy(
            input = "",
            messages = currentMessages,
            isStreaming = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val history = currentMessages
                .filter { !it.isError && (it.role == "user" || it.role == "assistant") }
                .map { ChatMessage(role = it.role, content = it.content) }

            val audience = currentAudience()
            val kbHits = knowledgeBase.search(text, audience = audience, maxResults = 2)

            val result = aiChatRepository.chat(
                history = history,
                profile = profile.value,
                weather = weather.value,
                ricePrices = ricePrices.value,
                knowledgeHits = kbHits,
                audience = audience
            )
            val reply = if (result.isSuccess) {
                firestoreRepository.incrementAiQueryCount()
                UiMessage(role = "assistant", content = result.getOrNull().orEmpty())
            } else {
                // Message từ repository đã là câu tiếng Việt gần gũi — hiển thị trực tiếp.
                UiMessage(
                    role = "assistant",
                    content = result.exceptionOrNull()?.message ?: "AI tạm không trả lời được. Bà con thử lại sau.",
                    isError = true
                )
            }
            _state.value = _state.value.copy(
                messages = _state.value.messages + reply,
                isStreaming = false
            )
        }
    }

    fun usePresetPrompt(prompt: String) {
        _state.value = _state.value.copy(input = prompt)
        send()
    }

    // === Voice ===

    fun startVoice() {
        stt.start { recognized ->
            val current = _state.value.input
            val merged = if (current.isBlank()) recognized else "$current $recognized"
            _state.value = _state.value.copy(input = merged)
        }
    }

    fun stopVoice() { stt.stop() }
    fun cancelVoice() { stt.cancel() }
    fun clearVoiceError() { stt.clearError() }

    override fun onCleared() {
        stt.release()
        super.onCleared()
    }

    private fun welcomeMessages(
        audience: KnowledgeBaseRepository.Audience = KnowledgeBaseRepository.Audience.FARMER
    ): List<UiMessage> {
        val content = when (audience) {
            KnowledgeBaseRepository.Audience.TRADER ->
                "Xin chào! Tôi là **Chuyên Gia Thị Trường Lúa Gạo** của bạn. " +
                    "Hỏi tôi về **giá thu mua**, kiểm định ẩm/tạp chất, logistics sà lan, biên lợi nhuận, " +
                    "hay cách đàm phán giao dịch với nông dân."
            KnowledgeBaseRepository.Audience.FARMER ->
                "Xin chào! Tôi là Trợ Lý Nông nghiệp của bạn. " +
                    "Tôi đã biết tên, vị trí và thời tiết hiện tại của bạn nên có thể tư vấn sát hơn. " +
                    "Hãy hỏi tôi về **giá lúa**, sâu bệnh, lịch bón phân, hoặc kỹ thuật canh tác."
        }
        return listOf(UiMessage(role = "assistant", content = content))
    }
}
