package com.GiaThinh.canlua.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.data.model.AppLanguage
import com.GiaThinh.canlua.data.model.FontScale
import com.GiaThinh.canlua.repository.BackupStatus
import com.GiaThinh.canlua.repository.SyncStatus
import com.GiaThinh.canlua.ui.screen.profile.RoleSwitcher
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.viewmodel.AuthViewModel
import com.GiaThinh.canlua.ui.viewmodel.ProfileViewModel
import com.GiaThinh.canlua.ui.viewmodel.SettingsViewModel
import com.GiaThinh.canlua.ui.viewmodel.SyncViewModel
import com.GiaThinh.canlua.util.PremiumState
import com.GiaThinh.canlua.util.TrackScreenRender
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
    TrackScreenRender("settings")
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsStateWithLifecycle()
    val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val syncViewModel: SyncViewModel = hiltViewModel()
    val syncStatus by syncViewModel.syncStatus.collectAsStateWithLifecycle()
    val lastSyncTime by syncViewModel.lastSyncTime.collectAsStateWithLifecycle()
    val backupStatus by syncViewModel.backupStatus.collectAsStateWithLifecycle()
    val lastBackupTime by syncViewModel.lastBackupTime.collectAsStateWithLifecycle()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val profile by profileViewModel.profile.collectAsStateWithLifecycle(initialValue = null)
    val premiumInfo by PremiumState.info.collectAsStateWithLifecycle()
    var pendingRole by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.topbar_settings),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_back),
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
                    .imePadding()
                    .navigationBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Premium Card — luôn hiển thị; trạng thái ACTIVE hay UPGRADE
                //    quyết định gradient + nội dung. Tap → mở PremiumScreen.
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
                                bg = AppColors.GoldLight,
                                tint = AppColors.GoldDark,
                                icon = { tint, mod ->
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = tint,
                                        modifier = mod
                                    )
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_premium_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (premiumInfo.isActive) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterVertically)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (premiumInfo.isEarlyAdopter) AppColors.GoldAccent
                                                    else AppColors.GreenPrimary
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (premiumInfo.isEarlyAdopter) {
                                                    stringResource(R.string.settings_premium_early_adopter)
                                                } else {
                                                    stringResource(R.string.settings_premium_active)
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                val activatedLabel = stringResource(R.string.settings_premium_activated)
                                val earlyAdopterLabel = stringResource(R.string.settings_premium_early_adopter)
                                val subtitle = if (premiumInfo.isActive) {
                                    val plan = premiumInfo.plan?.takeIf { it.isNotBlank() }
                                        ?: if (premiumInfo.isEarlyAdopter) earlyAdopterLabel
                                        else activatedLabel
                                    val sinceLabel = if (premiumInfo.sinceMs > 0L) {
                                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                        stringResource(
                                            R.string.settings_premium_since,
                                            sdf.format(Date(premiumInfo.sinceMs))
                                        )
                                    } else ""
                                    "$plan$sinceLabel"
                                } else {
                                    stringResource(R.string.settings_premium_subtitle)
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextHint,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        TextButton(
                            onClick = { navController.navigate("premium") },
                            colors = ButtonDefaults.textButtonColors(contentColor = AppColors.GreenPrimary)
                        ) {
                            Text(
                                text = if (premiumInfo.isActive) {
                                    stringResource(R.string.settings_premium_manage)
                                } else {
                                    stringResource(R.string.settings_premium_upgrade)
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

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
                                    text = stringResource(R.string.settings_tts_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.settings_tts_subtitle),
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
                                    text = stringResource(R.string.settings_sync_backup_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                                SyncStatusText(syncStatus)
                            }
                        }

                        // Switch toggle for Auto-Sync
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tự động đồng bộ ngầm",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                                Text(
                                    text = "Tự động tải/đẩy dữ liệu Firebase ngầm khi mở app hoặc có thay đổi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextHint,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Switch(
                                checked = isAutoSyncEnabled,
                                onCheckedChange = { viewModel.setAutoSyncEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AppColors.GreenPrimary,
                                    uncheckedThumbColor = AppColors.TextHint,
                                    uncheckedTrackColor = AppColors.SurfaceContainer
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = AppColors.SurfaceContainer
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusRow(stringResource(R.string.settings_last_sync), lastSyncTime)
                            StatusRow(stringResource(R.string.settings_last_backup), lastBackupTime)
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
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    AppColors.GreenPrimary
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
                                Text(stringResource(R.string.settings_backup_action), fontWeight = FontWeight.SemiBold)
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
                                Text(stringResource(R.string.settings_sync_action), fontWeight = FontWeight.SemiBold)
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
                            Text(stringResource(R.string.settings_sync_detail))
                        }
                    }
                }

                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            IconTile(
                                bg = AppColors.GreenSurface,
                                tint = AppColors.GreenPrimary,
                                icon = { tint, mod ->
                                    Icon(
                                        Icons.Default.Language,
                                        contentDescription = null,
                                        tint = tint,
                                        modifier = mod
                                    )
                                }
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_language_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.settings_language_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextHint,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        LanguageOptions(
                            selected = language,
                            onSelect = { viewModel.setLanguage(it) }
                        )
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
                            text = stringResource(R.string.settings_font_size_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.settings_font_size_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextHint
                        )

                        FontScaleOptions(
                            selected = fontScale,
                            onSelect = { viewModel.setFontScale(it) }
                        )
                    }
                }

                // ── Phiếu đã xoá (history + restore) ──
                // Đổi vai trò (FARMER ↔ TRADER) là setting hành vi app, đặt ở Settings
                // hợp lý hơn trong Profile. Bao bọc trong dialog confirm vì tác động
                // mạnh: đổi nav graph + permission set.
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("deleted_cards") }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconTile(
                            bg = AppColors.Error.copy(alpha = 0.12f),
                            tint = AppColors.Error,
                            icon = { tint, mod ->
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = mod
                                )
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_deleted_cards_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.settings_deleted_cards_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextHint,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // ── Role switcher Card ──
                profile?.let { p ->
                    SettingsCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RoleSwitcher(
                                currentRole = p.role,
                                onRequestChange = { newRole -> pendingRole = newRole }
                            )
                        }
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
                            text = stringResource(R.string.settings_account_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = authState.userLabel ?: stringResource(R.string.settings_not_signed_in),
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
                            Text(stringResource(R.string.settings_sign_out), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Confirm dialog đổi role — đối xứng với flow cũ ở Profile.
    pendingRole?.let { newRole ->
        AlertDialog(
            onDismissRequest = { pendingRole = null },
            title = { Text(stringResource(R.string.settings_change_role_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    when (newRole) {
                        "TRADER" -> stringResource(R.string.settings_change_role_to_trader)
                        else -> stringResource(R.string.settings_change_role_to_farmer)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    profile?.let { current ->
                        profileViewModel.updateProfile(current = current, role = newRole)
                    }
                    pendingRole = null
                }) { Text(stringResource(R.string.settings_change_role_confirm), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRole = null }) { Text(stringResource(R.string.action_cancel)) }
            },
            shape = RoundedCornerShape(20.dp)
        )
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
private fun LanguageOptions(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppLanguage.entries.forEach { language ->
            val isSelected = language == selected
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) AppColors.GreenSurface else AppColors.SurfaceContainer,
                onClick = { onSelect(language) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(language.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) AppColors.GreenDark else AppColors.TextPrimary
                        )
                        Text(
                            text = stringResource(language.nativeLabelRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextHint
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelect(language) },
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
                        text = stringResource(scale.labelRes),
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
    val value = time?.let { formatter.format(Date(it)) } ?: stringResource(R.string.settings_empty_time)
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

@Composable
private fun SyncStatusText(status: SyncStatus) {
    val text = when (status) {
        is SyncStatus.Syncing -> stringResource(R.string.settings_sync_status_syncing)
        is SyncStatus.Success -> stringResource(R.string.settings_sync_status_success)
        is SyncStatus.Error -> stringResource(R.string.settings_sync_status_error, status.message)
        SyncStatus.Idle -> stringResource(R.string.settings_sync_status_idle)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.TextHint
    )
}
