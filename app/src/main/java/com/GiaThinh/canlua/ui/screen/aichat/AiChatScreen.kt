package com.GiaThinh.canlua.ui.screen.aichat

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
import androidx.compose.material.icons.outlined.SmartToy
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
 * Placeholder "Coming Soon" cho tab AI Chat.
 * Sẽ thay thế bằng chatbot nông nghiệp RAG trong Phase 2.
 */
@Composable
fun AiChatScreen() {
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
                .background(AppColors.GreenSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.SmartToy,
                contentDescription = null,
                tint = AppColors.GreenPrimary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Trợ Lý AI Khuyến Nông",
            style = MaterialTheme.typography.headlineMedium,
            color = AppColors.TextPrimary
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Hỏi đáp về sâu bệnh, thuốc BVTV, lịch gieo sạ, " +
                    "và nhận cảnh báo thời tiết kết hợp AI — sắp ra mắt.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "🚧 Đang phát triển — Phase 2",
            style = MaterialTheme.typography.labelLarge,
            color = AppColors.GreenDark
        )
    }
}
