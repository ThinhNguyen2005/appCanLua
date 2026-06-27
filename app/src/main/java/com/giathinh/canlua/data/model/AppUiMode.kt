package com.giathinh.canlua.data.model

import androidx.annotation.StringRes
import com.giathinh.canlua.R

enum class AppUiMode {
    SIMPLE,
    STANDARD;

    companion object {
        fun fromName(name: String?): AppUiMode =
            entries.firstOrNull { it.name == name } ?: STANDARD
    }
}
