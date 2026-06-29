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
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.giathinh.canlua.ui.feedback.LocalAppToast
import com.giathinh.canlua.data.model.AppThemeMode
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.ui.viewmodel.SettingsViewModel
import com.giathinh.canlua.ui.navigation.BottomNavItem
import com.giathinh.canlua.repository.WeighDefaults
import com.giathinh.canlua.ui.theme.lockedAwareTextFieldColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.outlined.Scale

import com.giathinh.canlua.util.TrackScreenRender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    TrackScreenRender("settings")

    val isTtsEnabled by viewModel.isTtsEnabled.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val uiMode by viewModel.uiMode.collectAsStateWithLifecycle()

    val weighDefaults by viewModel.weighDefaults.collectAsStateWithLifecycle()

    var bagSamplingLocal by remember { mutableStateOf(weighDefaults.bagMethodIsSampling) }
    var bagsPerKgTextLocal by remember {
        mutableStateOf(if (!weighDefaults.bagMethodIsSampling && weighDefaults.bagSampleCount > 0) weighDefaults.bagSampleCount.toString() else "8")
    }
    var sampleCountTextLocal by remember {
        mutableStateOf(if (weighDefaults.bagSampleCount > 0) weighDefaults.bagSampleCount.toString() else "8")
    }
    var sampleWeightTextLocal by remember {
        mutableStateOf(if (weighDefaults.bagSampleTotalWeight > 0.0) weighDefaults.bagSampleTotalWeight.toString() else "1.0")
    }
    var inputModeLocal by remember { mutableStateOf(weighDefaults.weightInputMode) }

    LaunchedEffect(weighDefaults) {
        bagSamplingLocal = weighDefaults.bagMethodIsSampling
        inputModeLocal = weighDefaults.weightInputMode
        
        val currentBagsPerKg = bagsPerKgTextLocal.toIntOrNull() ?: 8
        if (!weighDefaults.bagMethodIsSampling && weighDefaults.bagSampleCount != currentBagsPerKg) {
            bagsPerKgTextLocal = weighDefaults.bagSampleCount.toString()
        }
        val currentSampleCount = sampleCountTextLocal.toIntOrNull() ?: 8
        if (weighDefaults.bagSampleCount != currentSampleCount) {
            sampleCountTextLocal = weighDefaults.bagSampleCount.toString()
        }
        val currentSampleWeight = sampleWeightTextLocal.toDoubleOrNull() ?: 1.0
        if (weighDefaults.bagSampleTotalWeight != currentSampleWeight) {
            sampleWeightTextLocal = weighDefaults.bagSampleTotalWeight.toString()
        }
    }

    LaunchedEffect(bagSamplingLocal, bagsPerKgTextLocal, sampleCountTextLocal, sampleWeightTextLocal, inputModeLocal) {
        kotlinx.coroutines.delay(500L)
        val sampleCount = if (bagSamplingLocal) {
            sampleCountTextLocal.toIntOrNull()?.coerceAtLeast(1) ?: 8
        } else {
            bagsPerKgTextLocal.toIntOrNull()?.coerceAtLeast(1) ?: 8
        }
        val sampleWeight = if (bagSamplingLocal) {
            sampleWeightTextLocal.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 1.0
        } else {
            1.0
        }
        val newDefaults = WeighDefaults(
            impurityIsPercent = weighDefaults.impurityIsPercent,
            bagMethodIsSampling = bagSamplingLocal,
            bagSampleCount = sampleCount,
            bagSampleTotalWeight = sampleWeight,
            weightInputMode = inputModeLocal
        )
        if (newDefaults != weighDefaults) {
            viewModel.setWeighDefaults(newDefaults)
        }
    }

    var themeExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

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
                    val prevRoute = navController.previousBackStackEntry?.destination?.route
                    val isFromTab = prevRoute == BottomNavItem.SCALE.route ||
                            prevRoute == BottomNavItem.HISTORY.route ||
                            prevRoute == BottomNavItem.STATISTICS.route ||
                            prevRoute == "settings"
                    if (navController.previousBackStackEntry != null && !isFromTab) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_back),
                                tint = AppColors.TextPrimary
                            )
                        }
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

            // --- UI Mode Switch (Simple/Standard) ---
            SettingsCardBox {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Giao diện đơn giản",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Làm to chữ, phóng to nút bấm, ẩn các tính năng phức tạp dành cho người lớn tuổi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                    }
                    Switch(
                        checked = uiMode == com.giathinh.canlua.data.model.AppUiMode.SIMPLE,
                        onCheckedChange = { isSimple ->
                            viewModel.setUiMode(
                                if (isSimple) com.giathinh.canlua.data.model.AppUiMode.SIMPLE
                                else com.giathinh.canlua.data.model.AppUiMode.STANDARD
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AppColors.GreenPrimary,
                            uncheckedThumbColor = AppColors.TextSecondary,
                            uncheckedTrackColor = AppColors.Surface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

            // --- TTS Switch ---
            SettingsCardBox {
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
                        IconBadge(icon = Icons.Outlined.RecordVoiceOver, bg = AppColors.OrangeSurface, tint = AppColors.Orange)
                        Column {
                            Text(
                                text = stringResource(R.string.settings_tts_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.settings_tts_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextHint
                            )
                        }
                    }
                    Switch(
                        checked = isTtsEnabled,
                        onCheckedChange = { viewModel.setTtsEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AppColors.GreenPrimary,
                            uncheckedThumbColor = AppColors.TextSecondary,
                            uncheckedTrackColor = AppColors.Surface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(
                icon = Icons.Outlined.Scale,
                label = "Cấu hình cân lúa",
                iconBg = AppColors.GreenSurface,
                iconTint = AppColors.GreenPrimary
            )

            // 1. Hình thức bao bì mặc định (tùy chỉnh hình thức bao bì)
            SettingsCardBox {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Cách trừ bao bì mặc định",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "Quy định cách trừ khối lượng bao bì khi tạo phiếu cân mới.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Option 1: Bao đơn vị
                    Surface(
                        onClick = { bagSamplingLocal = false },
                        shape = RoundedCornerShape(12.dp),
                        color = if (!bagSamplingLocal) AppColors.GreenSurface else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = !bagSamplingLocal,
                                onClick = { bagSamplingLocal = false },
                                colors = RadioButtonDefaults.colors(selectedColor = AppColors.GreenPrimary)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bao đơn vị (X bao = 1 kg)", fontWeight = FontWeight.SemiBold)
                                Text("Ví dụ: 8 bao quy đổi ra 1 kg bì", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            }
                        }
                    }

                    if (!bagSamplingLocal) {
                        OutlinedTextField(
                            value = bagsPerKgTextLocal,
                            onValueChange = { bagsPerKgTextLocal = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Số bao quy đổi 1 kg bì") },
                            suffix = { Text("bao = 1 kg") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            colors = lockedAwareTextFieldColors()
                        )
                    }

                    // Option 2: Cân mẫu
                    Surface(
                        onClick = { bagSamplingLocal = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (bagSamplingLocal) AppColors.GreenSurface else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = bagSamplingLocal,
                                onClick = { bagSamplingLocal = true },
                                colors = RadioButtonDefaults.colors(selectedColor = AppColors.GreenPrimary)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Cân mẫu (Tùy chỉnh)", fontWeight = FontWeight.SemiBold)
                                Text("Cân X bao mẫu ra Y kg bì", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            }
                        }
                    }

                    if (bagSamplingLocal) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = sampleCountTextLocal,
                                onValueChange = { sampleCountTextLocal = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Số bao mẫu") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = lockedAwareTextFieldColors()
                            )
                            OutlinedTextField(
                                value = sampleWeightTextLocal,
                                onValueChange = {
                                    sampleWeightTextLocal = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                                        .replace(',', '.')
                                },
                                label = { Text("Tổng kg mẫu") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = lockedAwareTextFieldColors()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Bố cục bàn phím cân lúa
            SettingsCardBox {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Bàn phím nhập cân",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "Thay đổi cỡ chữ và phím nhập trên màn hình cân lúa.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Standard Mode
                        Surface(
                            onClick = { inputModeLocal = "SMALL" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (inputModeLocal == "SMALL") AppColors.GreenSurface else Color.Transparent,
                            border = BorderStroke(1.dp, if (inputModeLocal == "SMALL") AppColors.GreenPrimary else AppColors.Divider),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                RadioButton(
                                    selected = inputModeLocal == "SMALL",
                                    onClick = { inputModeLocal = "SMALL" },
                                    colors = RadioButtonDefaults.colors(selectedColor = AppColors.GreenPrimary)
                                )
                                Text("Bàn phím nhỏ", fontWeight = FontWeight.SemiBold)
                                Text("Phím nhỏ gọn", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            }
                        }

                        // Large Mode
                        Surface(
                            onClick = { inputModeLocal = "LARGE" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (inputModeLocal == "LARGE") AppColors.GreenSurface else Color.Transparent,
                            border = BorderStroke(1.dp, if (inputModeLocal == "LARGE") AppColors.GreenPrimary else AppColors.Divider),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                RadioButton(
                                    selected = inputModeLocal == "LARGE",
                                    onClick = { inputModeLocal = "LARGE" },
                                    colors = RadioButtonDefaults.colors(selectedColor = AppColors.GreenPrimary)
                                )
                                Text("Bàn phím to", fontWeight = FontWeight.SemiBold)
                                Text("Nút bấm lớn dễ ấn", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
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
