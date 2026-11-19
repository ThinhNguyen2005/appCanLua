package com.giathinh.canlua.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.giathinh.canlua.ads.AdConsentManager
import com.giathinh.canlua.data.model.AppLanguage
import com.giathinh.canlua.data.model.AppThemeMode
import com.giathinh.canlua.data.model.FontScale
import com.giathinh.canlua.repository.AuthManager
import com.giathinh.canlua.repository.CardRepository
import com.giathinh.canlua.repository.SettingsRepository
import com.giathinh.canlua.repository.WeighDefaults
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val prefs = InMemorySharedPreferences()
    private val context = mockk<Context>(relaxed = true)
    private val cardRepository = mockk<CardRepository>(relaxed = true)
    private val authManager = mockk<AuthManager>(relaxed = true)
    private val adConsentManager = mockk<AdConsentManager>(relaxed = true)
    private val privacyRequiredFlow = MutableStateFlow(false)

    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) } returns prefs
        every { adConsentManager.isPrivacyOptionsRequired } returns privacyRequiredFlow
        settingsRepository = SettingsRepository(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SettingsViewModel = SettingsViewModel(
        settingsRepository = settingsRepository,
        cardRepository = cardRepository,
        authManager = authManager,
        adConsentManager = adConsentManager
    )

    @Test
    fun `appearance, language, and accessibility settings propagate reactively through StateFlows`() =
        runTest(testDispatcher) {
            val vm = createViewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.fontScale.collect() }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.appThemeMode.collect() }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.language.collect() }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.isTtsEnabled.collect() }
            advanceUntilIdle()

            assertEquals(FontScale.NORMAL, vm.fontScale.value)
            assertEquals(AppThemeMode.LIGHT, vm.appThemeMode.value)
            assertEquals(AppLanguage.VIETNAMESE, vm.language.value)
            assertTrue(vm.isTtsEnabled.value)

            vm.setFontScale(FontScale.XLARGE)
            vm.setThemeMode(AppThemeMode.OLED)
            vm.setLanguage(AppLanguage.ENGLISH)
            vm.setTtsEnabled(false)
            advanceUntilIdle()

            assertEquals(FontScale.XLARGE, vm.fontScale.value)
            assertEquals(AppThemeMode.OLED, vm.appThemeMode.value)
            assertEquals(AppLanguage.ENGLISH, vm.language.value)
            assertFalse(vm.isTtsEnabled.value)
        }

    @Test
    fun `sync, guest mode, and weigh defaults persist and emit updated StateFlows`() = runTest(testDispatcher) {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.isAutoSyncEnabled.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.isSyncOnlyWifi.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.isGuestMode.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.weighDefaults.collect() }
        advanceUntilIdle()

        assertFalse(vm.isAutoSyncEnabled.value)
        assertTrue(vm.isSyncOnlyWifi.value)
        assertFalse(vm.isGuestMode.value)

        val customDefaults = WeighDefaults(
            impurityIsPercent = true,
            bagMethodIsSampling = true,
            bagSampleCount = 5,
            bagSampleTotalWeight = 2.5,
            weightInputMode = "LARGE"
        )
        vm.setAutoSyncEnabled(true)
        vm.setSyncOnlyWifi(false)
        vm.setGuestMode(true)
        vm.setWeighDefaults(customDefaults)
        advanceUntilIdle()

        assertTrue(vm.isAutoSyncEnabled.value)
        assertFalse(vm.isSyncOnlyWifi.value)
        assertTrue(vm.isGuestMode.value)
        assertEquals(customDefaults, vm.weighDefaults.value)
    }

    @Test
    fun `privacy options reflect UMP consent manager and delegate form display`() = runTest(testDispatcher) {
        val vm = createViewModel()
        assertFalse(vm.isPrivacyOptionsRequired.value)

        privacyRequiredFlow.value = true
        assertTrue(vm.isPrivacyOptionsRequired.value)

        val activity = mockk<Activity>(relaxed = true)
        vm.showPrivacyOptionsForm(activity)
        verify(exactly = 1) { adConsentManager.showPrivacyOptionsForm(activity) }
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()
        private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

        override fun getAll(): MutableMap<String, *> = data.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor()

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) {
            if (listener != null) listeners.add(listener)
        }

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) {
            listeners.remove(listener)
        }

        private inner class Editor : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clearAll = false

            override fun putString(key: String?, value: String?) = apply { if (key != null) pending[key] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?) = this
            override fun putInt(key: String?, value: Int) = apply { if (key != null) pending[key] = value }
            override fun putLong(key: String?, value: Long) = apply { if (key != null) pending[key] = value }
            override fun putFloat(key: String?, value: Float) = apply { if (key != null) pending[key] = value }
            override fun putBoolean(key: String?, value: Boolean) = apply { if (key != null) pending[key] = value }
            override fun remove(key: String?) = apply { if (key != null) pending[key] = REMOVED }
            override fun clear() = apply { clearAll = true }

            override fun commit(): Boolean {
                applyChanges()
                return true
            }

            override fun apply() {
                applyChanges()
            }

            private fun applyChanges() {
                if (clearAll) data.clear()
                val changedKeys = pending.keys.toList()
                pending.forEach { (k, v) -> if (v === REMOVED) data.remove(k) else data[k] = v }
                changedKeys.forEach { key -> listeners.forEach { it.onSharedPreferenceChanged(this@InMemorySharedPreferences, key) } }
            }
        }

        private companion object {
            val REMOVED = Any()
        }
    }
}
