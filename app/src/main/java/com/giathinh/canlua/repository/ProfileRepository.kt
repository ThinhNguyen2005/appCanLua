package com.giathinh.canlua.repository

import com.giathinh.canlua.data.dao.ProfileDao
import com.giathinh.canlua.data.model.Profile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.map
import com.giathinh.canlua.util.CccdCrypto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository profile theo Firebase UID — mỗi tài khoản có profile riêng,
 * không lẫn lộn role giữa các user khi share máy.
 *
 * `latestProfile()` reactive theo `FirebaseAuth.AuthStateListener`: khi sign-out
 * sẽ emit `null`, khi sign-in user mới sẽ tự switch sang profile của user đó.
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    /**
     * Lưu profile cho user **đang đăng nhập**. Nếu chưa sign-in → no-op.
     * Tự gán `uid = auth.currentUser?.uid` để caller không cần biết về UID.
     */
    suspend fun saveProfile(profile: Profile): Boolean {
        val uid = profile.uid.ifBlank { auth.currentUser?.uid.orEmpty() }
        if (uid.isBlank()) return false
        val savedProfile = profile.copy(uid = uid)
        val encryptedProfile = savedProfile.copy(cccd = CccdCrypto.encrypt(savedProfile.cccd).orEmpty())
        profileDao.insert(encryptedProfile)
        runCatching {
            firestore.collection("profiles").document(uid).set(encryptedProfile).await()
        }
        return true
    }

    suspend fun ensureCurrentProfile(): Profile? {
        val uid = auth.currentUser?.uid.orEmpty()
        if (uid.isBlank()) return null

        val local = profileDao.getProfileByUid(uid).first()
        if (local != null) {
            return local.copy(cccd = CccdCrypto.decrypt(local.cccd).orEmpty())
        }

        val snapshot = runCatching {
            firestore.collection("profiles").document(uid).get().await()
        }.getOrNull()

        val remote = snapshot?.data?.let { data ->
            Profile(
                uid = uid,
                name = data["name"] as? String ?: "",
                phone = data["phone"] as? String ?: "",
                region = data["region"] as? String ?: "",
                note = data["note"] as? String ?: "",
                role = data["role"] as? String ?: "FARMER",
                cccd = CccdCrypto.decrypt(data["cccd"] as? String ?: "").orEmpty(),
                username = data["username"] as? String ?: "",
                email = data["email"] as? String ?: "",
                roleGrantedBy = data["roleGrantedBy"] as? String ?: "self",
                roleGrantedAt = data["roleGrantedAt"] as? Long ?: 0L
            )
        }

        if (remote != null) {
            val encryptedRemote = remote.copy(cccd = CccdCrypto.encrypt(remote.cccd).orEmpty())
            profileDao.insert(encryptedRemote)
        }
        return remote
    }

    /**
     * Stream profile của user hiện tại. Tự re-subscribe khi auth state đổi
     * để 2 ViewModel đọc cùng flow đều thấy đúng profile của session mới.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun latestProfile(): Flow<Profile?> = authUidFlow().flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
            val isGuest = prefs.getBoolean("guest_mode", false)
            if (isGuest) {
                flowOf(Profile(
                    uid = "GUEST",
                    name = "Khách",
                    role = "FARMER"
                ))
            } else {
                flowOf(null)
            }
        } else {
            profileDao.getProfileByUid(uid).map { profile ->
                profile?.copy(cccd = CccdCrypto.decrypt(profile.cccd).orEmpty())
            }
        }
    }

    suspend fun deleteCurrent() {
        val uid = auth.currentUser?.uid ?: return
        profileDao.deleteByUid(uid)
    }

    private fun authUidFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
}
