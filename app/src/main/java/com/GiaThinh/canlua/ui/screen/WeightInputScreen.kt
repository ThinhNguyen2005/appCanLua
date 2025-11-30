package com.GiaThinh.canlua.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.data.model.WeightEntry
import com.GiaThinh.canlua.ui.viewmodel.CardViewModel
import java.text.NumberFormat
import java.util.Locale

// --- ĐỊNH NGHĨA MÀU SẮC (Chuẩn theo app tham khảo) ---
import com.GiaThinh.canlua.ui.theme.GreenTableHeader
import com.GiaThinh.canlua.ui.theme.YellowHighlight
import com.GiaThinh.canlua.ui.theme.RedText
import com.GiaThinh.canlua.ui.theme.White

// Sử dụng màu sắc từ theme
private val GreenHeader = GreenTableHeader // Xanh lá đậm (header bảng)
private val YellowButton = YellowHighlight // Vàng đậm (Nút/Tổng cột)
private val GreyBorder = Color.LightGray

@Composable
fun WeightInputScreen(
    cardId: Long,
    navController: NavController,
    viewModel: CardViewModel = hiltViewModel()
) {
    val currentCard by viewModel.currentCard.collectAsState()
    val weightEntries by viewModel.weightEntries.collectAsState()

    LaunchedEffect(cardId) {
        viewModel.loadCardById(cardId)
    }

    val card = currentCard ?: return

    // Tính toán lại các chỉ số hiển thị (đảm bảo logic update realtime)
    val totalWeight = card.totalWeight // Tổng khối lượng cân được (Chưa trừ bì)
    val remainingWeight = card.netWeight // Đã trừ bì và tạp chất (logic trong entity)
    val totalAmount = card.totalAmount
    val remainingAmount = card.remainingAmount
    val isPaidFull = remainingAmount <= 0.0 && card.totalAmount > 0

    // Tổ chức dữ liệu bảng 5x5
    val tables = remember(weightEntries) { organizeIntoTables(weightEntries) }
    // Tính tổng cột dọc (5 cột)
    val columnTotals = remember(weightEntries) { calculateColumnTotals(weightEntries) }

    Scaffold(
        topBar = {
            CustomTopBar(
                title = card.name,
                totalWeight = totalWeight,
                count = weightEntries.size,
                isLocked = card.isLocked,
                onBack = { navController.popBackStack() },
                onToggleLock = { viewModel.toggleCardLock(cardId) }
            )
        },
        containerColor = Color(0xFFF5F5F5) // Nền tổng thể hơi xám nhẹ
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp),
        ) {
            // 1. PHẦN THÔNG TIN & TÍNH TOÁN (KẾT QUẢ)
            item {
                ResultInfoSection(
                    cardName = card.name,
                    totalWeight = totalWeight,
                    bagWeight = card.bagWeight,
                    impurityWeight = card.impurityWeight,
                    netWeight = remainingWeight,
                    pricePerKg = card.pricePerKg,
                    totalAmount = totalAmount,
                    depositAmount = card.depositAmount,
                    paidAmount = card.paidAmount,
                    remainingAmount = remainingAmount,
                    isPaidFull = isPaidFull,
                    isLocked = card.isLocked,
                    onNameChange = { viewModel.updateCardName(cardId, it) },
                    onBagWeightChange = { viewModel.updateCardBagWeight(cardId, it) },
                    onImpurityWeightChange = { viewModel.updateCardImpurityWeight(cardId, it) },
                    onPriceChange = { viewModel.updateCardPricePerKg(cardId, it) },
                    onDepositChange = { viewModel.updateCardDepositAmount(cardId, it) },
                    onPaidChange = { viewModel.updateCardPaidAmount(cardId, it) },
                    onPaidFullToggle = { isChecked ->
                        if (isChecked) {
                            val needToPay = totalAmount - card.depositAmount
                            viewModel.updateCardPaidAmount(cardId, if (needToPay > 0) needToPay else 0.0)
                        } else {
                            viewModel.updateCardPaidAmount(cardId, 0.0)
                        }
                    }
                )
            }

            itemsIndexed(tables) { index, table ->
                // Chỉ hiển thị bảng nếu có dữ liệu hoặc là bảng cuối cùng (để nhập)
                val isLastTable = index == tables.size - 1
                val hasData = table.flatten().any { it != null }
                
                if (hasData || isLastTable) {
                    WeightTableStyled(
                        tableIndex = index + 1,
                        tableData = table,
                        weightEntries = weightEntries,
                        tableIndexInList = index,
                        onWeightEntered = { weight, rowIndex, colIndex ->
                            // CHỈ THÊM 1 ENTRY MỖI LẦN - Không tự động thêm 5 lần
                            viewModel.addWeightEntryDirectly(cardId, weight)
                        },
                        onWeightUpdated = { entry, newWeight ->
                            // Cập nhật entry hiện có
                            viewModel.updateWeightEntry(
                                entry.copy(
                                    weight = newWeight,
                                    netWeight = newWeight - card.bagWeight - card.impurityWeight
                                )
                            )
                        },
                        isLocked = card.isLocked
                    )
                }
            }

            // 3. TỔNG CỘT (COLUMN TOTALS)
            item {
                ColumnTotalsRow(columnTotals)
            }

            // 4. TỔNG CỘNG (GRAND TOTAL)
            item {
                GrandTotalSection(remainingWeight)
            }
        }
    }
}

