package com.GiaThinh.canlua.ui.component.market

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.data.firestore.FirestoreRicePrice
import com.GiaThinh.canlua.ui.component.RiceVarietyDropdown
import com.GiaThinh.canlua.ui.component.ThousandSeparatorTransformation
import com.GiaThinh.canlua.ui.theme.AppColors

/**
 * Form đăng / sửa giá lúa cho TRADER. Dùng trong ModalBottomSheet ở MarketScreen.
 *
 * @param existing Bid hiện có (edit mode), null = create mode.
 */
@Composable
fun BidEditorSheet(
    existing: FirestoreRicePrice? = null,
    isSaving: Boolean = false,
    onSubmit: (
        variety: String,
        priceMin: Double,
        priceMax: Double,
        region: String,
        trend: String,
        note: String,
        existingId: String?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var variety by remember { mutableStateOf(existing?.variety.orEmpty()) }
    var priceMinRaw by remember {
        mutableStateOf(existing?.priceMin?.toLong()?.toString().orEmpty())
    }
    var priceMaxRaw by remember {
        mutableStateOf(existing?.priceMax?.toLong()?.toString().orEmpty())
    }
    var region by remember { mutableStateOf(existing?.region.orEmpty()) }
    var trend by remember { mutableStateOf(existing?.trend ?: "STABLE") }
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }

    val isValid = variety.isNotBlank() &&
            priceMinRaw.isNotBlank() &&
            priceMaxRaw.isNotBlank() &&
            (priceMinRaw.toDoubleOrNull() ?: 0.0) > 0.0 &&
            (priceMaxRaw.toDoubleOrNull() ?: 0.0) >=
            (priceMinRaw.toDoubleOrNull() ?: 0.0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (existing == null) "Đăng giá thu mua mới" else "Cập nhật giá",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.GreenPrimary,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Giá lúa của bạn sẽ hiện công khai ở Bảng giá thị trường cho nông dân tham khảo.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextHint
        )

        // Giống lúa — dropdown chuẩn hoá
        RiceVarietyDropdown(
            selected = variety,
            onSelect = { variety = it },
            modifier = Modifier.fillMaxWidth()
        )

        // Giá min / max
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = priceMinRaw,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }
                    if (digits.length <= 7) priceMinRaw = digits
                },
                label = { Text("Giá thấp (đ/kg)") },
                placeholder = { Text("VD: 8.000", style = MaterialTheme.typography.bodySmall) },
                visualTransformation = ThousandSeparatorTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).heightIn(min = 60.dp),
                shape = RoundedCornerShape(14.dp),
                colors = sheetTextFieldColors()
            )
            OutlinedTextField(
                value = priceMaxRaw,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }
                    if (digits.length <= 7) priceMaxRaw = digits
                },
                label = { Text("Giá cao (đ/kg)") },
                placeholder = { Text("VD: 8.500", style = MaterialTheme.typography.bodySmall) },
                visualTransformation = ThousandSeparatorTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).heightIn(min = 60.dp),
                shape = RoundedCornerShape(14.dp),
                colors = sheetTextFieldColors()
            )
        }

        OutlinedTextField(
            value = region,
            onValueChange = { region = it },
            label = { Text("Khu vực thu mua") },
            placeholder = { Text("VD: Cần Thơ, Tiền Giang...", style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            shape = RoundedCornerShape(14.dp),
            colors = sheetTextFieldColors()
        )

        // Xu hướng
        Text(
            text = "XU HƯỚNG GIÁ",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextSecondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrendChip("UP", "Đang tăng", AppColors.Success, trend) { trend = it }
            TrendChip("STABLE", "Ổn định", AppColors.Info, trend) { trend = it }
            TrendChip("DOWN", "Đang giảm", AppColors.Error, trend) { trend = it }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { if (it.length <= 200) note = it },
            label = { Text("Ghi chú (tuỳ chọn)") },
            placeholder = { Text("VD: Mua tại ruộng, ưu tiên lúa khô...", style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            shape = RoundedCornerShape(14.dp),
            colors = sheetTextFieldColors()
        )

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("Huỷ", color = AppColors.TextSecondary, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = {
                    onSubmit(
                        variety,
                        priceMinRaw.toDoubleOrNull() ?: 0.0,
                        priceMaxRaw.toDoubleOrNull() ?: 0.0,
                        region,
                        trend,
                        note,
                        existing?.id
                    )
                },
                enabled = isValid && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.GreenPrimary,
                    disabledContainerColor = AppColors.GreenPrimary.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isSaving) "Đang lưu..." else if (existing == null) "Đăng giá" else "Cập nhật",
                    fontWeight = FontWeight.Bold,
                    color = AppColors.CardBg
                )
            }
        }
    }
}

@Composable
private fun TrendChip(
    key: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    selected: String,
    onSelect: (String) -> Unit
) {
    val isSelected = key == selected
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) color.copy(alpha = 0.18f) else AppColors.SurfaceContainer)
            .clickable { onSelect(key) }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) color else AppColors.TextSecondary
        )
    }
}

@Composable
private fun sheetTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AppColors.TextPrimary,
    unfocusedTextColor = AppColors.TextPrimary,
    focusedLabelColor = AppColors.GreenPrimary,
    unfocusedLabelColor = AppColors.TextHint,
    focusedBorderColor = AppColors.GreenPrimary,
    unfocusedBorderColor = AppColors.Divider
)
