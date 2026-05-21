package com.GiaThinh.canlua.ui.component.weight

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.GiaThinh.canlua.R
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.ui.component.AnimatedNumber
import com.GiaThinh.canlua.ui.component.ExplainingPopover
import com.GiaThinh.canlua.ui.theme.AppColors
import java.text.NumberFormat
import java.util.Locale

/**
 * Card 2/3: Chỉ số cân — tổng KG, bì, tạp chất, đơn giá, real-time calc.
 */
@Composable
fun WeightMetricsCard(
    totalWeight: Double,
    bagWeight: Double,
    impurityWeight: Double,
    moisturePercent: Double,
    netWeight: Double,
    pricePerKg: Double,
    totalAmount: Double,
    bagCount: Int,
    isLocked: Boolean,
    onBagWeightChange: (Double) -> Unit,
    onImpurityWeightChange: (Double) -> Unit,
    onMoistureChange: (Double) -> Unit,
    onPriceChange: (Double) -> Unit
) {
    val fmt = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }
    var bagText by remember(bagWeight) {
        mutableStateOf(if (bagWeight > 0) bagWeight.toString() else "")
    }
    var impText by remember(impurityWeight) {
        mutableStateOf(if (impurityWeight > 0) impurityWeight.toString() else "")
    }
    var moistureText by remember(moisturePercent) {
        mutableStateOf(if (moisturePercent > 0) moisturePercent.toString() else "")
    }
    var priceText by remember(pricePerKg) {
        mutableStateOf(if (pricePerKg > 0) "%.0f".format(pricePerKg) else "")
    }

    var showBagInfo by remember { mutableStateOf(false) }
    var showImpurityInfo by remember { mutableStateOf(false) }
    var showMoistureInfo by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) AppColors.LockedBg else AppColors.CardBg
        ),
        // Locked state: bỏ elevation để tránh shadow chồng chéo gây ảo giác "shadow quá đà".
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLocked) 0.dp else 2.dp
        ),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Scale, null, tint = AppColors.GreenPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.weight_metrics_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Surface(
                    color = AppColors.GreenSurface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    AnimatedNumber(
                        value = bagCount,
                        formatter = { count -> "$count bao" },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.GreenPrimary
                    )
                }
            }

            // Tổng KG (big display) - High Contrast
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.GoldLight)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.weight_label_total_weight), style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                    AnimatedNumber(
                        value = totalWeight,
                        formatter = { "${"%.1f".format(it)} kg" },
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.RemainingHighlight // Auto-adapt dark/light
                    )
                    Text(
                        stringResource(R.string.weight_label_before_tare),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextHint
                    )
                }
            }

            // Trừ bì + Tạp chất + Độ ẩm — OutlinedTextField đã có `enabled = !isLocked`,
            // không cần overlay Box .clickable rườm rà (gây cảm giác "mờ và shadow quá đà").
            // Khi locked, người dùng vẫn nhìn thấy giá trị nhưng không tap được — clean & predictable.
            //
            // Layout 2 hàng theo mental model nông dân:
            //  - Row 1: Bì + Tạp chất — cặp khấu trừ VẬT LÝ (cùng nhóm)
            //  - Row 2: Độ ẩm — khấu trừ KỸ THUẬT (full width, đủ chỗ hiển thị label)
            // Trước đây 3 trường 1 hàng → label clip "B..." "T..." trên màn nhỏ.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = bagText,
                    onValueChange = {
                        if (!isLocked) {
                            bagText = it
                            onBagWeightChange(it.toDoubleOrNull() ?: 0.0)
                        }
                    },
                    label = { Text(stringResource(R.string.weight_metrics_bag_label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingIcon = {
                        IconButton(
                            onClick = { showBagInfo = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = stringResource(R.string.weight_metrics_bag_info_content),
                                tint = AppColors.GreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    enabled = !isLocked,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = impText,
                    onValueChange = {
                        if (!isLocked) {
                            impText = it
                            onImpurityWeightChange(it.toDoubleOrNull() ?: 0.0)
                        }
                    },
                    label = { Text(stringResource(R.string.weight_metrics_impurity_label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingIcon = {
                        IconButton(
                            onClick = { showImpurityInfo = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = stringResource(R.string.weight_metrics_impurity_info_content),
                                tint = AppColors.GreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    enabled = !isLocked,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Row 2: Độ ẩm — full width vì đây là chỉ số quan trọng nhất (quy đổi tiền)
            // và label "Độ ẩm (%)" + leading icon WaterDrop cần đủ space hiển thị.
            OutlinedTextField(
                value = moistureText,
                onValueChange = {
                    if (!isLocked) {
                        moistureText = it
                        onMoistureChange(it.toDoubleOrNull() ?: 0.0)
                    }
                },
                label = { Text(stringResource(R.string.weight_metrics_moisture_label)) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.WaterDrop,
                        contentDescription = null,
                        tint = AppColors.Info,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { showMoistureInfo = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.weight_metrics_moisture_info_content),
                            tint = AppColors.Info,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                enabled = !isLocked,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // KL thực (after deductions) - High Contrast
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.GreenSurface)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.weight_label_net_weight), style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                    AnimatedNumber(
                        value = netWeight,
                        formatter = { "${"%.1f".format(it)} kg" },
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.GreenPrimary // Auto-adapt: xanh đậm light, xanh sáng dark
                    )
                    Text(
                        stringResource(R.string.weight_metrics_net_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.GreenDark
                    )
                }
            }

            // Thành tiền - High Contrast
            HorizontalDivider(color = AppColors.Divider)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.weight_metrics_total_amount), style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
                AnimatedNumber(
                    value = totalAmount,
                    formatter = { "${fmt.format(it)} đ" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.GreenPrimary // Auto-adapt theo dark/light mode
                )
            }
        }
    }

    // Interactive Explaining Popovers
    ExplainingPopover(
        visible = showBagInfo,
        title = stringResource(R.string.weight_metrics_bag_info_title),
        description = stringResource(R.string.weight_metrics_bag_info_description),
        onDismiss = { showBagInfo = false }
    )

    ExplainingPopover(
        visible = showImpurityInfo,
        title = stringResource(R.string.weight_metrics_impurity_info_title),
        description = stringResource(R.string.weight_metrics_impurity_info_description),
        onDismiss = { showImpurityInfo = false }
    )

    ExplainingPopover(
        visible = showMoistureInfo,
        title = stringResource(R.string.weight_metrics_moisture_info_title),
        description = stringResource(R.string.weight_metrics_moisture_info_description),
        onDismiss = { showMoistureInfo = false }
    )
}

