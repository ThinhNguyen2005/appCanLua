package com.GiaThinh.canlua.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val region: String = "",
    val note: String = "",
    val role: String = "FARMER",
    val cccd: String = "",
    val username: String = "",
    val email: String = ""
)

