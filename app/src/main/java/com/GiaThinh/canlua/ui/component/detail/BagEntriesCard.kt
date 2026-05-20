package com.GiaThinh.canlua.ui.component.detail

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.data.model.WeightEntry
import com.GiaThinh.canlua.ui.theme.AppColors
import java.text.NumberFormat

/**
 * Card hiển thị danh sách bao cân đã nhập, paginated theo bảng 5×5.
 *
 * Trước đây nằm chung trong `CardDetailScreen.kt` (~330 dòng) — tách ra để
 * file màn chính ngắn lại và composable này dễ test/preview riêng.
 *
 * @param bagCountTotal tổng số bao đã cân (hiển thị badge)
 * @param tables danh sách bảng — mỗi bảng tối đa 25 entries
 * @param pagerState state cho HorizontalPager swipe giữa các bảng
 * @param activeTableIndex index bảng đang chọn (sync với pagerState)
 * @param isLocked phiếu đã chốt → không cho long-press để xoá
 * @param onTableSelected user tap chip "Bảng N"
 * @param onAddFirstBag CTA khi `bagCountTotal == 0` → mở WeightInputScreen
 * @param onEntryLongPress long-press 1 ô trong grid (chỉ khi unlocked)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BagEntriesCard(
    bagCountTotal: Int,
    tables: List<List<WeightEntry>>,
    pagerState: PagerState,
    activeTableIndex: Int,
    isLocked: Boolean,
    onTableSelected: (Int) -> Unit,
    onAddFirstBag: () -> Unit,
    onEntryLongPress: (WeightEntry, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Scale,
                        contentDescription = null,
                        tint = AppColors.GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Chi tiết các bao cân",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }
                Surface(
                    color = AppColors.GreenSurface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "$bagCountTotal bao",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.GreenPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (bagCountTotal == 0) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Chưa có bao cân nào được nhập.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextHint,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = onAddFirstBag,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, AppColors.GreenPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.GreenPrimary)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Thêm bao cân đầu tiên", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                // Selector chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tables.size) { index ->
                        val isActive = index == activeTableIndex
                        val borderWidth by animateDpAsState(
                            targetValue = if (isActive) 1.5.dp else 0.dp,
                            animationSpec = tween(180),
                            label = "chip_border"
                        )
                        Surface(
                            onClick = { onTableSelected(index) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isActive) AppColors.GreenSurface else AppColors.SurfaceContainer,
                            border = if (isActive) BorderStroke(borderWidth, AppColors.GreenPrimary) else null,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = "Bảng ${index + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (isActive) AppColors.GreenPrimary else AppColors.TextSecondary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 8.dp,
                    pageSize = PageSize.Fill,
                    modifier = Modifier.fillMaxWidth()
                ) { pageIndex ->
                    if (pageIndex < tables.size) {
                        val tableEntries = tables[pageIndex]
                        BagGrid(
                            entries = tableEntries,
                            globalOffset = pageIndex * 25,
                            isLocked = isLocked,
                            onEntryLongPress = onEntryLongPress
                        )
                    }
                }
            }
        }
    }
}

/**
 * Row-major 5×5 grid: index 0..24 = (row=idx/5, col=idx%5).
 * Đọc trái-phải, trên-xuống — tự nhiên với người dùng.
 */
@Composable
private fun BagGrid(
    entries: List<WeightEntry>,
    globalOffset: Int,
    isLocked: Boolean,
    onEntryLongPress: (WeightEntry, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (r in 0 until 5) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (c in 0 until 5) {
                    val entryIdx = r * 5 + c
                    val entry = entries.getOrNull(entryIdx)
                    val globalIndex = globalOffset + entryIdx + 1
                    if (entry != null) {
                        BagCell(
                            indexLabel = "#$globalIndex",
                            weight = entry.weight,
                            isLocked = isLocked,
                            onLongPress = { onEntryLongPress(entry, globalIndex) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BagCell(
    indexLabel: String,
    weight: Double,
    isLocked: Boolean,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val weightText = if (weight % 1.0 == 0.0) "%.0f".format(weight) else "%.1f".format(weight)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.GreenSurface)
            .combinedClickable(
                onClick = { /* reserved for future quick edit */ },
                onLongClick = if (!isLocked) {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                } else null
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = indexLabel,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.GreenPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = weightText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
        }
    }
}

/**
 * Long-press trên bag cell → mở bottom sheet với option Xoá. Trước đây
 * delete trực tiếp dễ nhấn nhầm — sheet thêm 1 confirmation dialog nữa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BagEntryActionSheet(
    entry: WeightEntry,
    globalIndex: Int,
    numberFormat: NumberFormat,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var confirmDelete by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.CardBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.Divider) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.GreenSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Scale,
                        contentDescription = null,
                        tint = AppColors.GreenPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        "Bao cân #$globalIndex",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        "${numberFormat.format(entry.weight)} kg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
            }

            HorizontalDivider(color = AppColors.Divider)

            // Action: Delete
            Surface(
                onClick = { confirmDelete = true },
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = AppColors.Error
                    )
                    Text(
                        "Xóa bao cân này",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Error
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Xóa bao #$globalIndex?") },
            text = { Text("Hành động này không thể hoàn tác. Bao ${numberFormat.format(entry.weight)} kg sẽ bị xóa khỏi bảng.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Xóa", color = AppColors.Error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Hủy") }
            },
            containerColor = AppColors.CardBg
        )
    }
}
