package com.GiaThinh.canlua.ui.component.profile

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.ui.theme.AppColors

private val shimmerColors
    @Composable get() = shimmerColorsImpl()

@Composable
private fun shimmerColorsImpl(): SkeletonShimmerColors {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (isDark) {
        SkeletonShimmerColors(
            base = Color(0xFF1E1E1E),
            highlight = Color(0xFF3A3A3A),
            highlight2 = Color(0xFF2A2A2A)
        )
    } else {
        SkeletonShimmerColors(
            base = Color(0xFFD6D6D6),
            highlight = Color(0xFFEDEDED),
            highlight2 = Color(0xFFC8C8C8)
        )
    }
}

private data class SkeletonShimmerColors(
    val base: Color,
    val highlight: Color,
    val highlight2: Color
)

private fun Color.luminance(): Float {
    val r = red; val g = green; val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}

@Composable
fun FarmerProfileSkeleton() {
    val colors = shimmerColors
    val transition = rememberInfiniteTransition(label = "profile_skeleton")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeleton_progress"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TIER 0: Gradient Hero Header — ~80dp
        item {
            HeroHeaderSkeleton(colors, progress)
        }

        // TIER 1: Quick stats glass grid (3 cells) — ~96dp + top padding
        item {
            QuickStatsGridSkeleton(colors, progress)
        }

        // Section title skeleton
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    ShimmerLine(colors, progress, width = 180.dp, height = 18.dp)
                    Spacer(Modifier.height(6.dp))
                    ShimmerLine(colors, progress, width = 240.dp, height = 12.dp)
                }
            }
        }

        // TIER 2: Season chips — ~32dp
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    ShimmerBox(colors, progress, Modifier.size(width = 72.dp, height = 32.dp), 16.dp)
                }
            }
        }

        // TIER 3: Primary KPI 2×2 — ~228dp (108*2 + 12)
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCellSkeleton(colors, progress, Modifier.weight(1f))
                    KpiCellSkeleton(colors, progress, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCellSkeleton(colors, progress, Modifier.weight(1f))
                    KpiCellSkeleton(colors, progress, Modifier.weight(1f))
                }
            }
        }

        // TIER 4: Secondary stats pill row — ~56dp
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    ShimmerBox(colors, progress, Modifier.weight(1f).height(56.dp), 14.dp)
                }
            }
        }

        // TIER 5: AI insights card — match AiInsightsCard height (~160dp)
        item {
            ShimmerBox(colors, progress,
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(160.dp),
                18.dp
            )
        }

        // TIER 5b: Bar chart card — ~220dp
        item {
            ShimmerBox(colors, progress,
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(220.dp),
                18.dp
            )
        }

        // TIER 7: Trader history nav row — ~56dp
        item {
            ShimmerBox(colors, progress,
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                14.dp
            )
        }

        // TIER 8: Account operations — Personal info card (~180dp) + RoleSwitcher + logout row
        item {
            ShimmerBox(colors, progress,
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(180.dp),
                18.dp
            )
        }
    }
}

@Composable
private fun HeroHeaderSkeleton(colors: SkeletonShimmerColors, progress: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerCircle(colors, progress, Modifier.size(56.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerLine(colors, progress, width = 160.dp, height = 18.dp)
            ShimmerLine(colors, progress, width = 200.dp, height = 12.dp)
        }
        ShimmerBox(colors, progress, Modifier.size(width = 64.dp, height = 26.dp), 13.dp)
    }
}

@Composable
private fun QuickStatsGridSkeleton(colors: SkeletonShimmerColors, progress: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(3) {
            ShimmerBox(colors, progress, Modifier.weight(1f).height(96.dp), 16.dp)
        }
    }
}

@Composable
private fun KpiCellSkeleton(
    colors: SkeletonShimmerColors,
    progress: Float,
    modifier: Modifier = Modifier
) {
    ShimmerBox(colors, progress, modifier.height(108.dp), 16.dp)
}

@Composable
private fun ShimmerCircle(
    colors: SkeletonShimmerColors,
    progress: Float,
    modifier: Modifier = Modifier.size(48.dp)
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .shimmerBrush(colors, progress)
    )
}

@Composable
private fun ShimmerBox(
    colors: SkeletonShimmerColors,
    progress: Float,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .shimmerBrush(colors, progress)
    )
}

@Composable
private fun ShimmerLine(
    colors: SkeletonShimmerColors,
    progress: Float,
    width: Dp,
    height: Dp = 12.dp
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .shimmerBrush(colors, progress)
    )
}

private fun Modifier.shimmerBrush(
    colors: SkeletonShimmerColors,
    progress: Float
): Modifier = this.background(
    brush = Brush.linearGradient(
        colors = listOf(colors.base, colors.highlight, colors.highlight2, colors.base),
        start = Offset(progress * 600f - 300f, 0f),
        end = Offset(progress * 600f, 600f)
    )
)
