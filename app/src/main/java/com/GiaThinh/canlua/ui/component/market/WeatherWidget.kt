package com.GiaThinh.canlua.ui.component.market

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.GiaThinh.canlua.data.model.WeatherInfo

/**
 * Phase 2.3 — Weather Widget premium.
 *
 * Layout 2 vùng:
 *  - Top row: animated icon + nhiệt độ lớn + tên thành phố
 *  - Bottom strip: 3 stat chips (Ẩm / Gió / Mưa)
 *  - Advisory banner (nếu có) ở dưới cùng
 */
@Composable
fun WeatherWidget(
    weather: WeatherInfo?,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    isStale: Boolean = false,
    modifier: Modifier = Modifier
) {
    val gradient = pickGradient(weather)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(gradient))
                .clickable(onClick = onRefresh)
        ) {
            // Decorative blob trên góc phải để add depth
            DecorativeGlow()

            Column(modifier = Modifier.padding(20.dp)) {
                when {
                    isLoading && weather == null -> LoadingRow()
                    errorMessage != null && weather == null -> ErrorRow(errorMessage)
                    weather != null -> WeatherContent(weather, isLoading, isStale)
                    else -> ErrorRow("Chưa có dữ liệu thời tiết")
                }
            }
        }
    }
}

@Composable
private fun DecorativeGlow() {
    Box(
        modifier = Modifier
            .padding(start = 220.dp)
            .size(180.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
    )
}

@Composable
private fun WeatherContent(weather: WeatherInfo, isRefreshing: Boolean, isStale: Boolean) {
    Column {
        // === Top row: location + freshness ===
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = weather.location,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            FreshnessBadge(weather.updatedAt, isRefreshing, isStale)
        }

        Spacer(Modifier.height(14.dp))

        // === Hero row: animated icon + temperature + condition ===
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedWeatherIcon(weather.iconKey)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "${weather.temperature}",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 60.sp
                    )
                    Text(
                        text = "°C",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Text(
                    text = weather.condition,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Cảm giác ${weather.feelsLike}°",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // === Stat chips row ===
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.WaterDrop,
                label = "Độ ẩm",
                value = "${weather.humidity}%"
            )
            StatChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Air,
                label = "Gió",
                value = "${"%.1f".format(weather.windSpeed)} m/s"
            )
            StatChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Umbrella,
                label = "Mây phủ",
                value = "${weather.rainChance}%"
            )
        }

        // === Advisory banner ===
        AnimatedVisibility(
            visible = weather.advisory != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Thermostat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = weather.advisory.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun FreshnessBadge(updatedAt: Long, isRefreshing: Boolean, isStale: Boolean) {
    val text = if (isRefreshing) "Đang cập nhật..."
        else if (isStale) formatAge(updatedAt) else "Vừa xong"
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 1.5.dp,
                modifier = Modifier.size(12.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AnimatedWeatherIcon(iconKey: String) {
    val transition = rememberInfiniteTransition(label = "weather-icon")
    val isSunny = iconKey.startsWith("01")
    val isRain = iconKey.startsWith("09") || iconKey.startsWith("10")

    // Sun rotates slowly, rain bobs up-down via alpha pulse
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isSunny) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = if (isRain) 1.0f else 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f * pulse)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconFor(iconKey),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(46.dp)
                .rotate(rotation)
        )
    }
}

@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 2.dp,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = "Đang tải thời tiết...",
            color = Color.White,
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
            Icons.Filled.Refresh,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Không tải được thời tiết",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun iconFor(iconKey: String): ImageVector {
    return when {
        iconKey.startsWith("01") -> Icons.Filled.WbSunny
        iconKey.startsWith("02") || iconKey.startsWith("03") || iconKey.startsWith("04") -> Icons.Filled.Cloud
        iconKey.startsWith("09") || iconKey.startsWith("10") -> Icons.Filled.WaterDrop
        iconKey.startsWith("11") -> Icons.Filled.Thunderstorm
        iconKey.startsWith("13") -> Icons.Filled.Grain
        iconKey.endsWith("n") -> Icons.Filled.NightsStay
        else -> Icons.Filled.WbSunny
    }
}

private fun pickGradient(weather: WeatherInfo?): List<Color> {
    val main = weather?.iconKey.orEmpty()
    return when {
        // Nắng — cam-vàng-xanh dịu
        main.startsWith("01") -> listOf(
            Color(0xFFFF8E53), Color(0xFFFE6B8B), Color(0xFF1976D2)
        )
        // Mây — xanh xám sang trọng
        main.startsWith("02") || main.startsWith("03") || main.startsWith("04") -> listOf(
            Color(0xFF455A64), Color(0xFF607D8B), Color(0xFF90A4AE)
        )
        // Mưa — xanh lam đậm
        main.startsWith("09") || main.startsWith("10") -> listOf(
            Color(0xFF1A237E), Color(0xFF3949AB), Color(0xFF5C6BC0)
        )
        // Sấm sét — tím-xám
        main.startsWith("11") -> listOf(
            Color(0xFF263238), Color(0xFF512DA8), Color(0xFF7E57C2)
        )
        // Đêm — tím navy
        main.endsWith("n") -> listOf(
            Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)
        )
        else -> listOf(
            Color(0xFF1E88E5), Color(0xFF42A5F5), Color(0xFFFFB74D)
        )
    }
}

private fun formatAge(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val mins = diff / 60_000
    val hours = diff / 3_600_000
    return when {
        mins < 1 -> "Vừa xong"
        mins < 60 -> "$mins phút trước"
        hours < 24 -> "$hours giờ trước"
        else -> "Đã lưu"
    }
}
