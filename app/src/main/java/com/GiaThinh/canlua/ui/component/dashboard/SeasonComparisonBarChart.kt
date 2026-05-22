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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.data.model.SeasonStats
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.util.DashboardFormatter

/**
 * So sánh sản lượng (tấn) giữa các vụ — custom Canvas bar chart.
 *
 * Dùng Canvas thay Vico để tránh API surface lớn cho chart đơn giản này.
 * Hiển thị tối đa 6 vụ, mỗi cột có label vụ rút gọn (ĐX'26 / HT'25 / TĐ'24)
 * và value text trên đầu cột.
 *
 * Highlight cột đang được chọn → đậm hơn.
 */
@Composable
fun SeasonComparisonBarChart(
    seasons: List<SeasonStats>,
    selectedSeason: String?,
    metric: ChartMetric = ChartMetric.WEIGHT,
    modifier: Modifier = Modifier
) {
    val displayItems = remember(seasons) { seasons.take(6).reversed() }  // oldest → newest left to right
    val maxValue = remember(displayItems, metric) {
        displayItems.maxOfOrNull { metric.extract(it) } ?: 0.0
    }

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
                text = stringResource(
                    when (metric) {
                        ChartMetric.WEIGHT -> R.string.profile_season_comparison_weight
                        ChartMetric.REVENUE -> R.string.profile_season_comparison_revenue
                    }
                ),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )

            Spacer(Modifier.height(20.dp))

            if (displayItems.isEmpty() || maxValue == 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.profile_season_comparison_empty),
                        fontSize = 13.sp,
                        color = AppColors.TextHint
                    )
                }
            } else {
                BarChartCanvas(
                    items = displayItems,
                    maxValue = maxValue,
                    selectedSeason = selectedSeason,
                    metric = metric
                )
            }
        }
    }
}

@Composable
private fun BarChartCanvas(
    items: List<SeasonStats>,
    maxValue: Double,
    selectedSeason: String?,
    metric: ChartMetric
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { season ->
            val value = metric.extract(season)
            val ratio = (value / maxValue).toFloat().coerceIn(0.05f, 1f)
            val isSelected = season.season == selectedSeason

            Bar(
                value = value,
                ratio = ratio,
                label = shortLabel(season.season),
                isSelected = isSelected,
                metric = metric,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Bar(
    value: Double,
    ratio: Float,
    label: String,
    isSelected: Boolean,
    metric: ChartMetric,
    modifier: Modifier = Modifier
) {
    val barColor = if (isSelected) AppColors.GreenPrimary else AppColors.GreenLight.copy(alpha = 0.55f)
    val barHeight = (140 * ratio).toInt().dp

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Value text trên đầu cột
        Text(
            text = when (metric) {
                ChartMetric.WEIGHT -> DashboardFormatter.weight(value)
                ChartMetric.REVENUE -> DashboardFormatter.money(value)
            },
            fontSize = 9.sp,
            color = AppColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )

        Spacer(Modifier.size(4.dp))

        // Bar — single Box với background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(barColor)
        )

        Spacer(Modifier.size(6.dp))

        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) AppColors.GreenPrimary else AppColors.TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

enum class ChartMetric {
    WEIGHT, REVENUE;

    fun extract(stats: SeasonStats): Double = when (this) {
        WEIGHT -> stats.totalNetWeight
        REVENUE -> stats.totalRevenue
    }
}

/** Rút gọn "Đông Xuân 2026" → "ĐX'26", "Hè Thu 2025" → "HT'25". */
private fun shortLabel(season: String): String {
    val parts = season.split(" ")
    val yearShort = parts.lastOrNull()?.takeLast(2) ?: ""
    val prefix = when {
        season.startsWith("Đông Xuân") -> "ĐX"
        season.startsWith("Hè Thu") -> "HT"
        season.startsWith("Thu Đông") -> "TĐ"
        else -> parts.firstOrNull()?.take(3) ?: season.take(3)
    }
    return if (yearShort.isNotEmpty()) "$prefix'$yearShort" else prefix
}
