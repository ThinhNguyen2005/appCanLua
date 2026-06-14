package com.GiaThinh.canlua.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

private val surfaceColor
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

@Composable
fun CardListSkeleton() {
    val colors = skeletonColors()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShimmerBox(Modifier.fillMaxWidth().height(54.dp), 28.dp, colors)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(3) {
                ShimmerBox(Modifier.width(80.dp).height(32.dp), 16.dp, colors)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        repeat(3) {
            ShimmerBox(Modifier.fillMaxWidth().height(110.dp), 16.dp, colors)
        }
    }
}

@Composable
fun MarketSkeleton() {
    val colors = skeletonColors()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.surface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(2) {
                ShimmerBox(Modifier.weight(1f).height(42.dp), 18.dp, colors)
            }
        }
        CompactWeatherSkeleton(colors)
        repeat(3) { index ->
            MarketRowSkeleton(colors = colors, isPriceRow = index == 0)
        }
    }
}

@Composable
private fun CompactWeatherSkeleton(colors: SkeletonColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerBox(Modifier.size(48.dp), 0.dp, colors, isCircle = true)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerBox(Modifier.fillMaxWidth(0.58f).height(14.dp), 7.dp, colors)
            ShimmerBox(Modifier.fillMaxWidth(0.82f).height(26.dp), 10.dp, colors)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    ShimmerBox(Modifier.weight(1f).height(24.dp), 10.dp, colors)
                }
            }
        }
    }
}

@Composable
private fun MarketRowSkeleton(colors: SkeletonColors, isPriceRow: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isPriceRow) 84.dp else 96.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerBox(
            Modifier.size(if (isPriceRow) 38.dp else 52.dp),
            if (isPriceRow) 12.dp else 0.dp,
            colors,
            isCircle = !isPriceRow
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerBox(Modifier.fillMaxWidth(0.74f).height(15.dp), 8.dp, colors)
            ShimmerBox(
                Modifier.fillMaxWidth(if (isPriceRow) 0.46f else 0.9f).height(12.dp),
                6.dp,
                colors
            )
            if (!isPriceRow) {
                ShimmerBox(Modifier.fillMaxWidth(0.38f).height(12.dp), 6.dp, colors)
            }
        }
    }
}

@Composable
fun ProfileSkeleton() {
    val colors = skeletonColors()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        ShimmerBox(Modifier.size(100.dp), 0.dp, colors, isCircle = true)
        ShimmerBox(Modifier.width(150.dp).height(24.dp), 8.dp, colors)
        ShimmerBox(Modifier.width(90.dp).height(16.dp), 8.dp, colors)
        Spacer(modifier = Modifier.height(24.dp))
        repeat(4) {
            ShimmerBox(Modifier.fillMaxWidth().height(56.dp), 12.dp, colors)
        }
    }
}

@Composable
fun CardDetailSkeleton() {
    val colors = skeletonColors()
    val topSpacer = 52.dp // heights.expanded (TOPBAR_H)
    val cardSpacing = 14.dp // same as LazyColumn verticalArrangement

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(cardSpacing)
    ) {
        // Top spacer — matches heights.expanded + 16.dp in real content
        item { Spacer(modifier = Modifier.height(topSpacer + 16.dp)) }

        // Card 1: Thông tin phiếu (CardInfoCard)
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShimmerBox(Modifier.width(130.dp).height(16.dp), 8.dp, colors)
                    repeat(5) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ShimmerBox(Modifier.width(110.dp).height(13.dp), 6.dp, colors)
                            ShimmerBox(Modifier.width(130.dp).height(13.dp), 6.dp, colors)
                        }
                    }
                }
            }
        }

        // Card 2: Khối lượng (WeightSummaryCard)
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShimmerBox(Modifier.width(110.dp).height(16.dp), 8.dp, colors)
                    repeat(4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ShimmerBox(Modifier.width(100.dp).height(13.dp), 6.dp, colors)
                            ShimmerBox(Modifier.width(90.dp).height(13.dp), 6.dp, colors)
                        }
                    }
                    ShimmerBox(Modifier.fillMaxWidth().height(1.dp), 0.dp, colors.copy(surface = colors.surface.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(4.dp))
                    ShimmerBox(Modifier.fillMaxWidth().height(40.dp), 10.dp, colors)
                }
            }
        }

        // Card 3: Tài chính (FinancialSummaryCard)
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShimmerBox(Modifier.width(90.dp).height(16.dp), 8.dp, colors)
                    repeat(3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ShimmerBox(Modifier.width(100.dp).height(13.dp), 6.dp, colors)
                            ShimmerBox(Modifier.width(110.dp).height(13.dp), 6.dp, colors)
                        }
                    }
                    // "Còn lại" row (highlighted)
                    ShimmerBox(Modifier.fillMaxWidth().height(1.dp), 0.dp, colors.copy(surface = colors.surface.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ShimmerBox(Modifier.width(80.dp).height(14.dp), 7.dp, colors)
                        ShimmerBox(Modifier.width(120.dp).height(14.dp), 7.dp, colors)
                    }
                }
            }
        }

        // Card 4: Bảng cân (BagEntriesCard with HorizontalPager tabs)
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShimmerBox(Modifier.width(120.dp).height(16.dp), 8.dp, colors)
                    // Tab row skeleton
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(2) {
                            ShimmerBox(
                                Modifier.weight(1f).fillMaxHeight(),
                                6.dp,
                                colors,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Table rows skeleton
                    repeat(4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ShimmerBox(Modifier.width(28.dp).height(13.dp), 6.dp, colors)
                            ShimmerBox(Modifier.width(120.dp).height(13.dp), 6.dp, colors)
                            ShimmerBox(Modifier.width(60.dp).height(13.dp), 6.dp, colors)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeightInputSkeleton() {
    val colors = skeletonColors()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerBox(Modifier.fillMaxWidth().height(130.dp), 16.dp, colors)
        ShimmerBox(Modifier.fillMaxWidth().weight(1f), 16.dp, colors)
        ShimmerBox(Modifier.fillMaxWidth().height(220.dp), 16.dp, colors)
    }
}

@Composable
fun DefaultSkeleton() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
    }
}

/** Shared shimmer box — thống nhất 45° shimmer trên toàn app. */
@Composable
private fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    colors: SkeletonColors = skeletonColors(),
    isCircle: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(if (isCircle) CircleShape else RoundedCornerShape(cornerRadius))
            .shimmerEffect(colors)
    )
}
