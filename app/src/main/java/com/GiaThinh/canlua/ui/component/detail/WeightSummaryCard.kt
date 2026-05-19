package com.GiaThinh.canlua.ui.component.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.ui.component.AnimatedNumber
import com.GiaThinh.canlua.ui.theme.AppColors
import java.text.NumberFormat

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
    netWeight: Double,
    numberFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
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
                    "Khối lượng",
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
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Tổng khối lượng",
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
                        "Chưa trừ bì",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextHint
                    )
                }
            }

            // Stat rows
            FluentStatRow(
                icon = Icons.Outlined.ShoppingBag,
                label = "Số bao",
                trailing = {
                    AnimatedNumber(
                        value = bagCount,
                        formatter = { "$it bao" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                }
            )
            FluentStatRow(
                icon = Icons.Outlined.Inventory2,
                label = "Trừ bì",
                trailing = {
                    AnimatedNumber(
                        value = bagWeight,
                        formatter = { "${numberFormat.format(it)} KG" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                }
            )
            FluentStatRow(
                icon = Icons.Outlined.Scale,
                label = "Tạp chất",
                trailing = {
                    AnimatedNumber(
                        value = impurityWeight,
                        formatter = { "${numberFormat.format(it)} KG" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                }
            )

            HorizontalDivider(color = AppColors.Divider, thickness = 0.6.dp)

            // KL thực (highlight tonal layer 2)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.GreenSurface)
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
                            "Khối lượng thực",
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
    iconTint: Color = AppColors.TextSecondary
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }
        trailing()
    }
}
