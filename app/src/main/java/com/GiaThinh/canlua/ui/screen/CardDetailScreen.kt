package com.GiaThinh.canlua.ui.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.WeightEntry
import com.GiaThinh.canlua.ui.component.CustomHeader
import com.GiaThinh.canlua.ui.component.RiceVarietyDropdown
import com.GiaThinh.canlua.ui.component.ThousandSeparatorTransformation
import com.GiaThinh.canlua.ui.component.detail.CardInfoCard
import com.GiaThinh.canlua.ui.component.detail.DetailSkeleton
import com.GiaThinh.canlua.ui.component.detail.FinancialSummaryCard
import com.GiaThinh.canlua.ui.component.detail.RemainingHeroCard
import com.GiaThinh.canlua.ui.component.detail.WeightSummaryCard
import com.GiaThinh.canlua.ui.component.rememberHeaderHeights
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.util.isScrollingUp
import com.GiaThinh.canlua.ui.viewmodel.CardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: Long,
    navController: NavController,
    viewModel: CardViewModel = hiltViewModel()
) {
    val currentCard by viewModel.currentCard.collectAsState()
    val weightEntries by viewModel.weightEntries.collectAsState()

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }

    LaunchedEffect(cardId) { viewModel.loadCardById(cardId) }

    val heights = rememberHeaderHeights()
    val scrollState = rememberLazyListState()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Pull-to-refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(550) // perceptible feedback even on cache hit
            viewModel.loadCardById(cardId)
            isRefreshing = false
        }
    }

    // Long-press → bottom sheet on bag cell
    var entryActionTarget by remember { mutableStateOf<WeightEntry?>(null) }
    var entryActionGlobalIndex by remember { mutableStateOf(0) }

    val collapseRangePx = with(density) { heights.expanded.toPx() }
    val collapseFraction by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset < 50) {
                0f
            } else {
                val scroll = if (scrollState.firstVisibleItemIndex == 0) {
                    scrollState.firstVisibleItemScrollOffset.toFloat()
                } else {
                    collapseRangePx
                }
                (scroll / collapseRangePx).coerceIn(0f, 1f)
            }
        }
    }

    val clampedFraction = collapseFraction.coerceIn(0f, 1f)
    val headerHeight = lerp(heights.expanded, heights.collapsed, clampedFraction)

    var showOverflow by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppColors.Surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            // Drive-style Extended FAB — chỉ hiện khi card đã load và user đang ở đầu trang
            currentCard?.let { c ->
                val fabVisible = scrollState.isScrollingUp() && entryActionTarget == null
                AnimatedVisibility(
                    visible = fabVisible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (c.isLocked) {
                                Toast.makeText(context, "Vui lòng mở khóa phiếu trước khi cân!", Toast.LENGTH_SHORT).show()
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                navController.navigate("weight_input/${cardId}")
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Outlined.Scale,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        text = {
                            Text(
                                "Cân lúa",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        },
                        expanded = true,
                        containerColor = if (c.isLocked) AppColors.GreenPrimary.copy(alpha = 0.45f) else AppColors.GreenPrimary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 12.dp
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        // === PLACEHOLDER CARD (Performance Optimization) ===
        // Khi currentCard == null (data đang load), header vẫn render với card rỗng.
        // → Frame 0 đã có header đầy đủ (back button, FAB, layout), không có cảm giác
        //   "header appear sau content". Khi data về, chỉ Text bên trong recompose
        //   (cheap), không mount lại view tree.
        val placeholderCard = remember {
            Card(
                id = 0L,
                name = "",
                date = Date(),
                traderName = ""
            )
        }
        val displayCard = currentCard ?: placeholderCard
        val isLoading = currentCard == null

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // === Content layer ===
            if (isLoading) {
                Column {
                    Spacer(Modifier.height(heights.expanded + 16.dp))
                    DetailSkeleton()
                }
            } else {
                val card = currentCard!!
                val tables = remember(weightEntries) { weightEntries.chunked(25) }
                val pagerState = rememberPagerState(
                    initialPage = 0,
                    pageCount = { tables.size.coerceAtLeast(1) }
                )
                var selectedTableIndex by remember(tables.size) { mutableStateOf(0) }
                val activeTableIndex = selectedTableIndex.coerceIn(0, (tables.size - 1).coerceAtLeast(0))

                LaunchedEffect(pagerState.currentPage) {
                    if (selectedTableIndex != pagerState.currentPage) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTableIndex = pagerState.currentPage
                    }
                }

                LaunchedEffect(activeTableIndex) {
                    if (pagerState.currentPage != activeTableIndex && activeTableIndex < tables.size) {
                        pagerState.animateScrollToPage(activeTableIndex)
                    }
                }

                val lastEntryTime = weightEntries.maxOfOrNull { it.timestamp }

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isRefreshing = true
                    },
                    state = pullState,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullState,
                            isRefreshing = isRefreshing,
                            color = AppColors.GreenPrimary,
                            containerColor = AppColors.GreenSurface,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = heights.collapsed + 8.dp)
                        )
                    }
                ) {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(heights.expanded + 16.dp)) }

                        // Card 0: Thông tin phiếu (thương lái · SDT · giống lúa · ngày · địa chỉ ruộng)
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                val createdLabel = remember(card.date) {
                                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(card.date)
                                }
                                CardInfoCard(
                                    traderName = card.traderName,
                                    traderPhone = card.traderPhone,
                                    riceVariety = card.riceVariety,
                                    seasonLabel = card.seasonLabel,
                                    createdDateLabel = createdLabel,
                                    fieldAddress = card.fieldAddress,
                                    hasCoordinates = card.latitude != null && card.longitude != null,
                                    onCallTrader = {
                                        val phone = card.traderPhone.trim()
                                        if (phone.isBlank()) return@CardInfoCard
                                        runCatching {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }.onFailure {
                                            Toast.makeText(context, "Không mở được ứng dụng gọi điện", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onOpenMap = {
                                        val lat = card.latitude
                                        val lon = card.longitude
                                        if (lat == null || lon == null) {
                                            Toast.makeText(context, "Chưa có tọa độ GPS", Toast.LENGTH_SHORT).show()
                                            return@CardInfoCard
                                        }
                                        val label = card.fieldAddress.ifBlank { card.name.ifBlank { "Ruộng lúa" } }
                                        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(label)})")
                                        runCatching {
                                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }.onFailure {
                                            Toast.makeText(context, "Không tìm thấy ứng dụng bản đồ", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onRefreshLocation = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.refreshFieldLocation(cardId)
                                        Toast.makeText(context, "Đang cập nhật vị trí…", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }

                        // Card 1: Khối lượng
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                WeightSummaryCard(
                                    totalWeight = card.totalWeight,
                                    bagCount = card.bagCount,
                                    bagWeight = card.bagWeight,
                                    impurityWeight = card.impurityWeight,
                                    netWeight = card.netWeight,
                                    numberFormat = numberFormat
                                )
                            }
                        }

                        // Card 2: Tài chính (gộp "Còn lại" vào cuối với style highlight vàng)
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                FinancialSummaryCard(
                                    pricePerKg = card.pricePerKg,
                                    totalAmount = card.totalAmount,
                                    depositAmount = card.depositAmount,
                                    paidAmount = card.paidAmount,
                                    remainingAmount = card.remainingAmount,
                                    numberFormat = numberFormat
                                )
                            }
                        }

                        // (Card "Còn lại" đã gộp vào FinancialSummaryCard ở trên)

                        // Card 4: Chi tiết bao cân
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                BagEntriesCard(
                                    bagCountTotal = weightEntries.size,
                                    tables = tables,
                                    pagerState = pagerState,
                                    activeTableIndex = activeTableIndex,
                                    isLocked = card.isLocked,
                                    onTableSelected = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedTableIndex = it
                                    },
                                    onAddFirstBag = {
                                        if (card.isLocked) {
                                            Toast.makeText(context, "Vui lòng mở khóa bảng trước khi chỉnh sửa!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            navController.navigate("weight_input/${cardId}")
                                        }
                                    },
                                    onEntryLongPress = { entry, globalIdx ->
                                        if (card.isLocked) {
                                            Toast.makeText(context, "Phiếu cân đang khóa!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            entryActionGlobalIndex = globalIdx
                                            entryActionTarget = entry
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("Xóa phiếu?") },
                        text = { Text("Hành động này sẽ xóa phiếu và các cân nặng liên quan.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showDeleteConfirm = false
                                scope.launch {
                                    viewModel.deleteCard(card)
                                    navController.popBackStack()
                                }
                            }) {
                                Text("Xóa", color = AppColors.Error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) { Text("Hủy") }
                        }
                    )
                }

                if (showEditDialog) {
                    EditCardDialog(
                        card = card,
                        onDismiss = { showEditDialog = false },
                        onConfirm = { updatedCard ->
                            viewModel.updateCard(updatedCard)
                            showEditDialog = false
                            Toast.makeText(context, "Đã cập nhật thông tin phiếu cân", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Bag entry action sheet (long-press)
                entryActionTarget?.let { entry ->
                    BagEntryActionSheet(
                        entry = entry,
                        globalIndex = entryActionGlobalIndex,
                        numberFormat = numberFormat,
                        onDismiss = { entryActionTarget = null },
                        onDelete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.deleteWeightEntry(entry)
                            entryActionTarget = null
                            Toast.makeText(context, "Đã xóa bao #$entryActionGlobalIndex", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // === HEADER LAYER (luôn render từ frame 0) ===
            // Đặt OUT of if/else → header tồn tại NGAY khi composable mount,
            // không có cảm giác "appear sau content". Khi data từ null → loaded,
            // chỉ Text bên trong recompose (rẻ), view tree không tear down/rebuild.
            CustomHeader(
                card = displayCard,
                collapseFraction = clampedFraction,
                lastEntryTime = if (isLoading) null else weightEntries.maxOfOrNull { it.timestamp },
                modifier = Modifier.height(headerHeight),
                onBack = { navController.popBackStack() },
                onAdd = {
                    if (isLoading) return@CustomHeader
                    if (displayCard.isLocked) {
                        Toast.makeText(context, "Vui lòng mở khóa bảng trước khi chỉnh sửa!", Toast.LENGTH_SHORT).show()
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate("weight_input/${cardId}")
                    }
                },
                showOverflow = showOverflow,
                onOverflowChange = { if (!isLoading) showOverflow = it },
                onEditCard = {
                    if (isLoading) return@CustomHeader
                    if (displayCard.isLocked) {
                        Toast.makeText(context, "Vui lòng mở khóa bảng trước khi chỉnh sửa!", Toast.LENGTH_SHORT).show()
                    } else {
                        showEditDialog = true
                    }
                },
                onDeleteCard = {
                    if (isLoading) return@CustomHeader
                    if (displayCard.isLocked) {
                        Toast.makeText(context, "Vui lòng mở khóa bảng trước khi chỉnh sửa!", Toast.LENGTH_SHORT).show()
                    } else {
                        showDeleteConfirm = true
                    }
                },
                onCreateQr = { if (!isLoading) navController.navigate("qr_generate/${displayCard.id}") },
                onScanQr = { if (!isLoading) navController.navigate("qr_scan") },
                onToggleLock = {
                    if (isLoading) return@CustomHeader
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleCardLock(displayCard.id)
                    Toast.makeText(
                        context,
                        if (displayCard.isLocked) "Đã mở khóa phiếu cân" else "Đã khóa phiếu cân",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bag entries card (selector + 5×5 row-major grid)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BagEntriesCard(
    bagCountTotal: Int,
    tables: List<List<WeightEntry>>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    activeTableIndex: Int,
    isLocked: Boolean,
    onTableSelected: (Int) -> Unit,
    onAddFirstBag: () -> Unit,
    onEntryLongPress: (WeightEntry, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Scale,
                        contentDescription = null,
                        tint = AppColors.GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Chi tiết các bao cân",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }
                Surface(
                    color = AppColors.GreenSurface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "$bagCountTotal bao",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.GreenPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (bagCountTotal == 0) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Chưa có bao cân nào được nhập.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextHint,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = onAddFirstBag,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, AppColors.GreenPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.GreenPrimary)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Thêm bao cân đầu tiên", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                // Selector chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tables.size) { index ->
                        val isActive = index == activeTableIndex
                        val borderWidth by animateDpAsState(
                            targetValue = if (isActive) 1.5.dp else 0.dp,
                            animationSpec = tween(180),
                            label = "chip_border"
                        )
                        Surface(
                            onClick = { onTableSelected(index) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isActive) AppColors.GreenSurface else AppColors.SurfaceContainer,
                            border = if (isActive) BorderStroke(borderWidth, AppColors.GreenPrimary) else null,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = "Bảng ${index + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (isActive) AppColors.GreenPrimary else AppColors.TextSecondary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 8.dp,
                    pageSize = PageSize.Fill,
                    modifier = Modifier.fillMaxWidth()
                ) { pageIndex ->
                    if (pageIndex < tables.size) {
                        val tableEntries = tables[pageIndex]
                        BagGrid(
                            entries = tableEntries,
                            globalOffset = pageIndex * 25,
                            isLocked = isLocked,
                            onEntryLongPress = onEntryLongPress
                        )
                    }
                }
            }
        }
    }
}

/**
 * Row-major 5×5 grid: index 0..24 = (row=idx/5, col=idx%5).
 * Đọc trái-phải, trên-xuống — tự nhiên với người dùng.
 */
@Composable
private fun BagGrid(
    entries: List<WeightEntry>,
    globalOffset: Int,
    isLocked: Boolean,
    onEntryLongPress: (WeightEntry, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (r in 0 until 5) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (c in 0 until 5) {
                    val entryIdx = r * 5 + c
                    val entry = entries.getOrNull(entryIdx)
                    val globalIndex = globalOffset + entryIdx + 1
                    if (entry != null) {
                        BagCell(
                            indexLabel = "#$globalIndex",
                            weight = entry.weight,
                            isLocked = isLocked,
                            onLongPress = { onEntryLongPress(entry, globalIndex) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BagCell(
    indexLabel: String,
    weight: Double,
    isLocked: Boolean,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val weightText = if (weight % 1.0 == 0.0) "%.0f".format(weight) else "%.1f".format(weight)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.GreenSurface)
            .combinedClickable(
                onClick = { /* reserved for future quick edit */ },
                onLongClick = if (!isLocked) {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                } else null
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = indexLabel,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.GreenPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = weightText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────────
// Bag entry action sheet — long-press → ModalBottomSheet
// ───────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BagEntryActionSheet(
    entry: WeightEntry,
    globalIndex: Int,
    numberFormat: NumberFormat,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var confirmDelete by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.CardBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.Divider) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.GreenSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Scale,
                        contentDescription = null,
                        tint = AppColors.GreenPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        "Bao cân #$globalIndex",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        "${numberFormat.format(entry.weight)} kg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
            }

            HorizontalDivider(color = AppColors.Divider)

            // Action: Delete
            Surface(
                onClick = { confirmDelete = true },
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = AppColors.Error
                    )
                    Text(
                        "Xóa bao cân này",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Error
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Xóa bao #$globalIndex?") },
            text = { Text("Hành động này không thể hoàn tác. Bao ${numberFormat.format(entry.weight)} kg sẽ bị xóa khỏi bảng.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Xóa", color = AppColors.Error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Hủy") }
            },
            containerColor = AppColors.CardBg
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Edit dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = AppColors.TextHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.GreenPrimary,
                unfocusedBorderColor = AppColors.Divider
            )
        )
    }
}

@Composable
fun EditCardDialog(
    card: Card,
    onDismiss: () -> Unit,
    onConfirm: (Card) -> Unit
) {
    var farmerName by remember { mutableStateOf(card.name) }
    var traderName by remember { mutableStateOf(card.traderName) }
    var riceVariety by remember { mutableStateOf(card.riceVariety) }
    var seasonLabel by remember { mutableStateOf(card.seasonLabel) }
    var moisturePercent by remember { mutableStateOf(if (card.moisturePercent > 0) card.moisturePercent.toString() else "") }
    var pricePerKg by remember { mutableStateOf(if (card.pricePerKg > 0) "%.0f".format(card.pricePerKg) else "") }
    var depositAmount by remember { mutableStateOf(if (card.depositAmount > 0) "%.0f".format(card.depositAmount) else "") }
    var paidAmount by remember { mutableStateOf(if (card.paidAmount > 0) "%.0f".format(card.paidAmount) else "") }

    val isValid = farmerName.isNotBlank() && traderName.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = AppColors.CardBg,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.GreenSurface)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Sửa Phiếu Cân",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.GreenPrimary
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FormTextField(
                        value = farmerName,
                        onValueChange = { farmerName = it },
                        label = "Tên nông dân *",
                        placeholder = "Nhập tên nông dân"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1.1f)) {
                            RiceVarietyDropdown(
                                selected = riceVariety,
                                onSelect = { riceVariety = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        FormTextField(
                            value = seasonLabel,
                            onValueChange = { seasonLabel = it },
                            label = "Vụ mùa",
                            placeholder = "VD: Đông Xuân 26",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    FormTextField(
                        value = traderName,
                        onValueChange = { traderName = it },
                        label = "Tên thương lái *",
                        placeholder = "Nhập tên thương lái mua lúa"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = moisturePercent,
                            onValueChange = { moisturePercent = it },
                            label = { Text("Độ ẩm (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = pricePerKg,
                            onValueChange = { pricePerKg = it },
                            label = { Text("Đơn giá (đ/kg)") },
                            visualTransformation = ThousandSeparatorTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = depositAmount,
                            onValueChange = { depositAmount = it },
                            label = { Text("Tiền cọc (đ)") },
                            visualTransformation = ThousandSeparatorTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = paidAmount,
                            onValueChange = { paidAmount = it },
                            label = { Text("Đã trả (đ)") },
                            visualTransformation = ThousandSeparatorTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = AppColors.Divider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Huỷ", color = AppColors.TextSecondary)
                    }
                    Button(
                        onClick = {
                            val updated = card.copy(
                                name = farmerName.trim(),
                                traderName = traderName.trim(),
                                riceVariety = riceVariety,
                                seasonLabel = seasonLabel.trim(),
                                moisturePercent = moisturePercent.toDoubleOrNull() ?: 0.0,
                                pricePerKg = pricePerKg.toDoubleOrNull() ?: 0.0,
                                depositAmount = depositAmount.toDoubleOrNull() ?: 0.0,
                                paidAmount = paidAmount.toDoubleOrNull() ?: 0.0
                            )
                            onConfirm(updated)
                        },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Lưu thay đổi", color = Color.White)
                    }
                }
            }
        }
    }
}