package com.giathinh.canlua.ads

import com.giathinh.canlua.util.PremiumState
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests DRIVING logic production của [NativeAdLoaderManager] qua seam
 * [ConsentProvider] + [NativeAdLoaderBridge] — không còn bản sao điều kiện.
 *
 * Kiểm chứng:
 *  - Premium / chưa consent / SDK chưa Ready: không bao giờ gửi request.
 *  - Đủ điều kiện: gửi đúng 1 request với ad unit ID từ provider.
 *  - Mất consent hoặc lên Premium trong lúc tải: ad bị hủy, không xuất hiện trên UI.
 *  - Callback từ request lỗi thời bị vô hiệu hóa (generation counter).
 *  - destroyAd(): hủy ad đang giữ, chuyển UI về Idle.
 */
class NativeAdLogicTest {

    private lateinit var mockNativeAd: NativeAd
    private lateinit var mockReplacementAd: NativeAd

    @Before
    fun setup() {
        mockNativeAd = mockk(relaxed = true)
        mockReplacementAd = mockk(relaxed = true)
        setPremium(false)
    }

    @After
    fun teardown() {
        setPremium(false)
    }

    // ── Fakes tối giản cho seam ─────────────────────────────────────────────

    private class FakeConsentProvider : ConsentProvider {
        override val canRequestAds = MutableStateFlow(false)
        override val isPrivacyOptionsRequired = MutableStateFlow(false)
        override val sdkState = MutableStateFlow<MobileAdsSdkState>(MobileAdsSdkState.NotInitialized)
        override val adUnitId: String = "ca-app-pub-TEST/FAKE_UNIT"
        var initializeCalls = 0
        override fun initializeMobileAds() { initializeCalls++ }
        override fun showPrivacyOptionsForm(
            activity: android.app.Activity,
            onDismissed: ((com.google.android.ump.FormError?) -> Unit)?
        ) = Unit
    }

    private class FakeBridge : NativeAdLoaderBridge {
        val callbacks = mutableListOf<NativeAdLoaderCallback>()
        var requestCount = 0
        override fun load(adUnitId: String, callback: NativeAdLoaderCallback) {
            requestCount++
            callbacks.add(callback)
        }
    }

    /** PremiumState là object toàn cục; unit test JVM set trực tiếp StateFlow nền qua reflection. */
    private fun setPremium(value: Boolean) {
        val field = PremiumState::class.java.getDeclaredField("_isPremium")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(PremiumState) as MutableStateFlow<Boolean>
        flow.value = value
    }

    private fun eligibleProvider() = FakeConsentProvider().apply {
        canRequestAds.value = true
        sdkState.value = MobileAdsSdkState.Ready
    }

    // ── gating trước khi request ────────────────────────────────────────────

    @Test
    fun `premium user never initiates ad request`() {
        setPremium(true)
        val provider = eligibleProvider()
        val bridge = FakeBridge()
        val manager = NativeAdLoaderManager(provider, bridge)

        manager.loadAd()

        assertEquals(0, bridge.requestCount)
        assertEquals(NativeAdUiState.Idle, manager.adState.value)
    }

    @Test
    fun `user without consent does not initiate ad request`() {
        val provider = FakeConsentProvider().apply { sdkState.value = MobileAdsSdkState.Ready }
        val bridge = FakeBridge()
        val manager = NativeAdLoaderManager(provider, bridge)

        manager.loadAd()

        assertEquals(0, bridge.requestCount)
        assertEquals(NativeAdUiState.Idle, manager.adState.value)
    }

    @Test
    fun `sdk not ready defers ad request even with consent`() {
        val provider = FakeConsentProvider().apply {
            canRequestAds.value = true
            sdkState.value = MobileAdsSdkState.Initializing
        }
        val bridge = FakeBridge()
        val manager = NativeAdLoaderManager(provider, bridge)

        manager.loadAd()

        assertEquals(0, bridge.requestCount)
        assertEquals(NativeAdUiState.Idle, manager.adState.value)
    }

    @Test
    fun `eligible user triggers exactly one request with provider ad unit id`() {
        val provider = eligibleProvider()
        val bridge = FakeBridge()
        val manager = NativeAdLoaderManager(provider, bridge)

        manager.loadAd()
        manager.loadAd() // trùng lặp — phải bị chặn

        assertEquals(1, bridge.requestCount)
        assertEquals(NativeAdUiState.Loading, manager.adState.value)
    }

    // ── callback lifecycle ──────────────────────────────────────────────────

    @Test
    fun `loaded ad appears in UI when still eligible`() {
        val provider = eligibleProvider()
        val bridge = FakeBridge()
        val manager = NativeAdLoaderManager(provider, bridge)

        manager.loadAd()
        bridge.callbacks.last().onNativeAdLoaded(mockNativeAd)

        assertTrue(manager.adState.value is NativeAdUiState.Success)
        assertEquals(mockNativeAd, (manager.adState.value as NativeAdUiState.Success).nativeAd)
        verify(exactly = 0) { mockNativeAd.destroy() }
    }

