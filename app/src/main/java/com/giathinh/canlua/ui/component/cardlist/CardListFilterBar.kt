package com.giathinh.canlua.ui.component.cardlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.R
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.viewmodel.CardQuickFilter

@Composable
fun CardListFilterBar(
    selectedSeason: String?,
    selectedVariety: String?,
    onOpenFilter: () -> Unit,
    onClearSeason: () -> Unit,
    onClearVariety: () -> Unit,
    modifier: Modifier = Modifier,
    quickFilter: CardQuickFilter = CardQuickFilter.ALL,
    onQuickFilterChange: (CardQuickFilter) -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onClearAll: (() -> Unit)? = null
) {
    val activeCount = (if (selectedSeason != null) 1 else 0) +
        (if (selectedVariety != null) 1 else 0) +
        (if (quickFilter != CardQuickFilter.ALL) 1 else 0)

    val quickFilterLabel = when (quickFilter) {
        CardQuickFilter.ALL -> null
        CardQuickFilter.UNLOCKED -> stringResource(R.string.card_list_quick_unlocked)
        CardQuickFilter.UNPRICED -> stringResource(R.string.card_list_quick_unpriced)
        CardQuickFilter.UNPAID -> stringResource(R.string.card_list_quick_unpaid)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CardListSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier.weight(1f)
            )

            FilterSheetTriggerButton(
                activeCount = activeCount,
                onClick = onOpenFilter
            )
        }

        if (activeCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickFilterLabel?.let { statusLabel ->
                    ActiveFilterChip(
                        label = statusLabel,
                        onClear = { onQuickFilterChange(CardQuickFilter.ALL) }
                    )
                }
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
    }
}

@Composable
private fun FilterSheetTriggerButton(
    activeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = activeCount > 0
    val contentColor = if (isActive) AppColors.GreenPrimary else AppColors.TextSecondary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) AppColors.GreenSurface else AppColors.CardBg,
        border = BorderStroke(
            width = 1.dp,
            color = if (isActive) AppColors.GreenPrimary else AppColors.Divider
        ),
        modifier = modifier.heightIn(min = 52.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (isActive) {
                    stringResource(R.string.card_list_filter_count, activeCount)
                } else {
                    stringResource(R.string.card_list_filter)
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CardListSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = {
            Text(
                text = stringResource(R.string.card_list_search_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextHint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (query.isNotBlank()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.card_list_search_clear),
                        tint = AppColors.TextSecondary
                    )
                }
            }
        } else {
            null
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.GreenPrimary,
            unfocusedBorderColor = AppColors.Divider,
            focusedContainerColor = AppColors.CardBg,
            unfocusedContainerColor = AppColors.CardBg
        ),
        modifier = modifier
    )
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
            selectedLabelColor = AppColors.OnGreenPrimary,
            selectedTrailingIconColor = AppColors.OnGreenPrimary
        ),
        border = InputChipDefaults.inputChipBorder(
            enabled = true,
            selected = true,
            selectedBorderColor = Color.Transparent
        )
    )
}
