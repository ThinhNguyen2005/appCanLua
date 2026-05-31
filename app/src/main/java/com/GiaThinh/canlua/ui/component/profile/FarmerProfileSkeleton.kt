package com.GiaThinh.canlua.ui.component.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.ui.component.shimmerEffect

/**
 * Skeleton match layout 8-tier của [FarmerProfileScreen].
 *
 * Mục tiêu: giữ LazyColumn cùng padding/spacing với UI thật để khi
 * Crossfade sang real content, các tier không bị "pop in" thẳng đứng.
 *
 * Render các block chính: Hero, QuickStats glass grid, section title, season chips,
 * KPI 2×2, secondary stats, bar chart, AI insights, trader history nav row,
 * personal info, premium upsell.
 */
@Composable
fun FarmerProfileSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TIER 0: Hero header skeleton
        item { HeroHeaderSkeleton() }

        // TIER 1: Quick stats glass grid (3 cells)
        item { QuickStatsGridSkeleton() }

        // Section title skeleton
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    ShimmerBar(width = 180.dp, height = 18.dp)
                    Spacer(Modifier.height(6.dp))
                    ShimmerBar(width = 240.dp, height = 12.dp)
                }
            }
        }

        // TIER 2: Season chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(width = 72.dp, height = 32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .shimmerEffect()
                    )
                }
            }
        }

        // TIER 3: Primary KPI 2×2
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCellSkeleton(Modifier.weight(1f))
                    KpiCellSkeleton(Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCellSkeleton(Modifier.weight(1f))
                    KpiCellSkeleton(Modifier.weight(1f))
                }
            }
        }

        // TIER 4: Secondary stats pill row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .shimmerEffect()
                    )
                }
            }
        }

        // TIER 5: Bar chart card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(220.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .shimmerEffect()
            )
        }

        // TIER 6: AI insights card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .shimmerEffect()
            )
        }

        // Trader history nav row
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .shimmerEffect()
            )
        }

        // Personal info card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .shimmerEffect()
            )
        }

        // Premium upsell
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .shimmerEffect()
            )
        }
    }
}

@Composable
private fun HeroHeaderSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .shimmerEffect()
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerBar(width = 160.dp, height = 18.dp)
            ShimmerBar(width = 200.dp, height = 12.dp)
        }
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 26.dp)
                .clip(RoundedCornerShape(13.dp))
                .shimmerEffect()
        )
    }
}

@Composable
private fun QuickStatsGridSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .shimmerEffect()
            )
        }
    }
}

@Composable
private fun KpiCellSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(108.dp)
            .clip(RoundedCornerShape(16.dp))
            .shimmerEffect()
    )
}

@Composable
private fun ShimmerBar(width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(4.dp))
            .shimmerEffect()
    )
}
