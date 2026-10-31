package com.giathinh.canlua.ads

import android.app.Activity
import android.content.Context
import com.giathinh.canlua.BuildConfig
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trạng thái khởi tạo Mobile Ads Next-Gen SDK — phân biệt rõ 3 pha để
 * UI/loader chỉ tải quảng cáo khi SDK thật sự sẵn sàng.
 */
sealed interface MobileAdsSdkState {
    data object NotInitialized : MobileAdsSdkState
    data object Initializing : MobileAdsSdkState
    data object Ready : MobileAdsSdkState
}

/**
 * Điểm trừu tượng hóa trạng thái consent/SDK để test logic production
 * (NativeAdLoaderManager) mà không dựng thêm framework.
 */
interface ConsentProvider {
    val canRequestAds: StateFlow<Boolean>
    val isPrivacyOptionsRequired: StateFlow<Boolean>
    val sdkState: StateFlow<MobileAdsSdkState>
    val adUnitId: String

    fun initializeMobileAds()
    fun showPrivacyOptionsForm(activity: Activity, onDismissed: ((FormError?) -> Unit)? = null)
}

/**
 * Quản lý sự đồng thuận quyền riêng tư (UMP - User Messaging Platform)
 * và khởi tạo Google Mobile Ads (GMA) Next-Gen SDK.
 *
 * Đảm bảo:
 *  - Luôn kiểm tra consent trước khi request quảng cáo (luồng UMP chính thức:
 *    requestConsentInfoUpdate -> loadAndShowConsentFormIfRequired -> showPrivacyOptionsForm).
 *  - Chỉ đánh dấu Ready trong callback hoàn tất khởi tạo thật sự của SDK.
 *  - App ID nhất quán: cùng một giá trị BuildConfig.ADMOB_APP_ID được truyền vào
 *    MobileAds.initialize và manifest placeholder APPLICATION_ID.
 *  - Debug dùng test ID chính thức của Google; release lấy ID thật từ local.properties
 *    (build release fail nếu thiếu/placeholder/test ID — xem checkAdmobReleaseIds).
 *  - Không giữ Activity: chỉ nhận Activity tại thời điểm gọi API form, lưu applicationContext.
 */
@Singleton
class AdConsentManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : ConsentProvider {

    companion object {
        private const val TAG = "AdConsentManager"
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val canRequestAds = MutableStateFlow(false)
    override val isPrivacyOptionsRequired = MutableStateFlow(false)
    override val sdkState = MutableStateFlow<MobileAdsSdkState>(MobileAdsSdkState.NotInitialized)

    private var consentInformation: ConsentInformation? = null
    private val initRequested = AtomicBoolean(false)

    /** Test ID chính thức của Google cho debug; release override bằng ID thật (BuildConfig). */
    override val adUnitId: String =
        if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/2247696110" else BuildConfig.ADMOB_NATIVE_UNIT_ID

    /**
     * Thu thập sự đồng thuận UMP và khởi tạo MobileAds nếu được phép.
     * Gọi trong Activity.onCreate() (thường là MainActivity).
     */
    fun gatherConsent(activity: Activity, onConsentCompleted: ((Boolean) -> Unit)? = null) {
        val info = UserMessagingPlatform.getConsentInformation(activity).also { consentInformation = it }
        val params = ConsentRequestParameters.Builder().build()

        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        android.util.Log.w(TAG, "Consent form error: [${formError.errorCode}] ${formError.message}")
                    }
                    updateConsentState(info)
                    onConsentCompleted?.invoke(info.canRequestAds())
                }
            },
            { requestConsentError ->
                android.util.Log.w(TAG, "Consent request error: [${requestConsentError.errorCode}] ${requestConsentError.message}")
                // Theo mẫu chính thức của Google: vẫn dùng consent của phiên trước nếu có.
                updateConsentState(info)
                onConsentCompleted?.invoke(info.canRequestAds())
            }
        )

        // Kiểm tra ngay trạng thái hiện tại (nếu phiên trước đã có consent)
        if (info.canRequestAds()) {
            canRequestAds.value = true
            initializeMobileAds()
        }
    }

    private fun updateConsentState(info: ConsentInformation) {
        val canRequest = info.canRequestAds()
        canRequestAds.value = canRequest
        // UMP không cung cấp listener trạng thái consent; đọc trực tiếp sau mỗi lần
        // cập nhật thông tin consent / đóng form privacy options.
        val isRequired = info.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        isPrivacyOptionsRequired.value = isRequired
        android.util.Log.i(TAG, "Consent updated: canRequestAds = $canRequest, isPrivacyOptionsRequired = $isRequired")

        if (canRequest) {
            initializeMobileAds()
        }
    }

    /**
     * Khởi tạo Google Mobile Ads Next-Gen SDK — tối đa MỘT lần khởi tạo đang chạy.
     * Chỉ chuyển sang Ready trong callback onAdapterInitializationComplete (API thật:
     * MobileAds.initialize(context, config, OnAdapterInitializationCompleteListener)).
     * Native Validator được giữ NGUYÊN trạng thái bật mặc định (không gọi setNativeValidatorDisabled()).
     */
    override fun initializeMobileAds() {
        if (sdkState.value is MobileAdsSdkState.Ready || !initRequested.compareAndSet(false, true)) return
        sdkState.value = MobileAdsSdkState.Initializing

        android.util.Log.i(TAG, "Initializing Google Mobile Ads Next-Gen SDK...")
        appScope.launch {
            try {
                val initConfig = InitializationConfig.Builder(BuildConfig.ADMOB_APP_ID).build()
                MobileAds.initialize(appContext, initConfig) { status ->
                    if (MobileAds.isInitialized) {
                        sdkState.value = MobileAdsSdkState.Ready
                        android.util.Log.i(TAG, "Google Mobile Ads initialized successfully.")
                    } else {
                        // Khôi phục khả năng thử lại ở lần gatherConsent/consent-change kế tiếp.
                        sdkState.value = MobileAdsSdkState.NotInitialized
                        initRequested.set(false)
                        android.util.Log.w(TAG, "MobileAds init completed but not initialized: ${status.adapterStatusMap}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error initializing MobileAds", e)
                sdkState.value = MobileAdsSdkState.NotInitialized
                initRequested.set(false)
            }
        }
    }

    /**
     * Mở form Tùy chọn quyền riêng tư (Privacy Options) khi người dùng yêu cầu từ Cài đặt.
     * UMP trả về cùng một thể singleton ConsentInformation cho một context, nên reuse
     * thể đã lưu thay vì gọi lại getConsentInformation.
     */
    override fun showPrivacyOptionsForm(activity: Activity, onDismissed: ((FormError?) -> Unit)?) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            (consentInformation ?: UserMessagingPlatform.getConsentInformation(activity.applicationContext))
                ?.let { updateConsentState(it) }
            onDismissed?.invoke(formError)
        }
    }
}
