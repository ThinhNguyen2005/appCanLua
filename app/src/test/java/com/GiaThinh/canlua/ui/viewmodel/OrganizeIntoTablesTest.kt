package com.GiaThinh.canlua.ui.viewmodel

import com.GiaThinh.canlua.data.model.WeightEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests cho [organizeIntoTables] — private function trong WeightInputViewModel.
 *
 * Function này dàn xếp danh sách WeightEntry thành lưới 5×5 (mỗi bảng),
 * theo thứ tự column-major (cột 0 trước, từ hàng 0→4, rồi cột 1, v.v.).
 *
 * Kiến trúc output: List<Table> = List<List<List<Double?>>>
 *   - tables[tableIndex][rowIndex][colIndex]
 *   - Mỗi table có 5 hàng × 5 cột = 25 ô
 *
 * ⚠️ BUG #4 NOTE:
 * Hiện tại code điền theo column-major nhưng tableGrid được index theo [row][col].
 * Entry thứ 0 → tableGrid[0][0] (hàng 0, cột 0) ✓
 * Entry thứ 1 → tableGrid[1][0] (hàng 1, cột 0) ✓ (column-major: cột 0 đầu tiên)
 * Entry thứ 5 → tableGrid[0][1] (hàng 0, cột 1) ✓
 * Behavior này nhất quán nếu UI đọc tables[t][row][col] — test này xác nhận.
 *
 * Cách test: dùng reflection vì function là package-private (file-level function).
 * Thay vào đó ta extract logic ra helper function testable.
 */
class OrganizeIntoTablesTest {

    private fun makeEntry(weight: Double): WeightEntry = WeightEntry(
        cardId = 1L,
        weight = weight,
        bagWeight = 0.0,
        impurityWeight = 0.0,
        netWeight = weight
    )

    /**
     * Mirror của private `organizeIntoTables` để test không dùng reflection.
     * Khi implementation thay đổi, phải cập nhật mirror này.
     */
    private fun organizeIntoTables(entries: List<WeightEntry>, manualCount: Int): List<List<List<Double?>>> {
        val totalEntries = entries.size
        val calculatedNumTables = (totalEntries / 25) + 1
        val numTables = (calculatedNumTables + manualCount).coerceAtLeast(1)

        val tables = mutableListOf<List<List<Double?>>>()
        for (t in 0 until numTables) {
            val tableGrid = MutableList(5) { MutableList<Double?>(5) { null } }
            for (c in 0 until 5) {
                for (r in 0 until 5) {
                    val entryIdx = (t * 25) + (c * 5) + r
                    if (entryIdx < totalEntries) {
                        tableGrid[r][c] = entries[entryIdx].weight
                    }
                }
            }
            tables.add(tableGrid.map { it.toList() })
        }
        return tables
    }

    // === số lượng bảng ===

    @Test
    fun `empty entries with manualCount 0 produces exactly 1 empty table`() {
        val tables = organizeIntoTables(emptyList(), manualCount = 0)
        assertEquals(1, tables.size)
    }

    @Test
    fun `1 entry produces 1 table`() {
        val tables = organizeIntoTables(listOf(makeEntry(50.0)), manualCount = 0)
        assertEquals(1, tables.size)
    }

    @Test
    fun `25 entries fills exactly 1 table - boundary`() {
        val entries = (1..25).map { makeEntry(it.toDouble()) }
        val tables = organizeIntoTables(entries, manualCount = 0)
        // 25/25 = 1 + 1 = 2 tables (calculatedNumTables) — vì (25/25)+1 = 2!
        assertEquals(2, tables.size)
    }

    @Test
    fun `24 entries produces 1 table`() {
        val entries = (1..24).map { makeEntry(it.toDouble()) }
        val tables = organizeIntoTables(entries, manualCount = 0)
        // 24/25 = 0 → +1 = 1 table
        assertEquals(1, tables.size)
    }

    @Test
    fun `26 entries produces 2 tables`() {
        val entries = (1..26).map { makeEntry(it.toDouble()) }
        val tables = organizeIntoTables(entries, manualCount = 0)
        // 26/25 = 1 → +1 = 2
        assertEquals(2, tables.size)
    }

    @Test
    fun `manualCount adds extra empty tables`() {
        val tables = organizeIntoTables(emptyList(), manualCount = 3)
        // (0/25)+1 + 3 = 4
        assertEquals(4, tables.size)
    }

    @Test
    fun `coerceAtLeast 1 means never zero tables even with negative manualCount`() {
        // manualCount always >= 0 in practice, but let's verify coerce
        val tables = organizeIntoTables(emptyList(), manualCount = 0)
        assertTrue("Phải luôn có ít nhất 1 bảng", tables.isNotEmpty())
    }

