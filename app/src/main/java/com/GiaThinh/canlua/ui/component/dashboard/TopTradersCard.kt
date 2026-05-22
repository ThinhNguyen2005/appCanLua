package com.GiaThinh.canlua.ui.component.dashboard

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.data.model.TraderStat
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.util.DashboardFormatter

/**
 * Card top thương lái mua nhiều nhất trong vụ.
 * Mỗi item: rank circle · tên · revenue · progress bar relative · số phiếu.
 */
@Composable
fun TopTradersCard(
    items: List<TraderStat>,
    modifier: Modifier = Modifier
) {
    val maxRevenue = items.maxOfOrNull { it.revenue } ?: 1.0

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_top_traders_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )

            Spacer(Modifier.height(14.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.profile_no_deals_this_season),
                        fontSize = 13.sp,
                        color = AppColors.TextHint
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items.forEachIndexed { idx, trader ->
                        TraderRow(
                            rank = idx + 1,
                            trader = trader,
                            relativeWidth = (trader.revenue / maxRevenue).toFloat()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TraderRow(rank: Int, trader: TraderStat, relativeWidth: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Rank circle
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(rankColor(rank).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = rankColor(rank)
            )
        }

        Spacer(Modifier.size(12.dp))

        // Name + progress + revenue
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trader.traderName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = DashboardFormatter.money(trader.revenue),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.GreenPrimary
                )
            }

            Spacer(Modifier.height(4.dp))

            // Progress bar relative + deals count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(AppColors.SurfaceContainer)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(relativeWidth.coerceIn(0.05f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(rankColor(rank))
                    )
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.profile_slip_count, trader.deals),
                    fontSize = 11.sp,
                    color = AppColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** Top 1 = vàng, 2 = bạc, 3 = đồng, còn lại = xanh primary. */
private fun rankColor(rank: Int) = when (rank) {
    1 -> androidx.compose.ui.graphics.Color(0xFFF9A825)  // Gold
    2 -> androidx.compose.ui.graphics.Color(0xFF90A4AE)  // Silver
    3 -> androidx.compose.ui.graphics.Color(0xFF8D6E63)  // Bronze
    else -> androidx.compose.ui.graphics.Color(0xFF2E7D32)  // Green
}
