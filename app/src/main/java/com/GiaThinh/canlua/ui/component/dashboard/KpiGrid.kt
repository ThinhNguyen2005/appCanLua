package com.GiaThinh.canlua.ui.component.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class KpiGridItem(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val accentColor: Color,
    val deltaPercent: Double? = null,
    val deltaLabel: String? = null,
    val highlight: Boolean = false
)

@Composable
fun KpiGrid(
    items: List<KpiGridItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { item ->
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = item.icon,
                        label = item.label,
                        value = item.value,
                        accentColor = item.accentColor,
                        deltaPercent = item.deltaPercent,
                        deltaLabel = item.deltaLabel,
                        highlight = item.highlight
                    )
                }
                if (rowItems.size == 1) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
