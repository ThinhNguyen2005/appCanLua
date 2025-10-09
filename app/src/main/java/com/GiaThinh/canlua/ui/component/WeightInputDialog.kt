package com.GiaThinh.canlua.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun WeightInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, Double) -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var bagWeight by remember { mutableStateOf("0") }
    var impurityWeight by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nhập cân nặng") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Tổng cân nặng (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bagWeight,
                    onValueChange = { bagWeight = it },
                    label = { Text("Khối lượng bao bì (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = impurityWeight,
                    onValueChange = { impurityWeight = it },
                    label = { Text("Khối lượng tạp chất (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                val totalWeight = weight.toDoubleOrNull() ?: 0.0
                val bagW = bagWeight.toDoubleOrNull() ?: 0.0
                val impurityW = impurityWeight.toDoubleOrNull() ?: 0.0
                val netWeight = totalWeight - bagW - impurityW

                if (weight.isNotBlank()) {
                    Text(
                        text = "Khối lượng thực tế: ${String.format("%.2f", netWeight)} kg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val w = weight.toDoubleOrNull() ?: 0.0
                    val bw = bagWeight.toDoubleOrNull() ?: 0.0
                    val iw = impurityWeight.toDoubleOrNull() ?: 0.0
                    onConfirm(w, bw, iw)
                },
                enabled = weight.isNotBlank() && weight.toDoubleOrNull() != null
            ) {
                Text("Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

