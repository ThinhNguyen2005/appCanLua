package com.giathinh.canlua.ui.screen

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
import com.giathinh.canlua.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.giathinh.canlua.data.model.WeightEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.giathinh.canlua.util.RiceCalculator
import com.giathinh.canlua.ui.component.weight.WeightMetricsCard
import com.giathinh.canlua.ui.component.pressableScale
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.viewmodel.WeightInputViewModel
import com.giathinh.canlua.util.HapticUtil
import com.giathinh.canlua.util.TrackScreenRender
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

import com.giathinh.canlua.ui.component.TransitionSafeWrapper
import com.giathinh.canlua.ui.component.WeightInputSkeleton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightInputScreen(
    cardId: Long,
    navController: NavController,
    onEditCard: () -> Unit = {},
    onDeleteCard: () -> Unit = {},
    onCreateQr: () -> Unit = {},
    onScanQr: () -> Unit = {}
) {
    TrackScreenRender("weight_input")
    val viewModel: WeightInputViewModel = hiltViewModel()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isDataReady = !isLoading

    // Kích hoạt load dữ liệu ban đầu ngay lập tức ở ngoài wrapper để tránh deadlock.
    LaunchedEffect(cardId) {
        viewModel.loadCardById(cardId)
    }

    WeightInputScreenContent(
        cardId = cardId,
        navController = navController,
        viewModel = viewModel,
        onEditCard = onEditCard,
        onDeleteCard = onDeleteCard,
        onCreateQr = onCreateQr,
        onScanQr = onScanQr
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightInputScreenContent(
    cardId: Long,
    navController: NavController,
    viewModel: WeightInputViewModel,
    onEditCard: () -> Unit = {},
    onDeleteCard: () -> Unit = {},
    onCreateQr: () -> Unit = {},
    onScanQr: () -> Unit = {}
) {
    // Trang nhập cân chủ yếu là gõ số vào ô → 120Hz không mang lại lợi ích thị giác.
    // Request 60Hz để tiết kiệm pin vì user dành phần lớn thời gian ở đây.
    com.giathinh.canlua.ui.util.RequestLowRefreshRate()
    
    // Sử dụng collectAsStateWithLifecycle để tự động giải phóng tài nguyên khi chạy nền
    val currentCard by viewModel.currentCard.collectAsStateWithLifecycle()
    val weightEntries by viewModel.weightEntries.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val appToast = com.giathinh.canlua.ui.feedback.LocalAppToast.current

    val onBagWeightChange = remember(cardId) { { weight: Double -> viewModel.updateCardBagWeight(cardId, weight) } }
    val onBagMethodChange = remember(cardId) {
        { isSampling: Boolean, count: Int, weight: Double ->
            viewModel.updateCardBagMethod(cardId, isSampling, count, weight)
        }
    }
    val onImpurityWeightChange = remember(cardId) { { weight: Double -> viewModel.updateCardImpurityWeight(cardId, weight) } }
    val onMoistureChange = remember(cardId) { { moisture: Double -> viewModel.updateCardMoisture(cardId, moisture) } }
    val onPriceChange = remember(cardId) { { price: Double -> viewModel.updateCardPricePerKg(cardId, price) } }

    // === KHAI BÁO STATE (MANDATORY: Đặt trước check null để bảo toàn Composition Tree) ===

    var showLockConfirmDialog by remember { mutableStateOf(false) }

    val tables by viewModel.tables.collectAsStateWithLifecycle()

    // === STABLE STATE REFS (rememberUpdatedState) ===
    // Cho phép lambda onCellWeightEntered được tạo một lần duy nhất (remember {})
    // nhưng vẫn đọc được giá trị mới nhất mà không recreate → 24 ô còn lại không bị
    // invalidate khi người dùng gõ vào ô số 1.
    val currentWeightEntries by rememberUpdatedState(weightEntries)
    val currentCardState by rememberUpdatedState(currentCard)

    // Chuyển dữ liệu bảng sang PersistentList — kiểu này được Compose Compiler
    // nhận diện NATIVELY là Stable (không cần @Immutable) → WeightTableCard
    // và ColumnTotalsRow trở thành Skippable hoàn toàn theo báo cáo Compiler
    val stableTables = remember(tables) {
        tables.map { outer ->
            ImmutableTableData(
                outer.map { inner -> inner.toPersistentList() }.toPersistentList()
            )
        }.toPersistentList()
    }

    // Stable callback duy nhất cho toàn màn hình — tham chiếu KHÔNG đổi qua mọi recompose.
    // Nhờ rememberUpdatedState, lambda này luôn đọc weightEntries và card mới nhất mà
    // không cần recreate → các GridCell giữ nguyên tham chiếu callback → Skippable hoàn toàn.
    val onCellWeightEntered: (Int, Double) -> Unit = remember {
        { entryIdx, weight ->
            val entries = currentWeightEntries
            val activeCard = currentCardState
            val existingEntry = if (entryIdx < entries.size) entries[entryIdx] else null
            if (existingEntry != null && activeCard != null) {
                val netWeight = RiceCalculator.calcNetWeight(
                    rawWeight = weight,
                    bagWeight = activeCard.bagWeight,
                    impurityWeight = 0.0,
                    moisturePercent = activeCard.moisturePercent
                )
                viewModel.updateWeightEntry(
                    existingEntry.copy(
                        weight = weight,
                        bagWeight = activeCard.bagWeight,
                        impurityWeight = 0.0,
                        netWeight = netWeight
                    )
                )
            } else if (entryIdx == entries.size) {
                viewModel.addWeightEntryDirectly(cardId, weight)
                HapticUtil.tick(context)
            } else {
                appToast.warning("Vui lòng nhập lần lượt từ ô trống kế tiếp")
            }
        }
    }


    val liveTotalWeight = remember(weightEntries) { weightEntries.sumOf { it.weight } }
    val liveBagCount = weightEntries.size
    val calcParams = remember(liveTotalWeight, liveBagCount, currentCard) {
        CalcParams(
            totalRaw = liveTotalWeight,
            bagCount = liveBagCount,
            bagWeight = currentCard?.bagWeight ?: 0.0,
            bagMethodIsSampling = currentCard?.bagMethodIsSampling ?: false,
            bagSampleCount = currentCard?.bagSampleCount ?: 0,
            bagSampleTotalWeight = currentCard?.bagSampleTotalWeight ?: 0.0,
            impurityValue = currentCard?.impurityWeight ?: 0.0,
            moisturePercent = currentCard?.moisturePercent ?: 0.0
        )
    }
    val liveNetWeight = remember(calcParams) {
        RiceCalculator.calcNetWeightWithModes(
            totalRaw = calcParams.totalRaw,
            bagCount = calcParams.bagCount,
            bagWeight = calcParams.bagWeight,
            bagMethodIsSampling = calcParams.bagMethodIsSampling,
            bagSampleCount = calcParams.bagSampleCount,
            bagSampleTotalWeight = calcParams.bagSampleTotalWeight,
            impurityValue = calcParams.impurityValue,
            impurityIsPercent = currentCard?.impurityIsPercent ?: false,
            moisturePercent = calcParams.moisturePercent
        )
    }
    val liveTotalAmount = remember(liveNetWeight, currentCard?.pricePerKg) {
        liveNetWeight * (currentCard?.pricePerKg ?: 0.0)
    }

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
    // Trì hoãn render hệ thống grid 25 ô × N bảng + FocusRequesters + khởi tạo TTS
    // đến khi enter animation (NAV_DURATION_MS = 300ms) hoàn tất + 50ms buffer.
    // Tại sao? Mở màn trùng với enter animation → main thread bị nghẽn:
    //   - HorizontalPager measure tất cả pages
    //   - 25 FocusRequester per table được tạo (5×5 grid)
    //   - TTS engine init bounce qua system binder dù chạy trên Dispatchers.IO
    // 350ms đảm bảo enter animation chạy mượt 60fps trước khi composition heavy
    // work bắt đầu. Gate 150ms cũ chỉ che ~½ animation → vẫn drop frame.
    var isAnimationFinished by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(350L)
        isAnimationFinished = true
    }

    // TTS init đợi đến sau animation — binder transaction tới TextToSpeech service
    // có thể block main thread vài chục ms dù launch trên IO dispatcher.
    LaunchedEffect(isAnimationFinished) {
        if (isAnimationFinished) viewModel.startTts()
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
                            style = MaterialTheme.typography.titleLarge.copy(
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
                        val totalWeightStr = remember(liveTotalWeight) {
                            "%.1f".format(java.util.Locale.US, liveTotalWeight).replace(".", ",")
                        }
                        Text(
                            text = stringResource(R.string.weight_input_collapsed_title, totalWeightStr, liveBagCount),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = AppColors.RemainingHighlight
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
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp)
        ) {
            // === CARD 1: Chỉ số cân ===
            // shadow() được đặt TRƯỚC clip() trong WeightMetricsCard modifier chain
            // → vẽ ở parent layer, không bị offscreen bitmap của graphicsLayer clip.
            item(key = "metrics") {
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
                    onBagWeightChange = onBagWeightChange,
                    onImpurityWeightChange = onImpurityWeightChange,
                    onMoistureChange = onMoistureChange,
                    onPriceChange = onPriceChange,
                    impurityIsPercent = card.impurityIsPercent,
                    bagMethodIsSampling = card.bagMethodIsSampling,
                    bagSampleCount = card.bagSampleCount,
                    bagSampleTotalWeight = card.bagSampleTotalWeight,
                    onBagMethodChange = onBagMethodChange
                )
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
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.TextPrimary
                        )

                        if (!card.isLocked) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.incrementManualTableCount()
                                    HapticUtil.confirm(context)
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = AppColors.GreenPrimary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.weight_input_add_table), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold))
                            }
                        }
                    }
                }
            }

            if (isAnimationFinished) {
                stableTables.forEachIndexed { pageIndex, stableTable ->
                    item(key = "weight_table_$pageIndex") {
                        val onNeedNextTable = remember { { viewModel.incrementManualTableCount() } }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            WeightTableCard(
                                tableIndex = pageIndex + 1,
                                stableTableData = stableTable,
                                tableIndexInList = pageIndex,
                                onNeedNextTable = onNeedNextTable,
                                onWeightEntered = onCellWeightEntered,
                                isLocked = card.isLocked,
                                weightInputMode = card.weightInputMode
                            )
                            // Truyền trực tiếp wrapper — ColumnTotalsRow tự remember tính tổng
                            // → Skippable hoàn toàn khi bảng khác thay đổi
                            ColumnTotalsRow(stableTableData = stableTable)
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
    stableTableData: ImmutableTableData,
    tableIndexInList: Int,
    onNeedNextTable: () -> Unit,
    onWeightEntered: (Int, Double) -> Unit,
    isLocked: Boolean,
    weightInputMode: String = "SMALL"
) {
    val tableData = stableTableData.data
    // remember(stableTableData) → tableTotal chỉ tính lại khi nội dung bảng này thay đổi
    val tableTotal = remember(stableTableData) { tableData.flatten().filterNotNull().sum() }
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
                Text(stringResource(R.string.weight_input_table_title, tableIndex), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
                Text("${"%.1f".format(tableTotal)} kg", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
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

                            // onWeightEntered từ screen có tham chiếu ổn định →
                            // remember chỉ cần key entryIdx, không đổi khi ô khác thay đổi
                            val onCellWeightEntered = remember(entryIdx, onWeightEntered) {
                                { weight: Double -> onWeightEntered(entryIdx, weight) }
                            }
                            val onCellNextFocus = remember(rowIdx, colIdx, onNeedNextTable) {
                                {
                                    if (rowIdx < 4) {
                                        focusRequesters[rowIdx + 1][colIdx].requestFocus()
                                    } else if (colIdx < 4) {
                                        focusRequesters[0][colIdx + 1].requestFocus()
                                    } else {
                                        onNeedNextTable()
                                    }
                                    Unit
                                }
                            }

                            GridCell(
                                value = weightVal,
                                onValueEntered = onCellWeightEntered,
                                onNextFocus = onCellNextFocus,
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
    val appToast = com.giathinh.canlua.ui.feedback.LocalAppToast.current

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
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
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
private fun ColumnTotalsRow(stableTableData: ImmutableTableData) {
    // remember(stableTableData): phép tính tổng cột chỉ chạy lại khi bảng này thay đổi
    // → ColumnTotalsRow trở thành Skippable hoàn toàn khi bảng khác được cập nhật
    val totals = remember(stableTableData) { calculateColumnTotals(stableTableData.data) }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.GoldLight),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.5.dp, AppColors.GoldDark.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.weight_input_column_totals), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = AppColors.GoldDark))
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                totals.forEach { total ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppColors.SurfaceContainer.copy(alpha = 0.7f))
                            .border(1.dp, AppColors.GoldDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "%.1f".format(total),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = AppColors.TextPrimary)
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

private data class CalcParams(
    val totalRaw: Double,
    val bagCount: Int,
    val bagWeight: Double,
    val bagMethodIsSampling: Boolean,
    val bagSampleCount: Int,
    val bagSampleTotalWeight: Double,
    val impurityValue: Double,
    val moisturePercent: Double
)

// ImmutableTableData dùng PersistentList<PersistentList<Double?>> — kiểu dữ liệu này
// được Compose Compiler nhận diện NATIVELY là Stable mà không cần @Immutable.
// Kết quả: WeightTableCard và ColumnTotalsRow được máy biên dịch xác nhận là
// "restartable AND skippable" trong báo cáo Compose Compiler Stability Reports.
data class ImmutableTableData(val data: PersistentList<PersistentList<Double?>>)
