package com.giathinh.canlua.repository

import com.giathinh.canlua.data.dao.ProfileDao
import com.giathinh.canlua.data.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.giathinh.canlua.util.CccdCrypto
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.flow.first

@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val cccdCrypto: CccdCrypto,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    suspend fun saveProfile(profile: Profile): Boolean {
        val uid = profile.uid.ifBlank { "" }
        val savedProfile = profile.copy(uid = uid)
        val encryptedProfile = savedProfile.copy(cccd = cccdCrypto.encrypt(savedProfile.cccd).orEmpty())
        profileDao.insert(encryptedProfile)
        return true
    }

    suspend fun ensureCurrentProfile(): Profile? {
        return latestProfile().first()
    }

    fun latestProfile(): Flow<Profile?> {
        val uid = ""
        return profileDao.getProfileByUid(uid).map { profile ->
            profile?.copy(cccd = cccdCrypto.decrypt(profile.cccd).orEmpty())
        }
    }
}
