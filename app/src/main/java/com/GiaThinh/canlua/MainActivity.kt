package com.GiaThinh.canlua

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.navDeepLink
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.GiaThinh.canlua.data.model.FontScale
import com.GiaThinh.canlua.ui.MainScreen
import com.GiaThinh.canlua.ui.screen.AppSplashScreen
import com.GiaThinh.canlua.ui.screen.AuthScreen
import com.GiaThinh.canlua.ui.screen.ProfileSetupScreen
import com.GiaThinh.canlua.ui.screen.RoleRequestScreen
import com.GiaThinh.canlua.ui.theme.CanLuaTheme
import com.GiaThinh.canlua.ui.viewmodel.AuthViewModel
import com.GiaThinh.canlua.ui.viewmodel.InitViewModel
import com.GiaThinh.canlua.util.LocaleUtil
import com.GiaThinh.canlua.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Tạo InitViewModel trước setContent để dùng với setKeepOnScreenCondition.
    // @AndroidEntryPoint đã override defaultViewModelProviderFactory → Hilt factory.
    private lateinit var initViewModel: InitViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() PHẢI gọi trước super.onCreate().
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Khởi tạo InitViewModel sau super() (Hilt đã inject xong).
        initViewModel = ViewModelProvider(this)[InitViewModel::class.java]

        // Giữ native splash cho đến khi Room DB đã warm-up xong.
        // Condition được kiểm tra mỗi frame — trả false → splash exit.
        splashScreen.setKeepOnScreenCondition { !initViewModel.isDataReady.value }

        enableEdgeToEdge()
        com.GiaThinh.canlua.util.PerformanceTracker.setActivity(this)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()
            val fontScale by settingsViewModel.fontScale.collectAsStateWithLifecycle(FontScale.NORMAL)
            val language by settingsViewModel.language.collectAsStateWithLifecycle()
            val authState by authViewModel.uiState.collectAsStateWithLifecycle()
            // isDataReady đã true khi native splash exit; subscribe ở đây để trigger
            // LaunchedEffect khi trạng thái thay đổi (edge case: auth nhanh hơn DB).
            val isDataReady by initViewModel.isDataReady.collectAsStateWithLifecycle()
            LocaleUtil.applyLanguage(this, language)

            CanLuaTheme(fontScale = fontScale) {
                com.GiaThinh.canlua.ui.feedback.AppToastHost {
                    val rootNavController = rememberNavController()

                    // startDest chờ cả hai: auth state xác định + DB đã warm-up.
                    val startDest = when {
                        !authState.isSignedIn -> "login"
                        authState.needsProfileSetup == null || !isDataReady -> "splash"
                        authState.needsProfileSetup == true -> "profile_setup"
                        else -> "main"
                    }

                    // Khi bất kỳ điều kiện nào thay đổi, điều hướng đến đúng màn.
                    LaunchedEffect(authState.isSignedIn, authState.needsProfileSetup, isDataReady) {
                        val currentRoute = rootNavController.currentDestination?.route
                        if (currentRoute == "role_request") return@LaunchedEffect

                        val target = when {
                            !authState.isSignedIn -> "login"
                            authState.needsProfileSetup == null || !isDataReady -> null // chờ
                            authState.needsProfileSetup == true -> "profile_setup"
                            else -> "main"
                        } ?: return@LaunchedEffect

                        if (currentRoute != target) {
                            rootNavController.navigate(target) {
                                popUpTo(rootNavController.graph.id) { inclusive = true }
                            }
                        }
                    }

                    NavHost(
                        navController = rootNavController,
                        startDestination = startDest
                    ) {
                        composable("splash") {
                            // Màn hình chờ có thương hiệu — hiển thị trong khoảng thời gian
                            // auth đang xác định HOẶC Room chưa warm-up xong (thường < 300ms).
                            AppSplashScreen()
                        }

                        composable("login") {
                            AuthScreen(onSuccess = { /* no-op */ })
                        }

                        composable("profile_setup") {
                            ProfileSetupScreen(
                                navController = rootNavController,
                                onComplete = { authViewModel.markProfileCompleted() }
                            )
                        }

                        composable("role_request") {
                            RoleRequestScreen(navController = rootNavController)
                        }

                        composable(
                            route = "main?cardId={cardId}",
                            deepLinks = listOf(
                                navDeepLink {
                                    uriPattern = "https://canluavn.web.app/share/{cardId}"
                                },
                                navDeepLink {
                                    uriPattern = "https://canluavn.firebaseapp.com/share/{cardId}"
                                }
                            )
                        ) { backStackEntry ->
                            val cardId = backStackEntry.arguments?.getString("cardId")
                            MainScreen(deeplinkCardId = cardId)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        com.GiaThinh.canlua.util.PerformanceTracker.clearActivity()
        super.onDestroy()
    }
}
