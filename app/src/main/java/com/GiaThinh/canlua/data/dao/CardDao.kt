package com.GiaThinh.canlua.data.dao

import androidx.room.*
import com.GiaThinh.canlua.data.model.Card
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY date DESC")
    fun getAllCards(): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardById(id: Long): Card?

    @Insert
    suspend fun insertCard(card: Card): Long

    @Update
    suspend fun updateCard(card: Card)

    @Delete
    suspend fun deleteCard(card: Card)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCardById(id: Long)

    // === Phase 1: QR Handshake queries ===

    @Query("SELECT * FROM cards WHERE qrToken = :token LIMIT 1")
    suspend fun findByQrToken(token: String): Card?

    @Query("UPDATE cards SET qrToken = :token WHERE id = :cardId")
    suspend fun updateQrToken(cardId: Long, token: String)

    @Query("UPDATE cards SET isLocked = 1, lockedByTraderId = :traderId WHERE id = :cardId")
    suspend fun lockCard(cardId: Long, traderId: String)

    // === Phase 1: Filter & search ===

    @Query("SELECT * FROM cards WHERE riceVariety = :variety ORDER BY date DESC")
    fun getCardsByRiceVariety(variety: String): Flow<List<Card>>

    @Query("SELECT DISTINCT riceVariety FROM cards WHERE riceVariety != '' ORDER BY riceVariety")
    fun getDistinctRiceVarieties(): Flow<List<String>>
}
