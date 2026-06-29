package com.giathinh.canlua.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.util.HapticUtil


/**
 * Bottom Navigation Bar — icon + label LUÔN hiện, indicator pill bao icon khi active.
 *
 * ## Thiết kế (v2 — 2026-05-26)
 * Trước đây: chỉ tab active mới hiện label (pill expand ngang) → tab idle khó nhận biết.
 * Giờ: tất cả tab hiện label dưới icon (M3 NavigationBar pattern) → trực quan + đều.
 *
 * Layout per item:
 *  - Indicator pill (rounded 16dp) wrap icon khi selected, background GreenPrimary.
 *  - Label dưới icon, font weight đổi Medium → Bold khi selected.
 *  - Item dùng `weight(1f)` chia đều → fit 4 hoặc 5 tab trên màn hình nhỏ.
 *
 * Tương thích màn hình nhỏ:
 *  - Padding ngang outer 10dp (giảm từ 16dp) → fit thiết bị 320dp width.
 *  - Item padding 4dp horizontal, indicator 16dp wide.
 *  - Label `fontSize=11sp, maxLines=1, ellipsis` → không overflow ngay cả "Tài khoản".
 *  - Tổng height 72dp (icon 24 + indicator pad + label 14 + spacing).
 */
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import com.giathinh.canlua.ui.navigation.BottomNavItem

class BottomBarCutoutShape(private val cutoutRadiusDp: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cutoutRadius = with(density) { cutoutRadiusDp.toPx() }
        val path = Path().apply {
            moveTo(0f, 0f)
            val middle = size.width / 2
            val cutoutLeft = middle - cutoutRadius
            val cutoutRight = middle + cutoutRadius
            
            lineTo(cutoutLeft, 0f)
            arcTo(
                rect = Rect(
                    left = cutoutLeft,
                    top = -cutoutRadius,
                    right = cutoutRight,
                    bottom = cutoutRadius
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun ModernBottomBar(
    items: List<BottomBarItemSpec>,
    currentRoute: String?,
    onItemClick: (BottomBarItemSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = BottomBarCutoutShape(38.dp), // Radius 38dp to comfortably fit 56dp FAB + 10dp gap
        color = AppColors.CardBg,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.DividerStrong.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding() // Đệm hệ thống dưới nút điều hướng ảo
                .height(64.dp), // Chiều cao tối ưu tiêu chuẩn
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side items (Cân Lúa, Lịch sử)
            val leftItems = items.filter { it.route == BottomNavItem.SCALE.route || it.route == BottomNavItem.HISTORY.route }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                leftItems.forEach { item ->
                    val selected = currentRoute == item.route
                    LabeledNavItem(
                        item = item,
                        selected = selected,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Central spacer for the FAB
            Spacer(modifier = Modifier.width(76.dp))

            // Right side items (Thống kê, Cài đặt)
            val rightItems = items.filter { it.route != BottomNavItem.SCALE.route && it.route != BottomNavItem.HISTORY.route }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rightItems.forEach { item ->
                    val selected = currentRoute == item.route
                    LabeledNavItem(
                        item = item,
                        selected = selected,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Item: indicator pill bao icon + label luôn hiện dưới.
 *
 * Animations:
 *  - Indicator width animate dp (0 → 36dp khi selected) — không dùng AnimatedVisibility
 *    để label/icon không nhảy vị trí.
 *  - Icon swap outlined ↔ filled bằng AnimatedContent crossfade.
 *  - Content color animate sang green khi selected.
 */
@Composable
private fun LabeledNavItem(
    item: BottomBarItemSpec,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val labelText = item.labelRes?.let { androidx.compose.ui.res.stringResource(it) } ?: item.label
    val interactionSource = remember { MutableInteractionSource() }

    val activeColor = AppColors.GreenPrimary
    val onActiveColor = AppColors.CardBg
    val idleColor = AppColors.TextSecondary

    val iconTint by animateColorAsState(
        targetValue = if (selected) onActiveColor else idleColor,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "nav_icon_tint"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) activeColor else idleColor,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "nav_label_tint"
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "nav_indicator_alpha"
    )
    val indicatorScaleX by animateFloatAsState(
        targetValue = if (selected) 1f else 0.5f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "nav_indicator_scale_x"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                HapticUtil.tick(context)
                onClick()
            }
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Indicator pill bao icon — width/alpha animate, height cố định.
        Box(
            modifier = Modifier
                .height(28.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background pill — nằm dưới icon, dùng graphicsLayer để không bị méo hình
            if (indicatorAlpha > 0f) {
                Surface(
                    color = activeColor,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .size(width = 40.dp, height = 28.dp)
                        .graphicsLayer {
                            alpha = indicatorAlpha
                            scaleX = indicatorScaleX
                        }
                ) {}
            }
            // Icon ở trên indicator — crossfade outlined ↔ filled khi đổi state.
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    (fadeIn(tween(220, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(tween(160, easing = FastOutSlowInEasing)))
                },
                label = "nav_icon_swap"
            ) { isSelected ->
                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.icon,
                    contentDescription = labelText,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        // Label — luôn hiện, đậm hơn khi selected, ellipsis cho thiết bị nhỏ.
        Text(
            text = labelText,
            color = labelColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(
                lineHeight = 13.sp
            )
        )
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
    val label: String = "",
    val labelRes: Int? = null
)
