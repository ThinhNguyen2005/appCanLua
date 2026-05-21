package com.GiaThinh.canlua.ui.component.detail

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.ui.component.AnimatedNumber
import com.GiaThinh.canlua.ui.theme.AppColors
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
    numberFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
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

            HorizontalDivider(color = AppColors.Divider, thickness = 0.6.dp)

            FluentStatRow(
                icon = Icons.Outlined.AttachMoney,
                label = stringResource(R.string.detail_financial_price),
                trailing = {
                    AnimatedNumber(
                        value = pricePerKg,
                        formatter = { "${numberFormat.format(it)} đ" },
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
                        formatter = { "${numberFormat.format(it)} đ" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.GreenPrimary
                    )
                }
            )
            FluentStatRow(
                icon = Icons.Outlined.CreditCard,
                label = stringResource(R.string.detail_financial_deposit),
                trailing = {
                    AnimatedNumber(
                        value = depositAmount,
                        formatter = { "${numberFormat.format(it)} đ" },
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
                        formatter = { "${numberFormat.format(it)} đ" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Success
                    )
                }
            )

            HorizontalDivider(color = AppColors.Divider, thickness = 0.6.dp)

            // ── Còn lại — highlight tonal layer (style giống "Khối lượng thực", nền vàng) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.GoldLight)
                    .padding(vertical = 12.dp, horizontal = 16.dp)
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
                        formatter = { "${numberFormat.format(it)} đ" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.GoldDark
                    )
                }
            }
        }
    }
}
