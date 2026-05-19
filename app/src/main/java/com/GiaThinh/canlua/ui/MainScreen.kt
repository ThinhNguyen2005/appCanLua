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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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

    // Title theo tab/route — riêng AI Chat dùng tên brand thay cho label tab.
    val topBarTitle = when (currentRoute) {
        BottomNavItem.AI_CHAT.route -> "Trợ Lý Khuyến Nông"
        "settings" -> "Cài Đặt"
        "sync_status" -> "Trạng Thái Đồng Bộ"
        else -> currentTab?.label ?: "Cân Lúa"
    }

    // Subtitle hiện chỉ dành cho AI Chat
    val topBarSubtitle = if (currentRoute == BottomNavItem.AI_CHAT.route) "Được hỗ trợ bởi Gemini" else null

    val showTopBar = currentRoute in navItems.map { it.route }

    // Drawer state cho AI Chat (lifted lên đây để TopBar có thể mở drawer).
    val aiChatDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Column {
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
                        if (currentRoute == BottomNavItem.AI_CHAT.route) {
                            IconButton(onClick = {
                                scope.launch { aiChatDrawerState.open() }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Mở danh sách phiên chat"
                                )
                            }
                        }
                    },
                    actions = {
                        if (isOnTabScreen) { // Show Settings icon on any main tab for easy access
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentRoute == item.route

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppColors.GreenPrimary,
                                selectedTextColor = AppColors.GreenPrimary,
                                unselectedIconColor = AppColors.TextSecondary,
                                unselectedTextColor = AppColors.TextSecondary,
                                indicatorColor = AppColors.GreenSurface
                            )
                        )
                    }
                }
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

            // Main content
            Box(modifier = Modifier.fillMaxSize()) {
                AppNavHost(
                    navController = navController,
                    startDestination = navItems.first().route,
                    modifier = Modifier.fillMaxSize(),
                    aiChatDrawerState = aiChatDrawerState
                )
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
