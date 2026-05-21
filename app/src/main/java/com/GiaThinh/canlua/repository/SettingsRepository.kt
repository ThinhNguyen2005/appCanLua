package com.GiaThinh.canlua.repository

import android.content.Context
import android.content.SharedPreferences
import com.GiaThinh.canlua.data.model.AppLanguage
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

    private val _fontScale = MutableStateFlow(readFontScale())
    val fontScale: Flow<FontScale> = _fontScale.asStateFlow()

    private val _language = MutableStateFlow(readLanguage())
    val language: Flow<AppLanguage> = _language.asStateFlow()

    init {
        prefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_FONT_SCALE) {
                _fontScale.value = readFontScale()
            }
            if (key == KEY_LANGUAGE) {
                _language.value = readLanguage()
            }
        }
    }
    
    fun isTtsEnabled(): Boolean {
        return prefs.getBoolean(KEY_TTS_ENABLED, false)
    }
    
    fun setTtsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_ENABLED, enabled).apply()
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

    private fun readFontScale(): FontScale {
        val name = prefs.getString(KEY_FONT_SCALE, FontScale.NORMAL.name)
        return FontScale.fromName(name)
    }

    private fun readLanguage(): AppLanguage {
        val tag = prefs.getString(KEY_LANGUAGE, AppLanguage.VIETNAMESE.tag)
        return AppLanguage.fromTag(tag)
    }
}

