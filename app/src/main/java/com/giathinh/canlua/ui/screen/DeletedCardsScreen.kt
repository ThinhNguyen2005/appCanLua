package com.giathinh.canlua.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card as M3Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.giathinh.canlua.data.model.DeletedCard
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.viewmodel.DeletedCardsViewModel
import com.giathinh.canlua.util.TrackScreenRender
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lịch sử phiếu cân đã xoá — hiện tombstones theo ownerUid.
 *
 * Mỗi row có 2 action:
 *  - **Khôi phục**: recreate card với firestoreId = null, lastModifiedMs = now
 *    → push lên cloud thành doc mới + remove tombstone. Navigate đến card_detail.
 *  - **Xoá vĩnh viễn**: purge tombstone (cloud đã xoá ở delete sync gốc).
 */
import com.giathinh.canlua.ui.component.TransitionSafeWrapper
import com.giathinh.canlua.ui.component.DefaultSkeleton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletedCardsScreen(
    navController: NavController
) {
    TrackScreenRender("deleted_cards")
    val viewModel: DeletedCardsViewModel = hiltViewModel()
    TransitionSafeWrapper(
        isDataReady = true, // Màn hình local DB cực nhẹ, render tức thời mang lại cảm giác mượt mà
        skeletonContent = { DefaultSkeleton() }
    ) {
        DeletedCardsScreenContent(
            navController = navController,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletedCardsScreenContent(
    navController: NavController,
    viewModel: DeletedCardsViewModel
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val restored by viewModel.restored.collectAsStateWithLifecycle()
    var pendingPurge by remember { mutableStateOf<DeletedCard?>(null) }

    // Sau khi restore thành công → navigate vào card detail.
    LaunchedEffect(restored) {
        restored?.let { newId ->
            viewModel.clearRestoredEvent()
            navController.navigate("card_detail/$newId") {
                popUpTo("deleted_cards") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Phiếu đã xoá",
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
                )
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
            if (items.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "${items.size} phiếu đã xoá. Khôi phục hoặc xoá vĩnh viễn để giải phóng dung lượng.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextHint
                        )
                    }
                    items(items, key = { it.id }) { tomb ->
                        DeletedCardRow(
                            tomb = tomb,
                            onRestore = { viewModel.restore(tomb.id) },
                            onPurge = { pendingPurge = tomb }
                        )
                    }
                }
            }
        }
    }

    pendingPurge?.let { tomb ->
        AlertDialog(
            onDismissRequest = { pendingPurge = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.DeleteForever,
                        contentDescription = null,
                        tint = AppColors.Error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Xoá vĩnh viễn?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Phiếu \"${tomb.name.ifBlank { "(không tên)" }}\" sẽ bị xoá hoàn toàn khỏi lịch sử. " +
                        "Hành động này KHÔNG thể hoàn tác."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.purge(tomb.id)
                        pendingPurge = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Error)
                ) { Text("Xoá vĩnh viễn", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingPurge = null }) { Text("Huỷ") }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = AppColors.Surface
        )
    }
}

@Composable
private fun DeletedCardRow(
    tomb: DeletedCard,
    onRestore: () -> Unit,
    onPurge: () -> Unit
) {
    val moneyFmt = remember { NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")) }
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")) }

    M3Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AppColors.Error.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = AppColors.Error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tomb.name.ifBlank { "(không tên)" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "Phiếu ngày ${dateFmt.format(Date(tomb.cardDate))}" +
                            if (tomb.seasonLabel.isNotBlank()) " · ${tomb.seasonLabel}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextHint
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${moneyFmt.format(tomb.totalWeight.toLong())} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
                Text(
                    text = "${moneyFmt.format(tomb.totalAmount.toLong())} đ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.GreenPrimary
                )
            }

            Text(
                text = "Đã xoá lúc ${dateFmt.format(Date(tomb.deletedAt))}",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextHint
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPurge,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Error)
                ) {
                    Icon(Icons.Outlined.DeleteForever, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Xoá vĩnh viễn")
                }
                Button(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary)
                ) {
                    Icon(Icons.Filled.Restore, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Khôi phục", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Delete,
            contentDescription = null,
            tint = AppColors.TextHint,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Chưa có phiếu nào bị xoá",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextSecondary
        )
        Text(
            text = "Phiếu bạn xoá sẽ được lưu lại đây để có thể khôi phục.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextHint
        )
    }
}
