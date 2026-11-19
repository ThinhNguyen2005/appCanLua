package com.giathinh.canlua.data.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScaleProtocolParserTest {

    private val parser = CompositeScaleParser()

    @Test
    fun `composite parser decodes generic ASCII, Yaohua reversed frames, and rejects invalid inputs`() {
        data class Case(
            val rawLine: String,
            val expectedWeight: Double?,
            val expectedStable: Boolean?
        )

        val cases = listOf(
            // Standard stable / unstable ASCII formats
            Case("ST,GS,+  52.40kg", 52.40, true),
            Case("ST,NT,+0052.40 kg", 52.40, true),
            Case("US,NT,  12.5 kg", 12.5, false),
            Case("MO,GS,+ 48.10kg", 48.10, false),
            Case("ST,GS,+  0.00kg", 0.0, true),
            Case("\u0002ST,GS,  53.75kg\u0003\r\n", 53.75, true),
            // Bare numbers & comma decimal separator
            Case("52.40", 52.40, null),
            Case("52,40", 52.40, null),
            Case("+000.00kg", 0.0, null),
            // Yaohua reversed frames
            Case("=04.2500", 52.40, null),
            Case("=00.0000", 0.0, null),
            Case("=5.8400 ", 48.5, null),
            // Invalid / negative / overload / garbage frames
            Case("US,GS,-  1.20kg", null, null),
            Case("=00.51-", null, null),
            Case("=INVALID", null, null),
            Case("=", null, null),
            Case("OL", null, null),
            Case("ST,OL,+999.9kg", null, null),
            Case("ERR 01", null, null),
            Case("", null, null),
            Case("   \r\n", null, null),
            Case("hello rice", null, null),
            Case("ST,GS,+999.99kg", null, null)
        )

        cases.forEach { case ->
            val actual = parser.parse(case.rawLine)
            if (case.expectedWeight == null) {
                assertNull("Expected null for '${case.rawLine}'", actual)
            } else {
                assertNotNull("Expected valid reading for '${case.rawLine}'", actual)
                assertEquals("Weight mismatch for '${case.rawLine}'", case.expectedWeight, actual!!.weightKg, 0.001)
                assertEquals("Stability mismatch for '${case.rawLine}'", case.expectedStable, actual.isStable)
            }
        }
    }
}
