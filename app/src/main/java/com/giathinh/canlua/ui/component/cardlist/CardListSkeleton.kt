package com.giathinh.canlua.ui.component.cardlist

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.ui.component.ShimmerBox
import com.giathinh.canlua.ui.component.SkeletonColors
import com.giathinh.canlua.ui.component.rememberShimmerProgress
import com.giathinh.canlua.ui.component.shimmerEffect
import com.giathinh.canlua.ui.component.skeletonColors

import androidx.compose.runtime.CompositionLocalProvider
import com.giathinh.canlua.ui.component.LocalShimmerProgress

/**
 * Skeleton danh sách phiếu cân — đồng bộ cấu trúc layout với CardListScreen
 * nhằm giảm thiểu layout shift và visual jump khi chuyển sang nội dung thực tế.
 */
@Composable
fun CardListSkeleton(
    count: Int = 3,
    listState: LazyListState = rememberLazyListState()
) {
    val sharedProgress = rememberShimmerProgress()
    CompositionLocalProvider(LocalShimmerProgress provides sharedProgress) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics { contentDescription = "Loading" },
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "skeleton_summary") {
                SummaryCardSkeleton(progress = sharedProgress)
            }
            item(key = "skeleton_filter_placeholder") {
                Spacer(Modifier.height(44.dp))
            }
            item(key = "skeleton_list_header") {
                Spacer(Modifier.height(20.dp))
            }
            items(
                count = count,
                key = { "skeleton_card_$it" }
            ) { _ ->
                SkeletonCard(progress = sharedProgress)
            }
        }
    }
}

/**
 * Skeleton "Hôm nay" — placeholder cho [CardListSummaryCard].
 */
@Composable
fun SummaryCardSkeleton(
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    val colors = skeletonColors()
    val shimmerProgress = progress ?: rememberShimmerProgress()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBlock(widthFraction = 0.34f, height = 12.dp, corner = 6.dp, colors = colors, progress = shimmerProgress)
                    Spacer(Modifier.height(8.dp))
                    ShimmerBlock(widthFraction = 0.52f, height = 18.dp, corner = 8.dp, colors = colors, progress = shimmerProgress)
                }
                ShimmerBlock(widthFraction = 0.18f, height = 24.dp, corner = 12.dp, colors = colors, progress = shimmerProgress)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.shimmerBase.copy(alpha = 0.38f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShimmerBox(
                            modifier = Modifier.size(22.dp),
                            cornerRadius = 8.dp,
                            colors = colors,
                            progress = shimmerProgress
                        )
                        ShimmerBlock(widthFraction = 0.82f, height = 10.dp, corner = 5.dp, colors = colors, progress = shimmerProgress)
                        ShimmerBlock(widthFraction = 0.7f, height = 16.dp, corner = 8.dp, colors = colors, progress = shimmerProgress)
                    }
                }
            }
        }
    }
}

/**
 * Skeleton loading card — placeholder từng phiếu cân.
 */
@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    val colors = skeletonColors()
    val shimmerProgress = progress ?: rememberShimmerProgress()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(
                modifier = Modifier.size(44.dp),
                cornerRadius = 14.dp,
                colors = colors,
                progress = shimmerProgress
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerBlock(widthFraction = 0.72f, height = 16.dp, corner = 8.dp, colors = colors, progress = shimmerProgress)
                ShimmerBlock(widthFraction = 0.46f, height = 12.dp, corner = 6.dp, colors = colors, progress = shimmerProgress)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerBlock(widthFraction = 0.32f, height = 26.dp, corner = 10.dp, colors = colors, progress = shimmerProgress)
                    ShimmerBlock(widthFraction = 0.28f, height = 26.dp, corner = 10.dp, colors = colors, progress = shimmerProgress)
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerBlock(widthFraction = 0.18f, height = 16.dp, corner = 8.dp, colors = colors, progress = shimmerProgress)
                ShimmerBlock(widthFraction = 0.22f, height = 28.dp, corner = 12.dp, colors = colors, progress = shimmerProgress)
            }
        }
    }
}

@Composable
fun SkeletonList(
    count: Int = 3,
    modifier: Modifier = Modifier
) {
    val sharedProgress = rememberShimmerProgress()
    Column(modifier = modifier) {
        repeat(count) {
            SkeletonCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                progress = sharedProgress
            )
        }
    }
}

@Composable
private fun ShimmerBlock(
    widthFraction: Float,
    height: Dp,
    corner: Dp = 4.dp,
    colors: SkeletonColors = skeletonColors(),
    progress: Float? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(corner))
            .shimmerEffect(colors, progress),
    )
}
