package com.GiaThinh.canlua

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Configuration
import com.GiaThinh.canlua.repository.AuthManager
import com.GiaThinh.canlua.repository.CardRepository
import com.GiaThinh.canlua.repository.ProfileRepository
import com.GiaThinh.canlua.repository.RoleRequestRepository
import com.GiaThinh.canlua.repository.SyncManager
import com.GiaThinh.canlua.repository.SyncWorker
import com.GiaThinh.canlua.util.PremiumState
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class CanLuaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var cardRepository: CardRepository

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var roleRequestRepository: RoleRequestRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // --- ĐOẠN ĐÃ SỬA ---
    // Thay đổi từ 'fun getWorkManagerConfiguration()' thành 'val workManagerConfiguration'
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    // -------------------

    override fun onCreate() {
        super.onCreate()
        PremiumState.init(this)
        scheduleOrphanClaim()
        scheduleAutoPullOnSignIn()
        scheduleAutoApplyRoleApproval()
        schedulePeriodicSync()
    }

    /**
     * One-shot migration cho v12 (per-user data isolation):
     * Cards tạo trước update v12 có `ownerUid = ''` (orphan). User đầu tiên
     * đăng nhập sau update sẽ "claim" toàn bộ orphan làm của mình — phù hợp
     * với app cá nhân 1 user/máy trước đó.
     *
     * Sau lần claim đầu, không còn orphan trên device → các user kế tiếp
     * (B, C…) bắt đầu data trắng. Idempotent: gọi nhiều lần cũng an toàn.
     */
    private fun scheduleOrphanClaim() {
        appScope.launch {
            // Đợi đến khi có user sign-in (cold start hoặc lần đăng nhập đầu).
            authManager.authStateFlow.filterNotNull().first()
            cardRepository.claimOrphanCardsForCurrentUser()
        }
    }

    /**
     * Auto pull cards/entries/transactions từ Firestore mỗi khi user đăng nhập.
     *
     * Trigger: phát hiện uid chuyển từ null → non-null (cold-start với session
     * lưu sẵn, sign-in mới, hoặc sign-out → sign-in user khác). Khác uid →
     * trigger lại lần nữa cho user mới.
     *
     * `distinctUntilChanged` để không trigger lại khi token refresh (uid không
     * đổi). Pull lỗi (no internet, Firestore down) → silent — user vẫn dùng
     * được local data offline-first.
     */
    private fun scheduleAutoPullOnSignIn() {
        appScope.launch {
            authManager.authStateFlow
                .map { it?.uid }
                .distinctUntilChanged()
                .collect { uid ->
                    if (uid != null && syncManager.isOnline()) {
                        syncManager.pullAllForCurrentUser()
                    }
                }
        }
    }

    /**
     * Auto-apply TRADER role khi admin duyệt `roleRequests/{uid}.status = APPROVED`.
     *
     * Flow:
     *  1. User submit form RoleRequestScreen → Firestore doc tạo với status = PENDING.
     *  2. Admin xem Firebase Console, đổi status = APPROVED + cập nhật reviewerNote.
     *  3. App đang chạy → observer dưới phát hiện thay đổi → cập nhật local
     *     `Profile.role = "TRADER"` → MainScreen reactive đổi nav graph sang trader.
     *
     * Idempotent: nếu profile đã là TRADER thì không update lại.
     * App offline lúc admin duyệt: lần next online + tap RoleRequestScreen sẽ
     * thấy status APPROVED và observer trigger.
     */
    private fun scheduleAutoApplyRoleApproval() {
        appScope.launch {
            authManager.authStateFlow
                .map { it?.uid }
                .distinctUntilChanged()
                .collect { uid ->
                    if (uid == null) return@collect
                    roleRequestRepository.observeMyRequest().collect { request ->
                        if (request?.status == RoleRequestRepository.Status.APPROVED.name) {
                            // Đọc profile hiện tại để giữ nguyên các field khác
                            val current = profileRepository.latestProfile().first()
                            if (current != null && current.role != "TRADER") {
                                profileRepository.saveProfile(
                                    current.copy(
                                        role = "TRADER",
                                        roleGrantedBy = "admin",
                                        roleGrantedAt = request.reviewedAt ?: System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }
        }
    }

    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "sync-worker",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}