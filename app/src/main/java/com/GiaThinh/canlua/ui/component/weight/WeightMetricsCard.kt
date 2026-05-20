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
import androidx.compose.ui.text.font.FontWeight
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
                Text("Chỉ Số Cân", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Surface(
                    color = AppColors.GreenSurface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    AnimatedNumber(
                        value = bagCount,
                        formatter = { "$it bao" },
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
                    Text("Tổng khối lượng", style = MaterialTheme.typography.bodyMedium, color = Color.Black, fontWeight = FontWeight.Bold)
                    AnimatedNumber(
                        value = totalWeight,
                        formatter = { "${"%.1f".format(it)} kg" },
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFB71C1C) // Đỏ đậm cực kỳ nổi bật
                    )
                    Text(
                        "Chưa trừ bì",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextHint
                    )
                }
            }

            // Trừ bì + Tạp chất + Độ ẩm — OutlinedTextField đã có `enabled = !isLocked`,
            // không cần overlay Box .clickable rườm rà (gây cảm giác "mờ và shadow quá đà").
            // Khi locked, người dùng vẫn nhìn thấy giá trị nhưng không tap được — clean & predictable.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = bagText,
                    onValueChange = {
                        if (!isLocked) {
                            bagText = it
                            onBagWeightChange(it.toDoubleOrNull() ?: 0.0)
                        }
                    },
                    label = { Text("Bì (kg/bao)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingIcon = {
                        IconButton(
                            onClick = { showBagInfo = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = "Tìm hiểu trọng lượng bì",
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
                    label = { Text("Tạp chất (kg)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingIcon = {
                        IconButton(
                            onClick = { showImpurityInfo = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = "Tìm hiểu tạp chất",
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
                    value = moistureText,
                    onValueChange = {
                        if (!isLocked) {
                            moistureText = it
                            onMoistureChange(it.toDoubleOrNull() ?: 0.0)
                        }
                    },
                    label = { Text("Độ ẩm (%)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingIcon = {
                        IconButton(
                            onClick = { showMoistureInfo = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Outlined.WaterDrop,
                                contentDescription = "Tìm hiểu khấu trừ độ ẩm",
                                tint = AppColors.Info,
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
                    Text("Khối lượng thực", style = MaterialTheme.typography.bodyMedium, color = Color.Black, fontWeight = FontWeight.Bold)
                    AnimatedNumber(
                        value = netWeight,
                        formatter = { "${"%.1f".format(it)} kg" },
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1B5E20) // Xanh lá đậm tương phản cao
                    )
                    Text(
                        "Khối lượng thực tế",
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
                Text("Thành tiền", style = MaterialTheme.typography.titleMedium, color = Color.Black, fontWeight = FontWeight.Bold)
                AnimatedNumber(
                    value = totalAmount,
                    formatter = { "${fmt.format(it)} đ" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1B5E20) // Xanh lá đậm tương phản mạnh
                )
            }
        }
    }

    // Interactive Explaining Popovers
    ExplainingPopover(
        visible = showBagInfo,
        title = "⚖️ Trọng Lượng Bì",
        description = "Trọng lượng trung bình của vỏ bao đựng lúa (thường khoảng 0.5kg - 1.2kg tùy loại bao). Tổng trọng lượng bì (Bì × Số bao) sẽ được khấu trừ trực tiếp khỏi tổng khối lượng lúa.",
        onDismiss = { showBagInfo = false }
    )

    ExplainingPopover(
        visible = showImpurityInfo,
        title = "🍂 Khấu Trừ Tạp Chất",
        description = "Khối lượng tạp chất (rơm rạ, đất cát, hạt lép) có trong lô lúa được hai bên thỏa thuận trừ trực tiếp bằng số kg cố định để đảm bảo công bằng cho người mua lúa sạch.",
        onDismiss = { showImpurityInfo = false }
    )

    ExplainingPopover(
        visible = showMoistureInfo,
        title = "💧 Khấu Trừ Độ Ẩm",
        description = "Độ ẩm lúa thực tế đo tại ruộng. Độ ẩm tiêu chuẩn thương mại là 14%. Nếu độ ẩm cao hơn, một tỷ lệ hao hụt sấy sẽ được khấu trừ vào khối lượng thực tế theo thỏa thuận.",
        onDismiss = { showMoistureInfo = false }
    )
}

