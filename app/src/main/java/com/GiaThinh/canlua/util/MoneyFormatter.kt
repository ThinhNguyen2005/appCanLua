package com.GiaThinh.canlua.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Format tiền theo cách bà con nông dân đọc — không hiển thị xu/lẻ thập phân
 * và có dòng đọc rút gọn bằng tiếng Việt phía dưới (vd "1 triệu 2", "8 trăm nghìn").
 *
 * Lý do: nông dân khi xem phiếu thường đọc bằng miệng cho người khác nghe,
 * UI hiển thị thêm chữ giúp tránh nhầm số 0.
 */
object MoneyFormatter {

    private val viFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))

    /**
     * Format số tiền dạng "1.200.000 đ" — luôn LÀM TRÒN, không có thập phân.
     * Dùng cho mọi field tiền trên UI.
     */
    fun formatVndShort(amount: Double): String = "${viFormat.format(amount.toLong())} đ"

    /**
     * Quy ra chữ tiếng Việt rút gọn theo cách bà con đọc.
     *
     * Quy tắc:
     *  - Tròn triệu → "1 triệu", "5 triệu chẵn" (chẵn nếu user muốn nhấn mạnh)
     *  - Triệu + trăm nghìn tròn → "1 triệu 2" (= 1.200.000)
     *  - Triệu + nghìn lẻ → "1 triệu 250 nghìn"
     *  - Tròn trăm nghìn dưới triệu → "8 trăm nghìn" (= 800.000)
     *  - Dưới triệu, trên nghìn → "50 nghìn" / "500 nghìn"
     *  - Tròn tỷ → "1 tỷ", "1 tỷ 2 trăm triệu"
     *  - 0 hoặc âm → chuỗi rỗng
     */
    fun toVietnameseWords(amount: Double): String {
        val raw = amount.toLong()
        if (raw <= 0L) return ""
        val abs = abs(raw)

        val ty = abs / 1_000_000_000L
        val afterTy = abs % 1_000_000_000L
        val trieu = afterTy / 1_000_000L
        val afterTrieu = afterTy % 1_000_000L
        val tramNghin = afterTrieu / 100_000L  // số trăm nghìn (0..9)
        val nghin = afterTrieu / 1_000L        // tổng nghìn (0..999)
        val nghinLe = nghin % 100              // phần nghìn lẻ chưa tròn trăm
        val dong = abs % 1_000L

        val parts = mutableListOf<String>()

        if (ty > 0) {
            parts += "$ty tỷ"
            // Có triệu kèm? Đọc thêm trăm triệu nếu tròn (vd "1 tỷ 2 trăm triệu")
            val tramTrieu = trieu / 100
            if (tramTrieu > 0 && trieu % 100 == 0L && afterTrieu == 0L) {
                parts += "$tramTrieu trăm triệu"
                return parts.joinToString(" ")
            }
        }

        if (trieu > 0) {
            parts += "$trieu triệu"
            when {
                afterTrieu == 0L -> Unit
                nghinLe == 0L && tramNghin > 0 -> parts += "$tramNghin"           // "1 triệu 2"
                else -> parts += "$nghin nghìn"                                    // "1 triệu 250 nghìn"
            }
        } else if (nghin > 0) {
            when {
                nghin >= 100 && nghinLe == 0L -> parts += "${nghin / 100} trăm nghìn"  // "8 trăm nghìn"
                else -> parts += "$nghin nghìn"                                         // "50 nghìn"
            }
        } else if (dong > 0) {
            parts += "$dong đồng"
        }

        return parts.joinToString(" ")
    }
}
