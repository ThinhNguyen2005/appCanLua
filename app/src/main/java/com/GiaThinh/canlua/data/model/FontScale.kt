package com.GiaThinh.canlua.data.model

enum class FontScale(val scale: Float, val label: String) {
    SMALL(0.9f, "Nhỏ"),
    NORMAL(1.0f, "Chuẩn"),
    LARGE(1.1f, "Lớn"),
    XLARGE(1.2f, "Rất lớn");

    companion object {
        fun fromName(name: String?): FontScale =
            values().firstOrNull { it.name == name } ?: NORMAL
    }
}

