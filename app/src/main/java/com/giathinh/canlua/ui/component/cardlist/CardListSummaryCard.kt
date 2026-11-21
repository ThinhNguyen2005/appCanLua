package com.giathinh.canlua.ui.component.cardlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.R
import com.giathinh.canlua.repository.SyncStatus
import com.giathinh.canlua.ui.component.AnimatedNumber
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.theme.AppDimensions
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CardListSummaryCard(
    cardCount: Int,
    totalKg: Double,
    totalAmount: Double,
    syncStatus: SyncStatus,
    showSyncStatus: Boolean,
    modifier: Modifier = Modifier,
    titlePrefix: String = stringResource(R.string.card_list_today_prefix),
    amountLabel: String = stringResource(R.string.card_list_total_income),
    avgPricePerKg: Double? = null,
    bagCount: Int? = null,
    estimatedYieldKgPerCong: Double? = null
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusLg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, AppColors.GreenPrimary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.SpacingMd),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.card_list_summary_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = stringResource(R.string.card_list_summary_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                @Suppress("UNUSED_PARAMETER")
                val ignored = showSyncStatus
            }

            // Bento 2x2 Grid Row 1: Phiếu & Sản lượng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryMetric(
                    label = titlePrefix,
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.weight(1f)
                ) {
                    AnimatedNumber(
                        value = cardCount,
                        formatter = { numberFormat.format(it) },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SummaryMetric(
                    label = stringResource(R.string.card_list_summary_yield),
                    icon = Icons.Outlined.Scale,
                    tint = AppColors.GoldAccent,
                    modifier = Modifier.weight(1f)
                ) {
                    AnimatedNumber(
                        value = totalKg,
                        formatter = { "${numberFormat.format(it)} kg" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Bento 2x2 Grid Row 2: Tổng thu & Số bao
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryMetric(
                    label = amountLabel,
                    icon = Icons.Outlined.Payments,
                    tint = AppColors.Success,
                    modifier = Modifier.weight(1f)
                ) {
                    AnimatedNumber(
                        value = totalAmount,
                        formatter = { "${numberFormat.format(it.toLong())} đ" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SummaryMetric(
                    label = stringResource(R.string.card_list_summary_bag_count),
                    icon = Icons.Outlined.Inventory2,
                    tint = AppColors.Info,
                    modifier = Modifier.weight(1f)
                ) {
                    AnimatedNumber(
                        value = bagCount ?: 0,
                        formatter = { numberFormat.format(it) },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Row 3: chỉ số phụ (đơn giá TB / năng suất ước tính) — chỉ hiện khi có data thực.
            val hasSecondaryMetrics = avgPricePerKg != null || estimatedYieldKgPerCong != null
            if (hasSecondaryMetrics) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (avgPricePerKg != null) {
                        SecondaryMetric(
                            label = stringResource(R.string.card_list_summary_avg_price),
                            value = "${numberFormat.format(avgPricePerKg.toLong())} đ/kg",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (estimatedYieldKgPerCong != null) {
                        SecondaryMetric(
                            label = stringResource(R.string.card_list_summary_yield_per_cong),
                            value = "${numberFormat.format(estimatedYieldKgPerCong.toLong())} kg/công",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    valueContent: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppDimensions.CornerRadiusMd))
            .background(AppColors.SurfaceContainer)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(AppDimensions.CornerRadiusSm))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
        valueContent()
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SecondaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppDimensions.CornerRadiusMd))
            .background(AppColors.SurfaceContainer)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
