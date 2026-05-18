package com.GiaThinh.canlua.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

/**
 * MainScreen — Shell chính chứa TopBar + BottomNavigationBar + Content.
 * Đây là composable gốc cho user đã đăng nhập.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    val profileViewModel: com.GiaThinh.canlua.ui.viewmodel.ProfileViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val profile by profileViewModel.profile.collectAsState(initial = null)
    
    // Determine nav items based on role
    val isTrader = profile?.role == "TRADER"
    val navItems = if (isTrader) BottomNavItem.traderNavItems else BottomNavItem.farmerNavItems

    // Xác định tab hiện tại
    val currentTab = navItems.find { it.route == currentRoute }
    val isOnTabScreen = currentTab != null

    // Các route con mà vẫn hiển thị bottom bar (detail, weight input...)
    val showBottomBar = isOnTabScreen || currentRoute in listOf("sync_status")

    // Title theo tab/route
    val topBarTitle = currentTab?.label ?: when (currentRoute) {
        "settings" -> "Cài Đặt"
        "sync_status" -> "Trạng Thái Đồng Bộ"
        else -> "Cân Lúa"
    }

    // Ẩn TopBar trên các sub-screen có TopBar riêng
    val showTopBar = currentRoute in navItems.map { it.route }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = topBarTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
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
                    modifier = Modifier.fillMaxSize()
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
