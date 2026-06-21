package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.data.model.Profile
import com.giathinh.canlua.data.model.TraderHistoryItem
import com.giathinh.canlua.repository.CardRepository
import com.giathinh.canlua.repository.ProfileRepository
import com.giathinh.canlua.repository.SettingsRepository
import com.giathinh.canlua.ui.screen.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val cardRepository: CardRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val isGuestMode: StateFlow<Boolean> = settingsRepository.guestMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = settingsRepository.isGuestMode()
        )

    fun disableGuestMode() {
        settingsRepository.setGuestMode(false)
    }

    /** Latest profile — emits null until first save. */
    val profile = profileRepository.latestProfile()

    /**
     * Lịch sử thương lái đã từng mua ruộng — reactive, tự update khi card mới được tạo.
     * Hiển thị ở FarmerProfileScreen → "Thương lái đã giao dịch".
     */
    val traderHistory: StateFlow<List<TraderHistoryItem>> =
        cardRepository.getTraderHistory()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /** Lưu profile mới (đường register / setup lần đầu). */
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
                uid = "", // ProfileRepository.saveProfile() tự gán từ FirebaseAuth
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

    /**
     * Update profile hiện tại — dùng cho FarmerProfileScreen / TraderProfileScreen.
     * `current` đã có UID đúng (load từ DAO theo UID), copy() giữ nguyên UID nên insert REPLACE
     * sẽ ghi đè đúng row của user hiện tại.
     */
    fun updateProfile(
        current: Profile,
        name: String = current.name,
        phone: String = current.phone,
        region: String = current.region,
        note: String = current.note,
        role: String = current.role,
        cccd: String = current.cccd,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val updated = current.copy(
                name = name.trim(),
                phone = phone.trim(),
                region = region.trim(),
                note = note.trim(),
                role = role,
                cccd = cccd.trim()
            )
            profileRepository.saveProfile(updated)
            onSaved()
        }
    }
}
