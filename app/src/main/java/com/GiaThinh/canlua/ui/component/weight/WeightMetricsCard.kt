package com.GiaThinh.canlua.ui.component.weight

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.GiaThinh.canlua.ui.theme.AppColors
import java.text.NumberFormat
import java.util.Locale

/**
 * Card 2/3: Chỉ số cân — tổng KG, bì, tạp chất, đơn giá, real-time calc.
 */
@Composable
fun WeightMetricsCard(
    traderName: String,
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
    val fmt = remember { NumberFormat.getNumberInstance(Locale("vi", "VN")) }
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

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(2.dp),
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
                    Text(
                        "$bagCount bao",
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
                    Text(
                        "${"%.1f".format(totalWeight)} kg",
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

            val context = LocalContext.current
            // Trừ bì + Tạp chất + Độ ẩm
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Trừ bì
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = bagText,
                        onValueChange = {
                            if (!isLocked) {
                                bagText = it
                                onBagWeightChange(it.toDoubleOrNull() ?: 0.0)
                            }
                        },
                        label = { Text("Bì (kg/bao)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        enabled = !isLocked,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (isLocked) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    Toast.makeText(context, "Vui lòng mở khóa bảng trước khi chỉnh sửa!", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
                // Tạp chất
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = impText,
                        onValueChange = {
                            if (!isLocked) {
                                impText = it
                                onImpurityWeightChange(it.toDoubleOrNull() ?: 0.0)
                            }
                        },
                        label = { Text("Tạp chất (kg)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        enabled = !isLocked,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (isLocked) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    Toast.makeText(context, "Vui lòng mở khóa bảng trước khi chỉnh sửa!", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
                // Độ ẩm
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = moistureText,
                        onValueChange = {
                            if (!isLocked) {
                                moistureText = it
                                onMoistureChange(it.toDoubleOrNull() ?: 0.0)
                            }
                        },
                        label = { Text("Độ ẩm (%)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        enabled = !isLocked,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (isLocked) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    Toast.makeText(context, "Vui lòng mở khóa bảng trước khi chỉnh sửa!", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
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
                    Text(
                        "${"%.1f".format(netWeight)} kg",
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
                Text(
                    "${fmt.format(totalAmount)} đ",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1B5E20) // Xanh lá đậm tương phản mạnh
                )
            }
        }
    }
}
