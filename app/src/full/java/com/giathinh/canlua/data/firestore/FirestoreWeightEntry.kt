package com.giathinh.canlua.data.firestore

import com.google.firebase.firestore.DocumentId

/**
 * Firestore representation of WeightEntry
 */
data class FirestoreWeightEntry(
    @DocumentId
    val id: String = "",
    val cardId: String, // Firestore card document ID
    val weight: Double = 0.0,
    val bagWeight: Double = 0.0,
    val impurityWeight: Double = 0.0,
    val netWeight: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String? = null,
    val syncTimestamp: Long = System.currentTimeMillis(),
    val localId: Long? = null // Local Room database ID for mapping
)

