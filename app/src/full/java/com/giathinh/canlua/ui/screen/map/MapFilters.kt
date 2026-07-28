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
                    "Tìm tên / giống lúa / địa chỉ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextHint
                )
            },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = AppColors.TextHint)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Close, contentDescription = "Xoá")
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
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (activeFilters || showFilters)
                        AppColors.GreenPrimary.copy(alpha = 0.18f)
                    else
                        Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onToggleFilters) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = "Bộ lọc",
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
                "Hiện $matchCount / $totalCount điểm",
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
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            "Giá lúa",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextHint,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PriceFilter.entries.size, key = { PriceFilter.entries[it].name }) { idx ->
                val pf = PriceFilter.entries[idx]
                FilterChip(
                    selected = priceFilter == pf,
                    onClick = { onPriceChange(pf) },
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
                "Giống lúa",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextHint,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(varieties.size + 1, key = { if (it == 0) "_all" else varieties[it - 1] }) { i ->
                    if (i == 0) {
                        FilterChip(
                            selected = selectedVariety == null,
                            onClick = { onVarietyChange(null) },
                            label = { Text("Tất cả") }
                        )
                    } else {
                        val v = varieties[i - 1]
                        FilterChip(
                            selected = selectedVariety == v,
                            onClick = {
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

internal fun PriceFilter.label(): String = when (this) {
    PriceFilter.ALL -> "Tất cả"
    PriceFilter.HAS_PRICE -> "Đã có giá"
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
