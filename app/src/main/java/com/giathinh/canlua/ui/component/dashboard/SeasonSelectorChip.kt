package com.giathinh.canlua.ui.component.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.ui.theme.AppColors

/**
 * Horizontal scroll chips để chọn vụ.
 * Selected → GreenPrimary background + check icon.
 * Unselected → outline với border GreenPrimary.
 */
@Composable
fun SeasonSelectorChip(
    seasons: List<String>,
    selectedSeason: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    simpleMode: Boolean = false
) {
    LazyRow(
        modifier = modifier.wrapContentHeight(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(seasons, key = { it }) { season ->
            ChipItem(
                label = season,
                selected = season == selectedSeason,
                onClick = { onSelect(season) },
                simpleMode = simpleMode
            )
        }
    }
}

@Composable
private fun ChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    simpleMode: Boolean = false
) {
    val greenPrimary = AppColors.GreenPrimary
    val cardBg = AppColors.CardBg
    val textPrimary = AppColors.TextPrimary

    val bgColor by animateColorAsState(
        if (selected) greenPrimary else cardBg,
        label = "chip_bg"
    )
    val contentColor by animateColorAsState(
        if (selected) Color.White else textPrimary,
        label = "chip_content"
    )

    val minHeight = if (simpleMode) 56.dp else 48.dp
    val textStyle = if (simpleMode) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium

    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .border(
                width = if (selected) 0.dp else 1.5.dp,
                color = if (selected) Color.Transparent else greenPrimary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                onClick = onClick,
                onClickLabel = "Chọn $label"
            )
            .padding(horizontal = if (simpleMode) 18.dp else 16.dp, vertical = if (simpleMode) 12.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(if (simpleMode) 22.dp else 18.dp)
            )
        }
        Text(
            text = label,
            style = textStyle.copy(
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
            )
        )
    }
}
