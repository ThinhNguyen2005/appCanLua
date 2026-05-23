package com.GiaThinh.canlua.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.GiaThinh.canlua.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.WeightEntry
import com.GiaThinh.canlua.ui.component.CustomHeader
import com.GiaThinh.canlua.ui.component.RiceVarietyDropdown
import com.GiaThinh.canlua.ui.component.ThousandSeparatorTransformation
import com.GiaThinh.canlua.ui.component.detail.BagEntriesCard
import com.GiaThinh.canlua.ui.component.detail.BagEntryActionSheet
import com.GiaThinh.canlua.ui.component.detail.CardInfoCard
import com.GiaThinh.canlua.ui.component.detail.DetailSkeleton
import com.GiaThinh.canlua.ui.component.detail.FinancialSummaryCard
import com.GiaThinh.canlua.ui.component.detail.RemainingHeroCard
import com.GiaThinh.canlua.ui.component.detail.WeightSummaryCard
import com.GiaThinh.canlua.ui.component.rememberHeaderHeights
import com.GiaThinh.canlua.ui.feedback.LocalAppToast
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.util.isScrollingUp
import com.GiaThinh.canlua.ui.viewmodel.CardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: Long,
    navController: NavController,
    viewModel: CardViewModel = hiltViewModel()
) {
    com.GiaThinh.canlua.util.TrackScreenRender("card_detail")
    val currentCard by viewModel.currentCard.collectAsStateWithLifecycle()
    val weightEntries by viewModel.weightEntries.collectAsStateWithLifecycle()

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }

    LaunchedEffect(cardId) { viewModel.loadCardById(cardId) }

    val heights = rememberHeaderHeights()
    val scrollState = rememberLazyListState()
    val density = LocalDensity.current


    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val appToast = LocalAppToast.current
    val scope = rememberCoroutineScope()

    // Pull-to-refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(550) // perceptible feedback even on cache hit
            viewModel.loadCardById(cardId)
            isRefreshing = false
        }
    }

    // Long-press → bottom sheet on bag cell
    var entryActionTarget by remember { mutableStateOf<WeightEntry?>(null) }
    var entryActionGlobalIndex by remember { mutableStateOf(0) }

    // Cache pixel range thay vì tính `with(density){ … }.toPx()` mỗi recompose.
    // Header height + collapseFraction là 2 hot path scroll — phải tránh
    // recompute trong composition phase.
    val collapseRangePx = remember(density, heights.expanded) {
        with(density) { heights.expanded.toPx() }
    }
    val collapseFraction by remember(collapseRangePx) {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset < 50) {
                0f
            } else {
                val scroll = if (scrollState.firstVisibleItemIndex == 0) {
                    scrollState.firstVisibleItemScrollOffset.toFloat()
                } else {
                    collapseRangePx
                }
                (scroll / collapseRangePx).coerceIn(0f, 1f)
            }
        }
    }

    // collapseFraction đã coerce trong derivedStateOf — không cần coerce lại.
    val headerHeight = lerp(heights.expanded, heights.collapsed, collapseFraction)

    var showOverflow by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showUnlockConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppColors.Surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            // Drive-style Extended FAB — chỉ hiện khi card đã load và user đang ở đầu trang
            currentCard?.let { c ->
                val fabVisible = scrollState.isScrollingUp() && entryActionTarget == null
                AnimatedVisibility(
                    visible = fabVisible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (c.isLocked) {
                                appToast.warning(context.getString(R.string.card_detail_unlock_card_first))
                            } else {
                                navController.navigate("weight_input/${cardId}")
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Outlined.Scale,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        text = {
                            Text(
                                stringResource(R.string.card_detail_weigh_action),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        },
                        expanded = true,
                        containerColor = if (c.isLocked) AppColors.GreenPrimary.copy(alpha = 0.45f) else AppColors.GreenPrimary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 12.dp
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        // === PLACEHOLDER CARD (Performance Optimization) ===
        // Khi currentCard == null (data đang load), header vẫn render với card rỗng.
        // → Frame 0 đã có header đầy đủ (back button, FAB, layout), không có cảm giác
        //   "header appear sau content". Khi data về, chỉ Text bên trong recompose
        //   (cheap), không mount lại view tree.
        val placeholderCard = remember {
            Card(
                id = 0L,
                name = "",
                date = Date(),
                traderName = ""
            )
        }
        val displayCard = currentCard ?: placeholderCard
        val isLoading = currentCard == null

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // === Content layer ===
            if (isLoading) {
                Column {
                    Spacer(Modifier.height(heights.expanded + 16.dp))
                    DetailSkeleton()
                }
            } else {
                val card = currentCard!!
                val tables = remember(weightEntries) { weightEntries.chunked(25) }
                val pagerState = rememberPagerState(
                    initialPage = 0,
                    pageCount = { tables.size.coerceAtLeast(1) }
                )
                val activeTableIndex = pagerState.currentPage.coerceIn(0, (tables.size - 1).coerceAtLeast(0))

                val lastEntryTime = weightEntries.maxOfOrNull { it.timestamp }

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isRefreshing = true
                    },
                    state = pullState,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullState,
                            isRefreshing = isRefreshing,
                            color = AppColors.GreenPrimary,
                            containerColor = AppColors.GreenSurface,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = heights.collapsed + 8.dp)
                        )
                    }
                ) {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(heights.expanded + 16.dp)) }

                        // Card 0: Thông tin phiếu (thương lái · SDT · giống lúa · ngày · địa chỉ ruộng)
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                val createdLabel = remember(card.date) {
                                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(card.date)
                                }
                                CardInfoCard(
                                    traderName = card.traderName,
                                    traderPhone = card.traderPhone,
                                    riceVariety = card.riceVariety,
                                    seasonLabel = card.seasonLabel,
                                    createdDateLabel = createdLabel,
                                    fieldAddress = card.fieldAddress,
                                    hasCoordinates = card.latitude != null && card.longitude != null,
                                    onCallTrader = {
                                        val phone = card.traderPhone.trim()
                                        if (phone.isBlank()) return@CardInfoCard
                                        runCatching {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }.onFailure {
                                            appToast.error(context.getString(R.string.card_detail_call_app_error))
                                        }
                                    },
                                    onOpenMap = {
                                        val lat = card.latitude
                                        val lon = card.longitude
                                        if (lat == null || lon == null) {
                                            appToast.error(context.getString(R.string.card_detail_missing_gps))
                                            return@CardInfoCard
                                        }
                                        val label = card.fieldAddress.ifBlank { card.name.ifBlank { context.getString(R.string.card_detail_default_field_label) } }
                                        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(label)})")
                                        runCatching {
                                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }.onFailure {
                                            appToast.error(context.getString(R.string.card_detail_map_app_error))
                                        }
                                    },
                                    onRefreshLocation = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.refreshFieldLocation(cardId)
                                        appToast.info(context.getString(R.string.card_detail_updating_location))
                                    },
                                    isLocked = card.isLocked,
                                    cccd = card.cccd
                                )
                            }
                        }

                        // Card 1: Khối lượng
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                WeightSummaryCard(
                                    totalWeight = card.totalWeight,
                                    bagCount = card.bagCount,
                                    bagWeight = card.bagWeight,
                                    impurityWeight = card.impurityWeight,
                                    moisturePercent = card.moisturePercent,
                                    netWeight = card.netWeight,
                                    numberFormat = numberFormat,
                                    isLocked = card.isLocked
                                )
                            }
                        }

                        // Card 2: Tài chính (gộp "Còn lại" vào cuối với style highlight vàng)
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                FinancialSummaryCard(
                                    pricePerKg = card.pricePerKg,
                                    totalAmount = card.totalAmount,
                                    depositAmount = card.depositAmount,
                                    paidAmount = card.paidAmount,
                                    remainingAmount = card.remainingAmount,
                                    isPaid = card.isPaid,
                                    onPaidChange = { viewModel.updateCard(card.copy(isPaid = it)) },
                                    isLocked = card.isLocked,
                                    numberFormat = numberFormat
                                )
                            }
                        }

                        // (Card "Còn lại" đã gộp vào FinancialSummaryCard ở trên)

                        // Card 4: Chi tiết bao cân
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                BagEntriesCard(
                                    bagCountTotal = weightEntries.size,
                                    tables = tables,
                                    pagerState = pagerState,
                                    activeTableIndex = activeTableIndex,
                                    isLocked = card.isLocked,
                                    onTableSelected = { index ->
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        scope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    onAddFirstBag = {
                                        if (card.isLocked) {
                                            appToast.warning(context.getString(R.string.card_detail_unlock_table_first))
                                        } else {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            navController.navigate("weight_input/${cardId}")
                                        }
                                    },
                                    onEntryLongPress = { entry, globalIdx ->
                                        if (card.isLocked) {
                                            appToast.warning(context.getString(R.string.card_detail_card_locked))
                                        } else {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            entryActionGlobalIndex = globalIdx
                                            entryActionTarget = entry
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text(stringResource(R.string.card_detail_delete_title)) },
                        text = { Text(stringResource(R.string.card_detail_delete_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showDeleteConfirm = false
                                scope.launch {
                                    viewModel.deleteCard(card)
                                    navController.popBackStack()
                                }
                            }) {
                                Text(stringResource(R.string.card_detail_delete_confirm), color = AppColors.Error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
                        }
                    )
                }

                if (showEditDialog) {
                    EditCardDialog(
                        card = card,
                        onDismiss = { showEditDialog = false },
                        onConfirm = { updatedCard ->
                            viewModel.updateCard(updatedCard)
                            showEditDialog = false
                            appToast.success(context.getString(R.string.card_detail_update_success))
                        }
                    )
                }

                // Bag entry action sheet (long-press)
                entryActionTarget?.let { entry ->
                    BagEntryActionSheet(
                        entry = entry,
                        globalIndex = entryActionGlobalIndex,
                        numberFormat = numberFormat,
                        onDismiss = { entryActionTarget = null },
                        onDelete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.deleteWeightEntry(entry)
                            entryActionTarget = null
                            appToast.success(context.getString(R.string.card_detail_deleted_bag, entryActionGlobalIndex))
                        }
                    )
                }
            }

            // === HEADER LAYER (luôn render từ frame 0) ===
            // Đặt OUT of if/else → header tồn tại NGAY khi composable mount,
            // không có cảm giác "appear sau content". Khi data từ null → loaded,
            // chỉ Text bên trong recompose (rẻ), view tree không tear down/rebuild.
            CustomHeader(
                card = displayCard,
                collapseFraction = collapseFraction,
                lastEntryTime = if (isLoading) null else weightEntries.maxOfOrNull { it.timestamp },
                modifier = Modifier.height(headerHeight),
                onBack = { navController.popBackStack() },
                onAdd = {
                    if (isLoading) return@CustomHeader
                    if (displayCard.isLocked) {
                        appToast.warning(context.getString(R.string.card_detail_unlock_table_first))
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate("weight_input/${cardId}")
                    }
                },
                showOverflow = showOverflow,
                onOverflowChange = { if (!isLoading) showOverflow = it },
                onEditCard = {
                    if (isLoading) return@CustomHeader
                    if (displayCard.isLocked) {
                        appToast.warning(context.getString(R.string.card_detail_unlock_table_first))
                    } else {
                        showEditDialog = true
                    }
                },
                onDeleteCard = {
                    if (isLoading) return@CustomHeader
                    if (displayCard.isLocked) {
                        appToast.warning(context.getString(R.string.card_detail_unlock_table_first))
                    } else if (!displayCard.isPaid) {
                        appToast.warning(context.getString(R.string.card_detail_delete_blocked_unpaid))
                    } else {
                        showDeleteConfirm = true
                    }
                },
                onCreateQr = { if (!isLoading) navController.navigate("qr_generate/${displayCard.id}") },
                onScanQr = { if (!isLoading) navController.navigate("qr_scan") },
                onExportPdf = {
                    if (isLoading) return@CustomHeader
                    if (!com.GiaThinh.canlua.util.PremiumState.isPremium.value) {
                        appToast.warning(context.getString(R.string.card_detail_pdf_premium_required))
                        navController.navigate("premium")
                        return@CustomHeader
                    }
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching {
                            val file = com.GiaThinh.canlua.util.PdfExporter.export(
                                context = context,
                                card = displayCard,
                                entries = weightEntries
                            )
                            val uri = com.GiaThinh.canlua.util.PdfExporter.shareUri(context, file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.card_detail_pdf_subject, displayCard.id))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(intent, context.getString(R.string.card_detail_pdf_share_title))
                            )
                        }.onFailure {
                            com.GiaThinh.canlua.util.AnalyticsHelper.logNonFatal(it, tag = "pdf_export")
                            appToast.error(
                                context.getString(
                                    R.string.card_detail_pdf_error,
                                    it.message ?: context.getString(R.string.card_detail_unknown_error)
                                )
                            )
                        }
                    }
                },
                onToggleLock = {
                    if (isLoading) return@CustomHeader
                    if (displayCard.isLocked) {
                        // Mở khóa phiếu đã chốt → cần xác nhận để tránh nhấn nhầm.
                        showUnlockConfirm = true
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleCardLock(displayCard.id)
                        appToast.success(context.getString(R.string.card_detail_locked_success))
                    }
                }
            )

            if (showUnlockConfirm) {
                AlertDialog(
                    onDismissRequest = { showUnlockConfirm = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = AppColors.GoldDark,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.card_detail_unlock_title),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.card_detail_unlock_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextPrimary
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleCardLock(displayCard.id)
                                appToast.success(context.getString(R.string.card_detail_unlocked_success))
                                showUnlockConfirm = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = AppColors.GoldDark)
                        ) {
                            Text(stringResource(R.string.card_detail_unlock_confirm), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUnlockConfirm = false }) {
                            Text(stringResource(R.string.action_cancel), color = AppColors.TextSecondary)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = AppColors.Surface
                )
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Edit dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = AppColors.TextHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.GreenPrimary,
                unfocusedBorderColor = AppColors.Divider
            )
        )
    }
}

