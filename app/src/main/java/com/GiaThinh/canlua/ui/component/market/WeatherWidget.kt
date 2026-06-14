package com.GiaThinh.canlua.ui.component.market

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.GiaThinh.canlua.data.model.WeatherInfo
import com.GiaThinh.canlua.ui.theme.AppColors

/**
 * WeatherWidget tối giản theo phong cách Material 3 (Gmail Style).
 * Loại bỏ gradient sặc sỡ, vòng tròn trang trí và đổ bóng rườm rà.
 * Tập trung hiển thị thông tin dạng phẳng trên nền CardBg của app.
 */
@Composable
fun WeatherWidget(
    weather: WeatherInfo?,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    hasPermission: Boolean = true,
    isStale: Boolean = false, // Giữ nguyên signature để tránh lỗi compile ở gọi hàm
    containerColor: Color = AppColors.CardBg,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 24.dp))
            .background(containerColor)
            .clickable(onClick = onRefresh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when {
                !hasPermission -> NoPermissionRow(onRefresh)
                isLoading && weather == null -> LoadingRow()
                errorMessage != null && weather == null -> ErrorRow(errorMessage)
                weather != null -> WeatherContent(weather)
                else -> ErrorRow("Chưa có dữ liệu thời tiết")
            }
        }
    }
}

@Composable
private fun NoPermissionRow(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Thời tiết theo định vị",
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Cấp quyền vị trí để cập nhật thời tiết nơi bạn đang ở",
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.SurfaceContainer)
                .clickable(onClick = onRefresh)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Cấp quyền",
                color = AppColors.GreenPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun WeatherContent(weather: WeatherInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Căn lề Trái: Tên vị trí (dòng trên) & Nhiệt độ lớn + Trạng thái viết hoa chữ cái đầu (dòng dưới)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = weather.location,
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.TextSecondary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${weather.temperature}°C",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        lineHeight = 34.sp
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextHint
                    )
                    val capitalizedCondition = remember(weather.condition) {
                        weather.condition.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
                        }
                    }
                    Text(
                        text = capitalizedCondition,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }

            // Căn lề Phải: Weather Icon dạng nét mảnh (Outline) kích thước 44.dp
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppColors.SurfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconFor(weather.iconKey),
                    contentDescription = null,
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 3 Chỉ số phụ (Ẩm, Gió, Mây) hiển thị phẳng 1 dòng: Nhãn (Regular xám) & Số liệu (Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Độ ẩm: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextHint
                )
                Text(
                    text = "${weather.humidity}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }

            Row(
                modifier = Modifier.weight(1.1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gió: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextHint
                )
                Text(
                    text = "${"%.1f".format(weather.windSpeed)} m/s",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }

            Row(
                modifier = Modifier.weight(0.9f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mây: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextHint
                )
                Text(
                    text = "${weather.rainChance}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }
        }

        // Advisory Banner (chỉ hiển thị nếu có khuyến nghị thời tiết)
        AnimatedVisibility(
            visible = weather.advisory != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.SurfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Thermostat,
                    contentDescription = null,
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = weather.advisory.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            color = AppColors.GreenPrimary,
            strokeWidth = 2.dp,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = "Đang tải thời tiết...",
            color = AppColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ErrorRow(message: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Không tải được thời tiết",
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                color = AppColors.TextHint,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun iconFor(iconKey: String): ImageVector {
    return when {
        iconKey.startsWith("01") -> Icons.Outlined.WbSunny
        iconKey.startsWith("02") || iconKey.startsWith("03") || iconKey.startsWith("04") -> Icons.Outlined.Cloud
        iconKey.startsWith("09") || iconKey.startsWith("10") -> Icons.Outlined.WaterDrop
        iconKey.startsWith("11") -> Icons.Outlined.Thunderstorm
        iconKey.startsWith("13") -> Icons.Outlined.Grain
        iconKey.endsWith("n") -> Icons.Outlined.NightsStay
        else -> Icons.Outlined.WbSunny
    }
}