// =============================================================================
// SECTIONS COMPONENTS
// =============================================================================

@Composable
fun ResultInfoSection(
    cardName: String,
    totalWeight: Double,
    bagWeight: Double,
    impurityWeight: Double,
    netWeight: Double,
    pricePerKg: Double,
    totalAmount: Double,
    depositAmount: Double,
    paidAmount: Double,
    remainingAmount: Double,
    isPaidFull: Boolean,
    isLocked: Boolean,
    onNameChange: (String) -> Unit,
    onBagWeightChange: (Double) -> Unit,
    onImpurityWeightChange: (Double) -> Unit,
    onPriceChange: (Double) -> Unit,
    onDepositChange: (Double) -> Unit,
    onPaidChange: (Double) -> Unit,
    onPaidFullToggle: (Boolean) -> Unit
) {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header KẾT QUẢ
            Text(
                text = "KẾT QUẢ",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RedText, RoundedCornerShape(4.dp))
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center,
                color = White,
                fontWeight = FontWeight.Bold
            )

            // Tên nông dân (Editable)
            LabelInputRow(
                label = "Tên nông dân",
                value = cardName,
                onValueChange = { onNameChange(it) },
                isEditable = !isLocked,
                isText = true
            )

            // Tổng khối lượng (Read-only, Yellow)
            LabelDisplayRow(
                label = "Tổng khối lượng",
                value = "${String.format("%.1f", totalWeight)} KG",
                bgColor = YellowHighlight,
                note = "(*) Khối lượng CHƯA trừ bì"
            )

            // Trừ bì & Trừ tạp chất (Editable)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    LabelInputRow(
                        label = "Trừ bì (kg)",
                        value = if (bagWeight == 0.0) "" else bagWeight.toString(),
                        onValueChange = { onBagWeightChange(it.toDoubleOrNull() ?: 0.0) },
                        isEditable = !isLocked,
                        isCompact = true
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    LabelInputRow(
                        label = "Trừ tạp chất (kg)",
                        value = if (impurityWeight == 0.0) "" else impurityWeight.toString(),
                        onValueChange = { onImpurityWeightChange(it.toDoubleOrNull() ?: 0.0) },
                        isEditable = !isLocked,
                        isCompact = true
                    )
                }
            }

            // Khối lượng còn lại (Read-only, Yellow)
            LabelDisplayRow(
                label = "Khối lượng còn lại",
                value = "${String.format("%.1f", netWeight)} KG",
                bgColor = YellowHighlight,
                note = "(*) Khối lượng ĐÃ trừ bì & tạp chất"
            )

            // Đơn giá (Editable)
            LabelInputRow(
                label = "Đơn giá (Vnđ)",
                value = if (pricePerKg == 0.0) "" else String.format("%.0f", pricePerKg),
                onValueChange = { onPriceChange(it.toDoubleOrNull() ?: 0.0) },
                isEditable = !isLocked,
                isMoney = true
            )

            // Thành tiền (Read-only, Yellow)
            LabelDisplayRow(
                label = "Thành tiền",
                value = "${formatter.format(totalAmount)} Vnđ",
                bgColor = YellowHighlight,
                isBoldValue = true,
                textColor = RedText,
                note = "(*) T.Tiền = KL Còn lại x Đơn giá"
            )

            // Tiền cọc & Đã trả (Editable)
            LabelInputRow(
                label = "Tiền đặt cọc (-)",
                value = if (depositAmount == 0.0) "" else String.format("%.0f", depositAmount),
                onValueChange = { onDepositChange(it.toDoubleOrNull() ?: 0.0) },
                isEditable = !isLocked,
                isMoney = true
            )

            LabelInputRow(
                label = "Tiền đã trả (-)",
                value = if (paidAmount == 0.0) "" else String.format("%.0f", paidAmount),
                onValueChange = { onPaidChange(it.toDoubleOrNull() ?: 0.0) },
                isEditable = !isLocked,
                isMoney = true
            )

            // Tiền còn lại (Read-only, Highlight Grey)
            LabelDisplayRow(
                label = "TIỀN CÒN LẠI",
                value = "${formatter.format(remainingAmount)} Vnđ",
                bgColor = Color(0xFFEEEEEE),
                isBoldValue = true,
                textColor = RedText,
                note = "(*) Còn lại = Thành tiền - (Cọc + Đã trả)"
            )

            // Switch Đã trả đủ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Đã trả đủ tiền",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = GreenHeader
                )
                Switch(
                    checked = isPaidFull,
                    onCheckedChange = { if (!isLocked) onPaidFullToggle(it) },
                    enabled = !isLocked,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = White,
                        checkedTrackColor = GreenHeader
                    )
                )
            }
        }
    }
}

