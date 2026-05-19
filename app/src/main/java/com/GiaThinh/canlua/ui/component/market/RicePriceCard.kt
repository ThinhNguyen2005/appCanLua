package com.GiaThinh.canlua.ui.component.market

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.GiaThinh.canlua.data.model.RicePrice
import com.GiaThinh.canlua.ui.theme.AppColors
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Card hiển thị giá lúa theo giống — design theo PRD dòng 109-115.
 * - Tên giống lớn, badge xu hướng (UP/DOWN/STABLE)
 * - Min / Max / Avg 7 ngày
 * - Tap để mở chart
 */
@Composable
fun RicePriceCard(
    price: RicePrice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (trendColor, trendIcon, trendLabel) = when (price.trend) {
        "UP" -> Triple(AppColors.Success, Icons.AutoMirrored.Filled.TrendingUp, "Tăng")
        "DOWN" -> Triple(AppColors.Error, Icons.AutoMirrored.Filled.TrendingDown, "Giảm")
        else -> Triple(AppColors.Info, Icons.AutoMirrored.Filled.TrendingFlat, "Ổn định")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header: variety name + trend badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = price.variety,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                TrendBadge(
                    color = trendColor,
                    icon = trendIcon,
                    label = trendLabel
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = formatRelativeTime(price.updatedAt),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextHint
            )

            Spacer(Modifier.height(16.dp))

            // Price grid: 3 columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PriceColumn(
                    label = "Thấp nhất",
                    value = formatPrice(price.priceMin),
                    color = AppColors.TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                PriceColumn(
                    label = "Cao nhất",
                    value = formatPrice(price.priceMax),
                    color = AppColors.GreenDark,
                    isHighlight = true,
                    modifier = Modifier.weight(1f)
                )
                PriceColumn(
                    label = "TB 7 ngày",
                    value = formatPrice(price.priceAvg7d),
                    color = AppColors.GoldDark,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.SurfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nguồn: ${price.traderName ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Xem biểu đồ →",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.GreenPrimary
                )
            }
        }
    }
}

@Composable
private fun PriceColumn(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isHighlight) AppColors.GreenSurface else AppColors.SurfaceContainer)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextHint,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = "đ/kg",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextHint,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun TrendBadge(
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

private fun formatPrice(price: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
    return nf.format(price.toLong())
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "Vừa cập nhật"
        minutes < 60 -> "Cập nhật $minutes phút trước"
        hours < 24 -> "Cập nhật $hours giờ trước"
        days < 7 -> "Cập nhật $days ngày trước"
        else -> "Đã lâu"
    }
}
