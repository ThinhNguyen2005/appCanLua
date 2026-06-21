package com.giathinh.canlua.ui.component.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.R
import com.giathinh.canlua.data.model.VarietyStat
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.util.DashboardFormatter

/**
 * Pie chart phân bổ giống lúa.
 *
 * Vì Vico 2.0 chỉ hỗ trợ Cartesian (Column/Line) → tự vẽ Canvas.
 * Donut style với gap giữa các slice + legend bên dưới.
 *
 * Màu cho slice lấy luân phiên từ palette gắn liền với "ngũ cốc Việt".
 */
@Composable
fun VarietyPieChart(
    items: List<VarietyStat>,
    modifier: Modifier = Modifier
) {
    val total = remember(items) { items.sumOf { it.weight } }
    val palette = remember { piePalette() }

    // Animation: vẽ từ 0% → 100% khi data về
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(items) {
        sweep.snapTo(0f)
        sweep.animateTo(
            targetValue = 1f,
            animationSpec = tween(700, easing = FastOutSlowInEasing)
        )
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
                text = stringResource(R.string.profile_variety_distribution_title),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
            )

            Spacer(Modifier.height(16.dp))

            if (total == 0.0 || items.isEmpty()) {
                EmptyChartHint(stringResource(R.string.profile_variety_distribution_empty))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut chart
                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(140.dp)) {
                            val strokeWidth = 24.dp.toPx()
                            val arcSize = Size(
                                width = size.width - strokeWidth,
                                height = size.height - strokeWidth
                            )
                            val arcOffset = Offset(strokeWidth / 2, strokeWidth / 2)

                            var startAngle = -90f
                            val totalSweep = 360f * sweep.value

                            items.forEachIndexed { index, item ->
                                val percent = (item.weight / total).toFloat()
                                val rawSweep = percent * totalSweep
                                // Gap nhỏ 2° giữa các slice (chỉ khi có > 1 slice)
                                val gap = if (items.size > 1) 2f else 0f
                                val drawSweep = (rawSweep - gap).coerceAtLeast(0f)

                                drawArc(
                                    color = palette[index % palette.size],
                                    startAngle = startAngle,
                                    sweepAngle = drawSweep,
                                    useCenter = false,
                                    topLeft = arcOffset,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth)
                                )
                                startAngle += rawSweep
                            }
                        }

                        // Center text: tổng tấn
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = DashboardFormatter.weight(total),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AppColors.TextPrimary
                                )
                            )
                            Text(
                                text = stringResource(R.string.profile_total),
                                style = MaterialTheme.typography.labelMedium,
                                color = AppColors.TextHint
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    // Legend
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items.forEachIndexed { index, item ->
                            LegendItem(
                                color = palette[index % palette.size],
                                name = item.variety,
                                weight = item.weight,
                                percent = item.weight / total * 100.0
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, name: String, weight: Double, percent: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )
            )
            Text(
                text = "${DashboardFormatter.weight(weight)} · ${DashboardFormatter.percent(percent)}",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun EmptyChartHint(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextHint
        )
    }
}

/**
 * Palette 6 màu hài hòa cho pie chart.
 * Lấy cảm hứng từ ngũ cốc Việt: vàng nắng / xanh lúa / đỏ gạch / nâu đất / cam mật / tím hồng.
 */
private fun piePalette(): List<Color> = listOf(
    Color(0xFF2E7D32),  // Xanh lúa đậm (primary)
    Color(0xFFF9A825),  // Vàng lúa chín
    Color(0xFFE65100),  // Cam mật
    Color(0xFF6A1B9A),  // Tím hồng (Jasmine)
    Color(0xFF00838F),  // Xanh teal (lúa nước)
    Color(0xFF8D6E63)   // Nâu đất
)
