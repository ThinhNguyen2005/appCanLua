package com.giathinh.canlua.ui.screen.market

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.giathinh.canlua.data.firestore.FirestoreRicePrice
import com.giathinh.canlua.ui.component.market.BidEditorSheet
import com.giathinh.canlua.ui.component.market.MarketSkeletonList
import com.giathinh.canlua.ui.component.market.NativeAdPlaceholder
import com.giathinh.canlua.ui.component.market.NewsSection
import com.giathinh.canlua.ui.component.market.PriceTrendChart
import com.giathinh.canlua.ui.component.market.RicePriceCard
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.util.isScrollingUp
import com.giathinh.canlua.ui.viewmodel.MarketViewModel
import com.giathinh.canlua.ui.viewmodel.NewsViewModel
import com.giathinh.canlua.ui.viewmodel.ProfileViewModel
import com.giathinh.canlua.ui.viewmodel.TraderBidsViewModel
import com.giathinh.canlua.util.PremiumState
import com.giathinh.canlua.util.TrackScreenRender
import com.giathinh.canlua.R
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch

/**
 * Module 2 — Bảng Tin Giá Lúa & Thị Trường
 *
 * Layout: TabRow + HorizontalPager 2 trang.
 *  - Page 0 "Tin tức": NativeAd + NewsSection (cuộn chung trong LazyColumn)
 *  - Page 1 "Bảng giá lúa": Giá rao của bạn (nếu trader) + Bảng giá thu mua
 *
 * Tab và pager đồng bộ 2 chiều.
 */
