package com.giathinh.canlua.ui.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.giathinh.canlua.BuildConfig
import com.giathinh.canlua.R
import com.giathinh.canlua.ads.AdConsentManager
import com.giathinh.canlua.data.model.AppLanguage
import com.giathinh.canlua.data.model.AppThemeMode
import com.giathinh.canlua.data.model.FontScale
import com.giathinh.canlua.data.model.Profile
import com.giathinh.canlua.ui.feedback.LocalAppToast
import com.giathinh.canlua.ui.screen.profile.PersonalInfoCard
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.viewmodel.AuthViewModel
import com.giathinh.canlua.ui.viewmodel.ProfileViewModel
import com.giathinh.canlua.ui.viewmodel.SettingsViewModel
import com.giathinh.canlua.util.BackupManager
import com.giathinh.canlua.util.HapticUtil
import com.giathinh.canlua.util.PremiumState
import com.giathinh.canlua.util.TrackScreenRender
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun restartActivity(context: Context) {
    val activity = context.findActivity() ?: return
    val intent = activity.intent
    activity.finish()
    activity.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    TrackScreenRender("settings")

    val isTtsEnabled by viewModel.isTtsEnabled.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val profile by profileViewModel.profile.collectAsStateWithLifecycle(initialValue = null)
    val traderHistory by profileViewModel.traderHistory.collectAsStateWithLifecycle(initialValue = emptyList())
    val isGuestMode by viewModel.isGuestMode.collectAsStateWithLifecycle(initialValue = false)
    val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsStateWithLifecycle()

    var pendingRole by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showRestartConfirm by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    var fontScaleExpanded by remember { mutableStateOf(false) }

    // State preservation for user inputs across configuration changes / process kill
    var editing by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var region by rememberSaveable { mutableStateOf("") }
    var cccd by remember { mutableStateOf("") } // keep remember for sensitive data

    // Reload form whenever profile updates (e.g. sync finish) if not actively editing
    LaunchedEffect(profile) {
        if (!editing) {
            profile?.let {
                name = it.name
                phone = it.phone
                region = it.region
                cccd = it.cccd
            }
        }
    }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val isPremium by PremiumState.isPremium.collectAsStateWithLifecycle()
    val appToast = LocalAppToast.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(
                context = context,
                uri = uri,
                onSuccess = { appToast.success(context.getString(R.string.settings_export_success)) },
                onError = { err -> appToast.error(context.getString(R.string.settings_export_error, err)) }
            )
        }
    }

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingRestoreCount by remember { mutableStateOf<Int?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                        reader.readText()
                    }
                } ?: throw IllegalStateException("Không thể đọc tệp sao lưu")
                val payload = Gson().fromJson(json, BackupManager.BackupPayload::class.java)
                val count = payload?.data?.size ?: 0
                pendingRestoreCount = count
                pendingRestoreUri = uri
            } catch (e: Exception) {
                appToast.error(context.getString(R.string.settings_import_error, e.message ?: "Tệp không hợp lệ"))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.topbar_settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticUtil.tick(context)
                        navController.popBackStack()
                    }) {
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
                )
            )
        },
        containerColor = AppColors.Surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Surface)
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            val isUserGuest = isGuestMode || !authState.isSignedIn

            // ── KHỐI 1: TÀI KHOẢN (ACCOUNT) ─────────────────────────────────
            SectionTitle(stringResource(R.string.settings_section_account))

            if (isUserGuest) {
                GuestAccountCard(
                    onLoginClick = {
                        HapticUtil.tick(context)
                        viewModel.setGuestMode(false)
                    }
                )
            } else {
                SettingsGroupCard {
                    PersonalInfoCard(
                        editing = editing,
                        name = name, onName = { name = it },
                        phone = phone, onPhone = { phone = it },
                        region = region, onRegion = { region = it },
                        cccd = cccd, onCccd = { cccd = it },
                        onToggleEdit = {
                            if (editing) {
                                profile?.let { current ->
                                    profileViewModel.updateProfile(
                                        current = current,
                                        name = name,
                                        phone = phone,
                                        region = region,
                                        cccd = cccd
                                    )
                                }
                            }
                            editing = !editing
                        },
                        onCancelEdit = {
                            profile?.let {
                                name = it.name
                                phone = it.phone
                                region = it.region
                                cccd = it.cccd
                            }
                            editing = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingsGroupCard {
                    RoleSwitcherContent(
                        profile = profile,
                        isGuest = false,
                        onRoleChange = { pendingRole = it }
                    )

                    if (profile?.role == "FARMER") {
                        HorizontalDivider(
                            color = AppColors.Divider.copy(alpha = 0.4f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        SettingsClickableRow(
                            icon = Icons.Outlined.History,
                            iconBg = AppColors.GreenSurface,
                            iconTint = AppColors.GreenPrimary,
                            title = stringResource(R.string.profile_trader_history_title),
                            subtitle = if (traderHistory.isEmpty()) {
                                stringResource(R.string.profile_no_transactions)
                            } else {
                                stringResource(R.string.profile_trader_partner_count, traderHistory.size)
                            },
                            onClick = {
                                HapticUtil.tick(context)
                                navController.navigate("trader_history")
                            }
                        )
                    }
                }
            }

            // ── KHỐI 2: CÂN LÚA & THAO TÁC (CORE WEIGHING SETTINGS) ─────────
            SectionTitle("Cân lúa & Thao tác")
            SettingsGroupCard {
                ToggleRow(
                    icon = Icons.Outlined.RecordVoiceOver,
                    title = stringResource(R.string.settings_tts_title),
                    subtitle = stringResource(R.string.settings_tts_subtitle),
                    checked = isTtsEnabled,
                    onCheckedChange = {
                        HapticUtil.tick(context)
                        viewModel.setTtsEnabled(it)
                    }
                )

                HorizontalDivider(
                    color = AppColors.Divider.copy(alpha = 0.4f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                FontScaleExpandableRow(
                    isExpanded = fontScaleExpanded,
                    onToggle = {
                        HapticUtil.tick(context)
                        fontScaleExpanded = !fontScaleExpanded
                    },
                    selectedFontScale = fontScale,
                    onSelect = {
                        HapticUtil.tick(context)
                        viewModel.setFontScale(it)
                        fontScaleExpanded = false
                    }
                )
            }

            // ── KHỐI 3: HIỂN THỊ & NGÔN NGỮ (DISPLAY & LANGUAGE) ───────────
            SectionTitle(stringResource(R.string.settings_section_display))
            SettingsGroupCard {
                ThemeExpandableRow(
                    isExpanded = themeExpanded,
                    onToggle = {
                        HapticUtil.tick(context)
                        themeExpanded = !themeExpanded
                    },
                    selectedTheme = appThemeMode,
                    onSelect = {
                        HapticUtil.tick(context)
                        viewModel.setThemeMode(it)
                        themeExpanded = false
                    }
                )

                HorizontalDivider(
                    color = AppColors.Divider.copy(alpha = 0.4f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                LanguageExpandableRow(
                    isExpanded = languageExpanded,
                    onToggle = {
                        HapticUtil.tick(context)
                        languageExpanded = !languageExpanded
                    },
                    selectedLanguage = language,
                    onSelect = {
                        HapticUtil.tick(context)
                        viewModel.setLanguage(it)
                        languageExpanded = false
                    }
                )
            }

            // ── KHỐI 4: QUẢN LÝ DỮ LIỆU & SAO LƯU ──────────────────────────
            SectionTitle(stringResource(R.string.settings_section_data))
            SettingsGroupCard {
                ToggleRow(
                    icon = Icons.Outlined.CloudSync,
                    title = stringResource(R.string.settings_auto_sync_title),
                    subtitle = stringResource(R.string.settings_auto_sync_subtitle),
                    checked = isAutoSyncEnabled,
                    onCheckedChange = {
                        HapticUtil.tick(context)
                        viewModel.setAutoSyncEnabled(it)
                    }
                )

                HorizontalDivider(
                    color = AppColors.Divider.copy(alpha = 0.4f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Phiếu đã xoá dùng icon khôi phục trung tính, không dùng màu đỏ
                SettingsClickableRow(
                    icon = Icons.Outlined.RestoreFromTrash,
                    iconBg = AppColors.SurfaceContainer,
                    iconTint = AppColors.TextSecondary,
                    title = stringResource(R.string.settings_deleted_cards_title),
                    subtitle = stringResource(R.string.settings_deleted_cards_subtitle),
                    onClick = {
                        HapticUtil.tick(context)
                        navController.navigate("deleted_cards")
                    }
                )

                HorizontalDivider(
                    color = AppColors.Divider.copy(alpha = 0.4f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Sao lưu dữ liệu (kèm nhãn Premium, không toast trùng)
                SettingsClickableRow(
                    icon = Icons.Outlined.Backup,
                    iconBg = AppColors.GreenSurface,
                    iconTint = AppColors.GreenPrimary,
                    title = stringResource(R.string.settings_backup_title),
                    subtitle = if (isPremium) {
                        stringResource(R.string.settings_backup_desc)
                    } else {
                        "Xuất file JSON sao lưu an toàn (Dành riêng cho Premium)"
                    },
                    trailingBadge = if (!isPremium) "PREMIUM" else null,
                    onClick = {
                        HapticUtil.tick(context)
                        if (isPremium) {
                            exportLauncher.launch("CanLua_SaoLuu_${System.currentTimeMillis() / 1000}.json")
                        } else {
                            navController.navigate("premium")
                        }
                    }
                )

                HorizontalDivider(
                    color = AppColors.Divider.copy(alpha = 0.4f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Khôi phục dữ liệu (mở dialog xác nhận gộp dữ liệu)
                SettingsClickableRow(
                    icon = Icons.Outlined.Restore,
                    iconBg = AppColors.BlueSurface,
                    iconTint = AppColors.Blue,
                    title = stringResource(R.string.settings_restore_title),
                    subtitle = "Gộp dữ liệu từ file sao lưu JSON vào máy",
                    onClick = {
                        HapticUtil.tick(context)
                        importLauncher.launch(arrayOf("application/json"))
                    }
                )

                val isPrivacyOptionsRequired by AdConsentManager.isPrivacyOptionsRequired.collectAsStateWithLifecycle()
                if (isPrivacyOptionsRequired) {
                    HorizontalDivider(
                        color = AppColors.Divider.copy(alpha = 0.4f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsClickableRow(
                        icon = Icons.Outlined.Security,
                        iconBg = AppColors.BlueSurface,
                        iconTint = AppColors.Info,
                        title = stringResource(R.string.settings_privacy_options),
                        subtitle = stringResource(R.string.settings_privacy_options_subtitle),
                        onClick = {
                            HapticUtil.tick(context)
                            val activity = context.findActivity()
                            if (activity != null) {
                                AdConsentManager.showPrivacyOptionsForm(activity)
                            }
                        }
                    )
                }
            }

            // ── KHỐI 5: ĐĂNG XUẤT (TÁCH RIÊNG Ở CUỐI) ──────────────────────
            if (!isUserGuest) {
                Spacer(modifier = Modifier.height(24.dp))
                SignOutCard(
                    userLabel = authState.userLabel ?: stringResource(R.string.settings_not_signed_in),
                    onClick = {
                        HapticUtil.tick(context)
                        showLogoutConfirm = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            SettingsFooter()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── HỘP THOẠI XÁC NHẬN KHÔI PHỤC ─────────────────────────────────────────
    if (pendingRestoreUri != null) {
        val count = pendingRestoreCount ?: 0
        AlertDialog(
            onDismissRequest = {
                pendingRestoreUri = null
                pendingRestoreCount = null
            },
            icon = {
                Icon(
                    Icons.Outlined.Restore,
                    contentDescription = null,
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Xác nhận khôi phục sao lưu",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tệp sao lưu chứa $count phiếu cân.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "Dữ liệu từ tệp sẽ được GỘP THÊM vào danh sách hiện tại trên máy của bạn (không làm mất hoặc ghi đè các phiếu đã có).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingRestoreUri
                        pendingRestoreUri = null
                        pendingRestoreCount = null
                        if (uri != null) {
                            viewModel.importBackup(
                                context = context,
                                uri = uri,
                                onSuccess = { importedCount ->
                                    appToast.success(context.getString(R.string.settings_import_success, importedCount))
                                },
                                onError = { err ->
                                    appToast.error(context.getString(R.string.settings_import_error, err))
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Khôi phục", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingRestoreUri = null
                    pendingRestoreCount = null
                }) {
                    Text(stringResource(R.string.action_cancel), color = AppColors.TextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = AppColors.Surface
        )
    }

    // ── HỘP THOẠI ĐĂNG XUẤT ─────────────────────────────────────────────────
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(R.string.settings_sign_out), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.settings_logout_rationale)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        HapticUtil.tick(context)
                        authViewModel.signOut()
                        showLogoutConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Error)
                ) {
                    Text(stringResource(R.string.settings_sign_out), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    HapticUtil.tick(context)
                    showLogoutConfirm = false
                }) {
                    Text(stringResource(R.string.action_cancel), color = AppColors.TextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = AppColors.Surface
        )
    }

    if (pendingRole != null) {
        RoleChangeDialog(
            newRole = pendingRole!!,
            onDismiss = { pendingRole = null },
            onConfirm = { newRole ->
                profile?.let { current -> profileViewModel.updateProfile(current = current, role = newRole) }
                pendingRole = null
                showRestartConfirm = true
            }
        )
    }

    if (showRestartConfirm) {
        RestartConfirmDialog(
            onDismiss = { showRestartConfirm = false },
            onRestart = {
                showRestartConfirm = false
                restartActivity(context)
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = AppColors.TextSecondary,
        modifier = Modifier.padding(start = 6.dp, top = 22.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun GuestAccountCard(onLoginClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AppColors.GreenSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = AppColors.GreenPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Chưa đăng nhập tài khoản",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "Bạn đang dùng ở chế độ Khách",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextHint
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Đăng nhập để tự động sao lưu dữ liệu lên đám mây, đồng bộ giữa các máy và mở khoá đầy đủ vai trò Nông dân & Thương lái.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Outlined.Login,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Đăng nhập ngay",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SignOutCard(
    userLabel: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = AppColors.Error.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppColors.Error.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        tint = AppColors.Error,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_sign_out),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Error
                    )
                    Text(
                        text = userLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextHint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = AppColors.Error.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailingBadge: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (enabled) AppColors.TextPrimary else AppColors.TextHint
                        )
                        if (trailingBadge != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AppColors.GoldAccent.copy(alpha = 0.16f),
                                modifier = Modifier.padding(start = 2.dp)
                            ) {
                                Text(
                                    text = trailingBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.GoldDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextHint,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = AppColors.TextHint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.GreenSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AppColors.GreenPrimary, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppColors.GreenPrimary,
                uncheckedThumbColor = AppColors.TextHint,
                uncheckedTrackColor = AppColors.SurfaceContainer
            )
        )
    }
}

@Composable
private fun FontScaleExpandableRow(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    selectedFontScale: FontScale,
    onSelect: (FontScale) -> Unit
) {
    ExpandableRowContent(
        isExpanded = isExpanded,
        onToggle = onToggle,
        icon = Icons.Outlined.FormatSize,
        title = stringResource(R.string.settings_font_size_title),
        selectedValue = stringResource(selectedFontScale.labelRes),
        optionsContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FontScale.values().forEach { scale ->
                    val isSelected = scale == selectedFontScale
                    Surface(
                        onClick = { onSelect(scale) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) AppColors.GreenSurface else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(scale.labelRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
    )
}

@Composable
private fun ThemeExpandableRow(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    selectedTheme: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit
) {
    ExpandableRowContent(
        isExpanded = isExpanded,
        onToggle = onToggle,
        icon = Icons.Outlined.DarkMode,
        title = stringResource(R.string.settings_theme_title),
        selectedValue = stringResource(selectedTheme.labelRes),
        optionsContent = {
            ThemeModeOptions(
                selected = selectedTheme,
                onSelect = onSelect
            )
        }
    )
}

@Composable
private fun LanguageExpandableRow(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    selectedLanguage: AppLanguage,
    onSelect: (AppLanguage) -> Unit
) {
    ExpandableRowContent(
        isExpanded = isExpanded,
        onToggle = onToggle,
        icon = Icons.Outlined.Translate,
        title = stringResource(R.string.settings_language_title),
        selectedValue = stringResource(selectedLanguage.nativeLabelRes),
        optionsContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppLanguage.values().forEach { lang ->
                    val isSelected = lang == selectedLanguage
                    Surface(
                        onClick = { onSelect(lang) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) AppColors.GreenSurface else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(lang.nativeLabelRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) AppColors.GreenPrimary else AppColors.TextPrimary
                                )
                                Text(
                                    text = stringResource(lang.labelRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextHint
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AppColors.GreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun ExpandableRowContent(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    icon: ImageVector,
    title: String,
    selectedValue: String,
    optionsContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        Surface(
            onClick = onToggle,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.GreenSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = AppColors.GreenPrimary, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = selectedValue,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.GreenPrimary
                        )
                    }
                }
                val rotation by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "chevronRotation"
                )
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(R.string.settings_collapse) else stringResource(R.string.settings_expand),
                    tint = AppColors.TextSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }

        if (isExpanded) {
            HorizontalDivider(
                color = AppColors.Divider.copy(alpha = 0.4f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Box(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp, top = 4.dp)
            ) {
                optionsContent()
            }
        }
    }
}

@Composable
private fun RoleSwitcherContent(
    profile: Profile?,
    isGuest: Boolean = false,
    onRoleChange: (String) -> Unit
) {
    val context = LocalContext.current
    val appToast = LocalAppToast.current

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.profile_user_role),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        Text(
            text = if (isGuest) stringResource(R.string.settings_role_guest_locked) else stringResource(R.string.profile_user_role_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = if (isGuest) AppColors.Orange else AppColors.TextHint
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AppColors.SurfaceContainer)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val currentRole = profile?.role ?: "FARMER"
            RoleChipButton(
                icon = Icons.Filled.Agriculture,
                label = stringResource(R.string.farmer),
                selected = currentRole != "TRADER",
                onClick = {
                    HapticUtil.tick(context)
                    if (isGuest) {
                        appToast.warning(context.getString(R.string.settings_role_guest_toast))
                    } else if (currentRole != "FARMER") {
                        onRoleChange("FARMER")
                    }
                },
                modifier = Modifier.weight(1f)
            )
            RoleChipButton(
                icon = Icons.Filled.Storefront,
                label = stringResource(R.string.trader),
                selected = currentRole == "TRADER",
                onClick = {
                    HapticUtil.tick(context)
                    if (isGuest) {
                        appToast.warning(context.getString(R.string.settings_role_guest_toast))
                    } else if (currentRole != "TRADER") {
                        onRoleChange("TRADER")
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RoleChipButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) AppColors.GreenPrimary else Color.Transparent)
            .clickable(enabled = !selected, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) Color.White else AppColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun ThemeModeOptions(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AppThemeMode.values().forEach { mode ->
            val isSelected = mode == selected
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) AppColors.GreenSurface else Color.Transparent,
                onClick = {
                    HapticUtil.tick(context)
                    onSelect(mode)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            imageVector = when (mode) {
                                AppThemeMode.LIGHT -> Icons.Outlined.LightMode
                                AppThemeMode.HIGH_CONTRAST -> Icons.Outlined.Contrast
                                AppThemeMode.OLED -> Icons.Outlined.DarkMode
                                AppThemeMode.AUTO -> Icons.Outlined.BrightnessAuto
                            },
                            contentDescription = null,
                            tint = if (isSelected) AppColors.GreenPrimary else AppColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(mode.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) AppColors.GreenDark else AppColors.TextPrimary
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            HapticUtil.tick(context)
                            onSelect(mode)
                        },
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
private fun SettingsFooter() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Cân Lúa v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextHint)
        Text(text = "Gia Thịnh \u00a9 2026", style = MaterialTheme.typography.labelSmall, color = AppColors.TextHint.copy(alpha = 0.7f))
    }
}

@Composable
private fun RoleChangeDialog(
    newRole: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_change_role_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    when (newRole) {
                        "TRADER" -> stringResource(R.string.settings_change_role_to_trader)
                        else -> stringResource(R.string.settings_change_role_to_farmer)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_role_restart_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                HapticUtil.tick(context)
                onConfirm(newRole)
            }) {
                Text(stringResource(R.string.settings_change_role_confirm), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                HapticUtil.tick(context)
                onDismiss()
            }) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun RestartConfirmDialog(
    onDismiss: () -> Unit,
    onRestart: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_restart_title), fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(R.string.settings_restart_text)) },
        confirmButton = {
            TextButton(onClick = {
                HapticUtil.tick(context)
                onRestart()
            }) {
                Text(stringResource(R.string.settings_restart_confirm), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                HapticUtil.tick(context)
                onDismiss()
            }) {
                Text(stringResource(R.string.settings_restart_dismiss), color = AppColors.TextSecondary)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = AppColors.Surface
    )
}
