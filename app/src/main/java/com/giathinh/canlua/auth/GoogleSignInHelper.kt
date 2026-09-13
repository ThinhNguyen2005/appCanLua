package com.giathinh.canlua.auth

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.giathinh.canlua.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
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
 * Modern Credential Manager + Google Identity API (thay thế hoàn toàn GoogleSignIn legacy).
 * Tự động kiểm tra kết nối mạng trước khi gọi API, tránh báo lỗi sai nguyên nhân.
 */
class GoogleSignInHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    private val webClientId = context.getString(R.string.default_web_client_id)

    suspend fun signIn(
        onSuccess: (GoogleAccount) -> Unit,
        onCancel: () -> Unit,
        onError: (message: String) -> Unit
    ) {
        // Kiểm tra mạng trước khi mở picker
        if (!isNetworkAvailable(context)) {
            Log.w(TAG, "Cannot sign in: no internet connection")
            onError(context.getString(R.string.auth_error_no_network))
            return
        }

        val targetContext = findActivity(context) ?: context
        try {
            // Ưu tiên GetSignInWithGoogleOption cho hành động click nút "Tiếp tục với Google"
            val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val response = credentialManager.getCredential(targetContext, request)
            val account = extractAccount(response.credential)
            if (account != null) {
                onSuccess(account)
            } else {
                onError(context.getString(R.string.auth_google_missing_id_token))
            }
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No credential available with GetSignInWithGoogleOption", e)
            if (!isNetworkAvailable(context)) {
                onError(context.getString(R.string.auth_error_no_network))
            } else {
                // Thử fallback sang GetGoogleIdOption (filterByAuthorizedAccounts = false)
                tryFallbackGoogleId(targetContext, onSuccess, onCancel, onError)
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "User cancelled Google sign-in: ${e.message}")
            onCancel()
        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredential failed [type=${e.type}]: ${e.message}", e)
            if (!isNetworkAvailable(context)) {
                onError(context.getString(R.string.auth_error_no_network))
            } else {
                onError(humanizeError(e))
            }
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Parse ID token failed", e)
            onError(context.getString(R.string.auth_google_invalid_token))
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error during Google sign-in", e)
            if (!isNetworkAvailable(context)) {
                onError(context.getString(R.string.auth_error_no_network))
            } else {
                onError(e.message ?: context.getString(R.string.auth_google_failed))
            }
        }
    }

    private suspend fun tryFallbackGoogleId(
        targetContext: Context,
        onSuccess: (GoogleAccount) -> Unit,
        onCancel: () -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val response = credentialManager.getCredential(targetContext, request)
            val account = extractAccount(response.credential)
            if (account != null) {
                onSuccess(account)
            } else {
                onError(context.getString(R.string.auth_google_missing_id_token))
            }
        } catch (e: NoCredentialException) {
            Log.w(TAG, "Fallback NoCredentialException", e)
            if (!isNetworkAvailable(context)) {
                onError(context.getString(R.string.auth_error_no_network))
            } else {
                onError(context.getString(R.string.auth_google_no_account))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "User cancelled fallback: ${e.message}")
            onCancel()
        } catch (e: Exception) {
            Log.e(TAG, "Fallback failed", e)
            if (!isNetworkAvailable(context)) {
                onError(context.getString(R.string.auth_error_no_network))
            } else {
                val msg = (e as? GetCredentialException)?.let { humanizeError(it) }
                    ?: e.message
                    ?: context.getString(R.string.auth_google_failed)
                onError(msg)
            }
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
            msg.contains("ERR_INTERNET_DISCONNECTED", ignoreCase = true) ||
            msg.contains("NETWORK_ERROR", ignoreCase = true) ||
            msg.contains("NetworkException", ignoreCase = true) ->
                context.getString(R.string.auth_error_no_network)
            msg.contains("DEVELOPER_ERROR", ignoreCase = true) || msg.contains("10:", ignoreCase = true) ->
                context.getString(R.string.auth_google_developer_error)
            else -> msg.ifBlank { context.getString(R.string.auth_google_failed) }
        }
    }

    private fun isNetworkAvailable(ctx: Context): Boolean {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    private fun findActivity(ctx: Context): Activity? {
        var c = ctx
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
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
