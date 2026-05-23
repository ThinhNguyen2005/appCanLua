package com.GiaThinh.canlua

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Configuration
import android.content.Context
import android.content.pm.PackageManager
import com.GiaThinh.canlua.repository.AuthManager
import com.GiaThinh.canlua.repository.CardRepository
import com.GiaThinh.canlua.repository.ProfileRepository
import com.GiaThinh.canlua.repository.SettingsRepository
import com.GiaThinh.canlua.repository.SyncManager
import com.GiaThinh.canlua.repository.SyncWorker
import com.GiaThinh.canlua.util.AnalyticsHelper
import com.GiaThinh.canlua.util.FirebaseRemoteConfigManager
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
    lateinit var settingsRepository: SettingsRepository

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
        AnalyticsHelper.init(this)
        PremiumState.init(this)
        FirebaseRemoteConfigManager.init(this)
        // Sync: dùng cached/default → chạy ngay để user nhận premium nếu đã từng cài trước cutoff.
        // Nhưng firstInstallTime > cutoff (user cài hôm nay) → không nhận.
        applyEarlyAdopterPremiumSync()
        // Async: fetch Firebase → force update → check lại nếu cần.
        // Gọi forceFetch trước check để lấy giá trị mới nhất trước khi apply.
        appScope.launch {
            val fetched = FirebaseRemoteConfigManager.forceFetch()
            if (fetched) {
                // Config mới → check lại (phòng trường hợp cutoff mới đã pass).
                applyEarlyAdopterPremiumSync()
            }
        }
        scheduleOrphanClaim()
        scheduleAutoPullOnSignIn()
        observeAutoSyncPreference()
    }

    /**
     * Sync version: dùng giá trị cached/default từ Firebase Remote Config.
     * Chạy ngay trong onCreate — không blocking.
     */
    private fun applyEarlyAdopterPremiumSync() {
        try {
            val firstInstallTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime
            PremiumState.applyEarlyAdopterIfEligible(
                context = applicationContext,
                firstInstallTimeMs = firstInstallTime,
                earlyAdopterEnabled = FirebaseRemoteConfigManager.earlyAdopterEnabled,
                remoteCutoffMs = FirebaseRemoteConfigManager.earlyAdopterCutoffMs.takeIf { it > 0 },
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // Không xác định được first install time → bỏ qua early adopter
        }
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
                    if (uid != null && syncManager.isOnline() && settingsRepository.isAutoSyncEnabled()) {
                        syncManager.pullAllForCurrentUser()
                    }
                }
        }
    }

    private fun observeAutoSyncPreference() {
        appScope.launch {
            settingsRepository.autoSyncEnabled.collect { enabled ->
                if (enabled) {
                    schedulePeriodicSync()
                } else {
                    WorkManager.getInstance(this@CanLuaApplication).cancelUniqueWork("sync-worker")
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