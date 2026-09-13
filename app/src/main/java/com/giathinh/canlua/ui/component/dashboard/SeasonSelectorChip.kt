package com.giathinh.canlua.ui.component.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.ui.theme.AppColors

/**
 * Horizontal scroll chips để chọn vụ mùa (filter dashboard theo vụ).
 * Sử dụng Material3 [FilterChip] chuẩn accessibility và touch target,
 * kết hợp branding của ứng dụng:
 *  - Selected: container [AppColors.GreenPrimary], content [MaterialTheme.colorScheme.onPrimary], icon check thanh mảnh.
 *  - Unselected: container [AppColors.CardBg], border [AppColors.GreenPrimary] (alpha 0.4f), shape bo tròn 20dp.
 */
@Composable
fun SeasonSelectorChip(
    seasons: List<String>,
    selectedSeason: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uniqueSeasons = remember(seasons) { seasons.distinct() }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(uniqueSeasons, key = { it }) { season ->
            val selected = season == selectedSeason
            SeasonFilterChip(
                label = season,
                selected = selected,
                onClick = { onSelect(season) }
            )
        }
    }
}

@Composable
private fun SeasonFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val chipShape = RoundedCornerShape(20.dp)

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            )
        },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        shape = chipShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = AppColors.CardBg,
            labelColor = AppColors.TextPrimary,
            iconColor = AppColors.TextSecondary,
            selectedContainerColor = AppColors.GreenPrimary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = AppColors.GreenPrimary.copy(alpha = 0.4f),
            selectedBorderColor = Color.Transparent,
            borderWidth = 1.dp,
            selectedBorderWidth = 0.dp
        )
    )
}

