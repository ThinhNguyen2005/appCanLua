package com.giathinh.canlua.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

private const val ANIMATION_DURATION_MS = 150

/**
 * Animated number with vertical slide transition.
 * Increasing values slide up from below; decreasing values slide down from above.
 *
 * Use for live-updating weight/money displays where each digit change should feel
 * tactile, like a physical counter rolling.
 */
@Composable
fun AnimatedNumber(
    value: Double,
    formatter: (Double) -> String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
) {
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        transitionSpec = {
            val goingUp = targetState > initialState
            val enter = slideInVertically(
                animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutLinearInEasing),
            ) { height -> if (goingUp) height else -height } + fadeIn(
                animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutLinearInEasing),
            )
            val exit = slideOutVertically(
                animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutLinearInEasing),
            ) { height -> if (goingUp) -height else height } + fadeOut(
                animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutLinearInEasing),
            )
            enter.togetherWith(exit).using(SizeTransform(clip = false))
        },
        label = "animated_number",
    ) { current ->
        Text(
            text = formatter(current),
            style = style,
            color = color,
            fontWeight = fontWeight,
        )
    }
}

/**
 * Int variant — for counters like "X bao".
 */
@Composable
fun AnimatedNumber(
    value: Int,
    formatter: (Int) -> String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
) {
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        transitionSpec = {
            val goingUp = targetState > initialState
            val enter = slideInVertically(
                animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutLinearInEasing),
            ) { height -> if (goingUp) height else -height } + fadeIn(
                animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutLinearInEasing),
            )
            val exit = slideOutVertically(
                animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutLinearInEasing),
            ) { height -> if (goingUp) -height else height } + fadeOut(
                animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutLinearInEasing),
            )
            enter.togetherWith(exit).using(SizeTransform(clip = false))
        },
        label = "animated_number_int",
    ) { current ->
        Text(
            text = formatter(current),
            style = style,
            color = color,
            fontWeight = fontWeight,
        )
    }
}
