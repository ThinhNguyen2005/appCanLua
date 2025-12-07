package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.data.dao.ProfileDao
import com.GiaThinh.canlua.data.model.Profile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao
) {
    suspend fun saveProfile(profile: Profile) {
        profileDao.insert(profile)
    }

    fun latestProfile(): Flow<Profile?> = profileDao.getLatestProfile()
}

