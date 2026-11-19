package com.giathinh.canlua.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.giathinh.canlua.data.model.MutationReceipt

@Dao
interface MutationReceiptDao {

    @Query("SELECT * FROM mutation_receipts WHERE ownerUid = :uid AND operationId = :operationId LIMIT 1")
    suspend fun get(uid: String, operationId: String): MutationReceipt?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(receipt: MutationReceipt)
}