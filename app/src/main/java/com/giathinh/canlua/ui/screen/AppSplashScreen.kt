package com.giathinh.canlua.ui.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.R
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.theme.AppDimensions

/**
 * Màn hình chờ chuyển tiếp — chỉ hiển thị ngắn (~100-300ms) ngay sau khi người dùng
 * vừa đăng nhập ở [AuthScreen] và đang chờ kiểm tra trạng thái hồ sơ.
 *
 * Khi khởi động lạnh (cold start), Native Splash Screen của Android đã giữ màn hình
 * cho tới khi Room DB và Auth sẵn sàng nên không bị chớp 2 lần.
 */
@Composable
fun AppSplashScreen() {
    val pulse by rememberInfiniteTransition(label = "splash_pulse").animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AppColors.GreenSurface,
                        AppColors.Surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = AppDimensions.SpacingXxl)
        ) {
            Surface(
                shape = RoundedCornerShape(AppDimensions.CornerRadiusXl),
                color = AppColors.CardBg,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, AppColors.GreenPrimary.copy(alpha = 0.16f)),
                modifier = Modifier.alpha(pulse)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_subject),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(AppDimensions.SpacingMd)
                        .size(72.dp)
                )
            }

            Spacer(Modifier.height(AppDimensions.SpacingMd))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(AppDimensions.SpacingXs))

            Text(
                text = stringResource(R.string.splash_slogan),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(AppDimensions.SpacingXxl))

            CircularProgressIndicator(
                color = AppColors.GreenPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(AppDimensions.SpacingXxl)
            )

            Spacer(Modifier.height(AppDimensions.SpacingSm))

            Text(
                text = stringResource(R.string.splash_loading),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextHint,
                textAlign = TextAlign.Center
            )
        }
    }
}