package com.giathinh.canlua.repository

import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T05 — Quản lý phiên đồng bộ bất biến.
 *
 * Mỗi thao tác sync chụp một [Session] (uid + generation + processNonce) lúc bắt
 * đầu và kiểm tra [isValid] trước khi gửi, sau khi nhận response, trước khi apply
 * vào Room và trước khi ack. Đổi tài khoản / đăng xuất tăng [generation] → mọi
 * session cũ hết hiệu lực, không thể stamp dữ liệu của A sang B.
 *
 * KHÔNG thay thế server rules: request đã được server nhận không thể thu hồi bằng
 * client cancellation. Đây là lớp bảo vệ phía client.
 */
@Singleton
class SyncSessionCoordinator @Inject constructor(
    private val auth: FirebaseAuth
) {
    private val processNonce: String = UUID.randomUUID().toString()
    private val generation = AtomicLong(0L)

    @Volatile
    private var observedUid: String? = auth.currentUser?.uid

    data class Session(
        val uid: String,
        val generation: Long,
        val processNonce: String
    )

    /** Session hiện tại, hoặc null nếu chưa đăng nhập. */
    fun currentSession(): Session? {
        val uid = auth.currentUser?.uid ?: return null
        return Session(uid, generation.get(), processNonce)
    }

    /** Gọi khi auth state đổi (kể cả null lúc logout) để tăng generation khi đổi uid. */
    fun onAuthChanged(uid: String?) {
        if (uid != observedUid) {
            observedUid = uid
            generation.incrementAndGet()
        }
    }

    /** Vô hiệu hóa mọi session đang chạy (gọi trước Firebase signOut). */
    fun invalidate() {
        observedUid = null
        generation.incrementAndGet()
    }

    /** Session còn hiệu lực khi đúng uid hiện tại + đúng generation + đúng process. */
    fun isValid(session: Session): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return session.uid == uid &&
            session.generation == generation.get() &&
            session.processNonce == processNonce
    }
}