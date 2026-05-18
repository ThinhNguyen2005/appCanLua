package com.GiaThinh.canlua.ui.component.weight

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.ui.component.RiceVarietyDropdown
import com.GiaThinh.canlua.ui.theme.AppColors

/**
 * Card 1/3: Thông tin lô hàng — tên, giống lúa, độ ẩm, vụ mùa.
 */
@Composable
fun LotInfoCard(
    farmerName: String,
    riceVariety: String,
    moisturePercent: Double,
    seasonLabel: String,
    isLocked: Boolean,
    onNameChange: (String) -> Unit,
    onVarietyChange: (String) -> Unit,
    onMoistureChange: (Double) -> Unit,
    onSeasonChange: (String) -> Unit
) {
    var moistureText by remember(moisturePercent) {
        mutableStateOf(if (moisturePercent > 0) "%.1f".format(moisturePercent) else "")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Person, null, tint = AppColors.GreenPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Thông Tin Lô", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // Tên nông dân
            OutlinedTextField(
                value = farmerName,
                onValueChange = { if (!isLocked) onNameChange(it) },
                label = { Text("Tên nông dân") },
                enabled = !isLocked,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Giống lúa
            RiceVarietyDropdown(
                selected = riceVariety,
                onSelect = { if (!isLocked) onVarietyChange(it) },
                modifier = Modifier.fillMaxWidth()
            )

            // Độ ẩm + Vụ mùa
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = moistureText,
                    onValueChange = {
                        if (!isLocked) {
                            moistureText = it
                            it.toDoubleOrNull()?.let { v -> onMoistureChange(v) }
                        }
                    },
                    label = { Text("Độ ẩm (%)") },
                    leadingIcon = { Icon(Icons.Outlined.WaterDrop, null, modifier = Modifier.size(18.dp)) },
                    enabled = !isLocked,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = seasonLabel,
                    onValueChange = { if (!isLocked) onSeasonChange(it) },
                    label = { Text("Vụ mùa") },
                    enabled = !isLocked,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}
