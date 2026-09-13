package com.giathinh.canlua.ui.component.detail

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.ui.component.ShimmerBox
import com.giathinh.canlua.ui.component.rememberShimmerProgress
import com.giathinh.canlua.ui.component.skeletonColors

import androidx.compose.runtime.CompositionLocalProvider
import com.giathinh.canlua.ui.component.LocalShimmerProgress

/**
 * Skeleton loading chi tiết phiếu cân (CardDetailScreen) — mô phỏng 4 card chính:
 * 1. CardInfoCard (Thông tin chung)
 * 2. WeightSummaryCard (Khối lượng & bao)
 * 3. FinancialSummaryCard (Tiền & thanh toán)
 * 4. BagEntriesCard (Bảng cân từng bao & tab)
 */
@Composable
fun CardDetailSkeleton(
    modifier: Modifier = Modifier,
    topSpacerHeight: androidx.compose.ui.unit.Dp = 68.dp // TopBar + padding
) {
    val colors = skeletonColors()
    val sharedProgress = rememberShimmerProgress()

    CompositionLocalProvider(LocalShimmerProgress provides sharedProgress) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top spacer khớp vị trí header
            item { Spacer(modifier = Modifier.height(topSpacerHeight)) }

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
                    ShimmerBox(Modifier.width(130.dp).height(16.dp), 8.dp, colors, progress = sharedProgress)
                    repeat(5) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ShimmerBox(Modifier.width(110.dp).height(13.dp), 6.dp, colors, progress = sharedProgress)
                            ShimmerBox(Modifier.width(130.dp).height(13.dp), 6.dp, colors, progress = sharedProgress)
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
                    ShimmerBox(Modifier.width(110.dp).height(16.dp), 8.dp, colors, progress = sharedProgress)
                    repeat(4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ShimmerBox(Modifier.width(100.dp).height(13.dp), 6.dp, colors, progress = sharedProgress)
                            ShimmerBox(Modifier.width(90.dp).height(13.dp), 6.dp, colors, progress = sharedProgress)
                        }
                    }
                    ShimmerBox(Modifier.fillMaxWidth().height(1.dp), 0.dp, colors, progress = sharedProgress)
                    Spacer(modifier = Modifier.height(4.dp))
                    ShimmerBox(Modifier.fillMaxWidth().height(40.dp), 10.dp, colors, progress = sharedProgress)
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
                    ShimmerBox(Modifier.width(90.dp).height(16.dp), 8.dp, colors, progress = sharedProgress)
                    repeat(3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ShimmerBox(Modifier.width(100.dp).height(13.dp), 6.dp, colors, progress = sharedProgress)
                            ShimmerBox(Modifier.width(110.dp).height(13.dp), 6.dp, colors, progress = sharedProgress)
                        }
                    }
                    ShimmerBox(Modifier.fillMaxWidth().height(1.dp), 0.dp, colors, progress = sharedProgress)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ShimmerBox(Modifier.width(80.dp).height(14.dp), 7.dp, colors, progress = sharedProgress)
                        ShimmerBox(Modifier.width(120.dp).height(14.dp), 7.dp, colors, progress = sharedProgress)
                    }
                }
            }
        }

        // Card 4: Bảng cân (BagEntriesCard)
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
                    ShimmerBox(Modifier.width(120.dp).height(16.dp), 8.dp, colors, progress = sharedProgress)
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
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                cornerRadius = 6.dp,
                                colors = colors,
                                progress = sharedProgress
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    repeat(4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ShimmerBox(Modifier.width(28.dp).height(13.dp), 6.dp, colors, progress = sharedProgress)
                            ShimmerBox(Modifier.width(120.dp).height(13.dp), 6.dp, colors, progress = sharedProgress)
                            ShimmerBox(Modifier.width(60.dp).height(13.dp), 6.dp, colors, progress = sharedProgress)
                        }
                    }
                }
            }
        }
    }
}
}
