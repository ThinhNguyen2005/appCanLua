package com.giathinh.canlua.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.giathinh.canlua.ui.component.CardItem
import com.giathinh.canlua.ui.component.CreateCardBottomSheet
import com.giathinh.canlua.ui.component.CreateCardMode
import com.giathinh.canlua.ui.component.CardListSkeleton
import com.giathinh.canlua.ui.component.cardlist.CardListEmptyState
import com.giathinh.canlua.ui.component.cardlist.CardListFilterBar
import com.giathinh.canlua.ui.component.cardlist.CardListFilterSheet
import com.giathinh.canlua.ui.component.cardlist.CardListSummaryCard
import com.giathinh.canlua.ui.component.cardlist.DeleteCardConfirmDialog
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.util.isScrollingUp
import com.giathinh.canlua.ui.viewmodel.CardListViewModel
import com.giathinh.canlua.ui.viewmodel.DeleteCardEvent
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import com.giathinh.canlua.ui.component.TransitionSafeWrapper
import com.giathinh.canlua.ui.feedback.LocalAppToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen(
    navController: NavController
) {
    com.giathinh.canlua.util.TrackScreenRender("card_list")
    val viewModel: CardListViewModel = hiltViewModel()

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isDataReady = !isLoading

    TransitionSafeWrapper(
        isDataReady = isDataReady,
        skeletonContent = { CardListSkeleton() }
    ) {
        CardListScreenContent(
            navController = navController,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreenContent(
    navController: NavController,
    viewModel: CardListViewModel
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val showSkeleton = isLoading
    
    val selectedFilter by viewModel.selectedVarietyFilter.collectAsStateWithLifecycle()
    val availableVarieties by viewModel.availableVarieties.collectAsStateWithLifecycle()
    val suggestedVarieties by viewModel.suggestedRiceVarieties.collectAsStateWithLifecycle()
    val selectedSeason by viewModel.selectedSeasonFilter.collectAsStateWithLifecycle()
    val availableSeasons by viewModel.availableSeasons.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val appToast = LocalAppToast.current
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<com.giathinh.canlua.data.model.Card?>(null) }
    var manualRefreshing by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val scrollingUp = listState.isScrollingUp()
    val fabVisible by remember { derivedStateOf { scrollingUp || cards.isEmpty() } }

    LaunchedEffect(fabVisible) {
        com.giathinh.canlua.ui.util.BottomBarVisibility.set(fabVisible)
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { com.giathinh.canlua.ui.util.BottomBarVisibility.reset() }
    }

    LaunchedEffect(manualRefreshing) {
        if (manualRefreshing) {
            delay(500)
            manualRefreshing = false
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.deleteEvents.collect { event ->
            when (event) {
                is DeleteCardEvent.Success -> appToast.success("Đã xoá phiếu ${event.cardName}")
                DeleteCardEvent.Error -> appToast.error("Không xoá được phiếu. Thử lại sau")
            }
        }
    }

    val ownerName = "Nông dân"
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")) }

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
    
    val todayAvgPricePerKg = remember(todayCards) {
        val pricedCards = todayCards.filter { it.pricePerKg > 0 }
        if (pricedCards.isEmpty()) null
        else pricedCards.map { it.pricePerKg }.average()
    }
    
    val todayBagCount = remember(todayCards) {
        val total = todayCards.sumOf { it.bagCount }
        if (total <= 0) null else total
    }

    val onCardClick = remember(navController) {
        { id: Long -> navController.navigate("card_detail/$id") }
    }
    val onCardDelete: (com.giathinh.canlua.data.model.Card) -> Unit = remember {
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
                                    avgPricePerKg = todayAvgPricePerKg,
                                    bagCount = todayBagCount
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
                                            title = stringResource(com.giathinh.canlua.R.string.card_list_empty_filter_title),
                                            subtitle = stringResource(com.giathinh.canlua.R.string.card_list_empty_filter_subtitle)
                                        )
                                    } else {
                                        CardListEmptyState()
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
                onClick = { showCreateDialog = true },
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

    if (showCreateDialog) {
        CreateCardBottomSheet(
            ownerName = ownerName,
            suggestedVarieties = suggestedVarieties,
            mode = CreateCardMode.FARMER,
            onDismiss = { showCreateDialog = false },
            onCreate = { counterpartyName, counterpartyPhone, variety, season, moisture, price, deposit, cccd, impurityWeight, recordLocation ->
                val cardName = ownerName
                val cardTraderName = counterpartyName
                val cardTraderPhone = counterpartyPhone
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
