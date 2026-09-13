package com.giathinh.canlua.ui.component.market

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.ui.component.ShimmerBox
import com.giathinh.canlua.ui.component.SkeletonColors
import com.giathinh.canlua.ui.component.rememberShimmerProgress
import com.giathinh.canlua.ui.component.skeletonColors

import androidx.compose.runtime.CompositionLocalProvider
import com.giathinh.canlua.ui.component.LocalShimmerProgress

/**
 * Skeleton loading cho màn hình Thị trường lúa gạo (MarketScreen).
 */
@Composable
fun MarketSkeleton(
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    val colors = skeletonColors()
    val sharedProgress = progress ?: rememberShimmerProgress()

    CompositionLocalProvider(LocalShimmerProgress provides sharedProgress) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tab row skeleton
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
                    ShimmerBox(
                        modifier = Modifier.weight(1f).height(42.dp),
                        cornerRadius = 18.dp,
                        colors = colors,
                        progress = sharedProgress
                    )
                }
            }
            // Market price & news rows skeleton
            repeat(3) { index ->
                MarketRowSkeleton(
                    colors = colors,
                    isPriceRow = index == 0,
                    progress = sharedProgress
                )
            }
        }
    }
}

@Composable
fun MarketRowSkeleton(
    colors: SkeletonColors = skeletonColors(),
    isPriceRow: Boolean = false,
    progress: Float? = null
) {
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
            modifier = Modifier.size(if (isPriceRow) 38.dp else 52.dp),
            cornerRadius = if (isPriceRow) 12.dp else 0.dp,
            colors = colors,
            progress = progress,
            isCircle = !isPriceRow
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerBox(
                modifier = Modifier.fillMaxWidth(0.74f).height(15.dp),
                cornerRadius = 8.dp,
                colors = colors,
                progress = progress
            )
            ShimmerBox(
                modifier = Modifier.fillMaxWidth(if (isPriceRow) 0.46f else 0.9f).height(12.dp),
                cornerRadius = 6.dp,
                colors = colors,
                progress = progress
            )
            if (!isPriceRow) {
                ShimmerBox(
                    modifier = Modifier.fillMaxWidth(0.38f).height(12.dp),
                    cornerRadius = 6.dp,
                    colors = colors,
                    progress = progress
                )
            }
        }
    }
}
