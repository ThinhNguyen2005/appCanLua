package com.GiaThinh.canlua.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.util.HapticUtil


/**
 * Modern Floating Pill Navigation Bar.
 *
 * ## Thiết kế
 * - Floating capsule (32dp corners) cách edge ~16dp ngang, lơ lửng phía trên nav bar gesture.
 * - Item active: pill ngang chứa icon (filled) + label (animated expand).
 * - Item idle: chỉ icon outlined, no label → giảm visual noise.
 * - Spring animation cho transition giữa các tab + haptic feedback khi tap.
 * - Shadow ambient nhẹ + tonal surface tạo cảm giác premium.
 *
 * ## Tham số
 * @param items Danh sách các tab.
 * @param currentRoute Route hiện tại (so khớp với [BottomBarItemSpec.route]).
 * @param onItemClick Callback khi user tap vào item.
 */
@Composable
fun ModernBottomBar(
    items: List<BottomBarItemSpec>,
    currentRoute: String?,
    onItemClick: (BottomBarItemSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = AppColors.CardBg,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = AppColors.ShadowAmbient,
                    spotColor = AppColors.ShadowDirect
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    PillNavItem(
                        item = item,
                        selected = selected,
                        onClick = { onItemClick(item) }
                    )
                }
            }
        }
    }
}

/**
 * Một item trong bottom bar — animation pill expand/shrink khi selected.
 */
@Composable
private fun PillNavItem(
    item: BottomBarItemSpec,
    selected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }

    // Selected: pill bg = green primary | Idle: transparent
    val bgColor = if (selected) AppColors.GreenPrimary else androidx.compose.ui.graphics.Color.Transparent
    val contentColor = if (selected) {
        // Trên dark mode GreenPrimary là light green → text/icon nên là forest dark.
        // Trên light mode GreenPrimary là forest dark → text/icon nên là white.
        AppColors.CardBg
    } else {
        AppColors.TextSecondary
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .height(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                HapticUtil.tick(context)
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = if (selected) 16.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon swap outlined ↔ filled với crossfade
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    (fadeIn(tween(220)) togetherWith fadeOut(tween(160)))
                },
                label = "icon-swap"
            ) { isSelected ->
                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.icon,
                    contentDescription = item.label,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Label chỉ xuất hiện khi selected — animate width
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(tween(180, delayMillis = 60)),
                exit = shrinkHorizontally(tween(180)) + fadeOut(tween(120))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Spec data class — adapter giữa domain BottomNavItem và component.
 * Tránh component coupling trực tiếp với navigation package.
 */
data class BottomBarItemSpec(
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String
)
