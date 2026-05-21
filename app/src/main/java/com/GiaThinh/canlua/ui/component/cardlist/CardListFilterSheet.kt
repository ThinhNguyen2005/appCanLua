package com.GiaThinh.canlua.ui.component.cardlist

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.GiaThinh.canlua.R
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListFilterSheet(
    availableSeasons: List<String>,
    availableVarieties: List<String>,
    selectedSeason: String?,
    selectedVariety: String?,
    onSelectSeason: (String?) -> Unit,
    onSelectVariety: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val filterSheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = filterSheetState,
        containerColor = AppColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.card_list_filter_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                if (selectedSeason != null || selectedVariety != null) {
                    TextButton(onClick = {
                        onSelectSeason(null)
                        onSelectVariety(null)
                    }) {
                        Text(stringResource(R.string.action_clear_filter), color = AppColors.Error)
                    }
                }
            }

            if (availableSeasons.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.card_list_filter_season),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSeason == null,
                        onClick = { onSelectSeason(null) },
                        label = { Text(stringResource(R.string.card_list_filter_all_seasons)) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.GreenPrimary,
                            selectedLabelColor = AppColors.CardBg,
                            selectedLeadingIconColor = AppColors.CardBg
                        )
                    )
                    availableSeasons.forEach { season ->
                        FilterChip(
                            selected = selectedSeason == season,
                            onClick = { onSelectSeason(if (selectedSeason == season) null else season) },
                            label = { Text(season) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.GreenPrimary,
                                selectedLabelColor = AppColors.CardBg
                            )
                        )
                    }
                }
            }

            if (availableVarieties.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.card_list_filter_variety),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedVariety == null,
                        onClick = { onSelectVariety(null) },
                        label = { Text(stringResource(R.string.card_list_filter_all)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.GreenPrimary,
                            selectedLabelColor = AppColors.CardBg
                        )
                    )
                    availableVarieties.forEach { variety ->
                        FilterChip(
                            selected = selectedVariety == variety,
                            onClick = { onSelectVariety(if (selectedVariety == variety) null else variety) },
                            label = { Text(variety) },
                            leadingIcon = {
                                if (selectedVariety == variety) {
                                    Icon(
                                        Icons.Outlined.Grass,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.GreenPrimary,
                                selectedLabelColor = AppColors.CardBg
                            )
                        )
                    }
                }
            }
        }
    }
}
