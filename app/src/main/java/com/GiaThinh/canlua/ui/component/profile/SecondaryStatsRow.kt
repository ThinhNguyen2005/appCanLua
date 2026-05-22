package com.GiaThinh.canlua.ui.component.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.util.DashboardFormatter

/**
 * Hàng pills phụ — các yếu tố KHÔNG cùng cấp với KPI chính (Sản lượng, Doanh thu).
 *
 * Theo yêu cầu phân cấp UI: tạp chất, độ ẩm, tỷ lệ khô/ướt là chỉ số bổ trợ
 * nên kích thước nhỏ hơn KpiCard chính, font 11sp, padding 6×10dp.
 *
 * Layout 2 dòng:
 *  - Dòng 1: Độ ẩm TB · Tạp chất (kg)
 *  - Dòng 2: Lúa khô (≤14%) · Lúa ướt (>14%)
 */
@Composable
fun SecondaryStatsRow(
    avgMoisture: Double,
    totalImpurity: Double,
    dryCardCount: Int,
    wetCardCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Dòng 1: Độ ẩm TB + Tạp chất
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SecondaryPill(
                icon = Icons.Filled.WaterDrop,
                label = stringResource(R.string.profile_stats_avg_moisture),
                value = if (avgMoisture > 0) DashboardFormatter.percent(avgMoisture) else "—",
                tint = AppColors.Info,
                modifier = Modifier.weight(1f)
            )
            SecondaryPill(
                icon = Icons.Filled.CleaningServices,
                label = stringResource(R.string.profile_stats_impurity),
                value = if (totalImpurity > 0) DashboardFormatter.weight(totalImpurity) else "—",
                tint = AppColors.Warning,
                modifier = Modifier.weight(1f)
            )
        }

        // Dòng 2: Combined dry/wet pill
        DryWetSplitPill(
            dryCount = dryCardCount,
            wetCount = wetCardCount
        )
    }
}

@Composable
private fun SecondaryPill(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
        }
        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                color = AppColors.TextHint,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 13.sp,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * Pill ghép đôi hiển thị tỷ lệ Lúa khô vs Lúa ướt trong cùng 1 thẻ rộng.
 * Phân định rõ ràng bằng vạch divider mảnh, 2 nửa cân bằng visually.
 */
@Composable
private fun DryWetSplitPill(
    dryCount: Int,
    wetCount: Int,
    modifier: Modifier = Modifier
) {
    val dryColor = AppColors.GreenPrimary    // chuẩn lưu kho
    val wetColor = Color(0xFF1976D2)         // cần sấy

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceContainer)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SplitHalf(
            icon = Icons.Filled.Grass,
            label = stringResource(R.string.profile_stats_dry_rice),
            value = stringResource(R.string.profile_slip_count, dryCount),
            tint = dryColor,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(width = 1.dp, height = 28.dp)
                .background(AppColors.Divider)
        )
        SplitHalf(
            icon = Icons.Filled.Opacity,
            label = stringResource(R.string.profile_stats_wet_rice),
            value = stringResource(R.string.profile_slip_count, wetCount),
            tint = wetColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SplitHalf(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                color = AppColors.TextHint,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = value,
                fontSize = 12.sp,
                color = tint,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
