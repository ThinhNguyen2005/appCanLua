package com.giathinh.canlua.ui.component.market

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.R
import com.giathinh.canlua.data.model.RicePrice
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.util.HapticUtil
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Card hiển thị giá lúa theo giống — list-item ngang gọn.
 * - Tên giống + badge xu hướng (UP/DOWN/STABLE) ở header
 * - 1 dòng meta: loại sản phẩm · khu vực · cập nhật
 * - 3 cột giá (Thấp/Cao/TB7)
 * - Icon chevron-right ở góc phải, tap để mở chart
 */
@Composable
fun RicePriceCard(
    price: RicePrice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (trendColor, trendIcon, trendLabel) = when (price.trend) {
        "UP" -> Triple(AppColors.Success, Icons.AutoMirrored.Filled.TrendingUp, stringResource(R.string.market_bid_trend_up))
        "DOWN" -> Triple(AppColors.Error, Icons.AutoMirrored.Filled.TrendingDown, stringResource(R.string.market_bid_trend_down))
        else -> Triple(AppColors.Info, Icons.AutoMirrored.Filled.TrendingFlat, stringResource(R.string.market_bid_trend_stable))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                HapticUtil.tick(context)
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            AppColors.Divider.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header: variety name + trend badge + chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = price.variety,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                TrendBadge(
                    color = trendColor,
                    icon = trendIcon,
                    label = trendLabel
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.market_card_chart_desc),
                    tint = AppColors.TextHint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Meta row: rice type · region · updated (gọn 1 dòng)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val riceTypeBg = when {
                    price.riceType.contains("ướt", ignoreCase = true) -> Color(0xFFE3F2FD)
                    price.riceType.contains("Khô", ignoreCase = true) -> Color(0xFFFFF3E0)
                    price.riceType.contains("nếp", ignoreCase = true) -> Color(0xFFFCE4EC)
                    price.riceType.contains("gạo", ignoreCase = true) -> Color(0xFFF3E5F5)
                    price.riceType.contains("lúa", ignoreCase = true) -> Color(0xFFE8F5E9)
                    else -> Color(0xFFE0F2F1)
                }
                val riceTypeColor = when {
                    price.riceType.contains("ướt", ignoreCase = true) -> Color(0xFF1E88E5)
                    price.riceType.contains("Khô", ignoreCase = true) -> Color(0xFFFB8C00)
                    price.riceType.contains("nếp", ignoreCase = true) -> Color(0xFFD81B60)
                    price.riceType.contains("gạo", ignoreCase = true) -> Color(0xFF8E24AA)
                    price.riceType.contains("lúa", ignoreCase = true) -> Color(0xFF43A047)
                    else -> Color(0xFF00796B)
                }
                MetaBadge(
                    text = price.riceType,
                    background = riceTypeBg,
                    foreground = riceTypeColor
                )
                MetaBadge(
                    text = price.region.ifBlank { "ĐBSCL" },
                    background = AppColors.SurfaceContainer,
                    foreground = AppColors.TextSecondary
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint
                )
                Text(
                    text = formatRelativeTime(price.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint
                )
            }

            Spacer(Modifier.height(12.dp))

            // Price grid: 3 columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PriceColumn(
                    label = stringResource(R.string.market_price_min_label),
                    value = formatPrice(price.priceMin),
                    color = AppColors.TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                PriceColumn(
                    label = stringResource(R.string.market_price_max_label),
                    value = formatPrice(price.priceMax),
                    color = AppColors.GreenDark,
                    isHighlight = true,
                    modifier = Modifier.weight(1f)
                )
                PriceColumn(
                    label = stringResource(R.string.market_price_avg_label),
                    value = formatPrice(price.priceAvg7d),
                    color = AppColors.GoldDark,
                    modifier = Modifier.weight(1f)
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
            color = AppColors.TextHint
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = stringResource(R.string.market_price_unit),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextHint
        )
    }
}

@Composable
private fun MetaBadge(
    text: String,
    background: Color,
    foreground: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            fontWeight = FontWeight.Medium
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
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
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

private val PRICE_FORMAT = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))

private fun formatPrice(price: Double): String = PRICE_FORMAT.format(price.toLong())

@Composable
private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 0) return stringResource(R.string.news_time_just_now)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> stringResource(R.string.news_time_just_now)
        minutes < 60 -> stringResource(R.string.news_time_minutes_ago, minutes)
        hours < 24 -> stringResource(R.string.news_time_hours_ago, hours)
        days < 7 -> stringResource(R.string.news_time_days_ago, days)
        days < 30 -> stringResource(R.string.news_time_weeks_ago, days / 7)
        else -> stringResource(R.string.news_time_months_ago, days / 30)
    }
}
