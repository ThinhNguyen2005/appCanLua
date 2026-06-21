package com.giathinh.canlua.util

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Mở URL bằng Chrome Custom Tab — UX trong app, theme phù hợp app, chia sẻ session với Chrome.
 * Fallback: nếu không có browser hỗ trợ → tự fallback sang ACTION_VIEW (CustomTabsIntent xử lý).
 */
object CustomTabsLauncher {
    fun open(context: Context, url: String, toolbarColor: Int? = null) {
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(false)
            .apply {
                if (toolbarColor != null) {
                    setDefaultColorSchemeParams(
                        CustomTabColorSchemeParams.Builder()
                            .setToolbarColor(toolbarColor)
                            .build()
                    )
                }
            }
            .build()
        runCatching { intent.launchUrl(context, Uri.parse(url)) }
    }
}
