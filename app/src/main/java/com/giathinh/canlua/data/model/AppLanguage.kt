package com.giathinh.canlua.data.model

import androidx.annotation.StringRes
import com.giathinh.canlua.R

enum class AppLanguage(
    val tag: String,
    @param:StringRes val labelRes: Int,
    @param:StringRes val nativeLabelRes: Int
) {
    VIETNAMESE("vi-VN", R.string.language_vietnamese, R.string.language_vietnamese_native),
    ENGLISH("en", R.string.language_english, R.string.language_english_native),
    KHMER("km", R.string.language_khmer, R.string.language_khmer_native),
    LAO("lo", R.string.language_lao, R.string.language_lao_native),
    CHINESE("zh-CN", R.string.language_chinese, R.string.language_chinese_native);

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            return entries.firstOrNull { it.tag == tag } ?: VIETNAMESE
        }
    }
}
