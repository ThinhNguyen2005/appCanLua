package com.giathinh.canlua.data.firestore

import com.google.firebase.firestore.DocumentId

data class FirestoreFeedback(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userRole: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val replyText: String? = null,
    val replyTimestamp: Long? = null,
    @field:JvmField
    val isReadByUser: Boolean = true
)
