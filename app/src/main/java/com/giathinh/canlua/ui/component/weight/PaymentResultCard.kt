package com.giathinh.canlua.ui.component.weight

import com.giathinh.canlua.R

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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.theme.lockedAwareTextFieldColors
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
    val haptic = LocalHapticFeedback.current
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
                Text(stringResource(R.string.weight_payment_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                label = { Text(stringResource(R.string.weight_payment_deposit)) },
                suffix = { Text(stringResource(R.string.currency_vnd_symbol)) },
                enabled = !isLocked,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = lockedAwareTextFieldColors()
            )

            OutlinedTextField(
                value = paidText,
                onValueChange = {
                    if (!isLocked) {
                        paidText = it
                        onPaidChange(it.toDoubleOrNull() ?: 0.0)
                    }
                },
                label = { Text(stringResource(R.string.weight_payment_paid)) },
                suffix = { Text(stringResource(R.string.currency_vnd_symbol)) },
                enabled = !isLocked,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = lockedAwareTextFieldColors()
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
                        if (isPaidFull) {
                            stringResource(R.string.weight_payment_paid_full)
                        } else {
                            stringResource(R.string.weight_payment_remaining)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaidFull) AppColors.GreenPrimary else AppColors.TextPrimary
                    )
                    if (!isPaidFull) {
                        Text(
                            stringResource(R.string.card_list_money_vnd, fmt.format(remainingAmount)),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppColors.RemainingHighlight // Auto-adapt dark/light
                        )
                    }
                    Text(
                        stringResource(
                            R.string.weight_payment_formula,
                            fmt.format(totalAmount),
                            fmt.format(depositAmount),
                            fmt.format(paidAmount)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextPrimary,
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
                Text(stringResource(R.string.weight_payment_paid_full_toggle), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Switch(
                    checked = isPaidFull,
                    onCheckedChange = {
                        if (!isLocked) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPaidFullToggle(it)
                        }
                    },
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
