package com.GiaThinh.canlua.ui.screen.market

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.outlined.PriceChange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.GiaThinh.canlua.data.firestore.FirestoreRicePrice
import com.GiaThinh.canlua.ui.component.market.BidEditorSheet
import com.GiaThinh.canlua.ui.component.market.MarketSkeletonList
import com.GiaThinh.canlua.ui.component.market.NativeAdPlaceholder
import com.GiaThinh.canlua.ui.component.market.NewsSection
import com.GiaThinh.canlua.ui.component.market.PriceTrendChart
import com.GiaThinh.canlua.ui.component.market.RicePriceCard
import com.GiaThinh.canlua.ui.component.market.WeatherWidget
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.util.isScrollingUp
import com.GiaThinh.canlua.ui.viewmodel.MarketViewModel
import com.GiaThinh.canlua.ui.viewmodel.NewsViewModel
import com.GiaThinh.canlua.ui.viewmodel.ProfileViewModel
import com.GiaThinh.canlua.ui.viewmodel.TraderBidsViewModel
import com.GiaThinh.canlua.ui.viewmodel.WeatherViewModel
import com.GiaThinh.canlua.util.PremiumState
import com.GiaThinh.canlua.util.TrackScreenRender
import kotlinx.coroutines.launch

/**
 * Module 2 — Bảng Tin Giá Lúa & Thị Trường
 *
 * Layout: TabRow + HorizontalPager 2 trang.
 *  - Page 0 "Tin tức": Weather widget + NativeAd + NewsSection (cuộn chung trong LazyColumn)
 *  - Page 1 "Bảng giá lúa": Giá rao của bạn (nếu trader) + Bảng giá thu mua
 *
 * Weather card chỉ thuộc tab Tin tức — cuộn cùng news, không sticky, không hiện ở
 * Prices tab hay tab khác. Tab và pager đồng bộ 2 chiều.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MarketScreen(
    viewModel: MarketViewModel = hiltViewModel(),
    weatherViewModel: WeatherViewModel = hiltViewModel(),
    newsViewModel: NewsViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    bidsViewModel: TraderBidsViewModel = hiltViewModel()
) {
    TrackScreenRender("market")
    val prices by viewModel.prices.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectedVariety by viewModel.selectedVariety.collectAsStateWithLifecycle()
    val timeRangeDays by viewModel.timeRangeDays.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val weatherState by weatherViewModel.state.collectAsStateWithLifecycle()

    val newsArticles by newsViewModel.articles.collectAsStateWithLifecycle()
    val newsTopic by newsViewModel.selectedTopic.collectAsStateWithLifecycle()
    val newsUi by newsViewModel.ui.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) weatherViewModel.onPermissionGranted()
    }

    val profile by profileViewModel.profile.collectAsStateWithLifecycle(initialValue = null)
    val isTrader = profile?.role == "TRADER"
    val isPremium by PremiumState.isPremium.collectAsStateWithLifecycle()
    val myBids by bidsViewModel.myBids.collectAsStateWithLifecycle()
    val bidUiState by bidsViewModel.uiState.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val newsListState = rememberLazyListState()
    val pricesListState = rememberLazyListState()

    // FAB chỉ hiện ở tab "Bảng giá lúa" — nơi thương lái thực sự đăng giá.
    val fabExpanded = pricesListState.isScrollingUp() || myBids.isEmpty()

    var editingBid by remember { mutableStateOf<FirestoreRicePrice?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    LaunchedEffect(bidUiState.successMessage, bidUiState.errorMessage) {
        bidUiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            bidsViewModel.clearMessage()
            showEditor = false
            editingBid = null
        }
        bidUiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            bidsViewModel.clearMessage()
        }
    }

    // Tải dữ liệu trì hoãn sau khi slide transition (300ms) kết thúc để tránh lag chuyển trang
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(400)
        viewModel.seedIfNeededDeferred()
        weatherViewModel.startObserving()
        newsViewModel.loadData()
        bidsViewModel.syncBids()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (isTrader && pagerState.currentPage == 1) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingBid = null
                        showEditor = true
                    },
                    expanded = fabExpanded,
                    icon = {
                        Icon(Icons.Filled.Add, contentDescription = "Đăng giá mới")
                    },
                    text = { Text("Đăng giá mới", fontWeight = FontWeight.SemiBold) },
                    containerColor = AppColors.GreenPrimary,
                    contentColor = AppColors.CardBg,
                    modifier = Modifier.padding(bottom = 80.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Weather không cố định trên đỉnh — đã chiếm gần nửa màn hình nếu sticky.
            // Đưa vào item đầu của LazyColumn NewsPage → cuộn 1 chút là tự ẩn, chỉ TabRow ở lại.
            // TabRow fixed phía trên pager để user luôn nhảy được tab kể cả đang ở cuối list.
            MarketTabRow(
                selectedTab = pagerState.currentPage,
                onSelect = { tab ->
                    scope.launch { pagerState.animateScrollToPage(tab) }
                }
            )

            // Remember stable callbacks to prevent child recomposition on parent state change
            val onWeatherRefresh = remember {
                {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                    weatherViewModel.load(forceRefresh = true)
                }
            }
            val onNewsRefresh = remember { { newsViewModel.refresh() } }
            val onRefreshNewsPage = remember {
                {
                    newsViewModel.refresh()
                    weatherViewModel.load(forceRefresh = true)
                }
            }
            val onSelectTopic = remember { newsViewModel::selectTopic }
            val onDismissError = remember { newsViewModel::clearError }
            val onRefreshPrices = remember { { viewModel.refreshFromFirestore() } }
            val onSelectTrend = remember { viewModel::setTrendFilter }
            val onSelectVariety = remember { viewModel::selectVariety }
            val onEditBid = remember {
                { bid: FirestoreRicePrice ->
                    editingBid = bid
                    showEditor = true
                }
            }
            val onDeleteBid = remember { { id: String -> bidsViewModel.deleteBid(id) } }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 0
            ) { page ->
                when (page) {
                    0 -> NewsPage(
                        listState = newsListState,
                        articles = newsArticles,
                        selectedTopic = newsTopic,
                        isRefreshing = newsUi.isRefreshing,
                        errorMessage = newsUi.errorMessage,
                        isPremium = isPremium,
                        weatherState = weatherState,
                        onWeatherRefresh = onWeatherRefresh,
                        onSelectTopic = onSelectTopic,
                        onRefresh = onRefreshNewsPage,
                        onNewsRefresh = onNewsRefresh,
                        onDismissError = onDismissError
                    )
                    else -> PricesPage(
                        listState = pricesListState,
                        prices = prices,
                        isLoading = isLoading,
                        filter = filter,
                        isTrader = isTrader,
                        myBids = myBids,
                        onRefresh = onRefreshPrices,
                        onSelectTrend = onSelectTrend,
                        onSelectVariety = onSelectVariety,
                        onEditBid = onEditBid,
                        onDeleteBid = onDeleteBid
                    )
                }
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

    if (showEditor && isTrader) {
        ModalBottomSheet(
            onDismissRequest = { 
                showEditor = false 
                editingBid = null
            },
            sheetState = editorSheetState,
            containerColor = AppColors.Surface
        ) {
            BidEditorSheet(
                existing = editingBid,
                isSaving = bidUiState.isSaving,
                onSubmit = { variety, pMin, pMax, region, trend, note, existingId ->
                    bidsViewModel.submitBid(variety, pMin, pMax, region, trend, note, existingId)
                },
                onDismiss = { 
                    showEditor = false 
                    editingBid = null
                }
            )
        }
    }
}

/**
 * Page 0 — Tin tức nông nghiệp (+ Weather card + Native Ad nếu user Free).
 * PullToRefreshBox riêng → kéo xuống refresh news + weather, không động đến market.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewsPage(
    listState: LazyListState,
    articles: List<com.GiaThinh.canlua.data.model.NewsArticle>,
    selectedTopic: com.GiaThinh.canlua.data.model.NewsTopic?,
    isRefreshing: Boolean,
    errorMessage: String?,
    isPremium: Boolean,
    weatherState: com.GiaThinh.canlua.ui.viewmodel.WeatherUiState,
    onWeatherRefresh: () -> Unit,
    onSelectTopic: (com.GiaThinh.canlua.data.model.NewsTopic?) -> Unit,
    onRefresh: () -> Unit,
    onNewsRefresh: () -> Unit,
    onDismissError: () -> Unit
) {
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        state = pullState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 80.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "weather") {
                AnimatedVisibility(
                    visible = weatherState.hasPermission && !weatherState.isRateLimited && weatherState.weather != null,
                    enter = fadeIn(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    WeatherWidget(
                        weather = weatherState.weather,
                        isLoading = weatherState.isLoading,
                        errorMessage = weatherState.errorMessage,
                        isStale = weatherState.isStale,
                        onRefresh = onWeatherRefresh
                    )
                }
            }

            item(key = "native_ad") {
                AnimatedVisibility(
                    visible = !isPremium,
                    enter = fadeIn(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    NativeAdPlaceholder(onClick = { /* TODO: deep link landing page */ })
                }
            }

            item {
                NewsSection(
                    articles = articles,
                    selectedTopic = selectedTopic,
                    isRefreshing = isRefreshing,
                    errorMessage = errorMessage,
                    onSelectTopic = onSelectTopic,
                    onRefresh = onNewsRefresh,
                    onDismissError = onDismissError
                )
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

/**
 * Page 1 — Bảng giá lúa: bids của trader (nếu có) + bảng giá thu mua chung.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PricesPage(
    listState: LazyListState,
    prices: List<com.GiaThinh.canlua.data.model.RicePrice>,
    isLoading: Boolean,
    filter: com.GiaThinh.canlua.ui.viewmodel.MarketFilter,
    isTrader: Boolean,
    myBids: List<FirestoreRicePrice>,
    onRefresh: () -> Unit,
    onSelectTrend: (String?) -> Unit,
    onSelectVariety: (String?) -> Unit,
    onEditBid: (FirestoreRicePrice) -> Unit,
    onDeleteBid: (String) -> Unit
) {
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        state = pullState,
        isRefreshing = isLoading && prices.isNotEmpty(),
        onRefresh = onRefresh
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 80.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isTrader && myBids.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = "Giá rao của bạn",
                        subtitle = "${myBids.size} tin đang đăng · nhấn để chỉnh sửa"
                    )
                }
                items(myBids, key = { "mine_${it.id}" }) { bid ->
                    MyBidCard(
                        bid = bid,
                        onEdit = { onEditBid(bid) },
                        onDelete = { onDeleteBid(bid.id) }
                    )
                }
            }

            item {
                SectionTitle(
                    title = "Bảng giá thu mua",
                    subtitle = "Cập nhật từ thương lái uy tín · ${prices.size} kết quả"
                )
            }

            item {
                FilterChipsRow(
                    selectedTrend = filter.trend,
                    onSelectTrend = onSelectTrend
                )
            }

            if (isLoading && prices.isEmpty()) {
                item { MarketSkeletonList(items = 4) }
            } else {
                items(prices, key = { it.id }) { price ->
                    RicePriceCard(
                        price = price,
                        onClick = { onSelectVariety(price.variety) }
                    )
                }
            }

            item {
                FooterNote()
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

/**
 * Tab row "Tin tức | Bảng giá lúa" — đồng bộ 2 chiều với HorizontalPager.
 * Tap tab → animateScrollToPage; vuốt ngang trên pager → indicator tự follow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarketTabRow(
    selectedTab: Int,
    onSelect: (Int) -> Unit
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = AppColors.Surface,
        contentColor = AppColors.GreenPrimary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Tab(
            selected = selectedTab == 0,
            onClick = { onSelect(0) },
            selectedContentColor = AppColors.GreenPrimary,
            unselectedContentColor = AppColors.TextSecondary
        ) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Tin tức",
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
        Tab(
            selected = selectedTab == 1,
            onClick = { onSelect(1) },
            selectedContentColor = AppColors.GreenPrimary,
            unselectedContentColor = AppColors.TextSecondary
        ) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PriceChange,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Bảng giá lúa",
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.titleSmall
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

/**
 * Card hiển thị bid riêng của trader trong section "Giá rao của bạn".
 * Tap → edit; long-press / icon delete → xoá.
 */
@Composable
private fun MyBidCard(
    bid: FirestoreRicePrice,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val numberFormat = remember { java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("vi-VN")) }
    val trendColor = when (bid.trend) {
        "UP" -> AppColors.Success
        "DOWN" -> AppColors.Error
        else -> AppColors.Info
    }
    val trendLabel = when (bid.trend) {
        "UP" -> "Đang tăng"
        "DOWN" -> "Đang giảm"
        else -> "Ổn định"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.GreenSurface)
            .clickable(onClick = onEdit)
            .padding(16.dp)
    ) {
        Column {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = bid.variety.ifBlank { "—" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(trendColor.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = trendLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = trendColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${numberFormat.format(bid.priceMin.toLong())} – ${numberFormat.format(bid.priceMax.toLong())} đ/kg",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.GreenPrimary
            )
            if (bid.region.isNotBlank()) {
                Text(
                    text = "Khu vực: ${bid.region}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
            if (bid.note.isNotBlank()) {
                Text(
                    text = bid.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint,
                    maxLines = 2
                )
            }
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End)
            ) {
                androidx.compose.material3.TextButton(onClick = onDelete) {
                    Text("Xoá", color = AppColors.Error)
                }
                androidx.compose.material3.TextButton(onClick = onEdit) {
                    Text("Sửa", color = AppColors.GreenPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
