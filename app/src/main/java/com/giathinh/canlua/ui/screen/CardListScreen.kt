package com.giathinh.canlua.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.giathinh.canlua.R
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.repository.SyncStatus
import com.giathinh.canlua.ui.component.CardItem
import com.giathinh.canlua.ui.component.CreateCardBottomSheet
import com.giathinh.canlua.ui.component.CreateCardMode
import com.giathinh.canlua.ui.component.cardlist.CardListCreateFab
import com.giathinh.canlua.ui.component.cardlist.CardListEmptyState
import com.giathinh.canlua.ui.component.cardlist.CardListFilterBar
import com.giathinh.canlua.ui.component.cardlist.CardListFilterSheet
import com.giathinh.canlua.ui.component.cardlist.CardListSkeleton
import com.giathinh.canlua.ui.component.cardlist.CardListSummaryCard
import com.giathinh.canlua.ui.component.cardlist.DeleteCardConfirmDialog
import com.giathinh.canlua.ui.component.cardlist.PremiumQuotaDialog
import com.giathinh.canlua.ui.feedback.LocalAppToast
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.theme.AppDimensions
import com.giathinh.canlua.ui.util.BottomBarVisibility
import com.giathinh.canlua.ui.util.isScrollingUp
import com.giathinh.canlua.ui.viewmodel.CardListViewModel
import com.giathinh.canlua.ui.viewmodel.CardQuickFilter
import com.giathinh.canlua.ui.viewmodel.DeleteCardEvent
import com.giathinh.canlua.ui.viewmodel.ProfileViewModel
import com.giathinh.canlua.ui.viewmodel.SyncViewModel
import com.giathinh.canlua.util.AnalyticsHelper
import com.giathinh.canlua.util.HapticUtil
import com.giathinh.canlua.util.PremiumState
import com.giathinh.canlua.util.TrackScreenRender
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

