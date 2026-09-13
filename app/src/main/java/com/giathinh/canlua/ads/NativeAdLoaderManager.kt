package com.giathinh.canlua.ads

import com.giathinh.canlua.util.PremiumState
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

sealed interface NativeAdUiState {
    data object Idle : NativeAdUiState
    data object Loading : NativeAdUiState
    data class Success(val nativeAd: NativeAd) : NativeAdUiState
    data class Error(val message: String, val code: String? = null) : NativeAdUiState
}

/**
 * Quản lý tải Native Ads ngoài chu trình Recomposition của Compose.
 *
 * Đảm bảo:
 *  - Premium User: Không bao giờ gửi request hay load ad.
 *  - Consent: Chỉ request khi User Messaging Platform cho phép (canRequestAds = true).
 *  - Lifecycle: destroy() ad cũ khi có ad mới hoặc khi giải phóng.
 *  - Test ID: Dùng mã vị trí quảng cáo thử nghiệm chính thức từ Google.
 */
@Singleton
class NativeAdLoaderManager @Inject constructor() {

    private val _adState = MutableStateFlow<NativeAdUiState>(NativeAdUiState.Idle)
    val adState: StateFlow<NativeAdUiState> = _adState.asStateFlow()

    private var activeNativeAd: NativeAd? = null
    private val isLoading = AtomicBoolean(false)

    /**
     * Tải quảng cáo Native Ad nếu thỏa mãn điều kiện:
     * 1. Người dùng không phải là Premium.
     * 2. UMP consent cho phép (canRequestAds = true).
     * 3. Hiện tại không đang trong tiến trình load.
     */
    fun loadAd(forceReload: Boolean = false) {
        // Nguyên tắc 19: User Premium tuyệt đối không request/load ad
        if (PremiumState.isPremium.value) {
            android.util.Log.d("NativeAdLoader", "User is Premium; skipping ad load.")
            destroyAd()
            return
        }

        // Nguyên tắc 14: Chỉ request ads khi UMP cho phép
        if (!AdConsentManager.canRequestAds.value) {
            android.util.Log.d("NativeAdLoader", "Cannot request ads: UMP consent not granted yet (canRequestAds = false).")
            return
        }

        if (!forceReload && (activeNativeAd != null || isLoading.get())) {
            android.util.Log.d("NativeAdLoader", "Ad already loaded or loading in progress.")
            return
        }

        isLoading.set(true)
        _adState.value = NativeAdUiState.Loading
        android.util.Log.d("NativeAdLoader", "Requesting Native Ad from AdMob...")

        val adTypes = listOf(NativeAd.NativeAdType.NATIVE)
        val adRequest = NativeAdRequest.Builder(
            AdConsentManager.TEST_NATIVE_AD_UNIT_ID,
            adTypes
        ).build()

        val callback = object : NativeAdLoaderCallback {
            override fun onNativeAdLoaded(nativeAd: NativeAd) {
                isLoading.set(false)
                android.util.Log.i("NativeAdLoader", "Native Ad loaded successfully: ${nativeAd.headline}")

                // Double check nếu trong lúc load người dùng vừa nâng cấp Premium
                if (PremiumState.isPremium.value) {
                    android.util.Log.d("NativeAdLoader", "User became Premium during load, destroying ad.")
                    nativeAd.destroy()
                    destroyAd()
                    return
                }

                // Hủy ad cũ trước khi gán ad mới
                activeNativeAd?.destroy()
                activeNativeAd = nativeAd
                _adState.value = NativeAdUiState.Success(nativeAd)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                isLoading.set(false)
                android.util.Log.w("NativeAdLoader", "Native Ad failed to load: [${adError.code.name}] ${adError.message}")
                _adState.value = NativeAdUiState.Error(
                    message = adError.message ?: "Failed to load native ad",
                    code = adError.code.name
                )
            }
        }

        try {
            NativeAdLoader.load(adRequest, callback)
        } catch (e: Exception) {
            isLoading.set(false)
            android.util.Log.e("NativeAdLoader", "Exception during NativeAdLoader.load", e)
            _adState.value = NativeAdUiState.Error(e.message ?: "Exception loading native ad")
        }
    }

    /**
     * Thu hồi và hủy NativeAd đang hoạt động để tránh rò rỉ bộ nhớ.
     */
    fun destroyAd() {
        activeNativeAd?.destroy()
        activeNativeAd = null
        isLoading.set(false)
        _adState.value = NativeAdUiState.Idle
    }
}
