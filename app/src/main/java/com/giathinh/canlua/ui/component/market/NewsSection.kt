package com.giathinh.canlua.ui.component.market

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.giathinh.canlua.ui.component.shimmer
import com.giathinh.canlua.util.CustomTabsLauncher
import com.giathinh.canlua.data.model.NewsArticle
import com.giathinh.canlua.data.model.NewsTopic
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.theme.AppDimensions
import java.util.concurrent.TimeUnit


/**
 * Section "Tin tức nông nghiệp" hiển thị trên tab Thị Trường.
 *
 * Bao gồm:
 *  - Title row + nút Refresh
 *  - FilterRow chips (Tất cả / Lúa / Gạo / Thị trường)
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
    var isExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 24.dp))
            .background(AppColors.CardBg)
            .padding(vertical = 16.dp)
    ) {

        // Filter chips (được nuốt trọn vào trong Card tổng, nền CardBg)
        TopicFilterRow(
            selected = selectedTopic,
            onSelect = onSelectTopic
        )

        Spacer(Modifier.height(16.dp))

        // Error banner — hiển thị bên dưới dải bộ lọc trong Card tổng
        AnimatedVisibility(visible = errorMessage != null) {
            ErrorBanner(message = errorMessage.orEmpty(), onDismiss = onDismissError)
        }

        // Content
        when {
            articles.isEmpty() && isRefreshing -> {
                repeat(10) { index ->
                    NewsCardSkeleton()
                    if (index < 9) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(0.5.dp)
                                .background(AppColors.Divider)
                        )
                    }
                }
            }
            articles.isEmpty() -> {
                EmptyState()
            }
            else -> {
                val displayCount = if (isExpanded) 20 else 10
                articles.take(displayCount).forEachIndexed { index, article ->
                    NewsCard(article = article)
                    if (index < displayCount - 1 && index < articles.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(0.5.dp)
                                .background(AppColors.Divider)
                        )
                    }
                }

                if (articles.size > 10) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(AppDimensions.CornerRadiusMd))
                            .background(AppColors.SurfaceContainer)
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isExpanded) "Thu gọn" else "Xem thêm bài viết (${articles.size - 10} bài khác)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.GreenPrimary
                        )
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
        NewsTopic.MARKET to "Thị trường"
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.first?.name ?: "_all" }) { (topic, label) ->
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

private fun getSourceLogoUrl(source: String): String? {
    val clean = source.lowercase().trim()
    return when {
        clean.contains("vnexpress") -> "https://upload.wikimedia.org/wikipedia/commons/e/e3/Logo_VnExpress.png"
        clean.contains("tuổi trẻ") || clean.contains("tuoi tre") -> "https://upload.wikimedia.org/wikipedia/commons/e/ea/Logo_B%C3%A1o_Tu%E1%BB%95i_Tr%E1%BA%BB.png"
        clean.contains("thanh niên") || clean.contains("thanh nien") -> "https://upload.wikimedia.org/wikipedia/commons/7/77/Logo-bao-thanh-nien.png"
        clean.contains("cafef") -> "https://cafefcdn.com/web_images/logo.png"
        clean.contains("vov") -> "https://vov.vn/sites/default/files/logo_vov_red.png"
        clean.contains("dân việt") || clean.contains("dan viet") -> "https://image.vietnamfinance.vn/2018/11/24/dan-viet.png"
        else -> null
    }
}

@Composable
private fun NewsCard(article: NewsArticle) {
    val context = LocalContext.current
    val toolbarColor = AppColors.GreenPrimary
    var isImageError by remember { mutableStateOf(false) }
    val imageUrl = article.thumbnail.takeIf { !it.isNullOrBlank() } ?: getSourceLogoUrl(article.source)
    val showImage = imageUrl != null && !isImageError

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                CustomTabsLauncher.open(
                    context = context,
                    url = article.link,
                    toolbarColor = toolbarColor.toArgb()
                )
            }
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (showImage) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(AppDimensions.CornerRadiusSm))
                    .background(AppColors.SurfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp),
                    onError = { isImageError = true }
                )
            }
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
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
                Text(
                    text = getTopicDisplayName(article.topic),
                    style = MaterialTheme.typography.labelSmall,
                    color = getTopicColor(article.topic),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextHint
                )
                Text(
                    text = article.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = " • ${formatRelativeTime(article.publishedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextHint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun getTopicDisplayName(topic: String): String {
    return when (topic) {
        NewsTopic.RICE.name -> "Lúa"
        NewsTopic.GRAIN.name -> "Gạo"
        NewsTopic.MARKET.name -> "Thị trường"
        else -> "Tin tức"
    }
}

@Composable
private fun getTopicColor(topic: String): Color {
    return when (topic) {
        NewsTopic.RICE.name -> AppColors.Success
        NewsTopic.GRAIN.name -> AppColors.GoldAccent
        NewsTopic.MARKET.name -> AppColors.Warning
        else -> AppColors.TextSecondary
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
private fun NewsCardSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(AppDimensions.CornerRadiusSm))
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
                    .fillMaxWidth(0.4f)
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

