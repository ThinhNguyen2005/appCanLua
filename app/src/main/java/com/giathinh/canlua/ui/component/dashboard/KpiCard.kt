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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.R
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.theme.AppDimensions
import com.giathinh.canlua.ui.util.DashboardFormatter

/**
 * Định nghĩa ngữ nghĩa của biến động chỉ số:
 *  - [POSITIVE_IS_GOOD]: Tăng là tốt (Xanh), giảm là xấu (Đỏ) - ví dụ: Sản lượng, Doanh thu.
 *  - [NEGATIVE_IS_GOOD]: Tăng là xấu (Đỏ), giảm là tốt (Xanh) - ví dụ: Công nợ, Chi phí, Tạp chất.
 *  - [NEUTRAL]: Không mang sắc thái tốt/xấu (Màu trung tính TextSecondary).
 */
enum class TrendSentiment {
    POSITIVE_IS_GOOD,
    NEGATIVE_IS_GOOD,
    NEUTRAL
}

/**
 * Card hiển thị 1 KPI:
 *  - Icon nhỏ + label (cố định 2 dòng để đồng bộ baseline giá trị giữa các card)
 *  - Value lớn (caller cần format compact, ellipsis chỉ đóng vai trò fallback)
 *  - Optional delta vs vụ trước (tách biệt chiều mũi tên toán học và màu sắc ngữ nghĩa)
 *  - Hỗ trợ reserveDeltaSpace để giữ chiều cao card bằng nhau khi nằm cạnh card có delta
 *
 * @param accentColor màu accent cho icon container, lấy từ AppColors
 * @param highlight nếu true → padding lớn hơn, typography.headlineMedium (dùng cho hero card)
 * @param isPositiveGood cờ nhanh báo hiệu delta dương có phải là tích cực không
 * @param sentiment phân loại sắc thái ngữ nghĩa của delta
 * @param reserveDeltaSpace dành sẵn chiều cao tương đương delta pill khi card không có delta
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
    highlight: Boolean = false,
    isPositiveGood: Boolean = true,
    sentiment: TrendSentiment = if (isPositiveGood) TrendSentiment.POSITIVE_IS_GOOD else TrendSentiment.NEGATIVE_IS_GOOD,
    reserveDeltaSpace: Boolean = false
) {
    val padding = if (highlight) AppDimensions.SpacingMd else 14.dp
    val valueStyle = if (highlight) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimensions.SpacingMd),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
        ) {
            // Icon hàng riêng với background shape trực tiếp
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = accentColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ),
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

            // Label cố định minLines = 2, maxLines = 2 để baseline của value bên dưới luôn thẳng hàng
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = AppColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                ),
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(AppDimensions.SpacingXs))

            // Value compact — caller đảm bảo formatter compact, ellipsis đóng vai trò fallback
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

            val hasDelta = deltaPercent != null || deltaLabel != null
            if (hasDelta) {
                Spacer(Modifier.height(6.dp))
                DeltaPill(
                    deltaPercent = deltaPercent,
                    deltaLabel = deltaLabel,
                    sentiment = sentiment
                )
            } else if (reserveDeltaSpace) {
                // Giữ chỗ cho khoảng cách 6dp + chiều cao pill 24dp để bằng chiều cao card có delta
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun DeltaPill(
    deltaPercent: Double?,
    deltaLabel: String?,
    sentiment: TrendSentiment
) {
    val icon = when {
        deltaPercent == null || deltaPercent == 0.0 -> Icons.Default.Remove
        deltaPercent > 0 -> Icons.Default.ArrowUpward
        else -> Icons.Default.ArrowDownward
    }

    val color = when {
        deltaPercent == null || deltaPercent == 0.0 -> AppColors.TextHint
        deltaPercent > 0 -> when (sentiment) {
            TrendSentiment.POSITIVE_IS_GOOD -> AppColors.Success
            TrendSentiment.NEGATIVE_IS_GOOD -> AppColors.Error
            TrendSentiment.NEUTRAL -> AppColors.TextSecondary
        }
        else -> when (sentiment) {
            TrendSentiment.POSITIVE_IS_GOOD -> AppColors.Error
            TrendSentiment.NEGATIVE_IS_GOOD -> AppColors.Success
            TrendSentiment.NEUTRAL -> AppColors.TextSecondary
        }
    }

    val text = when {
        deltaPercent == null -> deltaLabel ?: "—"
        else -> {
            val formattedDelta = DashboardFormatter.formatDelta(deltaPercent)
            if (!deltaLabel.isNullOrBlank()) {
                stringResource(R.string.dashboard_delta_with_label, formattedDelta, deltaLabel)
            } else {
                formattedDelta
            }
        }
    }

    Row(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(AppDimensions.CornerRadiusSm)
            )
            .padding(
                horizontal = AppDimensions.SpacingSm,
                vertical = AppDimensions.SpacingXs
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingXs)
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
