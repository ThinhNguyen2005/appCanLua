package com.GiaThinh.canlua.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.GiaThinh.canlua.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.data.model.WeightEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.GiaThinh.canlua.util.RiceCalculator
import com.GiaThinh.canlua.ui.component.weight.WeightMetricsCard
import com.GiaThinh.canlua.ui.component.pressableScale
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.viewmodel.CardViewModel
import com.GiaThinh.canlua.util.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightInputScreen(
    cardId: Long,
    navController: NavController,
    viewModel: CardViewModel = hiltViewModel(),
    onEditCard: () -> Unit = {},
    onDeleteCard: () -> Unit = {},
    onCreateQr: () -> Unit = {},
    onScanQr: () -> Unit = {}
) {
    val currentCard by viewModel.currentCard.collectAsState()
    val weightEntries by viewModel.weightEntries.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(cardId) { viewModel.loadCardById(cardId) }

    val card = currentCard ?: return

    // State cho việc tự thêm bảng nhập thủ công
    var manualTableCount by remember { mutableIntStateOf(0) }
    var showLockConfirmDialog by remember { mutableStateOf(false) }

    // Sắp xếp dữ liệu theo dạng Cột từ trên xuống (Column-Major)
    val tables = remember(weightEntries, manualTableCount) {
        organizeIntoTables(weightEntries, manualTableCount)
    }

    // === Single source of truth cho active table = pagerState.currentPage ===
    // Trước đây dùng `selectedTableIndex` riêng + 2 LaunchedEffect đồng bộ 2 chiều.
    // Khi user vuốt pager → currentPage đổi → state đổi → LaunchedEffect bên kia
    // gọi animateScrollToPage → interfere với gesture đang scroll → "không cuộn được".
    // Giờ chip click gọi explicit `scope.launch { animateScrollToPage }`, không có
    // state trung gian → không có loop.
    val pagerState = rememberPagerState(
        initialPage = (tables.size - 1).coerceAtLeast(0),
        pageCount = { tables.size }
    )
    val activeTableIndex = pagerState.currentPage

    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Collapsing animation đơn giản: fade theo offset thuần — KHÔNG còn co height
    // bằng `Modifier.layout` (vốn gây feedback loop scroll mất kiểm soát).
    // Card "Chỉ số cân" cuộn lên tự nhiên với LazyColumn, alpha mờ dần khi
    // user scroll xuống. Chỉ Draw phase, 0 layout recomputation.
    val scrollFraction by remember {
        derivedStateOf {
            // Sau ~200px scroll → fully faded. Ease-out để không hard transition.
            val firstOffset = if (lazyListState.firstVisibleItemIndex == 0) {
                lazyListState.firstVisibleItemScrollOffset.toFloat()
            } else {
                Float.MAX_VALUE
            }
            (firstOffset / 400f).coerceIn(0f, 1f)
        }
    }

    // === LAZY INIT (Performance Optimization) ===
    // Trì hoãn render hệ thống grid 25 ô × N bảng + FocusRequesters cho đến khi
    // page transition (250ms) đã hoàn tất + 150ms buffer.
    // Tại sao? Mở màn trùng với enter animation → main thread bị nghẽn:
    //   - organizeIntoTables() chạy đồng bộ
    //   - HorizontalPager measure tất cả pages
    //   - 25 FocusRequester per table được tạo (5×5 grid)
    // Trì hoãn 150ms cho phép enter animation chạy mượt 60fps trước khi
    // composition heavy work bắt đầu.
    var isAnimationFinished by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(150L)
        isAnimationFinished = true
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                shadowElevation = 3.dp,
                tonalElevation = 0.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // === LAYOUT STABILITY (Performance Optimization) ===
                        // heightIn(min) thay vì height() — khoá chiều cao tối thiểu để hệ thống
                        // không phải tính lại Measure/Layout pass khi IME (bàn phím ảo) bật/tắt
                        // hoặc khi nội dung con thay đổi alpha/scale.
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nút Quay lại
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.weight_input_back_content))
                    }

                    // Khu vực căn giữa tiêu đề dịch chuyển động
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        // 1. Tên thương lái (Mặc định) — fallback về tên nông dân nếu trader trống
                        val displayName = card.traderName.ifBlank { card.name }
                        Text(
                            text = displayName,
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer {
                                alpha = (1f - scrollFraction).coerceIn(0f, 1f)
                            }
                        )

                        // 2. Chỉ số cân thu gọn (Khi cuộn lên)
                        val totalWeightStr = "%.1f".format(card.totalWeight).replace(".", ",")
                        Text(
                            text = stringResource(R.string.weight_input_collapsed_title, totalWeightStr, card.bagCount),
                            style = TextStyle(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AppColors.RemainingHighlight // Auto-adapt dark/light
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer {
                                alpha = scrollFraction.coerceIn(0f, 1f)
                            }
                        )
                    }

                    IconButton(
                        onClick = {
                            if (card.isLocked) {
                                viewModel.toggleCardLock(card.id)
                                HapticUtil.confirm(context)
                            } else {
                                showLockConfirmDialog = true
                            }
                        }
                    ) {
                        if (card.isLocked) {
                            Icon(Icons.Default.Lock, stringResource(R.string.weight_input_unlock_content), tint = Color.Red)
                        } else {
                            Icon(Icons.Default.LockOpen, stringResource(R.string.weight_input_lock_content), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            // === CARD 1: Chỉ số cân ===
            // Refactor (2026-05): bỏ `Modifier.layout` co height — đó là root cause
            // bug scroll mất kiểm soát (feedback loop khi metricsOriginalHeight cập nhật
            // ngay trong layout pass + derivedStateOf đọc lại nó).
            // Giờ card cuộn tự nhiên với LazyColumn, alpha fade trong graphicsLayer
            // (Draw phase only — không trigger Layout recomputation).
            item(key = "metrics") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = (1f - scrollFraction * 0.6f).coerceIn(0f, 1f)
                        }
                ) {
                    WeightMetricsCard(
                        totalWeight = card.totalWeight,
                        bagWeight = card.bagWeight,
                        impurityWeight = card.impurityWeight,
                        moisturePercent = card.moisturePercent,
                        netWeight = card.netWeight,
                        pricePerKg = card.pricePerKg,
                        totalAmount = card.totalAmount,
                        bagCount = card.bagCount,
                        isLocked = card.isLocked,
                        onBagWeightChange = { viewModel.updateCardBagWeight(cardId, it) },
                        onImpurityWeightChange = { viewModel.updateCardImpurityWeight(cardId, it) },
                        onMoistureChange = { viewModel.updateCardMoisture(cardId, it) },
                        onPriceChange = { viewModel.updateCardPricePerKg(cardId, it) }
                    )
                }
            }

            // === Thanh chọn Bảng dữ liệu và Thêm Bảng Thủ Công [Chọn bảng nhập          + Thêm] ===
            item(key = "table_tabs") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.weight_input_select_table),
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            color = AppColors.TextPrimary
                        )

                        if (!card.isLocked) {
                            FilledTonalButton(
                                onClick = {
                                    manualTableCount++
                                    // Auto nhảy sang bảng vừa tạo — trigger animate
                                    // explicit thay vì set state (đã bỏ selectedTableIndex).
                                    scope.launch {
                                        pagerState.animateScrollToPage(tables.size)
                                    }
                                    HapticUtil.confirm(context)
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = AppColors.GreenPrimary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.weight_input_add_table), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }
                        }
                    }
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(tables.size) { index ->
                            val isSelected = index == activeTableIndex
                            val tabInteraction = remember { MutableInteractionSource() }
                            Button(
                                onClick = {
                                    // Click chip → explicit animate, không qua state
                                    // trung gian. Tránh re-trigger animate khi user
                                    // đang vuốt pager (gây nuốt gesture).
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                    HapticUtil.tick(context)
                                },
                                interactionSource = tabInteraction,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) AppColors.GreenPrimary else AppColors.CardBg,
                                    contentColor = if (isSelected) Color.White else AppColors.TextSecondary.copy(alpha = 0.7f)
                                ),
                                border = if (!isSelected) BorderStroke(1.dp, AppColors.Divider) else null,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .pressableScale(tabInteraction)
                            ) {
                                Text(
                                    text = stringResource(R.string.weight_input_table_tab, index + 1),
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // === Bảng dữ liệu hỗ trợ VUỐT NGANG (HorizontalPager) ===
            // === LAZY GATE ===
            // Chỉ render Pager + 25 GridCell + 25 FocusRequester sau khi
            // page transition hoàn tất (isAnimationFinished = true sau 150ms).
            // Trong lúc đó, hiện placeholder height ổn định (heightIn) để
            // tránh layout shift khi grid được mở màn.
            item(key = "selected_table_card") {
                if (isAnimationFinished) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { pageIndex ->
                        if (pageIndex < tables.size) {
                            val table = tables[pageIndex]
                            WeightTableCard(
                                tableIndex = pageIndex + 1,
                                tableData = table,
                                weightEntries = weightEntries,
                                tableIndexInList = pageIndex,
                                onWeightEntered = { weight ->
                                    viewModel.addWeightEntryDirectly(cardId, weight)
                                    HapticUtil.tick(context)
                                },
                                onWeightUpdated = { entry, newWeight ->
                                    val netWeight = RiceCalculator.calcNetWeight(
                                        rawWeight = newWeight,
                                        bagWeight = card.bagWeight,
                                        impurityWeight = 0.0,
                                        moisturePercent = card.moisturePercent
                                    )
                                    viewModel.updateWeightEntry(
                                        entry.copy(
                                            weight = newWeight,
                                            bagWeight = card.bagWeight,
                                            impurityWeight = 0.0,
                                            netWeight = netWeight
                                        )
                                    )
                                },
                                isLocked = card.isLocked
                            )
                        }
                    }
                } else {
                    // Placeholder — giữ chiều cao ổn định để grid xuất hiện mà không layout shift
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 360.dp)
                    )
                }
            }

            // === Tổng cột ===
            item(key = "col_totals") {
                val activeTable = tables.getOrNull(activeTableIndex) ?: emptyList()
                ColumnTotalsRow(calculateColumnTotals(activeTable))
            }

            // Bottom spacing
            item {
                Spacer(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(180.dp)
                )
            }
        }
    }

    if (showLockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLockConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AppColors.Error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.weight_input_lock_dialog_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Text(
                    text = stringResource(R.string.weight_input_lock_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.toggleCardLock(card.id)
                        HapticUtil.confirm(context)
                        showLockConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Error)
                ) {
                    Text(stringResource(R.string.weight_input_lock_confirm), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLockConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = AppColors.TextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = AppColors.Surface
        )
    }
}

