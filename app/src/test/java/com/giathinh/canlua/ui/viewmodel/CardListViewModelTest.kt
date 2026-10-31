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
        assertEquals(2, vm.cards.value.size)
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
    fun `createNewCard inserts card and handles deposit transaction`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val cardSlot = slot<Card>()
        coEvery { repository.insertCard(capture(cardSlot)) } returns 99L

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

        coVerify { repository.insertCard(any()) }
        coVerify { repository.insertTransaction(any()) }
        coVerify { repository.updateCardCalculations(99L) }
        assertEquals("Chú 7 Bến Lức", cardSlot.captured.name)
        assertEquals(8500.0, cardSlot.captured.pricePerKg, 0.001)
        assertEquals(5_000_000.0, cardSlot.captured.depositAmount, 0.001)
    }
}

