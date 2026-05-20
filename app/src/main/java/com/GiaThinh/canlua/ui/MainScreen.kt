package com.GiaThinh.canlua.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.GiaThinh.canlua.ui.navigation.AppNavHost
import com.GiaThinh.canlua.ui.navigation.BottomNavItem
import com.GiaThinh.canlua.ui.theme.AppColors
import kotlinx.coroutines.launch

/**
 * MainScreen — Shell chính chứa TopBar + BottomNavigationBar + Content.
 * Đây là composable gốc cho user đã đăng nhập.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    var isOffline by remember { mutableStateOf(!isNetworkAvailable(context)) }

    // Kiểm tra network định kỳ đơn giản
    DisposableEffect(Unit) {
        onDispose { }
    }

    val profileViewModel: com.GiaThinh.canlua.ui.viewmodel.ProfileViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
    val profile by profileViewModel.profile.collectAsState(initial = null)
    
    // Determine nav items based on role
    val isTrader = profile?.role == "TRADER"
    val navItems = if (isTrader) BottomNavItem.traderNavItems else BottomNavItem.farmerNavItems

    // Xác định tab hiện tại
    val currentTab = navItems.find { it.route == currentRoute }
    val isOnTabScreen = currentTab != null

    // Ẩn BottomBar khi keyboard đang mở — quan trọng cho AI Chat,
    // nếu không input bar sẽ bị đẩy lên thêm ~80dp (= chiều cao navBar)
    // do imePadding cộng dồn với paddingValues của Scaffold.
    val isImeVisible = WindowInsets.isImeVisible

    // Các route con mà vẫn hiển thị bottom bar (detail, weight input...)
    val showBottomBar = (isOnTabScreen || currentRoute in listOf("sync_status")) && !isImeVisible

    // Title theo tab/route — riêng AI Chat đổi theo audience để truyền tải đúng identity của bot.
    val topBarTitle = when (currentRoute) {
        BottomNavItem.AI_CHAT.route ->
            if (isTrader) "Chuyên Gia Thị Trường" else "Trợ Lý Khuyến Nông"
        "settings" -> "Cài Đặt"
        "sync_status" -> "Trạng Thái Đồng Bộ"
        "trader_transactions" -> "Sổ Giao Dịch"
        else -> currentTab?.label ?: "Cân Lúa"
    }

    // Subtitle — AI Chat hiện brand, các route khác ẩn.
    val topBarSubtitle = when (currentRoute) {
        BottomNavItem.AI_CHAT.route -> "Được hỗ trợ bởi Gemini"
        else -> null
    }

    val showTopBar = currentRoute in navItems.map { it.route } || currentRoute == "trader_transactions"

    // Drawer state cho AI Chat (lifted lên đây để TopBar có thể mở drawer).
    val aiChatDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Mixed scroll behavior — pin TopBar ở các tab giao dịch (Cân Lúa) và Profile
    // để tránh nhảy ẩn-hiện khi tay dính nước scroll vô tình. Các tab đọc dài
    // (Market/Dashboard/AI Chat) dùng enterAlways để thu hồi không gian.
    val pinnedBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val enterAlwaysBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val pinnedRoutes = listOf(
        BottomNavItem.SCALE.route,
        BottomNavItem.ACCOUNT.route,
        BottomNavItem.TRADER_PROFILE.route,
        "trader_transactions"
    )
    val scrollBehavior = if (currentRoute in pinnedRoutes) pinnedBehavior else enterAlwaysBehavior

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                        when {
                            currentRoute == BottomNavItem.AI_CHAT.route -> {
                                IconButton(onClick = {
                                    scope.launch { aiChatDrawerState.open() }
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = "Mở danh sách phiên chat"
                                    )
                                }
                            }
                            currentRoute == "trader_transactions" -> {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Quay lại"
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Trader ở tab Cân Lúa → icon QR scan để verify giao dịch nhanh.
                        if (isTrader && currentRoute == BottomNavItem.SCALE.route) {
                            IconButton(onClick = {
                                navController.navigate("qr_scan")
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = "Quét mã QR"
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
                                    contentDescription = "Cài đặt"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                    aiChatDrawerState = aiChatDrawerState
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = showBottomBar,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ModernBottomBar(
                        items = navItems.map { nav ->
                            BottomBarItemSpec(
                                route = nav.route,
                                icon = nav.icon,
                                selectedIcon = nav.selectedIcon,
                                label = nav.label
                            )
                        },
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
}

private fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
