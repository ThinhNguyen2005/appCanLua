package com.giathinh.canlua.ui.viewmodel

import com.giathinh.canlua.data.model.Profile
import com.giathinh.canlua.data.model.SeasonStats
import com.giathinh.canlua.data.model.TraderStat
import com.giathinh.canlua.data.model.VarietyStat
import com.giathinh.canlua.repository.AiChatRepository
import com.giathinh.canlua.repository.CardRepository
import com.giathinh.canlua.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<CardRepository>(relaxed = true)
    private val aiChatRepository = mockk<AiChatRepository>(relaxed = true)
    private val profileRepository = mockk<ProfileRepository>(relaxed = true)

    private val seasonsFlow = MutableStateFlow<List<String>>(emptyList())
    private val comparisonFlow = MutableStateFlow<List<SeasonStats>>(emptyList())

    private val heThuStats = SeasonStats(
        season = "Hè Thu 2025",
        cardCount = 4,
        totalNetWeight = 12_000.0,
        totalRevenue = 96_000_000.0,
        totalPaid = 80_000_000.0,
        totalRemaining = 16_000_000.0,
        avgPricePerKg = 8_000.0,
        avgMoisture = 14.5,
        totalBags = 240,
        wetCardCount = 3,
        dryCardCount = 1
    )

    private val dongXuanStats = SeasonStats(
        season = "Đông Xuân 2025",
        cardCount = 3,
        totalNetWeight = 10_000.0,
        totalRevenue = 80_000_000.0,
        totalPaid = 80_000_000.0,
        totalRemaining = 0.0,
        avgPricePerKg = 8_000.0,
        avgMoisture = 13.8,
        totalBags = 200,
        wetCardCount = 0,
        dryCardCount = 3
    )

    private val overallEmptyStats = SeasonStats(
        season = "Tất cả",
        cardCount = 0,
        totalNetWeight = 0.0,
        totalRevenue = 0.0,
        totalPaid = 0.0,
        totalRemaining = 0.0,
        avgPricePerKg = 0.0,
        avgMoisture = 0.0,
        totalBags = 0
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getDistinctSeasons() } returns seasonsFlow
        every { repository.getAllSeasonsComparison() } returns comparisonFlow
        every { repository.getOverallStats() } returns flowOf(overallEmptyStats)
        every { repository.getSeasonStats("Hè Thu 2025") } returns flowOf(heThuStats)
        every { repository.getSeasonStats("Đông Xuân 2025") } returns flowOf(dongXuanStats)
        every { repository.getVarietyBreakdown("Hè Thu 2025") } returns flowOf(
            listOf(VarietyStat(variety = "OM18", weight = 12_000.0, count = 4))
        )
        every { repository.getVarietyBreakdown("Đông Xuân 2025") } returns flowOf(
            listOf(VarietyStat(variety = "ST25", weight = 10_000.0, count = 3))
        )
        every { repository.getTopTraders("Hè Thu 2025") } returns flowOf(
            listOf(TraderStat(traderName = "Thương lái Ba", revenue = 96_000_000.0, deals = 4))
        )
        every { repository.getTopTraders("Đông Xuân 2025") } returns flowOf(emptyList())
        every { profileRepository.latestProfile() } returns flowOf(
            Profile(uid = "user-1", name = "Chú Năm", region = "Cần Thơ")
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): DashboardViewModel = DashboardViewModel(
        repository = repository,
        aiChatRepository = aiChatRepository,
        profileRepository = profileRepository
    )

    @Test
    fun `empty repository leaves seasons empty and aggregates at zero`() = runTest(testDispatcher) {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.dashboardData.collect()
        }
        advanceUntilIdle()

        val data = vm.dashboardData.value
        assertTrue(data.seasons.isEmpty())
        assertNull(data.selectedSeason)
        assertEquals(0, data.currentStats?.cardCount)
        assertTrue(data.isAggregated)
        assertNull(data.weightDeltaPercent)
        assertNull(data.revenueDeltaPercent)
    }

    @Test
    fun `auto selects newest season and computes aggregates plus delta against previous season`() = runTest(testDispatcher) {
        seasonsFlow.value = listOf("Hè Thu 2025", "Đông Xuân 2025")
        comparisonFlow.value = listOf(heThuStats, dongXuanStats)

        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.dashboardData.collect()
        }
        advanceUntilIdle()

        val data = vm.dashboardData.value
        assertEquals("Hè Thu 2025", data.selectedSeason)
        assertEquals(heThuStats, data.currentStats)
        assertEquals(dongXuanStats, data.previousStats)
        assertEquals(20.0, data.weightDeltaPercent ?: 0.0, 0.01)
        assertEquals(20.0, data.revenueDeltaPercent ?: 0.0, 0.01)
        assertEquals(listOf("OM18"), data.varieties.map { it.variety })
        assertEquals(listOf("Thương lái Ba"), data.topTraders.map { it.traderName })
    }

    @Test
    fun `selectSeason updates active stats, clears delta for oldest season, and resets AI analysis`() = runTest(testDispatcher) {
        seasonsFlow.value = listOf("Hè Thu 2025", "Đông Xuân 2025")
        comparisonFlow.value = listOf(heThuStats, dongXuanStats)
        coEvery { aiChatRepository.analyzeSeason(any(), any()) } returns Result.success("## Kết quả")

        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.dashboardData.collect()
        }
        advanceUntilIdle()

        vm.analyzeWithAi()
        advanceUntilIdle()
        val analyzed = vm.aiAnalysis.first { it is AiAnalysisState.Success }
        assertTrue(analyzed is AiAnalysisState.Success)

        vm.selectSeason("Đông Xuân 2025")
        advanceUntilIdle()

        val data = vm.dashboardData.value
        assertEquals("Đông Xuân 2025", data.selectedSeason)
        assertEquals(dongXuanStats, data.currentStats)
        assertNull(data.previousStats)
        assertNull(data.weightDeltaPercent)
        assertNull(data.revenueDeltaPercent)
        assertEquals(AiAnalysisState.Idle, data.aiAnalysis)
    }

    @Test
    fun `analyzeWithAi emits NO_DATA when current stats empty and handles success or failure`() = runTest(testDispatcher) {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.dashboardData.collect()
        }
        advanceUntilIdle()

        vm.analyzeWithAi()
        assertEquals(AiAnalysisState.Error(AiAnalysisState.Error.Reason.NO_DATA), vm.aiAnalysis.value)

        // Populate season data -> test failure then success
        seasonsFlow.value = listOf("Hè Thu 2025", "Đông Xuân 2025")
        comparisonFlow.value = listOf(heThuStats, dongXuanStats)
        advanceUntilIdle()

        coEvery { aiChatRepository.analyzeSeason(any(), any()) } returns Result.failure(RuntimeException("Timeout"))
        vm.analyzeWithAi()
        advanceUntilIdle()
        val failed = vm.aiAnalysis.first { it is AiAnalysisState.Error } as AiAnalysisState.Error
        assertEquals(AiAnalysisState.Error.Reason.FAILED, failed.reason)
        assertEquals("Timeout", failed.detail)

        vm.resetAiAnalysis()
        coEvery { aiChatRepository.analyzeSeason(any(), any()) } returns Result.success("## Vụ Hè Thu tốt")
        vm.analyzeWithAi()
        advanceUntilIdle()
        val success = vm.aiAnalysis.first { it is AiAnalysisState.Success } as AiAnalysisState.Success
        assertEquals("## Vụ Hè Thu tốt", success.markdown)
        coVerify(exactly = 2) { aiChatRepository.analyzeSeason(match { it.contains("Hè Thu 2025") }, any()) }
    }
}
