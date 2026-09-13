package com.giathinh.canlua.ui.component.detail

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.R
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.ui.theme.AppColors
import java.text.NumberFormat

private fun formatWeight(weight: Double, numberFormat: NumberFormat? = null): String {
    return numberFormat?.format(weight) ?: "%.1f".format(weight)
}

/**
 * Card hiển thị danh sách bao cân đã nhập, paginated theo bảng 5×5.
 *
 * @param bagCountTotal tổng số bao đã cân (hiển thị badge)
 * @param tables danh sách bảng — mỗi bảng tối đa 25 entries
 * @param pagerState state cho HorizontalPager swipe giữa các bảng (single source of truth)
 * @param isLocked phiếu đã chốt → read-only, không cho click/xoá, ẩn nút thêm bao
 * @param onTableSelected user tap tab "Bảng N"
 * @param onAddFirstBag CTA khi `bagCountTotal == 0` và `!isLocked` → mở WeightInputScreen
 * @param onEntryClick tap 1 ô trong grid (chỉ khi unlocked)
 * @param numberFormat định dạng khối lượng hiển thị thống nhất
 */
@Composable
fun BagEntriesCard(
    bagCountTotal: Int,
    tables: List<List<WeightEntry>>,
    pagerState: PagerState,
    isLocked: Boolean,
    onTableSelected: (Int) -> Unit,
    onAddFirstBag: () -> Unit,
    onEntryClick: (WeightEntry, Int) -> Unit,
    numberFormat: NumberFormat? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) AppColors.LockedSurface else AppColors.CardBg
        ),
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
                        stringResource(R.string.detail_bag_entries_title),
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
                        stringResource(R.string.detail_bag_entries_count, bagCountTotal),
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
                        stringResource(R.string.detail_bag_entries_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextHint,
                        textAlign = TextAlign.Center
                    )
                    // Read-only khi isLocked: không hiện CTA thêm bao
                    if (!isLocked) {
                        OutlinedButton(
                            onClick = onAddFirstBag,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, AppColors.GreenPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.GreenPrimary)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.detail_bag_entries_add_first), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                // Table selector tabs (pagerState.currentPage là single source of truth)
                val currentPage = pagerState.currentPage.coerceIn(0, (tables.size - 1).coerceAtLeast(0))
                SecondaryScrollableTabRow(
                    selectedTabIndex = currentPage,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    tables.indices.forEach { index ->
                        val isActive = index == currentPage
                        val borderWidth by animateDpAsState(
                            targetValue = if (isActive) 1.5.dp else 0.dp,
                            animationSpec = tween(180),
                            label = "tab_border"
                        )
                        Tab(
                            selected = isActive,
                            onClick = { onTableSelected(index) },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isActive) AppColors.GreenSurface else AppColors.SurfaceContainer)
                                .border(
                                    if (isActive) BorderStroke(borderWidth, AppColors.GreenPrimary) else BorderStroke(0.dp, Color.Transparent),
                                    RoundedCornerShape(10.dp)
                                ),
                            text = {
                                Text(
                                    text = stringResource(R.string.detail_bag_entries_table, index + 1),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isActive) AppColors.GreenPrimary else AppColors.TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = AppColors.DividerStrong, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 8.dp,
                    pageSize = PageSize.Fill,
                    modifier = Modifier.fillMaxWidth()
                ) { pageIndex ->
                    val tableEntries = tables.getOrNull(pageIndex) ?: return@HorizontalPager
                    BagGrid(
                        entries = tableEntries,
                        globalOffset = pageIndex * 25,
                        isLocked = isLocked,
                        onEntryClick = onEntryClick,
                        numberFormat = numberFormat
                    )
                }
            }
        }
    }
}

/**
 * Column-major 5×5 grid: index 0..24 = (col=idx/5, row=idx%5).
 * Khớp với thứ tự nhập ở WeightInputScreen.
 */
@Composable
private fun BagGrid(
    entries: List<WeightEntry>,
    globalOffset: Int,
    isLocked: Boolean,
    onEntryClick: (WeightEntry, Int) -> Unit,
    numberFormat: NumberFormat? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (r in 0 until 5) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (c in 0 until 5) {
                    val entryIdx = c * 5 + r
                    val entry = entries.getOrNull(entryIdx)
                    val globalIndex = globalOffset + entryIdx + 1
                    if (entry != null) {
                        BagCell(
                            indexLabel = "#$globalIndex",
                            weight = entry.weight,
                            isLocked = isLocked,
                            onClick = { onEntryClick(entry, globalIndex) },
                            numberFormat = numberFormat,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BagCell(
    indexLabel: String,
    weight: Double,
    isLocked: Boolean,
    onClick: () -> Unit,
    numberFormat: NumberFormat? = null,
    modifier: Modifier = Modifier
) {
    val weightText = formatWeight(weight, numberFormat)
    val cellContentDescription = stringResource(
        R.string.detail_bag_entries_semantics,
        indexLabel,
        weightText
    ) + if (!isLocked) {
        stringResource(R.string.detail_bag_entries_semantics_delete_hint)
    } else {
        ""
    }

    val interactiveModifier = if (!isLocked) {
        Modifier.clickable(
            role = Role.Button,
            onClickLabel = stringResource(R.string.detail_bag_entries_delete_action),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            // Đảm bảo touch target ≥48dp (chuẩn Material/A11y) ngay cả khi grid
            // 5 cột chia đều màn nhỏ.
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.GreenSurface)
            .border(1.dp, AppColors.GreenPrimary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .semantics(mergeDescendants = true) {
                contentDescription = cellContentDescription
            }
            .then(interactiveModifier)
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
 * Action sheet cho bag cell → hiển thị thông tin và tuỳ chọn Xoá.
 * Bao gồm xác nhận xóa trực quan ngay trong sheet để phòng tránh mất dữ liệu
 * khi hệ thống chưa hỗ trợ cơ chế Undo rollback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BagEntryActionSheet(
    entry: WeightEntry,
    globalIndex: Int,
    numberFormat: NumberFormat? = null,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val formattedWeight = formatWeight(entry.weight, numberFormat)
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
                        stringResource(R.string.detail_bag_entries_sheet_title, globalIndex),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        stringResource(R.string.weight_format_kg_lower, formattedWeight),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
            }

            HorizontalDivider(color = AppColors.Divider)

            if (!confirmDelete) {
                // Action: Delete trigger
                Surface(
                    onClick = { confirmDelete = true },
                    shape = RoundedCornerShape(12.dp),
                    color = AppColors.Error.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = AppColors.Error
                        )
                        Text(
                            stringResource(R.string.detail_bag_entries_delete_action),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Error
                        )
                    }
                }
            } else {
                // Inline confirmation: Giữ xác nhận rõ ràng vì hệ thống không có Undo rollback
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.SurfaceContainer, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.detail_bag_entries_delete_message,
                            formattedWeight
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { confirmDelete = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        Button(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                stringResource(R.string.card_detail_delete_confirm),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
