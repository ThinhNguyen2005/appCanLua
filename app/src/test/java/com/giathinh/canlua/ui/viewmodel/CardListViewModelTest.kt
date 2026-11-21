package com.giathinh.canlua.ui.viewmodel

import com.giathinh.canlua.data.location.LocationProvider
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.repository.SettingsRepository
import com.giathinh.canlua.repository.SyncableCardRepository
import com.giathinh.canlua.repository.WeighDefaults
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class CardListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<SyncableCardRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val locationProvider = mockk<LocationProvider>(relaxed = true)

    private val card1 = Card(
        id = 1L,
        name = "Nông dân A",
        riceVariety = "Đài Thơm 8",
        seasonLabel = "Đông Xuân 2026",
        date = Date()
    )
    private val card2 = Card(
        id = 2L,
        name = "Nông dân B",
        riceVariety = "ST25",
        seasonLabel = "Hè Thu 2026",
        date = Date()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getAllCards() } returns flowOf(listOf(card1, card2))
        every { repository.getDistinctRiceVarieties() } returns flowOf(listOf("Đài Thơm 8", "ST25"))
        every { repository.getSuggestedRiceVarieties() } returns flowOf(listOf("Đài Thơm 8", "OM18"))
        every { repository.getDistinctSeasons() } returns flowOf(listOf("Đông Xuân 2026", "Hè Thu 2026"))
        every { settingsRepository.getWeighDefaults() } returns WeighDefaults()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): CardListViewModel {
        return CardListViewModel(
            repository = repository,
            settingsRepository = settingsRepository,
            locationProvider = locationProvider
        )
    }

    @Test
    fun `cards flow emits all cards initially`() = runTest(testDispatcher) {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.cards.collect()
        }
        advanceUntilIdle()

        assertEquals(2, vm.cards.value.size)
    }

    @Test
    fun `setVarietyFilter filters cards correctly`() = runTest(testDispatcher) {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.cards.collect()
        }
        advanceUntilIdle()

        vm.setVarietyFilter("ST25")
        advanceUntilIdle()

        assertEquals(1, vm.cards.value.size)
        assertEquals("ST25", vm.cards.value[0].riceVariety)
    }

    @Test
    fun `setSeasonFilter filters cards correctly`() = runTest(testDispatcher) {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.cards.collect()
        }
        advanceUntilIdle()

        vm.setSeasonFilter("Đông Xuân 2026")
        advanceUntilIdle()

        assertEquals(1, vm.cards.value.size)
        assertEquals("Đông Xuân 2026", vm.cards.value[0].seasonLabel)
    }

    @Test
    fun `clearFilters resets filters and returns all cards`() = runTest(testDispatcher) {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.cards.collect()
        }
        advanceUntilIdle()

        vm.setVarietyFilter("ST25")
        advanceUntilIdle()
        assertEquals(1, vm.cards.value.size)

        vm.clearFilters()
        advanceUntilIdle()
        assertNull(vm.selectedVarietyFilter.value)
        assertNull(vm.selectedSeasonFilter.value)
        assertEquals(CardQuickFilter.ALL, vm.quickFilter.value)
        assertEquals("", vm.searchQuery.value)
        assertEquals(2, vm.cards.value.size)
    }

    @Test
    fun `setQuickFilter filters unlocked, unpriced, and unpaid cards`() = runTest(testDispatcher) {
        val pricedLockedPaid = Card(
            id = 10L,
            name = "Chú Hai",
            traderName = "Lái Tám",
            riceVariety = "OM18",
            date = Date(),
            pricePerKg = 8000.0,
            totalAmount = 8_000_000.0,
            remainingAmount = 0.0,
            isPaid = true,
            isLocked = true
        )
        val unpricedOpen = Card(
            id = 11L,
            name = "Cô Ba",
            traderName = "Lái Chín",
            riceVariety = "ST25",
            date = Date(),
            pricePerKg = 0.0,
            totalAmount = 0.0,
            remainingAmount = 0.0,
            isPaid = false,
            isLocked = false
        )
        val unpaidOpen = Card(
            id = 12L,
            name = "Anh Tư",
            traderName = "Lái Mười",
            riceVariety = "Đài Thơm 8",
            date = Date(),
            pricePerKg = 7500.0,
            totalAmount = 5_000_000.0,
            remainingAmount = 2_000_000.0,
            isPaid = false,
            isLocked = false
        )
        every { repository.getAllCards() } returns flowOf(listOf(pricedLockedPaid, unpricedOpen, unpaidOpen))
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.cards.collect()
        }
        advanceUntilIdle()

        vm.setQuickFilter(CardQuickFilter.UNLOCKED)
        advanceUntilIdle()
        assertEquals(listOf(11L, 12L), vm.cards.value.map { it.id })

        vm.setQuickFilter(CardQuickFilter.UNPRICED)
        advanceUntilIdle()
        assertEquals(listOf(11L), vm.cards.value.map { it.id })

        vm.setQuickFilter(CardQuickFilter.UNPAID)
        advanceUntilIdle()
        assertEquals(listOf(12L), vm.cards.value.map { it.id })
    }

    @Test
    fun `setSearchQuery filters by card name, trader name, or rice variety case-insensitively`() = runTest(testDispatcher) {
        val c1 = Card(id = 1L, name = "Trường Nguyễn", traderName = "Lái A", riceVariety = "ST25", date = Date())
        val c2 = Card(id = 2L, name = "Bảy Hậu Giang", traderName = "Thương lái Minh", riceVariety = "OM18", date = Date())
        every { repository.getAllCards() } returns flowOf(listOf(c1, c2))
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.cards.collect()
        }
        advanceUntilIdle()

        vm.setSearchQuery("trường")
        advanceUntilIdle()
        assertEquals(listOf(1L), vm.cards.value.map { it.id })

        vm.setSearchQuery("MINH")
        advanceUntilIdle()
        assertEquals(listOf(2L), vm.cards.value.map { it.id })

        vm.setSearchQuery("st25")
        advanceUntilIdle()
        assertEquals(listOf(1L), vm.cards.value.map { it.id })
    }

    @Test
    fun `deleteCard calls repository delete and emits Success event`() = runTest(testDispatcher) {
        val vm = createViewModel()
        var receivedEvent: DeleteCardEvent? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.deleteEvents.collect { receivedEvent = it }
        }

        vm.deleteCard(card1)
        advanceUntilIdle()

        coVerify { repository.deleteCard(card1) }
        assertTrue(receivedEvent is DeleteCardEvent.Success)
        assertEquals("Nông dân A", (receivedEvent as DeleteCardEvent.Success).cardName)
    }

    @Test
    fun `createNewCard uses atomic createCardWithDeposit`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val cardSlot = slot<Card>()
        val depositSlot = slot<Double>()
        coEvery { repository.createCardWithDeposit(capture(cardSlot), capture(depositSlot), any()) } returns
            com.giathinh.canlua.domain.CardMutationResult.Success(card1)

        vm.createNewCard(
            name = "Chú 7 Bến Lức",
            cccd = "082099123456",
            traderName = "Thương lái Năm",
            pricePerKg = 8500.0,
            depositAmount = 5_000_000.0,
            riceVariety = "OM18",
            moisturePercent = 14.0,
            seasonLabel = "Đông Xuân 2026"
        )
        advanceUntilIdle()

        coVerify { repository.createCardWithDeposit(any(), 5_000_000.0, any()) }
        coVerify(exactly = 0) { repository.insertTransaction(any()) }
        assertEquals("Chú 7 Bến Lức", cardSlot.captured.name)
        assertEquals(8500.0, cardSlot.captured.pricePerKg, 0.001)
        assertEquals(5_000_000.0, depositSlot.captured, 0.001)
    }
}

