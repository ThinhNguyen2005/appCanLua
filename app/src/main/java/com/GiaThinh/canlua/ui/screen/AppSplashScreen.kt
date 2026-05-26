package com.GiaThinh.canlua.ui.screen

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.R
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource

private val SplashGreen = Color(0xFF2E7D32)
private val SplashWhite = Color.White
private val SplashWhiteDim = Color(0xB3FFFFFF) // 70% white
private val SplashGold = Color(0xFFFFB300) // amber-700: đậm hơn FFC107, tương phản tốt trên nền xanh

/**
 * Màn hình chờ khởi động — hiển thị trong khi [InitViewModel] đang nạp dữ liệu
 * và [AuthViewModel] đang xác định trạng thái đăng nhập.
 *
 * Composable này chỉ hiển thị giao diện; điều hướng do [MainActivity] xử lý
 * thông qua LaunchedEffect theo dõi trạng thái auth + isDataReady.
 */
@Composable
fun AppSplashScreen() {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashGreen),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_subject),
                contentDescription = null,
                colorFilter = ColorFilter.tint(SplashGold, BlendMode.SrcIn),
                modifier = Modifier
                    .size(108.dp)
                    .alpha(pulse)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = SplashWhite
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Quản lý cân lúa thông minh",
                style = MaterialTheme.typography.bodyMedium,
                color = SplashWhiteDim
            )

            Spacer(Modifier.height(40.dp))

            CircularProgressIndicator(
                color = SplashWhite,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Đang tải dữ liệu…",
                style = MaterialTheme.typography.bodySmall,
                color = SplashWhiteDim
            )
        }
    }
}
