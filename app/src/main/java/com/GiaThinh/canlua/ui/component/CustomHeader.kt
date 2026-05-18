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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
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
private val META_CHIP_H   = 120.dp  // MetaRow + ChipRow + paddings + Divider
private val BOTTOM_PAD    = 12.dp

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
    // Expanded content mờ dần nhanh ở nửa đầu scroll
    val expandedAlpha = (1f - collapseFraction * 2.2f).coerceIn(0f, 1f)

    // Title khi collapse > 60%: [Tên Nông Dân] — [Tổng Số KG] KG — [Thành Tiền] đ
    val farmerName = card.name
    val titleText = if (collapseFraction > 0.6f) {
        val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN"))
        val totalWeightFormatted = if (card.totalWeight % 1.0 == 0.0) "%.0f".format(card.totalWeight) else "%.1f".format(card.totalWeight)
        val totalAmountFormatted = fmt.format(card.totalAmount.toLong())
        "$farmerName — $totalWeightFormatted KG — $totalAmountFormatted đ"
    } else {
        farmerName
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
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Nút Khóa Màn Hình
                IconButton(
                    onClick = onToggleLock,
                    modifier = Modifier.size(48.dp)
                ) {
                    if (card.isLocked) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Mở khóa",
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.LockOpen,
                            contentDescription = "Khóa",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Menu ⋮
                Box {
                    IconButton(
                        onClick = { onOverflowChange(!showOverflow) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, "Menu", tint = Color.White,
                            modifier = Modifier.size(24.dp))
                    }
                    DropdownMenu(
                        expanded = showOverflow,
                        onDismissRequest = { onOverflowChange(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sửa phiếu", fontSize = 15.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, null, tint = HeaderGreen)
                            },
                            onClick = { onOverflowChange(false); onEditCard() }
                        )
                        DropdownMenuItem(
                            text = { Text("Xóa phiếu", fontSize = 15.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, null, tint = Color.Red)
                            },
                            onClick = { onOverflowChange(false); onDeleteCard() }
                        )
                        DropdownMenuItem(
                            text = { Text("Tạo mã QR", fontSize = 15.sp) },
                            leadingIcon = {
                                Icon(Icons.Outlined.QrCode2, null, tint = HeaderGreen)
                            },
                            onClick = { onOverflowChange(false); onCreateQr() }
                        )
                        DropdownMenuItem(
                            text = { Text("Quét QR (Thương lái)", fontSize = 15.sp) },
                            leadingIcon = {
                                Icon(Icons.Outlined.CameraAlt, null, tint = HeaderGreen)
                            },
                            onClick = { onOverflowChange(false); onScanQr() }
                        )
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

                    // ── 3 metric chips ────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricChip("Tổng K/L", formatKg(card.totalWeight), Modifier.weight(1f))
                        MetricChip("Số bao", "${card.bagCount} bao",       Modifier.weight(1f))
                        MetricChip(
                            label = "Đơn giá",
                            value = if (card.pricePerKg > 0.0)
                                formatMoney(card.pricePerKg) + " đ" else "Chưa có",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = Color.White.copy(alpha = 0.25f)
                    )

                    // Bottom padding — tránh sát mép card bên dưới
                    Spacer(Modifier.height(BOTTOM_PAD))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Nút "Cân lúa" — thay thế nút "Chi tiết" trong CardDetailScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WeighingButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        border = BorderStroke(1.5.dp, HeaderGreen.copy(alpha = 0.7f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = HeaderGreen),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
    ) {
        Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text("Cân lúa", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

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
        Icon(icon, null, tint = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.size(15.dp))
        Text(
            text  = label,
            fontSize  = 10.sp,           // tăng từ 9 → 10
            color = Color.White.copy(alpha = 0.6f),
            lineHeight = 11.sp
        )
        Text(
            text  = value,
            fontSize  = 13.sp,           // tăng từ 11 → 13
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp
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
        Text(
            text  = value,
            fontSize  = 14.sp,           // tăng từ 12 → 14
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text  = label,
            fontSize  = 11.sp,           // tăng từ 9 → 11
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
    else "${NumberFormat.getNumberInstance(Locale("vi")).format(kg.toInt())} KG"

private fun formatMoney(amount: Double): String =
    NumberFormat.getNumberInstance(Locale("vi"))
        .format(amount.toLong()).replace(',', '.')
