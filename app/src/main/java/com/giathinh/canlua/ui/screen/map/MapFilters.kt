package com.giathinh.canlua.ui.screen.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.ui.theme.AppColors

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.giathinh.canlua.R
import com.giathinh.canlua.util.HapticUtil

/**
 * Toolbar tìm kiếm + nút toggle bộ lọc cho RiceMap.
 *
 * Tách khỏi RiceMapScreen.kt (909 dòng) để screen chính chỉ còn state + layout.
 * Visibility `internal` vì chỉ dùng trong cùng package `ui.screen.map`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchAndFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    activeFilters: Boolean,
    matchCount: Int,
    totalCount: Int
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.CardBg.copy(alpha = 0.96f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    stringResource(R.string.map_search_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextHint
                )
            },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = AppColors.TextHint)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            HapticUtil.tick(context)
                            onClear()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.map_action_clear))
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (activeFilters || showFilters)
                        AppColors.GreenPrimary.copy(alpha = 0.18f)
                    else
                        Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = {
                    HapticUtil.tick(context)
                    onToggleFilters()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = stringResource(R.string.map_filter_toggle),
                    tint = if (activeFilters) AppColors.GreenPrimary else AppColors.TextSecondary
                )
            }
        }
    }

    if (totalCount > 0 && (query.isNotBlank() || activeFilters)) {
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                stringResource(R.string.map_match_count, matchCount, totalCount),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Hai hàng FilterChip: ngưỡng giá + giống lúa. Show/hide qua AnimatedVisibility ở caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterChipsRow(
    priceFilter: PriceFilter,
    onPriceChange: (PriceFilter) -> Unit,
    varieties: List<String>,
    selectedVariety: String?,
    onVarietyChange: (String?) -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            stringResource(R.string.map_filter_price_header),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextHint,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PriceFilter.entries.size, key = { PriceFilter.entries[it].name }) { idx ->
                val pf = PriceFilter.entries[idx]
                FilterChip(
                    selected = priceFilter == pf,
                    onClick = {
                        HapticUtil.tick(context)
                        onPriceChange(pf)
                    },
                    label = { Text(pf.label()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.GreenPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (varieties.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.map_filter_variety_header),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextHint,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(varieties.size + 1, key = { if (it == 0) "_all" else varieties[it - 1] }) { i ->
                    if (i == 0) {
                        FilterChip(
                            selected = selectedVariety == null,
                            onClick = {
                                HapticUtil.tick(context)
                                onVarietyChange(null)
                            },
                            label = { Text(stringResource(R.string.map_filter_all)) }
                        )
                    } else {
                        val v = varieties[i - 1]
                        FilterChip(
                            selected = selectedVariety == v,
                            onClick = {
                                HapticUtil.tick(context)
                                onVarietyChange(if (selectedVariety == v) null else v)
                            },
                            label = { Text(v) }
                        )
                    }
                }
            }
        }
    }
}

internal enum class PriceFilter { ALL, HAS_PRICE, MIN_7K, MIN_8K, MIN_9K }

@Composable
internal fun PriceFilter.label(): String = when (this) {
    PriceFilter.ALL -> stringResource(R.string.map_filter_all)
    PriceFilter.HAS_PRICE -> stringResource(R.string.map_filter_has_price)
    PriceFilter.MIN_7K -> "≥ 7,000 đ"
    PriceFilter.MIN_8K -> "≥ 8,000 đ"
    PriceFilter.MIN_9K -> "≥ 9,000 đ"
}

/**
 * Strip diacritics + lowercase + chuyển "đ" → "d" để search không phân biệt dấu.
 * Ví dụ: "Nông Dân" → "nong dan", khớp khi user gõ "nong" / "dan" / "Nong Dan".
 */
internal fun String.normalizeForSearch(): String {
    val lowered = lowercase()
    val noDiacritics = java.text.Normalizer.normalize(lowered, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .replace("đ", "d")
    return noDiacritics
}

// LazyRow needs items() — import shim
internal inline fun androidx.compose.foundation.lazy.LazyListScope.items(
    count: Int,
    crossinline itemContent: @Composable (Int) -> Unit
) {
    items(count = count) { idx -> itemContent(idx) }
}
