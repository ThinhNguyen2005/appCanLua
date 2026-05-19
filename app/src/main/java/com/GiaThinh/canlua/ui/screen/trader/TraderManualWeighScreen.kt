package com.GiaThinh.canlua.ui.screen.trader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.util.MoneyVisualTransformation
import java.text.NumberFormat
import java.util.Locale

/**
 * Calculator cân lúa đối chiếu cho thương lái.
 *
 * Mục đích: cho phép trader tự nhập kg từng bao để có 1 con số "phía mình" — so với
 * con số phía nông dân. KHÔNG ghi vào DB phiếu gốc của nông dân (Single Source of Truth).
 *
 * Field nhập gồm:
 *  - Giá / kg
 *  - Trọng lượng bao bì / bao
 *  - Tỉ lệ tạp chất %
 *  - Danh sách kg gross của từng bao (thêm bằng nút +)
 *
 * Live calculation:
 *  - Tổng kg gross
 *  - Trừ bao bì = số bao × bagWeight
 *  - Trừ tạp = (gross - bao bì) × impurity%
 *  - Net kg
 *  - Thành tiền = net × giá/kg
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraderManualWeighScreen(
    navController: NavController
) {
    var pricePerKg by remember { mutableStateOf("") }
    var bagWeightPerBag by remember { mutableStateOf("1.5") }
    var impurityPercent by remember { mutableStateOf("0") }

    val bagsKg = remember { mutableStateListOf<String>() }

    // Parse helpers — chấp nhận dấu phẩy thập phân (vi-VN)
    fun parseDouble(s: String): Double = s.replace(",", ".").toDoubleOrNull() ?: 0.0

    val price = parseDouble(pricePerKg)
    val bagW = parseDouble(bagWeightPerBag)
    val impurity = parseDouble(impurityPercent).coerceIn(0.0, 100.0)

    val gross = bagsKg.sumOf { parseDouble(it) }
    val bagCount = bagsKg.count { parseDouble(it) > 0.0 }
    val totalBagDeduction = bagW * bagCount
    val afterBag = (gross - totalBagDeduction).coerceAtLeast(0.0)
    val impurityDeduction = afterBag * (impurity / 100.0)
    val net = (afterBag - impurityDeduction).coerceAtLeast(0.0)
    val totalAmount = net * price

    val numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
    val moneyTransform = MoneyVisualTransformation

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cân Đối Chiếu") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                actions = {
                    if (bagsKg.isNotEmpty()) {
                        IconButton(onClick = { bagsKg.clear() }) {
                            Icon(Icons.Filled.Refresh, "Xoá hết")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = AppColors.Surface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // === Live result hero ===
            item {
                ResultHero(
                    bagCount = bagCount,
                    grossKg = gross,
                    netKg = net,
                    totalAmount = totalAmount,
                    formatter = numberFormat
                )
            }

            // === Tham số tính ===
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tham số tính",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        OutlinedTextField(
                            value = pricePerKg,
                            onValueChange = { new -> pricePerKg = new.filter { it.isDigit() } },
                            label = { Text("Giá / kg (đ)") },
                            visualTransformation = moneyTransform,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = bagWeightPerBag,
                            onValueChange = { bagWeightPerBag = it },
                            label = { Text("Bao bì / bao (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = impurityPercent,
                            onValueChange = { impurityPercent = it },
                            label = { Text("Tạp chất (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // === Danh sách bao ===
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Danh sách bao (${bagsKg.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    TextButton(onClick = { bagsKg.add("") }) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Thêm bao")
                    }
                }
            }

            if (bagsKg.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { bagsKg.add("") }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.Calculate,
                                null,
                                tint = AppColors.GreenPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Bấm để thêm bao đầu tiên",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(bagsKg, key = { i, _ -> i }) { index, value ->
                    BagRow(
                        index = index,
                        value = value,
                        onChange = { bagsKg[index] = it },
                        onRemove = { bagsKg.removeAt(index) }
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { bagsKg.add("") },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Thêm bao", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun ResultHero(
    bagCount: Int,
    grossKg: Double,
    netKg: Double,
    totalAmount: Double,
    formatter: NumberFormat
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(AppColors.GreenPrimary, AppColors.GreenDark)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Số bao: $bagCount  ·  Tổng gross: ${formatter.format(grossKg)} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${formatter.format(netKg)} kg",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Khối lượng thực",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${formatter.format(totalAmount.toLong())} đ",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.GoldLight
                )
                Text(
                    text = "Thành tiền dự kiến",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun BagRow(
    index: Int,
    value: String,
    onChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppColors.GreenSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.GreenPrimary
                )
            }
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text("Kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Xoá bao ${index + 1}",
                    tint = AppColors.Error
                )
            }
        }
    }
}
