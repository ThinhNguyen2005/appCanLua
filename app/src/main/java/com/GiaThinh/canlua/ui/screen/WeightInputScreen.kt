package com.GiaThinh.canlua.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.data.model.WeightEntry
import com.GiaThinh.canlua.ui.component.weight.WeightMetricsCard
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

    // Sắp xếp dữ liệu theo dạng Cột từ trên xuống (Column-Major)
    val tables = remember(weightEntries, manualTableCount) { 
        organizeIntoTables(weightEntries, manualTableCount) 
    }
    val columnTotals = remember(weightEntries) { calculateColumnTotals(weightEntries) }

    // State cho phân trang bảng (Pagination)
    var selectedTableIndex by remember(tables.size) { mutableStateOf(tables.size - 1) }
    val activeTableIndex = selectedTableIndex.coerceIn(0, (tables.size - 1).coerceAtLeast(0))

    // PagerState phục vụ việc vuốt ngang xem bảng chi tiết
    val pagerState = rememberPagerState(
        initialPage = activeTableIndex,
        pageCount = { tables.size }
    )

    // Đồng bộ hoá 2 chiều giữa Tab Button Click và Vuốt Ngang (Pager)
    LaunchedEffect(pagerState.currentPage) {
        selectedTableIndex = pagerState.currentPage
    }

    LaunchedEffect(activeTableIndex) {
        if (pagerState.currentPage != activeTableIndex) {
            pagerState.animateScrollToPage(activeTableIndex)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 3.dp,
                tonalElevation = 0.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nút Quay lại
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }

                    Text(
                        text = "Cân lúa: ${card.name}",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(
                        onClick = {
                            navController.navigate("card_detail/${card.id}")
                        }
                    ) {
                        Text("Xem/Sửa", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // === CARD 1: Chỉ số cân (Tên nông dân chỉ hiển thị, Tổng khối lượng, Trừ bì, Trừ tạp chất, Đơn giá, Thành tiền) ===
            item(key = "metrics") {
                WeightMetricsCard(
                    farmerName = card.name,
                    totalWeight = card.totalWeight,
                    bagWeight = card.bagWeight,
                    impurityWeight = card.impurityWeight,
                    netWeight = card.netWeight,
                    pricePerKg = card.pricePerKg,
                    totalAmount = card.totalAmount,
                    bagCount = card.bagCount,
                    isLocked = card.isLocked,
                    onBagWeightChange = { viewModel.updateCardBagWeight(cardId, it) },
                    onImpurityWeightChange = { viewModel.updateCardImpurityWeight(cardId, it) },
                    onPriceChange = { viewModel.updateCardPricePerKg(cardId, it) }
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
                            text = "Chọn Bảng Nhập",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )

                        if (!card.isLocked) {
                            TextButton(
                                onClick = {
                                    manualTableCount++
                                    // Tự động nhảy sang bảng vừa tạo
                                    selectedTableIndex = tables.size
                                    HapticUtil.confirm(context)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Thêm", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                            Button(
                                onClick = { selectedTableIndex = index },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) AppColors.GreenPrimary else AppColors.CardBg,
                                    contentColor = if (isSelected) Color.White else AppColors.TextSecondary.copy(alpha = 0.7f)
                                ),
                                border = if (!isSelected) BorderStroke(1.dp, AppColors.Divider) else null,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Text(
                                    text = "Bảng ${index + 1}",
                                    fontSize = if (isSelected) 17.sp else 14.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // === Bảng dữ liệu hỗ trợ VUỐT NGANG (HorizontalPager) ===
            item(key = "selected_table_card") {
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
                                viewModel.updateWeightEntry(
                                    entry.copy(weight = newWeight, netWeight = newWeight - card.bagWeight - card.impurityWeight)
                                )
                            },
                            isLocked = card.isLocked
                        )
                    }
                }
            }

            // === Tổng cột ===
            item(key = "col_totals") { ColumnTotalsRow(columnTotals) }

            // Bottom spacing
            item { Spacer(Modifier.height(16.dp)) }
        }
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
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("BẢNG $tableIndex", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Text("${"%.1f".format(tableTotal)} kg", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Column headers
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(5) { i ->
                        Text(
                            "C${i + 1}", modifier = Modifier.weight(1f),
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
        if (it % 1.0 == 0.0) "%.0f".format(it) else "%.1f".format(it)
    } ?: ""
    var text by remember(value) { mutableStateOf(displayValue) }
    var isFocused by remember { mutableStateOf(false) }

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
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isLocked) {
            BasicTextField(
                value = if (isFocused) text else displayValue,
                onValueChange = { input ->
                    if (input.all { it.isDigit() || it == '.' } && input.length <= 3) {
                        text = input
                        if (input.length == 3) {
                            input.toDoubleOrNull()?.let {
                                if (it > 0) {
                                    text = "" // 🔥 Gán rỗng TRƯỚC khi lưu và nhảy focus để tránh lưu trùng lặp ô (double focus trigger)
                                    onValueEntered(it)
                                    onNextFocus()
                                }
                            }
                            if (text.isNotEmpty()) {
                                text = ""
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentHeight(Alignment.CenterVertically)
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        isFocused = state.isFocused
                        if (!state.isFocused && text.isNotEmpty()) {
                            val entered = text
                            text = "" // 🔥 Gán rỗng trước tiên để bảo vệ
                            entered.toDoubleOrNull()?.let { if (it > 0) onValueEntered(it) }
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
                    entered.toDoubleOrNull()?.let { if (it > 0) onValueEntered(it) }
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
            Text("TỔNG CỘT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AppColors.GoldDark)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                totals.forEach { total ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp) // Sử dụng chiều cao linh hoạt để hỗ trợ co giãn chữ hệ thống tốt hơn
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("%.1f".format(total), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
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
                    tableGrid[r][c] = entries[entryIdx].netWeight
                }
            }
        }
        tables.add(tableGrid.map { it.toList() })
    }
    return tables
}

private fun calculateColumnTotals(entries: List<WeightEntry>): List<Double> {
    val totals = MutableList(5) { 0.0 }
    entries.forEachIndexed { index, entry -> 
        // Trong Column-Major, cột = (vị trí trong bảng 25 ô) / 5
        val localIdx = index % 25
        val col = localIdx / 5
        if (col in 0..4) {
            totals[col] += entry.netWeight
        }
    }
    return totals
}