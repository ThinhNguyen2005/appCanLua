package com.GiaThinh.canlua.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.GiaThinh.canlua.ui.theme.AppColors
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.GiaThinh.canlua.data.model.Card
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

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

private val HeaderGreen   = Color(0xFF3D8B40)
private val TOPBAR_H      = 52.dp
private val META_CHIP_H   = 76.dp   // 120 → 76 (chỉ còn 1 hàng MetaItem, MetricChip đã removed)
private val BOTTOM_PAD    = 8.dp    // 12 → 8 (sát hơn)

/** Trả về expanded/collapsed height đã tính statusBar — dùng ở CardDetailScreen */
data class HeaderHeights(val expanded: Dp, val collapsed: Dp)

@Composable
fun rememberHeaderHeights(): HeaderHeights {
    return HeaderHeights(
        expanded  = TOPBAR_H + META_CHIP_H + BOTTOM_PAD,
        collapsed = TOPBAR_H
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
    onCreateQr: () -> Unit = {},
    onScanQr: () -> Unit = {},
    onToggleLock: () -> Unit = {}
) {
    // Expanded content fade mượt — giảm cường độ để tránh chuyển động mạnh
    // Fade chậm và đều: cần scroll ~70% mới ẩn hẳn (multiplier 1.4 thay vì 2.2)
    val expandedAlpha = (1f - collapseFraction * 1.4f).coerceIn(0f, 1f)

    // Title topbar — ẩn tên nông dân khi expanded (đã có ở Meta "Thương lái").
    // Chỉ hiện info compact khi đã collapse > 75%.
    val titleText = if (collapseFraction > 0.75f) {
        val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("vi-VN"))
        val totalWeightFormatted = if (card.totalWeight % 1.0 == 0.0) "%.0f".format(card.totalWeight) else "%.1f".format(card.totalWeight)
        val totalbagCount = fmt.format(card.bagCount)
        "$totalWeightFormatted KG · $totalbagCount bao"
    } else {
        ""  // Ẩn tên nông dân — title trống, đã có Meta row hiển thị bên dưới
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(HeaderGreen)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

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
                            contentDescription = "Quay lại",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = titleText,
                        style = TextStyle(
                            fontSize = 17.sp,                       // 15 → 17 (người lớn tuổi)
                            fontWeight = FontWeight.SemiBold,
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
                            contentDescription = "Chỉnh sửa phiếu",
                            tint = Color.White.copy(alpha = if (card.isLocked) 0.5f else 0.95f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // // Nút Khóa / Mở khóa
                    // IconButton(
                    //     onClick = onToggleLock,
                    //     modifier = Modifier.size(48.dp)
                    // ) {
                    //     if (card.isLocked) {
                    //         Icon(
                    //             Icons.Default.Lock,
                    //             contentDescription = "Mở khóa",
                    //             tint = Color(0xFFFFCDD2),
                    //             modifier = Modifier.size(22.dp)
                    //         )
                    //     } else {
                    //         Icon(
                    //             Icons.Default.LockOpen,
                    //             contentDescription = "Khóa",
                    //             tint = Color.White.copy(alpha = 0.8f),
                    //             modifier = Modifier.size(22.dp)
                    //         )
                    //     }
                    // }

                    // Menu ⋮
                    Box {
                        IconButton(
                            onClick = { onOverflowChange(!showOverflow) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                "Menu",
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
                                text = { Text("Xóa phiếu", fontSize = 15.sp, color = AppColors.Error) },
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
                                text = { Text("Tạo mã QR", fontSize = 15.sp, color = AppColors.TextPrimary) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.QrCode2, null, tint = HeaderGreen)
                                },
                                onClick = { onOverflowChange(false); onCreateQr() }
                            )
                            DropdownMenuItem(
                                text = { Text("Quét QR (Thương lái)", fontSize = 15.sp, color = AppColors.TextPrimary) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.CameraAlt, null, tint = HeaderGreen)
                                },
                                onClick = { onOverflowChange(false); onScanQr() }
                            )
                        }
                    }
                }
            }

            // ── Phần mở rộng — fade + clip khi scroll ────────────────────────
            // Chỉ render khi còn visible (tránh layout cost thừa)
            if (expandedAlpha > 0.01f) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(expandedAlpha)
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    // ── 4 meta items — font lớn hơn ──────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        MetaItem(
                            icon  = Icons.Default.Person,
                            label = "Thương lái",
                            value = card.traderName.ifBlank { "—" },
                            modifier = Modifier.weight(1f)
                        )
                        MetaItem(
                            icon  = Icons.Default.CalendarToday,
                            label = "Ngày tạo",
                            value = formatDate(card.date.time),
                            modifier = Modifier.weight(1.1f)
                        )
                        MetaItem(
                            icon  = Icons.Default.Grass,
                            label = "Giống lúa",
                            value = card.riceVariety.ifBlank { "—" },
                            modifier = Modifier.weight(0.9f)
                        )
                        MetaItem(
                            icon  = Icons.Default.AccessTime,
                            label = "Lần cuối",
                            value = lastEntryTime?.let { formatTimeShort(it) } ?: "—",
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    // // ── 3 metric chips ────────────────────────────────────────
                    // Row(
                    //     modifier = Modifier.fillMaxWidth(),
                    //     horizontalArrangement = Arrangement.spacedBy(8.dp)
                    // ) {
                    //     MetricChip("Tổng K/L", formatKg(card.totalWeight), Modifier.weight(1f))
                    //     MetricChip("Số bao", "${card.bagCount} bao",       Modifier.weight(1f))
                    //     MetricChip(
                    //         label = "Đơn giá",
                    //         value = if (card.pricePerKg > 0.0)
                    //             formatMoney(card.pricePerKg) + " đ" else "Chưa có",
                    //         modifier = Modifier.weight(1f)
                    //     )
                    // }

                    // Spacer(Modifier.height(16.dp))
                    
                    // HorizontalDivider(
                    //     modifier = Modifier
                    //         .fillMaxWidth()
                    //         .padding(horizontal = 16.dp),
                    //     thickness = 1.dp,
                    //     color = Color.White.copy(alpha = 0.25f)
                    // )

                    // Bottom padding — tránh sát mép card bên dưới
                    // Spacer(Modifier.height(BOTTOM_PAD))
                }
            }
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
            fontSize  = 12.sp,           // 10 → 12 (người lớn tuổi đọc rõ)
            color = Color.White.copy(alpha = 0.7f),
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text  = value,
            fontSize  = 15.sp,           // 13 → 15 (chuẩn body lớn)
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp
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
                fontSize  = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text  = label,
            fontSize  = 11.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatDate(ts: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ts))

private fun formatTimeShort(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

private fun formatKg(kg: Double): String =
    if (kg == 0.0) "0 KG"
    else "${NumberFormat.getNumberInstance(Locale.forLanguageTag("vi")).format(kg.toInt())} KG"

private fun formatMoney(amount: Double): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("vi"))
        .format(amount.toLong()).replace(',', '.')
