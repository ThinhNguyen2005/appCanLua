package com.GiaThinh.canlua.ui.component.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.ui.component.AnimatedNumber
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.util.MoneyFormatter
import java.text.NumberFormat

/**
 * Card 2/3 (DetailScreen): Tài chính.
 * Đơn giá, Thành tiền, Tiền cọc, Đã trả + Còn lại (highlight vàng).
 */
@Composable
fun FinancialSummaryCard(
    pricePerKg: Double,
    totalAmount: Double,
    depositAmount: Double,
    paidAmount: Double,
    remainingAmount: Double,
    isPaid: Boolean,
    onPaidChange: (Boolean) -> Unit,
    isLocked: Boolean,
    numberFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    // Locked đè Paid: phiếu khoá → hồng (cuối cùng); đã trả đủ chưa khoá → xanh nhạt.
    val containerColor = when {
        isLocked -> AppColors.LockedSurface
        isPaid -> AppColors.PaidSurface
        else -> AppColors.CardBg
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.MonetizationOn,
                    contentDescription = null,
                    tint = AppColors.GoldAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.detail_financial_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }

            HorizontalDivider(color = AppColors.DividerStrong, thickness = 1.dp)

            FluentStatRow(
                icon = Icons.Outlined.AttachMoney,
                label = stringResource(R.string.detail_financial_price),
                trailing = {
                    AnimatedNumber(
                        value = pricePerKg,
                        formatter = { MoneyFormatter.formatVndShort(it) },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                }
            )
            FluentStatRow(
                icon = Icons.Outlined.Calculate,
                label = stringResource(R.string.detail_financial_total_amount),
                trailing = {
                    AnimatedNumber(
                        value = totalAmount,
                        formatter = { MoneyFormatter.formatVndShort(it) },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.GreenPrimary
                    )
                }
            )
            // Dòng đọc tiếng Việt cho "Thành tiền" — căn phải, ngay dưới số tiền
            val context = androidx.compose.ui.platform.LocalContext.current
            MoneyFormatter.toWords(totalAmount, context).takeIf { it.isNotEmpty() }?.let { words ->
                Text(
                    text = words,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            FluentStatRow(
                icon = Icons.Outlined.CreditCard,
                label = stringResource(R.string.detail_financial_deposit),
                trailing = {
                    AnimatedNumber(
                        value = depositAmount,
                        formatter = { MoneyFormatter.formatVndShort(it) },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                }
            )
            FluentStatRow(
                icon = Icons.Outlined.CheckCircle,
                label = stringResource(R.string.detail_financial_paid),
                trailing = {
                    AnimatedNumber(
                        value = paidAmount,
                        formatter = { MoneyFormatter.formatVndShort(it) },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Success
                    )
                }
            )

            HorizontalDivider(color = AppColors.DividerStrong, thickness = 1.dp)

            // ── Còn lại — highlight tonal layer (style giống "Khối lượng thực", nền vàng) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.GoldLight)
                    .border(1.dp, AppColors.GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint = AppColors.GoldDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.detail_financial_remaining),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.GoldDark
                        )
                    }
                    AnimatedNumber(
                        value = remainingAmount,
                        formatter = { MoneyFormatter.formatVndShort(it) },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.GoldDark
                    )
                }
                MoneyFormatter.toWords(remainingAmount, context).takeIf { it.isNotEmpty() }?.let { words ->
                    Text(
                        text = words,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.GoldDark,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HorizontalDivider(color = AppColors.DividerStrong, thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = if (isPaid) AppColors.Success else AppColors.TextHint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.weight_payment_paid_full_toggle),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }
                Switch(
                    checked = isPaid,
                    onCheckedChange = {
                        if (!isLocked) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPaidChange(it)
                        }
                    },
                    enabled = !isLocked,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AppColors.GreenPrimary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}
