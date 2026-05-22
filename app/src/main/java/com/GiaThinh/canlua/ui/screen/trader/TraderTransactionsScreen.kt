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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.viewmodel.TraderTransactionItem
import com.GiaThinh.canlua.ui.viewmodel.TraderTransactionsViewModel
import com.GiaThinh.canlua.ui.viewmodel.TransactionFilter
import com.GiaThinh.canlua.util.TrackScreenRender
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sổ giao dịch — TRADER xem tất cả các thẻ đã verify QR, kèm tổng tài chính.
 *
 * Data flow: Firestore `cards` (lockedByTraderId == uid)
 *   → flatMapLatest sang `transactions` (cardId in [...])
 *   → combine + compute stats trong ViewModel.
 */
@Composable
fun TraderTransactionsScreen(
    navController: androidx.navigation.NavController,
    viewModel: TraderTransactionsViewModel = hiltViewModel()
) {
    TrackScreenRender("trader_transactions")
    val state by viewModel.uiState.collectAsState()
    val filter by viewModel.filter.collectAsState()

    Scaffold(containerColor = AppColors.Surface) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> LoadingState()
                state.totalCards == 0 -> EmptyTraderTransactions()
                else -> TransactionContent(
                    state = state,
                    filter = filter,
                    onFilterChange = viewModel::setFilter
                )
            }
        }
    }
}

// ────────────────────── Content ──────────────────────

@Composable
private fun TransactionContent(
    state: com.GiaThinh.canlua.ui.viewmodel.TraderTransactionsUiState,
    filter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderTitle(state.totalCards) }
        item { StatsHeroCard(state) }
        item {
            FilterRow(
                selected = filter,
                onSelect = onFilterChange
            )
        }

        if (state.items.isEmpty()) {
            item { FilteredEmptyState() }
        } else {
            items(state.items, key = { it.card.id }) { item ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    TransactionRow(item = item)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun HeaderTitle(totalCards: Int) {
    Column {
        Text(
            text = "Sổ giao dịch",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        Text(
            text = "$totalCards thẻ đã thu mua · cập nhật realtime",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextHint
        )
    }
}

// ────────────────────── Stats Hero ──────────────────────

@Composable
private fun StatsHeroCard(state: com.GiaThinh.canlua.ui.viewmodel.TraderTransactionsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.GreenPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            AppColors.GreenPrimary,
                            AppColors.GreenDark
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "Tổng giá trị thu mua",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatCurrency(state.totalAmount),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))

                // Progress bar đã thanh toán
                val progress = if (state.totalAmount > 0)
                    (state.totalPaid / state.totalAmount).toFloat().coerceIn(0f, 1f)
                else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Đã thanh toán: ${formatCurrency(state.totalPaid)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatChip(
                        icon = Icons.Filled.Scale,
                        label = "Khối lượng",
                        value = "${formatNumber(state.totalNetWeight)} kg",
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        icon = Icons.Filled.WarningAmber,
                        label = "Còn nợ",
                        value = "${state.unpaidCount} thẻ",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ────────────────────── Filter ──────────────────────

@Composable
private fun FilterRow(
    selected: TransactionFilter,
    onSelect: (TransactionFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TransactionFilter.values().forEach { f ->
            val isSelected = f == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) AppColors.GreenPrimary
                        else AppColors.SurfaceContainer
                    )
                    .clickable { onSelect(f) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    f.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else AppColors.TextSecondary
                )
            }
        }
    }
}

// ────────────────────── Row Item ──────────────────────

@Composable
private fun TransactionRow(item: TraderTransactionItem) {
    val card = item.card
    val statusColor = if (item.isFullyPaid) AppColors.Success else AppColors.Warning
    val statusBg = if (item.isFullyPaid) AppColors.GreenSurface else AppColors.GoldLight
    val statusIcon = if (item.isFullyPaid) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber
    val statusLabel = if (item.isFullyPaid) "Đã thanh toán" else "Còn nợ"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top: name + status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AppColors.GreenSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = AppColors.GreenPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        card.name.ifBlank { "Không tên" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatDate(card.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextHint
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Detail metrics — 3 columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBox(
                    icon = Icons.Filled.Scale,
                    label = "Khối lượng",
                    value = "${formatNumber(card.netWeight)} kg",
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    icon = Icons.Filled.AccountBalanceWallet,
                    label = "Tổng",
                    value = formatCurrencyShort(card.totalAmount),
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    icon = Icons.Filled.ReceiptLong,
                    label = "Còn lại",
                    value = formatCurrencyShort(item.remaining),
                    valueColor = if (item.isFullyPaid) AppColors.Success else AppColors.Error,
                    modifier = Modifier.weight(1f)
                )
            }

            if (card.riceVariety.isNotBlank() || card.bagCount > 0) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (card.riceVariety.isNotBlank()) {
                        TagChip(label = card.riceVariety, color = AppColors.Info)
                    }
                    if (card.bagCount > 0) {
                        TagChip(label = "${card.bagCount} bao", color = AppColors.GoldDark)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = AppColors.TextPrimary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceContainer)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = AppColors.TextHint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextHint)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TagChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

// ────────────────────── States ──────────────────────

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AppColors.GreenPrimary)
    }
}

@Composable
private fun EmptyTraderTransactions() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(AppColors.GoldLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = AppColors.GoldAccent,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Chưa có giao dịch nào",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Quét QR thẻ của nông dân để bắt đầu thu mua. Tất cả giao dịch sẽ được ghi nhận tự động tại đây.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FilteredEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.ReceiptLong,
                contentDescription = null,
                tint = AppColors.TextHint,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Không có giao dịch trong bộ lọc này",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }
    }
}

// ────────────────────── Formatters ──────────────────────

private val currencyFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))

private fun formatCurrency(value: Double): String =
    "${currencyFormat.format(value.toLong())}đ"

private fun formatCurrencyShort(value: Double): String {
    return when {
        value >= 1_000_000_000 -> "${"%.1f".format(value / 1_000_000_000)}tỷ"
        value >= 1_000_000 -> "${"%.1f".format(value / 1_000_000)}tr"
        value >= 1_000 -> "${"%.0f".format(value / 1_000)}k"
        else -> currencyFormat.format(value.toLong())
    }
}

private fun formatNumber(value: Double): String {
    return if (value % 1.0 == 0.0) currencyFormat.format(value.toLong())
    else "%.2f".format(value)
}

private fun formatDate(date: Date): String = dateFormat.format(date)
