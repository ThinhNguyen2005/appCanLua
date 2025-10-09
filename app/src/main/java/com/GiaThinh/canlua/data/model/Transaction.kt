package com.GiaThinh.canlua.data.model

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
    indices = [androidx.room.Index(value = ["cardId"])]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: Long,
    val amount: Double, // Số tiền
    val type: TransactionType,
    val description: String? = null,
    val date: Date = Date()
)

enum class TransactionType {
    DEPOSIT, // Tiền cọc
    PAYMENT, // Thanh toán
    REFUND // Hoàn tiền
}
