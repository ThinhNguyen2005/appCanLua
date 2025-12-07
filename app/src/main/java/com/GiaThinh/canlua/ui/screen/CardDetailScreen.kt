package com.GiaThinh.canlua.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Inventory2
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.ui.theme.*
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

    // Formatters
    val numberFormat = NumberFormat.getNumberInstance(Locale("vi", "VN"))

    LaunchedEffect(cardId) {
        viewModel.loadCardById(cardId)
    }

    val card = currentCard ?: return

    val scrollState = rememberLazyListState()
    val density = LocalDensity.current
    
    // Dimensions
    val headerHeight = 120.dp
    val pillHeight = 88.dp
    val collapsedPillHeight = 64.dp
    val pillHalfHeight = pillHeight / 2
    
    // Calculate scroll progress
    val collapseRangePx = with(density) { (headerHeight - pillHalfHeight).toPx() } // Distance to scroll before sticking
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

    val context = LocalContext.current
    var showOverflow by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
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
                // Spacer to push content down below the Header + Pill
                item {
                    Spacer(modifier = Modifier.height(headerHeight + pillHalfHeight))
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

                // ITEM 2: Summary Card
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SummaryCardItem(
                            card = card,
                            numberFormat = numberFormat
                        )
                    }
                }
            }

            // 2. The Header (Background) - Scrolls away
            // We translate it up based on collapseFraction
            CustomHeader(
                modifier = Modifier
                    .height(headerHeight)
                    .graphicsLayer {
                        translationY = -collapseFraction * collapseRangePx
                    },
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate("weight_input/${cardId}") },
                onPrint = {
                    Toast.makeText(context, "In hóa đơn (đang phát triển)", Toast.LENGTH_SHORT).show()
                },
                onEdit = { navController.navigate("weight_input/${cardId}") },
                onDelete = { showDeleteConfirm = true },
                showOverflow = showOverflow,
                onOverflowChange = { showOverflow = it }
            )


            
            // 3. The Sticky Info Box (Pill)
            // It sits at (HeaderHeight - HalfPill) initially, and moves up to 0 (or StatusBar)
            val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val initialPillTop = headerHeight - pillHalfHeight
            val targetPillTop = statusBarHeight + 8.dp // A bit of padding from top
            
            // Interpolate Y position
            val currentPillTop = with(density) {
                androidx.compose.ui.unit.lerp(initialPillTop, targetPillTop, collapseFraction)
            }
            
            // Interpolate Width (Optional: Expand to full width when stuck?)
            // Let's keep it as a pill but maybe wider
            val currentPillWidthFraction = 0.9f + (0.1f * collapseFraction) // 90% -> 100%

            Box(
                modifier = Modifier
                    .zIndex(1f)
                    .fillMaxWidth()
                    .offset(y = currentPillTop)
                    .padding(horizontal = 12.dp)
                    .height(androidx.compose.ui.unit.lerp(pillHeight, collapsedPillHeight, collapseFraction)),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp * (1 - collapseFraction))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (card.traderName.isBlank()) "Thương lái: Chưa có" else "Thương lái: ${card.traderName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Ngày: ${SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(card.date)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${numberFormat.format(card.totalWeight)} KG",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Green40
                            )
                            Text(
                                text = "${card.bagCount} bao",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
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
        }
    }
}

@Composable
fun CustomHeader(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onPrint: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showOverflow: Boolean,
    onOverflowChange: (Boolean) -> Unit
) {
    // Use a Box with a gradient background for a modern look
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Green40, GreenGrey40)
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Green40)
                }
                Column {
                    Text("Tên Người bán", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Green40)
                    Text("1xx kg / số lượng", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = RedHeader, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thêm", fontSize = 12.sp)
                }

//                Button(
//                    onClick = { onEdit() },
//                    colors = ButtonDefaults.buttonColors(containerColor = Amber80, contentColor = Color.Black),
//                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
//                    modifier = Modifier.height(34.dp)
//                ) {
//                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Text("Sửa", fontSize = 12.sp)
//                }

                IconButton(
                    onClick = { onOverflowChange(!showOverflow) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.Black)
                }

                DropdownMenu(
                    expanded = showOverflow,
                    onDismissRequest = { onOverflowChange(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text("In hóa đơn") },
                        leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                        onClick = {
                            onOverflowChange(false)
                            onPrint()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sửa phiếu") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            onOverflowChange(false)
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa phiếu", color = Color.Red) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                        onClick = {
                            onOverflowChange(false)
                            onDelete()
                        }
                    )
                }
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

                    OutlinedButton(
                        onClick = onOpenClick,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GreenGrey40),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenGrey40)
                    ) {
                        Text("Chi tiết")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
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