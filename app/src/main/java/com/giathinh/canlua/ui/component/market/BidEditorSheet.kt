package com.giathinh.canlua.ui.component.market

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.R
import com.giathinh.canlua.data.firestore.FirestoreRicePrice
import com.giathinh.canlua.ui.component.RiceVarietyDropdown
import com.giathinh.canlua.ui.component.ThousandSeparatorTransformation
import com.giathinh.canlua.ui.theme.AppColors

private data class RiceTypeOption(val key: String, val labelRes: Int)

private val RICE_TYPES = listOf(
    RiceTypeOption("lúa ướt", R.string.market_rice_type_wet),
    RiceTypeOption("lúa Khô", R.string.market_rice_type_dry),
    RiceTypeOption("gạo", R.string.market_rice_type_milled),
    RiceTypeOption("tấm", R.string.market_rice_type_broken),
    RiceTypeOption("nếp", R.string.market_rice_type_glutinous)
)

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
        riceType: String,
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
    var riceType by remember { mutableStateOf(existing?.riceType ?: "lúa Khô") }

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
            text = if (existing == null) {
                stringResource(R.string.market_bid_sheet_title_create)
            } else {
                stringResource(R.string.market_bid_sheet_title_edit)
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.GreenPrimary,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = stringResource(R.string.market_bid_sheet_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextHint
        )

        // Giống lúa — dropdown chuẩn hoá
        RiceVarietyDropdown(
            selected = variety,
            onSelect = { variety = it },
            modifier = Modifier.fillMaxWidth()
        )

        // Loại sản phẩm
        Text(
            text = stringResource(R.string.market_bid_section_product_type),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextSecondary
        )
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(RICE_TYPES.size) { idx ->
                val typeOption = RICE_TYPES[idx]
                TypeChip(
                    key = typeOption.key,
                    label = stringResource(typeOption.labelRes),
                    selected = riceType,
                    onSelect = { riceType = it }
                )
            }
        }

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
                label = { Text(stringResource(R.string.market_bid_price_min_label)) },
                placeholder = { Text(stringResource(R.string.market_bid_price_min_placeholder), style = MaterialTheme.typography.bodySmall) },
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
                label = { Text(stringResource(R.string.market_bid_price_max_label)) },
                placeholder = { Text(stringResource(R.string.market_bid_price_max_placeholder), style = MaterialTheme.typography.bodySmall) },
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
            label = { Text(stringResource(R.string.market_bid_region_label)) },
            placeholder = { Text(stringResource(R.string.market_bid_region_placeholder), style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            shape = RoundedCornerShape(14.dp),
            colors = sheetTextFieldColors()
        )

        // Xu hướng
        Text(
            text = stringResource(R.string.market_bid_trend_section),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextSecondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrendChip("UP", stringResource(R.string.market_bid_trend_up), AppColors.Success, trend) { trend = it }
            TrendChip("STABLE", stringResource(R.string.market_bid_trend_stable), AppColors.Info, trend) { trend = it }
            TrendChip("DOWN", stringResource(R.string.market_bid_trend_down), AppColors.Error, trend) { trend = it }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { if (it.length <= 200) note = it },
            label = { Text(stringResource(R.string.market_bid_note_label)) },
            placeholder = { Text(stringResource(R.string.market_bid_note_placeholder), style = MaterialTheme.typography.bodySmall) },
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
                Text(stringResource(R.string.action_cancel), color = AppColors.TextSecondary, fontWeight = FontWeight.Medium)
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
                        riceType,
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
                    text = if (isSaving) {
                        stringResource(R.string.market_bid_saving)
                    } else if (existing == null) {
                        stringResource(R.string.market_bid_submit_create)
                    } else {
                        stringResource(R.string.market_bid_submit_update)
                    },
                    fontWeight = FontWeight.Bold,
                    color = AppColors.CardBg
                )
            }
        }
    }
}

@Composable
private fun TypeChip(
    key: String,
    label: String,
    selected: String,
    onSelect: (String) -> Unit
) {
    val isSelected = key == selected
    val activeColor = AppColors.GreenPrimary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.18f) else AppColors.SurfaceContainer)
            .clickable { onSelect(key) }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) activeColor else AppColors.TextSecondary
        )
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
