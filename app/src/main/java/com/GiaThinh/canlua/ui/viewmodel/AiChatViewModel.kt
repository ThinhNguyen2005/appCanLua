package com.GiaThinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GiaThinh.canlua.data.model.ChatSession
import com.GiaThinh.canlua.data.model.Profile
import com.GiaThinh.canlua.data.model.RicePrice
import com.GiaThinh.canlua.data.model.WeatherInfo
import com.GiaThinh.canlua.data.remote.ai.ChatMessage
import com.GiaThinh.canlua.repository.AiChatRepository
import com.GiaThinh.canlua.repository.ChatSessionStore
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
    val sessions: List<ChatSession> = emptyList(),
    val currentSessionId: String? = null
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiChatRepository: AiChatRepository,
    marketRepository: MarketRepository,
    profileRepository: ProfileRepository,
    weatherRepository: WeatherRepository,
    private val knowledgeBase: KnowledgeBaseRepository,
    private val stt: SpeechRecognizerHelper,
    private val sessionStore: ChatSessionStore
) : ViewModel() {

    private val _state = MutableStateFlow(AiChatUiState())
    val state: StateFlow<AiChatUiState> = _state.asStateFlow()

    val voiceState: StateFlow<VoiceState> = combine(
        stt.state,
        stt.partialText,
        stt.errorMessage
    ) { s, partial, err ->
        VoiceState(state = s, partial = partial, error = err, available = stt.isAvailable)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = VoiceState(available = stt.isAvailable)
    )

    /** Cache giá lúa làm context RAG. Tự cập nhật khi Room đổi. */
    private val ricePrices: StateFlow<List<RicePrice>> = marketRepository.getAllPrices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    /** Hồ sơ user (Room). */
    private val profile: StateFlow<Profile?> = profileRepository.latestProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
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
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    init {
        if (sessionStore.sessions.value.isEmpty()) {
            sessionStore.createNew(welcome = welcomeMessages())
        } else if (sessionStore.currentId.value == null) {
            sessionStore.sessions.value.firstOrNull()?.let { sessionStore.switchTo(it.id) }
        }

        viewModelScope.launch {
            combine(sessionStore.sessions, sessionStore.currentId) { list, id ->
                list to id
            }.collect { (list, id) ->
                val current = list.find { it.id == id }
                _state.value = _state.value.copy(
                    sessions = list,
                    currentSessionId = id,
                    messages = current?.messages ?: emptyList()
                )
            }
        }
    }

    fun onInputChange(text: String) {
        _state.value = _state.value.copy(input = text, errorMessage = null)
    }

    fun send() {
        val text = _state.value.input.trim()
        val sessionId = _state.value.currentSessionId ?: return
        if (text.isEmpty() || _state.value.isStreaming) return

        sessionStore.renameTitleIfNeeded(sessionId, text)

        val userMsg = UiMessage(role = "user", content = text)
        sessionStore.appendMessage(sessionId, userMsg)

        _state.value = _state.value.copy(
            input = "",
            isStreaming = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val history = sessionStore.getSession(sessionId)?.messages
                ?.filter { !it.isError && (it.role == "user" || it.role == "assistant") }
                ?.map { ChatMessage(role = it.role, content = it.content) }
                .orEmpty()

            // Tra knowledge base theo câu user vừa gõ.
            val kbHits = knowledgeBase.search(text, maxResults = 2)

            val result = aiChatRepository.chat(
                history = history,
                profile = profile.value,
                weather = weather.value,
                ricePrices = ricePrices.value,
                knowledgeHits = kbHits
            )
            val reply = if (result.isSuccess) {
                UiMessage(role = "assistant", content = result.getOrNull().orEmpty())
            } else {
                UiMessage(
                    role = "assistant",
                    content = "Lỗi: ${result.exceptionOrNull()?.message ?: "Không phản hồi"}",
                    isError = true
                )
            }
            sessionStore.appendMessage(sessionId, reply)
            _state.value = _state.value.copy(isStreaming = false)
        }
    }

    fun usePresetPrompt(prompt: String) {
        _state.value = _state.value.copy(input = prompt)
        send()
    }

    fun newSession() {
        sessionStore.createNew(welcome = welcomeMessages())
        _state.value = _state.value.copy(input = "")
    }

    fun switchSession(id: String) {
        sessionStore.switchTo(id)
        _state.value = _state.value.copy(input = "")
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

    private fun welcomeMessages(): List<UiMessage> = listOf(
        UiMessage(
            role = "assistant",
            content = "Xin chào! Tôi là Trợ Lý Khuyến Nông của bạn. " +
                "Tôi đã biết tên, vị trí và thời tiết hiện tại của bạn nên có thể tư vấn sát hơn. " +
                "Hãy hỏi tôi về **giá lúa**, sâu bệnh, lịch bón phân, hoặc kỹ thuật canh tác."
        )
    )
}
