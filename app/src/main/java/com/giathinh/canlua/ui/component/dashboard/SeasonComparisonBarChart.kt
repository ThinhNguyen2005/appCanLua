package com.giathinh.canlua.ui.component.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giathinh.canlua.R
import com.giathinh.canlua.data.model.SeasonStats
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.util.DashboardFormatter
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import java.util.Locale
import kotlin.math.abs

/**
 * So sánh sản lượng (tấn) hoặc doanh thu (triệu đ) giữa các vụ mùa — Vico Column Cartesian Chart.
 *
 * Nguyên tắc UX:
 *  - Đọc câu kết luận trước (Summary headline: Giá trị vụ hiện tại + Delta so với vụ trước).
 *  - Nhìn biểu đồ để xác nhận xu hướng (Cột vụ đang chọn tô màu [AppColors.GreenPrimary], các vụ khác nhạt hơn).
 *  - Chạm để xem chi tiết (Vico [CartesianMarker] với chế độ toggle-on-tap hiển thị popover thông tin).
 *
 * @param seasons danh sách thống kê các vụ mùa
 * @param selectedSeason tên vụ mùa đang được chọn để highlight
 * @param metric tiêu chí hiển thị ([ChartMetric.WEIGHT] hoặc [ChartMetric.REVENUE])
 */
@Composable
fun SeasonComparisonBarChart(
    seasons: List<SeasonStats>,
    selectedSeason: String?,
    metric: ChartMetric = ChartMetric.WEIGHT,
    modifier: Modifier = Modifier
) {
    val displayItems = remember(seasons) { seasons.take(6).reversed() } // cũ nhất → mới nhất (trái sang phải)
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
            // Header tiêu đề biểu đồ
            Text(
                text = stringResource(
                    when (metric) {
                        ChartMetric.WEIGHT -> R.string.profile_season_comparison_weight
                        ChartMetric.REVENUE -> R.string.profile_season_comparison_revenue
                    }
                ),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextSecondary
                )
            )

            Spacer(Modifier.height(8.dp))

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
                // Tóm tắt kết luận trước: Giá trị vụ đang xem + Delta so với vụ liền trước
                val activeSeason = remember(displayItems, selectedSeason) {
                    displayItems.find { it.season == selectedSeason } ?: displayItems.lastOrNull()
                }
                val activeIndex = remember(displayItems, activeSeason) {
                    displayItems.indexOf(activeSeason)
                }
                val previousSeason = remember(displayItems, activeIndex) {
                    if (activeIndex > 0) displayItems[activeIndex - 1] else null
                }

                if (activeSeason != null) {
                    val activeValue = metric.extract(activeSeason)
                    val activeFormatted = when (metric) {
                        ChartMetric.WEIGHT -> DashboardFormatter.weight(activeValue)
                        ChartMetric.REVENUE -> DashboardFormatter.money(activeValue)
                    }
                    val deltaPercent = previousSeason?.let {
                        val prevVal = metric.extract(it)
                        DashboardFormatter.deltaPercent(activeValue, prevVal)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = activeFormatted,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AppColors.TextPrimary
                                )
                            )
                            if (previousSeason != null) {
                                val prevVal = metric.extract(previousSeason)
                                val diff = activeValue - prevVal
                                val formattedDiff = when (metric) {
                                    ChartMetric.WEIGHT -> DashboardFormatter.weight(abs(diff))
                                    ChartMetric.REVENUE -> DashboardFormatter.money(abs(diff))
                                }
                                val isPositive = diff >= 0
                                val arrow = if (isPositive) "↑" else "↓"
                                val deltaPctStr = deltaPercent?.let { " (${DashboardFormatter.formatDelta(it)})" } ?: ""
                                val toneColor = if (isPositive) AppColors.Success else AppColors.Error
                                Text(
                                    text = "$arrow $formattedDiff$deltaPctStr so với vụ trước",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = toneColor
                                    )
                                )
                            }
                        }
                        Text(
                            text = activeSeason.season,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = AppColors.TextSecondary
                            ),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }

                // Vico Column Chart Component
                VicoSeasonBarChart(
                    displayItems = displayItems,
                    selectedSeason = selectedSeason,
                    metric = metric
                )
            }
        }
    }
}

