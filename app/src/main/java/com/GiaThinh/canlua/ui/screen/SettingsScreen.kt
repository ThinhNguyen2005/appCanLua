package com.GiaThinh.canlua.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.data.model.AppLanguage
import com.GiaThinh.canlua.data.model.AppThemeMode
import com.GiaThinh.canlua.data.model.FontScale
import com.GiaThinh.canlua.repository.BackupStatus
import com.GiaThinh.canlua.repository.SyncStatus
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    syncViewModel: SyncViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    TrackScreenRender("settings")

    val isTtsEnabled by viewModel.isTtsEnabled.collectAsStateWithLifecycle()
    val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val syncStatus by syncViewModel.syncStatus.collectAsStateWithLifecycle()
    val lastSyncTime by syncViewModel.lastSyncTime.collectAsStateWithLifecycle()
    val backupStatus by syncViewModel.backupStatus.collectAsStateWithLifecycle()
    val lastBackupTime by syncViewModel.lastBackupTime.collectAsStateWithLifecycle()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val profile by profileViewModel.profile.collectAsStateWithLifecycle(initialValue = null)
    val premiumInfo by PremiumState.info.collectAsStateWithLifecycle()

    var pendingRole by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showRestartConfirm by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }
    var fontSizeExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.topbar_settings),
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
                windowInsets = WindowInsets(0.dp)
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
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            PremiumCard(
                isActive = premiumInfo.isActive,
                isEarlyAdopter = premiumInfo.isEarlyAdopter,
                plan = premiumInfo.plan,
                sinceMs = premiumInfo.sinceMs,
                onClick = { navController.navigate("premium") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(
                icon = Icons.Outlined.Person,
                label = stringResource(R.string.settings_section_account),
                iconBg = AppColors.BlueSurface,
                iconTint = AppColors.Blue
            )

            RoleSwitcherContent(
                profile = profile,
                onRoleChange = { pendingRole = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ClickableSettingsRow(
                icon = Icons.Outlined.Logout,
                iconBg = AppColors.Error.copy(alpha = 0.12f),
                iconTint = AppColors.Error,
                title = stringResource(R.string.settings_sign_out),
                subtitle = authState.userLabel ?: stringResource(R.string.settings_not_signed_in),
                enabled = authState.isSignedIn,
                onClick = { if (authState.isSignedIn) showLogoutConfirm = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(
                icon = Icons.Outlined.Palette,
                label = stringResource(R.string.settings_section_display),
                iconBg = AppColors.BlueSurface,
                iconTint = AppColors.Blue
            )

            TtsSettingsCard(
                isEnabled = isTtsEnabled,
                onToggle = { viewModel.setTtsEnabled(!isTtsEnabled) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            ThemeExpandableRow(
                isExpanded = themeExpanded,
                onToggle = { themeExpanded = !themeExpanded },
                selectedTheme = appThemeMode,
                onSelect = { viewModel.setThemeMode(it); themeExpanded = false }
            )

            Spacer(modifier = Modifier.height(8.dp))

            FontSizeExpandableRow(
                isExpanded = fontSizeExpanded,
                onToggle = { fontSizeExpanded = !fontSizeExpanded },
                selectedFontScale = fontScale,
                onSelect = { viewModel.setFontScale(it); fontSizeExpanded = false }
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(
                icon = Icons.Outlined.Language,
                label = stringResource(R.string.settings_section_language),
                iconBg = AppColors.GreenSurface,
                iconTint = AppColors.GreenPrimary
            )

            LanguageExpandableRow(
                isExpanded = languageExpanded,
                onToggle = { languageExpanded = !languageExpanded },
                selectedLanguage = language,
                onSelect = { viewModel.setLanguage(it); languageExpanded = false }
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(
                icon = Icons.Outlined.CloudSync,
                label = stringResource(R.string.settings_section_sync),
                iconBg = AppColors.Info.copy(alpha = 0.12f),
                iconTint = AppColors.Info
            )

            SettingsCardBox {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ToggleRow(
                        icon = Icons.Outlined.Sync,
                        title = "Tự động đồng bộ",
                        subtitle = "Tự động tải/đẩy dữ liệu Firebase khi mở app hoặc có thay đổi",
                        checked = isAutoSyncEnabled,
                        onCheckedChange = { viewModel.setAutoSyncEnabled(it) }
                    )
                    HorizontalDivider(color = AppColors.Divider.copy(alpha = 0.4f), thickness = 0.5.dp)
                    SyncStatusDisplay(
                        lastSyncTime = lastSyncTime,
                        lastBackupTime = lastBackupTime,
                        syncStatus = syncStatus
                    )
                    HorizontalDivider(color = AppColors.Divider.copy(alpha = 0.4f), thickness = 0.5.dp)
                    SyncActionButtons(
                        syncStatus = syncStatus,
                        backupStatus = backupStatus,
                        onBackup = { syncViewModel.backupNow() },
                        onSync = { syncViewModel.syncAll() }
                    )
                    ClickableLink(
                        text = stringResource(R.string.settings_sync_detail),
                        onClick = { navController.navigate("sync_status") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(
                icon = Icons.Outlined.Folder,
                label = stringResource(R.string.settings_section_data),
                iconBg = AppColors.OrangeSurface,
                iconTint = AppColors.Orange
            )

            ClickableSettingsRow(
                icon = Icons.Outlined.Delete,
                iconBg = AppColors.Error.copy(alpha = 0.12f),
                iconTint = AppColors.Error,
                title = stringResource(R.string.settings_deleted_cards_title),
                subtitle = stringResource(R.string.settings_deleted_cards_subtitle),
                onClick = { navController.navigate("deleted_cards") }
            )

            Spacer(modifier = Modifier.height(32.dp))

            SettingsFooter()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(R.string.settings_sign_out), fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn đăng xuất không? Dữ liệu chưa đồng bộ có thể bị mất.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.signOut()
                        showLogoutConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Error)
                ) {
                    Text(stringResource(R.string.settings_sign_out), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
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
            visible = true,
            onDismiss = { showRestartConfirm = false },
            onRestart = {
                showRestartConfirm = false
                restartActivity(context)
            }
        )
    }
}

private fun restartActivity(context: android.content.Context) {
    val activity = context as? android.app.Activity ?: return
    val intent = activity.intent
    activity.finish()
    activity.startActivity(intent)
    activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    label: String,
    iconBg: Color,
    iconTint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconBadge(icon = icon, bg = iconBg, tint = iconTint)
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextPrimary
        )
    }
}

@Composable
private fun IconBadge(icon: ImageVector, bg: Color, tint: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = bg, modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingsCardBox(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        content()
    }
}

@Composable
private fun ClickableSettingsRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "rowScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.07f),
                spotColor = Color.Black.copy(alpha = 0.07f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.CardBg)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = AppColors.GreenPrimary.copy(alpha = 0.15f)),
                enabled = enabled,
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconBadge(icon = icon, bg = iconBg, tint = iconTint)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (enabled) AppColors.TextPrimary else AppColors.TextHint
                    )
                    Text(
                        text = subtitle,
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconBadge(icon = icon, bg = AppColors.GreenSurface, tint = AppColors.GreenPrimary)
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
            onCheckedChange = onCheckedChange,
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
private fun ThemeExpandableRow(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    selectedTheme: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit
) {
    ExpandableCard(
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
private fun FontSizeExpandableRow(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    selectedFontScale: FontScale,
    onSelect: (FontScale) -> Unit
) {
    ExpandableCard(
        isExpanded = isExpanded,
        onToggle = onToggle,
        icon = Icons.Outlined.TextFields,
        title = stringResource(R.string.settings_font_size_title),
        selectedValue = stringResource(selectedFontScale.labelRes),
        optionsContent = {
            FontScaleOptions(
                selected = selectedFontScale,
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
    ExpandableCard(
        isExpanded = isExpanded,
        onToggle = onToggle,
        icon = Icons.Outlined.Translate,
        title = stringResource(R.string.settings_language_title),
        selectedValue = stringResource(selectedLanguage.nativeLabelRes),
        optionsContent = {
            LanguageOptions(
                selected = selectedLanguage,
                onSelect = onSelect
            )
        }
    )
}

@Composable
private fun ExpandableCard(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    icon: ImageVector,
    title: String,
    selectedValue: String,
    optionsContent: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "expandRowScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.animateContentSize(
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true, color = AppColors.GreenPrimary.copy(alpha = 0.15f)),
                        onClick = onToggle
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    IconBadge(icon = icon, bg = AppColors.GreenSurface, tint = AppColors.GreenPrimary)
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
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (isExpanded) "Thu gọn" else "Mở rộng",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    color = AppColors.SurfaceContainer
                ) {
                    optionsContent()
                }
            }
        }
    }
}

@Composable
private fun TtsSettingsCard(
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "ttsCardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = AppColors.GreenPrimary.copy(alpha = 0.15f)),
                    onClick = onToggle
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                IconBadge(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    bg = if (isEnabled) AppColors.GreenSurface else AppColors.SurfaceContainer,
                    tint = if (isEnabled) AppColors.GreenPrimary else AppColors.TextSecondary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_tts_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.settings_tts_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextHint,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AppColors.GreenPrimary,
                    uncheckedThumbColor = AppColors.TextHint,
                    uncheckedTrackColor = AppColors.SurfaceContainer
                )
            )
        }
    }
}

@Composable
private fun PremiumCard(
    isActive: Boolean,
    isEarlyAdopter: Boolean,
    plan: String?,
    sinceMs: Long,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "premiumScale"
    )

    val backgroundBrush = if (isActive) {
        Brush.linearGradient(
            colors = listOf(
                AppColors.GoldLight.copy(alpha = 0.25f),
                AppColors.GoldAccent.copy(alpha = 0.1f)
            )
        )
    } else {
        Brush.linearGradient(colors = listOf(AppColors.CardBg, AppColors.CardBg))
    }

    val shadowElevation = if (isActive) 4.dp else 2.dp
    val shadowColor = if (isActive) AppColors.GoldDark.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.06f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(20.dp),
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundBrush)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = AppColors.GoldDark.copy(alpha = 0.18f)),
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isActive) AppColors.GoldLight else AppColors.GoldLight.copy(alpha = 0.45f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = AppColors.GoldDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_premium_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isActive) stringResource(R.string.settings_premium_activated)
                        else stringResource(R.string.settings_premium_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextHint,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = AppColors.GoldLight.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = if (isActive) stringResource(R.string.settings_premium_manage)
                        else stringResource(R.string.settings_premium_upgrade),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.GoldDark,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }

            if (isActive) {
                val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
                val statusText = if (isEarlyAdopter)
                    stringResource(R.string.settings_premium_early_adopter)
                else
                    stringResource(R.string.settings_premium_active)
                val planText = plan?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.settings_premium_activated)
                val sinceText = if (sinceMs > 0L)
                    stringResource(R.string.settings_premium_since, sdf.format(Date(sinceMs)))
                else ""

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PremiumStatusPill(
                        text = statusText,
                        isEarlyAdopter = isEarlyAdopter,
                        modifier = Modifier.weight(weight = 0.42f, fill = false)
                    )
                    Text(
                        text = "$planText$sinceText",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumStatusPill(
    text: String,
    isEarlyAdopter: Boolean,
    modifier: Modifier = Modifier
) {
    val container = if (isEarlyAdopter) AppColors.GoldLight.copy(alpha = 0.75f) else AppColors.GreenSurface
    val content = if (isEarlyAdopter) AppColors.GoldDark else AppColors.GreenDark

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = container
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(content)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = content,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RoleSwitcherContent(
    profile: com.GiaThinh.canlua.data.model.Profile?,
    onRoleChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.CardBg)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.profile_user_role),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.profile_user_role_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextHint
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
                    onClick = { if (currentRole != "FARMER") onRoleChange("FARMER") },
                    modifier = Modifier.weight(1f)
                )
                RoleChipButton(
                    icon = Icons.Filled.Storefront,
                    label = stringResource(R.string.trader),
                    selected = currentRole == "TRADER",
                    onClick = { if (currentRole != "TRADER") onRoleChange("TRADER") },
                    modifier = Modifier.weight(1f)
                )
            }
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
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) AppColors.GreenPrimary else Color.Transparent)
            .clickable(enabled = !selected, onClick = onClick)
            .padding(vertical = 10.dp),
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
private fun SyncStatusDisplay(
    lastSyncTime: Long?,
    lastBackupTime: Long?,
    syncStatus: SyncStatus
) {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SyncStatusRow(
            icon = Icons.Outlined.Sync,
            label = stringResource(R.string.settings_last_sync),
            time = lastSyncTime?.let { formatter.format(Date(it)) }
                ?: stringResource(R.string.settings_empty_time),
            statusText = when (syncStatus) {
                is SyncStatus.Syncing -> stringResource(R.string.settings_sync_status_syncing)
                is SyncStatus.Success -> stringResource(R.string.settings_sync_status_success)
                is SyncStatus.Error -> stringResource(R.string.settings_sync_status_error, syncStatus.message)
                else -> null
            },
            statusColor = when (syncStatus) {
                is SyncStatus.Syncing -> AppColors.Info
                is SyncStatus.Success -> AppColors.GreenPrimary
                is SyncStatus.Error -> AppColors.Error
                else -> null
            }
        )
        SyncStatusRow(
            icon = Icons.Outlined.Backup,
            label = stringResource(R.string.settings_last_backup),
            time = lastBackupTime?.let { formatter.format(Date(it)) }
                ?: stringResource(R.string.settings_empty_time),
            statusText = null,
            statusColor = null
        )
    }
}

