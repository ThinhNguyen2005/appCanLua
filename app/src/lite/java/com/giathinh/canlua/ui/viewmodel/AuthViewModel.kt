package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

 data class AuthUiState(
    val isSignedIn: Boolean = false,
    val needsProfileSetup: Boolean? = false
)

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {
    val uiState = kotlinx.coroutines.flow.MutableStateFlow(AuthUiState())
    fun markProfileCompleted() { uiState.value = uiState.value.copy(needsProfileSetup = false) }
}