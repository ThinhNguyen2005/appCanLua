package com.GiaThinh.canlua.ui.component.cardlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.GiaThinh.canlua.R
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.repository.SyncStatus
import com.GiaThinh.canlua.ui.component.SyncStatusPulse
import com.GiaThinh.canlua.ui.theme.AppColors
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CardListSummaryCard(
    cardCount: Int,
    totalKg: Double,
    totalAmount: Double,
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier,
    titlePrefix: String = stringResource(R.string.card_list_today_prefix),
    amountLabel: String = stringResource(R.string.card_list_total_income)
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.GreenSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.card_list_today_card_count, titlePrefix, cardCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                    Spacer(Modifier.width(10.dp))
                    SyncStatusPulse(status = syncStatus, showLabel = false)
                }
                Text(
                    text = stringResource(R.string.card_list_weight_kg, numberFormat.format(totalKg)),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
                Text(
                    text = stringResource(R.string.card_list_money_vnd, numberFormat.format(totalAmount)),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.GreenPrimary
                )
            }
        }
    }
}
