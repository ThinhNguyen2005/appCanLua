package com.GiaThinh.canlua.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.ui.component.WeightInputDialog
import com.GiaThinh.canlua.ui.viewmodel.CardViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: Long,
    navController: NavController,
    viewModel: CardViewModel = hiltViewModel()
) {
    val currentCard by viewModel.currentCard.collectAsState()
    val weightEntries by viewModel.weightEntries.collectAsState()
    var showWeightInputDialog by remember { mutableStateOf(false) }

    LaunchedEffect(cardId) {
        viewModel.loadCardById(cardId)
    }

    val card = currentCard ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleCardLock(cardId) }
                    ) {
                        Icon(
                            imageVector = if (card.isLocked) Icons.Default.Lock else Icons.Default.Check,
                            contentDescription = if (card.isLocked) "Mở khóa" else "Khóa"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!card.isLocked) {
                FloatingActionButton(
                    onClick = { navController.navigate("weight_input/${cardId}") }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm cân nặng")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Information
            CardInfoSection(card = card)
            
            // Weight Entries
            WeightEntriesSection(
                weightEntries = weightEntries,
                onEditEntry = { /* TODO: Implement edit */ },
                onDeleteEntry = { entry ->
                    viewModel.deleteWeightEntry(entry)
                }
            )
        }
    }

    if (showWeightInputDialog) {
        WeightInputDialog(
            onDismiss = { showWeightInputDialog = false },
            onConfirm = { weight, bagWeight, impurityWeight ->
                viewModel.addWeightEntry(cardId, weight, bagWeight, impurityWeight)
                showWeightInputDialog = false
            }
        )
    }
}

@Composable
private fun CardInfoSection(card: Card) {
    val numberFormat = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Thông tin Card",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            card.cccd?.let { cccd ->
                Text("CCCD: $cccd")
            }

            Text("Ngày: ${dateFormat.format(card.date)}")
            Text("Tổng khối lượng: ${numberFormat.format(card.totalWeight)} kg")
            Text("Số bao: ${card.bagCount}")
            Text("Khối lượng bao bì: ${numberFormat.format(card.bagWeight)} kg")
            Text("Khối lượng tạp chất: ${numberFormat.format(card.impurityWeight)} kg")
            Text("Khối lượng đã trừ: ${numberFormat.format(card.netWeight)} kg")
            Text("Đơn giá/kg: ${numberFormat.format(card.pricePerKg)} đ")
            Text("Thành tiền: ${numberFormat.format(card.totalAmount)} đ")
            Text("Tiền cọc: ${numberFormat.format(card.depositAmount)} đ")
            Text("Đã trả: ${numberFormat.format(card.paidAmount)} đ")
            Text(
                text = "Còn lại: ${numberFormat.format(card.remainingAmount)} đ",
                color = if (card.remainingAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun WeightEntriesSection(
    weightEntries: List<com.GiaThinh.canlua.data.model.WeightEntry>,
    onEditEntry: (com.GiaThinh.canlua.data.model.WeightEntry) -> Unit,
    onDeleteEntry: (com.GiaThinh.canlua.data.model.WeightEntry) -> Unit
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale("vi", "VN"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Danh sách cân nặng",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (weightEntries.isEmpty()) {
                Text(
                    text = "Chưa có dữ liệu cân nặng",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                weightEntries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bao ${index + 1}: ${numberFormat.format(entry.netWeight)} kg")
                        Text(
                            text = "Tổng: ${numberFormat.format(entry.weight)} kg",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
