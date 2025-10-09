package com.GiaThinh.canlua.data.model

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
    indices = [androidx.room.Index(value = ["cardId"])]
)
data class WeightEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: Long,
    val weight: Double, // Khối lượng (kg)
    val bagWeight: Double = 0.0, // Khối lượng bao bì
    val impurityWeight: Double = 0.0, // Khối lượng tạp chất
    val netWeight: Double = 0.0, // Khối lượng thực tế (weight - bagWeight - impurityWeight)
    val timestamp: Long = System.currentTimeMillis()
)
