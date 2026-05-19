package com.GiaThinh.canlua.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.GiaThinh.canlua.ui.component.SkeletonList
import com.GiaThinh.canlua.ui.component.SyncStatusPulse
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.util.isScrollingUp
import com.GiaThinh.canlua.ui.viewmodel.CardViewModel
import com.GiaThinh.canlua.ui.viewmodel.ProfileViewModel
import com.GiaThinh.canlua.ui.viewmodel.SyncViewModel
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen(
    navController: NavController,
    viewModel: CardViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    syncViewModel: SyncViewModel = hiltViewModel()
) {
    val cards by viewModel.cards.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedFilter by viewModel.selectedVarietyFilter.collectAsState()
    val availableVarieties by viewModel.availableVarieties.collectAsState()
    val selectedSeason by viewModel.selectedSeasonFilter.collectAsState()
    val availableSeasons by viewModel.availableSeasons.collectAsState()
    val profileState by profileViewModel.profile.collectAsState(initial = null)
    val syncStatus by syncViewModel.syncStatus.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var manualRefreshing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val fabVisible = listState.isScrollingUp() || cards.isEmpty()

    // Tự động tắt refreshing khi sync xong (hoặc hết 800ms giả lập để user thấy phong cách)
    LaunchedEffect(manualRefreshing, syncStatus) {
        if (manualRefreshing && syncStatus !is SyncStatus.Syncing) {
            delay(600)
            manualRefreshing = false
        }
    }

    val farmerName = profileState?.name ?: "Nông dân"

    val numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
    val today = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))

    // Calculate today's stats
    val todayCards = cards.filter { card ->
        val cardCal = Calendar.getInstance().apply { time = card.date }
        cardCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                cardCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
    }
    val todayTotalKg = todayCards.sumOf { it.totalWeight }
    val todayTotalAmount = todayCards.sumOf { it.totalAmount }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // === Summary Header ===
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.GreenSurface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Hôm nay: ${todayCards.size} phiếu",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary
                            )
                            Spacer(Modifier.width(10.dp))
                            SyncStatusPulse(
                                status = syncStatus,
                                showLabel = false
                            )
                        }
                        Text(
                            text = "${numberFormat.format(todayTotalKg)} kg",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Tổng thu",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                        Text(
                            text = "${numberFormat.format(todayTotalAmount)} đ",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.GreenPrimary
                        )
                    }
                }
            }

            // === Season Filter Chips — vai trò chính trong cách tổ chức dữ liệu theo mùa vụ ===
            if (availableSeasons.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSeason == null,
                        onClick = { viewModel.setSeasonFilter(null) },
                        label = { Text("Mọi vụ") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.GreenPrimary,
                            selectedLabelColor = AppColors.CardBg,
                            selectedLeadingIconColor = AppColors.CardBg
                        )
                    )
                    availableSeasons.forEach { season ->
                        FilterChip(
                            selected = selectedSeason == season,
                            onClick = {
                                viewModel.setSeasonFilter(
                                    if (selectedSeason == season) null else season
                                )
                            },
                            label = { Text(season) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.GreenPrimary,
                                selectedLabelColor = AppColors.CardBg
                            )
                        )
                    }
                }
            }

            // === Variety Filter Chips ===
            if (availableVarieties.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { viewModel.setVarietyFilter(null) },
                        label = { Text("Tất cả") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.GreenPrimary,
                            selectedLabelColor = AppColors.CardBg
                        )
                    )
                    availableVarieties.forEach { variety ->
                        FilterChip(
                            selected = selectedFilter == variety,
                            onClick = {
                                viewModel.setVarietyFilter(
                                    if (selectedFilter == variety) null else variety
                                )
                            },
                            label = { Text(variety) },
                            leadingIcon = {
                                if (selectedFilter == variety) {
                                    Icon(
                                        Icons.Outlined.Grass,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.GreenPrimary,
                                selectedLabelColor = AppColors.CardBg
                            )
                        )
                    }
                }
            }

            // === Card List ===
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SkeletonList(count = 3)
            }

            AnimatedVisibility(
                visible = !isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val pullState = rememberPullToRefreshState()
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
                        val progress = pullState.distanceFraction.coerceIn(0f, 1f)
                        PullToRefreshDefaults.Indicator(
                            state = pullState,
                            isRefreshing = manualRefreshing,
                            color = AppColors.GreenPrimary,
                            containerColor = AppColors.GreenSurface,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .graphicsLayer {
                                    scaleX = progress
                                    scaleY = progress
                                    alpha = progress
                                }
                        )
                    }
                ) {
                    if (cards.isEmpty()) {
                        // Empty state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.GreenSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Scale,
                                    contentDescription = null,
                                    tint = AppColors.GreenLight,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Chưa có phiếu cân nào",
                                style = MaterialTheme.typography.titleMedium,
                                color = AppColors.TextSecondary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Nhấn nút + để tạo phiếu cân mới",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextHint,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Group cards by date
                        val groupedCards = cards.groupBy { card ->
                            dateFormat.format(card.date)
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                        onClick = {
                                            navController.navigate("card_detail/${card.id}")
                                        },
                                        onDelete = {
                                            viewModel.deleteCard(card)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB — auto-hide khi scroll xuống đọc danh sách
        AnimatedVisibility(
            visible = fabVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = AppColors.GreenPrimary,
                contentColor = AppColors.CardBg,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tạo phiếu cân")
            }
        }
    }

    // Create Dialog
    if (showCreateDialog) {
        CreateCardDialog(
            farmerName = farmerName,
            onDismiss = { showCreateDialog = false },
            onCreate = { traderName, traderPhone, variety, season, moisture, price, deposit ->
                viewModel.createNewCard(
                    name = farmerName,
                    cccd = "",
                    traderName = traderName,
                    pricePerKg = price,
                    depositAmount = deposit,
                    riceVariety = variety,
                    moisturePercent = moisture,
                    seasonLabel = season,
                    traderPhone = traderPhone
                )
            }
        )
    }
}
