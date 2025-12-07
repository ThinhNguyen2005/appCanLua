package com.GiaThinh.canlua.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.GiaThinh.canlua.data.model.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: Profile): Long

    @Query("SELECT * FROM profiles ORDER BY id DESC LIMIT 1")
    fun getLatestProfile(): Flow<Profile?>
}

