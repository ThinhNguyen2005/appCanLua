package com.giathinh.canlua.ui.viewmodel

import com.giathinh.canlua.data.dao.RicePriceDao
import com.giathinh.canlua.data.model.RicePrice
import com.giathinh.canlua.repository.MarketRepository
import com.giathinh.canlua.repository.SupabasePriceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TraderBidsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val ricePriceDao = mockk<RicePriceDao>(relaxed = true)
    private val supabase = mockk<SupabasePriceRepository>(relaxed = true)
    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val mockUser = mockk<FirebaseUser>(relaxed = true) {
        every { uid } returns TRADER_UID
    }

    private lateinit var marketRepository: MarketRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        marketRepository = MarketRepository(ricePriceDao = ricePriceDao, supabase = supabase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `myBids emits mapped bids for signed-in trader from Room flow`() = runTest(testDispatcher) {
        val bidsFlow = MutableStateFlow(
            listOf(
                RicePrice(
                    id = "bid-1",
                    variety = "ST25",
                    priceMin = 8_200.0,
                    priceMax = 8_500.0,
                    priceAvg7d = 8_350.0,
                    region = "Sóc Trăng",
                    updatedAt = 5_000L,
                    traderId = TRADER_UID,
                    traderName = "Vựa Ba",
                    trend = "UP",
                    riceType = "lúa Khô"
                )
            )
        )
        every { auth.currentUser } returns mockUser
        every { ricePriceDao.getPricesByTrader(TRADER_UID) } returns bidsFlow

        val vm = TraderBidsViewModel(marketRepository = marketRepository, auth = auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.myBids.collect()
        }
        advanceUntilIdle()

        assertEquals(1, vm.myBids.value.size)
        val bid = vm.myBids.value.first()
        assertEquals("bid-1", bid.id)
        assertEquals("ST25", bid.variety)
        assertEquals(8_200.0, bid.priceMin, 0.01)
        assertEquals(8_500.0, bid.priceMax, 0.01)
        assertEquals(TRADER_UID, bid.traderId)
        assertEquals("Vựa Ba", bid.traderName)

        // Emit update from DAO -> ViewModel StateFlow updates reactively
        bidsFlow.value = emptyList()
        advanceUntilIdle()
        assertTrue(vm.myBids.value.isEmpty())
    }

    @Test
    fun `myBids stays empty when user is signed out without querying trader bids`() = runTest(testDispatcher) {
        every { auth.currentUser } returns null

        val vm = TraderBidsViewModel(marketRepository = marketRepository, auth = auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.myBids.collect()
        }
        advanceUntilIdle()

        assertTrue(vm.myBids.value.isEmpty())
        verify(exactly = 0) { ricePriceDao.getPricesByTrader(any()) }
    }

    @Test
    fun `submitBid and deleteBid report disabled status message and clearMessage resets uiState`() = runTest(testDispatcher) {
        every { auth.currentUser } returns mockUser
        every { ricePriceDao.getPricesByTrader(TRADER_UID) } returns MutableStateFlow(emptyList())

        val vm = TraderBidsViewModel(marketRepository = marketRepository, auth = auth)

        vm.submitBid(
            variety = "OM18",
            priceMin = 7_500.0,
            priceMax = 7_900.0,
            region = "Cần Thơ",
            trend = "UP",
            note = "Lúa sạch"
        )
        assertFalse(vm.uiState.value.isSaving)
        assertNotNull(vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.successMessage)

        vm.clearMessage()
        assertNull(vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.successMessage)

        vm.deleteBid("bid-1")
        assertFalse(vm.uiState.value.isSaving)
        assertNotNull(vm.uiState.value.errorMessage)

        vm.clearMessage()
        assertNull(vm.uiState.value.errorMessage)
    }

    private companion object {
        const val TRADER_UID = "trader-99"
    }
}
