package com.giathinh.canlua.util

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wrapper đơn giản cho Firebase Remote Config.
 * Cung cấp type-safe access đến các config key mà app sử dụng.
 *
 * Key hiện tại:
 *  - `early_adopter_enabled`   (Boolean) — bật/tắt feature Early Adopter
 *  - `early_adopter_cutoff_ms` (Long)   — timestamp cutoff (millis, UTC)
 *  - `tutorial_video_url`      (String) — URL video hướng dẫn (đổi không cần update app)
 *  - `website_url`             (String) — URL trang web giới thiệu (đổi không cần update app)
 *
 * Remote Config được fetch ở chế độ **thirty-minutes cache** trên device,
 * đủ để không gây lag khởi tạo mà vẫn cập nhật được trong vòng 30 phút.
 */
object FirebaseRemoteConfigManager {

    // Keys — giữ tại đây để dễ tìm/thay đổi
    const val KEY_EARLY_ADOPTER_ENABLED = "early_adopter_enabled"
    const val KEY_EARLY_ADOPTER_CUTOFF_MS = "early_adopter_cutoff_ms"
    const val KEY_TUTORIAL_VIDEO_URL = "tutorial_video_url"
    const val KEY_WEBSITE_URL = "website_url"

    // Defaults — fallback khi chưa fetch hoặc fetch lỗi
    private const val DEFAULT_EARLY_ADOPTER_ENABLED = true
    private const val DEFAULT_EARLY_ADOPTER_CUTOFF_MS = 0L
    private const val DEFAULT_TUTORIAL_VIDEO_URL = "https://canluavn.web.app/"
    private const val DEFAULT_WEBSITE_URL = "https://canluavn.web.app/"

    private var remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    /** StateFlow để UI reactive — emit khi fetch hoàn tất. */
    private val _earlyAdopterEnabled = MutableStateFlow(DEFAULT_EARLY_ADOPTER_ENABLED)
    val earlyAdopterEnabledFlow: StateFlow<Boolean> = _earlyAdopterEnabled.asStateFlow()

    private val _cutoffMs = MutableStateFlow<Long?>(null)
    val cutoffMsFlow: StateFlow<Long?> = _cutoffMs.asStateFlow()

    private val _tutorialVideoUrl = MutableStateFlow(DEFAULT_TUTORIAL_VIDEO_URL)
    val tutorialVideoUrlFlow: StateFlow<String> = _tutorialVideoUrl.asStateFlow()

    private val _websiteUrl = MutableStateFlow(DEFAULT_WEBSITE_URL)
    val websiteUrlFlow: StateFlow<String> = _websiteUrl.asStateFlow()

    /**
     * Gọi 1 lần trong Application.onCreate() — KHÔNG blocking UI thread.
     * Thực hiện fetch + activate trên background.
     */
    fun init(context: Context) {
        // remoteConfig đã được khởi tạo inline, không cần gán lại.

        // 30 phút cache (minimum theo Firebase quota-free).
        // 0 giờ = không dùng cache → luôn fetch mới → không recommended cho production.
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(30 * 60L)
            .build()

        remoteConfig.setConfigSettingsAsync(configSettings)

        // Defaults — dùng putAll để đặt nhiều key cùng lúc.
        remoteConfig.setDefaultsAsync(
            mapOf(
                KEY_EARLY_ADOPTER_ENABLED to DEFAULT_EARLY_ADOPTER_ENABLED,
                KEY_EARLY_ADOPTER_CUTOFF_MS to DEFAULT_EARLY_ADOPTER_CUTOFF_MS,
                KEY_TUTORIAL_VIDEO_URL to DEFAULT_TUTORIAL_VIDEO_URL,
                KEY_WEBSITE_URL to DEFAULT_WEBSITE_URL
            )
        )

        // Fetch + activate ngay để lấy giá trị mới nhất.
        // Nếu lỗi → dùng cached/default value (không crash).
        fetchAndActivate()
    }

    /**
     * Fetch config từ Firebase, activate ngay nếu có thay đổi.
     * Chạy trên IO coroutine — không block UI.
     */
    private fun fetchAndActivate() {
        remoteConfig.fetch()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    remoteConfig.activate()
                    // Cập nhật StateFlows sau khi activate thành công
                    _earlyAdopterEnabled.value = remoteConfig.getBoolean(KEY_EARLY_ADOPTER_ENABLED)
                    val cutoff = remoteConfig.getLong(KEY_EARLY_ADOPTER_CUTOFF_MS)
                    _cutoffMs.value = cutoff.takeIf { it > 0 }
                    val tutorialUrl = remoteConfig.getString(KEY_TUTORIAL_VIDEO_URL)
                    if (tutorialUrl.isNotBlank()) _tutorialVideoUrl.value = tutorialUrl
                    val webUrl = remoteConfig.getString(KEY_WEBSITE_URL)
                    if (webUrl.isNotBlank()) _websiteUrl.value = webUrl
                }
                // else: dùng cached/default, không cần xử lý
            }
    }

    /**
     * Force fetch ngay lập tức — dùng khi cần đảm bảo config mới nhất
     * (ví dụ: trước khi check premium eligibility).
     *
     * Returns giá trị mới nhất (activated) hoặc cached/default nếu chưa fetch xong.
     */
    suspend fun forceFetch(): Boolean {
        return try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            // Lỗi network/Firebase → dùng cached
            false
        }
    }

    // ─── Type-safe getters ───────────────────────────────────────────

    val earlyAdopterEnabled: Boolean
        get() = remoteConfig.getBoolean(KEY_EARLY_ADOPTER_ENABLED)

    /**
     * Trả về timestamp cutoff (milliseconds, UTC).
     * = 0 có nghĩa là không set trên Firebase → dùng default trong code.
     */
    val earlyAdopterCutoffMs: Long
        get() = remoteConfig.getLong(KEY_EARLY_ADOPTER_CUTOFF_MS)
            .takeIf { it > 0 }
            ?: DEFAULT_EARLY_ADOPTER_CUTOFF_MS

    /**
     * URL video hướng dẫn sử dụng — có thể đổi qua Firebase Console
     * (vd YouTube, Drive, web link...) mà không cần update app.
     */
    val tutorialVideoUrl: String
        get() = remoteConfig.getString(KEY_TUTORIAL_VIDEO_URL)
            .takeIf { it.isNotBlank() }
            ?: DEFAULT_TUTORIAL_VIDEO_URL

    /**
     * URL trang web giới thiệu — có thể đổi qua Firebase Console
     * (vd landing page, blog, marketing site...) mà không cần update app.
     */
    val websiteUrl: String
        get() = remoteConfig.getString(KEY_WEBSITE_URL)
            .takeIf { it.isNotBlank() }
            ?: DEFAULT_WEBSITE_URL
}
