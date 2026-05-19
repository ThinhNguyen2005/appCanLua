package com.GiaThinh.canlua.ui.screen.market

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.GiaThinh.canlua.ui.component.market.MarketSkeletonList
import com.GiaThinh.canlua.ui.component.market.NewsSection
import com.GiaThinh.canlua.ui.component.market.PriceTrendChart
import com.GiaThinh.canlua.ui.component.market.RicePriceCard
import com.GiaThinh.canlua.ui.component.market.WeatherWidget
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.viewmodel.MarketViewModel
import com.GiaThinh.canlua.ui.viewmodel.NewsViewModel
import com.GiaThinh.canlua.ui.viewmodel.WeatherViewModel

/**
 * Module 2 — Bảng Tin Giá Lúa & Thị Trường
 *
 * Phase 2.1: read-only với mock data + Vico chart.
 * UX update: bỏ header trùng (TopBar đã có "Thị Trường"), thay icon Refresh
 * bằng cử chỉ Pull-to-Refresh, và Shimmer skeleton khi load lần đầu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    viewModel: MarketViewModel = hiltViewModel(),
    weatherViewModel: WeatherViewModel = hiltViewModel(),
    newsViewModel: NewsViewModel = hiltViewModel()
) {
    val prices by viewModel.prices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedVariety by viewModel.selectedVariety.collectAsState()
    val timeRangeDays by viewModel.timeRangeDays.collectAsState()
    val history by viewModel.history.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val weatherState by weatherViewModel.state.collectAsState()

    val newsArticles by newsViewModel.articles.collectAsState()
    val newsTopic by newsViewModel.selectedTopic.collectAsState()
    val newsUi by newsViewModel.ui.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) weatherViewModel.onPermissionGranted()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val pullState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        state = pullState,
        isRefreshing = isLoading && prices.isNotEmpty(), // ẩn indicator nếu chưa có data, để Shimmer lo
        onRefresh = {
            viewModel.refresh()
            weatherViewModel.load(forceRefresh = true)
            newsViewModel.refresh()
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Weather widget
            item {
                WeatherWidget(
                    weather = weatherState.weather,
                    isLoading = weatherState.isLoading,
                    errorMessage = weatherState.errorMessage,
                    isStale = weatherState.isStale,
                    onRefresh = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        weatherViewModel.load(forceRefresh = true)
                    }
                )
            }

            // News section — tình hình lúa/gạo/thời tiết từ báo chí
            item {
                NewsSection(
                    articles = newsArticles,
                    selectedTopic = newsTopic,
                    isRefreshing = newsUi.isRefreshing,
                    errorMessage = newsUi.errorMessage,
                    onSelectTopic = newsViewModel::selectTopic,
                    onRefresh = newsViewModel::refresh,
                    onDismissError = newsViewModel::clearError
                )
            }

            // Section title
            item {
                SectionTitle(
                    title = "Bảng giá thu mua",
                    subtitle = "Cập nhật từ thương lái uy tín · ${prices.size} kết quả"
                )
            }

            // Filter chips
            item {
                FilterChipsRow(
                    selectedTrend = filter.trend,
                    onSelectTrend = { viewModel.setTrendFilter(it) }
                )
            }

            // Price cards — Shimmer skeleton khi mạng yếu / lần đầu
            if (isLoading && prices.isEmpty()) {
                item {
                    MarketSkeletonList(items = 4)
                }
            } else {
                items(prices, key = { it.id }) { price ->
                    RicePriceCard(
                        price = price,
                        onClick = {
                            viewModel.selectVariety(price.variety)
                        }
                    )
                }
            }

            // Footer note
            item {
                FooterNote()
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // Chart bottom sheet
    if (selectedVariety != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectVariety(null) },
            sheetState = sheetState,
            containerColor = AppColors.Surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = selectedVariety ?: "",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 16.dp)
                )
                PriceTrendChart(
                    points = history,
                    timeRangeDays = timeRangeDays,
                    onTimeRangeChange = { viewModel.setTimeRange(it) }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextHint
        )
    }
}

@Composable
private fun FooterNote() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "💡 Dữ liệu mang tính tham khảo, được tổng hợp từ thương lái khu vực ĐBSCL. " +
                    "Phiên bản tiếp theo sẽ tích hợp giá real-time và thời tiết theo GPS.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextHint
        )
    }
}

/**
 * Filter chips dùng cho FARMER để lọc theo xu hướng giá.
 * Phase 2.2 — có thể mở rộng thêm region/variety về sau.
 */
@Composable
private fun FilterChipsRow(
    selectedTrend: String?,
    onSelectTrend: (String?) -> Unit
) {
    val options = listOf(
        Triple<String?, String, Color>(null, "Tất cả", AppColors.GreenPrimary),
        Triple<String?, String, Color>("UP", "Đang tăng", AppColors.Success),
        Triple<String?, String, Color>("STABLE", "Ổn định", AppColors.Info),
        Triple<String?, String, Color>("DOWN", "Đang giảm", AppColors.Error)
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(options) { (key, label, color) ->
            val selected = selectedTrend == key
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) color.copy(alpha = 0.18f) else AppColors.SurfaceContainer)
                    .clickable { onSelectTrend(key) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) color else AppColors.TextSecondary
                )
            }
        }
    }
}
