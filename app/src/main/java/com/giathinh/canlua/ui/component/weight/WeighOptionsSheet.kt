package com.giathinh.canlua.ui.component.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giathinh.canlua.R
import com.giathinh.canlua.data.model.FontScale
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.util.HapticUtil
import kotlin.math.roundToInt

private const val DEFAULT_BAGS_PER_KG = 8
private val BAG_PRESET_COUNTS = listOf(7, 8, 9, 10)
private const val INPUT_MODE_SMALL = "SMALL"
private const val INPUT_MODE_LARGE = "LARGE"
private const val SLIDER_STEPS = 2
private const val SLIDER_MAX = 3f
private const val FONT_PREVIEW_BASE_SP = 14f

internal data class BagTareValidation(
    val isValid: Boolean,
    val sampleCount: Int,
    val sampleTotalWeight: Double,
    val countError: Boolean,
    val weightError: Boolean
)

/**
 * Normalizes localized decimal separator (`,` -> `.`) and keeps at most one dot.
 */
internal fun sanitizeDecimalInput(raw: String): String {
    val builder = StringBuilder(raw.length)
    var hasSeparator = false
    for (ch in raw) {
        when {
            ch.isDigit() -> builder.append(ch)
            (ch == '.' || ch == ',') && !hasSeparator -> {
                builder.append('.')
                hasSeparator = true
            }
        }
    }
    return builder.toString()
}

/**
 * Validates positive tare inputs before enabling save so bag deduction ratios cannot corrupt cards.
 */
