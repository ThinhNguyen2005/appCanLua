package com.giathinh.canlua.ui.screen

import com.giathinh.canlua.R

import android.content.Intent
import android.net.Uri
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.giathinh.canlua.ui.component.PermissionRationaleDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.ui.component.CustomHeader
import com.giathinh.canlua.ui.component.RiceVarietyDropdown
import com.giathinh.canlua.ui.component.ThousandSeparatorTransformation
import com.giathinh.canlua.ui.component.detail.BagEntriesCard
import com.giathinh.canlua.ui.component.detail.BagEntryActionSheet
import com.giathinh.canlua.ui.component.detail.CardInfoCard
import com.giathinh.canlua.ui.component.detail.FinancialSummaryCard
import com.giathinh.canlua.ui.component.detail.WeightSummaryCard
import com.giathinh.canlua.ui.component.rememberHeaderHeights
import com.giathinh.canlua.ui.feedback.LocalAppToast
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.util.isScrollingUp
import com.giathinh.canlua.ui.viewmodel.CardDetailViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.canlua.ui.component.TransitionSafeWrapper
import com.giathinh.canlua.ui.component.CardDetailSkeleton

private val DETAIL_DATE_FMT: SimpleDateFormat =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

private val DETAIL_DATE_TIME_FMT: SimpleDateFormat =
    SimpleDateFormat("HH:mm · dd/MM/yyyy", Locale.getDefault())

