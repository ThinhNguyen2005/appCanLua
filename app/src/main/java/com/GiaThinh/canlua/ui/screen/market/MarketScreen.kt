package com.GiaThinh.canlua.ui.screen.market

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.ui.theme.AppColors

/**
 * Placeholder "Coming Soon" cho tab Thị Trường.
 * Sẽ thay thế bằng bảng giá lúa + thời tiết trong Phase 2.
 */
@Composable
fun MarketScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(AppColors.GoldLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.TrendingUp,
                contentDescription = null,
                tint = AppColors.GoldAccent,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Thị Trường Lúa",
            style = MaterialTheme.typography.headlineMedium,
            color = AppColors.TextPrimary
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Bảng giá lúa thu mua theo giống, biểu đồ xu hướng giá, " +
                    "và dự báo thời tiết khu vực của bạn — sắp ra mắt trong phiên bản tiếp theo.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "🚧 Đang phát triển — Phase 2",
            style = MaterialTheme.typography.labelLarge,
            color = AppColors.GoldDark
        )
    }
}
