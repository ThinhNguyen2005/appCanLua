package com.giathinh.canlua.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Quan hệ 1-N giữa Card và các thực thể con (WeightEntry, Transaction).
 * Dùng cho các tác vụ cần toàn bộ cây dữ liệu (như Sao lưu / Xuất dữ liệu)
 * giúp Room truy vấn trong 1 câu lệnh hợp nhất, triệt tiêu hoàn toàn lỗi N+1 query.
 */
data class CardWithDetails(
    @Embedded
    val card: Card,

    @Relation(
        parentColumn = "id",
        entityColumn = "cardId"
    )
    val entries: List<WeightEntry> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "cardId"
    )
    val transactions: List<Transaction> = emptyList()
)