@Composable
fun CardDetailScreen(
    cardId: Long,
    navController: NavController
) {
    com.giathinh.canlua.util.TrackScreenRender("card_detail")
    val viewModel: CardDetailViewModel = hiltViewModel()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isDataReady = !isLoading

    // Kích hoạt load dữ liệu ban đầu ngay lập tức ở ngoài wrapper để tránh deadlock.
    LaunchedEffect(cardId) {
        viewModel.loadCardById(cardId)
    }

    CardDetailScreenContent(
        cardId = cardId,
        navController = navController,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreenContent(
    cardId: Long,
    navController: NavController,
    viewModel: CardDetailViewModel
) {
    val recordLocation = remember { { viewModel.refreshFieldLocation(cardId) } }
    val toggleLock = remember { { viewModel.toggleCardLock(cardId) } }
    val currentCard by viewModel.currentCard.collectAsStateWithLifecycle()
    val weightEntries by viewModel.weightEntries.collectAsStateWithLifecycle()


    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).apply {
            maximumFractionDigits = 1
        }
    }

    val heights = rememberHeaderHeights()
    val scrollState = rememberLazyListState()
    val density = LocalDensity.current

    // Tính 1 lần mỗi khi weightEntries đổi — dùng cho cả header lẫn nội dung bên trong.
    val lastEntryTimeForHeader = remember(currentCard, weightEntries) {
        if (currentCard == null) null else weightEntries.maxOfOrNull { it.timestamp }
    }

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val appToast = LocalAppToast.current
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            viewModel.refreshFieldLocation(cardId)
            appToast.info(context.getString(R.string.card_detail_updating_location))
        } else {
            appToast.error(context.getString(R.string.card_detail_location_denied))
        }
    }

    var showLocationRationale by remember { mutableStateOf(false) }

    val viewModelLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    // Pull-to-refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()
    LaunchedEffect(viewModelLoading) {
        if (!viewModelLoading) {
            isRefreshing = false
        }
    }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            viewModel.loadCardById(cardId)
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
                val isScrollingUp = scrollState.isScrollingUp()
                val fabVisible = remember(isScrollingUp, entryActionTarget) {
                    isScrollingUp && entryActionTarget == null
                }
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
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        },
                        expanded = true,
                        containerColor = if (c.isLocked) AppColors.GreenPrimary.copy(alpha = 0.45f) else AppColors.GreenPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
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
        val card = currentCard ?: placeholderCard
        val isLoading = viewModelLoading

        // C-06: tables và pagerState khai báo NGOÀI Crossfade → không bị reset về page 0
        // mỗi khi skeleton → content transition. User giữ được vị trí trang hiện tại.
        val tables = remember(weightEntries) { weightEntries.chunked(25) }
        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { tables.size.coerceAtLeast(1) }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // === Content layer ===
            val activeTableIndex = pagerState.currentPage.coerceIn(0, (tables.size - 1).coerceAtLeast(0))

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
                                    DETAIL_DATE_FMT.format(card.date)
                                }
                                val updatedLabel = remember(card.lastModifiedMs, card.date) {
                                    val timestamp = card.lastModifiedMs.takeIf { it > 0L } ?: card.date.time
                                    DETAIL_DATE_TIME_FMT.format(Date(timestamp))
                                }
                                CardInfoCard(
                                    traderName = card.traderName,
                                    traderPhone = card.traderPhone,
                                    riceVariety = card.riceVariety,
                                    seasonLabel = card.seasonLabel,
                                    createdDateLabel = createdLabel,
                                    lastUpdatedLabel = updatedLabel,
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
                                        if (hasLocationPermission(context)) {
                                            viewModel.refreshFieldLocation(cardId)
                                            appToast.info(context.getString(R.string.card_detail_updating_location))
                                        } else {
                                            showLocationRationale = true
                                        }
                                    },
                                    isLocked = card.isLocked,
                                    cccd = card.cccd,
                                    modifier = Modifier.combinedClickable(
                                        onDoubleClick = {
                                            if (card.isLocked) {
                                                appToast.warning(context.getString(R.string.card_detail_unlock_table_first))
                                            } else {
                                                showEditDialog = true
                                            }
                                        },
                                        onClick = {}
                                    )
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
                                    isLocked = card.isLocked,
                                    impurityIsPercent = card.impurityIsPercent,
                                    bagMethodIsSampling = card.bagMethodIsSampling,
                                    bagSampleCount = card.bagSampleCount,
                                    bagSampleTotalWeight = card.bagSampleTotalWeight,
                                    modifier = Modifier.combinedClickable(
                                        onDoubleClick = {
                                            if (card.isLocked) {
                                                appToast.warning(context.getString(R.string.card_detail_unlock_table_first))
                                            } else {
                                                showEditDialog = true
                                            }
                                        },
                                        onClick = {}
                                    )
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
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        showLocationRationale = showLocationRationale,
                        permissionLauncher = permissionLauncher,
                        onDismiss = { showEditDialog = false },
                        onConfirm = { updatedCard ->
                            viewModel.updateCard(updatedCard)
                            showEditDialog = false
                            appToast.success(context.getString(R.string.card_detail_update_success))
                        },
                        onLocationRationaleConfirm = {
                            showLocationRationale = false
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onLocationRationaleDismiss = {
                            showLocationRationale = false
                        },
                        onRequestLocationPermission = {
                            showLocationRationale = true
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
            // === HEADER LAYER (luôn render từ frame 0) ===
            // Đặt OUT of if/else → header tồn tại NGAY khi composable mount,
            // không có cảm giác "appear sau content". Khi data từ null → loaded,
            // chỉ Text bên trong recompose (rẻ), view tree không tear down/rebuild.
            CustomHeader(
                card = card,
                collapseFraction = collapseFraction,
                lastEntryTime = lastEntryTimeForHeader,
                modifier = Modifier.height(headerHeight),
                onBack = { navController.popBackStack() },
                onAdd = {
                    if (isLoading) return@CustomHeader
                    if (card.isLocked) {
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
                    if (card.isLocked) {
                        appToast.warning(context.getString(R.string.card_detail_unlock_table_first))
                    } else {
                        showEditDialog = true
                    }
                },
                onDeleteCard = {
                    if (isLoading) return@CustomHeader
                    if (card.isLocked) {
                        appToast.warning(context.getString(R.string.card_detail_unlock_table_first))
                    } else if (!card.isPaid) {
                        appToast.warning(context.getString(R.string.card_detail_delete_blocked_unpaid))
                    } else {
                        showDeleteConfirm = true
                    }
                },
                onExportPdf = {
                    if (isLoading) return@CustomHeader
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching {
                            val file = com.giathinh.canlua.util.PdfExporter.export(
                                context = context,
                                card = card,
                                entries = weightEntries
                            )
                            val uri = com.giathinh.canlua.util.PdfExporter.shareUri(context, file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.card_detail_pdf_subject, card.id))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(intent, context.getString(R.string.card_detail_pdf_share_title))
                            )
                        }.onFailure {
                            com.giathinh.canlua.util.AnalyticsHelper.logNonFatal(it, tag = "pdf_export")
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
                    if (card.isLocked) {
                        // Mở khóa phiếu đã chốt → cần xác nhận để tránh nhấn nhầm.
                        showUnlockConfirm = true
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleCardLock(card.id)
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
                                viewModel.toggleCardLock(card.id)
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

            if (showLocationRationale && !showEditDialog) {
                PermissionRationaleDialog(
                    title = stringResource(R.string.permission_location_title),
                    message = stringResource(R.string.permission_location_rationale),
                    confirmText = stringResource(R.string.permission_location_button),
                    dismissText = stringResource(R.string.permission_dismiss_button),
                    onConfirm = {
                        showLocationRationale = false
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onDismiss = {
                        showLocationRationale = false
                    }
                )
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Edit dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EditCardDialog(
    card: Card,
    showLocationRationale: Boolean,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    onDismiss: () -> Unit,
    onConfirm: (Card) -> Unit,
    onLocationRationaleConfirm: () -> Unit,
    onLocationRationaleDismiss: () -> Unit,
    onRequestLocationPermission: () -> Unit
) {
    // ── Tab 0: Thông tin chung ────────────────────────────────────────────────
    var farmerName by remember { mutableStateOf(card.name) }
    var traderName by remember { mutableStateOf(card.traderName) }
    var traderPhone by remember { mutableStateOf(card.traderPhone) }
    var cccd by remember { mutableStateOf(card.cccd.orEmpty()) }
    var riceVariety by remember { mutableStateOf(card.riceVariety) }
    var seasonLabel by remember { mutableStateOf(card.seasonLabel) }

    // ── Tab 1: Trừ hao & Tài chính ───────────────────────────────────────────
    var moisturePercent by remember { mutableStateOf(if (card.moisturePercent > 0) card.moisturePercent.toString() else "") }
    var impurityWeight by remember { mutableStateOf(if (card.impurityWeight > 0) card.impurityWeight.toString() else "") }
    var pricePerKg by remember { mutableStateOf(if (card.pricePerKg > 0) "%.0f".format(card.pricePerKg) else "") }
    var depositAmount by remember { mutableStateOf(if (card.depositAmount > 0) "%.0f".format(card.depositAmount) else "") }
    var paidAmount by remember { mutableStateOf(if (card.paidAmount > 0) "%.0f".format(card.paidAmount) else "") }

    val isValid = farmerName.isNotBlank() &&
                  traderName.isNotBlank() &&
                  (cccd.isBlank() || cccd.length == 12)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = AppColors.CardBg,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Dialog header ─────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.GreenSurface)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = AppColors.GreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.card_detail_edit_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.GreenPrimary
                        )
                    }
                }

                // ── Form (Scrollable Content) ──────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Card 1: Thông tin chung (Accent Xanh lá)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(AppColors.GreenPrimary)
                            )
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.edit_card_tab_info),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.GreenPrimary
                                )

                                // Tên chủ ruộng
                                OutlinedTextField(
                                    value = farmerName,
                                    onValueChange = { farmerName = it },
                                    label = { Text(stringResource(R.string.card_detail_farmer_name_label)) },
                                    placeholder = { Text(stringResource(R.string.card_detail_farmer_name_placeholder), color = AppColors.TextHint) },
                                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppColors.GreenPrimary,
                                        unfocusedBorderColor = AppColors.Divider
                                    )
                                )

                                // Tên thương lái
                                OutlinedTextField(
                                    value = traderName,
                                    onValueChange = { traderName = it },
                                    label = { Text(stringResource(R.string.card_detail_trader_name_label)) },
                                    placeholder = { Text(stringResource(R.string.card_detail_trader_name_placeholder), color = AppColors.TextHint) },
                                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppColors.GreenPrimary,
                                        unfocusedBorderColor = AppColors.Divider
                                    )
                                )

                                // SĐT & CCCD (50/50)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = traderPhone,
                                        onValueChange = { input ->
                                            val filtered = input.filter { it.isDigit() || it == '+' }
                                            if (filtered.length <= 15) traderPhone = filtered
                                        },
                                        label = { Text(stringResource(R.string.pdf_trader_phone)) },
                                        leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AppColors.GreenPrimary,
                                            unfocusedBorderColor = AppColors.Divider
                                        )
                                    )
                                    OutlinedTextField(
                                        value = cccd,
                                        onValueChange = { input ->
                                            val filtered = input.filter { it.isDigit() }
                                            if (filtered.length <= 12) cccd = filtered
                                        },
                                        label = { Text(stringResource(R.string.create_card_cccd_label)) },
                                        placeholder = { Text(stringResource(R.string.create_card_cccd_placeholder), color = AppColors.TextHint) },
                                        leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        isError = cccd.isNotBlank() && cccd.length != 12,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AppColors.GreenPrimary,
                                            unfocusedBorderColor = AppColors.Divider
                                        )
                                    )
                                }

                                // Giống lúa & Vụ mùa (50/50)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        RiceVarietyDropdown(
                                            selected = riceVariety,
                                            onSelect = { riceVariety = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AppColors.GreenPrimary,
                                                unfocusedBorderColor = AppColors.Divider,
                                                focusedLabelColor = AppColors.GreenPrimary,
                                                unfocusedLabelColor = AppColors.TextHint
                                            )
                                        )
                                    }
                                    OutlinedTextField(
                                        value = seasonLabel,
                                        onValueChange = { seasonLabel = it },
                                        label = { Text(stringResource(R.string.card_detail_season_label)) },
                                        placeholder = { Text(stringResource(R.string.card_detail_season_placeholder), color = AppColors.TextHint) },
                                        leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AppColors.GreenPrimary,
                                            unfocusedBorderColor = AppColors.Divider
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Card 2: Trừ hao & Tài chính (Accent Cam)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(AppColors.Orange)
                            )
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.edit_card_tab_financial),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Orange
                                )

                                // Độ ẩm & Tạp chất (50/50)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = moisturePercent,
                                        onValueChange = { input ->
                                            val filtered = input.filter { it.isDigit() || it == '.' }
                                            if (filtered.length <= 4) moisturePercent = filtered
                                        },
                                        label = { Text(stringResource(R.string.create_card_moisture_label)) },
                                        placeholder = { Text("0.0", color = AppColors.TextHint) },
                                        leadingIcon = { Icon(Icons.Outlined.WaterDrop, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        suffix = { Text("%", color = AppColors.TextSecondary) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AppColors.GreenPrimary,
                                            unfocusedBorderColor = AppColors.Divider
                                        )
                                    )

                                    OutlinedTextField(
                                        value = impurityWeight,
                                        onValueChange = { input ->
                                            val filtered = input.filter { it.isDigit() || it == '.' }
                                            if (filtered.length <= 5) impurityWeight = filtered
                                        },
                                        label = { Text(stringResource(R.string.create_card_impurity_weight_label)) },
                                        placeholder = { Text("0.0", color = AppColors.TextHint) },
                                        leadingIcon = { Icon(Icons.Outlined.FilterAlt, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        suffix = { Text("kg", color = AppColors.TextSecondary) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AppColors.GreenPrimary,
                                            unfocusedBorderColor = AppColors.Divider
                                        )
                                    )
                                }

                                // Giá/kg
                                OutlinedTextField(
                                    value = pricePerKg,
                                    onValueChange = { input ->
                                        val filtered = input.filter { it.isDigit() }
                                        pricePerKg = filtered
                                    },
                                    label = { Text(stringResource(R.string.create_card_price_label)) },
                                    leadingIcon = { Icon(Icons.Outlined.AttachMoney, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    suffix = { Text(stringResource(R.string.edit_card_price_suffix), color = AppColors.TextSecondary) },
                                    visualTransformation = ThousandSeparatorTransformation(),
                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.GreenPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppColors.GreenPrimary,
                                        unfocusedBorderColor = AppColors.Divider
                                    )
                                )

                                // Đặt cọc & Đã trả (50/50)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = depositAmount,
                                        onValueChange = { input ->
                                            val filtered = input.filter { it.isDigit() }
                                            depositAmount = filtered
                                        },
                                        label = { Text(stringResource(R.string.create_card_deposit_label)) },
                                        leadingIcon = { Icon(Icons.Outlined.Savings, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        visualTransformation = ThousandSeparatorTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AppColors.GreenPrimary,
                                            unfocusedBorderColor = AppColors.Divider
                                        )
                                    )
                                    OutlinedTextField(
                                        value = paidAmount,
                                        onValueChange = { input ->
                                            val filtered = input.filter { it.isDigit() }
                                            paidAmount = filtered
                                        },
                                        label = { Text(stringResource(R.string.card_detail_paid_amount_label)) },
                                        leadingIcon = { Icon(Icons.Outlined.Payments, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        visualTransformation = ThousandSeparatorTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
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

                // ── Footer Buttons ────────────────────────────────────────────
                HorizontalDivider(color = AppColors.Divider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1.5f).height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel).uppercase(),
                            color = AppColors.TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            val updated = card.copy(
                                name = farmerName.trim(),
                                traderName = traderName.trim(),
                                riceVariety = riceVariety,
                                seasonLabel = seasonLabel.trim(),
                                moisturePercent = moisturePercent.toDoubleOrNull() ?: card.moisturePercent,
                                impurityWeight = impurityWeight.toDoubleOrNull() ?: card.impurityWeight,
                                pricePerKg = pricePerKg.toDoubleOrNull() ?: card.pricePerKg,
                                depositAmount = depositAmount.toDoubleOrNull() ?: card.depositAmount,
                                paidAmount = paidAmount.toDoubleOrNull() ?: card.paidAmount,
                                traderPhone = traderPhone.trim(),
                                cccd = cccd.trim().takeIf { it.isNotEmpty() },
                                fieldAddress = card.fieldAddress
                            )
                            onConfirm(updated)
                        },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary),
                        modifier = Modifier.weight(2f).height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.card_detail_save_changes).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showLocationRationale) {
        PermissionRationaleDialog(
            title = stringResource(R.string.permission_location_title),
            message = stringResource(R.string.permission_location_rationale),
            confirmText = stringResource(R.string.permission_location_button),
            dismissText = stringResource(R.string.permission_dismiss_button),
            onConfirm = onLocationRationaleConfirm,
            onDismiss = onLocationRationaleDismiss
        )
    }
}

private fun hasLocationPermission(context: android.content.Context): Boolean {
    return androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
    androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}
