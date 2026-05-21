package com.GiaThinh.canlua.ui.component.cardlist

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.GiaThinh.canlua.R
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.ui.theme.AppColors

@Composable
fun CardListFilterBar(
    selectedSeason: String?,
    selectedVariety: String?,
    onOpenFilter: () -> Unit,
    onClearSeason: () -> Unit,
    onClearVariety: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCount = (if (selectedSeason != null) 1 else 0) +
            (if (selectedVariety != null) 1 else 0)

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = onOpenFilter,
            label = {
                Text(
                    text = if (activeCount > 0) {
                            stringResource(R.string.card_list_filter_count, activeCount)
                        } else {
                            stringResource(R.string.card_list_filter)
                        },
                    fontWeight = FontWeight.SemiBold
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (activeCount > 0)
                    AppColors.GreenPrimary.copy(alpha = 0.12f)
                else
                    AppColors.SurfaceContainer,
                labelColor = if (activeCount > 0) AppColors.GreenPrimary else AppColors.TextSecondary,
                leadingIconContentColor = if (activeCount > 0) AppColors.GreenPrimary else AppColors.TextSecondary
            )
        )

        selectedSeason?.let { season ->
            ActiveFilterChip(label = season, onClear = onClearSeason)
        }
        selectedVariety?.let { variety ->
            ActiveFilterChip(label = variety, onClear = onClearVariety)
        }
    }
}

@Composable
private fun ActiveFilterChip(label: String, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.GreenPrimary)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.CardBg
        )
        IconButton(
            onClick = onClear,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.card_list_clear_named_filter, label),
                tint = AppColors.CardBg,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
