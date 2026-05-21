package com.GiaThinh.canlua.util

import android.content.Context
import android.content.res.Configuration
import com.GiaThinh.canlua.data.model.AppLanguage
import java.util.Locale

object LocaleUtil {
    fun applyLanguage(context: Context, language: AppLanguage) {
        val locale = Locale.forLanguageTag(language.tag)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
    }
}
