package com.GiaThinh.canlua.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.graphics.graphicsLayer

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
import com.GiaThinh.canlua.util.TrackScreenRender
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    TrackScreenRender("weight_input")
    
    // Sử dụng collectAsStateWithLifecycle để tự động giải phóng tài nguyên khi chạy nền
    val currentCard by viewModel.currentCard.collectAsStateWithLifecycle()
    val weightEntries by viewModel.weightEntries.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(cardId) { viewModel.loadCardById(cardId) }

    // === KHAI BÁO STATE (MANDATORY: Đặt trước check null để bảo toàn Composition Tree) ===

    var manualTableCount by remember { mutableIntStateOf(0) }
    var showLockConfirmDialog by remember { mutableStateOf(false) }

    val tables = remember(weightEntries, manualTableCount) {
        organizeIntoTables(weightEntries, manualTableCount)
    }

    val liveTotalWeight = remember(weightEntries) { weightEntries.sumOf { it.weight } }
    val liveBagCount = weightEntries.size
    val liveNetWeight = remember(
        liveTotalWeight,
        liveBagCount,
        currentCard?.bagWeight,
        currentCard?.impurityWeight,
        currentCard?.moisturePercent,
        currentCard?.impurityIsPercent,
        currentCard?.bagMethodIsSampling,
        currentCard?.bagSampleCount,
        currentCard?.bagSampleTotalWeight
    ) {
        RiceCalculator.calcNetWeightWithModes(
            totalRaw = liveTotalWeight,
            bagCount = liveBagCount,
            bagWeight = currentCard?.bagWeight ?: 0.0,
            bagMethodIsSampling = currentCard?.bagMethodIsSampling ?: false,
            bagSampleCount = currentCard?.bagSampleCount ?: 0,
            bagSampleTotalWeight = currentCard?.bagSampleTotalWeight ?: 0.0,
            impurityValue = currentCard?.impurityWeight ?: 0.0,
            impurityIsPercent = currentCard?.impurityIsPercent ?: false,
            moisturePercent = currentCard?.moisturePercent ?: 0.0
        )
    }
    val liveTotalAmount = liveNetWeight * (currentCard?.pricePerKg ?: 0.0)

    val lazyListState = rememberLazyListState()



    // Collapse animation fade hoàn toàn sau ~400px scroll
    // Nếu firstVisibleItemIndex > 0 → card đã cuộn khỏi màn hình → fade = 1.0f ngay
    val scrollFraction by remember {
        derivedStateOf {
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

    // === CHECK NULL DỮ LIỆU (Đặt SAU khi các state remember đã được đăng ký) ===
    val card = currentCard
    if (card == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Surface),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AppColors.GreenPrimary)
        }
        return
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
                        val totalWeightStr = "%.1f".format(liveTotalWeight).replace(".", ",")
                        Text(
                            text = stringResource(R.string.weight_input_collapsed_title, totalWeightStr, liveBagCount),
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
                        totalWeight = liveTotalWeight,
                        bagWeight = card.bagWeight,
                        impurityWeight = card.impurityWeight,
                        moisturePercent = card.moisturePercent,
                        netWeight = liveNetWeight,
                        pricePerKg = card.pricePerKg,
                        totalAmount = liveTotalAmount,
                        bagCount = liveBagCount,
                        isLocked = card.isLocked,
                        onBagWeightChange = { viewModel.updateCardBagWeight(cardId, it) },
                        onImpurityWeightChange = { viewModel.updateCardImpurityWeight(cardId, it) },
                        onMoistureChange = { viewModel.updateCardMoisture(cardId, it) },
                        onPriceChange = { viewModel.updateCardPricePerKg(cardId, it) },
                        impurityIsPercent = card.impurityIsPercent
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
                }
            }

            if (isAnimationFinished) {
                tables.forEachIndexed { pageIndex, table ->
                    item(key = "weight_table_$pageIndex") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            WeightTableCard(
                                tableIndex = pageIndex + 1,
                                tableData = table,
                                weightEntries = weightEntries,
                                tableIndexInList = pageIndex,
                                onNeedNextTable = { manualTableCount++ },
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
                                isLocked = card.isLocked,
                                weightInputMode = card.weightInputMode
                            )
                            ColumnTotalsRow(calculateColumnTotals(table))
                        }
                    }
                }
            } else {
                item(key = "weight_table_placeholder") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 360.dp)
                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(32.dp)
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
    onNeedNextTable: () -> Unit,
    onWeightEntered: (Double) -> Unit,
    onWeightUpdated: (WeightEntry, Double) -> Unit,
    isLocked: Boolean,
    weightInputMode: String = "SMALL"
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
                                    if (rowIdx < 4) {
                                        focusRequesters[rowIdx + 1][colIdx].requestFocus()
                                    } else if (colIdx < 4) {
                                        focusRequesters[0][colIdx + 1].requestFocus()
                                    } else {
                                        onNeedNextTable()
                                    }
                                },
                                focusRequester = focusRequesters[rowIdx][colIdx],
                                isLocked = isLocked,
                                weightInputMode = weightInputMode,
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

private fun maxInputDigits(mode: String): Int = if (mode == "LARGE") 4 else 3

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun GridCell(
    value: Double?,
    onValueEntered: (Double) -> Unit,
    onNextFocus: () -> Unit,
    focusRequester: FocusRequester,
    isLocked: Boolean,
    weightInputMode: String,
    modifier: Modifier
) {
    val displayValue = value?.let {
        "%.1f".format(it)
    } ?: ""
    var text by remember(value) { mutableStateOf(displayValue) }
    var isFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val appToast = com.GiaThinh.canlua.ui.feedback.LocalAppToast.current

    // Auto-lift: khi cell focus, kéo cell lên trên bàn phím để không bị che.
    // bringIntoViewRequester phối hợp với .imePadding() của LazyColumn — IME pad
    // đảm bảo view rút lại, bringIntoView scroll thêm để cell nằm trong vùng visible.
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    // Viền sắc nét cho điều kiện ngoài trời nắng / màn hình giảm sáng:
    //  - Empty: nền trắng (CardBg) + viền xám đậm DividerStrong → cell rõ ranh giới
    //  - Filled: nền GreenSurface + viền GreenPrimary 1.5dp
    //  - Focused: viền GreenPrimary 2dp (dày hơn để nhận biết ô đang gõ)
    val borderColor = when {
        isFocused -> AppColors.GreenPrimary
        value != null -> AppColors.GreenPrimary
        else -> AppColors.DividerStrong
    }
    val borderWidth = if (isFocused) 2.dp else 1.5.dp
    val cellBg = if (value != null) AppColors.GreenSurface else AppColors.CardBg

    Box(
        modifier = modifier
            .heightIn(min = 60.dp)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(cellBg)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .bringIntoViewRequester(bringIntoView)
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
                    val maxLen = maxInputDigits(weightInputMode)
                    if (input.all { it.isDigit() } && input.length <= maxLen) {
                        if (input.length > text.length) HapticUtil.textHandleMove(context)
                        text = input
                        if (input.length == maxLen) {
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
                            // Đẩy cell vào tầm nhìn — chờ IME mở (~250ms) rồi mới scroll.
                            scope.launch {
                                delay(280L)
                                runCatching { bringIntoView.bringIntoView() }
                            }
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
        border = BorderStroke(1.5.dp, AppColors.GoldDark.copy(alpha = 0.6f)),
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
                            .background(AppColors.SurfaceContainer.copy(alpha = 0.7f))
                            .border(1.dp, AppColors.GoldDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
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
    val numTables = (calculatedNumTables + manualCount).coerceAtLeast(1)
    
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