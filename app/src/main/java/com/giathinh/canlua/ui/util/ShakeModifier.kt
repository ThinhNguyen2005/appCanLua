package com.giathinh.canlua.ui.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val SHAKE_KEYFRAMES = listOf(-1f, 1f, -0.7f, 0.7f, -0.35f, 0.35f, 0f)

/**
 * Horizontal shake modifier triggered whenever [trigger] increments above `0`.
 * Applies [graphicsLayer] translationX in the draw phase to avoid extra recompositions.
 */
@Composable
fun Modifier.shake(trigger: Int, amplitudeDp: Dp = 10.dp): Modifier {
    val offset = remember { Animatable(0f) }
    val amplitudePx = with(LocalDensity.current) { amplitudeDp.toPx() }

    LaunchedEffect(trigger) {
        if (trigger > 0) {
            offset.snapTo(0f)
            for (target in SHAKE_KEYFRAMES) {
                offset.animateTo(targetValue = target, animationSpec = tween(durationMillis = 45))
            }
        }
    }

    return this.graphicsLayer {
        translationX = offset.value * amplitudePx
    }
}
