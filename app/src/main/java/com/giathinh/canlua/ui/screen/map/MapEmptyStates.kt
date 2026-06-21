package com.giathinh.canlua.ui.screen.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.ui.theme.AppColors

/**
 * Hint overlay khi map không có điểm nào hiển thị: 3 trạng thái nguyên nhân:
 *  - permission denied → hướng user vào Settings cấp quyền.
 *  - filter quá tight → gợi ý bỏ filter.
 *  - chưa có thẻ nào có GPS → giải thích flow tạo thẻ ngoài đồng.
 */
@Composable
internal fun EmptyMapHint(
    permissionGranted: Boolean,
    hasFilters: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.CardBg.copy(alpha = 0.95f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (permissionGranted) Icons.Filled.Map else Icons.Filled.LocationOff,
                contentDescription = null,
                tint = AppColors.GoldAccent,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        !permissionGranted -> "Cần quyền vị trí để hiển thị bản đồ"
                        hasFilters -> "Không có điểm nào khớp bộ lọc"
                        else -> "Chưa có thẻ cân nào có toạ độ GPS"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        !permissionGranted -> "Cấp quyền vị trí trong Cài đặt rồi quay lại."
                        hasFilters -> "Thử bỏ bớt bộ lọc hoặc đổi từ khoá tìm kiếm."
                        else -> "Tạo thẻ cân mới ngoài đồng — toạ độ sẽ tự ghi nhận."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

/**
 * Placeholder full-screen khi `MAPS_API_KEY` chưa được set trong build config.
 * Tránh crash GoogleMap composable khi developer chưa cấu hình môi trường.
 */
@Composable
internal fun MapComingSoonPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        AppColors.GreenPrimary.copy(alpha = 0.08f),
                        AppColors.Surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(AppColors.GreenPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Map, null, tint = AppColors.GreenPrimary, modifier = Modifier.size(48.dp))
            }
            Text(
                "Bản đồ thu mua",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                "Tính năng đang trong giai đoạn phát triển.\nSẽ sớm ra mắt trong bản cập nhật tiếp theo.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppColors.GreenPrimary.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "🚧  Sắp ra mắt",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.GreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
