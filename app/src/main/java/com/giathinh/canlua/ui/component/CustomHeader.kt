package com.giathinh.canlua.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.R
import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.ui.theme.AppColors
import java.text.NumberFormat
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// CustomHeader
//
// TopAppBar chuyên biệt cho CardDetailScreen.
// Cung cấp nút Back, tiêu đề tóm tắt khối lượng/số bao, nút Sửa, Khóa/Mở khóa,
// và Menu tùy chọn (Xóa, Xuất PDF).
// ─────────────────────────────────────────────────────────────────────────────

private val TOPBAR_H = 56.dp

/** Trả về expanded/collapsed height cho TopBar cố định 56dp */
data class HeaderHeights(val expanded: Dp, val collapsed: Dp)

@Composable
fun rememberHeaderHeights(): HeaderHeights {
    return remember {
        HeaderHeights(
            expanded = TOPBAR_H,
            collapsed = TOPBAR_H
        )
    }
}

@Composable
fun CustomHeader(
    card: Card,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    showOverflow: Boolean = false,
    onOverflowChange: (Boolean) -> Unit = {},
    onEditCard: () -> Unit = {},
    onDeleteCard: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onToggleLock: () -> Unit = {},
    collapseFraction: Float = 0f,
    lastEntryTime: Long? = null,
    onAdd: () -> Unit = {}
) {
    // Title: luôn hiển thị tóm tắt khối lượng và số bao
    val titleText = if (card.totalWeight > 0.0 || card.bagCount > 0) {
        val fmt = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")) }
        val weightStr = if (card.totalWeight % 1.0 == 0.0) {
            "%.0f".format(Locale.US, card.totalWeight)
        } else {
            "%.1f".format(Locale.US, card.totalWeight)
        }
        val bagsStr = fmt.format(card.bagCount)
        "$weightStr KG · $bagsStr bao"
    } else {
        stringResource(R.string.header_default_title)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.GreenPrimary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TOPBAR_H)
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Nút Back + Tiêu đề
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.content_back),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Cụm action bên phải: [✏️ Sửa] [🔒 Khóa/Mở khóa] [⋮ Menu]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onEditCard,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.header_edit_card_content),
                        tint = Color.White.copy(alpha = if (card.isLocked) 0.5f else 0.95f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onToggleLock,
                    modifier = Modifier.size(48.dp)
                ) {
                    if (card.isLocked) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = stringResource(R.string.header_unlock_card_content),
                            tint = AppColors.GoldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.LockOpen,
                            contentDescription = stringResource(R.string.header_lock_card_content),
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Menu ⋮ (Xóa phiếu, Xuất PDF)
                Box {
                    IconButton(
                        onClick = { onOverflowChange(!showOverflow) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.header_menu_content),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showOverflow,
                        onDismissRequest = { onOverflowChange(false) },
                        offset = DpOffset(x = (-4).dp, y = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        containerColor = AppColors.SurfaceContainer,
                        shadowElevation = 8.dp,
                        tonalElevation = 4.dp
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.header_delete_card),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = AppColors.Error
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, null, tint = AppColors.Error)
                            },
                            onClick = { onOverflowChange(false); onDeleteCard() }
                        )
                        HorizontalDivider(
                            color = AppColors.Divider,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.header_export_pdf),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = AppColors.TextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.PictureAsPdf, null, tint = AppColors.GreenPrimary)
                            },
                            onClick = { onOverflowChange(false); onExportPdf() }
                        )
                    }
                }
            }
        }
    }
}


