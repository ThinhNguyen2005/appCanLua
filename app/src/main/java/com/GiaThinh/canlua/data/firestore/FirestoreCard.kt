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
    val localId: Long? = null // Local Room database ID for mapping
)

