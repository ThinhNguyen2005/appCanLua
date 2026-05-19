package com.GiaThinh.canlua.data.model

import com.GiaThinh.canlua.ui.viewmodel.UiMessage

/**
 * Một phiên hội thoại với AI.
 *
 * In-memory only — sống trong ChatSessionStore (Singleton @Hilt) → process die là mất.
 * Đáp ứng yêu cầu: "giữ đến khi user tắt app hoàn toàn".
 */
data class ChatSession(
    val id: String,
    /** Tự sinh từ tin user đầu tiên (40 ký tự đầu). Hiển thị trên drawer. */
    val title: String,
    val messages: List<UiMessage>,
    val createdAt: Long = System.currentTimeMillis()
)
