package com.giathinh.canlua.ui.viewmodel

import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.repository.SettingsRepository
import com.giathinh.canlua.repository.SyncableCardRepository
import com.giathinh.canlua.repository.WeighDefaults
import com.giathinh.canlua.util.TextToSpeechManager
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
class WeightInputViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<SyncableCardRepository>(relaxed = true)
    private val ttsManager = mockk<TextToSpeechManager>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    private val defaultCard = Card(
        id = 1L,
        name = "Thẻ Test",
        date = Date(),
        bagWeight = 1.0,
        impurityWeight = 0.5,
        pricePerKg = 8000.0,
        moisturePercent = 14.5,
        isLocked = false
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { settingsRepository.ttsEnabled } returns flowOf(false)
        every { settingsRepository.isTtsEnabled() } returns false
        every { settingsRepository.weighDefaults } returns flowOf(WeighDefaults())
        every { settingsRepository.getWeighDefaults() } returns WeighDefaults()
        coEvery { repository.getCardById(1L) } returns defaultCard
        every { repository.getCardByIdFlow(1L) } returns flowOf(defaultCard)
        every { repository.getWeightEntriesByCardId(1L) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): WeightInputViewModel {
        return WeightInputViewModel(
            repository = repository,
            ttsManager = ttsManager,
            settingsRepository = settingsRepository
        )
    }

    @Test
    fun `loadCardById updates currentCard reactively`() = runTest(testDispatcher) {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.currentCard.collect()
        }
        vm.loadCardById(1L)
        advanceUntilIdle()

        assertEquals(defaultCard, vm.currentCard.value)
        assertEquals(14.5, vm.weightInputState.value.moisturePercent, 0.001)
    }

    @Test
    fun `updateCardBagWeight calls repository update and recalc`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val slot = slot<Card>()
        coEvery { repository.updateCard(capture(slot)) } coAnswers { }

        vm.updateCardBagWeight(1L, 1.2)
        advanceUntilIdle()

        coVerify { repository.updateCard(any()) }
        coVerify { repository.updateCardCalculations(1L) }
        assertEquals(1.2, slot.captured.bagWeight, 0.001)
    }

    @Test
    fun `updateCardBagMethod updates sampling method and recalcs`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val slot = slot<Card>()
        coEvery { repository.updateCard(capture(slot)) } coAnswers { }

        vm.updateCardBagMethod(1L, isSampling = true, sampleCount = 10, sampleTotalWeight = 11.5)
        advanceUntilIdle()

        coVerify { repository.updateCard(any()) }
        coVerify { repository.updateCardCalculations(1L) }
        assertTrue(slot.captured.bagMethodIsSampling)
        assertEquals(10, slot.captured.bagSampleCount)
        assertEquals(11.5, slot.captured.bagSampleTotalWeight, 0.001)
    }

    @Test
    fun `updateCardImpurityWeight updates impurity and recalcs`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val slot = slot<Card>()
        coEvery { repository.updateCard(capture(slot)) } coAnswers { }

        vm.updateCardImpurityWeight(1L, 2.5)
        advanceUntilIdle()

        coVerify { repository.updateCard(any()) }
        coVerify { repository.updateCardCalculations(1L) }
        assertEquals(2.5, slot.captured.impurityWeight, 0.001)
    }

    @Test
    fun `updateCardPricePerKg updates price and recalcs`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val slot = slot<Card>()
        coEvery { repository.updateCard(capture(slot)) } coAnswers { }

        vm.updateCardPricePerKg(1L, 8500.0)
        advanceUntilIdle()

        coVerify { repository.updateCard(any()) }
        coVerify { repository.updateCardCalculations(1L) }
        assertEquals(8500.0, slot.captured.pricePerKg, 0.001)
    }

    @Test
    fun `updateCardMoisture updates moisture and recalcs`() = runTest(testDispatcher) {
        val vm = createViewModel()
        val slot = slot<Card>()
        coEvery { repository.updateCard(capture(slot)) } coAnswers { }

        vm.updateCardMoisture(1L, 15.0)
        advanceUntilIdle()

        coVerify { repository.updateCard(any()) }
        coVerify { repository.updateCardCalculations(1L) }
        assertEquals(15.0, slot.captured.moisturePercent, 0.001)
    }

    @Test
    fun `toggleCardLock flips lock without recalculating`() = runTest(testDispatcher) {
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
    fun `addWeightEntryDirectly ignores invalid weight`() = runTest(testDispatcher) {
        val vm = createViewModel()

        vm.addWeightEntryDirectly(1L, -5.0)
        vm.addWeightEntryDirectly(1L, 0.0)
        vm.addWeightEntryDirectly(1L, 250.0) // > MAX_BAG_KG
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.addWeightEntryAndRecalc(any(), any()) }
    }

    @Test
    fun `addWeightEntryDirectly calls addWeightEntryAndRecalc for valid weight`() = runTest(testDispatcher) {
        val vm = createViewModel()
        coEvery { repository.addWeightEntryAndRecalc(1L, 50.5) } returns 1

        vm.addWeightEntryDirectly(1L, 50.5)
        advanceUntilIdle()

        coVerify { repository.addWeightEntryAndRecalc(1L, 50.5) }
    }

    @Test
    fun `keypad operations append and remove digits correctly`() {
        val vm = createViewModel()

        vm.appendToWeight("5")
        vm.appendToWeight("0")
        vm.appendToWeight(".")
        vm.appendToWeight("5")
        assertEquals("50.5", vm.weightInputState.value.currentWeight)

        // Cannot append second dot
        vm.appendToWeight(".")
        assertEquals("50.5", vm.weightInputState.value.currentWeight)

        // Can append second decimal digit
        vm.appendToWeight("2")
        assertEquals("50.52", vm.weightInputState.value.currentWeight)

        // Cannot append third decimal digit
        vm.appendToWeight("9")
        assertEquals("50.52", vm.weightInputState.value.currentWeight)

        // Remove last digit
        vm.removeLastDigit()
        assertEquals("50.5", vm.weightInputState.value.currentWeight)

        // Clear
        vm.clearWeightInput()
        assertEquals("", vm.weightInputState.value.currentWeight)
    }
}