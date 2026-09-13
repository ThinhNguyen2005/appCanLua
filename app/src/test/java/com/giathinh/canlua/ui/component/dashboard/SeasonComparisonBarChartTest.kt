package com.giathinh.canlua.ui.component.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class SeasonComparisonBarChartTest {

    @Test
    fun `formatSeasonShort converts standard Vietnamese season names correctly`() {
        assertEquals("ĐX '26", formatSeasonShort("Đông Xuân 2026"))
        assertEquals("HT '25", formatSeasonShort("Hè Thu 2025"))
        assertEquals("TĐ '24", formatSeasonShort("Thu Đông 2024"))
    }

    @Test
    fun `formatSeasonShort handles 2-digit years`() {
        assertEquals("ĐX '26", formatSeasonShort("Đông Xuân 26"))
        assertEquals("HT '25", formatSeasonShort("Hè Thu 25"))
        assertEquals("TĐ '24", formatSeasonShort("Thu Đông 24"))
    }

    @Test
    fun `formatSeasonShort handles alternate seasons like Vu 3`() {
        assertEquals("V3 '25", formatSeasonShort("Vụ 3 2025"))
        assertEquals("V3 '24", formatSeasonShort("Vu 3 2024"))
    }

    @Test
    fun `formatSeasonShort degrades gracefully for custom names`() {
        assertEquals("MN '25", formatSeasonShort("Mùa Nổi 2025"))
        assertEquals("MN", formatSeasonShort("Mùa Nổi"))
        assertEquals("LM '24", formatSeasonShort("Lúa Mùa 2024"))
        assertEquals("Cust", formatSeasonShort("Custom"))
        assertEquals("", formatSeasonShort(""))
        assertEquals("", formatSeasonShort("   "))
    }
}
