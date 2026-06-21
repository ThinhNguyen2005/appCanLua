package com.giathinh.canlua.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.giathinh.canlua.data.model.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: Profile): Long

    /**
     * Truy vấn profile của 1 user cụ thể (theo Firebase UID).
     * Đây là entry point chính từ phase này trở đi — tránh bug role-leak
     * giữa các tài khoản dùng chung máy.
     */
    @Query("SELECT * FROM profiles WHERE uid = :uid LIMIT 1")
    fun getProfileByUid(uid: String): Flow<Profile?>

    @Query("DELETE FROM profiles WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)

    /**
     * @deprecated Chỉ giữ lại cho debug / migration tạm thời.
     * Code app KHÔNG được dùng — phải đi qua [getProfileByUid].
     */
    @Query("SELECT * FROM profiles ORDER BY uid LIMIT 1")
    fun getLatestProfile(): Flow<Profile?>
}
