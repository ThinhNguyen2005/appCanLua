package com.GiaThinh.canlua.ui.component.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.util.DashboardFormatter

/**
 * Quick Stats Grid — 3 thẻ song song hiển thị số liệu lifetime trên trang cá nhân.
 *
 * Style: flat surface card với border subtle (không glassmorphism) — bởi vì header
 * đã được đơn giản hóa không còn nền màu, nên các card này cũng không đứng trên gradient.
 *
 * 3 chỉ số tổng quan:
 *  1. 🌾 Số vụ đã canh tác/thu mua
 *  2. ⚖️ Tổng khối lượng tịnh (tấn)
 *  3. 💰 Tổng doanh thu / chi phí (VNĐ)
 *
 * @param seasonCount số vụ đã ghi nhận
 * @param totalNetWeight tổng khối lượng tịnh kg toàn lifetime
 * @param totalRevenue tổng doanh thu (Farmer) hoặc tổng đã chi (Trader)
 * @param revenueLabel custom label cho card #3 ("Doanh thu" hoặc "Đã chi")
 */
@Composable
fun QuickStatsGlassGrid(
    seasonCount: Int,
    totalNetWeight: Double,
    totalRevenue: Double,
    modifier: Modifier = Modifier,
    revenueLabel: String? = null
) {
    val resolvedRevenueLabel = revenueLabel ?: stringResource(R.string.profile_stats_revenue)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassStatCard(
            icon = Icons.Outlined.Eco,
            value = seasonCount.toString(),
            label = stringResource(R.string.profile_stats_seasons),
            modifier = Modifier.weight(1f)
        )
        GlassStatCard(
            icon = Icons.Outlined.Scale,
            value = DashboardFormatter.weight(totalNetWeight),
            label = stringResource(R.string.profile_stats_yield),
            modifier = Modifier.weight(1f)
        )
        GlassStatCard(
            icon = Icons.Outlined.AccountBalanceWallet,
            value = DashboardFormatter.money(totalRevenue),
            label = resolvedRevenueLabel,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GlassStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(104.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.SurfaceContainer)
            .border(
                width = 1.dp,
                color = AppColors.Divider,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(AppColors.GreenSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = value,
                    color = AppColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    text = label,
                    color = AppColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
