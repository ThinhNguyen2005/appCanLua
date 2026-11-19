package com.giathinh.canlua.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Unit tests cho [MoneyFormatter.toWordsCore] và [MoneyFormatter.formatVndShort].
 *
 * Critical test: nếu logic sai → nông dân đọc nhầm số tiền → khiếu nại.
 */
class MoneyFormatterTest {

    private lateinit var originalLocale: Locale

    private val viLabels = MoneyLabels(
        billion = "tỷ",
        million = "triệu",
        hundredThousand = "trăm nghìn",
        thousand = "nghìn",
        dong = "đồng",
        hundred = "trăm",
    )

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    // === toWordsCore ===

    @Test
    fun `toWordsCore handles zero, negative, and tiny values as empty string`() {
        listOf(0.0, 0.4, -100.0, -999_999_999_999.0).forEach { amount ->
            assertEquals("amount=$amount", "", MoneyFormatter.toWordsCore(amount, viLabels))
        }
    }

    @Test
    fun `toWordsCore formats dong and nghin tiers accurately`() {
        listOf(
            1.0 to "1 đồng",
            999.0 to "999 đồng",
            1_000.0 to "1 nghìn",
            1_500.7 to "1 nghìn",
            50_000.0 to "50 nghìn",
            100_000.0 to "1 trăm nghìn",
            150_000.0 to "150 nghìn",
            200_000.0 to "2 trăm nghìn",
            500_000.0 to "5 trăm nghìn",
            800_000.0 to "8 trăm nghìn",
            900_000.0 to "9 trăm nghìn",
            999_000.0 to "999 nghìn"
        ).forEach { (amount, expected) ->
            assertEquals("amount=$amount", expected, MoneyFormatter.toWordsCore(amount, viLabels))
        }
    }

    @Test
    fun `toWordsCore formats trieu tier including tram-nghin shortcuts and boundaries`() {
        listOf(
            1_000_000.0 to "1 triệu",
            5_000_000.0 to "5 triệu",
            1_200_000.0 to "1 triệu 2",
            1_500_000.0 to "1 triệu 5",
            7_200_000.0 to "7 triệu 2",
            35_000_000.0 to "35 triệu",
            1_250_000.0 to "1 triệu 250 nghìn",
            1_001_000.0 to "1 triệu 1 nghìn",
            123_456_000.0 to "123 triệu 456 nghìn",
            999_999_000.0 to "999 triệu 999 nghìn",
            999_999_999.0 to "999 triệu 999 nghìn"
        ).forEach { (amount, expected) ->
            val out = MoneyFormatter.toWordsCore(amount, viLabels)
            assertEquals("amount=$amount", expected, out)
            if (amount < 1_000_000_000.0) {
                assertTrue("Không được có chữ 'tỷ' cho $amount", !out.contains("tỷ"))
            }
        }
    }

    @Test
    fun `toWordsCore formats ty tier including tram-trieu shortcut and trieu-le`() {
        listOf(
            1_000_000_000.0 to "1 tỷ",
            2_000_000_000.0 to "2 tỷ",
            5_000_000_000.0 to "5 tỷ",
            1_200_000_000.0 to "1 tỷ 2 trăm triệu",
            1_300_000_000.0 to "1 tỷ 3 trăm triệu",
            1_500_000_000.0 to "1 tỷ 5 trăm triệu",
            1_234_000_000.0 to "1 tỷ 234 triệu",
            1_250_000_000.0 to "1 tỷ 250 triệu",
            2_050_000_000.0 to "2 tỷ 50 triệu",
            12_345_000_000.0 to "12 tỷ 345 triệu",
            1_000_500_000.0 to "1 tỷ 5 trăm nghìn"
        ).forEach { (amount, expected) ->
            assertEquals("amount=$amount", expected, MoneyFormatter.toWordsCore(amount, viLabels))
        }
    }

    @Test
    fun `toWordsCore works with custom i18n labels`() {
        val enLabels = MoneyLabels(
            billion = "b",
            million = "m",
            hundredThousand = "hk",
            thousand = "k",
            dong = "d",
            hundred = "h",
        )
        assertEquals("1 m 250 k", MoneyFormatter.toWordsCore(1_250_000.0, enLabels))
    }

    // === formatVndShort ===

    @Test
    fun `formatVndShort uses vi-VN dot grouping regardless of JVM default locale`() {
        listOf(
            0.0 to "0 đ",
            999.9 to "999 đ",
            12_345.99 to "12.345 đ",
            1_000_000.0 to "1.000.000 đ",
            1_200_000.0 to "1.200.000 đ",
            1_500_000_000.0 to "1.500.000.000 đ"
        ).forEach { (amount, expected) ->
            assertEquals("amount=$amount", expected, MoneyFormatter.formatVndShort(amount))
        }

        val neg = MoneyFormatter.formatVndShort(-1_000_000.0)
        assertEquals("-1.000.000 đ", neg)
    }
}
