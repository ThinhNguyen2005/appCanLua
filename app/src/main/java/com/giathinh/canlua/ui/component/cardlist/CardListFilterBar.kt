package com.giathinh.canlua.ui.component.cardlist

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.R
import com.giathinh.canlua.ui.theme.AppColors

@Composable
fun CardListFilterBar(
    selectedSeason: String?,
    selectedVariety: String?,
    onOpenFilter: () -> Unit,
    onClearSeason: () -> Unit,
    onClearVariety: () -> Unit,
    modifier: Modifier = Modifier,
    onClearAll: (() -> Unit)? = null
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
            modifier = Modifier.heightIn(min = 48.dp),
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
                    Icons.Outlined.FilterList,
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
        if (activeCount > 1 && onClearAll != null) {
            ActiveFilterChip(
                label = stringResource(R.string.action_clear_filter),
                onClear = onClearAll
            )
        }
    }
}

@Composable
private fun ActiveFilterChip(label: String, onClear: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onClear,
        modifier = Modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(16.dp),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.card_list_clear_named_filter, label),
                modifier = Modifier.size(InputChipDefaults.IconSize)
            )
        },
        colors = InputChipDefaults.inputChipColors(
            selectedContainerColor = AppColors.GreenPrimary,
            selectedLabelColor = AppColors.CardBg,
            selectedTrailingIconColor = AppColors.CardBg
        ),
        border = InputChipDefaults.inputChipBorder(
            enabled = true,
            selected = true,
            selectedBorderColor = Color.Transparent
        )
    )
}
