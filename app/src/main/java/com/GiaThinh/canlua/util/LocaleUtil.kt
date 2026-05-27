package com.GiaThinh.canlua.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.GiaThinh.canlua.data.model.AppLanguage

object LocaleUtil {
    /**
     * Áp dụng ngôn ngữ cho toàn app.
     * Dùng AppCompatDelegate.setApplicationLocales thay cho updateConfiguration() deprecated.
     * - Android 13+: native per-app language (không cần restart Activity)
     * - Android < 13: AppCompat handle tự động qua resource configuration
     */
    fun applyLanguage(context: Context, language: AppLanguage) {
        val localeList = LocaleListCompat.forLanguageTags(language.tag)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
