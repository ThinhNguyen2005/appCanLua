package com.GiaThinh.canlua.ui.component.market

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.ui.theme.AppColors

/**
 * Native ad placeholder — mô phỏng bài báo trong NewsSection của MarketScreen.
 *
 * Thiết kế cẩn trọng:
 *  - Style giống news card (rounded 16dp, padding tương tự, headline + summary)
 *    để user không cảm thấy bị "chen" quảng cáo lạ.
 *  - Badge "Tài trợ" góc trên-phải (gold) — minh bạch theo guideline FTC/Google.
 *  - Icon thay cho ảnh thumbnail (chưa load AdMob thật).
 *  - Tap → onClick (caller decide: mở landing page hay deep link).
 *
 * Caller (MarketScreen) tự handle việc ẩn khi user là Premium qua AnimatedVisibility.
 */
@Composable
fun NativeAdPlaceholder(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.CardBg)
            .border(
                width = 1.dp,
                color = AppColors.GoldAccent.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Thumbnail giả lập — hero icon trong gradient gold
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    AppColors.GoldLight,
                                    AppColors.GoldAccent.copy(alpha = 0.4f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = null,
                        tint = AppColors.GoldDark,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.size(12.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Hàng đầu: nguồn + badge "Tài trợ"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Agriculture,
                                contentDescription = null,
                                tint = AppColors.GreenPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = "Phân bón Lúa Vàng",
                                style = MaterialTheme.typography.labelMedium,
                                color = AppColors.GreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppColors.GoldAccent.copy(alpha = 0.18f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Tài trợ",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.GoldDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.size(6.dp))

                    Text(
                        text = "Tăng năng suất lúa 18% với gói NPK chuyên dụng vụ Hè Thu",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        maxLines = 2
                    )
                }
            }

            Spacer(Modifier.size(8.dp))

            Text(
                text = "Ưu đãi cho thành viên Cân Lúa: giảm 12% khi đặt từ 5 bao trở lên. Giao tận ruộng trong 24h.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                maxLines = 3
            )
        }
    }
}
