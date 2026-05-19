package com.GiaThinh.canlua.ui.component.market

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.data.model.PricePoint
import com.GiaThinh.canlua.ui.theme.AppColors
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

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
                lineSeries {
                    series(points.map { it.priceAvg })
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
                    text = "Xu hướng giá",
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
                        text = "Chưa có dữ liệu lịch sử",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextHint
                    )
                }
            } else {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer()
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
                    StatChip("Min", minP, AppColors.Error)
                    StatChip("TB", avgP, AppColors.Info)
                    StatChip("Max", maxP, AppColors.Success)
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
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.SurfaceContainer)
            .padding(4.dp)
    ) {
        listOf(7, 30).forEach { days ->
            val selected = days == selectedDays
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected) AppColors.GreenPrimary else Color.Transparent
                    )
                    .clickable { onSelect(days) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${days}n",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else AppColors.TextSecondary
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
        "%.1fk".format(rounded / 1000.0)
    } else {
        rounded.toString()
    }
}
