package com.giathinh.canlua.ui.component

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.util.HapticUtil

/**
 * Bottom Navigation Bar dạng Floating Capsule hiện đại chuẩn Material 3.
 *
 * Tính năng nâng cấp:
 * - Thiết kế Floating Pill (viên con nhộng lơ lửng) bo tròn hoàn toàn [CircleShape], có đổ bóng nhẹ chuẩn M3.
 * - Con trượt động (Sliding Animated Pill Indicator) lướt mượt mà giữa các tab qua spring physics.
 * - Icon & Text biến đổi màu sắc và tỉ lệ thu phóng (scale) mượt mà khi kích hoạt.
 * - Phản hồi rung xúc giác tinh tế ([HapticUtil.tick] và [HapticFeedbackType.TextHandleMove]).
 * - Không sử dụng hiệu ứng kính lỏng (liquid glass), giữ trọn vẹn phong cách thanh lịch, tốc độ cao của Material 3.
 * - Tương thích hoàn toàn 100% với cả Dark Mode và AMOLED.
 */
@Composable
fun ModernBottomBar(
    items: List<BottomBarItemSpec>,
    currentRoute: String?,
    onItemClick: (BottomBarItemSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val selectedIndex = remember(currentRoute, items) {
        val idx = items.indexOfFirst { it.route == currentRoute }
        if (idx >= 0) idx else 0
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape
                ),
            shape = CircleShape,
            color = AppColors.CardBg,
            border = BorderStroke(1.dp, AppColors.Divider.copy(alpha = 0.55f)),
            shadowElevation = 6.dp,
            tonalElevation = 2.dp
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val tabCount = items.size
                val tabWidthPx = constraints.maxWidth.toFloat() / tabCount
                val tabWidth = with(density) { tabWidthPx.toDp() }

                val indicatorOffset = remember {
                    Animatable(selectedIndex * tabWidthPx)
                }

                LaunchedEffect(selectedIndex, tabWidthPx) {
                    val targetOffset = selectedIndex * tabWidthPx
                    indicatorOffset.animateTo(
                        targetValue = targetOffset,
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = 380f
                        )
                    )
                }

                // Con trượt động (Sliding Indicator Pill) lướt êm ái dưới tab đang chọn
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(tabWidth)
                        .graphicsLayer {
                            translationX = indicatorOffset.value
                        }
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                        .background(
                            color = AppColors.GreenPrimary.copy(alpha = 0.14f),
                            shape = CircleShape
                        )
                )

                // Hàng chứa các tab điều hướng
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = index == selectedIndex
                        val labelText = stringResource(item.labelRes)

                        ModernBottomTabItem(
                            item = item,
                            label = labelText,
                            isSelected = isSelected,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = {
                                HapticUtil.tick(context)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onItemClick(item)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernBottomTabItem(
    item: BottomBarItemSpec,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val selectionProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 380f
        ),
        label = "tabSelectionProgress"
    )

    val activeColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.GreenPrimary else AppColors.TextSecondary,
        animationSpec = tween(220),
        label = "tabActiveColor"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 28.dp),
                role = Role.Tab,
                onClick = onClick
            )
            .semantics { selected = isSelected },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                val scale = 0.94f + (0.06f * selectionProgress)
                scaleX = scale
                scaleY = scale
            },
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.icon,
                contentDescription = label,
                tint = activeColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = activeColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Spec data class đại diện cho từng tab trong BottomBar.
 * Tách biệt domain navigation khỏi giao diện hiển thị.
 */
data class BottomBarItemSpec(
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    @param:StringRes val labelRes: Int,
    @Deprecated("Dùng labelRes để đảm bảo đa ngôn ngữ") val label: String = ""
)
