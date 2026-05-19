package com.GiaThinh.canlua.ui.component.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.ui.theme.AppColors
import java.text.NumberFormat
import java.util.Locale

/**
 * Card 3/3: Kết quả tài chính — cọc, đã trả, còn lại, switch thanh toán đủ.
 */
@Composable
fun PaymentResultCard(
    totalAmount: Double,
    depositAmount: Double,
    paidAmount: Double,
    remainingAmount: Double,
    isLocked: Boolean,
    onDepositChange: (Double) -> Unit,
    onPaidChange: (Double) -> Unit,
    onPaidFullToggle: (Boolean) -> Unit
) {
    val fmt = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }
    val isPaidFull = remainingAmount <= 0.0 && totalAmount > 0

    var depositText by remember(depositAmount) {
        mutableStateOf(if (depositAmount > 0) "%.0f".format(depositAmount) else "")
    }
    var paidText by remember(paidAmount) {
        mutableStateOf(if (paidAmount > 0) "%.0f".format(paidAmount) else "")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Payments, null, tint = AppColors.GoldDark, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Thanh Toán", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // Tiền cọc + Đã trả
            OutlinedTextField(
                value = depositText,
                onValueChange = {
                    if (!isLocked) {
                        depositText = it
                        onDepositChange(it.toDoubleOrNull() ?: 0.0)
                    }
                },
                label = { Text("Tiền đặt cọc") },
                suffix = { Text("đ") },
                enabled = !isLocked,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = paidText,
                onValueChange = {
                    if (!isLocked) {
                        paidText = it
                        onPaidChange(it.toDoubleOrNull() ?: 0.0)
                    }
                },
                label = { Text("Tiền đã trả") },
                suffix = { Text("đ") },
                enabled = !isLocked,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Còn lại (big highlight) - High Contrast
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isPaidFull) AppColors.GreenSurface
                        else AppColors.GoldLight
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (isPaidFull) "ĐÃ THANH TOÁN ĐỦ ✓" else "TIỀN CÒN LẠI",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaidFull) Color(0xFF1B5E20) else Color.Black
                    )
                    if (!isPaidFull) {
                        Text(
                            "${fmt.format(remainingAmount)} đ",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD32F2F) // Đỏ tươi tương phản cao trên nền vàng nhạt
                        )
                    }
                    Text(
                        "= ${fmt.format(totalAmount)} - ${fmt.format(depositAmount)} - ${fmt.format(paidAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Switch thanh toán đủ
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Đã trả đủ tiền", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Switch(
                    checked = isPaidFull,
                    onCheckedChange = { if (!isLocked) onPaidFullToggle(it) },
                    enabled = !isLocked,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AppColors.CardBg,
                        checkedTrackColor = AppColors.GreenPrimary
                    )
                )
            }
        }
    }
}
