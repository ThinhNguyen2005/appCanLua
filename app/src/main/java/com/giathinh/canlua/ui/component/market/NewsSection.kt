package com.giathinh.canlua.ui.component.market

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.giathinh.canlua.R
import com.giathinh.canlua.util.HapticUtil
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
    val context = LocalContext.current
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

        // Error banner — hiển thị bên dưới dải bộ lọc trong Card tổng với animation lò xo mượt mà
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                    slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { -it / 2 },
            exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
                    slideOutVertically(spring(stiffness = Spring.StiffnessMediumLow)) { -it / 2 }
        ) {
            ErrorBanner(message = errorMessage.orEmpty(), onDismiss = onDismissError)
        }

        // Content with smooth Crossfade transition
        Crossfade(
            targetState = articles.isEmpty() && isRefreshing,
            label = "news_content_fade"
        ) { loading ->
            if (loading) {
                Column {
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
            } else {
                if (articles.isEmpty()) {
                    EmptyState(onRefresh = onRefresh)
                } else {
                    Column {
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
                                    .clickable {
                                        HapticUtil.tick(context)
                                        isExpanded = !isExpanded
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isExpanded) {
                                        stringResource(R.string.news_collapse)
                                    } else {
                                        stringResource(R.string.news_expand_more, articles.size - 10)
                                    },
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
    }
}

@Composable
private fun RefreshButton(isRefreshing: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val rotation by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "refreshRotation"
    )
    IconButton(
        onClick = {
            HapticUtil.tick(context)
            onClick()
        },
        enabled = !isRefreshing,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = stringResource(R.string.news_refresh_action),
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
    val context = LocalContext.current
    val items = listOf(
        null to stringResource(R.string.news_topic_all),
        NewsTopic.RICE to stringResource(R.string.news_topic_rice),
        NewsTopic.GRAIN to stringResource(R.string.news_topic_grain),
        NewsTopic.MARKET to stringResource(R.string.news_topic_market)
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.first?.name ?: "_all" }) { (topic, label) ->
            val isSelected = selected == topic
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) AppColors.GreenPrimary else AppColors.SurfaceContainer,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "topic_chip_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else AppColors.TextSecondary,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "topic_chip_text"
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .clickable {
                        HapticUtil.tick(context)
                        onSelect(topic)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
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
                HapticUtil.tick(context)
                CustomTabsLauncher.open(
                    context = context,
                    url = article.link,
                    toolbarColor = toolbarColor.toArgb()
                )
            }
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(AppDimensions.CornerRadiusSm))
                .background(AppColors.SurfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            if (showImage) {
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
            } else {
                com.giathinh.canlua.ui.screen.market.GoogleNewsPlaceholder(modifier = Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.width(12.dp))

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

@Composable
private fun getTopicDisplayName(topic: String): String {
    return when (topic) {
        NewsTopic.RICE.name -> stringResource(R.string.news_topic_rice)
        NewsTopic.GRAIN.name -> stringResource(R.string.news_topic_grain)
        NewsTopic.MARKET.name -> stringResource(R.string.news_topic_market)
        else -> stringResource(R.string.news_topic_default)
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
private fun EmptyState(onRefresh: () -> Unit) {
    val context = LocalContext.current
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
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.news_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.news_empty_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                HapticUtil.tick(context)
                onRefresh()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.GreenPrimary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.news_refresh_action),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.OfflineBg)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = null,
            tint = AppColors.OfflineText,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.news_error_cached),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.OfflineText,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )
        IconButton(
            onClick = {
                HapticUtil.tick(context)
                onDismiss()
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.news_close_action),
                tint = AppColors.OfflineText.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
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

@Composable
private fun formatRelativeTime(timestampMs: Long): String {
    val diff = System.currentTimeMillis() - timestampMs
    if (diff < 0) return stringResource(R.string.news_time_just_now)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> stringResource(R.string.news_time_just_now)
        minutes < 60 -> stringResource(R.string.news_time_minutes_ago, minutes)
        hours < 24 -> stringResource(R.string.news_time_hours_ago, hours)
        days < 7 -> stringResource(R.string.news_time_days_ago, days)
        days < 30 -> stringResource(R.string.news_time_weeks_ago, days / 7)
        else -> stringResource(R.string.news_time_months_ago, days / 30)
    }
}

