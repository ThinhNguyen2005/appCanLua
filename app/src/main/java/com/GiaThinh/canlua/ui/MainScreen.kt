package com.GiaThinh.canlua.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.GiaThinh.canlua.ui.component.BottomBarItemSpec
import com.GiaThinh.canlua.ui.component.ModernBottomBar
import com.GiaThinh.canlua.ui.component.OfflineStatusBanner
import com.GiaThinh.canlua.ui.component.weight.HelpBottomSheet
import com.GiaThinh.canlua.ui.component.weight.WeighOptionsSheet
import com.GiaThinh.canlua.ui.navigation.AppNavHost
import com.GiaThinh.canlua.ui.navigation.BottomNavItem
import com.GiaThinh.canlua.ui.theme.AppColors
import com.GiaThinh.canlua.ui.viewmodel.SettingsViewModel
import com.GiaThinh.canlua.repository.WeighDefaults
import kotlinx.coroutines.launch

/**
 * MainScreen — Shell chính chứa TopBar + BottomNavigationBar + Content.
 * Đây là composable gốc cho user đã đăng nhập.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(deeplinkCardId: String? = null) {
    val navController = rememberNavController()
    var currentDeeplinkCardId by remember(deeplinkCardId) { mutableStateOf(deeplinkCardId) }
    
    // Tự động đo hiệu năng, thời gian tải màn hình, và khung hình cho mọi màn hình chính
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val route = destination.route
            if (route != null) {
                com.GiaThinh.canlua.util.PerformanceTracker.onScreenChanged(route)
            }
        }
    }

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute by remember {
        derivedStateOf { navBackStackEntry.value?.destination?.route }
    }

    val profileViewModel: com.GiaThinh.canlua.ui.viewmodel.ProfileViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
    val profile by profileViewModel.profile.collectAsStateWithLifecycle(initialValue = null)
    val isTrader = profile?.role == "TRADER"

    val context = LocalContext.current
    var isOffline by remember { mutableStateOf(!isNetworkAvailable(context)) }

    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOffline = !isNetworkAvailable(context)
            }

            override fun onLost(network: Network) {
                isOffline = !isNetworkAvailable(context)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                isOffline = !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                        !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        isOffline = !isNetworkAvailable(context)

        onDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    val feedbackViewModel: com.GiaThinh.canlua.ui.viewmodel.FeedbackViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
    val unreadFeedbackCount by feedbackViewModel.unreadCount.collectAsStateWithLifecycle(initialValue = 0)
    val hasUnreadFeedback = unreadFeedbackCount > 0
    val showWeighOptionsSheet = remember { mutableStateOf(false) }
    val showHelpSheet = remember { mutableStateOf(false) }
    
    // Determine nav items based on role
    val navItems = if (isTrader) BottomNavItem.traderNavItems else BottomNavItem.farmerNavItems

    val bottomBarItems = remember(navItems) {
        navItems.map { nav ->
            BottomBarItemSpec(
                route = nav.route,
                icon = nav.icon,
                selectedIcon = nav.selectedIcon,
                label = "",
                labelRes = nav.labelRes
            )
        }
    }

    // Xác định tab hiện tại
    val currentTab = navItems.find { it.route == currentRoute }
    val isOnTabScreen = currentTab != null

    // Ẩn BottomBar khi keyboard đang mở — quan trọng cho AI Chat,
    // nếu không input bar sẽ bị đẩy lên thêm ~80dp (= chiều cao navBar)
    // do imePadding cộng dồn với paddingValues của Scaffold.
    val isImeVisible = WindowInsets.isImeVisible

    // Các route con mà vẫn hiển thị bottom bar (detail, weight input...)
    val scrollVisible by com.GiaThinh.canlua.ui.util.BottomBarVisibility.visible.collectAsStateWithLifecycle()
    val showBottomBar = (isOnTabScreen || currentRoute in listOf("sync_status")) && !isImeVisible && scrollVisible

    // Title theo tab/route — riêng AI Chat đổi theo audience để truyền tải đúng identity của bot.
    val defaultScaleTitle = stringResource(com.GiaThinh.canlua.R.string.nav_scale)
    val topBarTitleRes = remember(currentRoute, isTrader, currentTab) {
        when (currentRoute) {
            BottomNavItem.AI_CHAT.route ->
                if (isTrader) com.GiaThinh.canlua.R.string.topbar_ai_trader else com.GiaThinh.canlua.R.string.topbar_ai_farmer
            "settings" -> com.GiaThinh.canlua.R.string.topbar_settings
            "sync_status" -> com.GiaThinh.canlua.R.string.topbar_sync_status
            "trader_transactions" -> com.GiaThinh.canlua.R.string.topbar_trader_transactions
            else -> currentTab?.labelRes
        }
    }
    val topBarTitle = if (topBarTitleRes != null) stringResource(topBarTitleRes) else defaultScaleTitle

    // Subtitle — AI Chat hiện brand, các route khác ẩn.
    val topBarSubtitleRes = remember(currentRoute) {
        when (currentRoute) {
            BottomNavItem.AI_CHAT.route -> com.GiaThinh.canlua.R.string.topbar_ai_subtitle
            else -> null
        }
    }
    val topBarSubtitle = if (topBarSubtitleRes != null) stringResource(topBarSubtitleRes) else null

    val showTopBar = currentRoute in navItems.map { it.route } || currentRoute == "trader_transactions"

    // Drawer state cho AI Chat (lifted lên đây để TopBar có thể mở drawer).
    val aiChatDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Mixed scroll behavior — pin TopBar ở các tab giao dịch (Cân Lúa) và Profile
    // để tránh nhảy ẩn-hiện khi tay dính nước scroll vô tình. Các tab đọc dài
    // (Market/Dashboard/AI Chat) dùng enterAlways để thu hồi không gian.
    // Tạo scrollBehavior 1 lần, persist qua mọi recomposition.
    // NẾU KHÔNG có remember → mỗi recomposition tạo instance MỚI →
    // scroll state bị reset → TopBar nhấp nháy (flicker).
    val rawPinnedBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val rawEnterAlwaysBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val pinnedRoutes = listOf(
        BottomNavItem.SCALE.route,
        BottomNavItem.ACCOUNT.route,
        BottomNavItem.TRADER_PROFILE.route,
        "trader_transactions"
    )
    val scrollBehavior = remember(currentRoute) {
        if (currentRoute in pinnedRoutes) rawPinnedBehavior else rawEnterAlwaysBehavior
    }

    Scaffold(
        modifier = if (showTopBar) {
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            Modifier
        },
        topBar = {
            if (showTopBar) {
                CenterAlignedTopAppBar(
                    scrollBehavior = scrollBehavior,
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = topBarTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (topBarSubtitle != null) {
                                Text(
                                    text = topBarSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.TextHint
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        when (currentRoute) {
                            BottomNavItem.AI_CHAT.route -> IconButton(onClick = {
                                scope.launch { aiChatDrawerState.open() }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = stringResource(com.GiaThinh.canlua.R.string.content_open_chat_sessions)
                                )
                            }

                            "trader_transactions" -> IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(com.GiaThinh.canlua.R.string.content_back)
                                )
                            }

                            BottomNavItem.SCALE.route
                                // Trang Cân Lúa: nút Trợ giúp & Hướng dẫn ở trái.
                                -> IconButton(onClick = {
                                showHelpSheet.value = true
                            }) {
                                Box {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                                        contentDescription = stringResource(com.GiaThinh.canlua.R.string.help_sheet_open_content)
                                    )
                                    if (hasUnreadFeedback) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(AppColors.Error, CircleShape)
                                                .align(Alignment.TopEnd)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    actions = {
                        // Tab Cân Lúa: nút Tune chỉnh default 3 mode cân (kg/%, A/B, SMALL/LARGE)
                        // áp cho mọi phiếu mới tạo.
                        if (currentRoute == BottomNavItem.SCALE.route) {
                            IconButton(onClick = { showWeighOptionsSheet.value = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = stringResource(com.GiaThinh.canlua.R.string.weigh_options_icon_content)
                                )
                            }
                        }
                        // Trader ở tab Cân Lúa → icon QR scan để verify giao dịch nhanh.
                        if (isTrader && currentRoute == BottomNavItem.SCALE.route) {
                            IconButton(onClick = {
                                navController.navigate("qr_scan")
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = stringResource(com.GiaThinh.canlua.R.string.content_scan_qr)
                                )
                            }
                        }
                        // Settings chỉ hiện ở tab Hồ sơ — các tab khác giữ topbar tối giản.
                        val isProfileTab = currentRoute == BottomNavItem.ACCOUNT.route ||
                                currentRoute == BottomNavItem.TRADER_PROFILE.route
                        if (isProfileTab) {
                            IconButton(onClick = {
                                navController.navigate("settings")
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = stringResource(com.GiaThinh.canlua.R.string.content_settings)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = paddingValues.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                )
        ) {
            // Offline banner
            OfflineStatusBanner(isOffline = isOffline)

            // Main content + BottomBar overlay lơ lửng (không chiếm slot bottomBar
            // của Scaffold để tránh "dải solid" che nội dung dưới capsule).
            Box(modifier = Modifier.fillMaxSize()) {
                AppNavHost(
                    navController = navController,
                    startDestination = navItems.first().route,
                    modifier = Modifier.fillMaxSize(),
                    aiChatDrawerState = aiChatDrawerState,
                    deeplinkCardId = currentDeeplinkCardId,
                    onDeeplinkConsumed = { currentDeeplinkCardId = null }
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = showBottomBar,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ModernBottomBar(
                        items = bottomBarItems,
                        currentRoute = currentRoute,
                        onItemClick = { item ->
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showWeighOptionsSheet.value) {
        WeighOptionsSheetWrapper(
            onDismiss = { showWeighOptionsSheet.value = false }
        )
    }

    if (showHelpSheet.value) {
        HelpBottomSheet(
            hasUnreadFeedback = hasUnreadFeedback,
            onFeedbackClick = { navController.navigate("feedback") },
            onDismiss = { showHelpSheet.value = false }
        )
    }
}

@Composable
private fun WeighOptionsSheetWrapper(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
) {
    val weighDefaults by viewModel.weighDefaults.collectAsStateWithLifecycle()
    WeighOptionsSheet(
        impurityIsPercent = weighDefaults.impurityIsPercent,
        bagMethodIsSampling = weighDefaults.bagMethodIsSampling,
        bagSampleCount = weighDefaults.bagSampleCount,
        bagSampleTotalWeight = weighDefaults.bagSampleTotalWeight,
        weightInputMode = weighDefaults.weightInputMode,
        onDismiss = onDismiss,
        onSave = { impurityPct, bagSampling, sampleCount, sampleWeight, inputMode ->
            viewModel.setWeighDefaults(
                WeighDefaults(
                    impurityIsPercent = impurityPct,
                    bagMethodIsSampling = bagSampling,
                    bagSampleCount = sampleCount,
                    bagSampleTotalWeight = sampleWeight,
                    weightInputMode = inputMode
                )
            )
            onDismiss()
        }
    )
}

private fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
