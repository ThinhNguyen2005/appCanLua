package com.GiaThinh.canlua.ui.component.weight

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.ui.component.AnimatedNumber
import com.GiaThinh.canlua.ui.component.ExplainingPopover
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.theme.lockedAwareTextFieldColors
import com.GiaThinh.canlua.util.MoneyFormatter
import kotlinx.coroutines.delay

private val CARD_SHAPE = RoundedCornerShape(16.dp)

/**
 * Card 2/3: Chỉ số cân — tổng KG, bì, tạp chất, độ ẩm, đơn giá, tính toán real-time.
 */
@Composable
fun WeightMetricsCard(
    totalWeight: Double,
    bagWeight: Double,
    impurityWeight: Double,
    moisturePercent: Double,
    netWeight: Double,
    pricePerKg: Double,
    totalAmount: Double,
    bagCount: Int,
    isLocked: Boolean,
    onBagWeightChange: (Double) -> Unit,
    onImpurityWeightChange: (Double) -> Unit,
    onMoistureChange: (Double) -> Unit,
    onPriceChange: (Double) -> Unit,
    impurityIsPercent: Boolean = false,
    bagMethodIsSampling: Boolean = false,
    bagSampleCount: Int = 0,
    bagSampleTotalWeight: Double = 0.0,
    onBagMethodChange: (isSampling: Boolean, sampleCount: Int, sampleTotalWeight: Double) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var impText by remember {
        mutableStateOf(if (impurityWeight > 0) impurityWeight.toString() else "")
    }
    var moistureText by remember {
        mutableStateOf(if (moisturePercent > 0) moisturePercent.toString() else "")
    }
    var priceText by remember {
        mutableStateOf(if (pricePerKg > 0) "%.0f".format(pricePerKg) else "")
    }

    val focusManager = LocalFocusManager.current

    // Đồng bộ text khi DB thay đổi từ bên ngoài
    LaunchedEffect(impurityWeight) {
        val cur = impText.toDoubleOrNull() ?: 0.0
        if (cur != impurityWeight) impText = if (impurityWeight > 0) impurityWeight.toString() else ""
    }
    LaunchedEffect(moisturePercent) {
        val cur = moistureText.toDoubleOrNull() ?: 0.0
        if (cur != moisturePercent) moistureText = if (moisturePercent > 0) moisturePercent.toString() else ""
    }
    LaunchedEffect(pricePerKg) {
        val cur = priceText.toDoubleOrNull() ?: 0.0
        if (cur != pricePerKg) priceText = if (pricePerKg > 0) "%.0f".format(pricePerKg) else ""
    }

    // Debounce write về DB sau 500ms khi user ngừng gõ
    LaunchedEffect(impText, impurityWeight) {
        val curNum = impText.toDoubleOrNull() ?: 0.0
        if (curNum != impurityWeight) { delay(500L); onImpurityWeightChange(curNum) }
    }
    LaunchedEffect(moistureText, moisturePercent) {
        val curNum = moistureText.toDoubleOrNull() ?: 0.0
        if (curNum != moisturePercent) { delay(500L); onMoistureChange(curNum) }
    }

    var showBagInfo by remember { mutableStateOf(false) }
    var showImpurityInfo by remember { mutableStateOf(false) }
    var showMoistureInfo by remember { mutableStateOf(false) }
    var showFormulaInfo by remember { mutableStateOf(false) }
    var showBagDialog by remember { mutableStateOf(false) }

    val containerColor = if (isLocked) AppColors.LockedSurface else AppColors.CardBg

    val totalBagWeight = remember(bagCount, bagWeight, bagMethodIsSampling, bagSampleCount, bagSampleTotalWeight) {
        com.GiaThinh.canlua.util.RiceCalculator.calcTotalBagWeight(
            bagCount = bagCount,
            bagWeight = bagWeight,
            methodIsSampling = bagMethodIsSampling,
            sampleCount = bagSampleCount,
            sampleTotalWeight = bagSampleTotalWeight
        )
    }

    val bagDisplayText = remember(bagMethodIsSampling, bagSampleCount, bagSampleTotalWeight) {
        val ratio = if (bagSampleCount > 0) bagSampleCount else 8
        if (bagMethodIsSampling) {
            val sampleWeightStr = "%.1f".format(java.util.Locale.US, bagSampleTotalWeight)
                .replace(".0", "")
                .replace(".", ",")
            "$ratio bao = $sampleWeightStr kg"
        } else {
            "$ratio bao = 1 kg"
        }
    }

    val totalBagText = remember(totalBagWeight) {
        "%.1f".format(java.util.Locale.US, totalBagWeight).replace(".", ",")
    }

    val rawAfterBag = remember(totalWeight, totalBagWeight) {
        (totalWeight - totalBagWeight).coerceAtLeast(0.0)
    }

    val impurityKg = impurityWeight.coerceAtLeast(0.0)

    val impurityPreviewText = remember(impurityKg) {
        val impurityText = "%.1f".format(java.util.Locale.US, impurityKg).replace(".", ",")
        "Đang trừ $impurityText kg tạp chất"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = CARD_SHAPE, clip = false)
            .clip(CARD_SHAPE)
            .background(containerColor)
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header: title + bao count chip ──────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Scale,
                    contentDescription = null,
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.weight_metrics_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = { showFormulaInfo = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.weight_metrics_formula_info_content),
                        tint = AppColors.GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Surface(color = AppColors.GreenSurface, shape = RoundedCornerShape(8.dp)) {
                    AnimatedNumber(
                        value = bagCount,
                        formatter = { count -> "$count bao" },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.GreenPrimary
                    )
                }
            }

            // ── Tổng KG thô (display lớn) ───────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.WeightSurface)
                    .border(1.dp, AppColors.DividerStrong, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.weight_label_total_weight),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    AnimatedNumber(
                        value = totalWeight,
                        formatter = { "${"%.1f".format(it)} kg" },
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.RemainingHighlight
                    )
                    Text(
                        stringResource(R.string.weight_label_before_tare),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextHint
                    )
                }
            }

            // ── Dòng 1: Bao bì (Trừ bì) ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLocked) { showBagDialog = true }
            ) {
                OutlinedTextField(
                    value = bagDisplayText,
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isLocked,
                    label = {
                        Text(
                            stringResource(R.string.weight_metrics_bag_label),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supportingText = { Text("Đang trừ $totalBagText kg bao bì") },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showBagInfo = true }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = stringResource(R.string.weight_metrics_bag_info_content),
                                    tint = AppColors.GreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            IconButton(onClick = { if (!isLocked) showBagDialog = true }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Thiết lập bao bì",
                                    tint = AppColors.GreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = lockedAwareTextFieldColors(),
                    interactionSource = remember { MutableInteractionSource() }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Transparent)
                        .clickable(enabled = !isLocked) { showBagDialog = true }
                )
            }

            // ── Dòng 2: Tạp chất ────────────────────────────────────
            OutlinedTextField(
                value = impText,
                onValueChange = { if (!isLocked) impText = it },
                label = {
                    Text(
                        stringResource(R.string.weight_metrics_impurity_label),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showImpurityInfo = true }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.weight_metrics_impurity_info_content),
                            tint = AppColors.GreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                enabled = !isLocked,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Next)
                        onImpurityWeightChange(impText.toDoubleOrNull() ?: 0.0)
                    }
                ),
                supportingText = { Text(impurityPreviewText) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = lockedAwareTextFieldColors()
            )

            // ── Dòng 3: Độ ẩm ───────────────────────────────────────
            OutlinedTextField(
                value = moistureText,
                onValueChange = { if (!isLocked) moistureText = it },
                label = { Text(stringResource(R.string.weight_metrics_moisture_label)) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.WaterDrop,
                        contentDescription = null,
                        tint = AppColors.Info,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showMoistureInfo = true }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.weight_metrics_moisture_info_content),
                            tint = AppColors.Info,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                enabled = !isLocked,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onMoistureChange(moistureText.toDoubleOrNull() ?: 0.0)
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = lockedAwareTextFieldColors()
            )

            // ── KL thực (sau khấu trừ) ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.GreenSurface)
                    .border(1.dp, AppColors.GreenPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.weight_label_net_weight),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    AnimatedNumber(
                        value = netWeight,
                        formatter = { "${"%.1f".format(it)} kg" },
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.GreenPrimary
                    )
                    Text(
                        stringResource(R.string.weight_metrics_net_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextSecondary
                    )
                }
            }

            // ── Thành tiền ──────────────────────────────────────────
            HorizontalDivider(color = AppColors.DividerStrong, thickness = 1.dp)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.weight_metrics_total_amount),
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                AnimatedNumber(
                    value = totalAmount,
                    formatter = { MoneyFormatter.formatVndShort(it) },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.GreenPrimary
                )
            }
            val context = LocalContext.current
            MoneyFormatter.toWords(totalAmount, context).takeIf { it.isNotEmpty() }?.let { words ->
                Text(
                    text = words,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // ── Popovers giải thích ─────────────────────────────────────────
    ExplainingPopover(
        visible = showBagInfo,
        title = stringResource(R.string.weight_metrics_bag_info_title),
        description = stringResource(R.string.weight_metrics_bag_info_description),
        onDismiss = { showBagInfo = false }
    )
    ExplainingPopover(
        visible = showImpurityInfo,
        title = stringResource(R.string.weight_metrics_impurity_info_title),
        description = stringResource(R.string.weight_metrics_impurity_info_description),
        onDismiss = { showImpurityInfo = false }
    )
    ExplainingPopover(
        visible = showMoistureInfo,
        title = stringResource(R.string.weight_metrics_moisture_info_title),
        description = stringResource(R.string.weight_metrics_moisture_info_description),
        onDismiss = { showMoistureInfo = false }
    )
    ExplainingPopover(
        visible = showFormulaInfo,
        title = stringResource(R.string.weight_metrics_formula_info_title),
        description = stringResource(R.string.weight_metrics_formula_info_description),
        onDismiss = { showFormulaInfo = false }
    )

    // ── Dialog thiết lập cách trừ bao bì ────────────────────────────
    if (showBagDialog) {
        var isSamplingLocal by remember { mutableStateOf(bagMethodIsSampling) }
        var sampleCountLocal by remember {
            mutableStateOf(if (bagSampleCount > 0) bagSampleCount.toString() else "8")
        }
        var sampleWeightLocal by remember {
            mutableStateOf(if (bagSampleTotalWeight > 0.0) bagSampleTotalWeight.toString() else "1.0")
        }
        val dialogPreview = remember(isSamplingLocal, sampleCountLocal, sampleWeightLocal, bagCount) {
            val count = sampleCountLocal.toIntOrNull()?.coerceAtLeast(1) ?: 8
            val unitKg = if (isSamplingLocal) {
                sampleWeightLocal.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 1.0
            } else {
                1.0
            }
            val total = (bagCount.toDouble() / count.toDouble()) * unitKg
            "Với $bagCount bao hiện tại → trừ ${"%.1f".format(java.util.Locale.US, total).replace(".", ",")} kg"
        }

        AlertDialog(
            onDismissRequest = { showBagDialog = false },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Thiết lập trừ bao bì",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "Chọn cách quy đổi số bao sang kg bì. Công thức áp dụng ngay cho phiếu này.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BagMethodOption(
                        selected = !isSamplingLocal,
                        title = "8 bao = 1 kg",
                        subtitle = "Tùy chỉnh số bao cho 1 kg bì",
                        formula = "Tổng bì = Tổng bao / Số bao × 1 kg",
                        onClick = { isSamplingLocal = false }
                    ) {
                        OutlinedTextField(
                            value = sampleCountLocal,
                            onValueChange = { value -> sampleCountLocal = value.filter { it.isDigit() } },
                            label = { Text("Số bao") },
                            suffix = { Text("bao = 1 kg") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = lockedAwareTextFieldColors()
                        )
                    }

                    BagMethodOption(
                        selected = isSamplingLocal,
                        title = "Tùy chỉnh bao",
                        subtitle = "Nhập X bao tương ứng Y kg bì",
                        formula = "Tổng bì = Tổng bao / X × Y kg",
                        onClick = { isSamplingLocal = true }
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = sampleCountLocal,
                                onValueChange = { value -> sampleCountLocal = value.filter { it.isDigit() } },
                                label = { Text("Số bao") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = lockedAwareTextFieldColors()
                            )
                            OutlinedTextField(
                                value = sampleWeightLocal,
                                onValueChange = { value ->
                                    sampleWeightLocal = value.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                                        .replace(',', '.')
                                },
                                label = { Text("Số kg") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = lockedAwareTextFieldColors()
                            )
                        }
                    }

                    Surface(
                        color = AppColors.GreenSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, AppColors.GreenPrimary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = dialogPreview,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.GreenPrimary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = sampleCountLocal.toIntOrNull()?.coerceAtLeast(1) ?: 8
                        val weight = if (isSamplingLocal) {
                            sampleWeightLocal.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 1.0
                        } else {
                            1.0
                        }
                        onBagMethodChange(isSamplingLocal, count, weight)
                        showBagDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary)
                ) {
                    Text("Áp dụng", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBagDialog = false }) {
                    Text("Hủy", color = AppColors.TextSecondary)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = AppColors.Surface
        )
    }
}

@Composable
private fun BagMethodOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    formula: String,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) AppColors.GreenSurface else AppColors.CardBg,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) AppColors.GreenPrimary else AppColors.DividerStrong
        ),
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = onClick)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.TextPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = subtitle,
                        color = AppColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Surface(
                color = AppColors.Surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AppColors.DividerStrong)
            ) {
                Text(
                    text = formula,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.GreenPrimary
                )
            }

            if (selected) {
                content()
            }
        }
    }
}
