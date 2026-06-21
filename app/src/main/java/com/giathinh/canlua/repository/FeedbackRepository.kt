package com.giathinh.canlua.repository

import com.giathinh.canlua.BuildConfig
import com.giathinh.canlua.data.firestore.FirestoreFeedback
import com.giathinh.canlua.data.remote.HttpClient
import com.giathinh.canlua.util.ApiKeyObfuscator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val httpClient: HttpClient,
    private val profileRepository: ProfileRepository
) {
    private val userId: String?
        get() = auth.currentUser?.uid

    private val feedbacksCollection
        get() = firestore.collection("feedbacks")

    fun observeMyFeedbacks(): Flow<List<FirestoreFeedback>> = callbackFlow {
        val uid = userId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration: ListenerRegistration = feedbacksCollection
            .whereEqualTo("userId", uid)
            .addSnapshotListener(Dispatchers.IO.asExecutor(), MetadataChanges.EXCLUDE) { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject(FirestoreFeedback::class.java) }
                    .orEmpty()
                    .sortedBy { it.timestamp } // Thứ tự thời gian tăng dần để giống giao diện chat
                trySend(list)
            }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    /**
     * Badge "tin nhắn chưa đọc" — poll Firestore mỗi 30s thay vì giữ snapshot listener.
     * Lý do: badge luôn được MainScreen collect → listener không bao giờ dừng. Chấp nhận
     * trễ ~30s vì admin reply không thường xuyên, đổi lại không bombard main thread bằng
     * snapshot callbacks khi user đang ở tab khác.
     */
    fun observeUnreadReplyCount(intervalMs: Long = 30_000L): Flow<Int> = flow {
        while (true) {
            emit(fetchUnreadReplyCount())
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    private suspend fun fetchUnreadReplyCount(): Int {
        val uid = userId ?: return 0
        return try {
            val snapshot = feedbacksCollection
                .whereEqualTo("userId", uid)
                .whereEqualTo("isReadByUser", false)
                .get()
                .await()
            snapshot.documents
                .mapNotNull { it.toObject(FirestoreFeedback::class.java) }
                .count { it.replyText != null }
        } catch (e: Exception) {
            0
        }
    }

    suspend fun sendFeedback(messageText: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = userId ?: return@withContext Result.failure(IllegalStateException("User not logged in"))
        val profile = profileRepository.ensureCurrentProfile()
        val userName = profile?.name?.ifBlank { "Nông dân vô danh" } ?: "Nông dân"
        val userRole = profile?.role ?: "FARMER"

        try {
            val docRef = feedbacksCollection.document()
            val feedback = FirestoreFeedback(
                id = docRef.id,
                userId = uid,
                userName = userName,
                userRole = userRole,
                message = messageText,
                timestamp = System.currentTimeMillis(),
                replyText = null,
                replyTimestamp = null,
                isReadByUser = true
            )
            docRef.set(feedback).await()

            // Gửi thông báo đến Telegram Bot Admin
            sendTelegramNotification(docRef.id, userName, userRole, messageText)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markFeedbacksAsRead(): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = userId ?: return@withContext Result.failure(IllegalStateException("User not logged in"))
        try {
            val snapshot = feedbacksCollection
                .whereEqualTo("userId", uid)
                .whereEqualTo("isReadByUser", false)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val batch = firestore.batch()
                snapshot.documents.forEach { doc ->
                    val replyText = doc.getString("replyText")
                    if (replyText != null) {
                        batch.update(doc.reference, "isReadByUser", true)
                    }
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sendTelegramNotification(feedbackId: String, name: String, role: String, messageText: String) {
        try {
            val roleName = if (role.uppercase() == "TRADER") "Thương lái" else "Nông dân"
            val textMessage = """
<b>📩 PHẢN HỒI MỚI TỪ APP CÂN LÚA</b>
👤 <b>Người gửi:</b> ${name} (${roleName})
💬 <b>Nội dung:</b> "${messageText}"

Ref: <code>${feedbackId}</code>
──────────────────
👉 <b>Trả lời:</b> Hãy nhấn giữ tin nhắn này và chọn <b>"Reply" (Trả lời)</b> để gửi câu trả lời về App cho user.
            """.trimIndent()

            val token = ApiKeyObfuscator.decode(BuildConfig.TELEGRAM_BOT_TOKEN)
            val chatId = ApiKeyObfuscator.decode(BuildConfig.TELEGRAM_ADMIN_CHAT_ID)
            
            if (token.isEmpty() || chatId.isEmpty()) return

            val url = "https://api.telegram.org/bot$token/sendMessage"

            val body = mapOf(
                "chat_id" to chatId,
                "text" to textMessage,
                "parse_mode" to "HTML"
            )

            httpClient.postJson<Map<String, Any>>(url, body)
        } catch (e: Exception) {
            // Không làm ảnh hưởng luồng gửi feedback chính nếu Telegram lỗi mạng
            e.printStackTrace()
        }
    }
}
