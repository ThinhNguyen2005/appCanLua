package com.giathinh.canlua.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Scale
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.giathinh.canlua.util.HapticUtil
import com.giathinh.canlua.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.data.model.Card as CardModel
import com.giathinh.canlua.ui.theme.AppColors
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import com.giathinh.canlua.util.RiceCalculator

// Singleton formatter — chia sẻ giữa tất cả CardItem instances.
// Trước đây mỗi CardItem có remember riêng → 200 cards = 400 formatter instances (~600KB).
// SimpleDateFormat KHÔNG thread-safe nhưng CardItem chỉ format trên UI thread → an toàn.
private val VI_LOCALE: Locale = Locale.forLanguageTag("vi-VN")
private val NUMBER_FMT: NumberFormat = NumberFormat.getNumberInstance(VI_LOCALE)
private val DATE_FMT: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy", VI_LOCALE)

/**
 * Card item cho danh sách phiếu cân — hiển thị thông tin tóm tắt.
 * Hỗ trợ swipe-to-delete và click để xem chi tiết.
 *
 * Lambda nhận tham số (id / card) thay vì capture trực tiếp để parent có thể
 * remember 1 instance dùng chung cho toàn bộ items{} — tránh tạo 200 closure mới
 * mỗi khi danh sách recompose (sync/filter).
 */
@Composable
fun CardItem(
    card: CardModel,
    onClick: (Long) -> Unit,
    onDelete: (CardModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val displayBagWeight = totalDisplayBagWeight(card)

    @Suppress("DEPRECATION")
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && !card.isLocked && card.isPaid) {
                HapticUtil.error(context)
                onDelete(card)
                false
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
                .clickable {
                    HapticUtil.tick(context)
                    onClick(card.id)
                },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (card.isLocked) AppColors.LockedBg
                else if (card.isPaid) AppColors.GreenSurface
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
                                contentDescription = stringResource(R.string.card_item_locked_content),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = DATE_FMT.format(card.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "Xem chi tiết",
                            tint = AppColors.TextHint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Rice variety + moisture + bag + impurity row — wrap nếu nhiều chip
                if (card.riceVariety.isNotBlank() || card.moisturePercent > 0 ||
                    displayBagWeight > 0 || card.impurityWeight > 0) {
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (card.riceVariety.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Grass,
                                    contentDescription = stringResource(R.string.detail_info_rice_variety),
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
                                    contentDescription = "Độ ẩm",
                                    modifier = Modifier.size(14.dp),
                                    tint = AppColors.Info
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(
                                        R.string.card_item_moisture,
                                        "%.1f".format(card.moisturePercent)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.Info
                                )
                            }
                        }
                        if (displayBagWeight > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Inventory2,
                                    contentDescription = "Trọng lượng bao bì",
                                    modifier = Modifier.size(14.dp),
                                    tint = AppColors.TextSecondary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(
                                        R.string.card_item_bag_weight,
                                        "%.1f".format(displayBagWeight)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextSecondary
                                )
                            }
                        }
                        if (card.impurityWeight > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Scale,
                                    contentDescription = "Tỷ lệ tạp chất",
                                    modifier = Modifier.size(14.dp),
                                    tint = AppColors.TextSecondary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(
                                        R.string.card_item_impurity,
                                        "%.1f".format(card.impurityWeight)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextSecondary
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
                            text = stringResource(R.string.card_list_weight_kg, NUMBER_FMT.format(card.totalWeight)),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (card.bagCount > 0) {
                            Text(
                                text = stringResource(R.string.card_item_weigh_count, card.bagCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.card_list_money_vnd, NUMBER_FMT.format(card.totalAmount.toLong())),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.GreenPrimary
                        )
                        if (card.remainingAmount > 0) {
                            Text(
                                text = stringResource(
                                    R.string.card_item_remaining_amount,
                                    NUMBER_FMT.format(card.remainingAmount.toLong())
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.Warning
                            )
                        }
                    }
                }

                // Paid-in-full tag
                if (card.isPaid) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (card.isPaid) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppColors.GreenPrimary)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.card_item_paid_in_full),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Locked hoặc chưa thanh toán → render trực tiếp, không có swipe-to-delete.
    // Tránh backgroundContent đỏ hắt qua các cạnh khi user vô tình kéo nhẹ.
    if (card.isLocked || !card.isPaid) {
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
                        contentDescription = stringResource(R.string.card_item_delete_content),
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

private fun totalDisplayBagWeight(card: CardModel): Double {
    if (card.bagCount <= 0) {
        return if (card.bagMethodIsSampling && card.bagSampleCount > 0) {
            card.bagSampleTotalWeight
        } else {
            card.bagWeight
        }
    }

    return RiceCalculator.calcTotalBagWeight(
        bagCount = card.bagCount,
        bagWeight = card.bagWeight,
        methodIsSampling = card.bagMethodIsSampling,
        sampleCount = card.bagSampleCount,
        sampleTotalWeight = card.bagSampleTotalWeight
    )
}

