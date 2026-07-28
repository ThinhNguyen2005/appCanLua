package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.data.firestore.FirestoreFeedback
import com.giathinh.canlua.repository.FeedbackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedbackUiState(
    val isSending: Boolean = false,
    val sendSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val feedbackRepository: FeedbackRepository
) : ViewModel() {

    val feedbacks: StateFlow<List<FirestoreFeedback>> = feedbackRepository.observeMyFeedbacks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unreadCount: StateFlow<Int> = feedbackRepository.observeUnreadReplyCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun sendFeedback(messageText: String, onSuccess: () -> Unit) {
        if (messageText.isBlank()) {
            _uiState.value = FeedbackUiState(error = "Vui lòng nhập nội dung phản hồi")
            return
        }

        _uiState.value = FeedbackUiState(isSending = true)
        viewModelScope.launch {
            val result = feedbackRepository.sendFeedback(messageText)
            if (result.isSuccess) {
                _uiState.value = FeedbackUiState(sendSuccess = true)
                onSuccess()
            } else {
                _uiState.value = FeedbackUiState(
                    error = result.exceptionOrNull()?.message ?: "Lỗi gửi phản hồi"
                )
            }
        }
    }

    fun markAsRead() {
        viewModelScope.launch {
            feedbackRepository.markFeedbacksAsRead()
        }
    }
}