@Composable
fun EditCardDialog(
    card: Card,
    onDismiss: () -> Unit,
    onConfirm: (Card) -> Unit
) {
    var farmerName by remember { mutableStateOf(card.name) }
    var traderName by remember { mutableStateOf(card.traderName) }
    var riceVariety by remember { mutableStateOf(card.riceVariety) }
    var seasonLabel by remember { mutableStateOf(card.seasonLabel) }
    var moisturePercent by remember { mutableStateOf(if (card.moisturePercent > 0) card.moisturePercent.toString() else "") }
    var pricePerKg by remember { mutableStateOf(if (card.pricePerKg > 0) "%.0f".format(card.pricePerKg) else "") }
    var depositAmount by remember { mutableStateOf(if (card.depositAmount > 0) "%.0f".format(card.depositAmount) else "") }
    var paidAmount by remember { mutableStateOf(if (card.paidAmount > 0) "%.0f".format(card.paidAmount) else "") }

    val isValid = farmerName.isNotBlank() && traderName.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = AppColors.CardBg,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.GreenSurface)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.card_detail_edit_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.GreenPrimary
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FormTextField(
                        value = farmerName,
                        onValueChange = { farmerName = it },
                        label = stringResource(R.string.card_detail_farmer_name_label),
                        placeholder = stringResource(R.string.card_detail_farmer_name_placeholder)
                    )

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
                            label = stringResource(R.string.card_detail_season_label),
                            placeholder = stringResource(R.string.card_detail_season_placeholder),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    FormTextField(
                        value = traderName,
                        onValueChange = { traderName = it },
                        label = stringResource(R.string.card_detail_trader_name_label),
                        placeholder = stringResource(R.string.card_detail_trader_name_placeholder)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = moisturePercent,
                            onValueChange = { moisturePercent = it },
                            label = { Text(stringResource(R.string.create_card_moisture_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = pricePerKg,
                            onValueChange = { pricePerKg = it },
                            label = { Text(stringResource(R.string.create_card_price_label)) },
                            visualTransformation = ThousandSeparatorTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = depositAmount,
                            onValueChange = { depositAmount = it },
                            label = { Text(stringResource(R.string.create_card_deposit_label)) },
                            visualTransformation = ThousandSeparatorTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = paidAmount,
                            onValueChange = { paidAmount = it },
                            label = { Text(stringResource(R.string.card_detail_paid_amount_label)) },
                            visualTransformation = ThousandSeparatorTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = AppColors.Divider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel), color = AppColors.TextSecondary)
                    }
                    Button(
                        onClick = {
                            val updated = card.copy(
                                name = farmerName.trim(),
                                traderName = traderName.trim(),
                                riceVariety = riceVariety,
                                seasonLabel = seasonLabel.trim(),
                                moisturePercent = moisturePercent.toDoubleOrNull() ?: 0.0,
                                pricePerKg = pricePerKg.toDoubleOrNull() ?: 0.0,
                                depositAmount = depositAmount.toDoubleOrNull() ?: 0.0,
                                paidAmount = paidAmount.toDoubleOrNull() ?: 0.0
                            )
                            onConfirm(updated)
                        },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.card_detail_save_changes), color = Color.White)
                    }
                }
            }
        }
    }
}