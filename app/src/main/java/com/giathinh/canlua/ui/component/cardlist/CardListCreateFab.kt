package com.giathinh.canlua.ui.component.cardlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.R
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.theme.AppDimensions

/**
 * FAB Tạo phiếu cân cho CardListScreen.
 * Có hiệu ứng Spring slide-in/out tự động đồng bộ theo hướng cuộn danh sách (isScrollingUp).
 */
@Composable
fun CardListCreateFab(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Extended FAB có animation spring slide-in/out tự động theo hướng cuộn.
    // ContentDescription đầy đủ hỗ trợ tiếp cận cho người khiếm thị qua TalkBack.
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            initialOffsetY = { it / 2 }
        ),
        exit = fadeOut(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + slideOutVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            targetOffsetY = { it / 2 }
        ),
        modifier = modifier
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            containerColor = AppColors.GreenPrimary,
            contentColor = Color.White,
            shape = RoundedCornerShape(AppDimensions.CornerRadiusLg),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            ),
            icon = {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.card_list_create_card_action)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.card_list_create_card_action),
                    fontWeight = FontWeight.Bold
                )
            }
        )
    }
}
