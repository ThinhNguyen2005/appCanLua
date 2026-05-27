package com.GiaThinh.canlua.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.runtime.Immutable
import java.util.Date

@Immutable
@Entity(
    tableName = "cards",
    indices = [
        // Index cơ bản cho ownerUid (tất cả query theo user)
        androidx.room.Index(value = ["ownerUid"]),
        // H-02: Composite index để tăng tốc getAllCards (ORDER BY date DESC per user)
        androidx.room.Index(value = ["ownerUid", "date"]),
        // H-02: Index cho getCardsByRiceVariety + getSuggestedRiceVarieties
        androidx.room.Index(value = ["ownerUid", "riceVariety"])
    ]
)
data class Card(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /**
     * Firebase UID của chủ phiếu — khoá cô lập data per-user trên cùng device.
     * Empty string = card "orphan" tạo trước khi migration v12; sẽ được
     * `claimOrphanCards()` gán cho user đầu tiên đăng nhập sau update.
     * Mọi DAO query lọc theo field này → user A login máy chung không thấy phiếu của B.
     */
    val ownerUid: String = "",
    val name: String,
    val cccd: String? = null,
    val traderName: String = "",
    val date: Date,
    val totalWeight: Double = 0.0,
    val bagWeight: Double = 0.0,
    val impurityWeight: Double = 0.0,
    val netWeight: Double = 0.0,
    val depositAmount: Double = 0.0,
    val pricePerKg: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val bagCount: Int = 0,
    val isLocked: Boolean = false,

    // === PHASE 1 NEW FIELDS ===
    val riceVariety: String = "",          // Giống lúa (ST25, OM18, Jasmine 85...)
    val moisturePercent: Double = 0.0,     // Độ ẩm (%)
    val seasonLabel: String = "",          // Nhãn vụ mùa (Đông Xuân 2026, Hè Thu 2026...)
    val qrToken: String? = null,           // Hash SHA-256 cho xác thực QR Handshake
    val lockedByTraderId: String? = null,  // UID thương lái đã xác thực QR

    // === PHASE 2.6: GPS location for map ===
    val latitude: Double? = null,          // Tọa độ GPS lúc tạo thẻ (nơi cân lúa)
    val longitude: Double? = null,

    // === PHASE 2.8: Contact + field address ===
    val traderPhone: String = "",          // SĐT thương lái (tap để gọi)
    val fieldAddress: String = "",          // Địa chỉ ruộng (reverse geocode → tap mở map)

    // === PHASE 4: Cross-device sync (v13) ===
    /**
     * Firestore document ID — null khi chưa từng được push lên cloud.
     * Dùng làm khoá dedup khi pull về máy mới: cùng `firestoreId` → same card.
     * Local Room id (autoincrement) khác nhau giữa các thiết bị nên không dùng được.
     */
    val firestoreId: String? = null,

    // === PHASE 4 (v14): Conflict resolution timestamp ===
    /**
     * Thời điểm sửa đổi gần nhất (ms epoch). Repository tự stamp mỗi lần
     * insert/update. Khi pull từ Firestore, so sánh `lastModifiedMs` local vs
     * `syncTimestamp` cloud → bản nào mới hơn thắng. Tránh ghi đè sửa offline
     * của 1 máy bằng bản cloud cũ hơn (cloud-wins blanket trước đây).
     */
    val lastModifiedMs: Long = 0L,
    val isPaid: Boolean = false,

    // === PHASE 6 (v17): Per-card weigh modes ===
    /**
     * Tạp chất nhập theo % thay vì kg. Khi true: `impurityWeight` field hiểu là tỉ lệ %.
     * Mặc định false → giữ behavior cũ (kg tuyệt đối) cho phiếu cũ.
     */
    val impurityIsPercent: Boolean = false,
    /**
     * Cách tính bao bì:
     * - false (Cách A, default): `totalBag = bagCount × bagWeight` (1 bao đơn vị × số bao)
     * - true (Cách B, mẫu): cân `bagSampleCount` bao mẫu ra `bagSampleTotalWeight` kg
     *   → unit = total/count → × tổng số bao
     */
    val bagMethodIsSampling: Boolean = false,
    val bagSampleCount: Int = 0,
    val bagSampleTotalWeight: Double = 0.0,
    /**
     * Quy cách nhập KG trong GridCell:
     * - "SMALL" (default): auto-confirm sau 3 chữ số → div 10 (vd "503" → 50.3)
     * - "LARGE": auto-confirm sau 4 chữ số → div 10 (vd "1503" → 150.3)
     */
    val weightInputMode: String = "SMALL"
)
