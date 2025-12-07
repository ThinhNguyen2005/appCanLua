package com.GiaThinh.canlua.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GiaThinh.canlua.repository.AuthManager
import com.GiaThinh.canlua.repository.ProfileRepository
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class AuthUiState(
    val isSignedIn: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val userLabel: String? = null,
    val verificationId: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val firebaseAuth: FirebaseAuth,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isSignedIn = authManager.isAuthenticated,
            userLabel = getUserLabel()
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private fun getUserLabel(): String? {
        val user = authManager.currentUser
        return user?.email ?: user?.phoneNumber ?: user?.uid
    }

    fun signInEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            val result = authManager.signInWithEmailAndPassword(email, password)
            _uiState.value = if (result.isSuccess) {
                AuthUiState(isSignedIn = true, userLabel = getUserLabel())
            } else {
                _uiState.value.copy(loading = false, error = result.exceptionOrNull()?.message ?: "Đăng nhập thất bại")
            }
        }
    }

    fun registerEmail(
        email: String,
        password: String,
        name: String? = null,
        phone: String? = null,
        region: String? = null,
        note: String? = null,
        role: String? = null,
        cccd: String? = null,
        username: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            val result = authManager.createUserWithEmailAndPassword(email, password)
            _uiState.value = if (result.isSuccess) {
                saveProfileLocal(
                    name = name,
                    phone = phone,
                    region = region,
                    note = note,
                    role = role,
                    cccd = cccd,
                    username = username,
                    email = email
                )
                AuthUiState(isSignedIn = true, userLabel = getUserLabel())
            } else {
                _uiState.value.copy(loading = false, error = result.exceptionOrNull()?.message ?: "Đăng ký thất bại")
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            val result = authManager.signInAnonymously()
            _uiState.value = if (result.isSuccess) {
                AuthUiState(isSignedIn = true, info = "Đăng nhập ẩn danh", userLabel = getUserLabel())
            } else {
                _uiState.value.copy(loading = false, error = result.exceptionOrNull()?.message ?: "Đăng nhập thất bại")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            try {
                firebaseAuth.signInWithCredential(credential).await()
                _uiState.value = AuthUiState(isSignedIn = true, userLabel = getUserLabel())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message ?: "Đăng nhập Google thất bại")
            }
        }
    }

    fun sendOtp(activity: Activity, phoneNumber: String) {
        val normalized = normalizePhone(phoneNumber)
        if (normalized == null) {
            _uiState.value = _uiState.value.copy(loading = false, error = "Số điện thoại chưa đúng định dạng +84...")
            return
        }
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                _uiState.value = _uiState.value.copy(loading = false, verificationId = verificationId, info = "Đã gửi mã OTP")
            }
        }

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(normalized)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        _uiState.value = _uiState.value.copy(loading = true, error = null, info = null)
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(code: String) {
        val verificationId = _uiState.value.verificationId ?: return
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneCredential(credential)
    }

    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            try {
                firebaseAuth.signInWithCredential(credential).await()
                _uiState.value = AuthUiState(isSignedIn = true, userLabel = getUserLabel())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message ?: "OTP không hợp lệ")
            }
        }
    }

    fun signOut() {
        authManager.signOut()
        _uiState.value = AuthUiState(isSignedIn = false, info = "Đã đăng xuất")
    }

    private suspend fun saveProfileLocal(
        name: String?,
        phone: String?,
        region: String?,
        note: String?,
        role: String?,
        cccd: String?,
        username: String?,
        email: String?
    ) {
        val safeName = name?.trim().orEmpty()
        if (safeName.isBlank()) return
        profileRepository.saveProfile(
            com.GiaThinh.canlua.data.model.Profile(
                name = safeName,
                phone = phone?.trim().orEmpty(),
                region = region?.trim().orEmpty(),
                note = note?.trim().orEmpty(),
                role = role ?: "FARMER",
                cccd = cccd?.trim().orEmpty(),
                username = username?.trim().orEmpty(),
                email = email?.trim().orEmpty()
            )
        )
    }

    private fun normalizePhone(raw: String): String? {
        val digits = raw.filter { it.isDigit() || it == '+' }
        if (digits.isBlank()) return null
        return when {
            digits.startsWith("+84") && digits.length in 12..13 -> digits
            digits.startsWith("0") && digits.length in 10..11 -> "+84" + digits.drop(1)
            else -> null
        }
    }
}
