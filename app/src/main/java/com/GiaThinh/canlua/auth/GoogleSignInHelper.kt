package com.GiaThinh.canlua.auth

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.GiaThinh.canlua.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * Profile lấy ra từ GoogleIdTokenCredential — forward cho ViewModel để pre-fill local Profile
 * sau lần đăng nhập đầu tiên (giảm friction cho user).
 */
data class GoogleAccount(
    val idToken: String,
    val email: String?,
    val displayName: String?,
    val phoneNumber: String?,
    val photoUri: Uri?
)

/**
 * Wrapper Credential Manager + Sign in with Google — modern API thay cho `GoogleSignIn` deprecated.
 *
 * **Quyết định thiết kế:** chỉ dùng explicit `GetSignInWithGoogleOption` flow (bottom sheet),
 * không gọi silent `GetGoogleIdOption(filterByAuthorizedAccounts=true)` trước. Lý do:
 * - Silent step đôi khi throw `GetCredentialCancellationException` ("Account reauth failed [16]")
 *   ngay sau khi user pick account → gây ra "Bạn đã huỷ đăng nhập" dù user đã chọn.
 * - Explicit flow show bottom sheet rõ ràng — UX nhất quán cho mọi trường hợp.
 *
 * Usage:
 * ```
 * val helper = GoogleSignInHelper(context)
 * helper.signIn(
 *     onSuccess = { account -> viewModel.signInWithGoogle(account) },
 *     onCancel = { /* user dismissed picker — không show error */ },
 *     onError = { msg -> showSnackbar(msg) }
 * )
 * ```
 */
class GoogleSignInHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    private val webClientId = context.getString(R.string.default_web_client_id)

    suspend fun signIn(
        onSuccess: (GoogleAccount) -> Unit,
        onCancel: () -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            val option = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            val response = credentialManager.getCredential(context, request)
            val account = extractAccount(response.credential)
            if (account != null) onSuccess(account)
            else onError("Không lấy được Google ID Token.")
        } catch (e: NoCredentialException) {
            onError("Thiết bị chưa có tài khoản Google nào. Hãy thêm tài khoản trong Cài đặt.")
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "User cancelled Google sign-in: ${e.message}")
            onCancel()
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

    private fun extractAccount(credential: androidx.credentials.Credential): GoogleAccount? {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val cred = GoogleIdTokenCredential.createFrom(credential.data)
            return GoogleAccount(
                idToken = cred.idToken,
                email = cred.id,
                displayName = cred.displayName,
                phoneNumber = cred.phoneNumber,
                photoUri = cred.profilePictureUri
            )
        }
        return null
    }

    private fun humanizeError(e: GetCredentialException): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("DEVELOPER_ERROR", ignoreCase = true) ->
                "Cấu hình Google Sign-In sai (SHA-1 / package). Kiểm tra Firebase Console."
            else -> msg.ifBlank { "Đăng nhập Google thất bại." }
        }
    }

    companion object {
        private const val TAG = "GoogleSignInHelper"
    }
}

/**
 * Hiển thị prompt "Save password" của Google sau khi user đăng nhập email/password thành công.
 * Trả `true` nếu credential lưu OK, `false` nếu user huỷ hoặc lỗi (silent fail — không spam UI).
 *
 * Phải gọi với [Activity] (không phải application context) để Credential Manager bind UI.
 */
suspend fun saveLoginCredential(activity: Activity, email: String, password: String): Boolean {
    return try {
        val cm = CredentialManager.create(activity)
        val request = CreatePasswordRequest(id = email, password = password)
        cm.createCredential(activity, request)
        true
    } catch (e: Exception) {
        Log.w("GoogleSignInHelper", "Save credential skipped: ${e.message}")
        false
    }
}