@Composable
fun ColumnTotalsRow(totals: List<Double>) {
    Surface(
        color = YellowButton, // Màu vàng đậm
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                "TỔNG CỘT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                totals.forEach { total ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .background(White.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%.1f", total),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GrandTotalSection(netWeight: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TỔNG KHỐI LƯỢNG (Đã trừ)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
            Text(
                text = "${String.format("%.1f", netWeight)} KG",
                style = MaterialTheme.typography.displayMedium, // Font lớn
                fontWeight = FontWeight.ExtraBold,
                color = RedText
            )
        }
    }
}

// =============================================================================
// SMALL UI COMPONENTS (HELPER)
// =============================================================================

@Composable
fun LabelInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isEditable: Boolean,
    isText: Boolean = false,
    isMoney: Boolean = false,
    isCompact: Boolean = false
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = value,
            onValueChange = {
                if (isText) onValueChange(it)
                else if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) {
                    onValueChange(it)
                }
            },
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth().height(if (isCompact) 50.dp else 56.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textAlign = if (isText) TextAlign.Start else TextAlign.End,
                fontWeight = FontWeight.Bold
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isText) KeyboardType.Text else KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            suffix = if (isMoney) { { Text("đ") } } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = GreyBorder,
                disabledBorderColor = GreyBorder.copy(alpha = 0.5f),
                disabledTextColor = Color.Black // Giữ màu đen dù bị disable
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun LabelDisplayRow(
    label: String,
    value: String,
    bgColor: Color,
    isBoldValue: Boolean = false,
    textColor: Color = Color.Black,
    note: String? = null
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(bgColor, RoundedCornerShape(8.dp))
                .border(1.dp, GreyBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isBoldValue) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            )
        }
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = FontStyle.Italic,
                    color = RedText
                ),
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

