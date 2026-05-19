package com.GiaThinh.canlua.ui.component.detail

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.ui.theme.AppColors

/**
 * Shimmer skeleton 45° gradient cho CardDetailScreen.
 * 3 placeholder cards + grid placeholder.
 */
@Composable
fun DetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SkeletonCard(height = 220.dp)
        SkeletonCard(height = 180.dp)
        SkeletonCard(height = 110.dp)
    }
}

@Composable
private fun SkeletonCard(height: androidx.compose.ui.unit.Dp) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val translate by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    Card(
        modifier = Modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            AppColors.SurfaceContainer,
                            AppColors.Divider,
                            AppColors.SurfaceContainer
                        ),
                        start = Offset(translate * 600f - 300f, 0f),
                        end = Offset(translate * 600f, 600f)
                    )
                )
        )
    }
}
