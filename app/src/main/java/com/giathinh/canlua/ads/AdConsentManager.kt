package com.giathinh.canlua.ads

import android.app.Activity
import android.content.Context
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Quản lý sự đồng thuận quyền riêng tư (UMP - User Messaging Platform)
 * và khởi tạo Google Mobile Ads (GMA) Next-Gen SDK.
 *
 * Đảm bảo:
 *  - Luôn kiểm tra consent trước khi request quảng cáo.
 *  - Native Validator được bật mặc định (không gọi disable).
 *  - Sử dụng Test AdMob App ID trong quá trình phát triển.
 *  - Hỗ trợ Privacy Options UI khi UMP yêu cầu.
 */
object AdConsentManager {

    private const val TAG = "AdConsentManager"

    /** Test AdMob App ID chính thức từ Google */
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"

    /** Test Native Ad Unit ID chính thức từ Google */
    const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

    private val isMobileAdsInitialized = AtomicBoolean(false)
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _isPrivacyOptionsRequired = MutableStateFlow(false)
    val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    /**
     * Thu thập sự đồng thuận UMP và khởi tạo MobileAds nếu được phép.
     * Gọi trong Activity.onCreate() (thường là MainActivity).
     */
    fun gatherConsent(activity: Activity, onConsentCompleted: ((Boolean) -> Unit)? = null) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        android.util.Log.w(TAG, "Consent form error: [${formError.errorCode}] ${formError.message}")
                    }
                    updateConsentState(activity, consentInformation)
                    onConsentCompleted?.invoke(consentInformation.canRequestAds())
                }
            },
            { requestConsentError ->
                android.util.Log.w(TAG, "Consent request error: [${requestConsentError.errorCode}] ${requestConsentError.message}")
                updateConsentState(activity, consentInformation)
                onConsentCompleted?.invoke(consentInformation.canRequestAds())
            }
        )

        // Kiểm tra ngay trạng thái hiện tại (nếu phiên trước đã có consent)
        if (consentInformation.canRequestAds()) {
            _canRequestAds.value = true
            initializeMobileAds(activity.applicationContext)
        }
    }

    private fun updateConsentState(context: Context, consentInformation: ConsentInformation) {
        val canRequest = consentInformation.canRequestAds()
        _canRequestAds.value = canRequest
        val isRequired = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        _isPrivacyOptionsRequired.value = isRequired
        android.util.Log.i(TAG, "Consent updated: canRequestAds = $canRequest, isPrivacyOptionsRequired = $isRequired")

        if (canRequest) {
            initializeMobileAds(context.applicationContext)
        }
    }

    /**
     * Khởi tạo Google Mobile Ads Next-Gen SDK.
     * Native Validator được giữ NGUYÊN trạng thái bật mặc định (không gọi .setNativeValidatorDisabled()).
     */
    fun initializeMobileAds(context: Context) {
        if (isMobileAdsInitialized.getAndSet(true)) return

        android.util.Log.i(TAG, "Initializing Google Mobile Ads Next-Gen SDK...")
        appScope.launch {
            try {
                val initConfig = InitializationConfig.Builder(TEST_APP_ID).build()
                MobileAds.initialize(context, initConfig) {
                    android.util.Log.i(TAG, "Google Mobile Ads initialized successfully.")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error initializing MobileAds", e)
                isMobileAdsInitialized.set(false)
            }
        }
    }

    /**
     * Mở form Tùy chọn quyền riêng tư (Privacy Options) khi người dùng yêu cầu từ Cài đặt.
     */
    fun showPrivacyOptionsForm(activity: Activity, onDismissed: ((FormError?) -> Unit)? = null) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
            updateConsentState(activity, consentInformation)
            onDismissed?.invoke(formError)
        }
    }
}
