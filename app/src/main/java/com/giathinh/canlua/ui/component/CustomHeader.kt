package com.giathinh.canlua.ui.component

import com.giathinh.canlua.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.ui.theme.AppColors
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.giathinh.canlua.data.model.Card
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalDensity

// ─────────────────────────────────────────────────────────────────────────────
// CustomHeader v4
//
// Root cause v3 bị mất header khi scroll:
//   statusBarsPadding() ≈ 28dp ăn vào HEADER_COLLAPSED=52dp
//   → còn 24dp cho TopBar(height=52dp) → bị clip hoàn toàn
//
// Fix: CustomHeader tự tính statusBarHeight bằng WindowInsets,
//   expose ra ngoài qua rememberHeaderHeights() để CardDetailScreen
//   dùng đúng HEADER_COLLAPSED = TOPBAR_HEIGHT + statusBarHeight
//
// Cách dùng trong CardDetailScreen:
//   val heights = rememberHeaderHeights()
//   val headerHeight = lerp(heights.expanded, heights.collapsed, collapseFraction.coerceIn(0f,1f))
//   CustomHeader(
//       modifier = Modifier.height(headerHeight),
//       collapseFraction = collapseFraction.coerceIn(0f, 1f),
//       ...
//   )
// ─────────────────────────────────────────────────────────────────────────────

private val HeaderGreen   = Color(0xFF2E7D32)   // Green 800 — đậm hơn, sang hơn 3D8B40
private val TOPBAR_H      = 56.dp                // 52 → 56: tiêu chuẩn M3 TopAppBar

/** Trả về expanded/collapsed height — cả hai bằng nhau vì header đã phẳng */
data class HeaderHeights(val expanded: Dp, val collapsed: Dp)

@Composable
fun rememberHeaderHeights(): HeaderHeights {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return HeaderHeights(
        expanded  = TOPBAR_H + statusBarHeight,
        collapsed = TOPBAR_H + statusBarHeight
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Main composable

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CustomHeader(
    card: Card,
    collapseFraction: Float,            // PHẢI clamp 0f..1f trước khi truyền vào
    lastEntryTime: Long? = null,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    showOverflow: Boolean = false,
    onOverflowChange: (Boolean) -> Unit = {},
    onEditCard: () -> Unit = {},
    onDeleteCard: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onToggleLock: () -> Unit = {}
) {
    // Title: luôn hiển thị info trọng lượng — đã bỏ farmer name khỏi header
    // Khi card còn trống (mới tạo) → “Phiếu cân”, không để trống hổng.
    val titleText = if (card.totalWeight > 0.0 || card.bagCount > 0) {
        val fmt = remember { java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("vi-VN")) }
        val totalWeightFormatted = if (card.totalWeight % 1.0 == 0.0) "%.0f".format(card.totalWeight) else "%.1f".format(card.totalWeight)
        val totalbagCount = fmt.format(card.bagCount)
        "$totalWeightFormatted KG · $totalbagCount bao"
    } else {
        stringResource(R.string.header_default_title)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(HeaderGreen)
    ) {
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {

            // ── TopBar — LUÔN visible, không bị ảnh hưởng bởi collapseFraction ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TOPBAR_H)
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Nhóm action bên phải: [✏️ Edit] [🔓 Lock] [⋮ Menu]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Sửa phiếu (icon cây bút) — đã chuyển từ dropdown menu ra topbar
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

                    // Nút Khóa / Mở khóa — luôn hiển thị để user mở lại phiếu khi cần.
                    // Khi đã khoá: icon Lock màu vàng cảnh báo gây chú ý "tap để mở".
                    // Khi đang mở: icon LockOpen mờ — gợi ý "tap để khoá khi xong".
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

                    // Menu ⋮
                    Box {
                        IconButton(
                            onClick = { onOverflowChange(!showOverflow) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                stringResource(R.string.header_menu_content),
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
                            tonalElevation = 4.dp,
                            modifier = Modifier
                                .background(AppColors.SurfaceContainer, RoundedCornerShape(14.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.header_delete_card), style = MaterialTheme.typography.bodyLarge, color = AppColors.Error) },
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
                                text = { Text(stringResource(R.string.header_export_pdf), style = MaterialTheme.typography.bodyLarge, color = AppColors.TextPrimary) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.PictureAsPdf, null, tint = HeaderGreen)
                                },
                                onClick = { onOverflowChange(false); onExportPdf() }
                            )
                        }
                    }
                }
            }

            // Header phẳng — farmer name và toàn bộ meta đã chuyển xuống CardInfoCard.
            // Không còn expanded section → header rất gọn, tập trung vào nội dung bên dưới.
        }
    }
}

// (Legacy WeighingButton removed — đã được thay bằng FilledIconButton trên topbar)

// ─────────────────────────────────────────────────────────────────────────────
// Sub-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MetaItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp))                    // 15 → 18 (lớn hơn, dễ thấy)
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                val enter = slideInVertically(
                    animationSpec = tween(150, easing = FastOutLinearInEasing),
                ) { it } + fadeIn(tween(150, easing = FastOutLinearInEasing))
                val exit = slideOutVertically(
                    animationSpec = tween(150, easing = FastOutLinearInEasing),
                ) { -it } + fadeOut(tween(150, easing = FastOutLinearInEasing))
                enter.togetherWith(exit)
            },
            label = "metric_chip_value"
        ) { current ->
            Text(
                text  = current,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

// Cache formatter ở top-level để tránh allocate mỗi lần helper được gọi.
// Lưu ý: SimpleDateFormat KHÔNG thread-safe — chỉ gọi từ Main thread (composable).
private val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
private val TIME_FORMAT_SHORT = SimpleDateFormat("HH:mm", Locale.getDefault())
private val NUMBER_FORMAT_VI = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi"))

private fun formatDate(ts: Long): String = DATE_FORMAT.format(Date(ts))

private fun formatTimeShort(ts: Long): String = TIME_FORMAT_SHORT.format(Date(ts))

private fun formatKg(kg: Double): String =
    if (kg == 0.0) "0 KG"
    else "${NUMBER_FORMAT_VI.format(kg.toInt())} KG"

private fun formatMoney(amount: Double): String =
    NUMBER_FORMAT_VI.format(amount.toLong()).replace(',', '.')