internal fun validateBagTare(
    isSampling: Boolean,
    bagsPerKgText: String,
    sampleCountText: String,
    sampleWeightText: String
): BagTareValidation {
    return if (!isSampling) {
        val count = bagsPerKgText.toIntOrNull()
        val countError = count == null || count <= 0
        BagTareValidation(
            isValid = !countError,
            sampleCount = count ?: 0,
            sampleTotalWeight = 0.0,
            countError = countError,
            weightError = false
        )
    } else {
        val count = sampleCountText.toIntOrNull()
        val weight = sampleWeightText.toDoubleOrNull()
        val countError = count == null || count <= 0
        val weightError = weight == null || weight <= 0.0
        BagTareValidation(
            isValid = !countError && !weightError,
            sampleCount = count ?: 0,
            sampleTotalWeight = weight ?: 0.0,
            countError = countError,
            weightError = weightError
        )
    }
}

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
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val context = LocalContext.current

    var bagSampling by remember { mutableStateOf(bagMethodIsSampling) }
    var bagsPerKgText by remember {
        mutableStateOf(
            if (!bagMethodIsSampling && bagSampleCount > 0) {
                bagSampleCount.toString()
            } else {
                DEFAULT_BAGS_PER_KG.toString()
            }
        )
    }
    var sampleCountText by remember {
        mutableStateOf(if (bagMethodIsSampling && bagSampleCount > 0) bagSampleCount.toString() else "")
    }
    var sampleWeightText by remember {
        mutableStateOf(if (bagSampleTotalWeight > 0.0) bagSampleTotalWeight.toString() else "")
    }
    var inputMode by remember { mutableStateOf(weightInputMode.ifBlank { INPUT_MODE_SMALL }) }
    var impurityPercent by remember { mutableStateOf(impurityIsPercent) }
    var ttsLocal by remember { mutableStateOf(ttsEnabled) }

    val fontOptions = remember {
        listOf(FontScale.SMALL, FontScale.NORMAL, FontScale.LARGE, FontScale.XLARGE)
    }
    var currentFontScale by remember { mutableStateOf(fontScale) }
    val sliderState = rememberSliderState(
        value = fontOptions.indexOf(fontScale).coerceAtLeast(0).toFloat(),
        steps = SLIDER_STEPS,
        trackRange = 0f..SLIDER_MAX
    )

    val validation = remember(bagSampling, bagsPerKgText, sampleCountText, sampleWeightText) {
        validateBagTare(
            isSampling = bagSampling,
            bagsPerKgText = bagsPerKgText,
            sampleCountText = sampleCountText,
            sampleWeightText = sampleWeightText
        )
    }

    val isAtDefaults = !impurityPercent &&
        !bagSampling &&
        bagsPerKgText == DEFAULT_BAGS_PER_KG.toString() &&
        inputMode == INPUT_MODE_SMALL &&
        ttsLocal &&
        currentFontScale == FontScale.NORMAL

    val segmentedColors = SegmentedButtonDefaults.colors(
        activeContainerColor = AppColors.GreenPrimary,
        activeContentColor = AppColors.OnGreenPrimary,
        activeBorderColor = AppColors.GreenPrimary,
        inactiveContainerColor = AppColors.SurfaceContainer,
        inactiveContentColor = AppColors.TextSecondary,
        inactiveBorderColor = AppColors.Divider
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            WeighOptionsHeader(
                isAtDefaults = isAtDefaults,
                onReset = {
                    HapticUtil.tick(context)
                    impurityPercent = false
                    bagSampling = false
                    bagsPerKgText = DEFAULT_BAGS_PER_KG.toString()
                    sampleCountText = ""
                    sampleWeightText = ""
                    inputMode = INPUT_MODE_SMALL
                    ttsLocal = true
                    currentFontScale = FontScale.NORMAL
                    sliderState.value = fontOptions.indexOf(FontScale.NORMAL).coerceAtLeast(0).toFloat()
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WeighOptionsSectionLabel(text = stringResource(R.string.weigh_options_section_personalize))
                PersonalizeDisplayCard(
                    ttsEnabled = ttsLocal,
                    onTtsToggle = { enabled ->
                        HapticUtil.tick(context)
                        ttsLocal = enabled
                    },
                    currentFontScale = currentFontScale,
                    sliderState = sliderState,
                    onSliderValueChange = { newVal ->
                        sliderState.value = newVal
                        val idx = newVal.roundToInt().coerceIn(0, fontOptions.lastIndex)
                        val selected = fontOptions[idx]
                        if (selected != currentFontScale) {
                            HapticUtil.tick(context)
                            currentFontScale = selected
                        }
                    }
                )

                WeighOptionsSectionLabel(text = stringResource(R.string.weigh_options_section_bag_deduction))
                BagDeductionCard(
                    bagSampling = bagSampling,
                    bagsPerKgText = bagsPerKgText,
                    sampleCountText = sampleCountText,
                    sampleWeightText = sampleWeightText,
                    currentFontScale = currentFontScale,
                    validation = validation,
                    segmentedColors = segmentedColors,
                    onSelectBagMethod = { isSample ->
                        HapticUtil.tick(context)
                        bagSampling = isSample
                    },
                    onBagsPerKgChange = { raw -> bagsPerKgText = raw.filter { it.isDigit() } },
                    onPresetBagsSelect = { preset ->
                        HapticUtil.tick(context)
                        bagsPerKgText = preset.toString()
                    },
                    onSampleCountChange = { raw -> sampleCountText = raw.filter { it.isDigit() } },
                    onSampleWeightChange = { raw -> sampleWeightText = sanitizeDecimalInput(raw) }
                )

                WeighOptionsSectionLabel(text = stringResource(R.string.weigh_options_section_input_scale))
                WeightInputAndImpurityCard(
                    inputMode = inputMode,
                    impurityPercent = impurityPercent,
                    segmentedColors = segmentedColors,
                    onInputModeChange = { mode ->
                        HapticUtil.tick(context)
                        inputMode = mode
                    },
                    onImpurityUnitChange = { isPercent ->
                        HapticUtil.tick(context)
                        impurityPercent = isPercent
                    }
                )
            }

            WeighOptionsFooter(
                isSaveEnabled = validation.isValid,
                onSaveClick = {
                    onSave(
                        impurityPercent,
                        bagSampling,
                        validation.sampleCount,
                        validation.sampleTotalWeight,
                        inputMode,
                        ttsLocal,
                        currentFontScale
                    )
                }
            )
        }
    }
}

