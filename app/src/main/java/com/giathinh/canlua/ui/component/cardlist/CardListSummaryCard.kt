package com.giathinh.canlua.ui.component.cardlist

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.R
import com.giathinh.canlua.repository.SyncStatus
import com.giathinh.canlua.ui.theme.AppColors
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, AppColors.Divider.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top-edge accent strip — thay thế GreenSurface overlay để có visual weight
            // mà vẫn giữ tokens đồng bộ (CardBg + 1dp border).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(AppColors.GreenPrimary)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tổng quan hôm nay",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = "Theo dõi nhanh số phiếu, sản lượng và dòng tiền",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // showSyncStatus hiện được giữ nguyên signature để tương thích ngược,
                    // nhưng ở bản hiện tại KHÔNG render SyncStatusPulse theo yêu cầu
                    // "bỏ chấm pulse ở trang Cân lúa".
                    @Suppress("UNUSED_PARAMETER")
                    val ignored = showSyncStatus
                }

                // Row 1: 3 metric chính (giữ nguyên)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryMetric(
                        label = titlePrefix,
                        value = numberFormat.format(cardCount),
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                        tint = AppColors.GreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetric(
                        label = "Sản lượng",
                        value = "${numberFormat.format(totalKg)} kg",
                        icon = Icons.Outlined.Scale,
                        tint = AppColors.GoldAccent,
                        modifier = Modifier.weight(1.12f)
                    )
                    SummaryMetric(
                        label = amountLabel,
                        value = "${numberFormat.format(totalAmount.toLong())}đ",
                        icon = Icons.Outlined.Payments,
                        tint = AppColors.Success,
                        modifier = Modifier.weight(1.25f)
                    )
                }

                // Row 2: chỉ số phụ — chỉ hiện khi có data thực, tránh layout trống gây rối.
                val hasSecondaryMetrics = avgPricePerKg != null || bagCount != null || estimatedYieldKgPerCong != null
                if (hasSecondaryMetrics) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (avgPricePerKg != null) {
                            SecondaryMetric(
                                label = "Đơn giá TB",
                                value = "${numberFormat.format(avgPricePerKg.toLong())} đ/kg",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (bagCount != null) {
                            SecondaryMetric(
                                label = "Số bao",
                                value = numberFormat.format(bagCount),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (estimatedYieldKgPerCong != null) {
                            SecondaryMetric(
                                label = "Năng suất ước tính",
                                value = "${numberFormat.format(estimatedYieldKgPerCong.toLong())} kg/công",
                                modifier = Modifier.weight(1.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceContainer)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(17.dp)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceContainer)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
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
