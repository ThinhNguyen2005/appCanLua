package com.GiaThinh.canlua.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.ui.viewmodel.CardViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightInputScreen(
    cardId: Long,
    navController: NavController,
    viewModel: CardViewModel = hiltViewModel()
) {
    val currentCard by viewModel.currentCard.collectAsState()
    var currentWeight by remember { mutableStateOf("") }
    var bagWeight by remember { mutableStateOf("0") }
    var impurityWeight by remember { mutableStateOf("0") }
    var isLocked by remember { mutableStateOf(false) }

    LaunchedEffect(cardId) {
        viewModel.loadCardById(cardId)
    }

    val card = currentCard ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nhập cân nặng - ${card.name}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isLocked = !isLocked }
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Check,
                            contentDescription = if (isLocked) "Mở khóa" else "Khóa"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current weight display
            WeightDisplaySection(
                currentWeight = currentWeight,
                bagWeight = bagWeight,
                impurityWeight = impurityWeight,
                isLocked = isLocked
            )

            // Number pad
            NumberPadSection(
                onNumberClick = { number ->
                    if (!isLocked) {
                        currentWeight += number
                    }
                },
                onClearClick = {
                    if (!isLocked) {
                        currentWeight = ""
                    }
                },
                onBackspaceClick = {
                    if (!isLocked && currentWeight.isNotEmpty()) {
                        currentWeight = currentWeight.dropLast(1)
                    }
                },
                onConfirmClick = {
                    if (!isLocked && currentWeight.isNotBlank()) {
                        val weight = currentWeight.toDoubleOrNull() ?: 0.0
                        val bagW = bagWeight.toDoubleOrNull() ?: 0.0
                        val impurityW = impurityWeight.toDoubleOrNull() ?: 0.0
                        
                        if (weight > 0) {
                            viewModel.addWeightEntry(cardId, weight, bagW, impurityW)
                            currentWeight = ""
                        }
                    }
                },
                isLocked = isLocked
            )

            // Bag weight and impurity weight inputs
            WeightAdjustmentSection(
                bagWeight = bagWeight,
                impurityWeight = impurityWeight,
                onBagWeightChange = { bagWeight = it },
                onImpurityWeightChange = { impurityWeight = it },
                isLocked = isLocked
            )
        }
    }
}

@Composable
private fun WeightDisplaySection(
    currentWeight: String,
    bagWeight: String,
    impurityWeight: String,
    isLocked: Boolean
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Cân nặng hiện tại",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (currentWeight.isEmpty()) "0.00" else "${String.format("%.2f", currentWeight.toDoubleOrNull() ?: 0.0)}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = if (isLocked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "kg",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Bao bì",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.2f", bagWeight.toDoubleOrNull() ?: 0.0)} kg",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Tạp chất",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.2f", impurityWeight.toDoubleOrNull() ?: 0.0)} kg",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val totalWeight = currentWeight.toDoubleOrNull() ?: 0.0
            val bagW = bagWeight.toDoubleOrNull() ?: 0.0
            val impurityW = impurityWeight.toDoubleOrNull() ?: 0.0
            val netWeight = totalWeight - bagW - impurityW
            
            Text(
                text = "Thực tế: ${String.format("%.2f", netWeight)} kg",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun NumberPadSection(
    onNumberClick: (String) -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onConfirmClick: () -> Unit,
    isLocked: Boolean
) {
    val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", ".", "00")
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(300.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(numbers) { number ->
            Button(
                onClick = { onNumberClick(number) },
                modifier = Modifier.aspectRatio(1f),
                enabled = !isLocked
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
        
        // Clear button
        item {
            Button(
                onClick = onClearClick,
                modifier = Modifier.aspectRatio(1f),
                enabled = !isLocked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Xóa")
            }
        }
        
        // Backspace button
        item {
            Button(
                onClick = onBackspaceClick,
                modifier = Modifier.aspectRatio(1f),
                enabled = !isLocked
            ) {
                Text("⌫", style = MaterialTheme.typography.headlineSmall)
            }
        }
        
        // Confirm button
        item {
            Button(
                onClick = onConfirmClick,
                modifier = Modifier.aspectRatio(1f),
                enabled = !isLocked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text("✓", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Composable
private fun WeightAdjustmentSection(
    bagWeight: String,
    impurityWeight: String,
    onBagWeightChange: (String) -> Unit,
    onImpurityWeightChange: (String) -> Unit,
    isLocked: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Điều chỉnh khối lượng",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = bagWeight,
                    onValueChange = onBagWeightChange,
                    label = { Text("Bao bì (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    enabled = !isLocked
                )
                
                OutlinedTextField(
                    value = impurityWeight,
                    onValueChange = onImpurityWeightChange,
                    label = { Text("Tạp chất (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    enabled = !isLocked
                )
            }
        }
    }
}
