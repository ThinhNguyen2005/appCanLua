package com.GiaThinh.canlua.ui.component.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.data.model.SeasonStats
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.util.DashboardFormatter

// Singleton — tránh tạo SimpleDateFormat mới mỗi lần forEach season.
private val VI_LOCALE_BAR: java.util.Locale = java.util.Locale.forLanguageTag("vi-VN")
private val MONTH_FMT: java.text.SimpleDateFormat = java.text.SimpleDateFormat("MM", VI_LOCALE_BAR)
private val YEAR_FMT: java.text.SimpleDateFormat = java.text.SimpleDateFormat("yyyy", VI_LOCALE_BAR)

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
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
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
                        style = MaterialTheme.typography.bodyMedium,
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
    val context = LocalContext.current
    val isScrollable = items.size > 4
    val rowModifier = if (isScrollable) {
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .horizontalScroll(rememberScrollState())
    } else {
        Modifier
            .fillMaxWidth()
            .height(180.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { season ->
            val value = metric.extract(season)
            val ratio = (value / maxValue).toFloat().coerceIn(0.05f, 1f)
            val isSelected = season.season == selectedSeason
            val (prefix, year) = remember(season.lastDate, season.season) {
                if (season.lastDate > 0L) {
                    val date = java.util.Date(season.lastDate)
                    val monthNum = MONTH_FMT.format(date)
                    val monthLabel = "Tháng $monthNum"
                    val yearLabel = YEAR_FMT.format(date)
                    monthLabel to yearLabel
                } else {
                    seasonLabelParts(season.season)
                }
            }

            Bar(
                value = value,
                ratio = ratio,
                labelPrefix = prefix,
                labelYear = year,
                isSelected = isSelected,
                metric = metric,
                modifier = if (isScrollable) Modifier.width(72.dp) else Modifier.weight(1f),
                onClick = {
                    Toast.makeText(context, season.season, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun Bar(
    value: Double,
    ratio: Float,
    labelPrefix: String,
    labelYear: String,
    isSelected: Boolean,
    metric: ChartMetric,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val barColor = if (isSelected) AppColors.GreenPrimary else AppColors.GreenLight.copy(alpha = 0.55f)
    val barHeight = (140 * ratio).toInt().dp

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Value text trên đầu cột — 1 dòng, không wrap để cột không bị nâng cao bất thường
        Text(
            text = when (metric) {
                ChartMetric.WEIGHT -> DashboardFormatter.weight(value)
                ChartMetric.REVENUE -> DashboardFormatter.money(value)
            },
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextSecondary
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible
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

        // Tên vụ: Tự động xuống dòng tối đa 2 dòng nếu tên vụ dài, căn giữa đều đặn dưới cột
        Text(
            text = labelPrefix,
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (isSelected) AppColors.GreenPrimary else AppColors.TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
            ),
            maxLines = 2,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (labelYear.isNotEmpty()) {
            Text(
                text = labelYear,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isSelected) AppColors.GreenPrimary.copy(alpha = 0.85f) else AppColors.TextHint,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

enum class ChartMetric {
    WEIGHT, REVENUE;

    fun extract(stats: SeasonStats): Double = when (this) {
        WEIGHT -> stats.totalNetWeight
        REVENUE -> stats.totalRevenue
    }
}

/**
 * Tách "Đông Xuân 2026" → ("Đông Xuân", "'26"), "Hè Thu 2025" → ("Hè Thu", "'25").
 * Giữ nguyên tên gốc đầy đủ cho các vụ mùa (không ép viết tắt thành ĐX/HT/TĐ hay cắt cụt các tên vụ tự chọn),
 * chỉ tách riêng phần năm ở cuối nếu có (định dạng năm 2 hoặc 4 chữ số) để render xuống dòng cho gọn đẹp.
 */
private fun seasonLabelParts(season: String): Pair<String, String> {
    val parts = season.trim().split(" ")
    if (parts.size <= 1) {
        return season to ""
    }
    val lastPart = parts.last()
    val isYear = lastPart.all { it.isDigit() } && (lastPart.length == 4 || lastPart.length == 2)
    return if (isYear) {
        val namePart = parts.dropLast(1).joinToString(" ")
        val yearPart = if (lastPart.length == 4) "'${lastPart.takeLast(2)}" else "'$lastPart"
        namePart to yearPart
    } else {
        season to ""
    }
}
