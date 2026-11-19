package com.giathinh.canlua.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Card::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["cardId"]),
        androidx.room.Index(value = ["firestoreId"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: Long,
    val amount: Double, // Số tiền
    val type: TransactionType,
    val description: String? = null,
    val date: Date = Date(),
    /** Firestore doc id — null khi chưa sync lên cloud. */
    val firestoreId: String? = null,
    /** v20: định danh logical ổn định cho sync. */
    val syncId: String = "",
    /**
     * v20: khóa idempotency của lệnh thanh toán tạo ra giao dịch này. Cùng
     * operationId gửi lại không nhân đôi giao dịch. Null cho giao dịch legacy/cọc.
     */
    val operationId: String? = null
)

enum class TransactionType {
    DEPOSIT, // Tiền cọc
    PAYMENT, // Thanh toán
    REFUND // Hoàn tiền
}
