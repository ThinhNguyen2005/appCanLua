package com.giathinh.canlua.ui.screen.trader

import com.giathinh.canlua.R
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.giathinh.canlua.ui.component.DefaultSkeleton
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.viewmodel.TraderTransactionItem
import com.giathinh.canlua.ui.viewmodel.TraderTransactionsUiState
import com.giathinh.canlua.ui.viewmodel.TraderTransactionsViewModel
import com.giathinh.canlua.ui.viewmodel.TransactionFilter
import com.giathinh.canlua.util.HapticUtil
import com.giathinh.canlua.util.TrackScreenRender
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TraderTransactionsScreen(
    navController: NavController
) {
    TrackScreenRender("trader_transactions")
    val viewModel: TraderTransactionsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showSkeleton = state.isLoading && state.items.isEmpty()

    if (showSkeleton) {
        DefaultSkeleton()
    } else {
        TraderTransactionsScreenContent(
            navController = navController,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraderTransactionsScreenContent(
    navController: NavController,
    viewModel: TraderTransactionsViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.trader_tx_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticUtil.tick(context)
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_back),
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Surface,
                    titleContentColor = AppColors.TextPrimary
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        containerColor = AppColors.Surface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> LoadingState()
                state.totalCards == 0 -> EmptyTraderTransactions()
                else -> TransactionContent(
                    state = state,
                    filter = filter,
                    onFilterChange = viewModel::setFilter,
                    onCardClick = { cardId ->
                        HapticUtil.tick(context)
                        navController.navigate("card_detail/$cardId")
                    }
                )
            }
        }
    }
}

// ────────────────────── Content ──────────────────────

@Composable
private fun TransactionContent(
    state: TraderTransactionsUiState,
    filter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    onCardClick: (String) -> Unit
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
                    TransactionRow(
                        item = item,
                        onClick = { onCardClick(item.card.id) }
                    )
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
            text = stringResource(R.string.trader_tx_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        Text(
            text = stringResource(R.string.trader_tx_subtitle, totalCards),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextHint
        )
    }
}

// ────────────────────── Stats Hero ──────────────────────

@Composable
private fun StatsHeroCard(state: TraderTransactionsUiState) {
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
                    text = stringResource(R.string.trader_tx_total_amount),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatCurrency(state.totalAmount),
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
                        text = stringResource(R.string.trader_tx_paid, formatCurrency(state.totalPaid)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
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
                        label = stringResource(R.string.trader_tx_net_weight),
                        value = "${formatNumber(state.totalNetWeight)} kg",
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        icon = Icons.Filled.WarningAmber,
                        label = stringResource(R.string.trader_tx_debt),
                        value = stringResource(R.string.trader_tx_cards_count, state.unpaidCount),
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
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TransactionFilter.entries.forEach { f ->
            val isSelected = f == selected
            val labelText = when (f) {
                TransactionFilter.ALL -> stringResource(R.string.trader_tx_filter_all)
                TransactionFilter.THIS_WEEK -> stringResource(R.string.trader_tx_filter_this_week)
                TransactionFilter.THIS_MONTH -> stringResource(R.string.trader_tx_filter_this_month)
                TransactionFilter.UNPAID -> stringResource(R.string.trader_tx_filter_unpaid)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) AppColors.GreenPrimary
                        else AppColors.SurfaceContainer
                    )
                    .clickable {
                        HapticUtil.tick(context)
                        onSelect(f)
                    }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else AppColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ────────────────────── Row Item ──────────────────────

@Composable
private fun TransactionRow(
    item: TraderTransactionItem,
    onClick: () -> Unit
) {
    val card = item.card
    val statusColor = if (item.isFullyPaid) AppColors.Success else AppColors.Warning
    val statusBg = if (item.isFullyPaid) AppColors.GreenSurface else AppColors.GoldLight
    val statusIcon = if (item.isFullyPaid) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber
    val statusLabel = if (item.isFullyPaid) {
        stringResource(R.string.trader_tx_paid_status)
    } else {
        stringResource(R.string.trader_tx_unpaid_status)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
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
                        text = card.name.ifBlank { stringResource(R.string.trader_tx_unnamed) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatDate(card.date),
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
                            text = statusLabel,
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
                    label = stringResource(R.string.trader_tx_net_weight),
                    value = "${formatNumber(card.netWeight)} kg",
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    icon = Icons.Filled.AccountBalanceWallet,
                    label = stringResource(R.string.trader_tx_total),
                    value = formatCurrencyShort(card.totalAmount),
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    label = stringResource(R.string.trader_tx_remaining),
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
                        TagChip(
                            label = stringResource(R.string.trader_tx_bags_count, card.bagCount),
                            color = AppColors.GoldDark
                        )
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
                    Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = AppColors.GoldAccent,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.trader_tx_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.trader_tx_empty_desc),
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
                Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint = AppColors.TextHint,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.trader_tx_filtered_empty),
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
