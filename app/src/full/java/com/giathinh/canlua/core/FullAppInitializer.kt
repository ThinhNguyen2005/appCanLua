package com.giathinh.canlua.core

import android.app.Application
import android.content.pm.PackageManager
import androidx.work.WorkManager
import com.giathinh.canlua.BuildConfig
import com.giathinh.canlua.repository.AuthManager
import com.giathinh.canlua.repository.CardRepository
import com.giathinh.canlua.repository.SettingsRepository
import com.giathinh.canlua.repository.SyncManager
import com.giathinh.canlua.util.AnalyticsHelper
import com.giathinh.canlua.util.FirebaseRemoteConfigManager
import com.giathinh.canlua.util.PremiumState
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FullAppInitializer @Inject constructor(
    private val authManager: AuthManager,
    private val cardRepository: Lazy<CardRepository>,
    private val syncManager: Lazy<SyncManager>,
    private val settingsRepository: SettingsRepository
) : AppInitializer {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun init(application: Application) {
        val factory = if (BuildConfig.DEBUG) {
            runCatching {
                val clazz = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                clazz.getMethod("getInstance").invoke(null) as com.google.firebase.appcheck.AppCheckProviderFactory
            }.getOrNull() ?: PlayIntegrityAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        Firebase.appCheck.installAppCheckProviderFactory(factory)

        appScope.launch {
            AnalyticsHelper.init(application)
            PremiumState.init(application)
            FirebaseRemoteConfigManager.init(application)
            applyEarlyAdopterPremiumSync(application)
            val fetched = FirebaseRemoteConfigManager.forceFetch()
            if (fetched) {
                applyEarlyAdopterPremiumSync(application)
            }
        }
        scheduleOrphanClaim()
        observeAutoSyncPreference(application)
    }

    private fun applyEarlyAdopterPremiumSync(application: Application) {
        try {
            val firstInstallTime = application.packageManager.getPackageInfo(application.packageName, 0).firstInstallTime
            PremiumState.applyEarlyAdopterIfEligible(
                context = application.applicationContext,
                firstInstallTimeMs = firstInstallTime,
                earlyAdopterEnabled = FirebaseRemoteConfigManager.earlyAdopterEnabled,
                remoteCutoffMs = FirebaseRemoteConfigManager.earlyAdopterCutoffMs.takeIf { it > 0 },
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // Không xác định được first install time -> bỏ qua early adopter
        }
    }

    private fun scheduleOrphanClaim() {
        appScope.launch {
            authManager.authStateFlow.filterNotNull().collect {
                settingsRepository.setGuestMode(false)
                cardRepository.get().claimOrphanCardsForCurrentUser()
            }
        }
    }

    private fun observeAutoSyncPreference(application: Application) {
        appScope.launch {
            settingsRepository.autoSyncEnabled.collect { isEnabled ->
                if (!isEnabled) {
                    WorkManager.getInstance(application).cancelUniqueWork("sync-worker")
                }
            }
        }
    }
}
