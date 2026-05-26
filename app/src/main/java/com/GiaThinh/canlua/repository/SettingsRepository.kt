package com.GiaThinh.canlua.repository

import android.content.Context
import android.content.SharedPreferences
import com.GiaThinh.canlua.data.model.AppLanguage
import com.GiaThinh.canlua.data.model.AppThemeMode
import com.GiaThinh.canlua.data.model.FontScale
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
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    private val KEY_TTS_ENABLED = "tts_enabled"
    private val KEY_FONT_SCALE = "font_scale"
    private val KEY_LANGUAGE = "language"
    private val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
    private val KEY_THEME_MODE = "theme_mode"

    // Weigh-options defaults (v17): áp dụng cho phiếu mới tạo. Phiếu cũ giữ mode đã lưu.
    private val KEY_IMPURITY_IS_PERCENT = "weigh_impurity_is_percent"
    private val KEY_BAG_METHOD_IS_SAMPLING = "weigh_bag_method_is_sampling"
    private val KEY_BAG_SAMPLE_COUNT = "weigh_bag_sample_count"
    private val KEY_BAG_SAMPLE_TOTAL_WEIGHT = "weigh_bag_sample_total_weight"
    private val KEY_WEIGHT_INPUT_MODE = "weigh_weight_input_mode"

    private val _fontScale = MutableStateFlow(readFontScale())
    val fontScale: Flow<FontScale> = _fontScale.asStateFlow()

    private val _language = MutableStateFlow(readLanguage())
    val language: Flow<AppLanguage> = _language.asStateFlow()

    private val _appThemeMode = MutableStateFlow(readThemeMode())
    val appThemeMode: Flow<AppThemeMode> = _appThemeMode.asStateFlow()

    private val _autoSyncEnabled = MutableStateFlow(isAutoSyncEnabled())
    val autoSyncEnabled: Flow<Boolean> = _autoSyncEnabled.asStateFlow()

    private val _weighDefaults = MutableStateFlow(readWeighDefaults())
    val weighDefaults: Flow<WeighDefaults> = _weighDefaults.asStateFlow()

    private val _ttsEnabled = MutableStateFlow(prefs.getBoolean(KEY_TTS_ENABLED, true))
    val ttsEnabled: Flow<Boolean> = _ttsEnabled.asStateFlow()

    init {
        prefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_FONT_SCALE) {
                _fontScale.value = readFontScale()
            }
            if (key == KEY_LANGUAGE) {
                _language.value = readLanguage()
            }
            if (key == KEY_AUTO_SYNC_ENABLED) {
                _autoSyncEnabled.value = isAutoSyncEnabled()
            }
            if (key == KEY_TTS_ENABLED) {
                _ttsEnabled.value = prefs.getBoolean(KEY_TTS_ENABLED, true)
            }
            if (key == KEY_THEME_MODE) {
                _appThemeMode.value = readThemeMode()
            }
        }
    }

    fun getThemeMode(): AppThemeMode = readThemeMode()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _appThemeMode.value = mode
    }

    fun isTtsEnabled(): Boolean {
        return _ttsEnabled.value
    }

    fun setTtsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_ENABLED, enabled).apply()
        _ttsEnabled.value = enabled
    }

    fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, false)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply()
        _autoSyncEnabled.value = enabled
    }

    fun setFontScale(scale: FontScale) {
        prefs.edit().putString(KEY_FONT_SCALE, scale.name).apply()
        _fontScale.value = scale
    }

    fun getFontScale(): FontScale = readFontScale()

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.tag).apply()
        _language.value = language
    }

    fun getLanguage(): AppLanguage = readLanguage()

    fun getWeighDefaults(): WeighDefaults = readWeighDefaults()

    fun setWeighDefaults(d: WeighDefaults) {
        prefs.edit()
            .putBoolean(KEY_IMPURITY_IS_PERCENT, d.impurityIsPercent)
            .putBoolean(KEY_BAG_METHOD_IS_SAMPLING, d.bagMethodIsSampling)
            .putInt(KEY_BAG_SAMPLE_COUNT, d.bagSampleCount)
            .putFloat(KEY_BAG_SAMPLE_TOTAL_WEIGHT, d.bagSampleTotalWeight.toFloat())
            .putString(KEY_WEIGHT_INPUT_MODE, d.weightInputMode)
            .apply()
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
        val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.AUTO.name)
        return AppThemeMode.fromName(name)
    }
}

data class WeighDefaults(
    val impurityIsPercent: Boolean = false,
    val bagMethodIsSampling: Boolean = false,
    val bagSampleCount: Int = 0,
    val bagSampleTotalWeight: Double = 0.0,
    val weightInputMode: String = "SMALL"
)

