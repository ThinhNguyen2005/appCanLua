package com.giathinh.canlua.ui.screen.market

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.canlua.data.firestore.FirestoreRicePrice
import com.giathinh.canlua.ui.component.MarketSkeleton
import com.giathinh.canlua.ui.component.TransitionSafeWrapper
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
import kotlinx.coroutines.launch

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

    LaunchedEffect(isNewsActive) {
        if (isNewsActive) {
            newsViewModel.loadData()
        }
    }

    val newsArticles by newsViewModel.articles.collectAsStateWithLifecycle()
    val newsTopic by newsViewModel.selectedTopic.collectAsStateWithLifecycle()
    val newsUi by newsViewModel.ui.collectAsStateWithLifecycle()
    val profile by profileViewModel.profile.collectAsStateWithLifecycle(initialValue = null)
    val isTrader = profile?.role == "TRADER"
    val isPremium by PremiumState.isPremium.collectAsStateWithLifecycle()
    val myBids: List<Nothing> = emptyList()
    val bidUiState by bidsViewModel.uiState.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val newsListState = rememberLazyListState()
    val pricesListState = rememberLazyListState()

    val newsScrollingUp = newsListState.isScrollingUp()
    val pricesScrollingUp = pricesListState.isScrollingUp()
    val scrollingUp = if (pagerState.currentPage == 0) newsScrollingUp else pricesScrollingUp

    LaunchedEffect(scrollingUp) {
        com.giathinh.canlua.ui.util.BottomBarVisibility.set(scrollingUp)
    }

    DisposableEffect(Unit) {
        onDispose {
            com.giathinh.canlua.ui.util.BottomBarVisibility.reset()
        }
    }

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomContentPadding = remember(navBarPadding) { 96.dp + navBarPadding }
    val bottomFabPadding = remember(navBarPadding) { 80.dp + navBarPadding }

    val fabVisible = scrollingUp || prices.isEmpty()
    val fabExpanded = pricesScrollingUp || myBids.isEmpty()

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
                AnimatedVisibility(
                    visible = fabVisible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
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
                        modifier = Modifier.padding(bottom = bottomFabPadding),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
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
            MarketTabRow(
                selectedTab = pagerState.currentPage,
                onSelect = { tab ->
                    scope.launch { pagerState.animateScrollToPage(tab) }
                }
            )

            val onNewsRefresh = remember { { newsViewModel.refresh() } }
            val onRefreshNewsPage = remember { { newsViewModel.refresh() } }
            val onSelectTopic = remember { newsViewModel::selectTopic }
            val onDismissError = remember { newsViewModel::clearError }
            val onRefreshPrices = remember { { viewModel.refreshFromSupabase() } }
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
                        bottomPadding = bottomContentPadding,
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
                        bottomPadding = bottomContentPadding,
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

    if (selectedVariety != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectVariety(null) },
            sheetState = sheetState,
            containerColor = AppColors.Surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
            containerColor = AppColors.Surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
 * Phục vụ xử lý khi tin tức bị thiếu ảnh đại diện.
 * Biểu tượng được vẽ thủ công theo phong cách tối giản & thanh lịch của Google News.
 */
@Composable
fun GoogleNewsPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFF1F3F4), Color(0xFFE8EAED))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            // Biểu tượng tờ báo giả lập
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Khối màu xanh làm điểm nhấn thương hiệu giống Google News
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF4285F4))
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(modifier = Modifier.size(width = 32.dp, height = 4.dp).background(Color(0xFF5F6368)))
                    Box(modifier = Modifier.size(width = 20.dp, height = 4.dp).background(Color(0xFF5F6368)))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Các đường line giả văn bản bên dưới tờ báo
            Box(modifier = Modifier.size(width = 58.dp, height = 4.dp).background(Color(0xFF9AA0A6)))
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.size(width = 44.dp, height = 4.dp).background(Color(0xFFBDC1C6)))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewsPage(
    listState: LazyListState,
    articles: List<com.giathinh.canlua.data.model.NewsArticle>,
    selectedTopic: com.giathinh.canlua.data.model.NewsTopic?,
    isRefreshing: Boolean,
    errorMessage: String?,
    isPremium: Boolean,
    bottomPadding: androidx.compose.ui.unit.Dp,
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
                bottom = bottomPadding
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PricesPage(
    listState: LazyListState,
    prices: List<com.giathinh.canlua.data.model.RicePrice>,
    isLoading: Boolean,
    filter: com.giathinh.canlua.ui.viewmodel.MarketFilter,
    isTrader: Boolean,
    myBids: List<FirestoreRicePrice>,
    bottomPadding: androidx.compose.ui.unit.Dp,
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
                bottom = bottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isTrader && myBids.isNotEmpty()) {
                val isOffline = myBids.any { it.isFromCache }
                item {
                    SectionTitle(
                        title = if (isOffline) "Giá rao của bạn (ngoại tuyến)" else "Giá rao của bạn",
                        subtitle = "${myBids.size} tin đang đăng · chạm nhẹ để chỉnh sửa"
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
                    subtitle = "Bảng giá lúa gạo toàn quốc · ${prices.size} kết quả"
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
 * UI TabRow được tái cấu trúc theo style Modern Pill Indicator.
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
        modifier = Modifier.fillMaxWidth(),
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedTab),
                width = 48.dp,
                color = AppColors.GreenPrimary,
                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
            )
        }
    ) {
        Tab(
            selected = selectedTab == 0,
            onClick = { onSelect(0) },
            selectedContentColor = AppColors.GreenPrimary,
            unselectedContentColor = AppColors.TextSecondary
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
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
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PriceChange,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Bảng giá lúa, gạo",
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        Spacer(Modifier.height(2.dp))
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
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceContainer)
            .padding(12.dp)
    ) {
        Text(
            text = "💡 Dữ liệu mang tính tham khảo, được tổng hợp từ thương lái khu vực ĐBSCL. " +
                    "Phiên bản tiếp theo sẽ tích hợp giá real-time định vị bằng vệ tinh GPS.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextHint
        )
    }
}

/**
 * Filter chips cải tiến với phản hồi xúc giác nhẹ nhàng khi nhấn và thiết kế thanh lịch hơn.
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
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (selected) color.copy(alpha = 0.12f) else Color.Transparent
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) color else AppColors.Divider,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelectTrend(key)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
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
}

/**
 * Card giá cá nhân của Trader, nâng cấp bo góc và hiển thị thông tin rõ ràng.
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
                    text = bid.variety.ifBlank { "Chưa xác định" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(trendColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = trendLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = trendColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${numberFormat.format(bid.priceMin.toLong())} – ${numberFormat.format(bid.priceMax.toLong())} đ/kg",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.GreenPrimary
            )
            Spacer(Modifier.height(4.dp))
            if (bid.region.isNotBlank()) {
                Text(
                    text = "📍 Khu vực: ${bid.region}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
            if (bid.note.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = bid.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                androidx.compose.material3.TextButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Xoá tin", color = AppColors.Error, fontWeight = FontWeight.Medium)
                }
                androidx.compose.material3.Button(
                    onClick = onEdit,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = AppColors.GreenPrimary.copy(alpha = 0.1f),
                        contentColor = AppColors.GreenPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Sửa giá", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
