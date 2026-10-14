package com.giathinh.canlua.ui.screen

import com.giathinh.canlua.R
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import android.content.Intent
import android.net.Uri
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.text.style.TextAlign
import com.giathinh.canlua.ui.theme.AppDimensions
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
import androidx.core.net.toUri
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
import com.giathinh.canlua.ui.viewmodel.CardDetailViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.canlua.ui.util.rememberScrollDirectionState


@Composable
fun CardDetailScreen(
    cardId: Long,
    navController: NavController
) {
    com.giathinh.canlua.util.TrackScreenRender("card_detail")
    val viewModel: CardDetailViewModel = hiltViewModel()

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
    val currentCard by viewModel.currentCard.collectAsStateWithLifecycle()
    val weightEntries by viewModel.weightEntries.collectAsStateWithLifecycle()

    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dateTimeFmt = remember { SimpleDateFormat("HH:mm · dd/MM/yyyy", Locale.getDefault()) }

    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).apply {
            maximumFractionDigits = 1
        }
    }

    val heights = rememberHeaderHeights()
    val scrollState = rememberLazyListState()
    val scrollDir = rememberScrollDirectionState()
    val density = LocalDensity.current

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val appToast = LocalAppToast.current
    val scope = rememberCoroutineScope()

    val locationUpdatingMsg = stringResource(R.string.card_detail_updating_location)
    val locationDeniedMsg = stringResource(R.string.card_detail_location_denied)
    val callAppErrorMsg = stringResource(R.string.card_detail_call_app_error)
    val missingGpsMsg = stringResource(R.string.card_detail_missing_gps)
    val defaultFieldLabel = stringResource(R.string.card_detail_default_field_label)
    val mapAppErrorMsg = stringResource(R.string.card_detail_map_app_error)
    val unlockTableFirstMsg = stringResource(R.string.card_detail_unlock_table_first)
    val unlockCardFirstMsg = stringResource(R.string.card_detail_unlock_card_first)
    val cardLockedMsg = stringResource(R.string.card_detail_card_locked)
    val updateSuccessMsg = stringResource(R.string.card_detail_update_success)
    val lockedSuccessMsg = stringResource(R.string.card_detail_locked_success)
    val unlockedSuccessMsg = stringResource(R.string.card_detail_unlocked_success)
    val deleteBlockedUnpaidMsg = stringResource(R.string.card_detail_delete_blocked_unpaid)
    val deletedBagFmt = stringResource(R.string.card_detail_deleted_bag, "%d")
    val pdfPremiumRequiredMsg = stringResource(R.string.card_detail_pdf_premium_required)
    val pdfSubjectTemplate = stringResource(R.string.card_detail_pdf_subject)
    val pdfShareTitle = stringResource(R.string.card_detail_pdf_share_title)
    val pdfErrorTemplate = stringResource(R.string.card_detail_pdf_error)
    val unknownErrorMsg = stringResource(R.string.card_detail_unknown_error)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            viewModel.refreshFieldLocation(cardId)
            appToast.success(locationUpdatingMsg)
        } else {
            appToast.error(locationDeniedMsg)
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
    var entryActionGlobalIndex by remember {
        mutableIntStateOf(0)
    }
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

    val headerHeight = lerp(heights.expanded, heights.collapsed, collapseFraction)

    var showOverflow by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showUnlockConfirm by remember { mutableStateOf(false) }
    var showEditEntryDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<WeightEntry?>(null) }
    var entryToEditIndex by remember { mutableIntStateOf(0) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }

    val loadedCard = currentCard
    if (!viewModelLoading && loadedCard == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Surface)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(AppDimensions.SpacingLg)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(AppDimensions.SpacingMd))
                Text(
                    text = stringResource(R.string.weight_input_card_not_found),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(AppDimensions.SpacingSm))
                Text(
                    text = stringResource(R.string.weight_input_card_not_found_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(AppDimensions.SpacingLg))
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.GreenPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(AppDimensions.CornerRadiusMd),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .padding(horizontal = AppDimensions.SpacingMd)
                ) {
                    Text(
                        text = stringResource(R.string.weight_input_back_content),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        return
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollDir),
        containerColor = AppColors.Surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            currentCard?.let { c ->
                if (entryActionTarget == null) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (c.isLocked) showUnlockConfirm = true
                            else navController.navigate("weight_input/${cardId}")
                        },
                        icon = {
                            Icon(
                                imageVector = if (c.isLocked) Icons.Default.LockOpen else Icons.Outlined.Scale,
                                contentDescription = null,
                                modifier = Modifier.size(AppDimensions.IconLg)
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.card_detail_weigh_action),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        expanded = scrollDir.isScrollingUp || !scrollState.canScrollBackward,
                        containerColor = if (c.isLocked) AppColors.GoldDark else AppColors.GreenPrimary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(AppDimensions.CornerRadiusMd),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(heights.expanded + 16.dp)) }

                    // Card 0: Thông tin phiếu
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            val createdLabel = remember(card.date, dateFmt) {
                                dateFmt.format(card.date)
                            }
                            val updatedLabel = remember(card.lastModifiedMs, card.date, dateTimeFmt) {
                                val timestamp = card.lastModifiedMs.takeIf { it > 0L } ?: card.date.time
                                dateTimeFmt.format(Date(timestamp))
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
                                        val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    }.onFailure {
                                        appToast.error(callAppErrorMsg)
                                    }
                                },
                                onOpenMap = {
                                    val lat = card.latitude
                                    val lon = card.longitude
                                    if (lat == null || lon == null) {
                                        appToast.error(missingGpsMsg)
                                        return@CardInfoCard
                                    }
                                    val label = card.fieldAddress.ifBlank { card.name.ifBlank { defaultFieldLabel } }
                                    val uri = "geo:$lat,$lon?q=$lat,$lon(${Uri.encode(label)})".toUri()
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    }.onFailure {
                                        appToast.error(mapAppErrorMsg)
                                    }
                                },
                                onRefreshLocation = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (hasLocationPermission(context)) {
                                        viewModel.refreshFieldLocation(cardId)
                                        appToast.info(locationUpdatingMsg)
                                    } else {
                                        showLocationRationale = true
                                    }
                                },
                                isLocked = card.isLocked,
                                cccd = card.cccd,
                                modifier = Modifier.combinedClickable(
                                    onClickLabel = stringResource(R.string.card_detail_edit_title),
                                    onLongClickLabel = stringResource(R.string.card_detail_edit_title),
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (card.isLocked) {
                                            showUnlockConfirm = true
                                        } else {
                                            showEditDialog = true
                                        }
                                    },
                                    onDoubleClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (card.isLocked) {
                                            showUnlockConfirm = true
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
                                            appToast.warning(unlockCardFirstMsg)
                                        } else {
                                            showEditDialog = true
                                        }
                                    },
                                    onClick = {}
                                )
                            )
                        }
                    }

                    // Card 2: Tài chính
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
                                numberFormat = numberFormat,
                                onAddPayment = {
                                    showAddPaymentDialog = true
                                }
                            )
                        }
                    }

                    // Card 4: Chi tiết bao cân
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            BagEntriesCard(
                                bagCountTotal = weightEntries.size,
                                tables = tables,
                                pagerState = pagerState,
                                isLocked = card.isLocked,
                                onTableSelected = { index ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                onAddFirstBag = {
                                    if (card.isLocked) {
                                        appToast.warning(unlockTableFirstMsg)
                                    } else {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        navController.navigate("weight_input/${cardId}")
                                    }
                                },
                                onEntryClick = { entry, globalIdx ->
                                    if (card.isLocked) {
                                        appToast.warning(cardLockedMsg)
                                    } else {
                                        entryActionGlobalIndex = globalIdx
                                        entryActionTarget = entry
                                    }
                                },
                                numberFormat = numberFormat
                            )
                        }
                    }
                }
            }

            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = {
                        Text(
                            text = stringResource(R.string.card_detail_delete_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.card_detail_delete_message),
                            color = AppColors.TextSecondary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showDeleteConfirm = false
                                scope.launch {
                                    viewModel.deleteCard(card)
                                    navController.popBackStack()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Error,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(AppDimensions.CornerRadiusMd),
                            modifier = Modifier.heightIn(min = AppDimensions.TouchTargetMin)
                        ) {
                            Text(
                                text = stringResource(R.string.card_detail_delete_confirm),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteConfirm = false },
                            modifier = Modifier.heightIn(min = AppDimensions.TouchTargetMin)
                        ) {
                            Text(
                                text = stringResource(R.string.action_cancel),
                                color = AppColors.TextSecondary
                            )
                        }
                    },
                    containerColor = AppColors.CardBg,
                    shape = RoundedCornerShape(AppDimensions.CornerRadiusLg)
                )
            }

            if (showEditDialog) {
                EditCardDialog(
                    card = card,
                    showLocationRationale = showLocationRationale,
                    onDismiss = { showEditDialog = false },
                    onConfirm = { updatedCard ->
                        viewModel.updateCard(updatedCard)
                        showEditDialog = false
                        appToast.success(updateSuccessMsg)
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
                    }
                )
            }

            entryActionTarget?.let { entry ->
                BagEntryActionSheet(
                    entry = entry,
                    globalIndex = entryActionGlobalIndex,
                    numberFormat = numberFormat,
                    onDismiss = { entryActionTarget = null },
                    onEdit = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        entryToEdit = entry
                        entryToEditIndex = entryActionGlobalIndex
                        showEditEntryDialog = true
                        entryActionTarget = null
                    },
                    onDelete = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.deleteWeightEntry(entry)
                        entryActionTarget = null

                        appToast.success(deletedBagFmt.format(entryActionGlobalIndex))
                    }
                )
            }

            if (showEditEntryDialog && entryToEdit != null) {
                val targetEntry = entryToEdit!!
                EditWeightEntryDialog(
                    entry = targetEntry,
                    bagIndex = entryToEditIndex,
                    onDismiss = {
                        showEditEntryDialog = false
                        entryToEdit = null
                    },
                    onConfirm = { newWeight ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.updateWeightEntry(targetEntry.copy(weight = newWeight))
                        appToast.success("Đã cập nhật bao #$entryToEditIndex: ${numberFormat.format(newWeight)} kg")
                        showEditEntryDialog = false
                        entryToEdit = null
                    }
                )
            }

            if (showAddPaymentDialog) {
                AddPaymentDialog(
                    remainingAmount = card.remainingAmount,
                    numberFormat = numberFormat,
                    onDismiss = { showAddPaymentDialog = false },
                    onConfirm = { amount, description ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.addPayment(card.id, amount, description)
                        appToast.success("Đã ghi nhận thanh toán ${numberFormat.format(amount)} đ")
                        showAddPaymentDialog = false
                    }
                )
            }

            CustomHeader(
                card = card,
                modifier = Modifier.height(headerHeight),
                onBack = { navController.popBackStack() },
                showOverflow = showOverflow,
                onOverflowChange = { if (!isLoading) showOverflow = it },
                onEditCard = {
                    if (isLoading) return@CustomHeader
                    if (card.isLocked) {
                        appToast.warning(unlockTableFirstMsg)
                    } else {
                        showEditDialog = true
                    }
                },
                onDeleteCard = {
                    if (isLoading) return@CustomHeader
                    if (card.isLocked) {
                        appToast.warning(unlockTableFirstMsg)
                    } else if (!card.isPaid) {
                        appToast.warning(deleteBlockedUnpaidMsg)
                    } else {
                        showDeleteConfirm = true
                    }
                },
                onExportPdf = {
                    if (isLoading) return@CustomHeader
                    if (!com.giathinh.canlua.util.PremiumState.isPremium.value) {
                        appToast.warning(pdfPremiumRequiredMsg)
                        navController.navigate("premium")
                        return@CustomHeader
                    }
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
                                putExtra(Intent.EXTRA_SUBJECT, pdfSubjectTemplate.format(card.id))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(intent, pdfShareTitle)
                            )
                        }.onFailure {
                            com.giathinh.canlua.util.AnalyticsHelper.logNonFatal(it, tag = "pdf_export")
                            appToast.error(
                                pdfErrorTemplate.format(it.message ?: unknownErrorMsg)
                            )
                        }
                    }
                },
                onToggleLock = {
                    if (isLoading) return@CustomHeader
                    if (card.isLocked) {
                        showUnlockConfirm = true
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleCardLock(card.id)
                        appToast.success(lockedSuccessMsg)
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
                                appToast.success(unlockedSuccessMsg)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCardDialog(
    card: Card,
    showLocationRationale: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Card) -> Unit,
    onLocationRationaleConfirm: () -> Unit,
    onLocationRationaleDismiss: () -> Unit
) {
    // ── Tab 0: Thông tin chung ────────────────────────────────────────────────
    var farmerName by remember { mutableStateOf(card.name) }
    var traderName by remember { mutableStateOf(card.traderName) }
    var traderPhone by remember { mutableStateOf(card.traderPhone) }
    var cccd by remember { mutableStateOf(card.cccd.orEmpty()) }
    var riceVariety by remember { mutableStateOf(card.riceVariety) }
    var seasonLabel by remember { mutableStateOf(card.seasonLabel) }

    // ── Tab 1: Trừ hao ───────────────────────────────────────────────────────
    var moisturePercent by remember { mutableStateOf(if (card.moisturePercent > 0) card.moisturePercent.toString() else "") }
    var impurityWeight by remember { mutableStateOf(if (card.impurityWeight > 0) card.impurityWeight.toString() else "") }

    // ── Tab 2: Tài chính ─────────────────────────────────────────────────────
    var pricePerKg by remember { mutableStateOf(if (card.pricePerKg > 0) "%.0f".format(card.pricePerKg) else "") }
    var depositAmount by remember { mutableStateOf(if (card.depositAmount > 0) "%.0f".format(card.depositAmount) else "") }
    var paidAmount by remember { mutableStateOf(if (card.paidAmount > 0) "%.0f".format(card.paidAmount) else "") }

    var selectedTab by remember { mutableIntStateOf(0) }

    val isValid = farmerName.isNotBlank() &&
            traderName.isNotBlank() &&
            (cccd.isBlank() || cccd.length == 12)

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

                // ── Tab selector (Dùng SecondaryTabRow mới thay cho TabRow deprecated) ──
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = AppColors.CardBg,
                    contentColor = AppColors.GreenPrimary,
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(selectedTab),
                            color = AppColors.GreenPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Thông tin",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        icon = {
                            Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        selectedContentColor = AppColors.GreenPrimary,
                        unselectedContentColor = AppColors.TextSecondary
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Trừ hao",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        icon = {
                            Icon(Icons.Outlined.Scale, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        selectedContentColor = AppColors.GreenPrimary,
                        unselectedContentColor = AppColors.TextSecondary
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "Tài chính",
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        icon = {
                            Icon(Icons.Outlined.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        selectedContentColor = AppColors.GreenPrimary,
                        unselectedContentColor = AppColors.TextSecondary
                    )
                }

                // ── Tab content ───────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // ── Tab 0: Thông tin chung ────────────────────────
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

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
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
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
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
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppColors.GreenPrimary,
                                        unfocusedBorderColor = AppColors.Divider
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    RiceVarietyDropdown(
                                        selected = riceVariety,
                                        onSelect = { riceVariety = it },
                                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
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
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppColors.GreenPrimary,
                                        unfocusedBorderColor = AppColors.Divider
                                    )
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Grass,
                                    contentDescription = null,
                                    tint = AppColors.TextHint,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Giống lúa chỉ hiển thị, không ảnh hưởng tính toán",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.TextHint
                                )
                            }
                        }

                        1 -> {
                            // ── Tab 1: Trừ hao ────────────────────────────────
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = AppColors.GreenSurface,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Scale,
                                        contentDescription = null,
                                        tint = AppColors.GreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Thay đổi sẽ tính lại khối lượng thực nhận",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.GreenPrimary
                                    )
                                }
                            }

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
                                modifier = Modifier.fillMaxWidth(),
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
                                suffix = {
                                    Text(
                                        text = if (card.impurityIsPercent) "%" else "kg",
                                        color = AppColors.TextSecondary
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppColors.GreenPrimary,
                                    unfocusedBorderColor = AppColors.Divider
                                )
                            )
                        }

                        2 -> {
                            // ── Tab 2: Tài chính ──────────────────────────────
                            OutlinedTextField(
                                value = pricePerKg,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }
                                    pricePerKg = filtered
                                },
                                label = { Text(stringResource(R.string.create_card_price_label)) },
                                leadingIcon = { Icon(Icons.Outlined.AttachMoney, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                suffix = { Text("đ/kg", color = AppColors.TextSecondary) },
                                visualTransformation = ThousandSeparatorTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppColors.GreenPrimary,
                                    unfocusedBorderColor = AppColors.Divider
                                )
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
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
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
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
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
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

                // ── Action buttons ────────────────────────────────────────────
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
                                moisturePercent = moisturePercent.toDoubleOrNull() ?: card.moisturePercent,
                                impurityWeight = impurityWeight.toDoubleOrNull() ?: card.impurityWeight,
                                pricePerKg = pricePerKg.toDoubleOrNull() ?: card.pricePerKg,
                                depositAmount = depositAmount.toDoubleOrNull() ?: card.depositAmount,
                                paidAmount = paidAmount.toDoubleOrNull() ?: card.paidAmount,
                                traderPhone = traderPhone.trim(),
                                cccd = cccd.trim().takeIf { it.isNotEmpty() }
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
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

@Composable
fun EditWeightEntryDialog(
    entry: WeightEntry,
    bagIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var weightText by remember {
        mutableStateOf(
            if (entry.weight % 1.0 == 0.0) {
                entry.weight.toInt().toString()
            } else {
                entry.weight.toString().replace('.', ',')
            }
        )
    }
    val parsedWeight = remember(weightText) {
        weightText.replace(',', '.').toDoubleOrNull()
    }
    val isValid = parsedWeight != null && parsedWeight in 0.1..200.0
    val focusRequester = remember { FocusRequester() }

    // Tự động focus và bật bàn phím số ngay khi mở hộp thoại sửa bao lúa (Giảm Interaction Cost)
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = AppDimensions.FormMaxWidth),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingXs)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Scale,
                    contentDescription = null,
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.size(AppDimensions.IconLg)
                )
                Text(
                    text = "Sửa số ký bao #$bagIndex",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSm)) {
                Text(
                    text = "Nhập lại số ký thực tế cho bao này (0,1 kg – 200 kg):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
                        if (filtered.count { it == '.' || it == ',' } <= 1) {
                            weightText = filtered
                        }
                    },
                    label = { Text("Số ký bao (kg)") },
                    suffix = {
                        Text(
                            text = "kg",
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextSecondary
                        )
                    },
                    singleLine = true,
                    isError = weightText.isNotBlank() && !isValid,
                    supportingText = {
                        if (weightText.isNotBlank() && !isValid) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingXxs)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = AppColors.Error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Số ký phải từ 0,1 đến 200 kg",
                                    color = AppColors.Error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isValid) {
                                onConfirm(parsedWeight)
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(AppDimensions.CornerRadiusMd),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.GreenPrimary,
                        unfocusedBorderColor = AppColors.Divider
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { parsedWeight?.let(onConfirm) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary),
                shape = RoundedCornerShape(AppDimensions.CornerRadiusMd),
                modifier = Modifier.heightIn(min = AppDimensions.TouchTargetMin)
            ) {
                Text(
                    text = stringResource(R.string.card_detail_save_changes),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = AppDimensions.TouchTargetMin)
            ) {
                Text(stringResource(R.string.action_cancel), color = AppColors.TextSecondary)
            }
        },
        containerColor = AppColors.CardBg,
        shape = RoundedCornerShape(AppDimensions.CornerRadiusLg)
    )
}

