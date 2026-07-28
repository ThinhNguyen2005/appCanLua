package com.giathinh.canlua.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.auth.GoogleAccount
import com.giathinh.canlua.repository.AuthManager
import com.giathinh.canlua.repository.ProfileRepository
import com.giathinh.canlua.util.AnalyticsHelper
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val verificationId: String? = null,
    /**
     * `true` nếu user đã sign-in nhưng chưa có Profile (name blank trong Room DB).
     * `null` = chưa xác định — vẫn đang load. UI nên chờ thay vì điều hướng vội.
     */
    val needsProfileSetup: Boolean? = null
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

    init {
        // Subscribe vào FirebaseAuth state — đồng bộ TẤT CẢ instance của AuthViewModel
        // (mỗi NavBackStackEntry tạo instance mới theo Hilt) qua source-of-truth duy nhất.
        viewModelScope.launch {
            authManager.authStateFlow.collect { user ->
                val signedIn = user != null
                if (_uiState.value.isSignedIn != signedIn) {
                    _uiState.value = _uiState.value.copy(
                        isSignedIn = signedIn,
                        userLabel = getUserLabel(),
                        // Reset profile flag để đẩy lại đúng route sau khi state đổi.
                        needsProfileSetup = if (signedIn) null else false
                    )
                    if (signedIn) refreshProfileSetupFlag()
                }
            }
        }

        // Nếu user đã đăng nhập sẵn (cold start), load profile để quyết định route.
        if (authManager.isAuthenticated) {
            refreshProfileSetupFlag()
        } else {
            // Chưa sign-in — không cần setup, vào login luôn.
            _uiState.value = _uiState.value.copy(needsProfileSetup = false)
        }
    }

    /**
     * Đọc Profile của user hiện tại để quyết định user có cần màn hình ProfileSetup hay không.
     * Nếu Room chưa có dữ liệu, repository sẽ thử khôi phục từ Firestore trước khi kết luận setup mới.
     */
    private fun refreshProfileSetupFlag() {
        viewModelScope.launch {
            val profile = profileRepository.ensureCurrentProfile()
            val needsSetup = profile?.name?.isBlank() ?: true
            _uiState.value = _uiState.value.copy(needsProfileSetup = needsSetup)
            // Telemetry: gắn user identity vào Analytics + Crashlytics. Sau bước này
            // mọi event/non-fatal gắn kèm uid + role để filter trong console.
            AnalyticsHelper.setUser(authManager.currentUser?.uid, profile?.role)
        }
    }

    /**
     * Gọi từ ProfileSetupScreen sau khi user nhấn "Hoàn tất thiết lập".
     * Refresh flag → LaunchedEffect ở MainActivity sẽ điều hướng vào "main".
     */
    fun markProfileCompleted() {
        refreshProfileSetupFlag()
    }

    private fun getUserLabel(): String? {
        val user = authManager.currentUser
        return user?.email ?: user?.phoneNumber ?: user?.uid
    }

    fun signInEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            val result = authManager.signInWithEmailAndPassword(email, password)
            if (result.isSuccess) {
                _uiState.value = AuthUiState(
                    isSignedIn = true,
                    userLabel = getUserLabel(),
                    needsProfileSetup = null // pending until refresh below
                )
                refreshProfileSetupFlag()
            } else {
                val exception = result.exceptionOrNull()
                val errorMsg = when {
                    exception?.message?.contains("incorrect, malformed or has expired", ignoreCase = true) == true -> "Email hoặc mật khẩu không chính xác."
                    exception?.message?.contains("no user record", ignoreCase = true) == true -> "Tài khoản không tồn tại. Vui lòng đăng ký."
                    exception?.message?.contains("email address is badly formatted", ignoreCase = true) == true -> "Định dạng email không hợp lệ."
                    else -> exception?.message ?: "Đăng nhập thất bại"
                }
                _uiState.value = _uiState.value.copy(loading = false, error = errorMsg)
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
            if (result.isSuccess) {
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
                _uiState.value = AuthUiState(
                    isSignedIn = true,
                    userLabel = getUserLabel(),
                    needsProfileSetup = null
                )
                refreshProfileSetupFlag()
            } else {
                val exception = result.exceptionOrNull()
                val errorMsg = when {
                    exception?.message?.contains("email address is already in use", ignoreCase = true) == true -> "Email này đã được sử dụng. Vui lòng đăng nhập."
                    exception?.message?.contains("password should be at least", ignoreCase = true) == true -> "Mật khẩu phải có ít nhất 6 ký tự."
                    exception?.message?.contains("email address is badly formatted", ignoreCase = true) == true -> "Định dạng email không hợp lệ."
                    else -> exception?.message ?: "Đăng ký thất bại"
                }
                _uiState.value = _uiState.value.copy(loading = false, error = errorMsg)
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            val result = authManager.signInAnonymously()
            if (result.isSuccess) {
                _uiState.value = AuthUiState(
                    isSignedIn = true,
                    info = "Đăng nhập ẩn danh",
                    userLabel = getUserLabel(),
                    needsProfileSetup = null
                )
                refreshProfileSetupFlag()
            } else {
                _uiState.value = _uiState.value.copy(loading = false, error = result.exceptionOrNull()?.message ?: "Đăng nhập thất bại")
            }
        }
    }

    /**
     * Sign-in bằng Google + pre-fill Profile lần đầu từ thông tin Google trả về.
     * Nếu user đã có Profile (Room) thì giữ nguyên — chỉ pre-fill khi blank.
     */
    fun signInWithGoogle(account: GoogleAccount) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(account.idToken, null)
            try {
                firebaseAuth.signInWithCredential(credential).await()
                // Pre-fill Profile lần đầu nếu chưa có name (FirstSetup state).
                val existing = profileRepository.latestProfile().first()
                if (existing?.name.isNullOrBlank()) {
                    saveProfileLocal(
                        name = account.displayName,
                        phone = account.phoneNumber,
                        region = null,
                        note = null,
                        role = "FARMER",
                        cccd = null,
                        username = null,
                        email = account.email
                    )
                }
                _uiState.value = AuthUiState(
                    isSignedIn = true,
                    userLabel = getUserLabel(),
                    needsProfileSetup = null
                )
                refreshProfileSetupFlag()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message ?: "Đăng nhập Google thất bại")
            }
        }
    }

    fun sendOtp(activity: Activity, phoneNumber: String) {
        val activityRef = java.lang.ref.WeakReference(activity)
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

        val act = activityRef.get()
        if (act == null || act.isFinishing || act.isDestroyed) {
            _uiState.value = _uiState.value.copy(loading = false, error = "Không thể gửi OTP do màn hình đã bị đóng.")
            return
        }

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(normalized)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(act)
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
                _uiState.value = AuthUiState(
                    isSignedIn = true,
                    userLabel = getUserLabel(),
                    needsProfileSetup = null
                )
                refreshProfileSetupFlag()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message ?: "OTP không hợp lệ")
            }
        }
    }

    /**
     * Forgot-password placeholder — chưa gọi Firebase `sendPasswordResetEmail`.
     * Sẽ implement ở phase sau khi đã thiết kế xong UX (deep link, custom email template).
     */
    fun requestPasswordReset(@Suppress("UNUSED_PARAMETER") email: String) {
        _uiState.value = _uiState.value.copy(
            error = null,
            info = "Tính năng quên mật khẩu đang được phát triển. Vui lòng liên hệ admin."
        )
    }

    fun signOut() {
        authManager.signOut()
        _uiState.value = AuthUiState(
            isSignedIn = false,
            info = "Đã đăng xuất",
            needsProfileSetup = false
        )
        AnalyticsHelper.setUser(null, null)
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
            com.giathinh.canlua.data.model.Profile(
                uid = "", // ProfileRepository sẽ tự gán uid của user đang đăng nhập
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

    /**
     * Phân biệt user gõ email hay số điện thoại trong field đầu tiên ở AuthScreen.
     * - Phone: chỉ chứa digits/+/space, length ≥ 9 sau khi strip.
     * - Email: có ký tự '@'.
     */
    fun looksLikePhone(input: String): Boolean {
        val cleaned = input.trim().filter { !it.isWhitespace() }
        if (cleaned.contains('@')) return false
        val digits = cleaned.filter { it.isDigit() || it == '+' }
        return digits.length >= 9 && digits == cleaned
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
