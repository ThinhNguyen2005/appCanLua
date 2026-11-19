package com.giathinh.canlua.ui.viewmodel

import com.giathinh.canlua.data.location.GeoPoint
import com.giathinh.canlua.data.location.LocationProvider
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.Transaction
import com.giathinh.canlua.data.model.TransactionType
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.domain.CardMutationResult
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
    fun `updateCard forwards deposit target to atomic editCardFinancials`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val cardSlot = slot<Card>()
        coEvery { repository.editCardFinancials(capture(cardSlot), any()) } returns CardMutationResult.Success(defaultCard)

        val modifiedCard = defaultCard.copy(depositAmount = 7_000_000.0)
        vm.updateCard(modifiedCard)
        advanceUntilIdle()

        coVerify { repository.editCardFinancials(any(), any()) }
        assertEquals(7_000_000.0, cardSlot.captured.depositAmount, 0.001)
    }

    @Test
    fun `updateCard forwards paid target to atomic editCardFinancials`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val cardSlot = slot<Card>()
        coEvery { repository.editCardFinancials(capture(cardSlot), any()) } returns CardMutationResult.Success(defaultCard)

        val modifiedCard = defaultCard.copy(paidAmount = 15_000_000.0)
        vm.updateCard(modifiedCard)
        advanceUntilIdle()

        coVerify { repository.editCardFinancials(any(), any()) }
        assertEquals(15_000_000.0, cardSlot.captured.paidAmount, 0.001)
    }

    @Test
    fun `updateCard without financial changes still uses atomic command`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val modifiedCard = defaultCard.copy(traderPhone = "0987654321")

        vm.updateCard(modifiedCard)
        advanceUntilIdle()

        coVerify { repository.editCardFinancials(modifiedCard, any()) }
        coVerify(exactly = 0) { repository.insertTransaction(any()) }
    }

    @Test
    fun `updateCardName patches trimmed name without recalc`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val transform = slot<(Card) -> Card>()
        coEvery { repository.patchCard(any(), any(), any(), capture(transform)) } returns CardMutationResult.Success(defaultCard)

        vm.updateCardName(1L, "  Trần Thị B  ")
        advanceUntilIdle()

        coVerify { repository.patchCard(1L, any(), false, any()) }
        assertEquals("Trần Thị B", transform.captured(defaultCard).name)
    }

    @Test
    fun `updateCardName ignores blank name`() = runTest(testDispatcher) {
        val vm = createViewModel()

        vm.updateCardName(1L, "   ")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.patchCard(any(), any(), any(), any()) }
    }

    @Test
    fun `updateCardBagWeight patches bag weight with recalc`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val transform = slot<(Card) -> Card>()
        coEvery { repository.patchCard(any(), any(), any(), capture(transform)) } returns CardMutationResult.Success(defaultCard)

        vm.updateCardBagWeight(1L, 1.25)
        advanceUntilIdle()

        coVerify { repository.patchCard(1L, any(), true, any()) }
        assertEquals(1.25, transform.captured(defaultCard).bagWeight, 0.001)
    }

    @Test
    fun `updateCardImpurityWeight patches impurity with recalc`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val transform = slot<(Card) -> Card>()
        coEvery { repository.patchCard(any(), any(), any(), capture(transform)) } returns CardMutationResult.Success(defaultCard)

        vm.updateCardImpurityWeight(1L, 2.0)
        advanceUntilIdle()

        coVerify { repository.patchCard(1L, any(), true, any()) }
        assertEquals(2.0, transform.captured(defaultCard).impurityWeight, 0.001)
    }

    @Test
    fun `updateCardPricePerKg patches price with recalc`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val transform = slot<(Card) -> Card>()
        coEvery { repository.patchCard(any(), any(), any(), capture(transform)) } returns CardMutationResult.Success(defaultCard)

        vm.updateCardPricePerKg(1L, 8800.0)
        advanceUntilIdle()

        coVerify { repository.patchCard(1L, any(), true, any()) }
        assertEquals(8800.0, transform.captured(defaultCard).pricePerKg, 0.001)
    }

    @Test
    fun `updateCardMoisture patches moisture with recalc`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val transform = slot<(Card) -> Card>()
        coEvery { repository.patchCard(any(), any(), any(), capture(transform)) } returns CardMutationResult.Success(defaultCard)

        vm.updateCardMoisture(1L, 16.0)
        advanceUntilIdle()

        coVerify { repository.patchCard(1L, any(), true, any()) }
        assertEquals(16.0, transform.captured(defaultCard).moisturePercent, 0.001)
    }

    @Test
    fun `updateCardRiceVariety patches variety without recalc`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val transform = slot<(Card) -> Card>()
        coEvery { repository.patchCard(any(), any(), any(), capture(transform)) } returns CardMutationResult.Success(defaultCard)

        vm.updateCardRiceVariety(1L, "ST25")
        advanceUntilIdle()

        coVerify { repository.patchCard(1L, any(), false, any()) }
        assertEquals("ST25", transform.captured(defaultCard).riceVariety)
    }

    @Test
    fun `updateCardSeasonLabel patches season without recalc`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val transform = slot<(Card) -> Card>()
        coEvery { repository.patchCard(any(), any(), any(), capture(transform)) } returns CardMutationResult.Success(defaultCard)

        vm.updateCardSeasonLabel(1L, "Hè Thu 2026")
        advanceUntilIdle()

        coVerify { repository.patchCard(1L, any(), false, any()) }
        assertEquals("Hè Thu 2026", transform.captured(defaultCard).seasonLabel)
    }

    @Test
    fun `toggleCardLock uses explicit setCardLocked command`() = runTest(testDispatcher) {
        val vm = createViewModel()
        coEvery { repository.setCardLocked(any(), any(), any()) } returns CardMutationResult.Success(defaultCard.copy(isLocked = true))

        vm.toggleCardLock(1L)
        advanceUntilIdle()

        coVerify { repository.setCardLocked(1L, defaultCard.revision, true) }
        coVerify(exactly = 0) { repository.updateCardCalculations(1L) }
    }

    @Test
    fun `addPayment delegates to atomic idempotent command`() = runTest(testDispatcher) {
        val vm = createViewModel()
        coEvery { repository.addPayment(any(), any(), any(), any()) } returns CardMutationResult.Success(defaultCard)

        vm.addPayment(1L, 1_000_000.0, "Trả thêm đợt 2")
        advanceUntilIdle()

        coVerify { repository.addPayment(1L, 1_000_000.0, "Trả thêm đợt 2", any()) }
        coVerify(exactly = 0) { repository.insertTransaction(any()) }
    }

    @Test
    fun `addPayment delegates lock enforcement to repository`() = runTest(testDispatcher) {
        val vm = createViewModel()
        coEvery { repository.addPayment(any(), any(), any(), any()) } returns
            CardMutationResult.Failure(com.giathinh.canlua.domain.CardError.Locked)

        vm.addPayment(2L, 500_000.0)
        advanceUntilIdle()

        // VM không tự ghi ledger; repository atomic quyết định khóa/hợp lệ.
        coVerify { repository.addPayment(2L, 500_000.0, any(), any()) }
        coVerify(exactly = 0) { repository.insertTransaction(any()) }
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

