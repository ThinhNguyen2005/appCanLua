package com.GiaThinh.canlua.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import com.GiaThinh.canlua.ui.component.RiceVarietyDropdown
import com.GiaThinh.canlua.ui.component.ThousandSeparatorTransformation
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.unit.lerp
import com.GiaThinh.canlua.ui.component.CustomHeader
import com.GiaThinh.canlua.ui.component.WeighingButton
import com.GiaThinh.canlua.ui.component.rememberHeaderHeights
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.ui.theme.*
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.viewmodel.CardViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CardDetailScreen(
    cardId: Long,
    navController: NavController,
    viewModel: CardViewModel = hiltViewModel()
) {
    val currentCard by viewModel.currentCard.collectAsState()
    val weightEntries by viewModel.weightEntries.collectAsState()

    // Formatters
    val numberFormat = NumberFormat.getNumberInstance(Locale("vi", "VN"))

    LaunchedEffect(cardId) {
        viewModel.loadCardById(cardId)
    }

    val card = currentCard ?: return

    val tables = remember(weightEntries) { weightEntries.chunked(25) }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tables.size.coerceAtLeast(1) }
    )
    var selectedTableIndex by remember(tables.size) { mutableStateOf(0) }
    val activeTableIndex = selectedTableIndex.coerceIn(0, (tables.size - 1).coerceAtLeast(0))

    LaunchedEffect(pagerState.currentPage) {
        selectedTableIndex = pagerState.currentPage
    }

    LaunchedEffect(activeTableIndex) {
        if (pagerState.currentPage != activeTableIndex && activeTableIndex < tables.size) {
            pagerState.animateScrollToPage(activeTableIndex)
        }
    }

    val heights = rememberHeaderHeights()

    val scrollState = rememberLazyListState()
    val density = LocalDensity.current
    
    // Calculate scroll progress
    val collapseRangePx = with(density) { heights.expanded.toPx() } // Distance to scroll before fully collapsed
    val collapseFraction by remember {
        derivedStateOf {
            val scroll = if (scrollState.firstVisibleItemIndex == 0) {
                scrollState.firstVisibleItemScrollOffset.toFloat()
            } else {
                collapseRangePx // Fully collapsed if scrolled past first item
            }
            (scroll / collapseRangePx).coerceIn(0f, 1f)
        }
    }

    val clampedFraction = collapseFraction.coerceIn(0f, 1f)
    val headerHeight = lerp(heights.expanded, heights.collapsed, clampedFraction)

    val lastEntryTime = weightEntries.maxOfOrNull { it.timestamp }

    val context = LocalContext.current
    var showOverflow by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = LightGray,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // 1. The Scrollable Content (LazyColumn)
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Spacer to push content down below the Header
                item {
                    Spacer(modifier = Modifier.height(heights.expanded + 16.dp))
                }

                // ITEM 1: Detail Card
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DetailCardItem(
                            card = card,
                            numberFormat = numberFormat,
                            onOpenClick = {
                                navController.navigate("weight_input/${card.id}")
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // ITEM 2: Weight entries detail list (Replacing the old SummaryCardItem)
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        "${weightEntries.size} bao",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                if (weightEntries.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Chưa có bao cân nào được nhập.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                    }
                                } else {
                                    // Selector cho các Bảng
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(tables.size) { index ->
                                            val isActive = index == activeTableIndex
                                            val textStyle = if (isActive) {
                                                MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp,
                                                    color = AppColors.GreenPrimary
                                                )
                                            } else {
                                                MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Normal,
                                                    fontSize = 14.sp,
                                                    color = AppColors.TextSecondary
                                                )
                                            }
                                            Surface(
                                                onClick = {
                                                    selectedTableIndex = index
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isActive) AppColors.GreenSurface else Color.Transparent,
                                                border = if (isActive) BorderStroke(1.dp, AppColors.GreenPrimary) else null,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Bảng ${index + 1}",
                                                    style = textStyle,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Pager cho phép vuốt ngang qua lại các bảng
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { pageIndex ->
                                        if (pageIndex < tables.size) {
                                            val tableEntries = tables[pageIndex]
                                            val tableGrid = MutableList(5) { MutableList<com.GiaThinh.canlua.data.model.WeightEntry?>(5) { null } }
                                            for (c in 0 until 5) {
                                                for (r in 0 until 5) {
                                                    val entryIdx = c * 5 + r
                                                    if (entryIdx < tableEntries.size) {
                                                        tableGrid[r][c] = tableEntries[entryIdx]
                                                    }
                                                }
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                for (r in 0 until 5) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        for (c in 0 until 5) {
                                                            val entry = tableGrid[r][c]
                                                            val globalIndex = pageIndex * 25 + c * 5 + r + 1
                                                            if (entry != null) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .background(AppColors.GreenSurface)
                                                                        .padding(vertical = 8.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                        Text(
                                                                            text = "#$globalIndex",
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            color = AppColors.GreenPrimary,
                                                                            fontWeight = FontWeight.Bold
                                                                        )
                                                                        Text(
                                                                            text = if (entry.weight % 1.0 == 0.0) "%.0f".format(entry.weight) else "%.1f".format(entry.weight),
                                                                            style = MaterialTheme.typography.bodyMedium,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = AppColors.TextPrimary
                                                                        )
                                                                    }
                                                                }
                                                            } else {
                                                                Spacer(modifier = Modifier.weight(1f))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. The Header (Background) - Scrolls away
            CustomHeader(
                card = card,
                collapseFraction = clampedFraction,
                lastEntryTime = lastEntryTime,
                modifier = Modifier
                    .height(headerHeight)
                    .graphicsLayer {
                        translationY = -clampedFraction * collapseRangePx
                    },
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate("weight_input/${cardId}") },
                showOverflow = showOverflow,
                onOverflowChange = { showOverflow = it },
                onEditCard = { showEditDialog = true },
                onDeleteCard = { showDeleteConfirm = true },
                onCreateQr = { navController.navigate("qr_generate/${card.id}") },
                onScanQr = { navController.navigate("qr_scan") }
            )

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
                            Text("Xóa", color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text("Hủy")
                        }
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
        }
    }
}



@Composable
fun DetailCardItem(
    card: Card,
    numberFormat: NumberFormat,
    onOpenClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Decorative side bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(GreenGrey40)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Header: Date & Open Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Ngày tạo",
                            style = MaterialTheme.typography.labelMedium,
                            color = GrayLabel
                        )
                        Text(
                            text = dateFormat.format(card.date),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    WeighingButton(onClick = onOpenClick)
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = LightGray
                )

                // Stats Grid with Distinct Icons
                StatRow(
                    icon = Icons.Outlined.Inventory2, // Total Weight
                    label = "Tổng K/Lượng",
                    value = "${numberFormat.format(card.totalWeight)} KG",
                    valueColor = BlackText,
                    isBold = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatRow(
                    icon = Icons.Outlined.Scale, // Remaining Weight
                    label = "K/Lượng còn lại",
                    value = "${numberFormat.format(card.netWeight)} KG",
                    valueColor = BlackText,
                    isBold = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatRow(
                    icon = Icons.Outlined.ShoppingBag, // Bags (Changed from Inventory2)
                    label = "Số bao",
                    value = "${card.bagCount} bao",
                    valueColor = BlackText,
                    isBold = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Financials with Icons (Custom Row for better control)
                FinancialRow(icon = Icons.Outlined.AttachMoney, label = "Giá tiền:", value = "${numberFormat.format(card.pricePerKg)} đ")
                FinancialRow(icon = Icons.Outlined.Calculate, label = "Thành tiền:", value = "${numberFormat.format(card.totalAmount)} đ")
                FinancialRow(icon = Icons.Outlined.CreditCard, label = "Tiền Cọc:", value = "${numberFormat.format(card.depositAmount)} đ")
                FinancialRow(icon = Icons.Outlined.CheckCircle, label = "Đã trả:", value = "${numberFormat.format(card.paidAmount)} đ")

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = LightGray
                )

                // Remaining Amount (Explicitly rendered)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.AccountBalanceWallet, // Wallet Icon
                            contentDescription = null,
                            tint = RedText,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Còn lại",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RedText
                        )
                    }
                    Text(
                        text = "${numberFormat.format(card.remainingAmount)} đ",
                        style = MaterialTheme.typography.headlineSmall, // Larger text
                        fontWeight = FontWeight.Bold,
                        color = RedText
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCardItem(
    card: Card,
    numberFormat: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Red Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RedHeader)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Summarize, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "TỔNG CỘNG",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        "x(${card.bagCount} lượng)",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Content
            Column(modifier = Modifier.padding(16.dp)) {
                StatRow(
                    icon = Icons.Outlined.Inventory2,
                    label = "Tổng K/Lượng",
                    value = "${numberFormat.format(card.totalWeight)} KG",
                    valueColor = BlackText,
                    isBold = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatRow(
                    icon = Icons.Outlined.Scale,
                    label = "K/Lượng còn lại",
                    value = "${numberFormat.format(card.netWeight)} KG",
                    valueColor = RedText,
                    isBold = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatRow(
                    icon = Icons.Outlined.Inventory2,
                    label = "Số bao",
                    value = "${card.bagCount} bao",
                    valueColor = BlackText,
                    isBold = true
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = LightGray)

                MoneyRow(label = "Giá tiền:", value = "${numberFormat.format(card.pricePerKg)} đ")
                MoneyRow(label = "Thành tiền:", value = "${numberFormat.format(card.totalAmount)} đ")
                MoneyRow(label = "Tiền Cọc:", value = "${numberFormat.format(card.depositAmount)} đ")
                MoneyRow(label = "Đã trả:", value = "${numberFormat.format(card.paidAmount)} đ")

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = LightGray)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = GrayLabel,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Còn lại", fontWeight = FontWeight.Bold, color = RedText)
                    }
                    Text(
                        text = "${numberFormat.format(card.remainingAmount)} đ",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = RedText
                    )
                }
            }
        }
    }
}

@Composable
fun StatRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = GrayLabel,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = BlackText
            )
        }
        Text(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun MoneyRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = GrayLabel
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = BlackText
        )
    }
}

@Composable
fun FinancialRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = GrayLabel, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = GrayLabel)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = BlackText)
    }
}

class ThousandSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val parsed = originalText.toLongOrNull() ?: 0L
        val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN")).format(parsed)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val originalSub = originalText.substring(0, offset.coerceAtMost(originalText.length))
                val parsedSub = originalSub.toLongOrNull() ?: 0L
                val formattedSub = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN")).format(parsedSub)
                return formattedSub.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val transformedSub = formatted.substring(0, offset.coerceAtMost(formatted.length))
                val digitsOnly = transformedSub.filter { it.isDigit() }
                return digitsOnly.length.coerceAtMost(originalText.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

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