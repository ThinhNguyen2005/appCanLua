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
import com.GiaThinh.canlua.repository.SettingsRepository
import com.GiaThinh.canlua.repository.SyncManager
import dagger.Lazy
import com.GiaThinh.canlua.repository.SyncWorker
import com.GiaThinh.canlua.util.AnalyticsHelper
import com.GiaThinh.canlua.util.FirebaseRemoteConfigManager
import com.GiaThinh.canlua.util.PremiumState
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
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
    lateinit var workerFactory: Lazy<HiltWorkerFactory>

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var cardRepository: Lazy<CardRepository>

    @Inject
    lateinit var syncManager: Lazy<SyncManager>

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // --- ĐOẠN ĐÃ SỬA ---
    // Thay đổi từ 'fun getWorkManagerConfiguration()' thành 'val workManagerConfiguration'
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory.get())
            .build()
    // -------------------

    override fun onCreate() {
        super.onCreate()
        // Khởi tạo Firebase App Check với Debug provider trong bản build DEBUG (dùng reflection để tránh leak class vào release)
        // và Play Integrity trong bản build Release.
        val factory = if (BuildConfig.DEBUG) {
            runCatching {
                val clazz = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                clazz.getMethod("getInstance").invoke(null) as com.google.firebase.appcheck.AppCheckProviderFactory
            }.getOrNull() ?: PlayIntegrityAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        Firebase.appCheck.installAppCheckProviderFactory(factory)
        // M-02: Khởi tạo async để không block luồng UI lúc khởi chạy
        appScope.launch {
            AnalyticsHelper.init(this@CanLuaApplication)
            PremiumState.init(this@CanLuaApplication)
            FirebaseRemoteConfigManager.init(this@CanLuaApplication)
            // Sync: dùng cached/default → chạy ngay để user nhận premium nếu đã từng cài trước cutoff.
            // Nhưng firstInstallTime > cutoff (user cài hôm nay) → không nhận.
            applyEarlyAdopterPremiumSync()
            // Async: fetch Firebase → force update → check lại nếu cần.
            // Gọi forceFetch trước check để lấy giá trị mới nhất trước khi apply.
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
            cardRepository.get().claimOrphanCardsForCurrentUser()
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
                    if (uid != null && syncManager.get().canSync() && settingsRepository.isAutoSyncEnabled()) {
                        // Chỉ tự động tải dữ liệu từ đám mây về nếu cơ sở dữ liệu trên máy trống (đăng nhập lần đầu / cài mới)
                        val localCards = cardRepository.get().getAllCards().first()
                        if (localCards.isEmpty()) {
                            // M-03: Nhường CPU cho luồng chính vẽ giao diện khởi động
                            kotlinx.coroutines.yield()
                            syncManager.get().pullAllForCurrentUser()
                        }
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
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "sync-worker",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}