@Composable
private fun WeighOptionsHeader(
    isAtDefaults: Boolean,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.weigh_options_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(R.string.weigh_options_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
        TextButton(
            onClick = onReset,
            enabled = !isAtDefaults,
            modifier = Modifier.heightIn(min = 48.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = AppColors.GreenPrimary)
        ) {
            Text(
                text = stringResource(R.string.weigh_options_reset),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun WeighOptionsSectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = AppColors.TextSecondary,
        modifier = modifier
            .padding(start = 4.dp, top = 4.dp)
            .semantics { heading() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalizeDisplayCard(
    ttsEnabled: Boolean,
    onTtsToggle: (Boolean) -> Unit,
    currentFontScale: FontScale,
    sliderState: SliderState,
    onSliderValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val fontSizeTitle = stringResource(R.string.settings_font_size_title)
    val fontSizeLabel = stringResource(currentFontScale.labelRes)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(
                        value = ttsEnabled,
                        role = Role.Switch,
                        onValueChange = onTtsToggle
                    ),
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
                        tint = if (ttsEnabled) AppColors.GreenPrimary else AppColors.TextSecondary,
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
                    checked = ttsEnabled,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AppColors.OnGreenPrimary,
                        checkedTrackColor = AppColors.GreenPrimary,
                        uncheckedThumbColor = AppColors.TextHint,
                        uncheckedTrackColor = AppColors.SurfaceContainer
                    )
                )
            }

            HorizontalDivider(color = AppColors.Divider.copy(alpha = 0.3f), thickness = 0.5.dp)

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
                        text = fontSizeTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = fontSizeLabel,
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
                    Text(
                        text = "A",
                        fontSize = 12.sp,
                        color = AppColors.TextHint,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.clearAndSetSemantics {}
                    )
                    Slider(
                        state = sliderState,
                        onValueChange = onSliderValueChange,
                        colors = SliderDefaults.colors(
                            thumbColor = AppColors.GreenPrimary,
                            activeTrackColor = AppColors.GreenPrimary,
                            inactiveTrackColor = AppColors.SurfaceContainer,
                            activeTickColor = AppColors.GreenPrimary,
                            inactiveTickColor = AppColors.TextHint
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = fontSizeTitle
                                stateDescription = fontSizeLabel
                            }
                    )
                    Text(
                        text = "A",
                        fontSize = 22.sp,
                        color = AppColors.GreenPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clearAndSetSemantics {}
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AppColors.SurfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.weigh_options_font_preview),
                        fontSize = (FONT_PREVIEW_BASE_SP * currentFontScale.scale).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BagDeductionCard(
    bagSampling: Boolean,
    bagsPerKgText: String,
    sampleCountText: String,
    sampleWeightText: String,
    currentFontScale: FontScale,
    validation: BagTareValidation,
    segmentedColors: SegmentedButtonColors,
    onSelectBagMethod: (Boolean) -> Unit,
    onBagsPerKgChange: (String) -> Unit,
    onPresetBagsSelect: (Int) -> Unit,
    onSampleCountChange: (String) -> Unit,
    onSampleWeightChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bagOptions = listOf(
        false to stringResource(R.string.weigh_options_bag_single),
        true to stringResource(R.string.weigh_options_bag_sample)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                bagOptions.forEachIndexed { index, (isSample, label) ->
                    val isSelected = bagSampling == isSample
                    SegmentedButton(
                        selected = isSelected,
                        onClick = { onSelectBagMethod(isSample) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = bagOptions.size
                        ),
                        colors = segmentedColors,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = stringResource(
                    if (bagSampling) R.string.weigh_options_bag_hint_sample
                    else R.string.weigh_options_bag_hint_single
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )

            HorizontalDivider(color = AppColors.Divider.copy(alpha = 0.3f), thickness = 0.5.dp)

            if (!bagSampling) {
                SingleBagTareSection(
                    bagsPerKgText = bagsPerKgText,
                    isError = validation.countError,
                    onBagsPerKgChange = onBagsPerKgChange,
                    onPresetBagsSelect = onPresetBagsSelect
                )
            } else {
                SampleBagTareSection(
                    sampleCountText = sampleCountText,
                    sampleWeightText = sampleWeightText,
                    useColumnLayout = currentFontScale == FontScale.LARGE || currentFontScale == FontScale.XLARGE,
                    countError = validation.countError,
                    weightError = validation.weightError,
                    onSampleCountChange = onSampleCountChange,
                    onSampleWeightChange = onSampleWeightChange
                )
            }
        }
    }
}

@Composable
private fun SingleBagTareSection(
    bagsPerKgText: String,
    isError: Boolean,
    onBagsPerKgChange: (String) -> Unit,
    onPresetBagsSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.weigh_options_bag_single_params_title),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BAG_PRESET_COUNTS.forEach { presetCount ->
                val isSelected = bagsPerKgText == presetCount.toString()
                FilterChip(
                    selected = isSelected,
                    onClick = { onPresetBagsSelect(presetCount) },
                    label = {
                        Text(
                            text = stringResource(R.string.weigh_options_bag_preset_chip, presetCount),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.GreenPrimary,
                        selectedLabelColor = AppColors.OnGreenPrimary,
                        containerColor = AppColors.SurfaceContainer,
                        labelColor = AppColors.TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = AppColors.Divider,
                        selectedBorderColor = AppColors.GreenPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                )
            }
        }

        OutlinedTextField(
            value = bagsPerKgText,
            onValueChange = onBagsPerKgChange,
            label = { Text(stringResource(R.string.weigh_options_bags_per_kg_label)) },
            placeholder = { Text(stringResource(R.string.weigh_options_bags_per_kg_placeholder)) },
            isError = isError,
            supportingText = if (isError) {
                { Text(stringResource(R.string.weigh_options_bags_per_kg_error)) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.GreenPrimary,
                unfocusedBorderColor = AppColors.Divider
            )
        )
    }
}

@Composable
private fun SampleBagTareSection(
    sampleCountText: String,
    sampleWeightText: String,
    useColumnLayout: Boolean,
    countError: Boolean,
    weightError: Boolean,
    onSampleCountChange: (String) -> Unit,
    onSampleWeightChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AppColors.GreenPrimary,
        unfocusedBorderColor = AppColors.Divider
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.weigh_options_bag_sample_params_title),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )

        if (useColumnLayout) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = sampleCountText,
                    onValueChange = onSampleCountChange,
                    label = { Text(stringResource(R.string.weigh_options_bag_sample_count)) },
                    isError = countError,
                    supportingText = if (countError) {
                        { Text(stringResource(R.string.weigh_options_bag_sample_count_error)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = sampleWeightText,
                    onValueChange = onSampleWeightChange,
                    label = { Text(stringResource(R.string.weigh_options_bag_sample_weight)) },
                    isError = weightError,
                    supportingText = if (weightError) {
                        { Text(stringResource(R.string.weigh_options_bag_sample_weight_error)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = sampleCountText,
                    onValueChange = onSampleCountChange,
                    label = { Text(stringResource(R.string.weigh_options_bag_sample_count)) },
                    isError = countError,
                    supportingText = if (countError) {
                        { Text(stringResource(R.string.weigh_options_bag_sample_count_error)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = sampleWeightText,
                    onValueChange = onSampleWeightChange,
                    label = { Text(stringResource(R.string.weigh_options_bag_sample_weight)) },
                    isError = weightError,
                    supportingText = if (weightError) {
                        { Text(stringResource(R.string.weigh_options_bag_sample_weight_error)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = fieldColors
                )
            }
        }
    }
}

@Composable
private fun WeightInputAndImpurityCard(
    inputMode: String,
    impurityPercent: Boolean,
    segmentedColors: SegmentedButtonColors,
    onInputModeChange: (String) -> Unit,
    onImpurityUnitChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scaleOptions = listOf(
        INPUT_MODE_SMALL to stringResource(R.string.weigh_options_input_small),
        INPUT_MODE_LARGE to stringResource(R.string.weigh_options_input_large)
    )
    val impurityOptions = listOf(
        false to stringResource(R.string.unit_kg),
        true to stringResource(R.string.unit_percent)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                scaleOptions.forEachIndexed { index, (mode, label) ->
                    val isSelected = inputMode == mode
                    SegmentedButton(
                        selected = isSelected,
                        onClick = { onInputModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = scaleOptions.size
                        ),
                        colors = segmentedColors,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = stringResource(
                    if (inputMode == INPUT_MODE_LARGE) {
                        R.string.weigh_options_input_hint_large
                    } else {
                        R.string.weigh_options_input_hint_small
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )

            HorizontalDivider(color = AppColors.Divider.copy(alpha = 0.3f), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.FilterAlt,
                    contentDescription = null,
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.weight_label_impurity),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                SingleChoiceSegmentedButtonRow {
                    impurityOptions.forEachIndexed { index, (isPercent, label) ->
                        SegmentedButton(
                            selected = impurityPercent == isPercent,
                            onClick = { onImpurityUnitChange(isPercent) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = impurityOptions.size
                            ),
                            colors = segmentedColors,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(
                    if (impurityPercent) {
                        R.string.weigh_options_impurity_hint_percent
                    } else {
                        R.string.weigh_options_impurity_hint_kg
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun WeighOptionsFooter(
    isSaveEnabled: Boolean,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = AppColors.Divider.copy(alpha = 0.4f), thickness = 0.5.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onSaveClick,
                enabled = isSaveEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.GreenPrimary,
                    contentColor = AppColors.OnGreenPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.weigh_options_done),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
