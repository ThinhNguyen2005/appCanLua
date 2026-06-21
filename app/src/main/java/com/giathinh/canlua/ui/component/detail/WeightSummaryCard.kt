package com.giathinh.canlua.ui.component.detail

import com.giathinh.canlua.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.ui.component.AnimatedNumber
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.util.RiceCalculator
import java.text.NumberFormat
import java.util.Locale

/**
 * Card 1/3 (DetailScreen): Khối lượng.
 * Fluent depth: Surface CardBg + 2dp elevation, KL thực highlight bằng GreenSurface.
 */
@Composable
fun WeightSummaryCard(
    totalWeight: Double,
    bagCount: Int,
    bagWeight: Double,
    impurityWeight: Double,
    moisturePercent: Double,
    netWeight: Double,
    numberFormat: NumberFormat,
    isLocked: Boolean = false,
    impurityIsPercent: Boolean = false,
    bagMethodIsSampling: Boolean = false,
    bagSampleCount: Int = 0,
    bagSampleTotalWeight: Double = 0.0,
    modifier: Modifier = Modifier
) {
    val totalBagWeight = RiceCalculator.calcTotalBagWeight(
        bagCount = bagCount,
        bagWeight = bagWeight,
        methodIsSampling = bagMethodIsSampling,
        sampleCount = bagSampleCount,
        sampleTotalWeight = bagSampleTotalWeight
    )
    val impurityKg = impurityWeight.coerceAtLeast(0.0)
    val bagRatioText = if (bagMethodIsSampling) {
        val count = bagSampleCount.takeIf { it > 0 } ?: 8
        val kg = formatCompactKg(bagSampleTotalWeight.takeIf { it > 0.0 } ?: 1.0)
        "$bagCount bao · $count bao = $kg kg"
    } else {
        val count = bagSampleCount.takeIf { it > 0 } ?: bagWeight.takeIf { it > 0.0 }?.let { (1.0 / it).toInt() } ?: 8
        "$bagCount bao · $count bao = 1 kg"
    }
    val impurityNote = "Nhập trực tiếp kg tạp chất"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) AppColors.LockedSurface else AppColors.CardBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Scale,
                    contentDescription = null,
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.weight_label_weight),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }

            // Tổng KG (highlight tonal layer)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.WeightSurface)
                    .border(1.dp, AppColors.DividerStrong, RoundedCornerShape(12.dp))
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.weight_label_total_weight),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    AnimatedNumber(
                        value = totalWeight,
                        formatter = { "${numberFormat.format(it)} KG" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.RemainingHighlight
                    )
                    Text(
                        stringResource(R.string.weight_label_before_tare),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextHint
                    )
                }
            }

            // Stat rows
            FluentStatRow(
                icon = Icons.Outlined.ShoppingBag,
                label = stringResource(R.string.weight_label_bag_count),
                trailing = {
                    AnimatedNumber(
                        value = bagCount,
                        formatter = { count -> "$count bao" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                }
            )
            FluentStatRow(
                icon = Icons.Outlined.Inventory2,
                label = stringResource(R.string.weight_label_tare),
                supportingText = bagRatioText,
                trailing = {
                    AnimatedNumber(
                        value = totalBagWeight,
                        formatter = { "${numberFormat.format(it)} KG" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                }
            )
            FluentStatRow(
                icon = Icons.Outlined.Scale,
                label = stringResource(R.string.weight_label_impurity),
                supportingText = impurityNote,
                trailing = {
                    AnimatedNumber(
                        value = impurityKg,
                        formatter = { "${numberFormat.format(it)} KG" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                }
            )
            FluentStatRow(
                icon = Icons.Outlined.WaterDrop,
                label = stringResource(R.string.weight_metrics_moisture_label),
                trailing = {
                    AnimatedNumber(
                        value = moisturePercent,
                        formatter = { "${"%.1f".format(it)} %" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Info
                    )
                },
                iconTint = AppColors.Info
            )

            HorizontalDivider(color = AppColors.DividerStrong, thickness = 1.dp)

            // KL thực (highlight tonal layer 2)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.GreenSurface)
                    .border(1.dp, AppColors.GreenPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Scale,
                            contentDescription = null,
                            tint = AppColors.GreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.weight_label_net_weight),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.GreenPrimary
                        )
                    }
                    AnimatedNumber(
                        value = netWeight,
                        formatter = { "${numberFormat.format(it)} KG" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.GreenPrimary
                    )
                }
            }
        }
    }
}

@Composable
internal fun FluentStatRow(
    icon: ImageVector,
    label: String,
    trailing: @Composable () -> Unit,
    iconTint: Color = AppColors.TextSecondary,
    supportingText: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
                supportingText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextHint
                    )
                }
            }
        }
        trailing()
    }
}

private fun formatCompactKg(value: Double): String {
    val rounded = Math.round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", rounded).replace('.', ',')
    }
}
