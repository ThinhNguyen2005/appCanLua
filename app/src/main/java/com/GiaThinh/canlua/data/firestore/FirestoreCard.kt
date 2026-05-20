package com.GiaThinh.canlua.data.firestore

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Firestore representation of Card
 * Note: id is the document ID in Firestore
 */
data class FirestoreCard(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val cccd: String? = null,
    val date: Date = Date(),
    val totalWeight: Double = 0.0,
    val bagWeight: Double = 0.0,
    val impurityWeight: Double = 0.0,
    val netWeight: Double = 0.0,
    val depositAmount: Double = 0.0,
    val pricePerKg: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val bagCount: Int = 0,
    val isLocked: Boolean = false,
    val traderName: String = "",
    val riceVariety: String = "",
    val moisturePercent: Double = 0.0,
    val seasonLabel: String = "",
    val qrToken: String? = null,
    val lockedByTraderId: String? = null,
    val userId: String? = null, // Firebase user ID
    val deviceId: String? = null, // Device identifier
    val syncTimestamp: Long = System.currentTimeMillis(),
    /**
     * Mirror của `Card.lastModifiedMs` — thời điểm sửa đổi gần nhất ở client.
     * Dùng cho conflict resolution khi pull về máy khác:
     * cloud thắng nếu `lastModifiedMs cloud > lastModifiedMs local`.
     * `syncTimestamp` là thời điểm push lên cloud, có thể trễ hơn lastModifiedMs.
     */
    val lastModifiedMs: Long = 0L,
    val localId: Long? = null, // Local Room database ID for mapping

    // === PHASE 2.6: GPS location ===
    val latitude: Double? = null,
    val longitude: Double? = null,

    // === PHASE 2.8: Contact + field address ===
    val traderPhone: String = "",
    val fieldAddress: String = ""
)

