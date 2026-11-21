package com.giathinh.canlua.ui.component.market

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.R
import com.giathinh.canlua.data.model.PricePoint
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.util.HapticUtil
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import java.util.Locale

/**
 * Biểu đồ đường (Line Chart) cho lịch sử giá lúa.
 * Vico 2.0 — sử dụng CartesianChartModelProducer.
 */
@Composable
fun PriceTrendChart(
    points: List<PricePoint>,
    modifier: Modifier = Modifier,
    timeRangeDays: Int = 7,
    onTimeRangeChange: (Int) -> Unit
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(points) {
        if (points.isNotEmpty()) {
            modelProducer.runTransaction {
                lineModel {
                    series(points.map { it.priceAvg.toFloat() })
                }
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header với toggle 7d / 30d
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.market_chart_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                TimeRangeToggle(
                    selectedDays = timeRangeDays,
                    onSelect = onTimeRangeChange
                )
            }

            Spacer(Modifier.height(20.dp))

            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.market_chart_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextHint
                    )
                }
            } else {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(),
                        startAxis = VerticalAxis.rememberStart(),
                        bottomAxis = HorizontalAxis.rememberBottom()
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Min / Max stats
            if (points.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val minP = points.minOf { it.priceAvg }
                    val maxP = points.maxOf { it.priceAvg }
                    val avgP = points.map { it.priceAvg }.average()
                    StatChip(stringResource(R.string.market_chart_stat_min), minP, AppColors.Error)
                    StatChip(stringResource(R.string.market_chart_stat_avg), avgP, AppColors.Info)
                    StatChip(stringResource(R.string.market_chart_stat_max), maxP, AppColors.Success)
                }
            }
        }
    }
}

@Composable
private fun TimeRangeToggle(
    selectedDays: Int,
    onSelect: (Int) -> Unit
) {
    val context = LocalContext.current
    val dayOptions = listOf(7, 30)
    SingleChoiceSegmentedButtonRow {
        dayOptions.forEachIndexed { index, days ->
            val selected = days == selectedDays
            SegmentedButton(
                selected = selected,
                onClick = {
                    HapticUtil.tick(context)
                    onSelect(days)
                },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = dayOptions.size
                ),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = AppColors.GreenPrimary,
                    activeContentColor = Color.White,
                    activeBorderColor = AppColors.GreenPrimary,
                    inactiveContainerColor = AppColors.SurfaceContainer,
                    inactiveContentColor = AppColors.TextSecondary,
                    inactiveBorderColor = AppColors.Divider
                ),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(
                    text = stringResource(R.string.market_chart_days_toggle, days),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: Double, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextHint
        )
        Text(
            text = formatPriceShort(value),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

private fun formatPriceShort(price: Double): String {
    val rounded = price.toLong()
    return if (rounded >= 1000) {
        String.format(Locale.US, "%.1fk", rounded / 1000.0)
    } else {
        rounded.toString()
    }
}
