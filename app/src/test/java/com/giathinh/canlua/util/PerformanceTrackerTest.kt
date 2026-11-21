package com.giathinh.canlua.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PerformanceTrackerTest {

    @Test
    fun `sanitizeRouteName normalizes dynamic detail routes to match TrackScreenRender`() {
        assertEquals("card_detail", PerformanceTracker.sanitizeRouteName("card_detail/{cardId}"))
        assertEquals("card_detail", PerformanceTracker.sanitizeRouteName("card_detail"))
        assertEquals("weight_input", PerformanceTracker.sanitizeRouteName("weight_input/{cardId}"))
        assertEquals("weight_input", PerformanceTracker.sanitizeRouteName("weight_input"))
    }

    @Test
    fun `sanitizeRouteName maps profile screens consistently to account`() {
        assertEquals("account", PerformanceTracker.sanitizeRouteName("account"))
        assertEquals("account", PerformanceTracker.sanitizeRouteName("farmer_profile"))
        assertEquals("account", PerformanceTracker.sanitizeRouteName("trader_profile"))
    }

    @Test
    fun `sanitizeRouteName handles blank or punctuation-only routes safely`() {
        assertEquals("unknown", PerformanceTracker.sanitizeRouteName(""))
        assertEquals("unknown", PerformanceTracker.sanitizeRouteName("///"))
        assertEquals("market", PerformanceTracker.sanitizeRouteName("market"))
    }
}
