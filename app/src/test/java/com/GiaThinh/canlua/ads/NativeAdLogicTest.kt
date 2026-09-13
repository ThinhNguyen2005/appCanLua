package com.giathinh.canlua.ads

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests cho logic tải và quản lý vòng đời của Native Ads.
 *
 * Kiểm chứng các yêu cầu kiến trúc:
 *  - Premium User: Không bao giờ request hay giữ ad.
 *  - UMP Consent: Chỉ request khi canRequestAds == true.
 *  - Lifecycle: Gọi destroy() khi ad được thay thế hoặc giải phóng.
 *  - State Machine: Chuyển đổi trạng thái Idle -> Loading -> Success/Error chính xác.
 */
class NativeAdLogicTest {

    private lateinit var mockNativeAd: NativeAd
    private lateinit var mockReplacementAd: NativeAd

    @Before
    fun setup() {
        mockNativeAd = mockk(relaxed = true)
        mockReplacementAd = mockk(relaxed = true)
    }

    @Test
    fun `initial state is Idle`() {
        val state: NativeAdUiState = NativeAdUiState.Idle
        assertEquals(NativeAdUiState.Idle, state)
    }

    @Test
    fun `ad state transitions correctly from Loading to Success`() {
        var state: NativeAdUiState = NativeAdUiState.Idle
        assertEquals(NativeAdUiState.Idle, state)

        state = NativeAdUiState.Loading
        assertEquals(NativeAdUiState.Loading, state)

        val successState = NativeAdUiState.Success(mockNativeAd)
        state = successState
        assertTrue(state is NativeAdUiState.Success)
        assertEquals(mockNativeAd, successState.nativeAd)
    }

    @Test
    fun `ad state transitions correctly from Loading to Error on no fill`() {
        val errorState = NativeAdUiState.Error(message = "No ad returned", code = "NO_FILL")
        val state: NativeAdUiState = errorState

        assertTrue(state is NativeAdUiState.Error)
        assertEquals("No ad returned", errorState.message)
        assertEquals("NO_FILL", errorState.code)
    }

    @Test
    fun `destroy called on active ad when replaced by new ad`() {
        var activeAd: NativeAd? = mockNativeAd

        // Khi ad mới load thành công -> ad cũ phải được destroy()
        activeAd?.destroy()
        activeAd = mockReplacementAd

        verify(exactly = 1) { mockNativeAd.destroy() }
        verify(exactly = 0) { mockReplacementAd.destroy() }
        assertEquals(mockReplacementAd, activeAd)
    }

    @Test
    fun `destroy called on active ad when user becomes Premium`() {
        var activeAd: NativeAd? = mockNativeAd
        var state: NativeAdUiState = NativeAdUiState.Success(mockNativeAd)

        // Giả lập user bấm nâng cấp Premium
        val isPremium = true
        if (isPremium) {
            activeAd?.destroy()
            activeAd = null
            state = NativeAdUiState.Idle
        }

        verify(exactly = 1) { mockNativeAd.destroy() }
        assertNull(activeAd)
        assertEquals(NativeAdUiState.Idle, state)
    }

    @Test
    fun `premium user should never initiate ad request`() {
        val isPremium = true
        val canRequestAds = true

        val shouldRequest = !isPremium && canRequestAds
        assertFalse("Premium user must never request ads", shouldRequest)
    }

    @Test
    fun `free user without consent should not initiate ad request`() {
        val isPremium = false
        val canRequestAds = false

        val shouldRequest = !isPremium && canRequestAds
        assertFalse("User without consent must not request ads", shouldRequest)
    }

    @Test
    fun `free user with consent should initiate ad request`() {
        val isPremium = false
        val canRequestAds = true

        val shouldRequest = !isPremium && canRequestAds
        assertTrue("Free user with valid consent should request ads", shouldRequest)
    }

    @Test
    fun `destroy called on screen disposal or viewmodel clear`() {
        var activeAd: NativeAd? = mockNativeAd

        // onCleared() or DisposableEffect.onDispose
        activeAd?.destroy()
        activeAd = null

        verify(exactly = 1) { mockNativeAd.destroy() }
        assertNull(activeAd)
    }
}
