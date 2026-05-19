package com.GiaThinh.canlua.ui.screen.trader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.GiaThinh.canlua.data.firestore.FirestoreRicePrice
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.util.isScrollingUp
import com.GiaThinh.canlua.ui.viewmodel.TraderBidsViewModel
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Màn hình TRADER — quản lý các bid (giá đăng) của bản thân.
 * Tương đương "Rao mua" trong nav.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraderBidsScreen(
    viewModel: TraderBidsViewModel = hiltViewModel()
) {
    val bids by viewModel.myBids.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var showEditor by remember { mutableStateOf(false) }
    var editingBid by remember { mutableStateOf<FirestoreRicePrice?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val fabVisible = listState.isScrollingUp() || bids.isEmpty()

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
            showEditor = false
            editingBid = null
        }
        uiState.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingBid = null
                        showEditor = true
                    },
                    containerColor = AppColors.GreenPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Đăng giá mới", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = AppColors.Surface
    ) { padding ->
        if (bids.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Header(count = bids.size) }
                items(bids, key = { it.id }) { bid ->
                    BidCard(
                        bid = bid,
                        onEdit = {
                            editingBid = bid
                            showEditor = true
                        },
                        onDelete = { viewModel.deleteBid(bid.id) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showEditor) {
        ModalBottomSheet(
            onDismissRequest = { showEditor = false; editingBid = null },
            sheetState = sheetState,
            containerColor = AppColors.Surface
        ) {
            BidEditorForm(
                editingBid = editingBid,
                isSaving = uiState.isSaving,
                onSubmit = { variety, min, max, region, trend, note ->
                    viewModel.submitBid(
                        variety = variety,
                        priceMin = min,
                        priceMax = max,
                        region = region,
                        trend = trend,
                        note = note,
                        existingId = editingBid?.id
                    )
                }
            )
        }
    }
}

@Composable
private fun Header(count: Int) {
    Column {
        Text(
            text = "Giá lúa của bạn",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        Text(
            text = "$count bảng giá đang hoạt động · cập nhật realtime",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextHint
        )
    }
}

@Composable
private fun BidCard(
    bid: FirestoreRicePrice,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (trendColor, trendIcon) = when (bid.trend) {
        "UP" -> AppColors.Success to Icons.AutoMirrored.Filled.TrendingUp
        "DOWN" -> AppColors.Error to Icons.AutoMirrored.Filled.TrendingDown
        else -> AppColors.Info to Icons.AutoMirrored.Filled.TrendingFlat
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(trendColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bid.variety,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = formatRelative(bid.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextHint
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Sửa", tint = AppColors.GreenPrimary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Xoá", tint = AppColors.Error)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PriceBox("Min", bid.priceMin, AppColors.TextSecondary, Modifier.weight(1f))
                PriceBox("Max", bid.priceMax, AppColors.GreenDark, Modifier.weight(1f))
                PriceBox("Khu vực", null, AppColors.TextPrimary, Modifier.weight(1f), bid.region.ifEmpty { "—" })
            }
            if (bid.note.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Ghi chú: ${bid.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint
                )
            }
        }
    }
}

@Composable
private fun PriceBox(
    label: String,
    price: Double?,
    color: Color,
    modifier: Modifier = Modifier,
    textValue: String? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceContainer)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextHint)
        Spacer(Modifier.height(4.dp))
        Text(
            text = textValue ?: formatPrice(price ?: 0.0),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(AppColors.GoldLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Sell,
                    contentDescription = null,
                    tint = AppColors.GoldAccent,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Chưa có bảng giá nào",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Đăng bảng giá thu mua đầu tiên để nông dân tìm thấy bạn trên thị trường.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun BidEditorForm(
    editingBid: FirestoreRicePrice?,
    isSaving: Boolean,
    onSubmit: (variety: String, min: Double, max: Double, region: String, trend: String, note: String) -> Unit
) {
    var variety by remember { mutableStateOf(editingBid?.variety ?: "") }
    var minStr by remember { mutableStateOf(editingBid?.priceMin?.toLong()?.toString() ?: "") }
    var maxStr by remember { mutableStateOf(editingBid?.priceMax?.toLong()?.toString() ?: "") }
    var region by remember { mutableStateOf(editingBid?.region ?: "") }
    var trend by remember { mutableStateOf(editingBid?.trend ?: "STABLE") }
    var note by remember { mutableStateOf(editingBid?.note ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (editingBid == null) "Đăng giá mới" else "Cập nhật giá",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        OutlinedTextField(
            value = variety,
            onValueChange = { variety = it },
            label = { Text("Giống lúa (vd: ST25, OM18)") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = minStr,
                onValueChange = { minStr = it.filter { c -> c.isDigit() } },
                label = { Text("Giá min (đ/kg)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = textFieldColors(),
                singleLine = true
            )
            OutlinedTextField(
                value = maxStr,
                onValueChange = { maxStr = it.filter { c -> c.isDigit() } },
                label = { Text("Giá max (đ/kg)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = textFieldColors(),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = region,
            onValueChange = { region = it },
            label = { Text("Khu vực thu mua (vd: Cần Thơ)") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors(),
            singleLine = true
        )

        // Trend selector
        Column {
            Text("Xu hướng giá", style = MaterialTheme.typography.labelMedium, color = AppColors.TextHint)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("UP", "Tăng", AppColors.Success),
                    Triple("STABLE", "Ổn định", AppColors.Info),
                    Triple("DOWN", "Giảm", AppColors.Error)
                ).forEach { (key, label, color) ->
                    val selected = trend == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) color.copy(alpha = 0.18f) else AppColors.SurfaceContainer)
                            .clickable { trend = key }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) color else AppColors.TextSecondary
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Ghi chú thêm (không bắt buộc)") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors(),
            maxLines = 3
        )

        Button(
            onClick = {
                val min = minStr.toDoubleOrNull() ?: 0.0
                val max = maxStr.toDoubleOrNull() ?: 0.0
                if (variety.isNotBlank() && min > 0 && max >= min) {
                    onSubmit(variety, min, max, region, trend, note)
                }
            },
            enabled = !isSaving && variety.isNotBlank() && minStr.isNotBlank() && maxStr.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                if (isSaving) "Đang lưu..." else (if (editingBid == null) "Đăng giá" else "Cập nhật"),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AppColors.TextPrimary,
    unfocusedTextColor = AppColors.TextPrimary,
    focusedBorderColor = AppColors.GreenPrimary,
    unfocusedBorderColor = AppColors.Divider,
    focusedLabelColor = AppColors.GreenPrimary,
    unfocusedLabelColor = AppColors.TextHint,
    cursorColor = AppColors.GreenPrimary
)

private fun formatPrice(price: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
    return nf.format(price.toLong())
}

private fun formatRelative(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        mins < 1 -> "Vừa cập nhật"
        mins < 60 -> "$mins phút trước"
        hours < 24 -> "$hours giờ trước"
        days < 7 -> "$days ngày trước"
        else -> "Đã lâu"
    }
}
