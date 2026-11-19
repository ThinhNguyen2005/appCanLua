package com.giathinh.canlua.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.giathinh.canlua.data.model.SyncOutbox

@Dao
interface SyncOutboxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SyncOutbox)

    @Query("SELECT * FROM sync_outbox WHERE ownerUid = :uid ORDER BY createdAt ASC")
    suspend fun getPendingForOwner(uid: String): List<SyncOutbox>

    @Query("SELECT * FROM sync_outbox WHERE ownerUid = :uid AND syncId = :syncId ORDER BY localRevision DESC LIMIT 1")
    suspend fun getLatestForCard(uid: String, syncId: String): SyncOutbox?

    @Query("DELETE FROM sync_outbox WHERE ownerUid = :uid AND syncId = :syncId AND localRevision = :revision")
    suspend fun delete(uid: String, syncId: String, revision: Long)

    @Query("DELETE FROM sync_outbox WHERE ownerUid = :uid AND syncId = :syncId AND localRevision < :revision")
    suspend fun deleteOlderRevisions(uid: String, syncId: String, revision: Long)

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE ownerUid = :uid")
    suspend fun countPending(uid: String): Int
}