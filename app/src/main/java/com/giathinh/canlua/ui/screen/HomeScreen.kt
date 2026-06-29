package com.giathinh.canlua.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.giathinh.canlua.R
import com.giathinh.canlua.data.model.AppUiMode
import com.giathinh.canlua.ui.component.CardItem
import com.giathinh.canlua.ui.component.CreateCardBottomSheet
import com.giathinh.canlua.ui.component.CreateCardMode
import com.giathinh.canlua.ui.component.SkeletonList
import com.giathinh.canlua.ui.component.cardlist.CardListSummaryCard
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.theme.LocalAppUiMode
import com.giathinh.canlua.ui.viewmodel.CardListViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: CardListViewModel = hiltViewModel()
) {
    com.giathinh.canlua.util.TrackScreenRender("home_screen")

    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val uiMode = LocalAppUiMode.current

    if (uiMode == AppUiMode.SIMPLE) {
        HomeScreenSimple(
            navController = navController,
            viewModel = viewModel,
            cards = cards,
            isLoading = isLoading
        )
    } else {
        HomeScreenStandard(
            navController = navController,
            viewModel = viewModel,
            cards = cards,
            isLoading = isLoading
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenStandard(
    navController: NavController,
    viewModel: CardListViewModel,
    cards: List<com.giathinh.canlua.data.model.Card>,
    isLoading: Boolean
) {
    val suggestedVarieties by viewModel.suggestedRiceVarieties.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    val farmerLabel = stringResource(R.string.farmer)
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }
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
        if (pricedCards.isEmpty()) null else pricedCards.map { it.pricePerKg }.average()
    }
    val todayBagCount = remember(todayCards) {
        val total = todayCards.sumOf { it.bagCount }
        if (total <= 0) null else total
    }

    val recentCards = remember(cards) {
        cards.take(5)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp)
    ) { _ ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Summary Card
            item {
                CardListSummaryCard(
                    cardCount = todayCards.size,
                    totalKg = todayTotalKg,
                    totalAmount = todayTotalAmount,
                    avgPricePerKg = todayAvgPricePerKg,
                    bagCount = todayBagCount
                )
            }

            // 2. Quick Action Grid
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_quick_actions),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionCard(
                            title = stringResource(R.string.home_action_create_card),
                            icon = Icons.Default.Add,
                            backgroundColor = AppColors.GreenSurface,
                            iconColor = AppColors.GreenPrimary,
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            title = stringResource(R.string.home_action_history),
                            icon = Icons.Default.History,
                            backgroundColor = AppColors.BlueSurface,
                            iconColor = AppColors.Blue,
                            onClick = { navController.navigate("history") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionCard(
                            title = stringResource(R.string.home_action_statistics),
                            icon = Icons.Default.BarChart,
                            backgroundColor = AppColors.OrangeSurface,
                            iconColor = AppColors.Orange,
                            onClick = { navController.navigate("statistics") },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            title = stringResource(R.string.home_action_settings),
                            icon = Icons.Default.Settings,
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                            iconColor = AppColors.TextSecondary,
                            onClick = { navController.navigate("settings") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Recent 5 Cards list
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_recent_cards),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    TextButton(onClick = { navController.navigate("history") }) {
                        Text(
                            text = stringResource(R.string.home_view_all),
                            color = AppColors.GreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    SkeletonList(count = 2)
                }
            } else if (recentCards.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_empty_state),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            } else {
                items(
                    items = recentCards,
                    key = { it.id }
                ) { card ->
                    CardItem(
                        card = card,
                        onClick = { id -> navController.navigate("card_detail/$id") },
                        onDelete = { /* Handled on History Screen, no delete on Home standard for safety/cleanliness */ }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCardBottomSheet(
            ownerName = farmerLabel,
            suggestedVarieties = suggestedVarieties,
            onDismiss = { showCreateDialog = false },
            onCreate = { counterpartyName, counterpartyPhone, variety, season, moisture, price, deposit, cccd, impurityWeight, recordLocation ->
                viewModel.createNewCard(
                    name = farmerLabel,
    navController: NavController,
    viewModel: CardListViewModel,
    cards: List<com.giathinh.canlua.data.model.Card>,
    isLoading: Boolean
) {
    val suggestedVarieties by viewModel.suggestedRiceVarieties.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }

    val todayCards = remember(cards) {
        val todayCal = Calendar.getInstance()
        cards.filter { card ->
            val cardCal = Calendar.getInstance().apply { time = card.date }
            cardCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) &&
                    cardCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
        }
    }
    
    val todayTotalKg = remember(todayCards) { todayCards.sumOf { it.totalWeight } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp)
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Huge Metric display
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.GreenSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TỔNG CÂN HÔM NAY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.GreenDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${numberFormat.format(todayTotalKg)} kg",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.GreenPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Số phiếu: ${todayCards.size} phiếu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Large Action Buttons
            SimpleLargeButton(
                text = "TẠO PHIẾU CÂN MỚI",
                icon = Icons.Default.Add,
                color = AppColors.GreenPrimary,
                onClick = { showCreateDialog = true }
            )

            SimpleLargeButton(
                text = "XEM LỊCH SỬ PHIẾU",
                icon = Icons.Default.History,
                color = AppColors.Blue,
                onClick = { navController.navigate("history") }
            )

            SimpleLargeButton(
                text = "XEM THỐNG KÊ MÙA VỤ",
                icon = Icons.Default.BarChart,
                color = AppColors.Orange,
                onClick = { navController.navigate("statistics") }
            )

            SimpleLargeButton(
                text = "CÀI ĐẶT HỆ THỐNG",
                icon = Icons.Default.Settings,
                color = AppColors.TextSecondary,
                onClick = { navController.navigate("settings") }
            )
        }
    }

    if (showCreateDialog) {
        CreateCardBottomSheet(
            ownerName = "Nông dân",
            suggestedVarieties = suggestedVarieties,
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
                    recordLocation = recordLocation,
                    onCreated = { cardId ->
                        navController.navigate("card_detail/$cardId")
                        showCreateDialog = false
                    }
                )
            }
        )
    }
}

// Support components

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SimpleLargeButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Left
            )
        }
    }
}
