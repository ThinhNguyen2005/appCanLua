package com.giathinh.canlua.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FirebaseRemoteConfigManager {
    const val KEY_EARLY_ADOPTER_ENABLED = "early_adopter_enabled"
    const val KEY_EARLY_ADOPTER_CUTOFF_MS = "early_adopter_cutoff_ms"
    const val KEY_TUTORIAL_VIDEO_URL = "tutorial_video_url"
    const val KEY_WEBSITE_URL = "website_url"

    private const val DEFAULT_EARLY_ADOPTER_ENABLED = false
    private const val DEFAULT_EARLY_ADOPTER_CUTOFF_MS = 0L
    private const val DEFAULT_TUTORIAL_VIDEO_URL = "https://canluavn.web.app/"
    private const val DEFAULT_WEBSITE_URL = "https://canluavn.web.app/"

    val earlyAdopterEnabled: Boolean get() = false
    val earlyAdopterCutoffMs: Long get() = 0L
    val tutorialVideoUrl: String get() = DEFAULT_TUTORIAL_VIDEO_URL
    val websiteUrl: String get() = DEFAULT_WEBSITE_URL

    val earlyAdopterEnabledFlow: StateFlow<Boolean> = MutableStateFlow(DEFAULT_EARLY_ADOPTER_ENABLED).asStateFlow()
    val cutoffMsFlow: StateFlow<Long?> = MutableStateFlow<Long?>(null).asStateFlow()
    val tutorialVideoUrlFlow: StateFlow<String> = MutableStateFlow(DEFAULT_TUTORIAL_VIDEO_URL).asStateFlow()
    val websiteUrlFlow: StateFlow<String> = MutableStateFlow(DEFAULT_WEBSITE_URL).asStateFlow()

    fun init(context: Context) {}
    suspend fun forceFetch(): Boolean = false
}
