package com.giathinh.canlua.ui.viewmodel

import com.giathinh.canlua.data.model.WeightEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests cho [organizeIntoTables] — gọi trực tiếp `internal fun organizeIntoTables`
 * trong `WeightInputViewModel.kt`.
 *
 * Function này dàn xếp danh sách WeightEntry thành lưới 5×5 (mỗi bảng),
 * theo thứ tự column-major (cột 0 trước, từ hàng 0→4, rồi cột 1, v.v.).
 *
 * Kiến trúc output: List<Table> = List<List<List<Double?>>>
 *   - tables[tableIndex][rowIndex][colIndex]
 *   - Mỗi table có 5 hàng × 5 cột = 25 ô
 */
class OrganizeIntoTablesTest {

    private fun makeEntry(weight: Double): WeightEntry = WeightEntry(
        cardId = 1L,
        weight = weight,
        bagWeight = 0.0,
        impurityWeight = 0.0,
        netWeight = weight
    )

    // === số lượng bảng ===

    @Test
    fun `table count matches exact 25-entry boundaries`() {
        data class Case(val entryCount: Int, val manualCount: Int, val expectedTables: Int)
        listOf(
            Case(entryCount = 0, manualCount = 0, expectedTables = 1),
            Case(entryCount = 1, manualCount = 0, expectedTables = 1),
            Case(entryCount = 24, manualCount = 0, expectedTables = 1),
            Case(entryCount = 25, manualCount = 0, expectedTables = 1),
            Case(entryCount = 26, manualCount = 0, expectedTables = 2),
            Case(entryCount = 50, manualCount = 0, expectedTables = 2),
            Case(entryCount = 51, manualCount = 0, expectedTables = 3),
            Case(entryCount = 0, manualCount = 3, expectedTables = 4),
            Case(entryCount = 25, manualCount = 1, expectedTables = 2),
            Case(entryCount = 0, manualCount = -5, expectedTables = 1)
        ).forEach { (entryCount, manualCount, expectedTables) ->
            val entries = (1..entryCount).map { makeEntry(it.toDouble()) }
            val tables = organizeIntoTables(entries, manualCount = manualCount)
            assertEquals(
                "entryCount=$entryCount manualCount=$manualCount",
                expectedTables,
                tables.size
            )
            assertTrue("Phải luôn có ít nhất 1 bảng", tables.isNotEmpty())
        }
    }

    // === kích thước mỗi bảng ===

    @Test
    fun `each table has 5 rows of 5 columns`() {
        val tables = organizeIntoTables(emptyList(), manualCount = 0)
        assertEquals(5, tables[0].size)
        tables[0].forEach { row -> assertEquals(5, row.size) }
    }

    // === điền dữ liệu theo column-major ===

    @Test
    fun `fills entries in column-major order across tables`() {
        val entries = (1..26).map { makeEntry(it.toDouble()) }
        val tables = organizeIntoTables(entries, manualCount = 0)

        assertEquals(2, tables.size)
        assertEquals(1.0, tables[0][0][0]!!, 0.0)   // entry 1: col 0, row 0
        assertEquals(2.0, tables[0][1][0]!!, 0.0)   // entry 2: col 0, row 1
        assertEquals(5.0, tables[0][4][0]!!, 0.0)   // entry 5: col 0, row 4
        assertEquals(6.0, tables[0][0][1]!!, 0.0)   // entry 6: col 1, row 0
        assertEquals(25.0, tables[0][4][4]!!, 0.0)  // entry 25: table 0, [4][4]
        assertEquals(26.0, tables[1][0][0]!!, 0.0)  // entry 26: table 1, [0][0]
    }

    @Test
    fun `empty cells and manual trailing table cells are null`() {
        val singleEntryTables = organizeIntoTables(listOf(makeEntry(99.0)), manualCount = 0)
        assertNull("Ô [0][1] phải null", singleEntryTables[0][0][1])
        assertNull("Ô [1][0] phải null", singleEntryTables[0][1][0])
        assertNull("Ô [4][4] phải null", singleEntryTables[0][4][4])

        val fullPlusManual = organizeIntoTables((1..25).map { makeEntry(it.toDouble()) }, manualCount = 1)
        assertEquals(2, fullPlusManual.size)
        assertNotNull("Entry thứ 25 phải không null ở [4][4]", fullPlusManual[0][4][4])
        fullPlusManual[1].forEach { row ->
            row.forEach { cell -> assertNull("Bảng thêm thủ công phải toàn null", cell) }
        }
    }

    // === tổng số entries bảo toàn ===

    @Test
    fun `all non-null cells preserve count and weight sum`() {
        val weights = listOf(10.0, 20.0, 15.0, 25.5, 8.0, 32.25)
        val entries = weights.map { makeEntry(it) }
        val tables = organizeIntoTables(entries, manualCount = 0)

        var count = 0
        var sum = 0.0
        tables.forEach { t ->
            t.forEach { r ->
                r.forEach { c ->
                    if (c != null) {
                        count++
                        sum += c
                    }
                }
            }
        }
        assertEquals(weights.size, count)
        assertEquals(weights.sum(), sum, 0.001)
    }
}
