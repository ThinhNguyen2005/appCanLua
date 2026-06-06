package com.GiaThinh.canlua.ui.component.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.data.model.FontScale
import com.GiaThinh.canlua.ui.theme.AppColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeighOptionsSheet(
    impurityIsPercent: Boolean,
    bagMethodIsSampling: Boolean,
    bagSampleCount: Int,
    bagSampleTotalWeight: Double,
    weightInputMode: String,
    ttsEnabled: Boolean,
    fontScale: FontScale,
    onDismiss: () -> Unit,
    onSave: (
        impurityIsPercent: Boolean,
        bagMethodIsSampling: Boolean,
        bagSampleCount: Int,
        bagSampleTotalWeight: Double,
        weightInputMode: String,
        ttsEnabled: Boolean,
        fontScale: FontScale
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var bagSampling by remember { mutableStateOf(bagMethodIsSampling) }
    var sampleCountText by remember {
        mutableStateOf(if (bagSampleCount > 0) bagSampleCount.toString() else "")
    }
    var sampleWeightText by remember {
        mutableStateOf(if (bagSampleTotalWeight > 0.0) bagSampleTotalWeight.toString() else "")
    }
    var inputMode by remember { mutableStateOf(weightInputMode.ifBlank { "SMALL" }) }
    var ttsLocal by remember { mutableStateOf(ttsEnabled) }

    val fontOptions = listOf(FontScale.SMALL, FontScale.NORMAL, FontScale.LARGE, FontScale.XLARGE)
    var currentFontScale by remember { mutableStateOf(fontScale) }
    var sliderPosition by remember { mutableFloatStateOf(fontOptions.indexOf(fontScale).coerceAtLeast(0).toFloat()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.weigh_options_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            // === Card 1: Cá nhân hóa & hiển thị ===
            Text(
                text = "CÁ NHÂN HÓA & HIỂN THỊ",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Row 1: Đọc số khi nhập (TTS)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = if (ttsLocal) AppColors.GreenPrimary else AppColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_tts_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.settings_tts_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextSecondary
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = ttsLocal,
                            onCheckedChange = { ttsLocal = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppColors.GreenPrimary,
                                uncheckedThumbColor = AppColors.TextHint,
                                uncheckedTrackColor = AppColors.SurfaceContainer
                            )
                        )
                    }

                    HorizontalDivider(color = AppColors.Divider.copy(alpha = 0.3f), thickness = 0.5.dp)

                    // Row 2: Cỡ chữ hiển thị
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TextFields,
                                contentDescription = null,
                                tint = AppColors.GreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.settings_font_size_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = stringResource(currentFontScale.labelRes),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.GreenPrimary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("A", fontSize = 12.sp, color = AppColors.TextHint, fontWeight = FontWeight.Normal)
                            Slider(
                                value = sliderPosition,
                                onValueChange = { newVal ->
                                    sliderPosition = newVal
                                    val idx = newVal.roundToInt().coerceIn(0, fontOptions.lastIndex)
                                    currentFontScale = fontOptions[idx]
                                },
                                valueRange = 0f..3f,
                                steps = 2,
                                colors = SliderDefaults.colors(
                                    thumbColor = AppColors.GreenPrimary,
                                    activeTrackColor = AppColors.GreenPrimary,
                                    inactiveTrackColor = AppColors.SurfaceContainer,
                                    activeTickColor = AppColors.GreenPrimary,
                                    inactiveTickColor = AppColors.TextHint
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Text("A", fontSize = 22.sp, color = AppColors.GreenPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // === Card 2: Hình thức trừ bao bì ===
            Text(
                text = "HÌNH THỨC TRỪ BAO BÌ",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Segmented Control
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AppColors.SurfaceContainer,
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val options = listOf(
                                false to stringResource(R.string.weigh_options_bag_single),
                                true to stringResource(R.string.weigh_options_bag_sample)
                            )
                            options.forEach { (isSample, label) ->
                                val isSelected = bagSampling == isSample
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) AppColors.GreenPrimary else Color.Transparent)
                                        .clickable { bagSampling = isSample },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else AppColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = stringResource(
                            if (bagSampling) R.string.weigh_options_bag_hint_sample
                            else R.string.weigh_options_bag_hint_single
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextHint
                    )

                    if (bagSampling) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppColors.GreenSurface.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, AppColors.GreenPrimary.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Thông số cân mẫu bao lúa",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.GreenDark
                                )

                                val useColumnLayout = currentFontScale == FontScale.LARGE || currentFontScale == FontScale.XLARGE
                                if (useColumnLayout) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = sampleCountText,
                                            onValueChange = { v -> sampleCountText = v.filter { it.isDigit() } },
                                            label = { Text(stringResource(R.string.weigh_options_bag_sample_count)) },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AppColors.GreenPrimary,
                                                unfocusedBorderColor = AppColors.Divider
                                            )
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
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AppColors.GreenPrimary,
                                                unfocusedBorderColor = AppColors.Divider
                                            )
                                        )
                                    }
                                } else {
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
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AppColors.GreenPrimary,
                                                unfocusedBorderColor = AppColors.Divider
                                            )
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
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AppColors.GreenPrimary,
                                                unfocusedBorderColor = AppColors.Divider
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // === Card 3: Quy cách nhập cân ===
            Text(
                text = "QUY CÁCH NHẬP CÂN",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Segmented Control
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AppColors.SurfaceContainer,
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val options = listOf(
                                "SMALL" to stringResource(R.string.weigh_options_input_small),
                                "LARGE" to stringResource(R.string.weigh_options_input_large)
                            )
                            options.forEach { (mode, label) ->
                                val isSelected = inputMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) AppColors.GreenPrimary else Color.Transparent)
                                        .clickable { inputMode = mode },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else AppColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = stringResource(
                            if (inputMode == "LARGE") R.string.weigh_options_input_hint_large
                            else R.string.weigh_options_input_hint_small
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextHint
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val n = sampleCountText.toIntOrNull() ?: 0
                    val w = sampleWeightText.toDoubleOrNull() ?: 0.0
                    onSave(false, bagSampling, n, w, inputMode, ttsLocal, currentFontScale)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary)
            ) {
                Text(stringResource(R.string.weigh_options_done), color = AppColors.CardBg, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
