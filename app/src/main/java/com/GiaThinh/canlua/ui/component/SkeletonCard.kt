package com.GiaThinh.canlua.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Skeleton loading card — shimmer animation while data is loading.
 * Prevents blank screens and gives visual feedback.
 */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    val shimmer = rememberInfiniteTransition(label = "skeleton_shimmer")
    val alpha by shimmer.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Gray.copy(alpha = alpha * 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerBackground(alpha)
            )
            Spacer(Modifier.height(12.dp))
            // Content placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerBackground(alpha)
            )
            Spacer(Modifier.height(8.dp))
            // Content placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerBackground(alpha)
            )
            Spacer(Modifier.height(12.dp))
            // Bottom row placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmerBackground(alpha)
            )
        }
    }
}

@Composable
fun SkeletonList(count: Int = 3) {
    Column {
        repeat(count) {
            SkeletonCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
    }
}

private fun Modifier.shimmerBackground(alpha: Float): Modifier {
    return this.then(
        Modifier.clip(RoundedCornerShape(4.dp))
    )
}
