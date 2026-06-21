package com.giathinh.canlua.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.giathinh.canlua.data.model.DeletedCard
import kotlinx.coroutines.flow.Flow

@Dao
interface DeletedCardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deletedCard: DeletedCard): Long

    /** List lịch sử phiếu đã xoá của user, mới nhất trên đầu. */
    @Query("SELECT * FROM deleted_cards WHERE ownerUid = :uid ORDER BY deletedAt DESC")
    fun observe(uid: String): Flow<List<DeletedCard>>

    /**
     * Check 1 firestoreId đã có tombstone chưa — dùng trong pull dedup
     * để skip insert phiếu đã user xoá rồi.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM deleted_cards WHERE ownerUid = :uid AND firestoreId = :fsId)")
    suspend fun isTombstoned(uid: String, fsId: String): Boolean

    /** Lấy tombstone theo firestoreId — dùng khi restore. */
    @Query("SELECT * FROM deleted_cards WHERE ownerUid = :uid AND firestoreId = :fsId LIMIT 1")
    suspend fun getByFirestoreId(uid: String, fsId: String): DeletedCard?

    @Query("SELECT * FROM deleted_cards WHERE id = :id")
    suspend fun getById(id: Long): DeletedCard?

    /** Đánh dấu cloud đã xoá thành công sau khi background retry — không retry nữa. */
    @Query("UPDATE deleted_cards SET cloudDeleted = 1 WHERE id = :id")
    suspend fun markCloudDeleted(id: Long)

    /** List các tombstone chưa push cloud delete — background worker pickup. */
    @Query("SELECT * FROM deleted_cards WHERE ownerUid = :uid AND cloudDeleted = 0 AND firestoreId IS NOT NULL")
    suspend fun getPendingCloudDeletes(uid: String): List<DeletedCard>

    /** Xoá vĩnh viễn 1 tombstone — sau khi user "Xoá vĩnh viễn" trong UI. */
    @Query("DELETE FROM deleted_cards WHERE id = :id")
    suspend fun purge(id: Long)

    /** Xoá toàn bộ lịch sử của user — option "Dọn lịch sử". */
    @Query("DELETE FROM deleted_cards WHERE ownerUid = :uid")
    suspend fun purgeAllForOwner(uid: String): Int

    /** Đếm tombstone — dùng cho badge/menu Settings. */
    @Query("SELECT COUNT(*) FROM deleted_cards WHERE ownerUid = :uid")
    fun countForOwner(uid: String): Flow<Int>
}
