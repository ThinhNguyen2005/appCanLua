package com.giathinh.canlua.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.canlua.repository.CardRepository
import com.giathinh.canlua.repository.ProfileRepository
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * ViewModel khởi tạo — chạy song song với native SplashScreen.
 *
 * v3 (2026-05-26): Bỏ timeout cứng 2.5s. Splash giữ đến khi data Room (cards +
 * profile) thực sự load xong, không theo thời gian cố định. 15s timeout chỉ là
 * safety net để tránh kẹt vĩnh viễn nếu DB lỗi nghiêm trọng — không phải SLA.
 *
 * Lý do: cards + profile đều là Room local → luôn xong < 1s ngay cả thiết bị yếu.
 * KHÔNG đợi Firestore sync ở đây vì mạng có thể dead → user kẹt splash vô hạn.
 * Firestore sync chạy background qua SyncManager; nếu offline, Room data cũ vẫn dùng được.
 */
@HiltViewModel
class InitViewModel @Inject constructor(
    private val cardRepository: Lazy<CardRepository>,
    private val profileRepository: Lazy<ProfileRepository>
) : ViewModel() {

    private val _isDataReady = MutableStateFlow(false)
    val isDataReady: StateFlow<Boolean> = _isDataReady.asStateFlow()

    init {
        viewModelScope.launch {
            // 15s safety net — chỉ hit khi DB corrupt / lỗi nghiêm trọng.
            // Bình thường awaitAll xong trong vài chục → vài trăm ms.
            withTimeoutOrNull(15_000L) {
                withContext(Dispatchers.IO) {
                    awaitAll(
                        async { runCatching { cardRepository.get().getAllCards().first() } },
                        async { runCatching { profileRepository.get().latestProfile().first() } }
                    )
                }
            }
            _isDataReady.value = true
        }
    }
}
