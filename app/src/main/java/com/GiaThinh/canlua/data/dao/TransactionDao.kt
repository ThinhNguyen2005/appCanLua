package com.GiaThinh.canlua.data.dao

import androidx.room.*
import com.GiaThinh.canlua.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE cardId = :cardId ORDER BY date DESC")
    fun getTransactionsByCardId(cardId: Long): Flow<List<Transaction>>

    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE cardId = :cardId")
    suspend fun deleteTransactionsByCardId(cardId: Long)

    @Query("SELECT SUM(amount) FROM transactions WHERE cardId = :cardId AND type = 'PAYMENT'")
    suspend fun getTotalPaidAmountByCardId(cardId: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE cardId = :cardId AND type = 'DEPOSIT'")
    suspend fun getTotalDepositAmountByCardId(cardId: Long): Double?

    // === Phase 4: Cross-device sync ===

    @Query("SELECT * FROM transactions WHERE firestoreId = :fsId LIMIT 1")
    suspend fun getByFirestoreId(fsId: String): Transaction?

    @Query("UPDATE transactions SET firestoreId = :fsId WHERE id = :localId")
    suspend fun updateFirestoreId(localId: Long, fsId: String)
}

