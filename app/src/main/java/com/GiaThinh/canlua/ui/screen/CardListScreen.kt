package com.GiaThinh.canlua.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.repository.SyncStatus
import com.GiaThinh.canlua.ui.component.CardItem
import com.GiaThinh.canlua.ui.component.CreateCardDialog
import com.GiaThinh.canlua.ui.component.CreateCardMode
import com.GiaThinh.canlua.ui.component.CardListSkeleton
import com.GiaThinh.canlua.ui.component.cardlist.CardListEmptyState
import com.GiaThinh.canlua.ui.component.cardlist.CardListFilterBar
import com.GiaThinh.canlua.ui.component.cardlist.CardListFilterSheet
import com.GiaThinh.canlua.ui.component.cardlist.CardListSummaryCard
import com.GiaThinh.canlua.ui.component.cardlist.DeleteCardConfirmDialog
import com.GiaThinh.canlua.ui.component.cardlist.PremiumQuotaDialog
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.util.isScrollingUp
import com.GiaThinh.canlua.ui.viewmodel.CardListViewModel
import com.GiaThinh.canlua.ui.viewmodel.ProfileViewModel
import com.GiaThinh.canlua.ui.viewmodel.SyncViewModel
import com.GiaThinh.canlua.util.HapticUtil
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen(
    navController: NavController,
    viewModel: CardListViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    syncViewModel: SyncViewModel = hiltViewModel()
) {
    com.GiaThinh.canlua.util.TrackScreenRender("scale")
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    // Skeleton hiện đến khi Room thực sự emit data (không min duration cứng).
    // Vì InitViewModel đã warm Room page cache ở splash → isLoading flip false
    // gần như tức thì; nếu DB chậm bất thường (thiết bị yếu, lần đầu), skeleton
    // sẽ tự giữ lâu hơn — đúng tinh thần "đến khi load xong".
    val showSkeleton = isLoading
    val selectedFilter by viewModel.selectedVarietyFilter.collectAsStateWithLifecycle()
    val availableVarieties by viewModel.availableVarieties.collectAsStateWithLifecycle()
    val suggestedVarieties by viewModel.suggestedRiceVarieties.collectAsStateWithLifecycle()
    val selectedSeason by viewModel.selectedSeasonFilter.collectAsStateWithLifecycle()
    val availableSeasons by viewModel.availableSeasons.collectAsStateWithLifecycle()
    val profileState by profileViewModel.profile.collectAsStateWithLifecycle(initialValue = null)
    val syncStatus by syncViewModel.syncStatus.collectAsStateWithLifecycle()
    val isPremium by com.GiaThinh.canlua.util.PremiumState.isPremium.collectAsStateWithLifecycle()
    val cardsToday by com.GiaThinh.canlua.util.PremiumState.dailyCreated.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPremiumGate by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<com.GiaThinh.canlua.data.model.Card?>(null) }
    var manualRefreshing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scrollingUp = listState.isScrollingUp()
    // derivedStateOf: chỉ trigger recompose khi giá trị BOOLEAN thay đổi,
    // không phải mỗi khi scrollingUp hoặc cards.isEmpty() State đọc lại.
    val fabVisible by remember {
        derivedStateOf { scrollingUp || cards.isEmpty() }
    }

    // Đồng bộ ẩn/hiện bottom bar theo hướng cuộn — FAB không bị thanh điều hướng chồng.
    LaunchedEffect(fabVisible) {
        com.GiaThinh.canlua.ui.util.BottomBarVisibility.set(fabVisible)
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { com.GiaThinh.canlua.ui.util.BottomBarVisibility.reset() }
    }

    // Tự động tắt refreshing khi sync xong (hoặc hết 800ms giả lập để user thấy phong cách)
    LaunchedEffect(manualRefreshing, syncStatus) {
        if (manualRefreshing && syncStatus !is SyncStatus.Syncing) {
            delay(150)
            manualRefreshing = false
        }
    }

    // Owner = người đang đăng nhập; counterparty tùy theo role (farmer vs trader).
    val isTrader = profileState?.role == "TRADER"
    val ownerName = profileState?.name ?: if (isTrader) "Thương lái" else "Nông dân"

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")) }

    // Calculate today's stats - Optimizing recomposition with remember
    val todayCards = remember(cards) {
        val todayCal = Calendar.getInstance()
        cards.filter { card ->
            val cardCal = Calendar.getInstance().apply { time = card.date }
            cardCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) &&
                    cardCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
        }
    }
    val todayTotalKg = remember(todayCards) { todayCards.sumOf { it.totalWeight } }
    val todayTotalAmount = remember(todayCards) { todayCards.sumOf { it.totalAmount } }

    // Lift lambda ra ngoài items{} → 1 instance dùng chung cho cả danh sách,
    // tránh tạo 200 closure mới mỗi khi cards thay đổi (sync/filter).
    val onCardClick = remember(navController) {
        { id: Long -> navController.navigate("card_detail/$id") }
    }
    val onCardDelete: (com.GiaThinh.canlua.data.model.Card) -> Unit = remember {
        { card ->
            cardToDelete = card
            showDeleteConfirmDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // === Card List (chứa cả Summary + Filter chips để cuộn theo) ===
            // Crossfade thay vì 2 AnimatedVisibility riêng biệt vì skeleton và real content
            // GIỜ CÙNG cấu trúc LazyColumn → swap mượt, không unmeasure → measure lại.
            // listState DÙNG CHUNG giữa skeleton và real content → scroll position
            // được bảo toàn khi skeleton → content (sau khi InitViewModel warm cache).
            // C-05: pullState khai báo NGOÀI Crossfade → không bị reset khi transition.
            val pullState = rememberPullToRefreshState()
            Crossfade(
                targetState = showSkeleton,
                animationSpec = tween(durationMillis = 300),
                label = "card_list_crossfade"
            ) { skeleton ->
                if (skeleton) {
                    CardListSkeleton(count = 3, listState = listState)
                } else {
                    PullToRefreshBox(
                        isRefreshing = manualRefreshing,
                        onRefresh = {
                            manualRefreshing = true
                            viewModel.refreshCards()
                            syncViewModel.syncAll()
                        },
                        state = pullState,
                        modifier = Modifier.fillMaxSize(),
                        indicator = {
                            PullToRefreshDefaults.Indicator(
                                state = pullState,
                                isRefreshing = manualRefreshing,
                                color = AppColors.GreenPrimary,
                                containerColor = AppColors.GreenSurface,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .graphicsLayer {
                                        val p = pullState.distanceFraction.coerceIn(0f, 1f)
                                        scaleX = p
                                        scaleY = p
                                        alpha = p
                                    }
                            )
                        }
                    ) {
                        val groupedCards = remember(cards) {
                            cards.groupBy { card ->
                                dateFormat.format(card.date)
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 96.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item(key = "summary_header") {
                                CardListSummaryCard(
                                    cardCount = todayCards.size,
                                    totalKg = todayTotalKg,
                                    totalAmount = todayTotalAmount,
                                    syncStatus = syncStatus
                                )
                            }

                            if (availableSeasons.isNotEmpty() || availableVarieties.isNotEmpty()) {
                                item(key = "filter_bar") {
                                    CardListFilterBar(
                                        selectedSeason = selectedSeason,
                                        selectedVariety = selectedFilter,
                                        onOpenFilter = { showFilterSheet = true },
                                        onClearSeason = { viewModel.setSeasonFilter(null) },
                                        onClearVariety = { viewModel.setVarietyFilter(null) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            if (cards.isEmpty()) {
                                val isFiltered = selectedFilter != null || selectedSeason != null
                                item(key = "empty") {
                                    if (isFiltered) {
                                        CardListEmptyState(
                                            icon = Icons.Default.Info,
                                            title = stringResource(com.GiaThinh.canlua.R.string.card_list_empty_filter_title),
                                            subtitle = stringResource(com.GiaThinh.canlua.R.string.card_list_empty_filter_subtitle),
                                            onSyncClick = null
                                        )
                                    } else {
                                        CardListEmptyState(
                                            onSyncClick = { syncViewModel.syncAll() },
                                            syncing = syncStatus is SyncStatus.Syncing
                                        )
                                    }
                                }
                            } else {
                                groupedCards.forEach { (date, cardsInDay) ->
                                    item(key = "header_$date") {
                                        Text(
                                            text = date,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = AppColors.TextSecondary,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(
                                                top = 8.dp,
                                                bottom = 4.dp
                                            )
                                        )
                                    }
                                    items(
                                        items = cardsInDay,
                                        key = { it.id }
                                    ) { card ->
                                        CardItem(
                                            card = card,
                                            onClick = onCardClick,
                                            onDelete = onCardDelete
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB — auto-hide khi scroll xuống đọc danh sách; bottom bar cũng tự ẩn theo
        // (xem LaunchedEffect ở trên). Dùng Modifier.navigationBarsPadding() thay vì
        // WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() để
        // tránh alloc PaddingValues object mỗi recomposition khi scroll danh sách.
        AnimatedVisibility(
            visible = fabVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 96.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    // Premium gate: free user tối đa FREE_CARDS_PER_DAY phiếu/ngày.
                    // Đếm reactive từ cardsToday → nếu vượt mở dialog upsell thay vì tạo.
                    if (!isPremium && cardsToday >= com.GiaThinh.canlua.util.PremiumState.FREE_CARDS_PER_DAY) {
                        showPremiumGate = true
                        com.GiaThinh.canlua.util.AnalyticsHelper.premiumGateShown(cardsToday)
                    } else {
                        showCreateDialog = true
                    }
                },
                containerColor = AppColors.GreenPrimary,
                contentColor = AppColors.CardBg,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = {
                    Text(
                        text = "Tạo phiếu cân",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    }

    // Create Dialog — reuse cho cả farmer & trader (mode swap label).
    if (showCreateDialog) {
        CreateCardDialog(
            ownerName = ownerName,
            suggestedVarieties = suggestedVarieties,
            mode = if (isTrader) CreateCardMode.TRADER else CreateCardMode.FARMER,
            onDismiss = { showCreateDialog = false },
            onCreate = { counterpartyName, counterpartyPhone, variety, season, moisture, price, deposit, cccd, bagWeight, impurityWeight, recordLocation ->
                // FARMER: name=farmer (owner), traderName=counterparty.
                // TRADER: name=farmer (counterparty), traderName=trader (owner).
                val cardName = if (isTrader) counterpartyName else ownerName
                val cardTraderName = if (isTrader) ownerName else counterpartyName
                val cardTraderPhone = if (isTrader) profileState?.phone.orEmpty() else counterpartyPhone
                viewModel.createNewCard(
                    name = cardName,
                    cccd = cccd,
                    traderName = cardTraderName,
                    pricePerKg = price,
                    depositAmount = deposit,
                    riceVariety = variety,
                    moisturePercent = moisture,
                    seasonLabel = season,
                    traderPhone = cardTraderPhone,
                    bagWeight = bagWeight,
                    impurityWeight = impurityWeight,
                    recordLocation = recordLocation
                )
                // Tăng counter chống gian lận. Counter chỉ tăng — xoá phiếu cũ
                // KHÔNG giảm → user free không thể bypass quota 3 phiếu/ngày.
                if (!isPremium) {
                    com.GiaThinh.canlua.util.PremiumState.incrementDailyCreated(context)
                }
                com.GiaThinh.canlua.util.AnalyticsHelper.cardCreated(
                    role = if (isTrader) "TRADER" else "FARMER",
                    hasGps = profileState?.region?.isNotBlank() == true
                )
            }
        )
    }

    if (showDeleteConfirmDialog && cardToDelete != null) {
        val targetCard = cardToDelete ?: return
        DeleteCardConfirmDialog(
            card = targetCard,
            onConfirm = {
                viewModel.deleteCard(targetCard)
                HapticUtil.error(context)
                showDeleteConfirmDialog = false
                cardToDelete = null
            },
            onDismiss = {
                showDeleteConfirmDialog = false
                cardToDelete = null
            }
        )
    }

    if (showPremiumGate) {
        PremiumQuotaDialog(
            cardsToday = cardsToday,
            freeLimit = com.GiaThinh.canlua.util.PremiumState.FREE_CARDS_PER_DAY,
            onUpgrade = {
                showPremiumGate = false
                navController.navigate("premium")
            },
            onDismiss = { showPremiumGate = false }
        )
    }

    if (showFilterSheet) {
        CardListFilterSheet(
            availableSeasons = availableSeasons,
            availableVarieties = availableVarieties,
            selectedSeason = selectedSeason,
            selectedVariety = selectedFilter,
            onSelectSeason = viewModel::setSeasonFilter,
            onSelectVariety = viewModel::setVarietyFilter,
            onDismiss = { showFilterSheet = false }
        )
    }
}
