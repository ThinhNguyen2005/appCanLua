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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.ui.component.shimmerEffect

/**
 * Skeleton match layout 8-tier của [com.GiaThinh.canlua.ui.screen.trader.TraderProfileScreen].
 *
 * Tương tự FarmerProfileSkeleton nhưng TIER 5 là Pie Chart thay vì Bar Chart,
 * và TIER 7 là Pie Chart + Ledger Nav thay vì TopTraders + TraderHistory Nav.
 *
 * Mục tiêu: khi Crossfade chuyển từ skeleton sang real content, các tier
 * không bị "pop in" thẳng đứng nhờ giữ cùng cấu trúc LazyColumn.
 */
@Composable
fun TraderProfileSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TIER 0: Hero header skeleton (gold-themed trader)
        item { HeroHeaderSkeleton() }

        // TIER 1: Quick stats glass grid (3 cells: Vụ / Tấn / Đã chi)
        item { QuickStatsGridSkeleton() }

        // Section title skeleton
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    ShimmerBar(width = 200.dp, height = 18.dp)
                    Spacer(Modifier.height(6.dp))
                    ShimmerBar(width = 260.dp, height = 12.dp)
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

        // TIER 3: Primary KPI 2×3 (Trader: 6 cells)
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

        // TIER 5: Bar chart card (shared with farmer profile)
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

        // TIER 7: Variety Pie Chart skeleton (trader-specific)
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pie chart placeholder
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(50))
                            .shimmerEffect()
                    )
                    // Legend items
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(4) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .shimmerEffect()
                                )
                                Spacer(Modifier.width(8.dp))
                                ShimmerBar(width = 120.dp, height = 12.dp)
                            }
                        }
                    }
                }
            }
        }

        // Transaction ledger nav row
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

        // Account section title
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    ShimmerBar(width = 160.dp, height = 18.dp)
                    Spacer(Modifier.height(6.dp))
                    ShimmerBar(width = 220.dp, height = 12.dp)
                }
            }
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
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .shimmerEffect()
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ShimmerBar(width = 160.dp, height = 18.dp)
            ShimmerBar(width = 200.dp, height = 12.dp)
        }
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 26.dp)
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
