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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.GiaThinh.canlua.data.model.Card
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
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Icon(
                    imageVector = if (card.isLocked) Icons.Default.Lock else Icons.Default.Check,
                    contentDescription = if (card.isLocked) "Đã khóa" else "Mở khóa",
                    tint = if (card.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            card.cccd?.let { cccd ->
                Text(
                    text = "CCCD: $cccd",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Ngày: ${dateFormat.format(card.date)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tổng KL: ${numberFormat.format(card.totalWeight)} kg",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Số bao: ${card.bagCount}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Thành tiền: ${numberFormat.format(card.totalAmount)} đ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Còn lại: ${numberFormat.format(card.remainingAmount)} đ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (card.remainingAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