@Composable
private fun VicoSeasonBarChart(
    displayItems: List<SeasonStats>,
    selectedSeason: String?,
    metric: ChartMetric
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(displayItems, metric) {
        if (displayItems.isNotEmpty()) {
            modelProducer.runTransaction {
                columnModel {
                    series(displayItems.map { metric.extract(it) })
                }
            }
        }
    }

    val selectedIndex = remember(displayItems, selectedSeason) {
        displayItems.indexOfFirst { it.season == selectedSeason }
    }

    val selectedColumn = rememberLineComponent(
        fill = Fill(AppColors.GreenPrimary),
        thickness = 22.dp,
        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
    )

    val unselectedColumn = rememberLineComponent(
        fill = Fill(AppColors.GreenLight.copy(alpha = 0.55f)),
        thickness = 22.dp,
        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
    )

    val columnProvider = remember(selectedIndex, selectedColumn, unselectedColumn) {
        object : ColumnCartesianLayer.ColumnProvider {
            override fun getColumn(
                entry: ColumnCartesianLayerModel.Entry,
                extraStore: ExtraStore
            ): LineComponent {
                return if (selectedIndex >= 0 && entry.x.toInt() == selectedIndex) {
                    selectedColumn
                } else {
                    unselectedColumn
                }
            }

            override fun getWidestSeriesColumn(
                seriesKey: Any,
                seriesIndex: Int,
                extraStore: ExtraStore
            ): LineComponent = selectedColumn
        }
    }

    val displayLabels = remember(displayItems) {
        displayItems.map { formatSeasonShort(it.season) }
    }

    val bottomAxisFormatter = remember(displayLabels) {
        CartesianValueFormatter { _, x, _ ->
            displayLabels.getOrElse(x.toInt()) { "" }
        }
    }

    val startAxisFormatter = remember(metric) {
        CartesianValueFormatter { _, y, _ ->
            when (metric) {
                ChartMetric.WEIGHT -> {
                    if (y >= 1000.0) "%.0ft".format(Locale.US, y / 1000.0) else "%.0fkg".format(Locale.US, y)
                }
                ChartMetric.REVENUE -> {
                    if (y >= 1_000_000.0) "%.0ftr".format(Locale.US, y / 1_000_000.0) else "%.0fk".format(Locale.US, y / 1000.0)
                }
            }
        }
    }

    val markerValueFormatter = remember(displayItems, metric) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val colTarget = targets.firstOrNull() as? ColumnCartesianLayerMarkerTarget
            val entry = colTarget?.columns?.firstOrNull()?.entry
            val index = entry?.x?.toInt() ?: -1
            if (index in displayItems.indices) {
                val season = displayItems[index]
                val value = metric.extract(season)
                val formattedVal = when (metric) {
                    ChartMetric.WEIGHT -> DashboardFormatter.weight(value)
                    ChartMetric.REVENUE -> DashboardFormatter.money(value)
                }
                val prevSeason = if (index > 0) displayItems[index - 1] else null
                val deltaLine = if (prevSeason != null) {
                    val prevVal = metric.extract(prevSeason)
                    val diff = value - prevVal
                    val pct = DashboardFormatter.deltaPercent(value, prevVal)
                    val formattedDiff = when (metric) {
                        ChartMetric.WEIGHT -> DashboardFormatter.weight(abs(diff))
                        ChartMetric.REVENUE -> DashboardFormatter.money(abs(diff))
                    }
                    val arrow = if (diff >= 0) "↑" else "↓"
                    val pctStr = pct?.let { " (${DashboardFormatter.formatDelta(it)})" } ?: ""
                    "$arrow $formattedDiff$pctStr so với ${formatSeasonShort(prevSeason.season)}"
                } else {
                    null
                }

                buildString {
                    append(season.season)
                    append("\n")
                    append(formattedVal)
                    if (deltaLine != null) {
                        append("\n")
                        append(deltaLine)
                    }
                }
            } else {
                ""
            }
        }
    }

    val marker = rememberDefaultCartesianMarker(
        label = rememberTextComponent(
            style = TextStyle(
                color = AppColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            padding = Insets(horizontal = 10.dp, vertical = 6.dp),
            background = rememberShapeComponent(
                fill = Fill(AppColors.CardBg),
                shape = RoundedCornerShape(8.dp),
                strokeFill = Fill(AppColors.GreenPrimary.copy(alpha = 0.5f)),
                strokeThickness = 1.dp
            ),
            lineCount = 4
        ),
        valueFormatter = markerValueFormatter,
        labelPosition = DefaultCartesianMarker.LabelPosition.Top,
        guideline = null,
        indicator = null
    )

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = columnProvider,
                columnCollectionSpacing = 16.dp
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = startAxisFormatter,
                guideline = rememberLineComponent(
                    fill = Fill(AppColors.Divider.copy(alpha = 0.5f)),
                    thickness = 1.dp
                )
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = bottomAxisFormatter,
                guideline = null
            ),
            marker = marker,
            markerController = CartesianMarkerController.rememberToggleOnTap()
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    )
}

enum class ChartMetric {
    WEIGHT, REVENUE;

    fun extract(stats: SeasonStats): Double = when (this) {
        WEIGHT -> stats.totalNetWeight
        REVENUE -> stats.totalRevenue
    }
}

/**
 * Rút gọn tên mùa vụ cho nhãn trục hoành (X-axis):
 *  - "Đông Xuân 2026" → "ĐX '26"
 *  - "Hè Thu 2025" → "HT '25"
 *  - "Thu Đông 2024" → "TĐ '24"
 *  - Fallback an toàn cho tên vụ tự chọn: lấy chữ cái đầu các từ + năm rút gọn.
 */
internal fun formatSeasonShort(season: String): String {
    val trimmed = season.trim()
    if (trimmed.isEmpty()) return ""

    val parts = trimmed.split(Regex("\\s+"))
    val lastPart = parts.lastOrNull() ?: ""
    val hasYear = lastPart.all { it.isDigit() } && (lastPart.length == 4 || lastPart.length == 2)
    val yearSuffix = if (hasYear) {
        if (lastPart.length == 4) "'${lastPart.takeLast(2)}" else "'$lastPart"
    } else {
        ""
    }

    val nameParts = if (hasYear) parts.dropLast(1) else parts
    val nameText = nameParts.joinToString(" ")
    val lower = nameText.lowercase(Locale.forLanguageTag("vi-VN"))

    val prefix = when {
        lower.contains("đông xuân") -> "ĐX"
        lower.contains("hè thu") -> "HT"
        lower.contains("thu đông") -> "TĐ"
        lower.contains("vụ 3") || lower.contains("vu 3") -> "V3"
        nameParts.size >= 2 -> nameParts.mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        else -> nameText.take(4)
    }

    return if (yearSuffix.isNotEmpty()) "$prefix $yearSuffix" else prefix
}