@Composable
fun AddPaymentDialog(
    remainingAmount: Double,
    numberFormat: NumberFormat,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, description: String) -> Unit
) {
    val fullRemainingLong = remainingAmount.toLong().coerceAtLeast(0L)
    var amountText by remember {
        mutableStateOf(if (fullRemainingLong > 0L) fullRemainingLong.toString() else "")
    }
    var descriptionText by remember { mutableStateOf("") }

    val rawAmount = remember(amountText) {
        amountText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    }
    val isValid = rawAmount > 0.0
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = AppDimensions.FormMaxWidth),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingXs)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Payments,
                    contentDescription = null,
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.size(AppDimensions.IconLg)
                )
                Text(
                    text = "Thanh toán thêm đợt này",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSm)) {
                if (remainingAmount > 0) {
                    Surface(
                        color = AppColors.WeightSurface,
                        shape = RoundedCornerShape(AppDimensions.CornerRadiusMd),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(AppDimensions.SpacingSm),
                            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingXs)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Còn lại cần trả:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.GoldDark,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${numberFormat.format(remainingAmount)} đ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AppColors.GoldDark,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            // Chip điền nhanh số tiền (Rule #14: Giảm thao tác gõ số ngoài đồng)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingXs)
                            ) {
                                SuggestionChip(
                                    onClick = {
                                        com.giathinh.canlua.util.HapticUtil.tick(context)
                                        amountText = fullRemainingLong.toString()
                                    },
                                    label = { Text("Trả đủ 100%", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                                SuggestionChip(
                                    onClick = {
                                        com.giathinh.canlua.util.HapticUtil.tick(context)
                                        amountText = (fullRemainingLong / 2).toString()
                                    },
                                    label = { Text("Trả 50%", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        amountText = input.filter { it.isDigit() }
                    },
                    label = { Text("Số tiền thanh toán (VNĐ)") },
                    suffix = {
                        Text(
                            text = "đ",
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextSecondary
                        )
                    },
                    visualTransformation = ThousandSeparatorTransformation(),
                    singleLine = true,
                    isError = amountText.isNotBlank() && !isValid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppDimensions.CornerRadiusMd),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.GreenPrimary,
                        unfocusedBorderColor = AppColors.Divider
                    )
                )

                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Ghi chú / Phương thức") },
                    placeholder = {
                        Text(
                            text = "Tiền mặt, Chuyển khoản...",
                            color = AppColors.TextHint
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isValid) {
                                val desc = descriptionText.trim().ifBlank { "Thanh toán thêm" }
                                onConfirm(rawAmount, desc)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppDimensions.CornerRadiusMd),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.GreenPrimary,
                        unfocusedBorderColor = AppColors.Divider
                    )
                )

                // Gợi ý chọn nhanh phương thức thanh toán (1 chạm thay vì gõ chữ)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingXs)
                ) {
                    listOf("Tiền mặt", "Chuyển khoản").forEach { method ->
                        FilterChip(
                            selected = descriptionText == method,
                            onClick = {
                                com.giathinh.canlua.util.HapticUtil.tick(context)
                                descriptionText = if (descriptionText == method) "" else method
                            },
                            label = { Text(method, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val desc = descriptionText.trim().ifBlank { "Thanh toán thêm" }
                    onConfirm(rawAmount, desc)
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary),
                shape = RoundedCornerShape(AppDimensions.CornerRadiusMd),
                modifier = Modifier.heightIn(min = AppDimensions.TouchTargetMin)
            ) {
                Text("Xác nhận trả", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = AppDimensions.TouchTargetMin)
            ) {
                Text(stringResource(R.string.action_cancel), color = AppColors.TextSecondary)
            }
        },
        containerColor = AppColors.CardBg,
        shape = RoundedCornerShape(AppDimensions.CornerRadiusLg)
    )
}
