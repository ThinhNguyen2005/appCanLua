package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.repository.RoleRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoleRequestUiState(
    val submitting: Boolean = false,
    val error: String? = null,
    val justSubmitted: Boolean = false
)

@HiltViewModel
class RoleRequestViewModel @Inject constructor(
    private val repo: RoleRequestRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(RoleRequestUiState())
    val ui: StateFlow<RoleRequestUiState> = _ui.asStateFlow()

    /** Trạng thái request hiện có của user — null nếu chưa từng submit. */
    val myRequest: StateFlow<RoleRequestRepository.RoleRequest?> = repo.observeMyRequest()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun submit(businessName: String, taxId: String, phone: String, reason: String) {
        if (_ui.value.submitting) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(submitting = true, error = null, justSubmitted = false)
            val r = repo.submit(
                businessName = businessName.trim(),
                taxId = taxId.trim(),
                phone = phone.trim(),
                reason = reason.trim()
            )
            _ui.value = if (r.isSuccess) {
                RoleRequestUiState(justSubmitted = true)
            } else {
                _ui.value.copy(
                    submitting = false,
                    error = r.exceptionOrNull()?.message ?: "Gửi yêu cầu thất bại"
                )
            }
        }
    }

    fun clearError() { _ui.value = _ui.value.copy(error = null) }
}
