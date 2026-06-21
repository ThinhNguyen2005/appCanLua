package com.giathinh.canlua.ui.component.cardlist

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.giathinh.canlua.R
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.ui.theme.AppColors

@Composable
fun PremiumQuotaDialog(
    cardsToday: Int,
    freeLimit: Int,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = AppColors.GoldDark,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.premium_quota_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Text(
                text = stringResource(R.string.premium_quota_message, freeLimit, cardsToday),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary
            )
        },
        confirmButton = {
            TextButton(
                onClick = onUpgrade,
                colors = ButtonDefaults.textButtonColors(contentColor = AppColors.GoldDark)
            ) {
                Text(stringResource(R.string.premium_upgrade), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_later), color = AppColors.TextSecondary)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = AppColors.Surface
    )
}
