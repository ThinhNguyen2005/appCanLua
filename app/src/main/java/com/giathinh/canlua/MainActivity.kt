package com.giathinh.canlua

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.navDeepLink
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.giathinh.canlua.data.model.AppThemeMode
import com.giathinh.canlua.data.model.FontScale
import com.giathinh.canlua.ui.MainScreen
import com.giathinh.canlua.ui.screen.AppSplashScreen
import com.giathinh.canlua.ui.screen.AuthScreen
import com.giathinh.canlua.ui.screen.ProfileSetupScreen
import com.giathinh.canlua.ui.screen.RoleRequestScreen
import com.giathinh.canlua.ui.theme.CanLuaTheme
import com.giathinh.canlua.ui.viewmodel.AuthViewModel
import com.giathinh.canlua.ui.viewmodel.InitViewModel
import com.giathinh.canlua.util.LocaleUtil
import com.giathinh.canlua.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    // Tạo InitViewModel trước setContent để dùng với setKeepOnScreenCondition.
    // @AndroidEntryPoint đã override defaultViewModelProviderFactory → Hilt factory.
    private lateinit var initViewModel: InitViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() PHẢI gọi trước super.onCreate().
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Request all runtime permissions at once on app launch
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO
            )
        )

        // Khởi tạo InitViewModel sau super() (Hilt đã inject xong).
        initViewModel = ViewModelProvider(this)[InitViewModel::class.java]

        // Giữ native splash cho đến khi Room DB đã warm-up xong.
        // Condition được kiểm tra mỗi frame — trả false → splash exit.
        splashScreen.setKeepOnScreenCondition { !initViewModel.isDataReady.value }

        enableEdgeToEdge()
        com.giathinh.canlua.util.PerformanceTracker.setActivity(this)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()
            val fontScale by settingsViewModel.fontScale.collectAsStateWithLifecycle(FontScale.NORMAL)
            val appThemeMode by settingsViewModel.appThemeMode.collectAsStateWithLifecycle(AppThemeMode.AUTO)
            val language by settingsViewModel.language.collectAsStateWithLifecycle()
            val authState by authViewModel.uiState.collectAsStateWithLifecycle()
            val isGuestMode by settingsViewModel.isGuestMode.collectAsStateWithLifecycle(initialValue = false)
            // isDataReady đã true khi native splash exit; subscribe ở đây để trigger
            // LaunchedEffect khi trạng thái thay đổi (edge case: auth nhanh hơn DB).
            val isDataReady by initViewModel.isDataReady.collectAsStateWithLifecycle()
            LocaleUtil.applyLanguage(this, language)

            CanLuaTheme(appThemeMode = appThemeMode, fontScale = fontScale) {
                com.giathinh.canlua.ui.feedback.AppToastHost {
                    val rootNavController = rememberNavController()

                    // startDest chờ cả hai: auth state xác định + DB đã warm-up.
                    val startDest = when {
                        isGuestMode -> "main?cardId={cardId}"
                        !authState.isSignedIn -> "login"
                        authState.needsProfileSetup == null || !isDataReady -> "splash"
                        authState.needsProfileSetup == true -> "profile_setup"
                        else -> "main?cardId={cardId}"
                    }

                    // Khi bất kỳ điều kiện nào thay đổi, điều hướng đến đúng màn.
                    LaunchedEffect(authState.isSignedIn, authState.needsProfileSetup, isDataReady, isGuestMode) {
                        val currentRoute = rootNavController.currentDestination?.route
                        if (currentRoute == "role_request") return@LaunchedEffect

                        val target = when {
                            isGuestMode -> "main?cardId={cardId}"
                            !authState.isSignedIn -> "login"
                            authState.needsProfileSetup == null || !isDataReady -> null // chờ
                            authState.needsProfileSetup == true -> "profile_setup"
                            else -> "main?cardId={cardId}"
                        } ?: return@LaunchedEffect

                        val isTargetMain = target.startsWith("main")
                        val isCurrentMain = currentRoute?.startsWith("main") == true
                        if (currentRoute != target && !(isTargetMain && isCurrentMain)) {
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
                            AuthScreen(
                                onSuccess = { /* no-op */ },
                                onSkipLogin = { settingsViewModel.setGuestMode(true) }
                            )
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
        com.giathinh.canlua.util.PerformanceTracker.clearActivity()
        super.onDestroy()
    }
}
