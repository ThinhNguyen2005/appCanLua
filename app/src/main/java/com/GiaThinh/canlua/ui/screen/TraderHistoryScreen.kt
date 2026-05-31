package com.GiaThinh.canlua.ui.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Card as M3Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.data.model.TraderHistoryItem
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.viewmodel.ProfileViewModel
import com.GiaThinh.canlua.util.TrackScreenRender
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Màn lịch sử đầy đủ tất cả thương lái đã từng giao dịch với farmer.
 *
 * Tách khỏi `FarmerProfileScreen` (chỉ hiện top 5) để khi user có 50+ thương lái,
 * Profile vẫn ngắn gọn — đầy đủ list nằm ở route riêng.
 *
 * Reuse `ProfileViewModel.traderHistory` (đã aggregate sẵn từ CardDao) nên
 * không cần thêm query mới.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraderHistoryScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    TrackScreenRender("trader_history")
    val history by profileViewModel.traderHistory.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Lịch sử thương lái",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Surface,
                    titleContentColor = AppColors.TextPrimary
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        containerColor = AppColors.Surface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Surface)
                .padding(padding)
        ) {
            if (history.isEmpty()) {
                EmptyHistoryFull()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SummaryHeader(count = history.size)
                    }
                    items(history, key = { "${it.traderName}-${it.traderPhone}" }) { item ->
                        TraderHistoryRowFull(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryHeader(count: Int) {
    M3Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.GreenSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppColors.GreenPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.History,
                    null,
                    tint = AppColors.CardBg,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    text = "$count thương lái đã từng mua",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = "Sắp xếp theo lần giao dịch gần nhất",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint
                )
            }
        }
    }
}

@Composable
private fun TraderHistoryRowFull(item: TraderHistoryItem) {
    val context = LocalContext.current
    val moneyFmt = remember { NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")) }
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")) }

    var showContactOptions by remember { mutableStateOf(false) }

    if (showContactOptions) {
        AlertDialog(
            onDismissRequest = { showContactOptions = false },
            title = {
                Text(
                    text = "Liên hệ thương lái",
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            },
            text = {
                Text(
                    text = "Chọn phương thức liên hệ với ${item.traderName} (${item.traderPhone})",
                    color = AppColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showContactOptions = false
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.traderPhone}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Không thể thực hiện cuộc gọi", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Gọi điện", color = AppColors.GreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showContactOptions = false
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://zalo.me/${item.traderPhone}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Không thể mở Zalo", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Nhắn Zalo", color = AppColors.GreenPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    M3Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppColors.GreenSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.traderName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.GreenPrimary
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    item.traderName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                if (item.traderPhone.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showContactOptions = true }
                            .padding(vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Filled.Phone, null,
                            tint = AppColors.GreenPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            item.traderPhone,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AppColors.GreenPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                if (!item.traderCccd.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            Icons.Filled.Person, null,
                            tint = AppColors.TextHint,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "CCCD: ${item.traderCccd}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextHint
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.MonetizationOn, null,
                        tint = AppColors.GoldDark,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "${moneyFmt.format(item.totalRevenue.toLong())} đ",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("•", color = AppColors.TextHint)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "${item.deals} phiên",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CalendarToday, null,
                        tint = AppColors.TextHint,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "Lần cuối ${dateFmt.format(Date(item.lastDealDate))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextHint
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryFull() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Storefront, null,
            tint = AppColors.TextHint,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Chưa có thương lái nào",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextSecondary
        )
        Text(
            "Mỗi khi tạo phiếu cân, tên thương lái sẽ tự động được lưu vào đây.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextHint
        )
    }
}