private const val SCREEN_NAME_SCALE = "scale"
private const val ROLE_TRADER = "TRADER"
private const val ROLE_FARMER = "FARMER"
private const val ROUTE_PREMIUM = "premium"
private const val KEY_SUMMARY_HEADER = "summary_header"
private const val KEY_FILTER_BAR = "filter_bar"
private const val KEY_EMPTY = "empty"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CardListScreen(
    navController: NavController
) {
    TrackScreenRender(SCREEN_NAME_SCALE)
    val viewModel: CardListViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val syncViewModel: SyncViewModel = hiltViewModel()

    CardListScreenContent(
        navController = navController,
        viewModel = viewModel,
        profileViewModel = profileViewModel,
        syncViewModel = syncViewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CardListScreenContent(
    navController: NavController,
    viewModel: CardListViewModel,
    profileViewModel: ProfileViewModel,
    syncViewModel: SyncViewModel
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val showSkeleton = isLoading && cards.isEmpty()
    val selectedFilter by viewModel.selectedVarietyFilter.collectAsStateWithLifecycle()
    val availableVarieties by viewModel.availableVarieties.collectAsStateWithLifecycle()
    val suggestedVarieties by viewModel.suggestedRiceVarieties.collectAsStateWithLifecycle()
    val selectedSeason by viewModel.selectedSeasonFilter.collectAsStateWithLifecycle()
    val availableSeasons by viewModel.availableSeasons.collectAsStateWithLifecycle()
    val quickFilter by viewModel.quickFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val profileState by profileViewModel.profile.collectAsStateWithLifecycle(initialValue = null)
    val syncStatus by syncViewModel.syncStatus.collectAsStateWithLifecycle()
    val lastSyncTime by syncViewModel.lastSyncTime.collectAsStateWithLifecycle()
    val isPremium by PremiumState.isPremium.collectAsStateWithLifecycle()
    val cardsToday by PremiumState.dailyCreated.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val appToast = LocalAppToast.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPremiumGate by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<Card?>(null) }
    var manualRefreshing by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scrollingUp = listState.isScrollingUp()
    val fabVisible by remember {
        derivedStateOf { scrollingUp || cards.isEmpty() }
    }

    LaunchedEffect(fabVisible) {
        BottomBarVisibility.set(fabVisible)
    }
    DisposableEffect(Unit) {
        onDispose { BottomBarVisibility.reset() }
    }

    LaunchedEffect(manualRefreshing, syncStatus) {
        if (manualRefreshing && syncStatus !is SyncStatus.Syncing) {
            delay(150.milliseconds)
            manualRefreshing = false
        }
    }

    val deleteErrorMsg = stringResource(R.string.card_list_delete_error_toast)
    val deleteSuccessTemplate = stringResource(R.string.card_list_delete_success_toast)
    LaunchedEffect(viewModel, deleteSuccessTemplate, deleteErrorMsg) {
        viewModel.deleteEvents.collect { event ->
            when (event) {
                is DeleteCardEvent.Success -> {
                    appToast.success(deleteSuccessTemplate.format(event.cardName))
                }
                DeleteCardEvent.Error -> appToast.error(deleteErrorMsg)
            }
        }
    }

    val isTrader = profileState?.role == ROLE_TRADER
    val defaultRoleLabel = if (isTrader) {
        stringResource(R.string.trader)
    } else {
        stringResource(R.string.farmer)
    }
    val ownerName = profileState?.name?.takeIf { it.isNotBlank() } ?: defaultRoleLabel

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")) }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }
    val todayDateStr = remember(cards) { dateFormat.format(Calendar.getInstance().time) }
    val yesterdayDateStr = remember(cards) {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.let { dateFormat.format(it.time) }
    }

    val groupedCards = remember(cards) {
        cards.groupBy { card -> dateFormat.format(card.date) }
    }
    val todayCards = remember(groupedCards, todayDateStr) {
        groupedCards[todayDateStr].orEmpty()
    }
    val latestDateEntry = remember(groupedCards) {
        groupedCards.entries.firstOrNull()
    }
    val hasTodayCards = todayCards.isNotEmpty()
    val summaryCards = if (hasTodayCards) todayCards else (latestDateEntry?.value.orEmpty())
    val isLatestFallback = !hasTodayCards && summaryCards.isNotEmpty()

    val summaryTitlePrefix = when {
        hasTodayCards -> stringResource(R.string.card_list_today_prefix)
        isLatestFallback -> stringResource(R.string.card_list_latest_date_prefix, latestDateEntry?.key.orEmpty())
        else -> stringResource(R.string.card_list_today_prefix)
    }
    val summaryTotalKg = remember(summaryCards) { summaryCards.sumOf { it.totalWeight } }
    val summaryTotalAmount = remember(summaryCards) { summaryCards.sumOf { it.totalAmount } }
    val summaryAvgPricePerKg = remember(summaryCards) {
        val pricedCards = summaryCards.filter { it.pricePerKg > 0 }
        if (pricedCards.isEmpty()) null else pricedCards.map { it.pricePerKg }.average()
    }
    val summaryBagCount = remember(summaryCards) {
        val total = summaryCards.sumOf { it.bagCount }
        if (total <= 0) null else total
    }

    val onCardClick = remember(navController) {
        { id: Long -> navController.navigate("card_detail/$id") }
    }
    val onContinueWeigh = remember(navController) {
        { id: Long -> navController.navigate("weight_input/$id") }
    }
    val onCardDelete: (Card) -> Unit = remember {
        { card ->
            cardToDelete = card
            showDeleteConfirmDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            val pullState = rememberPullToRefreshState()
            Crossfade(
                targetState = showSkeleton,
                label = "card_list_crossfade"
            ) { isSkeleton ->
                if (isSkeleton) {
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
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = AppDimensions.SpacingMd,
                                end = AppDimensions.SpacingMd,
                                top = AppDimensions.SpacingXs,
                                bottom = AppDimensions.BottomBarHeight + AppDimensions.SpacingMd
                            ),
                            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingXs)
                        ) {
                            item(key = KEY_SUMMARY_HEADER) {
                                CardListSummaryCard(
                                    cardCount = summaryCards.size,
                                    totalKg = summaryTotalKg,
                                    totalAmount = summaryTotalAmount,
                                    syncStatus = syncStatus,
                                    showSyncStatus = false,
                                    avgPricePerKg = summaryAvgPricePerKg,
                                    bagCount = summaryBagCount,
                                    titlePrefix = summaryTitlePrefix,
                                    modifier = Modifier.animateItem()
                                )
                            }

                            val showFilterBar = cards.isNotEmpty() ||
                                availableSeasons.isNotEmpty() ||
                                availableVarieties.isNotEmpty() ||
                                quickFilter != CardQuickFilter.ALL ||
                                searchQuery.isNotBlank() ||
                                selectedFilter != null ||
                                selectedSeason != null
                            if (showFilterBar) {
                                item(key = KEY_FILTER_BAR) {
                                    CardListFilterBar(
                                        selectedSeason = selectedSeason,
                                        selectedVariety = selectedFilter,
                                        onOpenFilter = { showFilterSheet = true },
                                        onClearSeason = { viewModel.setSeasonFilter(null) },
                                        onClearVariety = { viewModel.setVarietyFilter(null) },
                                        quickFilter = quickFilter,
                                        onQuickFilterChange = viewModel::setQuickFilter,
                                        searchQuery = searchQuery,
                                        onSearchQueryChange = viewModel::setSearchQuery,
                                        onClearAll = viewModel::clearFilters,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem()
                                    )
                                }
                            }

                            if (cards.isEmpty()) {
                                val isFiltered = selectedFilter != null ||
                                    selectedSeason != null ||
                                    quickFilter != CardQuickFilter.ALL ||
                                    searchQuery.isNotBlank()
                                item(key = KEY_EMPTY) {
                                    if (isFiltered) {
                                        CardListEmptyState(
                                            icon = Icons.Default.Info,
                                            title = stringResource(R.string.card_list_empty_filter_title),
                                            subtitle = stringResource(R.string.card_list_empty_filter_subtitle),
                                            onSyncClick = null,
                                            actionButtonText = stringResource(R.string.card_list_clear_filter_action),
                                            onActionClick = viewModel::clearFilters,
                                            modifier = Modifier.animateItem()
                                        )
                                    } else {
                                        CardListEmptyState(
                                            onSyncClick = if (syncViewModel.isUserSignedIn && lastSyncTime == null) {
                                                { syncViewModel.syncAll() }
                                            } else {
                                                null
                                            },
                                            syncing = syncStatus is SyncStatus.Syncing,
                                            actionButtonText = stringResource(R.string.card_list_create_card_action),
                                            onActionClick = {
                                                if (!isPremium && cardsToday >= PremiumState.FREE_CARDS_PER_DAY) {
                                                    showPremiumGate = true
                                                    AnalyticsHelper.premiumGateShown(cardsToday)
                                                } else {
                                                    showCreateDialog = true
                                                }
                                            },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                            } else {
                                groupedCards.forEach { (date, cardsInDay) ->
                                    stickyHeader(key = "header_$date") {
                                        val friendlyDateLabel = when (date) {
                                            todayDateStr -> stringResource(R.string.card_list_date_today_prefix, date)
                                            yesterdayDateStr -> stringResource(R.string.card_list_date_yesterday_prefix, date)
                                            else -> date
                                        }
                                        val dayTotalKg = remember(cardsInDay) { cardsInDay.sumOf { it.totalWeight } }
                                        val formattedDayKg = remember(dayTotalKg) { numberFormat.format(dayTotalKg) }

                                        // Phân cấp rõ tiêu đề nhóm ngày + số lượng phiếu + tổng kg + hỗ trợ TalkBack heading
                                        // Surface với background opaque để khi cuộn không bị card bên dưới lọt qua chữ
                                        Surface(
                                            color = AppColors.Surface,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        top = AppDimensions.SpacingSm,
                                                        bottom = AppDimensions.SpacingXxs
                                                    )
                                                    .semantics { heading() },
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = friendlyDateLabel,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = AppColors.TextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(AppDimensions.CornerRadiusSm),
                                                    color = AppColors.SurfaceContainer
                                                ) {
                                                    Text(
                                                        text = stringResource(
                                                            R.string.card_list_day_summary_yield,
                                                            cardsInDay.size,
                                                            formattedDayKg
                                                        ),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = AppColors.TextSecondary,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(
                                                            horizontal = AppDimensions.SpacingXs,
                                                            vertical = 2.dp
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    items(
                                        items = cardsInDay,
                                        key = { it.id }
                                    ) { card ->
                                        CardItem(
                                            card = card,
                                            onClick = onCardClick,
                                            onDelete = onCardDelete,
                                            modifier = Modifier.animateItem(),
                                            onContinueWeigh = onContinueWeigh
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        CardListCreateFab(
            visible = fabVisible,
            onClick = {
                HapticUtil.tick(context)
                if (!isPremium && cardsToday >= PremiumState.FREE_CARDS_PER_DAY) {
                    showPremiumGate = true
                    AnalyticsHelper.premiumGateShown(cardsToday)
                } else {
                    showCreateDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(
                    end = AppDimensions.SpacingMd,
                    bottom = AppDimensions.BottomBarHeight + AppDimensions.SpacingMd
                )
        )
    }

    if (showCreateDialog) {
        CreateCardBottomSheet(
            ownerName = ownerName,
            suggestedVarieties = suggestedVarieties,
            mode = if (isTrader) CreateCardMode.TRADER else CreateCardMode.FARMER,
            onDismiss = { showCreateDialog = false },
            onCreate = { counterpartyName, counterpartyPhone, variety, season, moisture, price, deposit, cccd, impurityWeight, recordLocation ->
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
                    impurityWeight = impurityWeight,
                    recordLocation = recordLocation
                )
                if (!isPremium) {
                    PremiumState.incrementDailyCreated(context)
                }
                AnalyticsHelper.cardCreated(
                    role = if (isTrader) ROLE_TRADER else ROLE_FARMER,
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
            freeLimit = PremiumState.FREE_CARDS_PER_DAY,
            onUpgrade = {
                showPremiumGate = false
                navController.navigate(ROUTE_PREMIUM)
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
            onClearFilters = viewModel::clearFilters,
            onDismiss = { showFilterSheet = false },
            selectedQuickFilter = quickFilter,
            onSelectQuickFilter = viewModel::setQuickFilter
        )
    }
}