@Composable
private fun SyncStatusRow(
    icon: ImageVector,
    label: String,
    time: String,
    statusText: String?,
    statusColor: Color?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(time, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = AppColors.TextPrimary)
            statusText?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = statusColor ?: AppColors.TextHint) }
        }
    }
}

@Composable
private fun SyncActionButtons(
    syncStatus: SyncStatus,
    backupStatus: BackupStatus,
    onBackup: () -> Unit,
    onSync: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onBackup,
            modifier = Modifier.weight(1f),
            enabled = backupStatus !is BackupStatus.BackingUp,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.GreenPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.GreenPrimary)
        ) {
            if (backupStatus is BackupStatus.BackingUp) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AppColors.GreenPrimary)
            } else {
                Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.settings_backup_action), fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onSync,
            modifier = Modifier.weight(1f),
            enabled = syncStatus !is SyncStatus.Syncing,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.GreenPrimary, contentColor = Color.White)
        ) {
            if (syncStatus is SyncStatus.Syncing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Icon(Icons.Outlined.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.settings_sync_action), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ClickableLink(text: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = AppColors.GreenPrimary.copy(alpha = 0.15f)),
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = AppColors.GreenPrimary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppColors.GreenPrimary)
    }
}

@Composable
private fun LanguageOptions(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit
) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AppLanguage.entries.forEach { lang ->
            val isSelected = lang == selected
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) AppColors.GreenSurface else Color.Transparent,
                onClick = { onSelect(lang) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(lang.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) AppColors.GreenDark else AppColors.TextPrimary
                        )
                        Text(
                            text = stringResource(lang.nativeLabelRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextHint
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelect(lang) },
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
private fun ThemeModeOptions(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit
) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AppThemeMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) AppColors.GreenSurface else Color.Transparent,
                onClick = { onSelect(mode) }
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
                        onClick = { onSelect(mode) },
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
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { scale ->
            val isSelected = scale == selected
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) AppColors.GreenSurface else Color.Transparent,
                onClick = { onSelect(scale) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(scale.labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) AppColors.GreenDark else AppColors.TextPrimary,
                        modifier = Modifier.padding(start = 30.dp)
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
private fun SettingsFooter() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "CanLua v1.0.0", style = MaterialTheme.typography.bodySmall, color = AppColors.TextHint)
        Text(text = "GiaThinh \u00a9 2024", style = MaterialTheme.typography.labelSmall, color = AppColors.TextHint.copy(alpha = 0.7f))
    }
}

@Composable
private fun RoleChangeDialog(
    newRole: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
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
                    text = "Vai trò sẽ thay đổi sau khi khởi động lại ứng dụng.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(newRole) }) {
                Text(stringResource(R.string.settings_change_role_confirm), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun RestartConfirmDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onRestart: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Khởi động lại ứng dụng", fontWeight = FontWeight.Bold) },
        text = { Text("Vui lòng khởi động lại ứng dụng để cập nhật vai trò và giao diện mới.") },
        confirmButton = {
            TextButton(onClick = onRestart) {
                Text("Khởi động lại ngay", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Để sau", color = AppColors.TextSecondary)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = AppColors.Surface
    )
}
