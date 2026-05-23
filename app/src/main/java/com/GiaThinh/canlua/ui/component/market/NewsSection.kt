package com.GiaThinh.canlua.ui.component.market

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.GiaThinh.canlua.data.model.NewsArticle
import com.GiaThinh.canlua.data.model.NewsTopic
import com.GiaThinh.canlua.ui.component.shimmer
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.util.CustomTabsLauncher
import java.util.concurrent.TimeUnit

/**
 * Section "Tin tức nông nghiệp" hiển thị trên tab Thị Trường.
 *
 * Bao gồm:
 *  - Title row + nút Refresh
 *  - FilterRow chips (Tất cả / Lúa / Gạo / Thời tiết / Thị trường)
 *  - List NewsCard (max 8 bài, tap → Custom Tab)
 *  - Banner lỗi khi network failure (giữ cache cũ)
 *  - Skeleton placeholder khi loading lần đầu
 */
@Composable
fun NewsSection(
    articles: List<NewsArticle>,
    selectedTopic: NewsTopic?,
    isRefreshing: Boolean,
    errorMessage: String?,
    onSelectTopic: (NewsTopic?) -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.CardBg)
            .padding(vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppColors.GreenSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = null,
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tin tức nông nghiệp",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = "Cập nhật từ Google News & các báo VN",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint
                )
            }
            RefreshButton(isRefreshing = isRefreshing, onClick = onRefresh)
        }

        Spacer(Modifier.height(12.dp))

        // Filter chips
        TopicFilterRow(
            selected = selectedTopic,
            onSelect = onSelectTopic
        )

        Spacer(Modifier.height(12.dp))

        // Error banner — vẫn hiện cache phía dưới
        AnimatedVisibility(visible = errorMessage != null) {
            ErrorBanner(message = errorMessage.orEmpty(), onDismiss = onDismissError)
        }

        // Content
        when {
            articles.isEmpty() && isRefreshing -> {
                NewsSkeletonList()
            }
            articles.isEmpty() -> {
                EmptyState()
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    articles.take(20).forEach { article ->
                        NewsCard(article = article)
                    }
                }
            }
        }
    }
}

@Composable
private fun RefreshButton(isRefreshing: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        label = "refreshRotation"
    )
    IconButton(onClick = onClick, enabled = !isRefreshing) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Làm mới",
            tint = AppColors.TextSecondary,
            modifier = Modifier.rotate(rotation)
        )
    }
}

@Composable
private fun TopicFilterRow(
    selected: NewsTopic?,
    onSelect: (NewsTopic?) -> Unit
) {
    val items = listOf<Pair<NewsTopic?, String>>(
        null to "Tất cả",
        NewsTopic.RICE to "Lúa",
        NewsTopic.GRAIN to "Gạo",
        NewsTopic.WEATHER to "Thời tiết",
        NewsTopic.MARKET to "Thị trường"
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { (topic, label) ->
            val isSelected = selected == topic
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) AppColors.GreenPrimary
                        else AppColors.SurfaceContainer
                    )
                    .clickable { onSelect(topic) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else AppColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun NewsCard(article: NewsArticle) {
    val context = LocalContext.current
    val toolbarColor = AppColors.GreenPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.SurfaceContainer)
            .clickable {
                CustomTabsLauncher.open(
                    context = context,
                    url = article.link,
                    toolbarColor = toolbarColor.toArgb()
                )
            }
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.Divider)
        ) {
            if (!article.thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(article.thumbnail)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(86.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = null,
                    tint = AppColors.TextHint,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(2.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (article.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = article.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TopicBadge(topic = article.topic)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${article.source} · ${formatRelativeTime(article.publishedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextHint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TopicBadge(topic: String) {
    val (label, color) = when (topic) {
        NewsTopic.RICE.name -> "🌾 Lúa" to AppColors.Success
        NewsTopic.GRAIN.name -> "🌾 Gạo" to AppColors.GoldAccent
        NewsTopic.WEATHER.name -> "🌧️ Thời tiết" to AppColors.Info
        NewsTopic.MARKET.name -> "📊 Thị trường" to AppColors.Warning
        else -> "📰 Tin" to AppColors.TextHint
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Article,
            contentDescription = null,
            tint = AppColors.TextHint,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Chưa có tin tức",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextSecondary
        )
        Text(
            text = "Kéo xuống để cập nhật, hoặc kiểm tra mạng",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextHint
        )
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.OfflineBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = null,
            tint = AppColors.OfflineText,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Không cập nhật được — đang dùng dữ liệu cũ",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.OfflineText,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Đóng",
                tint = AppColors.OfflineText.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun NewsSkeletonList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(3) { NewsCardSkeleton() }
    }
}

@Composable
private fun NewsCardSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.SurfaceContainer)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.SurfaceContainer)
                .shimmer()
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.SurfaceContainer)
                    .shimmer()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.SurfaceContainer)
                    .shimmer()
            )
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.SurfaceContainer)
                    .shimmer()
            )
        }
    }
}

private fun formatRelativeTime(timestampMs: Long): String {
    val diff = System.currentTimeMillis() - timestampMs
    if (diff < 0) return "vừa xong"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "vừa xong"
        minutes < 60 -> "$minutes phút trước"
        hours < 24 -> "$hours giờ trước"
        days < 7 -> "$days ngày trước"
        days < 30 -> "${days / 7} tuần trước"
        else -> "${days / 30} tháng trước"
    }
}

