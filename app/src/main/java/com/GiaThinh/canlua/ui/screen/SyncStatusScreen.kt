package com.GiaThinh.canlua.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.GiaThinh.canlua.repository.SyncStatus
import com.GiaThinh.canlua.ui.viewmodel.SyncViewModel
import java.text.SimpleDateFormat
import java.util.*

import com.GiaThinh.canlua.util.TrackScreenRender
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusScreen(
    navController: NavController,
    viewModel: SyncViewModel = hiltViewModel()
) {
    TrackScreenRender("sync_status")
    val syncStatus by viewModel.syncStatus.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đồng bộ dữ liệu") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (syncStatus) {
                        is SyncStatus.Success -> MaterialTheme.colorScheme.primaryContainer
                        is SyncStatus.Error -> MaterialTheme.colorScheme.errorContainer
                        is SyncStatus.Syncing -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = when (syncStatus) {
                            is SyncStatus.Success -> Icons.Default.CloudDone
                            is SyncStatus.Error -> Icons.Default.Error
                            is SyncStatus.Syncing -> Icons.Default.CloudSync
                            else -> Icons.Default.CloudOff
                        },
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = when (syncStatus) {
                            is SyncStatus.Success -> MaterialTheme.colorScheme.onPrimaryContainer
                            is SyncStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
                            is SyncStatus.Syncing -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Text(
                        text = when (syncStatus) {
                            is SyncStatus.Success -> "Đồng bộ thành công"
                            is SyncStatus.Error -> "Lỗi đồng bộ"
                            is SyncStatus.Syncing -> "Đang đồng bộ..."
                            else -> "Chưa đồng bộ"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )

                    if (syncStatus is SyncStatus.Error) {
                        Text(
                            text = (syncStatus as SyncStatus.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (lastSyncTime != null) {
                        val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
                        Text(
                            text = "Lần đồng bộ cuối: ${dateFormat.format(Date(lastSyncTime!!))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Sync Button
            Button(
                onClick = { viewModel.syncAll() },
                modifier = Modifier.fillMaxWidth(),
                enabled = syncStatus !is SyncStatus.Syncing
            ) {
                if (syncStatus is SyncStatus.Syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Đồng bộ ngay")
            }
            
            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Thông tin đồng bộ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Đồng bộ tất cả thẻ, cân nặng và giao dịch",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Yêu cầu kết nối internet",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Dữ liệu được lưu trên Firebase",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

