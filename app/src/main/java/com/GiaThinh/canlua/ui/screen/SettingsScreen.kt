package com.GiaThinh.canlua.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.GiaThinh.canlua.data.model.FontScale
import com.GiaThinh.canlua.repository.BackupStatus
import com.GiaThinh.canlua.repository.SyncStatus
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.viewmodel.AuthViewModel
import com.GiaThinh.canlua.ui.viewmodel.SettingsViewModel
import com.GiaThinh.canlua.ui.viewmodel.SyncViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SettingsScreen — đồng bộ màu sắc với toàn app qua AppColors design tokens.
 *
 * Mapping:
 *  - Background  → AppColors.Surface
 *  - Card BG     → AppColors.CardBg
 *  - Primary     → AppColors.GreenPrimary (button, switch, radio)
 *  - Text        → AppColors.TextPrimary / TextSecondary / TextHint
 *  - Icon tile   → tint-tinted background (GreenSurface, GoldLight, ...) tùy chức năng
 *  - Danger      → AppColors.Error (đăng xuất)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val syncViewModel: SyncViewModel = hiltViewModel()
    val syncStatus by syncViewModel.syncStatus.collectAsState()
    val lastSyncTime by syncViewModel.lastSyncTime.collectAsState()
    val backupStatus by syncViewModel.backupStatus.collectAsState()
    val lastBackupTime by syncViewModel.lastBackupTime.collectAsState()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Cài đặt",
                        style = MaterialTheme.typography.headlineMedium,
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
                // FIX LỖI 1: Tắt tự động thêm status bar padding cho TopAppBar này,
                // vì Scaffold bên ngoài (MainScreen) đã thêm WindowInsets rồi.
                windowInsets = WindowInsets(0.dp)
            )
        },
        containerColor = AppColors.Surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Surface)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── TTS Card ──
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            IconTile(
                                bg = AppColors.GreenSurface,
                                tint = AppColors.GreenPrimary,
                                icon = { tint, mod ->
                                    Icon(
                                        Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = tint,
                                        modifier = mod
                                    )
                                }
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Đọc số khi nhập",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                                Text(
                                    text = "Bật/tắt tính năng đọc số khi nhập cân",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextHint,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        Switch(
                            checked = isTtsEnabled,
                            onCheckedChange = { viewModel.setTtsEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppColors.GreenPrimary,
                                uncheckedThumbColor = AppColors.TextHint,
                                uncheckedTrackColor = AppColors.SurfaceContainer
                            )
                        )
                    }
                }

                // ── Sync & Backup Card ──
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            IconTile(
                                bg = AppColors.Info.copy(alpha = 0.12f),
                                tint = AppColors.Info,
                                icon = { tint, mod ->
                                    Icon(
                                        Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = tint,
                                        modifier = mod
                                    )
                                }
                            )
                            Column {
                                Text(
                                    text = "Đồng bộ & Backup",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                                Text(
                                    text = syncStatusText(syncStatus),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextHint
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusRow("Lần đồng bộ cuối", lastSyncTime)
                            StatusRow("Lần backup cuối", lastBackupTime)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { syncViewModel.backupNow() },
                                modifier = Modifier.wrapContentWidth(),
                                enabled = backupStatus !is BackupStatus.BackingUp,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AppColors.GreenPrimary
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(AppColors.GreenPrimary)
                                )
                            ) {
                                if (backupStatus is BackupStatus.BackingUp) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = AppColors.GreenPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(
                                        Icons.Default.Backup,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("Backup", fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = { syncViewModel.syncAll() },
                                modifier = Modifier.wrapContentWidth(),
                                enabled = syncStatus !is SyncStatus.Syncing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.GreenPrimary,
                                    contentColor = Color.White,
                                    disabledContainerColor = AppColors.GreenPrimary.copy(alpha = 0.4f),
                                    disabledContentColor = Color.White
                                )
                            ) {
                                if (syncStatus is SyncStatus.Syncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(
                                        Icons.Default.CloudSync,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("Đồng bộ", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        TextButton(
                            onClick = { navController.navigate("sync_status") },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = AppColors.GreenPrimary
                            )
                        ) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Xem chi tiết trạng thái")
                        }
                    }
                }

                // ── Font scale Card ──
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Kích thước chữ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = "Chọn mức chữ dễ đọc cho toàn bộ ứng dụng",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextHint
                        )

                        FontScaleOptions(
                            selected = fontScale,
                            onSelect = { viewModel.setFontScale(it) }
                        )
                    }
                }

                // ── Account Card ──
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tài khoản",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = authState.userLabel ?: "Chưa đăng nhập",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextHint
                        )

                        Button(
                            onClick = { authViewModel.signOut() },
                            enabled = authState.isSignedIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Error.copy(alpha = 0.1f),
                                contentColor = AppColors.Error,
                                disabledContainerColor = AppColors.SurfaceContainer,
                                disabledContentColor = AppColors.TextHint
                            )
                        ) {
                            Text("Đăng xuất", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Reusable bits
// ─────────────────────────────────────────────────────────────

/** Card chuẩn cho settings — đồng nhất shape, color, elevation với app. */
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        content()
    }
}

/** Avatar-style icon tile thay cho `Surface` cũ — giúp icon nổi mà không cần Material container colors. */
@Composable
private fun IconTile(
    bg: Color,
    tint: Color,
    icon: @Composable (tint: Color, modifier: Modifier) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon(tint, Modifier.size(24.dp))
        }
    }
}

@Composable
private fun FontScaleOptions(
    selected: FontScale,
    onSelect: (FontScale) -> Unit
) {
    val options = listOf(FontScale.SMALL, FontScale.NORMAL, FontScale.LARGE, FontScale.XLARGE)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { scale ->
            val isSelected = scale == selected
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) AppColors.GreenSurface else AppColors.SurfaceContainer,
                onClick = { onSelect(scale) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = scale.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) AppColors.GreenDark else AppColors.TextPrimary
                    )
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelect(scale) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = AppColors.GreenPrimary,
                            unselectedColor = AppColors.TextHint
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, time: Long?) {
    val formatter = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }
    val value = time?.let { formatter.format(Date(it)) } ?: "Chưa có"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextHint
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextSecondary
        )
    }
}

private fun syncStatusText(status: SyncStatus): String = when (status) {
    is SyncStatus.Syncing -> "Đang đồng bộ..."
    is SyncStatus.Success -> "Đồng bộ thành công"
    is SyncStatus.Error -> "Lỗi đồng bộ: ${status.message}"
    SyncStatus.Idle -> "Chưa đồng bộ"
}
