package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.data.model.ChatSession
import com.GiaThinh.canlua.ui.viewmodel.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory store cho các phiên chat AI.
 *
 * Singleton sống cùng vòng đời Application → khi user kill app, toàn bộ session bị xóa.
 * Đây là behavior mong muốn theo yêu cầu của user ("giữ đến khi tắt app hoàn toàn").
 *
 * Khi user signOut, gọi [clear] để tránh leak history sang user khác trên cùng device.
 */
@Singleton
class ChatSessionStore @Inject constructor() {

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _currentId = MutableStateFlow<String?>(null)
    val currentId: StateFlow<String?> = _currentId.asStateFlow()

    /** Tạo phiên mới với welcome message, set làm current. Trả về id. */
    fun createNew(welcome: List<UiMessage> = emptyList()): String {
        val id = UUID.randomUUID().toString()
        val session = ChatSession(
            id = id,
            title = DEFAULT_TITLE,
            messages = welcome
        )
        _sessions.value = listOf(session) + _sessions.value
        _currentId.value = id
        return id
    }

    fun switchTo(id: String) {
        if (_sessions.value.any { it.id == id }) {
            _currentId.value = id
        }
    }

    /** Lấy session theo id; null nếu không tồn tại. */
    fun getSession(id: String): ChatSession? = _sessions.value.find { it.id == id }

    /** Append message vào session. No-op nếu id không tồn tại. */
    fun appendMessage(id: String, message: UiMessage) {
        _sessions.value = _sessions.value.map {
            if (it.id == id) it.copy(messages = it.messages + message) else it
        }
    }

    /**
     * Auto-rename session theo tin user đầu tiên (chỉ rename khi title còn là DEFAULT_TITLE).
     * Cắt tối đa 40 ký tự + thêm "…" nếu dài hơn.
     */
    fun renameTitleIfNeeded(id: String, firstUserMessage: String) {
        val target = _sessions.value.find { it.id == id } ?: return
        if (target.title != DEFAULT_TITLE) return
        val trimmed = firstUserMessage.trim()
        if (trimmed.isEmpty()) return
        val newTitle = if (trimmed.length > MAX_TITLE_LEN) {
            trimmed.take(MAX_TITLE_LEN).trimEnd() + "…"
        } else {
            trimmed
        }
        _sessions.value = _sessions.value.map {
            if (it.id == id) it.copy(title = newTitle) else it
        }
    }

    /** Xóa toàn bộ session — gọi khi user signOut. */
    fun clear() {
        _sessions.value = emptyList()
        _currentId.value = null
    }

    companion object {
        const val DEFAULT_TITLE = "Phiên mới"
        private const val MAX_TITLE_LEN = 40
    }
}