import com.giathinh.canlua.ui.component.TransitionSafeWrapper
import com.giathinh.canlua.ui.component.MarketSkeleton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MarketScreen() {
    TrackScreenRender("market")
    val viewModel: MarketViewModel = hiltViewModel()
    val newsViewModel: NewsViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val bidsViewModel: TraderBidsViewModel = hiltViewModel()

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val prices by viewModel.prices.collectAsStateWithLifecycle()
    val newsArticles by newsViewModel.articles.collectAsStateWithLifecycle()
    val isDataReady = !isLoading || prices.isNotEmpty() || newsArticles.isNotEmpty()



    TransitionSafeWrapper(
        isDataReady = isDataReady,
        skeletonContent = { MarketSkeleton() }
    ) {
        MarketScreenContent(
            viewModel = viewModel,
            newsViewModel = newsViewModel,
            profileViewModel = profileViewModel,
            bidsViewModel = bidsViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MarketScreenContent(
    viewModel: MarketViewModel,
    newsViewModel: NewsViewModel,
    profileViewModel: ProfileViewModel,
    bidsViewModel: TraderBidsViewModel
) {
    val prices by viewModel.prices.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val refreshError by viewModel.refreshError.collectAsStateWithLifecycle()
    val selectedVariety by viewModel.selectedVariety.collectAsStateWithLifecycle()
    val timeRangeDays by viewModel.timeRangeDays.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val activePage by remember { derivedStateOf { pagerState.currentPage } }
    val isNewsActive = activePage == 0
    val isPricesActive = activePage == 1

    LaunchedEffect(isNewsActive) {
        if (isNewsActive) {
            newsViewModel.loadData()
        }
    }

    val newsArticles = if (isNewsActive) {
        newsViewModel.articles.collectAsStateWithLifecycle().value
    } else {
        emptyList()
    }
    val newsTopic = if (isNewsActive) {
        newsViewModel.selectedTopic.collectAsStateWithLifecycle().value
    } else {
        null
    }
    val newsUi = if (isNewsActive) {
        newsViewModel.ui.collectAsStateWithLifecycle().value
    } else {
        com.giathinh.canlua.ui.viewmodel.NewsUiState()
    }
    val profile by profileViewModel.profile.collectAsStateWithLifecycle(initialValue = null)
    val isTrader = profile?.role == "TRADER"
    val isPremium by PremiumState.isPremium.collectAsStateWithLifecycle()
    val myBids = if (isPricesActive) {
        bidsViewModel.myBids.collectAsStateWithLifecycle().value
    } else {
        emptyList()
    }
    val bidUiState = if (isPricesActive) {
        bidsViewModel.uiState.collectAsStateWithLifecycle().value
    } else {
        com.giathinh.canlua.ui.viewmodel.TraderBidUiState()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val newsListState = rememberLazyListState()
    val pricesListState = rememberLazyListState()

    // FAB chỉ hiện ở tab "Bảng giá lúa" — nơi thương lái thực sự đăng giá.
    val fabExpanded = pricesListState.isScrollingUp() || myBids.isEmpty()

    var showEditor by remember { mutableStateOf(false) }
    var editingBid by remember { mutableStateOf<FirestoreRicePrice?>(null) }

    val onSubmitBid = remember(bidsViewModel) {
        { variety: String, pMin: Double, pMax: Double, region: String, trend: String, note: String, riceType: String, existingId: String? ->
            bidsViewModel.submitBid(variety, pMin, pMax, region, trend, note, riceType, existingId)
        }
    }
    val onDismissEditor = remember {
        {
            showEditor = false
            editingBid = null
        }
    }

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

    // H-08: Hiển thị Snackbar khi Market refresh thất bại (lỗi mạng / Firestore)
    LaunchedEffect(refreshError) {
        refreshError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearRefreshError()
        }
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
            val context = androidx.compose.ui.platform.LocalContext.current
            val onNewsRefresh = remember { { newsViewModel.refresh(forceLocalScrape = true) } }
            val onRefreshNewsPage = remember {
                {
                    newsViewModel.refresh(forceLocalScrape = true)
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
            onDismissRequest = onDismissEditor,
            sheetState = editorSheetState,
            containerColor = AppColors.Surface
        ) {
            BidEditorSheet(
                existing = editingBid,
                isSaving = bidUiState.isSaving,
                onSubmit = onSubmitBid,
                onDismiss = onDismissEditor
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
    articles: List<com.giathinh.canlua.data.model.NewsArticle>,
    selectedTopic: com.giathinh.canlua.data.model.NewsTopic?,
    isRefreshing: Boolean,
    errorMessage: String?,
    isPremium: Boolean,
    onSelectTopic: (com.giathinh.canlua.data.model.NewsTopic?) -> Unit,
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
                start = 0.dp,
                end = 0.dp,
                top = 12.dp,
                bottom = 80.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "native_ad") {
                AnimatedVisibility(
                    visible = !isPremium,
                    enter = fadeIn(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    NativeAdPlaceholder(
                        onClick = {},
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
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
    prices: List<com.giathinh.canlua.data.model.RicePrice>,
    isLoading: Boolean,
    filter: com.giathinh.canlua.ui.viewmodel.MarketFilter,
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
                val isOffline = myBids.any { it.isFromCache }
                item {
                    SectionTitle(
                        title = if (isOffline) "Giá rao của bạn (đang xem ngoại tuyến)" else "Giá rao của bạn",
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
                FilterChipsRow(
                    selectedTrend = filter.trend,
                    onSelectTrend = onSelectTrend
                )
            }

            item {
                SectionTitle(
                    title = "Giá thu mua hôm nay",
                    subtitle = "Bảng giá lúa, gạo · ${prices.size} kết quả"
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
 * Tab row "Tin tức | Bảng giá lúa, gạo" — đồng bộ 2 chiều với HorizontalPager.
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = "Tin tức",
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PriceChange,
                    contentDescription = "Bảng giá lúa, gạo",
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Bảng giá lúa, gạo",
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
                    "Phiên bản tiếp theo sẽ tích hợp giá real-time theo GPS.",
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
    val haptic = LocalHapticFeedback.current
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
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    // H-10: Đảm bảo touch target tối thiểu 48dp theo Material guidelines
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) color.copy(alpha = 0.18f) else AppColors.SurfaceContainer)
                    .clickable {
                        // H-10: Haptic feedback khi chọn filter
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectTrend(key)
                    }
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
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.GreenSurface)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onEdit()
            }
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
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