    @Test
    fun `consent lost during load destroys arriving ad and keeps UI clear`() {
        val provider = eligibleProvider()
        val bridge = FakeBridge()
        val manager = NativeAdLoaderManager(provider, bridge)

        manager.loadAd()
        // Người dùng thu hồi consent trong lúc tải → MarketViewModel gọi destroyAd()
        provider.canRequestAds.value = false
        manager.destroyAd()
        bridge.callbacks.last().onNativeAdLoaded(mockNativeAd)

        verify(exactly = 1) { mockNativeAd.destroy() }
        assertEquals(NativeAdUiState.Idle, manager.adState.value)
    }

    @Test
    fun `premium upgrade during load destroys arriving ad`() {
        val provider = eligibleProvider()
        val bridge = FakeBridge()
        val manager = NativeAdLoaderManager(provider, bridge)

        manager.loadAd()
        setPremium(true)
        manager.destroyAd()
        bridge.callbacks.last().onNativeAdLoaded(mockNativeAd)

        verify(exactly = 1) { mockNativeAd.destroy() }
        assertTrue(manager.adState.value !is NativeAdUiState.Success)
    }

    @Test
    fun `callback from stale request cannot overwrite newer state`() {
        val provider = eligibleProvider()
        val bridge = FakeBridge()
        val manager = NativeAdLoaderManager(provider, bridge)

        // Request 1
        manager.loadAd()
        val staleCallback = bridge.callbacks[0]
        // forceReload -> Request 2 thay thế, request 1 thành lỗi thời
        manager.loadAd(forceReload = true)
        val freshCallback = bridge.callbacks[1]

        freshCallback.onNativeAdLoaded(mockReplacementAd)
        staleCallback.onNativeAdLoaded(mockNativeAd) // về muộn — phải bị bỏ qua + destroy

        verify(exactly = 1) { mockNativeAd.destroy() }
        assertEquals(mockReplacementAd, (manager.adState.value as NativeAdUiState.Success).nativeAd)
    }

    @Test
    fun `old error callback does not overwrite current success`() {
        val provider = eligibleProvider()
        val bridge = FakeBridge()
        val manager = NativeAdLoaderManager(provider, bridge)

        manager.loadAd()
        val staleCallback = bridge.callbacks[0]
        manager.loadAd(forceReload = true)
        bridge.callbacks[1].onNativeAdLoaded(mockReplacementAd)

        staleCallback.onAdFailedToLoad(loadError())

        assertTrue(manager.adState.value is NativeAdUiState.Success)
    }

    @Test
    fun `error from current request surfaces in UI`() {
        val provider = eligibleProvider()
        val bridge = FakeBridge()
        val manager = NativeAdLoaderManager(provider, bridge)

        manager.loadAd()
        bridge.callbacks.last().onAdFailedToLoad(loadError())

        val state = manager.adState.value
        assertTrue(state is NativeAdUiState.Error)
    }

    @Test
    fun `error callback after consent loss is ignored`() {
        val provider = eligibleProvider()
        val bridge = FakeBridge()
        val manager = NativeAdLoaderManager(provider, bridge)

        manager.loadAd()
        provider.canRequestAds.value = false
        manager.destroyAd()
        bridge.callbacks.last().onAdFailedToLoad(loadError())

        assertEquals(NativeAdUiState.Idle, manager.adState.value)
    }

    @Test
    fun `destroyAd destroys held ad and resets to idle`() {
        val provider = eligibleProvider()
        val bridge = FakeBridge()
        val manager = NativeAdLoaderManager(provider, bridge)

        manager.loadAd()
        bridge.callbacks.last().onNativeAdLoaded(mockNativeAd)
        manager.destroyAd()

        verify(exactly = 1) { mockNativeAd.destroy() }
        assertEquals(NativeAdUiState.Idle, manager.adState.value)
    }

    @Test
    fun `new ad replaces and destroys old ad`() {
        val provider = eligibleProvider()
        val bridge = FakeBridge()
        val manager = NativeAdLoaderManager(provider, bridge)

        manager.loadAd()
        bridge.callbacks[0].onNativeAdLoaded(mockNativeAd)
        manager.loadAd(forceReload = true)
        bridge.callbacks[1].onNativeAdLoaded(mockReplacementAd)

        verify(exactly = 1) { mockNativeAd.destroy() }
        verify(exactly = 0) { mockReplacementAd.destroy() }
        assertEquals(mockReplacementAd, (manager.adState.value as NativeAdUiState.Success).nativeAd)
    }

    @Test
    fun `blank ad unit id refuses request`() {
        val provider = FakeConsentProvider().apply {
            canRequestAds.value = true
            sdkState.value = MobileAdsSdkState.Ready
        }
        val blankProvider = object : ConsentProvider by provider {
            override val adUnitId = ""
        }
        val bridge = FakeBridge()
        val manager = NativeAdLoaderManager(blankProvider, bridge)

        manager.loadAd()

        assertEquals(0, bridge.requestCount)
    }

    private fun loadError(): LoadAdError = mockk<LoadAdError>(relaxed = true) {
        every { message } returns "Simulated no-fill"
        every { code } returns LoadAdError.ErrorCode.NO_FILL
    }
}
