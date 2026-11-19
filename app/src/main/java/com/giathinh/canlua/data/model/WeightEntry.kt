package com.giathinh.canlua.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "weight_entries",
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
        androidx.room.Index(value = ["firestoreId"]),
        androidx.room.Index(value = ["cardId", "ordinal"])
    ]
)
data class WeightEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: Long,
    val weight: Double, // Khối lượng (kg)
    val bagWeight: Double = 0.0, // Khối lượng bao bì
    val impurityWeight: Double = 0.0, // Khối lượng tạp chất
    val netWeight: Double = 0.0, // Khối lượng thực tế (weight - bagWeight - impurityWeight)
    val timestamp: Long = System.currentTimeMillis(),
    /** Firestore doc id — null khi chưa sync lên cloud. Dùng dedup khi pull. */
    val firestoreId: String? = null,
    /** v20: định danh logical ổn định cho sync (không đổi khi sửa/xóa bao khác). */
    val syncId: String = "",
    /**
     * v20: thứ tự ổn định trong phiếu. Gán = max(ordinal)+1 khi thêm; KHÔNG đánh
     * lại khi xóa. Settlement và sync dùng cùng thứ tự (timestamp, id) để nhất quán.
     */
    val ordinal: Int = 0
)
