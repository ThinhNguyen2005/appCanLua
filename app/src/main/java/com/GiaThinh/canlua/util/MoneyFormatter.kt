package com.GiaThinh.canlua.util

import android.content.Context
import com.GiaThinh.canlua.R
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
     * Quy ra chữ rút gọn theo ngôn ngữ cấu hình.
     */
    fun toWords(amount: Double, context: Context): String {
        val labels = MoneyLabels(
            billion = context.getString(R.string.money_unit_billion),
            million = context.getString(R.string.money_unit_million),
            hundredThousand = context.getString(R.string.money_unit_hundred_thousand),
            thousand = context.getString(R.string.money_unit_thousand),
            dong = context.getString(R.string.money_unit_dong),
            hundred = context.getString(R.string.money_unit_hundred),
        )
        return toWordsCore(amount, labels)
    }

    /**
     * Pure form — không phụ thuộc Context, dễ unit test.
     * Quy ra chữ rút gọn theo ngôn ngữ cấu hình (vd "1 triệu 2", "8 trăm nghìn").
     *
     * Quy ước đọc:
     * - Tỷ: "1 tỷ" / "1 tỷ 2 trăm triệu" (nếu phần sau tròn trăm triệu)
     * - Triệu: "1 triệu" / "1 triệu 2" (tròn trăm nghìn) / "1 triệu 250 nghìn" (lẻ)
     * - Nghìn: "8 trăm nghìn" (tròn trăm) / "50 nghìn"
     * - Đồng: "5 đồng"
     */
    internal fun toWordsCore(amount: Double, labels: MoneyLabels): String {
        val raw = amount.toLong()
        if (raw <= 0L) return ""
        val absValue = abs(raw)

        val ty = absValue / 1_000_000_000L
        val afterTy = absValue % 1_000_000_000L
        val trieu = afterTy / 1_000_000L
        val afterTrieu = afterTy % 1_000_000L
        val tramNghin = afterTrieu / 100_000L  // số trăm nghìn (0..9)
        val nghin = afterTrieu / 1_000L        // tổng nghìn (0..999)
        val nghinLe = nghin % 100              // phần nghìn lẻ chưa tròn trăm
        val dong = absValue % 1_000L

        val parts = mutableListOf<String>()

        if (ty > 0) {
            parts += "$ty ${labels.billion}"
            // Có triệu kèm? Đọc thêm trăm triệu nếu phần sau tròn (vd "1 tỷ 2 trăm triệu")
            val tramTrieu = trieu / 100
            if (tramTrieu > 0 && trieu % 100 == 0L && afterTrieu == 0L) {
                parts += "$tramTrieu ${labels.hundred} ${labels.million}"
                return parts.joinToString(" ")
            }
        }

        if (trieu > 0) {
            parts += "$trieu ${labels.million}"
            when {
                afterTrieu == 0L -> Unit
                nghinLe == 0L && tramNghin > 0 -> parts += "$tramNghin"           // "1 triệu 2"
                else -> parts += "$nghin ${labels.thousand}"                       // "1 triệu 250 nghìn"
            }
        } else if (nghin > 0) {
            when {
                nghin >= 100 && nghinLe == 0L -> parts += "${nghin / 100} ${labels.hundredThousand}"  // "8 trăm nghìn"
                else -> parts += "$nghin ${labels.thousand}"                                          // "50 nghìn"
            }
        } else if (dong > 0) {
            parts += "$dong ${labels.dong}"
        }

        return parts.joinToString(" ")
    }
}

/**
 * Bộ label ngôn ngữ cho MoneyFormatter.toWordsCore.
 * Tách ra khỏi Context để core logic chạy thuần JVM, dễ unit test.
 */
data class MoneyLabels(
    val billion: String,
    val million: String,
    val hundredThousand: String,
    val thousand: String,
    val dong: String,
    val hundred: String,
)