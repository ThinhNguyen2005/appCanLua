package com.giathinh.canlua.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.ui.component.CardItem
import com.giathinh.canlua.ui.component.CardListSkeleton
import com.giathinh.canlua.ui.component.CreateCardBottomSheet
import com.giathinh.canlua.ui.component.CreateCardMode
import com.giathinh.canlua.ui.component.TransitionSafeWrapper
import com.giathinh.canlua.ui.component.cardlist.CardListEmptyState
import com.giathinh.canlua.ui.component.cardlist.CardListSummaryCard
import com.giathinh.canlua.ui.component.cardlist.DeleteCardConfirmDialog
import com.giathinh.canlua.ui.feedback.LocalAppToast
import com.giathinh.canlua.ui.navigation.BottomNavItem
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.util.isScrollingUp
import com.giathinh.canlua.ui.viewmodel.CardListViewModel
import com.giathinh.canlua.ui.viewmodel.DeleteCardEvent
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val HOME_MAX_CARDS = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController
) {
    com.giathinh.canlua.util.TrackScreenRender("home")
    val viewModel: CardListViewModel = hiltViewModel()

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    TransitionSafeWrapper(
        isDataReady = !isLoading,
        skeletonContent = { CardListSkeleton() }
    ) {
        HomeScreenContent(
            navController = navController,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    navController: NavController,
    viewModel: CardListViewModel
) {
    val allCards by viewModel.cards.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val suggestedVarieties by viewModel.suggestedRiceVarieties.collectAsStateWithLifecycle()
    val appToast = LocalAppToast.current

    // 5 phiếu gần nhất (all cards đã sắp xếp theo ngày desc từ DB)
    val recentCards = remember(allCards) { allCards.take(HOME_MAX_CARDS) }
    val hasMoreCards = remember(allCards) { allCards.size > HOME_MAX_CARDS }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<Card?>(null) }

    val listState = rememberLazyListState()
    val scrollingUp = listState.isScrollingUp()
    val fabVisible by remember { derivedStateOf { scrollingUp || recentCards.isEmpty() } }

    LaunchedEffect(fabVisible) {
        com.giathinh.canlua.ui.util.BottomBarVisibility.set(fabVisible)
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { com.giathinh.canlua.ui.util.BottomBarVisibility.reset() }
    }

    LaunchedEffect(viewModel) {
        viewModel.deleteEvents.collect { event ->
            when (event) {
                is DeleteCardEvent.Success -> appToast.success("Đã xoá phiếu ${event.cardName}")
                DeleteCardEvent.Error -> appToast.error("Không xoá được phiếu. Thử lại sau")
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")) }

    val todayCards = remember(allCards) {
        val todayCal = Calendar.getInstance()
        allCards.filter { card ->
            val cardCal = Calendar.getInstance().apply { time = card.date }
            cardCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) &&
                    cardCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
        }
    }

    val todayTotalKg = remember(todayCards) { todayCards.sumOf { it.totalWeight } }
    val todayTotalAmount = remember(todayCards) { todayCards.sumOf { it.totalAmount } }
    val todayAvgPrice = remember(todayCards) {
        val priced = todayCards.filter { it.pricePerKg > 0 }
        if (priced.isEmpty()) null else priced.map { it.pricePerKg }.average()
    }
    val todayBagCount = remember(todayCards) {
        val total = todayCards.sumOf { it.bagCount }
        if (total <= 0) null else total
    }

    val onCardClick = remember(navController) {
        { id: Long -> navController.navigate("card_detail/$id") }
    }
    val onCardDelete: (Card) -> Unit = remember {
        { card ->
            cardToDelete = card
            showDeleteConfirmDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            targetState = isLoading,
            animationSpec = tween(300),
            label = "home_crossfade"
        ) { skeleton ->
            if (skeleton) {
                CardListSkeleton(count = 3, listState = listState)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 104.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "summary_card") {
                        CardListSummaryCard(
                            cardCount = todayCards.size,
                            totalKg = todayTotalKg,
                            totalAmount = todayTotalAmount,
                            avgPricePerKg = todayAvgPrice,
                            bagCount = todayBagCount
                        )
                    }

                    if (recentCards.isEmpty()) {
                        item(key = "empty") { CardListEmptyState() }
                    } else {
                        item(key = "recent_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Phiếu cân gần nhất",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                                if (hasMoreCards) {
                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                navController.navigate(BottomNavItem.HISTORY.route) {
                                                    launchSingleTop = true
                                                }
                                            }
                                            .padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Xem tất cả",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = AppColors.GreenPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                            contentDescription = null,
                                            tint = AppColors.GreenPrimary,
                                            modifier = Modifier.padding(top = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        items(
                            items = recentCards,
                            key = { it.id }
                        ) { card ->
                            CardItem(
                                card = card,
                                onClick = onCardClick,
                                onDelete = onCardDelete
                            )
                        }

                        if (hasMoreCards) {
                            item(key = "view_more_footer") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate(BottomNavItem.HISTORY.route) {
                                                launchSingleTop = true
                                            }
                                        }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Xem toàn bộ ${allCards.size} phiếu cân",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AppColors.GreenPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                        contentDescription = null,
                                        tint = AppColors.GreenPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB Tạo phiếu cân
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
            ownerName = "Nông dân",
            suggestedVarieties = suggestedVarieties,
            mode = CreateCardMode.FARMER,
            onDismiss = { showCreateDialog = false },
            onCreate = { counterpartyName, counterpartyPhone, variety, season, moisture, price, deposit, cccd, impurityWeight, recordLocation ->
                viewModel.createNewCard(
                    name = "Nông dân",
                    cccd = cccd,
                    traderName = counterpartyName,
                    pricePerKg = price,
                    depositAmount = deposit,
                    riceVariety = variety,
                    moisturePercent = moisture,
                    seasonLabel = season,
                    traderPhone = counterpartyPhone,
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
}
