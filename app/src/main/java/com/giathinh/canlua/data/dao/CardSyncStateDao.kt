package com.giathinh.canlua.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.giathinh.canlua.data.model.CardSyncState

@Dao
interface CardSyncStateDao {

    @Query("SELECT * FROM card_sync_state WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): CardSyncState?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(state: CardSyncState)

    @Update
    suspend fun update(state: CardSyncState)

    @Query("SELECT * FROM card_sync_state WHERE ownerUid = :uid")
    suspend fun getAllForOwner(uid: String): List<CardSyncState>

    @Query("UPDATE card_sync_state SET localRevision = :revision, protocolState = :state WHERE syncId = :syncId")
    suspend fun updateLocalRevision(syncId: String, revision: Long, state: String)

    @Query("UPDATE card_sync_state SET baseRemoteRevision = :base, ackedRemoteRevision = :acked, protocolState = :state WHERE syncId = :syncId")
    suspend fun markAcked(syncId: String, base: String?, acked: String?, state: String)

    @Query("UPDATE card_sync_state SET protocolState = :state WHERE syncId = :syncId")
    suspend fun setState(syncId: String, state: String)
}