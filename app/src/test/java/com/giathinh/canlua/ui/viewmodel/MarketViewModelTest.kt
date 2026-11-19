package com.giathinh.canlua.ui.viewmodel

import android.content.Context
import com.giathinh.canlua.R
import com.giathinh.canlua.ads.AdConsentManager
import com.giathinh.canlua.ads.MobileAdsSdkState
import com.giathinh.canlua.ads.NativeAdLoaderManager
import com.giathinh.canlua.ads.NativeAdUiState
import com.giathinh.canlua.data.model.PricePoint
import com.giathinh.canlua.data.model.RicePrice
import com.giathinh.canlua.repository.MarketRepository
import com.giathinh.canlua.util.PremiumState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MarketViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val marketRepository = mockk<MarketRepository>(relaxed = true)
    private val nativeAdLoaderManager = mockk<NativeAdLoaderManager>(relaxed = true)
    private val adConsentManager = mockk<AdConsentManager>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    private val canRequestAdsFlow = MutableStateFlow(false)
    private val sdkStateFlow = MutableStateFlow<MobileAdsSdkState>(MobileAdsSdkState.NotInitialized)
    private val pricesFlow = MutableStateFlow(
        listOf(
            RicePrice("1", "ST25", 8_200.0, 8_600.0, 8_400.0, "Sóc Trăng", 1_000L, trend = "UP"),
            RicePrice("2", "OM18", 7_500.0, 7_900.0, 7_700.0, "Cần Thơ", 1_000L, trend = "STABLE"),
            RicePrice("3", "IR50404", 6_400.0, 6_800.0, 6_600.0, "An Giang", 1_000L, trend = "DOWN")
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        setPremium(false)
        every { marketRepository.getAllPrices() } returns pricesFlow
        coEvery { marketRepository.seedMockDataIfEmpty() } returns Unit
        coEvery { marketRepository.refreshFromSupabase(any()) } returns Result.success(Unit)
        every { nativeAdLoaderManager.adState } returns MutableStateFlow(NativeAdUiState.Idle)
        every { adConsentManager.canRequestAds } returns canRequestAdsFlow
        every { adConsentManager.sdkState } returns sdkStateFlow
        every { context.getString(R.string.market_connection_error) } returns "Lỗi kết nối"
        every { context.getString(R.string.market_refresh_error, any()) } answers {
            val args = secondArg<Array<Any?>>()
            "Không thể cập nhật: ${args.firstOrNull()}"
        }
    }

    @After
    fun tearDown() {
        setPremium(false)
        Dispatchers.resetMain()
    }

    private fun setPremium(value: Boolean) {
        val field = PremiumState::class.java.getDeclaredField("_isPremium")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(PremiumState) as MutableStateFlow<Boolean>
        flow.value = value
    }

    private fun createViewModel(): MarketViewModel = MarketViewModel(
        marketRepository = marketRepository,
        nativeAdLoaderManager = nativeAdLoaderManager,
        adConsentManager = adConsentManager,
        context = context
    )

    @Test
    fun `filters by variety and trend case-insensitively and clearFilters restores full list`() = runTest(testDispatcher) {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.prices.collect()
        }
        advanceUntilIdle()
        assertEquals(3, vm.prices.value.size)

        vm.setVarietyFilter("om")
        advanceUntilIdle()
        assertEquals(listOf("OM18"), vm.prices.value.map { it.variety })

        vm.setVarietyFilter(null)
        vm.setTrendFilter("UP")
        advanceUntilIdle()
        assertEquals(listOf("ST25"), vm.prices.value.map { it.variety })

        vm.clearFilters()
        advanceUntilIdle()
        assertEquals(MarketFilter(), vm.filter.value)
        assertEquals(3, vm.prices.value.size)
    }

    @Test
    fun `history reacts to both selectVariety and setTimeRange changes`() = runTest(testDispatcher) {
        val history7d = listOf(
            PricePoint(id = 1L, variety = "OM18", date = 1_000L, priceMin = 7_400.0, priceMax = 7_800.0, priceAvg = 7_600.0)
        )
        val history30d = listOf(
            PricePoint(id = 1L, variety = "OM18", date = 500L, priceMin = 7_100.0, priceMax = 7_500.0, priceAvg = 7_300.0),
            PricePoint(id = 2L, variety = "OM18", date = 1_000L, priceMin = 7_400.0, priceMax = 7_800.0, priceAvg = 7_600.0)
        )
        every { marketRepository.getHistory("OM18", 7) } returns flowOf(history7d)
        every { marketRepository.getHistory("OM18", 30) } returns flowOf(history30d)

        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.history.collect()
        }
        advanceUntilIdle()
        assertEquals(emptyList<PricePoint>(), vm.history.value)

        vm.selectVariety("OM18")
        advanceUntilIdle()
        assertEquals(history7d, vm.history.value)

        vm.setTimeRange(30)
        advanceUntilIdle()
        assertEquals(30, vm.timeRangeDays.value)
        assertEquals(history30d, vm.history.value)
    }

    @Test
    fun `refreshFromSupabase sets localized refreshError on failure and clearRefreshError clears it`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        coEvery { marketRepository.refreshFromSupabase(forceRefresh = true) } returns Result.failure(RuntimeException("Timeout"))
        vm.refreshFromSupabase()
        advanceUntilIdle()

        assertFalse(vm.isLoading.value)
        assertEquals("Không thể cập nhật: Timeout", vm.refreshError.value)

        vm.clearRefreshError()
        assertNull(vm.refreshError.value)
    }

    @Test
    fun `ad eligibility automatically loads ad when consented and ready, and destroys ad on Premium upgrade`() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        // Initial state has canRequestAds = false -> destroyAd called, loadAd not called
        verify(atLeast = 1) { nativeAdLoaderManager.destroyAd() }
        verify(exactly = 0) { nativeAdLoaderManager.loadAd(any()) }

        canRequestAdsFlow.value = true
        sdkStateFlow.value = MobileAdsSdkState.Ready
        advanceUntilIdle()
        verify(exactly = 1) { nativeAdLoaderManager.loadAd(false) }

        setPremium(true)
        advanceUntilIdle()
        verify(atLeast = 2) { nativeAdLoaderManager.destroyAd() }
    }
}
