package com.giathinh.canlua.data.firestore

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Firestore representation of Transaction
 */
data class FirestoreTransaction(
    @DocumentId
    val id: String = "",
    val cardId: String, // Firestore card document ID
    val amount: Double = 0.0,
    val type: String, // "DEPOSIT", "PAYMENT", "REFUND"
    val description: String? = null,
    val date: Date = Date(),
    val userId: String? = null,
    val syncTimestamp: Long = System.currentTimeMillis(),
    val localId: Long? = null // Local Room database ID for mapping
)

