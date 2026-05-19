package com.GiaThinh.canlua.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.GiaThinh.canlua.ui.theme.AppColors
import java.util.Locale

/**
 * Dialog tạo phiếu cân mới — v2.1
 * Tối ưu diện tích hiển thị, tích hợp Dynamic Theme, tự động định dạng tiền tệ.
 */
@Composable
fun CreateCardDialog(
    farmerName: String,            // Lấy tự động từ profile, hiển thị read-only
    onDismiss: () -> Unit,
    onCreate: (
        traderName: String,
        riceVariety: String,
        seasonLabel: String,
        moisturePercent: Double,
        pricePerKg: Double,
        depositAmount: Double
    ) -> Unit
) {
    // ── State ────────────────────────────────────────────────────────────────
    var traderName      by remember { mutableStateOf("") }
    var riceVariety     by remember { mutableStateOf("") }
    var seasonLabel     by remember { mutableStateOf("") }

    // Lưu chuỗi số thô, hiển thị được format qua VisualTransformation
    var moistureRaw     by remember { mutableStateOf("") }   // "18.2" -> 18.2%
    var priceRaw        by remember { mutableStateOf("") }   // "8200" -> 8.200 đ
    var depositRaw      by remember { mutableStateOf("") }   // "500000" -> 500.000 đ

    // Validation: Yêu cầu tối thiểu tên thương lái và giống lúa
    val isValid = traderName.isNotBlank() && riceVariety.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false  // Cho phép custom chiều rộng linh hoạt
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.97f)         // Tăng chiều rộng lên 97% màn hình
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = AppColors.CardBg,        // Tương thích với Dynamic Theme
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Header ────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.GreenSurface)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Column {
                        Text(
                            text = "Tạo Phiếu Cân Mới",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.GreenPrimary
                        )
                        
                        // Thông tin Nông dân: Đã có sẵn trong profile, hiển thị read-only
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AppColors.TextSecondary
                            )
                            Text(
                                text = "Nông dân: $farmerName",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // ── Form (Scrollable) ─────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)          // Không chiếm toàn bộ chiều cao màn hình
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // Section: Thông tin lô hàng
                    SectionLabel("THÔNG TIN LÔ HÀNG")

                    // Row 1: [Giống lúa ▼] [Vụ mùa] — 2 cột song song tiết kiệm diện tích đứng
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

                    // Row 2: Tên thương lái — full width
                    FormTextField(
                        value = traderName,
                        onValueChange = { traderName = it },
                        label = "Tên thương lái *",
                        placeholder = "Nhập tên thương lái mua lúa",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(4.dp))

                    // Section: Giá & Thanh toán
                    SectionLabel("GIÁ & THANH TOÁN")

                    // Row 3: [Độ ẩm %] [Đơn giá đ/kg] — BẰNG NHAU (weight = 1f cả 2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Độ ẩm (%)
                        OutlinedTextField(
                            value = moistureRaw,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() || it == '.' }
                                if (filtered.length <= 4) moistureRaw = filtered
                            },
                            label = { Text("Độ ẩm (%)") },
                            placeholder = { Text("VD: 18.5", style = MaterialTheme.typography.bodySmall) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).heightIn(min = 60.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = dialogTextFieldColors()
                        )

                        // Đơn giá (đ/kg) — Tự động thêm dấu chấm phân cách hàng ngàn
                        OutlinedTextField(
                            value = priceRaw,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }
                                if (digits.length <= 7) priceRaw = digits  // Giới hạn hợp lý dưới 10 triệu/kg
                            },
                            label = { Text("Đơn giá (đ/kg)") },
                            placeholder = { Text("VD: 8.200", style = MaterialTheme.typography.bodySmall) },
                            visualTransformation = ThousandSeparatorTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).heightIn(min = 60.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = dialogTextFieldColors()
                        )
                    }

                    // Row 4: Tiền cọc — full width
                    OutlinedTextField(
                        value = depositRaw,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 10) depositRaw = digits
                        },
                        label = { Text("Tiền cọc (đ)") },
                        placeholder = { Text("Tuỳ chọn — VD: 500.000", style = MaterialTheme.typography.bodySmall) },
                        visualTransformation = ThousandSeparatorTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = dialogTextFieldColors()
                    )
                }

                // ── Buttons — LUÔN visible cố định ở đáy, không bị che khuất ──
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = AppColors.Divider
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Huỷ",
                            color = AppColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Button(
                        onClick = {
                            onCreate(
                                traderName.trim(),
                                riceVariety,
                                seasonLabel.trim(),
                                moistureRaw.toDoubleOrNull() ?: 0.0,
                                priceRaw.toDoubleOrNull() ?: 0.0,
                                depositRaw.toDoubleOrNull() ?: 0.0
                            )
                            onDismiss()
                        },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.GreenPrimary,
                            disabledContainerColor = AppColors.GreenPrimary.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Tạo Phiếu",
                            fontWeight = FontWeight.Bold,
                            color = AppColors.CardBg
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VisualTransformation: Tự động chèn dấu chấm phân cách phần ngàn
// Ví dụ: Nhập "8200" -> Hiển thị "8.200"
// ─────────────────────────────────────────────────────────────────────────────
class ThousandSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        // Định dạng số chuẩn locale Việt Nam sử dụng dấu chấm phân cách ngàn
        val formatted = try {
            val number = original.toLong()
            String.format(Locale.forLanguageTag("vi-VN"), "%,d", number)
        } catch (e: Exception) {
            original
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                var transformedOffset = 0
                var originalOffset = 0
                while (transformedOffset < formatted.length && originalOffset < offset) {
                    if (formatted[transformedOffset].isDigit()) {
                        originalOffset++
                    }
                    transformedOffset++
                }
                // Di chuyển cursor qua dấu phân cách nếu ký tự kế tiếp không phải số
                while (transformedOffset < formatted.length && !formatted[transformedOffset].isDigit()) {
                    transformedOffset++
                }
                return transformedOffset.coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val safeOffset = offset.coerceAtMost(formatted.length)
                return formatted.take(safeOffset).count { it.isDigit() }.coerceAtMost(original.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = AppColors.GreenPrimary,
        letterSpacing = androidx.compose.ui.unit.TextUnit(
            1f, androidx.compose.ui.unit.TextUnitType.Sp
        )
    )
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        modifier = modifier.heightIn(min = 60.dp),
        shape = RoundedCornerShape(14.dp),
        colors = dialogTextFieldColors()
    )
}

@Composable
private fun dialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AppColors.TextPrimary,
    unfocusedTextColor = AppColors.TextPrimary,
    focusedLabelColor = AppColors.GreenPrimary,
    unfocusedLabelColor = AppColors.TextHint,
    focusedBorderColor = AppColors.GreenPrimary,
    unfocusedBorderColor = AppColors.Divider
)
