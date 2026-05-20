package com.GiaThinh.canlua.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Hồ sơ người dùng — keyed theo Firebase UID để mỗi tài khoản có profile RIÊNG
 * (trước đây dùng auto-id + LIMIT 1 nên các tài khoản trên cùng máy ghi đè role lẫn nhau).
 *
 * `roleGrantedBy` / `roleGrantedAt` chuẩn bị cho cơ chế admin-grant TRADER role:
 * user thường không tự đổi role → phải submit `roleRequests` để admin duyệt.
 */
@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val uid: String,
    val name: String,
    val phone: String = "",
    val region: String = "",
    val note: String = "",
    val role: String = "FARMER",
    val cccd: String = "",
    val username: String = "",
    val email: String = "",
    val roleGrantedBy: String = "self",   // "self" | "admin" | "system"
    val roleGrantedAt: Long = 0L
)