// =============================================================================
// Weight Table (kept from original, cleaned up)
// =============================================================================

@Composable
private fun WeightTableCard(
    tableIndex: Int,
    tableData: List<List<Double?>>,
    weightEntries: List<WeightEntry>,
    tableIndexInList: Int,
    onWeightEntered: (Double) -> Unit,
    onWeightUpdated: (WeightEntry, Double) -> Unit,
    isLocked: Boolean
) {
    val tableTotal = tableData.flatten().filterNotNull().sum()
    val focusRequesters = remember { List(5) { List(5) { FocusRequester() } } }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.GreenPrimary, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.weight_input_table_title, tableIndex), style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White))
                Text("${"%.1f".format(tableTotal)} kg", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White))
            }

            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Column headers
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(5) { i ->
                        Text(
                            stringResource(R.string.weight_input_column_header, i + 1), modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium, color = AppColors.GreenPrimary
                        )
                    }
                }

                // Grid cells - Vẽ theo cấu trúc Cột từ trên xuống (Column-Major)
                val displayRows = List(5) { r -> if (r < tableData.size) tableData[r] else List(5) { null } }
                displayRows.forEachIndexed { rowIdx, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val displayCols = List(5) { c -> if (c < row.size) row[c] else null }
                        displayCols.forEachIndexed { colIdx, weightVal ->
                            // Công thức tính index theo Cột (Column-Major)
                            val entryIdx = (tableIndexInList * 25) + (colIdx * 5) + rowIdx
                            val existingEntry = if (entryIdx < weightEntries.size) weightEntries[entryIdx] else null

                            GridCell(
                                value = weightVal,
                                onValueEntered = { weight ->
                                    if (weightVal != null && existingEntry != null) onWeightUpdated(existingEntry, weight)
                                    else onWeightEntered(weight)
                                },
                                onNextFocus = {
                                    // Nhảy focus dọc từ trên xuống dưới theo cột, hết cột nhảy sang cột kế tiếp
                                    if (rowIdx < 4) {
                                        focusRequesters[rowIdx + 1][colIdx].requestFocus()
                                    } else if (colIdx < 4) {
                                        focusRequesters[0][colIdx + 1].requestFocus()
                                    }
                                },
                                focusRequester = focusRequesters[rowIdx][colIdx],
                                isLocked = isLocked,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseWeightInput(input: String): Double? {
    val digits = input.filter { it.isDigit() }
    if (digits.isEmpty()) return null
    return digits.toDoubleOrNull()?.div(10)
}

@Composable
private fun GridCell(
    value: Double?,
    onValueEntered: (Double) -> Unit,
    onNextFocus: () -> Unit,
    focusRequester: FocusRequester,
    isLocked: Boolean,
    modifier: Modifier
) {
    val displayValue = value?.let {
        "%.1f".format(it)
    } ?: ""
    var text by remember(value) { mutableStateOf(displayValue) }
    var isFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val appToast = com.GiaThinh.canlua.ui.feedback.LocalAppToast.current

    Box(
        modifier = modifier
            .heightIn(min = 60.dp) // Sử dụng chiều cao linh hoạt để hỗ trợ co giãn chữ hệ thống tốt hơn (Font Scale)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (value != null) AppColors.GreenSurface else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                1.dp,
                if (isFocused) AppColors.GreenPrimary else AppColors.Divider,
                RoundedCornerShape(8.dp)
            )
            .then(
                if (isLocked) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        appToast.warning(context.getString(R.string.weight_input_unlock_before_edit))
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isLocked) {
            BasicTextField(
                value = if (isFocused) text else displayValue,
                onValueChange = { input ->
                    if (input.all { it.isDigit() } && input.length <= 3) {
                        if (input.length > text.length) HapticUtil.textHandleMove(context)
                        text = input
                        if (input.length == 3) {
                            parseWeightInput(input)?.let {
                                if (it > 0) {
                                    text = ""
                                    onValueEntered(it)
                                    onNextFocus()
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentHeight(Alignment.CenterVertically)
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        val wasFocused = isFocused
                        isFocused = state.isFocused
                        if (state.isFocused && !wasFocused) {
                            text = ""
                        }
                        if (!state.isFocused && wasFocused && text.isNotEmpty()) {
                            val entered = text
                            text = ""
                            parseWeightInput(entered)?.let { if (it > 0) onValueEntered(it) }
                        }
                    },
                textStyle = TextStyle(
                    textAlign = TextAlign.Center, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, color = AppColors.TextPrimary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = {
                    val entered = text
                    text = ""
                    parseWeightInput(entered)?.let { if (it > 0) onValueEntered(it) }
                    onNextFocus()
                }),
                singleLine = true
            )
        } else {
            Text(displayValue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
        }
    }
}

@Composable
private fun ColumnTotalsRow(totals: List<Double>) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.GoldLight),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.weight_input_column_totals), style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.GoldDark))
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                totals.forEach { total ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp) // Tăng chiều cao để người dùng trung niên dễ nhìn ngoài đồng ruộng
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppColors.SurfaceContainer.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "%.1f".format(total),
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.TextPrimary)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// Logic helpers (Column-Major implementation)
// =============================================================================

private fun organizeIntoTables(entries: List<WeightEntry>, manualCount: Int): List<List<List<Double?>>> {
    val totalEntries = entries.size
    val calculatedNumTables = (totalEntries / 25) + 1
    val numTables = calculatedNumTables + manualCount
    
    val tables = mutableListOf<List<List<Double?>>>()
    
    for (t in 0 until numTables) {
        val tableGrid = MutableList(5) { MutableList<Double?>(5) { null } }
        for (c in 0 until 5) {
            for (r in 0 until 5) {
                // Công thức ánh xạ dữ liệu phẳng sang Column-Major (cột trước, hàng sau)
                val entryIdx = (t * 25) + (c * 5) + r
                if (entryIdx < totalEntries) {
                    tableGrid[r][c] = entries[entryIdx].weight
                }
            }
        }
        tables.add(tableGrid.map { it.toList() })
    }
    return tables
}

private fun calculateColumnTotals(tableData: List<List<Double?>>): List<Double> {
    val totals = MutableList(5) { 0.0 }
    tableData.forEach { row ->
        row.forEachIndexed { col, weight ->
            if (col in 0..4 && weight != null) {
                totals[col] += weight
            }
        }
    }
    return totals
}