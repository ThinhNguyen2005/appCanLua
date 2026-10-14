package com.giathinh.canlua

import android.app.Application
import android.content.pm.PackageManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.giathinh.canlua.repository.AuthManager
import com.giathinh.canlua.repository.CardRepository
import com.giathinh.canlua.repository.SettingsRepository
import com.giathinh.canlua.repository.SyncManager
import com.giathinh.canlua.util.AnalyticsHelper
import com.giathinh.canlua.util.FirebaseRemoteConfigManager
import com.giathinh.canlua.util.PremiumState
import com.google.firebase.Firebase
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
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

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory.get())
            .build()

    override fun onCreate() {
        super.onCreate()
        val factory = if (BuildConfig.DEBUG) {
            runCatching {
                val clazz = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                clazz.getMethod("getInstance").invoke(null) as AppCheckProviderFactory
            }.getOrNull() ?: PlayIntegrityAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        Firebase.appCheck.installAppCheckProviderFactory(factory)

        appScope.launch {
            AnalyticsHelper.init(this@CanLuaApplication)
            PremiumState.init(this@CanLuaApplication)
            FirebaseRemoteConfigManager.init(this@CanLuaApplication)
            applyEarlyAdopterPremiumSync()
            val fetched = FirebaseRemoteConfigManager.forceFetch()
            if (fetched) {
                applyEarlyAdopterPremiumSync()
            }
        }
        scheduleOrphanClaim()
        observeAutoSyncPreference()
    }

    private fun applyEarlyAdopterPremiumSync() {
        try {
            val firstInstallTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime
            PremiumState.applyEarlyAdopterIfEligible(
                context = applicationContext,
                firstInstallTimeMs = firstInstallTime,
                earlyAdopterEnabled = FirebaseRemoteConfigManager.earlyAdopterEnabled,
                remoteCutoffMs = FirebaseRemoteConfigManager.earlyAdopterCutoffMs.takeIf { it > 0 },
            )
        } catch (_: PackageManager.NameNotFoundException) {
            // Không xác định được first install time -> bỏ qua early adopter
        }
    }

    private fun scheduleOrphanClaim() {
        appScope.launch {
            authManager.authStateFlow.filterNotNull().collect {
                settingsRepository.setGuestMode(false)
                PremiumState.init(this@CanLuaApplication)
                applyEarlyAdopterPremiumSync()
                cardRepository.get().claimOrphanCardsForCurrentUser()
                if (syncManager.get().refreshPendingSyncState()) {
                    syncManager.get().scheduleImmediateSync()
                }
            }
        }
    }

    private fun observeAutoSyncPreference() {
        appScope.launch {
            combine(
                settingsRepository.autoSyncEnabled,
                settingsRepository.syncOnlyWifi
            ) { autoSync, onlyWifi -> autoSync to onlyWifi }
                .collect { (autoSync, _) ->
                    val manager = syncManager.get()
                    if (autoSync) {
                        if (manager.refreshPendingSyncState()) {
                            manager.scheduleImmediateSync(replaceExisting = true)
                        }
                    } else {
                        manager.cancelScheduledSync()
                    }
                }
        }
    }
}