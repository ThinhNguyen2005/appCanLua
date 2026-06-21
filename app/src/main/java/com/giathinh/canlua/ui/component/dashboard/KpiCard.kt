package com.giathinh.canlua.ui.component.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.util.DashboardFormatter

/**
 * Card hiển thị 1 KPI:
 *  - Icon nhỏ + label
 *  - Value lớn (compact format)
 *  - Optional delta vs vụ trước (↑ xanh / ↓ đỏ)
 *
 * @param accentColor màu accent cho icon container, lấy từ AppColors
 * @param highlight nếu true → padding lớn hơn, value text 24sp (dùng cho hero card)
 */
@Composable
fun KpiCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = AppColors.GreenPrimary,
    deltaPercent: Double? = null,
    deltaLabel: String? = null,
    highlight: Boolean = false
) {
    val padding = if (highlight) 16.dp else 14.dp
    val valueStyle = if (highlight) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
        ) {
            // Icon hàng riêng — label đặt dưới full width để không bao giờ xuống dòng
            // do thiếu chỗ. Trước đây icon + label chung Row khiến "Sản lượng đã bán"
            // wrap 2 dòng làm các card lệch chiều cao.
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = AppColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            // Value autoshrink — value số dài "1.234.567 đ" sẽ … chứ không xuống dòng,
            // giữ chiều cao card đồng nhất giữa "1,2 tr" và "12,5 tr".
            Text(
                text = value,
                style = valueStyle.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.TextPrimary
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

            if (deltaPercent != null || deltaLabel != null) {
                Spacer(Modifier.height(6.dp))
                DeltaPill(deltaPercent = deltaPercent, deltaLabel = deltaLabel)
            }
        }
    }
}

@Composable
private fun DeltaPill(deltaPercent: Double?, deltaLabel: String?) {
    val (icon, color, text) = when {
        deltaPercent == null -> Triple(
            Icons.Default.Remove,
            AppColors.TextHint,
            deltaLabel ?: "—"
        )
        deltaPercent > 0 -> Triple(
            Icons.Default.ArrowUpward,
            AppColors.Success,
            "${DashboardFormatter.formatDelta(deltaPercent)} ${deltaLabel ?: ""}".trim()
        )
        deltaPercent < 0 -> Triple(
            Icons.Default.ArrowDownward,
            AppColors.Error,
            "${DashboardFormatter.formatDelta(deltaPercent)} ${deltaLabel ?: ""}".trim()
        )
        else -> Triple(
            Icons.Default.Remove,
            AppColors.TextHint,
            "0% ${deltaLabel ?: ""}".trim()
        )
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
