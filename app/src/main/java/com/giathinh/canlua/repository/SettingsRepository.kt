package com.giathinh.canlua.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.giathinh.canlua.data.model.AppLanguage
import com.giathinh.canlua.data.model.AppThemeMode
import com.giathinh.canlua.data.model.FontScale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _fontScale = MutableStateFlow(readFontScale())
    val fontScale: Flow<FontScale> = _fontScale.asStateFlow()

    private val _language = MutableStateFlow(readLanguage())
    val language: Flow<AppLanguage> = _language.asStateFlow()

    private val _appThemeMode = MutableStateFlow(readThemeMode())
    val appThemeMode: Flow<AppThemeMode> = _appThemeMode.asStateFlow()

    private val _autoSyncEnabled = MutableStateFlow(isAutoSyncEnabled())
    val autoSyncEnabled: Flow<Boolean> = _autoSyncEnabled.asStateFlow()

    private val _syncOnlyWifi = MutableStateFlow(prefs.getBoolean(KEY_SYNC_ONLY_WIFI, true))
    @Suppress("unused")
    val syncOnlyWifi: Flow<Boolean> = _syncOnlyWifi.asStateFlow()

    private val _weighDefaults = MutableStateFlow(readWeighDefaults())
    val weighDefaults: Flow<WeighDefaults> = _weighDefaults.asStateFlow()

    private val _ttsEnabled = MutableStateFlow(prefs.getBoolean(KEY_TTS_ENABLED, true))
    val ttsEnabled: Flow<Boolean> = _ttsEnabled.asStateFlow()

    private val _guestMode = MutableStateFlow(prefs.getBoolean(KEY_GUEST_MODE, false))
    val guestMode: Flow<Boolean> = _guestMode.asStateFlow()

    // Giữ tham chiếu mạnh (Strong Reference) tránh bị Garbage Collector thu hồi mất listener
    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                KEY_FONT_SCALE -> _fontScale.value = readFontScale()
                KEY_LANGUAGE -> _language.value = readLanguage()
                KEY_AUTO_SYNC_ENABLED -> _autoSyncEnabled.value = isAutoSyncEnabled()
                KEY_SYNC_ONLY_WIFI -> _syncOnlyWifi.value = prefs.getBoolean(KEY_SYNC_ONLY_WIFI, true)
                KEY_TTS_ENABLED -> _ttsEnabled.value = prefs.getBoolean(KEY_TTS_ENABLED, true)
                KEY_THEME_MODE -> _appThemeMode.value = readThemeMode()
                KEY_GUEST_MODE -> _guestMode.value = prefs.getBoolean(KEY_GUEST_MODE, false)
                KEY_IMPURITY_IS_PERCENT,
                KEY_BAG_METHOD_IS_SAMPLING,
                KEY_BAG_SAMPLE_COUNT,
                KEY_BAG_SAMPLE_TOTAL_WEIGHT,
                KEY_WEIGHT_INPUT_MODE -> _weighDefaults.value = readWeighDefaults()
            }
        }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun getThemeMode(): AppThemeMode = readThemeMode()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
        _appThemeMode.value = mode
    }

    fun isTtsEnabled(): Boolean {
        return _ttsEnabled.value
    }

    fun setTtsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_TTS_ENABLED, enabled) }
        _ttsEnabled.value = enabled
    }

    fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, false)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_SYNC_ENABLED, enabled) }
        _autoSyncEnabled.value = enabled
    }

    fun isSyncOnlyWifi(): Boolean {
        return prefs.getBoolean(KEY_SYNC_ONLY_WIFI, true)
    }

    @Suppress("unused")
    fun setSyncOnlyWifi(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SYNC_ONLY_WIFI, enabled) }
        _syncOnlyWifi.value = enabled
    }

    fun setFontScale(scale: FontScale) {
        prefs.edit { putString(KEY_FONT_SCALE, scale.name) }
        _fontScale.value = scale
    }

    fun getFontScale(): FontScale = readFontScale()

    fun setLanguage(language: AppLanguage) {
        prefs.edit { putString(KEY_LANGUAGE, language.tag) }
        _language.value = language
    }

    fun getLanguage(): AppLanguage = readLanguage()

    fun getWeighDefaults(): WeighDefaults = readWeighDefaults()

    fun setWeighDefaults(d: WeighDefaults) {
        prefs.edit {
            putBoolean(KEY_IMPURITY_IS_PERCENT, d.impurityIsPercent)
            putBoolean(KEY_BAG_METHOD_IS_SAMPLING, d.bagMethodIsSampling)
            putInt(KEY_BAG_SAMPLE_COUNT, d.bagSampleCount)
            putFloat(KEY_BAG_SAMPLE_TOTAL_WEIGHT, d.bagSampleTotalWeight.toFloat())
            putString(KEY_WEIGHT_INPUT_MODE, d.weightInputMode)
        }
        _weighDefaults.value = d
    }

    private fun readWeighDefaults(): WeighDefaults = WeighDefaults(
        impurityIsPercent = prefs.getBoolean(KEY_IMPURITY_IS_PERCENT, false),
        bagMethodIsSampling = prefs.getBoolean(KEY_BAG_METHOD_IS_SAMPLING, false),
        bagSampleCount = prefs.getInt(KEY_BAG_SAMPLE_COUNT, 0),
        bagSampleTotalWeight = prefs.getFloat(KEY_BAG_SAMPLE_TOTAL_WEIGHT, 0f).toDouble(),
        weightInputMode = prefs.getString(KEY_WEIGHT_INPUT_MODE, "SMALL") ?: "SMALL"
    )

    private fun readFontScale(): FontScale {
        val name = prefs.getString(KEY_FONT_SCALE, FontScale.NORMAL.name)
        return FontScale.fromName(name)
    }

    private fun readLanguage(): AppLanguage {
        val tag = prefs.getString(KEY_LANGUAGE, AppLanguage.VIETNAMESE.tag)
        return AppLanguage.fromTag(tag)
    }

    private fun readThemeMode(): AppThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.LIGHT.name)
        return AppThemeMode.fromName(name)
    }

    fun isGuestMode(): Boolean {
        return prefs.getBoolean(KEY_GUEST_MODE, false)
    }

    fun setGuestMode(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_GUEST_MODE, enabled) }
        _guestMode.value = enabled
    }

    companion object {
        private const val PREFS_NAME = "app_settings"

        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SYNC_ONLY_WIFI = "sync_only_wifi"
        private const val KEY_GUEST_MODE = "guest_mode"

        // Weigh-options defaults (v17): áp dụng cho phiếu mới tạo. Phiếu cũ giữ mode đã lưu.
        private const val KEY_IMPURITY_IS_PERCENT = "weigh_impurity_is_percent"
        private const val KEY_BAG_METHOD_IS_SAMPLING = "weigh_bag_method_is_sampling"
        private const val KEY_BAG_SAMPLE_COUNT = "weigh_bag_sample_count"
        private const val KEY_BAG_SAMPLE_TOTAL_WEIGHT = "weigh_bag_sample_total_weight"
        private const val KEY_WEIGHT_INPUT_MODE = "weigh_weight_input_mode"
    }
}

data class WeighDefaults(
    val impurityIsPercent: Boolean = false,
    val bagMethodIsSampling: Boolean = false,
    val bagSampleCount: Int = 0,
    val bagSampleTotalWeight: Double = 0.0,
    val weightInputMode: String = "SMALL"
)