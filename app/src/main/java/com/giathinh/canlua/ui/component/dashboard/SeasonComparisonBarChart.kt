package com.giathinh.canlua.ui.component.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.giathinh.canlua.R
import com.giathinh.canlua.data.model.SeasonStats
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.util.DashboardFormatter
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries

private val VI_LOCALE_BAR = java.util.Locale.forLanguageTag("vi-VN")
private val MONTH_FMT = java.text.SimpleDateFormat("MM", VI_LOCALE_BAR)
private val YEAR_FMT = java.text.SimpleDateFormat("yyyy", VI_LOCALE_BAR)

enum class ChartType { BAR, PIE }

@Composable
private fun ChartTypeTab(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) AppColors.GreenPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else AppColors.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private val SeasonPiePalette = listOf(Color(0xFF2E7D32), Color(0xFFF9A825), Color(0xFFE65100), Color(0xFF1976D2))

@Composable
private fun SeasonPieChart(items: List<SeasonStats>, metric: ChartMetric) {
    val total = remember(items, metric) { items.sumOf { metric.extract(it) } }
    val modelProducer = remember { PieChartModelProducer() }
    val values = remember(items, metric) { items.map { metric.extract(it) } }
    androidx.compose.runtime.LaunchedEffect(values) {
        modelProducer.runTransaction { pieSeries { series(values) } }
    }
    val chart = rememberPieChart(
        sliceProvider = PieChart.SliceProvider.series(
            SeasonPiePalette.map { PieChart.Slice(fill = Fill(it)) }
        ),
        spacing = 2.dp,
        innerSize = PieSize.Inner.fixed(42.dp)
    )
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
            PieChartHost(chart, modelProducer, Modifier.size(130.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (metric == ChartMetric.WEIGHT) DashboardFormatter.weight(total) else DashboardFormatter.money(total),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, color = AppColors.TextPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(stringResource(R.string.profile_total), style = MaterialTheme.typography.labelSmall, color = AppColors.TextHint)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEachIndexed { index, item ->
                val value = metric.extract(item)
                SeasonLegendItem(SeasonPiePalette[index % SeasonPiePalette.size], item.season, value, if (total > 0) value / total * 100 else 0.0, metric)
            }
        }
    }
}

@Composable
private fun SeasonLegendItem(color: Color, name: String, value: Double, percent: Double, metric: ChartMetric) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = AppColors.TextPrimary))
            val valueText = if (metric == ChartMetric.WEIGHT) DashboardFormatter.weight(value) else DashboardFormatter.money(value)
            Text("$valueText · ${DashboardFormatter.percent(percent)}", style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
        }
    }
}

@Composable
private fun SeasonBarChart(items: List<SeasonStats>, selectedSeason: String?, metric: ChartMetric) {
    val context = LocalContext.current
    val modelProducer = remember { CartesianChartModelProducer() }
    val values = remember(items, metric) { items.map { metric.extract(it) } }
    androidx.compose.runtime.LaunchedEffect(values) {
        modelProducer.runTransaction { columnModel { series(values) } }
    }
    val layer = rememberColumnCartesianLayer(
        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
            rememberLineComponent(Fill(AppColors.GreenPrimary), 24.dp)
        )
    )
    val chart = rememberCartesianChart(
        layer,
        startAxis = VerticalAxis.rememberStart(),
        bottomAxis = HorizontalAxis.rememberBottom()
    )
    Column(Modifier.fillMaxWidth()) {
        CartesianChartHost(chart, modelProducer, Modifier.fillMaxWidth().height(150.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            items.forEach { season ->
                val (prefix, year) = seasonLabel(season)
                Text(
                    text = if (year.isEmpty()) prefix else "$prefix\n$year",
                    modifier = Modifier.weight(1f).clickable { Toast.makeText(context, season.season, Toast.LENGTH_SHORT).show() },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (season.season == selectedSeason) AppColors.GreenPrimary else AppColors.TextSecondary,
                        fontWeight = if (season.season == selectedSeason) FontWeight.Bold else FontWeight.Medium
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SeasonComparisonBarChart(
    seasons: List<SeasonStats>,
    selectedSeason: String?,
    modifier: Modifier = Modifier,
    metric: ChartMetric = ChartMetric.WEIGHT
) {
    val displayItems = remember(seasons) { seasons.take(4).reversed() }
    val maxValue = remember(displayItems, metric) { displayItems.maxOfOrNull { metric.extract(it) } ?: 0.0 }
    var chartType by remember { mutableStateOf(ChartType.BAR) }
    Card(modifier = modifier, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = AppColors.CardBg), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (metric == ChartMetric.WEIGHT) R.string.profile_season_comparison_weight else R.string.profile_season_comparison_revenue), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary))
                Row(Modifier.clip(RoundedCornerShape(8.dp)).background(AppColors.Surface).padding(2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ChartTypeTab("Cột", chartType == ChartType.BAR) { chartType = ChartType.BAR }
                    ChartTypeTab("Tròn", chartType == ChartType.PIE) { chartType = ChartType.PIE }
                }
            }
            Spacer(Modifier.height(20.dp))
            if (displayItems.isEmpty() || maxValue == 0.0) {
                Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.profile_season_comparison_empty), style = MaterialTheme.typography.bodyMedium, color = AppColors.TextHint)
                }
            } else if (chartType == ChartType.BAR) {
                SeasonBarChart(displayItems, selectedSeason, metric)
            } else {
                SeasonPieChart(displayItems, metric)
            }
        }
    }
}

private fun seasonLabel(season: SeasonStats): Pair<String, String> = if (season.lastDate > 0L) {
    "Tháng ${MONTH_FMT.format(java.util.Date(season.lastDate))}" to YEAR_FMT.format(java.util.Date(season.lastDate))
} else seasonLabelParts(season.season)

enum class ChartMetric { WEIGHT, REVENUE; fun extract(stats: SeasonStats): Double = if (this == WEIGHT) stats.totalNetWeight else stats.totalRevenue }

private fun seasonLabelParts(season: String): Pair<String, String> {
    val parts = season.trim().split(" ")
    if (parts.size <= 1) return season to ""
    val last = parts.last()
    if (!last.all(Char::isDigit) || (last.length != 2 && last.length != 4)) return season to ""
    return parts.dropLast(1).joinToString(" ") to "'${last.takeLast(2)}"
}