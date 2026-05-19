package com.GiaThinh.canlua.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.GiaThinh.canlua.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * Wrapper Credential Manager + GoogleIdToken — modern API thay cho `GoogleSignIn` đã deprecated.
 *
 * Usage:
 * ```
 * val helper = GoogleSignInHelper(context)
 * helper.requestIdToken(
 *     onSuccess = { idToken -> viewModel.signInWithGoogle(idToken) },
 *     onError = { msg -> showSnackbar(msg) }
 * )
 * ```
 */
class GoogleSignInHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    private val webClientId = context.getString(R.string.default_web_client_id)

    /**
     * Hai bước:
     * 1. Thử lấy Google ID token "silently" (đã từng đăng nhập trên thiết bị).
     * 2. Nếu không có credential, fallback qua dialog "Sign in with Google" (rõ ràng).
     */
    suspend fun requestIdToken(
        onSuccess: (idToken: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            val token = trySilent() ?: tryExplicit()
            if (token != null) onSuccess(token)
            else onError("Không lấy được Google ID Token.")
        } catch (e: NoCredentialException) {
            // Không có account Google nào trên thiết bị
            onError("Thiết bị chưa có tài khoản Google nào. Hãy thêm tài khoản trong Cài đặt.")
        } catch (e: GetCredentialCancellationException) {
            // User huỷ dialog, hoặc framework báo "[16] Account reauth failed"
            // (xảy ra khi cần xác thực lại nhưng user đóng popup). Không log error để
            // tránh đập vỡ Crashlytics — đây là happy-path "user thay đổi quyết định".
            Log.i(TAG, "User cancelled Google sign-in: ${e.message}")
            onError("Bạn đã huỷ đăng nhập Google.")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredential failed", e)
            onError(humanizeError(e))
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Parse ID token failed", e)
            onError("Token Google không hợp lệ.")
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error", e)
            onError(e.message ?: "Đăng nhập Google thất bại.")
        }
    }

    /**
     * Bước 1: Request "auto-select" account đã có trên device.
     * Trả null nếu không có credential phù hợp (chưa đăng nhập / từ chối auto-select).
     */
    private suspend fun trySilent(): String? {
        return try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            val response = credentialManager.getCredential(context, request)
            extractIdToken(response.credential)
        } catch (e: NoCredentialException) {
            null
        } catch (e: GetCredentialCancellationException) {
            // Silent step bị huỷ → ném tiếp lên caller; KHÔNG fallback sang explicit
            // (vì tryExplicit cũng sẽ bị huỷ giống vậy → spam dialog).
            throw e
        } catch (e: GetCredentialException) {
            // User chưa từng đăng nhập app này — fallback explicit
            null
        }
    }

    /**
     * Bước 2: Explicit "Sign in with Google" dialog.
     * Dùng `GetSignInWithGoogleOption` để hiển thị bottom sheet chọn tài khoản.
     */
    private suspend fun tryExplicit(): String? {
        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val response = credentialManager.getCredential(context, request)
        return extractIdToken(response.credential)
    }

    private fun extractIdToken(credential: androidx.credentials.Credential): String? {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
            return googleCred.idToken
        }
        return null
    }

    private fun humanizeError(e: GetCredentialException): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("DEVELOPER_ERROR", ignoreCase = true) ->
                "Cấu hình Google Sign-In sai (SHA-1 / package). Kiểm tra Firebase Console."
            msg.contains("activity is cancelled", ignoreCase = true) ||
                msg.contains("user canceled", ignoreCase = true) ||
                msg.contains("Account reauth failed", ignoreCase = true) ||
                msg.contains("[16]", ignoreCase = true) -> "Bạn đã huỷ đăng nhập."
            else -> msg.ifBlank { "Đăng nhập Google thất bại." }
        }
    }

    companion object {
        private const val TAG = "GoogleSignInHelper"
    }
}
