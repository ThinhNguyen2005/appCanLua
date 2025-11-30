package com.GiaThinh.canlua.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CardItem(
    card: Card,
    onClick: () -> Unit
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with name and lock status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (card.cccd != null) {
                        Text(
                            text = "CCCD: ${card.cccd}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (card.isLocked) 
                        MaterialTheme.colorScheme.errorContainer 
                    else 
                        MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (card.isLocked) Icons.Default.Lock else Icons.Default.Check,
                            contentDescription = if (card.isLocked) "Đã khóa" else "Mở khóa",
                            tint = if (card.isLocked) 
                                MaterialTheme.colorScheme.onErrorContainer 
                            else 
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Date
            Text(
                text = "📅 ${dateFormat.format(card.date)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left column - Weight info
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InfoRow(
                        label = "Tổng KL",
                        value = "${numberFormat.format(card.totalWeight)} kg",
                        isHighlight = false
                    )
                    InfoRow(
                        label = "Số bao",
                        value = "${card.bagCount}",
                        isHighlight = false
                    )
                }
                
                // Right column - Money info
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InfoRow(
                        label = "Thành tiền",
                        value = "${numberFormat.format(card.totalAmount)} đ",
                        isHighlight = true
                    )
                    InfoRow(
                        label = "Còn lại",
                        value = "${numberFormat.format(card.remainingAmount)} đ",
                        isHighlight = true,
                        isError = card.remainingAmount > 0,
                        isSuccess = card.remainingAmount <= 0
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    isHighlight: Boolean,
    isError: Boolean = false,
    isSuccess: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = GrayLabel
        )
        Text(
            text = value,
            style = if (isHighlight) 
                MaterialTheme.typography.titleMedium 
            else 
                MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isError -> RedText
                isSuccess -> GreenSuccess
                isHighlight -> MaterialTheme.colorScheme.primary
                else -> BlackText
            }
        )
    }
}
