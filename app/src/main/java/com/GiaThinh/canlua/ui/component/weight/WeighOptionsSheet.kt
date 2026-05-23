package com.GiaThinh.canlua.ui.component.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeighOptionsSheet(
    impurityIsPercent: Boolean,
    bagMethodIsSampling: Boolean,
    bagSampleCount: Int,
    bagSampleTotalWeight: Double,
    weightInputMode: String,
    onDismiss: () -> Unit,
    onSave: (
        impurityIsPercent: Boolean,
        bagMethodIsSampling: Boolean,
        bagSampleCount: Int,
        bagSampleTotalWeight: Double,
        weightInputMode: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var impurityPercent by remember { mutableStateOf(impurityIsPercent) }
    var bagSampling by remember { mutableStateOf(bagMethodIsSampling) }
    var sampleCountText by remember {
        mutableStateOf(if (bagSampleCount > 0) bagSampleCount.toString() else "")
    }
    var sampleWeightText by remember {
        mutableStateOf(if (bagSampleTotalWeight > 0.0) bagSampleTotalWeight.toString() else "")
    }
    var inputMode by remember { mutableStateOf(weightInputMode.ifBlank { "SMALL" }) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.weigh_options_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            // === 1. Impurity unit ===
            SectionHeader(
                icon = { Icon(Icons.Outlined.Spa, null, tint = AppColors.GreenPrimary, modifier = Modifier.size(18.dp)) },
                title = stringResource(R.string.weigh_options_impurity_unit)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = !impurityPercent,
                    onClick = { impurityPercent = false },
                    label = { Text(stringResource(R.string.weigh_options_impurity_kg)) },
                    colors = greenChipColors()
                )
                FilterChip(
                    selected = impurityPercent,
                    onClick = { impurityPercent = true },
                    label = { Text(stringResource(R.string.weigh_options_impurity_percent)) },
                    colors = greenChipColors()
                )
            }
            Text(
                text = stringResource(
                    if (impurityPercent) R.string.weigh_options_impurity_hint_percent
                    else R.string.weigh_options_impurity_hint_kg
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )

            // === 2. Bag method ===
            SectionHeader(
                icon = { Icon(Icons.Outlined.Inventory2, null, tint = AppColors.GreenPrimary, modifier = Modifier.size(18.dp)) },
                title = stringResource(R.string.weigh_options_bag_method)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = !bagSampling,
                    onClick = { bagSampling = false },
                    label = { Text(stringResource(R.string.weigh_options_bag_single)) },
                    colors = greenChipColors()
                )
                FilterChip(
                    selected = bagSampling,
                    onClick = { bagSampling = true },
                    label = { Text(stringResource(R.string.weigh_options_bag_sample)) },
                    colors = greenChipColors()
                )
            }
            Text(
                text = stringResource(
                    if (bagSampling) R.string.weigh_options_bag_hint_sample
                    else R.string.weigh_options_bag_hint_single
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
            if (bagSampling) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = sampleCountText,
                        onValueChange = { v -> sampleCountText = v.filter { it.isDigit() } },
                        label = { Text(stringResource(R.string.weigh_options_bag_sample_count)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sampleWeightText,
                        onValueChange = { v ->
                            sampleWeightText = v.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                                .replace(',', '.')
                        },
                        label = { Text(stringResource(R.string.weigh_options_bag_sample_weight)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // === 3. KG input scale ===
            SectionHeader(
                icon = { Icon(Icons.Outlined.Scale, null, tint = AppColors.GreenPrimary, modifier = Modifier.size(18.dp)) },
                title = stringResource(R.string.weigh_options_input_scale)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = inputMode == "SMALL",
                    onClick = { inputMode = "SMALL" },
                    label = { Text(stringResource(R.string.weigh_options_input_small)) },
                    colors = greenChipColors()
                )
                FilterChip(
                    selected = inputMode == "LARGE",
                    onClick = { inputMode = "LARGE" },
                    label = { Text(stringResource(R.string.weigh_options_input_large)) },
                    colors = greenChipColors()
                )
            }
            Text(
                text = stringResource(
                    if (inputMode == "LARGE") R.string.weigh_options_input_hint_large
                    else R.string.weigh_options_input_hint_small
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    val n = sampleCountText.toIntOrNull() ?: 0
                    val w = sampleWeightText.toDoubleOrNull() ?: 0.0
                    onSave(impurityPercent, bagSampling, n, w, inputMode)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary)
            ) {
                Text(stringResource(R.string.weigh_options_done), color = AppColors.CardBg, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: @Composable () -> Unit, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        icon()
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
    }
}

@Composable
private fun greenChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = AppColors.GreenPrimary,
    selectedLabelColor = AppColors.CardBg
)
