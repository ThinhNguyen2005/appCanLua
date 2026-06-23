package com.giathinh.canlua.ui.screen

import com.giathinh.canlua.R

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
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.*
import com.giathinh.canlua.ui.screen.profile.PersonalInfoCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.giathinh.canlua.ui.feedback.LocalAppToast
import com.giathinh.canlua.data.model.AppThemeMode
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.viewmodel.AuthViewModel
import com.giathinh.canlua.ui.viewmodel.ProfileViewModel
import com.giathinh.canlua.ui.viewmodel.SettingsViewModel

import com.giathinh.canlua.util.TrackScreenRender

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

    var pendingRole by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showRestartConfirm by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }

    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var cccd by remember { mutableStateOf("") }

    LaunchedEffect(profile?.uid) {
        profile?.let {
            name = it.name
            phone = it.phone
            region = it.region
            cccd = it.cccd
        }
    }

    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val isPremium by com.giathinh.canlua.util.PremiumState.isPremium.collectAsStateWithLifecycle()
    val appToast = LocalAppToast.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(
                context = context,
                uri = uri,
                onSuccess = { appToast.success("Xuất dữ liệu sao lưu thành công!") },
                onError = { err -> appToast.error("Lỗi xuất dữ liệu: $err") }
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(
                context = context,
                uri = uri,
                onSuccess = { count -> appToast.success("Khôi phục thành công $count phiếu cân!") },
                onError = { err -> appToast.error("Lỗi khôi phục dữ liệu: $err") }
            )
        }
    }

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

            SectionHeader(
                icon = Icons.Outlined.Person,
                label = stringResource(R.string.settings_section_account),
                iconBg = AppColors.BlueSurface,
                iconTint = AppColors.Blue
            )

            RoleSwitcherContent(
                profile = profile,
                isGuest = isGuestMode,
                onRoleChange = { pendingRole = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tùy chỉnh thông tin cá nhân
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
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lịch sử thương lái (chỉ hiển thị với Nông dân)
            if (profile?.role == "FARMER") {
                ClickableSettingsRow(
                    icon = Icons.Outlined.History,
                    iconBg = AppColors.GreenSurface,
                    iconTint = AppColors.GreenPrimary,
                    title = stringResource(R.string.profile_trader_history_title),
                    subtitle = if (traderHistory.isEmpty()) {
                        stringResource(R.string.profile_no_transactions)
                    } else {
                        stringResource(R.string.profile_trader_partner_count, traderHistory.size)
                    },
                    onClick = { navController.navigate("trader_history") }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            ClickableSettingsRow(
                icon = Icons.Outlined.Logout,
                iconBg = AppColors.Error.copy(alpha = 0.12f),
                iconTint = AppColors.Error,
                title = stringResource(R.string.settings_sign_out),
                subtitle = authState.userLabel ?: stringResource(R.string.settings_not_signed_in),
                enabled = authState.isSignedIn,
                onClick = { if (authState.isSignedIn) showLogoutConfirm = true }
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionHeader(
                icon = Icons.Outlined.Palette,
                label = stringResource(R.string.settings_section_display),
                iconBg = AppColors.BlueSurface,
                iconTint = AppColors.Blue
            )

            ThemeExpandableRow(
                isExpanded = themeExpanded,
                onToggle = { themeExpanded = !themeExpanded },
                selectedTheme = appThemeMode,
                onSelect = { viewModel.setThemeMode(it); themeExpanded = false }
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionHeader(
                icon = Icons.Outlined.Folder,
                label = stringResource(R.string.settings_section_data),
                iconBg = AppColors.OrangeSurface,
                iconTint = AppColors.Orange
            )

            ClickableSettingsRow(
                icon = Icons.Outlined.DeleteForever,
                iconBg = AppColors.Error.copy(alpha = 0.12f),
                iconTint = AppColors.Error,
                title = stringResource(R.string.settings_deleted_cards_title),
                subtitle = stringResource(R.string.settings_deleted_cards_subtitle),
                onClick = { navController.navigate("deleted_cards") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ClickableSettingsRow(
                icon = Icons.Outlined.Backup,
                iconBg = AppColors.GreenSurface,
                iconTint = AppColors.GreenPrimary,
                title = "Sao lưu dữ liệu (Premium)",
                subtitle = "Xuất dữ liệu phiếu cân ra file JSON",
                onClick = {
                    if (isPremium) {
                        exportLauncher.launch("CanLua_SaoLuu_${System.currentTimeMillis() / 1000}.json")
                    } else {
                        appToast.warning("Tính năng sao lưu yêu cầu tài khoản Premium!")
                        navController.navigate("premium")
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ClickableSettingsRow(
                icon = Icons.Outlined.Restore,
                iconBg = AppColors.BlueSurface,
                iconTint = AppColors.Blue,
                title = "Khôi phục dữ liệu",
                subtitle = "Nhập dữ liệu phiếu cân từ file JSON",
                onClick = {
                    importLauncher.launch(arrayOf("application/json"))
                }
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
            text = { Text(stringResource(R.string.settings_logout_rationale)) },
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
    val elevation by animateFloatAsState(
        targetValue = if (pressed) 0f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "rowElevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.CardBg,
            disabledContainerColor = AppColors.CardBg.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation.dp,
            pressedElevation = 0.dp
        ),
        enabled = enabled,
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
        label = "expandableScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
        onClick = onToggle,
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier.animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                val rotation by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "chevronRotation"
                )
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = if (isExpanded) "Thu gọn" else "Mở rộng",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }

            if (isExpanded) {
                HorizontalDivider(
                    color = AppColors.Divider.copy(alpha = 0.5f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Box(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp)
                ) {
                    optionsContent()
                }
            }
        }
    }
}

@Composable
private fun RoleSwitcherContent(
    profile: com.giathinh.canlua.data.model.Profile?,
    isGuest: Boolean = false,
    onRoleChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.profile_user_role),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Text(
                text = if (isGuest) "🔒 Đăng nhập để chuyển đổi vai trò" else stringResource(R.string.profile_user_role_subtitle),
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
                val appToast = com.giathinh.canlua.ui.feedback.LocalAppToast.current
                RoleChipButton(
                    icon = Icons.Filled.Agriculture,
                    label = stringResource(R.string.farmer),
                    selected = currentRole != "TRADER",
                    onClick = {
                        if (isGuest) {
                            appToast.warning("Bạn cần đăng nhập mới có thể chuyển đổi vai trò!")
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
                        if (isGuest) {
                            appToast.warning("Bạn cần đăng nhập mới có thể chuyển đổi vai trò!")
                        } else if (currentRole != "TRADER") {
                            onRoleChange("TRADER")
                        }
                    },
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
        title = { Text(stringResource(R.string.settings_restart_title), fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(R.string.settings_restart_text)) },
        confirmButton = {
            TextButton(onClick = onRestart) {
                Text(stringResource(R.string.settings_restart_confirm), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_restart_dismiss), color = AppColors.TextSecondary)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = AppColors.Surface
    )
}
