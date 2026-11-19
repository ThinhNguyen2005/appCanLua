package com.giathinh.canlua.domain

import com.giathinh.canlua.data.model.Card

/**
 * Lỗi nghiệp vụ typed cho mutation phiếu cân. Repository trả về thay vì ném
 * exception tràn lan hoặc im lặng return (fail-open). ViewModel chuyển thành
 * trạng thái UI để người dùng biết vì sao thao tác không được ghi.
 */
sealed interface CardError {
    /** Không tìm thấy phiếu thuộc tài khoản hiện tại (fail-closed). */
    data object NotFound : CardError

    /** Phiếu đã khóa — không cho sửa đầu vào cân / thêm thanh toán. */
    data object Locked : CardError

    /** Revision đã đổi từ lúc UI đọc → yêu cầu refresh, không ghi đè snapshot cũ. */
    data object StaleRevision : CardError

    /** Dữ liệu nhập không hợp lệ (kèm lý do để hiển thị). */
    data class InvalidInput(val reason: String) : CardError

    /** Cùng operationId nhưng payload khác, hoặc xung đột đồng bộ. */
    data object Conflict : CardError

    /** Phiên đăng nhập đã đổi/hết hiệu lực. */
    data object SessionExpired : CardError
}

/** Kết quả một mutation phiếu. */
sealed interface CardMutationResult {
    data class Success(val card: Card) : CardMutationResult
    data class Failure(val error: CardError) : CardMutationResult

    val isSuccess: Boolean get() = this is Success
    fun cardOrNull(): Card? = (this as? Success)?.card
    fun errorOrNull(): CardError? = (this as? Failure)?.error
}