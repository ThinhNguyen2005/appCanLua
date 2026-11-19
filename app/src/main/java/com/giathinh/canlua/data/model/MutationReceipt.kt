package com.giathinh.canlua.data.model

import androidx.room.Entity

/**
 * v20: Biên nhận lệnh đã thực thi — chống thực thi lại khi retry (idempotency).
 *
 * Khóa chính ghép (ownerUid, operationId). [commandHash] là hash của payload lệnh;
 * cùng operationId nhưng hash khác → conflict (không được ghi đè kết quả cũ).
 * [createdCardId] dùng cho lệnh tạo phiếu để retry trả lại đúng id đã tạo.
 * Không lưu CCCD hay dữ liệu nhạy cảm trong biên nhận.
 */
@Entity(
    tableName = "mutation_receipts",
    primaryKeys = ["ownerUid", "operationId"]
)
data class MutationReceipt(
    val ownerUid: String,
    val operationId: String,
    val commandHash: String,
    val createdCardId: Long? = null,
    val resultCode: String = RESULT_OK,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val RESULT_OK = "OK"
    }
}