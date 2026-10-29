package com.giathinh.canlua.ui.viewmodel

import com.giathinh.canlua.data.location.GeoPoint
import com.giathinh.canlua.data.location.LocationProvider
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.Transaction
import com.giathinh.canlua.data.model.TransactionType
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.repository.SyncableCardRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class CardDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<SyncableCardRepository>(relaxed = true)
    private val locationProvider = mockk<LocationProvider>(relaxed = true)

    private val defaultCard = Card(
        id = 1L,
        name = "Thẻ Nguyễn Văn A",
        date = Date(),
        bagWeight = 1.0,
        impurityWeight = 0.5,
        pricePerKg = 8000.0,
        depositAmount = 5_000_000.0,
        paidAmount = 10_000_000.0,
        moisturePercent = 14.5,
        riceVariety = "Đài Thơm 8",
        seasonLabel = "Đông Xuân 2026",
        traderPhone = "0912345678",
        fieldAddress = "Ấp 1, Xã Tân Lập",
        isLocked = false
    )

    private val defaultWeightEntries = listOf(
        WeightEntry(id = 10L, cardId = 1L, weight = 50.0),
        WeightEntry(id = 11L, cardId = 1L, weight = 50.5)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.getCardById(1L) } returns defaultCard
        every { repository.getCardByIdFlow(1L) } returns flowOf(defaultCard)
        every { repository.getWeightEntriesByCardId(1L) } returns flowOf(defaultWeightEntries)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): CardDetailViewModel {
        return CardDetailViewModel(
            repository = repository,
            locationProvider = locationProvider
        )
    }

    @Test
    fun `loadCardById updates currentCard and weightEntries reactively`() = runTest(testDispatcher) {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.currentCard.collect()
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.weightEntries.collect()
        }

        vm.loadCardById(1L)
        advanceUntilIdle()

        assertEquals(defaultCard, vm.currentCard.value)
        assertEquals(defaultWeightEntries, vm.weightEntries.value)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `updateCard with deposit change inserts DEPOSIT transaction and recalcs`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val txSlot = slot<Transaction>()
        coEvery { repository.insertTransaction(capture(txSlot)) } returns 100L

        val modifiedCard = defaultCard.copy(depositAmount = 7_000_000.0) // Tăng 2 triệu
        vm.updateCard(modifiedCard)
        advanceUntilIdle()

        coVerify { repository.updateCard(modifiedCard) }
        coVerify { repository.insertTransaction(any()) }
        coVerify { repository.updateCardCalculations(1L) }
        assertEquals(2_000_000.0, txSlot.captured.amount, 0.001)
        assertEquals(TransactionType.DEPOSIT, txSlot.captured.type)
    }

    @Test
    fun `updateCard with paid change inserts PAYMENT transaction and recalcs`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val txSlot = slot<Transaction>()
        coEvery { repository.insertTransaction(capture(txSlot)) } returns 101L

        val modifiedCard = defaultCard.copy(paidAmount = 15_000_000.0) // Trả thêm 5 triệu
        vm.updateCard(modifiedCard)
        advanceUntilIdle()

        coVerify { repository.updateCard(modifiedCard) }
        coVerify { repository.insertTransaction(any()) }
        coVerify { repository.updateCardCalculations(1L) }
        assertEquals(5_000_000.0, txSlot.captured.amount, 0.001)
        assertEquals(TransactionType.PAYMENT, txSlot.captured.type)
    }

    @Test
    fun `updateCard without financial changes does not insert transaction or recalc`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val modifiedCard = defaultCard.copy(traderPhone = "0987654321")

        vm.updateCard(modifiedCard)
        advanceUntilIdle()

        coVerify { repository.updateCard(modifiedCard) }
        coVerify(exactly = 0) { repository.insertTransaction(any()) }
        coVerify(exactly = 0) { repository.updateCardCalculations(1L) }
    }

    @Test
    fun `updateCardName updates trimmed name when not blank`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val slot = slot<Card>()
        coEvery { repository.updateCard(capture(slot)) } coAnswers { }

        vm.updateCardName(1L, "  Trần Thị B  ")
        advanceUntilIdle()

        coVerify { repository.updateCard(any()) }
        coVerify(exactly = 0) { repository.updateCardCalculations(1L) }
        assertEquals("Trần Thị B", slot.captured.name)
    }

    @Test
    fun `updateCardName ignores blank name`() = runTest(testDispatcher) {
        val vm = createViewModel()

        vm.updateCardName(1L, "   ")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.updateCard(any()) }
    }

    @Test
    fun `updateCardBagWeight updates bag weight and recalcs`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val slot = slot<Card>()
        coEvery { repository.updateCard(capture(slot)) } coAnswers { }

        vm.updateCardBagWeight(1L, 1.25)
        advanceUntilIdle()

        coVerify { repository.updateCard(any()) }
        coVerify { repository.updateCardCalculations(1L) }
        assertEquals(1.25, slot.captured.bagWeight, 0.001)
    }

    @Test
    fun `updateCardImpurityWeight updates impurity weight and recalcs`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val slot = slot<Card>()
        coEvery { repository.updateCard(capture(slot)) } coAnswers { }

        vm.updateCardImpurityWeight(1L, 2.0)
        advanceUntilIdle()

        coVerify { repository.updateCard(any()) }
        coVerify { repository.updateCardCalculations(1L) }
        assertEquals(2.0, slot.captured.impurityWeight, 0.001)
    }

    @Test
    fun `updateCardPricePerKg updates price and recalcs`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val slot = slot<Card>()
        coEvery { repository.updateCard(capture(slot)) } coAnswers { }

        vm.updateCardPricePerKg(1L, 8800.0)
        advanceUntilIdle()

        coVerify { repository.updateCard(any()) }
        coVerify { repository.updateCardCalculations(1L) }
        assertEquals(8800.0, slot.captured.pricePerKg, 0.001)
    }

    @Test
    fun `updateCardMoisture updates moisture and recalcs`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val slot = slot<Card>()
        coEvery { repository.updateCard(capture(slot)) } coAnswers { }

        vm.updateCardMoisture(1L, 16.0)
        advanceUntilIdle()

        coVerify { repository.updateCard(any()) }
        coVerify { repository.updateCardCalculations(1L) }
        assertEquals(16.0, slot.captured.moisturePercent, 0.001)
    }

    @Test
    fun `updateCardRiceVariety updates variety without recalc`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val slot = slot<Card>()
        coEvery { repository.updateCard(capture(slot)) } coAnswers { }

        vm.updateCardRiceVariety(1L, "ST25")
        advanceUntilIdle()

        coVerify { repository.updateCard(any()) }
        coVerify(exactly = 0) { repository.updateCardCalculations(1L) }
        assertEquals("ST25", slot.captured.riceVariety)
    }

    @Test
    fun `updateCardSeasonLabel updates season without recalc`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val slot = slot<Card>()
        coEvery { repository.updateCard(capture(slot)) } coAnswers { }

        vm.updateCardSeasonLabel(1L, "Hè Thu 2026")
        advanceUntilIdle()

        coVerify { repository.updateCard(any()) }
        coVerify(exactly = 0) { repository.updateCardCalculations(1L) }
        assertEquals("Hè Thu 2026", slot.captured.seasonLabel)
    }

    @Test
    fun `toggleCardLock flips lock without recalc`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val slot = slot<Card>()
        coEvery { repository.updateCard(capture(slot)) } coAnswers { }

        vm.toggleCardLock(1L)
        advanceUntilIdle()

        coVerify { repository.updateCard(any()) }
        coVerify(exactly = 0) { repository.updateCardCalculations(1L) }
        assertTrue(slot.captured.isLocked)
    }

    @Test
    fun `addPayment inserts payment transaction and recalcs if unlocked`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val txSlot = slot<Transaction>()
        coEvery { repository.insertTransaction(capture(txSlot)) } returns 200L

        vm.addPayment(1L, 1_000_000.0, "Trả thêm đợt 2")
        advanceUntilIdle()

        coVerify { repository.insertTransaction(any()) }
        coVerify { repository.updateCardCalculations(1L) }
        assertEquals(1_000_000.0, txSlot.captured.amount, 0.001)
        assertEquals(TransactionType.PAYMENT, txSlot.captured.type)
        assertEquals("Trả thêm đợt 2", txSlot.captured.description)
    }

    @Test
    fun `addPayment ignores payment when card is locked`() = runTest(testDispatcher) {
        val lockedCard = defaultCard.copy(isLocked = true)
        coEvery { repository.getCardById(2L) } returns lockedCard

        val vm = createViewModel()
        vm.addPayment(2L, 500_000.0)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.insertTransaction(any()) }
        coVerify(exactly = 0) { repository.updateCardCalculations(any()) }
    }

    @Test
    fun `refreshFieldLocation updates coordinates and address`() = runTest(testDispatcher) {
        val vm = createViewModel()
        coEvery { locationProvider.getCurrentLocation(forceFresh = true) } returns GeoPoint(10.123, 105.456)
        coEvery { locationProvider.reverseGeocode(10.123, 105.456) } returns "Mỹ Tho, Tiền Giang"

        val slot = slot<Card>()
        coEvery { repository.updateCard(capture(slot)) } coAnswers { }

        vm.refreshFieldLocation(1L)
        advanceUntilIdle()

        coVerify { repository.updateCard(any()) }
        assertEquals(10.123, slot.captured.latitude ?: 0.0, 0.0001)
        assertEquals(105.456, slot.captured.longitude ?: 0.0, 0.0001)
        assertEquals("Mỹ Tho, Tiền Giang", slot.captured.fieldAddress)
    }

    @Test
    fun `deleteCard calls repository delete`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.deleteCard(defaultCard)
        advanceUntilIdle()

        coVerify { repository.deleteCard(defaultCard) }
    }

    @Test
    fun `updateWeightEntry calls repository update and recalcs`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val entry = WeightEntry(id = 50L, cardId = 1L, weight = 52.0)

        vm.updateWeightEntry(entry)
        advanceUntilIdle()

        coVerify { repository.updateWeightEntry(entry) }
        coVerify { repository.updateCardCalculations(1L) }
    }

    @Test
    fun `deleteWeightEntry calls repository delete and recalcs`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val entry = WeightEntry(id = 50L, cardId = 1L, weight = 52.0)

        vm.deleteWeightEntry(entry)
        advanceUntilIdle()

        coVerify { repository.deleteWeightEntry(entry) }
        coVerify { repository.updateCardCalculations(1L) }
    }
}

