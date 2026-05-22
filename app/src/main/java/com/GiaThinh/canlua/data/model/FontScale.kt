package com.GiaThinh.canlua.data.model

import androidx.annotation.StringRes
import com.GiaThinh.canlua.R

enum class FontScale(
    val scale: Float,
    @StringRes val labelRes: Int
) {
    SMALL(0.9f, R.string.font_small),
    NORMAL(1.0f, R.string.font_normal),
    LARGE(1.1f, R.string.font_large),
    XLARGE(1.2f, R.string.font_xlarge);

    companion object {
        fun fromName(name: String?): FontScale =
            values().firstOrNull { it.name == name } ?: NORMAL
    }
}
