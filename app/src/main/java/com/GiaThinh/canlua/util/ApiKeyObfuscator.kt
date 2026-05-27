package com.GiaThinh.canlua.util

import android.util.Base64

object ApiKeyObfuscator {
    /**
     * Giải mã API key dạng Base64 thành String thô ở runtime.
     * Giúp tránh việc các API key bị lưu plaintext trong DEX và dễ dàng trích xuất bằng APK decompilers.
     */
    fun decode(base64Str: String): String {
        if (base64Str.isBlank()) return ""
        return try {
            String(Base64.decode(base64Str, Base64.DEFAULT)).trim()
        } catch (e: Exception) {
            ""
        }
    }
}
