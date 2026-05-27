package com.GiaThinh.canlua.data.dao

import androidx.room.*
import com.GiaThinh.canlua.data.model.WeightEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightEntryDao {
    @Query("SELECT * FROM weight_entries WHERE cardId = :cardId ORDER BY timestamp ASC")
    fun getWeightEntriesByCardId(cardId: Long): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries WHERE cardId = :cardId")
    suspend fun getWeightEntriesByCardIdSync(cardId: Long): List<WeightEntry>

    @Insert
    suspend fun insertWeightEntry(weightEntry: WeightEntry): Long

    @Update
    suspend fun updateWeightEntry(weightEntry: WeightEntry)

    // H-01: Bulk update — Room gồm tất cả cập nhật vào 1 DB transaction
    // thay vì N transaction riêng lẻ → giảm disk write và WAL pressure.
    @Update
    suspend fun updateWeightEntries(entries: List<WeightEntry>)

    @Delete
    suspend fun deleteWeightEntry(weightEntry: WeightEntry)

    @Query("DELETE FROM weight_entries WHERE cardId = :cardId")
    suspend fun deleteWeightEntriesByCardId(cardId: Long)

    @Query("SELECT SUM(netWeight) FROM weight_entries WHERE cardId = :cardId")
    suspend fun getTotalNetWeightByCardId(cardId: Long): Double?

    @Query("SELECT SUM(weight) FROM weight_entries WHERE cardId = :cardId")
    suspend fun getTotalRawWeightByCardId(cardId: Long): Double?

    @Query("SELECT COUNT(*) FROM weight_entries WHERE cardId = :cardId")
    suspend fun getBagCountByCardId(cardId: Long): Int

    // === Phase 4: Cross-device sync ===

    @Query("SELECT * FROM weight_entries WHERE firestoreId = :fsId LIMIT 1")
    suspend fun getByFirestoreId(fsId: String): WeightEntry?

    @Query("UPDATE weight_entries SET firestoreId = :fsId WHERE id = :localId")
    suspend fun updateFirestoreId(localId: Long, fsId: String)
}