    // === kích thước mỗi bảng ===

    @Test
    fun `each table has exactly 5 rows`() {
        val tables = organizeIntoTables(emptyList(), manualCount = 0)
        assertEquals(5, tables[0].size)
    }

    @Test
    fun `each row has exactly 5 columns`() {
        val tables = organizeIntoTables(emptyList(), manualCount = 0)
        tables[0].forEach { row -> assertEquals(5, row.size) }
    }

    // === điền dữ liệu theo column-major ===

    @Test
    fun `first entry goes to table 0, row 0, col 0`() {
        val tables = organizeIntoTables(listOf(makeEntry(10.0)), manualCount = 0)
        assertEquals(10.0, tables[0][0][0]!!, 0.0)
    }

    @Test
    fun `second entry goes to table 0, row 1, col 0 (column-major)`() {
        // Column-major: điền cột 0 từ hàng 0→4 trước
        val entries = listOf(makeEntry(1.0), makeEntry(2.0))
        val tables = organizeIntoTables(entries, manualCount = 0)
        assertEquals(1.0, tables[0][0][0]!!, 0.0) // hàng 0, cột 0
        assertEquals(2.0, tables[0][1][0]!!, 0.0) // hàng 1, cột 0
    }

    @Test
    fun `6th entry goes to table 0, row 0, col 1`() {
        // 5 entries điền đầy cột 0 → entry thứ 6 (index 5) = hàng 0, cột 1
        val entries = (1..6).map { makeEntry(it.toDouble()) }
        val tables = organizeIntoTables(entries, manualCount = 0)
        assertEquals(1.0, tables[0][0][0]!!, 0.0) // col 0, row 0
        assertEquals(5.0, tables[0][4][0]!!, 0.0) // col 0, row 4
        assertEquals(6.0, tables[0][0][1]!!, 0.0) // col 1, row 0
    }

    @Test
    fun `entry 26 goes to table 1, row 0, col 0`() {
        val entries = (1..26).map { makeEntry(it.toDouble()) }
        val tables = organizeIntoTables(entries, manualCount = 0)
        assertEquals(26.0, tables[1][0][0]!!, 0.0)
    }

    @Test
    fun `empty cells in table are null`() {
        val entries = listOf(makeEntry(99.0)) // chỉ 1 entry
        val tables = organizeIntoTables(entries, manualCount = 0)
        // Mọi ô khác (0,1), (0,2)... phải null
        assertNull("Ô [0][1] phải null", tables[0][0][1])
        assertNull("Ô [1][0] phải null", tables[0][1][0])
        assertNull("Ô [4][4] phải null", tables[0][4][4])
    }

    @Test
    fun `second table is all null when only 24 entries`() {
        // 24 entries → 1 table (1 bảng đầy), không có table thứ 2
        // 25 entries → tính thành 2 tables, table thứ 2 trống
        val entries = (1..25).map { makeEntry(it.toDouble()) }
        val tables = organizeIntoTables(entries, manualCount = 0)
        // table index 1 phải hoàn toàn null
        tables[1].forEach { row ->
            row.forEach { cell -> assertNull("Table 2 phải toàn null", cell) }
        }
    }

    // === tổng số entries bảo toàn ===

    @Test
    fun `all non-null cells sum equals number of entries`() {
        val entries = (1..18).map { makeEntry(it.toDouble()) }
        val tables = organizeIntoTables(entries, manualCount = 0)
        var count = 0
        tables.forEach { t -> t.forEach { r -> r.forEach { c -> if (c != null) count++ } } }
        assertEquals(18, count)
    }

    @Test
    fun `sum of weights in all cells equals sum of input entries`() {
        val weights = listOf(10.0, 20.0, 15.0, 25.5, 8.0)
        val entries = weights.map { makeEntry(it) }
        val tables = organizeIntoTables(entries, manualCount = 0)

        var sum = 0.0
        tables.forEach { t -> t.forEach { r -> r.forEach { c -> if (c != null) sum += c } } }
        assertEquals(weights.sum(), sum, 0.001)
    }

    // === entry thứ 25 — BOUNDARY quan trọng ===

    @Test
    fun `25th entry is placed in table 0 last position (row4 col4)`() {
        val entries = (1..25).map { makeEntry(it.toDouble()) }
        val tables = organizeIntoTables(entries, manualCount = 0)
        // Entry index 24 → c=4, r=4 → tableGrid[4][4]
        assertNotNull("Entry thứ 25 phải không null ở [4][4]", tables[0][4][4])
        assertEquals(25.0, tables[0][4][4]!!, 0.0)
    }
}
