package com.GiaThinh.canlua.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun CreateCardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var cccd by remember { mutableStateOf("") }
    var pricePerKg by remember { mutableStateOf("") }
    var depositAmount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo Card Mới") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cccd,
                    onValueChange = { cccd = it },
                    label = { Text("CCCD (tùy chọn)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pricePerKg,
                    onValueChange = { pricePerKg = it },
                    label = { Text("Đơn giá/kg (VNĐ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = depositAmount,
                    onValueChange = { depositAmount = it },
                    label = { Text("Tiền cọc (VNĐ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val price = pricePerKg.toDoubleOrNull() ?: 0.0
                    val deposit = depositAmount.toDoubleOrNull() ?: 0.0
                    onConfirm(name.trim(), cccd.takeIf { it.isNotBlank() }, price, deposit)
                },
                enabled = name.isNotBlank() && pricePerKg.isNotBlank()
            ) {
                Text("Tạo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