@Composable
fun WeightTableStyled(
    tableIndex: Int,
    tableData: List<List<Double?>>,
    weightEntries: List<WeightEntry>,
    tableIndexInList: Int,
    onWeightEntered: (Double, Int, Int) -> Unit, // weight, rowIndex, colIndex
    onWeightUpdated: (WeightEntry, Double) -> Unit, // entry, newWeight
    isLocked: Boolean
) {
    val tableTotal = tableData.flatten().filterNotNull().sum()

    // Ma trận FocusRequester 5x5
    val focusRequesters = remember { List(5) { List(5) { androidx.compose.ui.focus.FocusRequester() } } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .background(Color.White)
    ) {
        // Header Bảng
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenHeader) // GreenHeader
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("BẢNG $tableIndex", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text(String.format("%.1f", tableTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }

        // Grid Input
        Column(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header cột C1-C5
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(5) { i ->
                    Text("C${i + 1}", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            }

            // Render các hàng (Rows)
            // Đảm bảo luôn render đủ 5 hàng để giữ cấu trúc grid cho Focus
            val displayRows = List(5) { rowIndex ->
                if (rowIndex < tableData.size) tableData[rowIndex] else List(5) { null }
            }

            displayRows.forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Render các cột (Cols)
                    val displayCols = List(5) { colIndex ->
                        if (colIndex < row.size) row[colIndex] else null
                    }

                    displayCols.forEachIndexed { colIndex, weightVal ->
                        // Tính entry index trong danh sách weightEntries
                        val entryIndex = (tableIndexInList * 25) + (rowIndex * 5) + colIndex
                        val existingEntry = if (entryIndex < weightEntries.size) weightEntries[entryIndex] else null
                        
                        GridInputCell(
                            value = weightVal,
                            onValueEntered = { weight ->
                                // Kiểm tra: nếu ô đã có dữ liệu và có entry tương ứng thì update, chưa có thì thêm mới
                                if (weightVal != null && existingEntry != null) {
                                    onWeightUpdated(existingEntry, weight)
                                } else {
                                    // Chỉ thêm mới nếu ô trống
                                    onWeightEntered(weight, rowIndex, colIndex)
                                }
                            },
                            onNextFocus = {
                                // Logic nhảy xuống: Nếu chưa phải hàng cuối (row 4) thì nhảy xuống row+1
                                if (rowIndex < 4) {
                                    focusRequesters[rowIndex + 1][colIndex].requestFocus()
                                }
                            },
                            focusRequester = focusRequesters[rowIndex][colIndex],
                            isLocked = isLocked,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GridInputCell(
    value: Double?,
    onValueEntered: (Double) -> Unit,
    onNextFocus: () -> Unit, // Callback để nhảy focus
    focusRequester: androidx.compose.ui.focus.FocusRequester, // Nhận FocusRequester từ cha
    isLocked: Boolean,
    modifier: Modifier
) {
    // Chỉ hiển thị phần nguyên nếu là số nguyên (ví dụ 222 thay vì 222.0)
    val displayValue = value?.let {
        if (it % 1.0 == 0.0) String.format("%.0f", it) else String.format("%.1f", it)
    } ?: ""

    var text by remember(value) { mutableStateOf(displayValue) }
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .aspectRatio(1.4f)
            .background(Color.White, RoundedCornerShape(4.dp))
            .border(1.dp, if (isFocused) Color(0xFF1565C0) else Color.LightGray, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!isLocked) {
            androidx.compose.foundation.text.BasicTextField(
                value = if (isFocused) text else displayValue,
                onValueChange = { input ->
                    // 1. Chỉ cho nhập số
                    if (input.all { it.isDigit() || it == '.' }) {
                        // 2. Giới hạn 3 ký tự
                        if (input.length <= 3) {
                            text = input

                            // 3. Nếu nhập đủ 3 số -> Lưu và Nhảy xuống
                            if (input.length == 3) {
                                input.toDoubleOrNull()?.let {
                                    if (it > 0) {
                                        onValueEntered(it)
                                        onNextFocus() // Nhảy xuống
                                    }
                                }
                                text = "" // Reset để hiển thị value từ DB
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentHeight(Alignment.CenterVertically)
                    .focusRequester(focusRequester) // Gán FocusRequester
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (!focusState.isFocused && text.isNotEmpty()) {
                            text.toDoubleOrNull()?.let {
                                if (it > 0) onValueEntered(it)
                                text = ""
                            }
                        }
                    },
                textStyle = androidx.compose.ui.text.TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp, // Số to rõ
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number, // Bàn phím số
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        // Xử lý khi bấm nút Enter/Next trên bàn phím
                        text.toDoubleOrNull()?.let {
                            if (it > 0) onValueEntered(it)
                        }
                        text = ""
                        onNextFocus() // Nhảy xuống
                    }
                ),
                singleLine = true
            )
        } else {
            Text(
                text = displayValue,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

// =============================================================================
// LOGIC HELPER FUNCTIONS
// =============================================================================

private fun organizeIntoTables(entries: List<WeightEntry>): List<List<List<Double?>>> {
    val rowsPerTable = 5
    val colsPerTable = 5
    val tables = mutableListOf<List<List<Double?>>>()
    var currentTable = mutableListOf<List<Double?>>()
    var currentRow = mutableListOf<Double?>()

    // Chỉ xử lý entries thực tế từ database
    entries.forEach { entry ->
        currentRow.add(entry.netWeight) // Sử dụng netWeight (đã trừ bì)
        if (currentRow.size == colsPerTable) {
            currentTable.add(currentRow.toList())
            currentRow.clear()
            if (currentTable.size == rowsPerTable) {
                tables.add(currentTable.toList())
                currentTable.clear()
            }
        }
    }

    // Xử lý row cuối cùng nếu chưa đủ 5 cột
    if (currentRow.isNotEmpty()) {
        while (currentRow.size < colsPerTable) currentRow.add(null)
        currentTable.add(currentRow)
    }
    
    // Điền đủ 5 hàng cho bảng cuối
    while (currentTable.size < rowsPerTable) {
        currentTable.add(List(colsPerTable) { null })
    }
    
    // Chỉ thêm bảng cuối nếu có dữ liệu
    if (currentTable.any { row -> row.any { it != null } }) {
        tables.add(currentTable)
    }
    
    // CHỈ thêm 1 bảng trống ở cuối để nhập (nếu chưa có hoặc bảng cuối đã đầy)
    val lastTableHasEmptyCell = tables.isNotEmpty() && tables.last().flatten().any { it == null }
    if (tables.isEmpty() || !lastTableHasEmptyCell) {
        tables.add(List(rowsPerTable) { List(colsPerTable) { null } })
    }
    
    return tables
}

private fun calculateColumnTotals(entries: List<WeightEntry>): List<Double> {
    val totals = MutableList(5) { 0.0 }
    entries.forEachIndexed { index, entry ->
        val colIndex = index % 5
        totals[colIndex] += entry.netWeight
    }
    return totals
}

@Composable
fun CustomTopBar(
    title: String,
    totalWeight: Double,
    count: Int,
    isLocked: Boolean,
    onBack: () -> Unit,
    onToggleLock: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(), // 1. Bỏ height(60.dp) ở đây
        shadowElevation = 4.dp
    ) {
        // Sử dụng Column hoặc Box để áp dụng insets
        Column {
            // Row chứa nội dung chính
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // 2. QUAN TRỌNG: Thêm dòng này để đẩy nội dung xuống khỏi thanh trạng thái
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(60.dp) // 3. Đặt chiều cao nội dung ở đây
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = White
                    )
                }

                Surface(
                    color = White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "${String.format("%.1f", totalWeight)} kg / $count",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = onToggleLock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YellowButton,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isLocked) "Xem" else "Sửa")
                }
            }
        }
    }
}