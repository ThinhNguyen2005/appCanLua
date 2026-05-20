package com.GiaThinh.canlua.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.data.model.Card as CardModel
import com.GiaThinh.canlua.ui.theme.AppColors
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Card item cho danh sách phiếu cân — hiển thị thông tin tóm tắt.
 * Hỗ trợ swipe-to-delete và click để xem chi tiết.
 */
@Composable
fun CardItem(
    card: CardModel,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && !card.isLocked) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    // Card content tách riêng để tái sử dụng cho 2 nhánh (locked / unlocked).
    // Locked → render thẳng Card không qua SwipeToDismissBox để tránh
    // backgroundContent màu đỏ hắt qua các cạnh (gây "shadow leak" 3 góc).
    val cardContent = @Composable {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (card.isLocked) 0.dp else 2.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (card.isLocked) AppColors.LockedBg
                else AppColors.CardBg
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header row: Name + Lock icon + Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (card.isLocked) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Đã khoá",
                                tint = AppColors.LockedText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = card.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Text(
                        text = dateFormat.format(card.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }

                // Rice variety + moisture row
                if (card.riceVariety.isNotBlank() || card.moisturePercent > 0) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (card.riceVariety.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Grass,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = AppColors.GreenPrimary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = card.riceVariety,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.GreenPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (card.moisturePercent > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.WaterDrop,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = AppColors.Info
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Ẩm ${"%.1f".format(card.moisturePercent)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.Info
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Weight + Amount row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "${numberFormat.format(card.totalWeight)} kg",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (card.bagCount > 0) {
                            Text(
                                text = "${card.bagCount} lần cân",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${numberFormat.format(card.totalAmount)} đ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.GreenPrimary
                        )
                        if (card.remainingAmount > 0) {
                            Text(
                                text = "Còn: ${numberFormat.format(card.remainingAmount)} đ",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.Warning
                            )
                        }
                    }
                }

                // QR verification status
                if (card.qrToken != null && card.isLocked) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.GreenSurface)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "✓ QR đã xác thực",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.Success,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Locked → render trực tiếp, không có swipe-to-delete (vì swipe đã disabled).
    // Tránh backgroundContent đỏ hắt qua các cạnh khi user vô tình kéo nhẹ.
    if (card.isLocked) {
        Box(modifier = modifier) { cardContent() }
    } else {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppColors.Error.copy(alpha = 0.9f))
                        .padding(end = 24.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Xoá",
                        tint = AppColors.CardBg,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
            modifier = modifier
        ) {
            cardContent()
        }
    }
}
