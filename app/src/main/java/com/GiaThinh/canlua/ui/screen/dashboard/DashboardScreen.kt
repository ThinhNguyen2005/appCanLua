package com.GiaThinh.canlua.ui.screen.dashboard

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
import androidx.compose.material.icons.outlined.BarChart
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
 * Placeholder "Coming Soon" cho tab Dashboard / Thống Kê.
 * Sẽ thay thế bằng biểu đồ sản lượng + AI chẩn đoán trong Phase 2-3.
 */
@Composable
fun DashboardScreen() {
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
                .background(AppColors.ComingSoonBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.BarChart,
                contentDescription = null,
                tint = AppColors.ComingSoonText,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Thống Kê Mùa Vụ",
            style = MaterialTheme.typography.headlineMedium,
            color = AppColors.TextPrimary
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Biểu đồ tròn, cột, đường cho sản lượng lúa, " +
                    "so sánh vụ mùa, và AI phân tích nguyên nhân trúng/thất — sắp ra mắt.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "🚧 Đang phát triển — Phase 2–3",
            style = MaterialTheme.typography.labelLarge,
            color = AppColors.ComingSoonText
        )
    }
}
