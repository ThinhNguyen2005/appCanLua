package com.GiaThinh.canlua.data.model

import androidx.annotation.StringRes
import com.GiaThinh.canlua.R

/**
 * Chế độ giao diện (theme) của app.
 *
 * @param labelRes String resource cho label hiển thị trong Settings.
 */
enum class AppThemeMode(
    @param:StringRes val labelRes: Int
) {
    LIGHT(R.string.theme_light),
    HIGH_CONTRAST(R.string.theme_high_contrast),
    OLED(R.string.theme_oled),
    AUTO(R.string.theme_auto);

    companion object {
        fun fromName(name: String?): AppThemeMode =
            entries.firstOrNull { it.name == name } ?: AUTO
    }
}
