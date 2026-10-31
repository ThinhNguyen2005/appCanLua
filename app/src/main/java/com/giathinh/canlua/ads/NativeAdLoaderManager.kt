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
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

sealed interface NativeAdUiState {
    data object Idle : NativeAdUiState
    data object Loading : NativeAdUiState
    data class Success(val nativeAd: NativeAd) : NativeAdUiState
    data class Error(val message: String, val code: String? = null) : NativeAdUiState
}

/**
 * Seam bọc API tĩnh NativeAdLoader.load() của SDK để unit test drive được
 * logic production của [NativeAdLoaderManager] mà không mockk static và không
 * phải dựng đối tượng nội bộ SDK trên JVM.
 */
interface NativeAdLoaderBridge {
    fun load(adUnitId: String, callback: NativeAdLoaderCallback)
}

/** Implementation thật delegate tới Next-Gen GMA SDK. */
class SdkNativeAdLoaderBridge @Inject constructor() : NativeAdLoaderBridge {
    override fun load(adUnitId: String, callback: NativeAdLoaderCallback) {
        val adTypes = listOf(NativeAd.NativeAdType.NATIVE)
        val adRequest = NativeAdRequest.Builder(adUnitId, adTypes).build()
        NativeAdLoader.load(adRequest, callback)
    }
}

/**
 * Quản lý tải Native Ads ngoài chu trình Recomposition của Compose.
 *
 * Đảm bảo:
 *  - Premium User: Không bao giờ gửi request hay load ad.
 *  - Consent: Chỉ request khi UMP cho phép (canRequestAds = true) VÀ SDK đã Ready.
 *  - Lifecycle: destroy() ad cũ khi có ad mới, khi mất consent/premium, hoặc khi giải phóng.
 *  - Callback lỗi thời (request cũ/trước khi revoke) không ghi đè trạng thái mới:
 *    dùng generation counter — mọi destroyAd()/load lại bump generation, callback sai
 *    generation sẽ hủy ad vừa nhận và return.
 *  - Ad Unit ID: debug = test ID Google, release = ID thật từ BuildConfig (qua ConsentProvider).
 */
@Singleton
class NativeAdLoaderManager @Inject constructor(
    private val consentProvider: ConsentProvider,
    private val loaderBridge: NativeAdLoaderBridge,
) {

    private val _adState = MutableStateFlow<NativeAdUiState>(NativeAdUiState.Idle)
    val adState: StateFlow<NativeAdUiState> = _adState.asStateFlow()

    private var activeNativeAd: NativeAd? = null
    private val isLoading = AtomicBoolean(false)

    /** Tăng mỗi khi load mới hoặc hủy ad — vô hiệu hóa callback của request lỗi thời. */
    private val generation = AtomicInteger(0)

    /** Điều kiện hiển thị ad hiện tại (premium/consent) — dùng cho callback kiểm tra lại. */
    private fun adsStillAllowed(): Boolean =
        !PremiumState.isPremium.value && consentProvider.canRequestAds.value

    /**
     * Tải quảng cáo Native Ad nếu thỏa mãn điều kiện:
     * 1. Người dùng không phải là Premium.
     * 2. UMP consent cho phép (canRequestAds = true).
     * 3. MobileAds SDK đã khởi tạo xong (sdkState = Ready).
     * 4. Hiện tại không đang trong tiến trình load và chưa có ad.
     */
    fun loadAd(forceReload: Boolean = false) {
        // Nguyên tắc 19: User Premium tuyệt đối không request/load ad
        if (PremiumState.isPremium.value) {
            android.util.Log.d("NativeAdLoader", "User is Premium; skipping ad load.")
            destroyAd()
            return
        }

        // Nguyên tắc 14: Chỉ request ads khi UMP cho phép
        if (!consentProvider.canRequestAds.value) {
            android.util.Log.d("NativeAdLoader", "Cannot request ads: UMP consent not granted yet (canRequestAds = false).")
            return
        }

        // Chi tải khi SDK đã sẵn sàng (callback hoàn tất init đã chạy)
        if (consentProvider.sdkState.value !is MobileAdsSdkState.Ready) {
            android.util.Log.d("NativeAdLoader", "MobileAds SDK not ready yet (state=${consentProvider.sdkState.value}); deferring ad load.")
            return
        }

        if (!forceReload && (activeNativeAd != null || isLoading.get())) {
            android.util.Log.d("NativeAdLoader", "Ad already loaded or loading in progress.")
            return
        }

        val adUnitId = consentProvider.adUnitId
        if (adUnitId.isBlank()) {
            android.util.Log.e("NativeAdLoader", "Ad unit ID is blank; refusing to request ads.")
            return
        }

        isLoading.set(true)
        _adState.value = NativeAdUiState.Loading
        val requestGeneration = generation.incrementAndGet()
        android.util.Log.d("NativeAdLoader", "Requesting Native Ad from AdMob...")

        val callback = object : NativeAdLoaderCallback {
            override fun onNativeAdLoaded(nativeAd: NativeAd) {
                isLoading.set(false)
                android.util.Log.i("NativeAdLoader", "Native Ad loaded successfully: ${nativeAd.headline}")

                // Callback lỗi thời (ad đã bị hủy/request mới đã thay) hoặc
                // điều kiện hiển thị không còn (premium, mất consent trong lúc load)
                // -> hủy ad vừa nhận, KHÔNG đưa vào UI.
                if (generation.get() != requestGeneration || !adsStillAllowed()) {
                    android.util.Log.d("NativeAdLoader", "Ad no longer allowed/stale; destroying received ad.")
                    nativeAd.destroy()
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
                if (generation.get() != requestGeneration || !adsStillAllowed()) return
                _adState.value = NativeAdUiState.Error(
                    message = adError.message ?: "Failed to load native ad",
                    code = adError.code.name
                )
            }
        }

        try {
            loaderBridge.load(adUnitId, callback)
        } catch (e: Exception) {
            isLoading.set(false)
            android.util.Log.e("NativeAdLoader", "Exception during NativeAdLoader.load", e)
            _adState.value = NativeAdUiState.Error(e.message ?: "Exception loading native ad")
        }
    }

    /**
     * Thu hồi và hủy NativeAd đang hoạt động để tránh rò rỉ bộ nhớ.
     * Bump generation để callback của request đang chạy (nếu về sau) bị vô hiệu hóa.
     */
    fun destroyAd() {
        generation.incrementAndGet()
        activeNativeAd?.destroy()
        activeNativeAd = null
        isLoading.set(false)
        _adState.value = NativeAdUiState.Idle
    }
}
