package com.GiaThinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GiaThinh.canlua.data.model.Profile
import com.GiaThinh.canlua.repository.ProfileRepository
import com.GiaThinh.canlua.ui.screen.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    val profile = profileRepository.latestProfile()

    fun saveProfile(
        name: String,
        phone: String?,
        region: String?,
        note: String?,
        role: UserRole,
        cccd: String?,
        username: String?,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val profile = Profile(
                name = name.trim(),
                phone = phone?.trim().orEmpty(),
                region = region?.trim().orEmpty(),
                note = note?.trim().orEmpty(),
                role = role.name,
                cccd = cccd?.trim().orEmpty(),
                username = username?.trim().orEmpty()
            )
            profileRepository.saveProfile(profile)
            onSaved()
        }
    }
}

