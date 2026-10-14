package com.giathinh.canlua

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.giathinh.canlua.ads.AdConsentManager
import com.giathinh.canlua.data.model.AppThemeMode
import com.giathinh.canlua.data.model.FontScale
import com.giathinh.canlua.ui.MainScreen
import com.giathinh.canlua.ui.feedback.AppToastHost
import com.giathinh.canlua.ui.screen.AppSplashScreen
import com.giathinh.canlua.ui.screen.AuthScreen
import com.giathinh.canlua.ui.screen.ProfileSetupScreen
import com.giathinh.canlua.ui.theme.CanLuaTheme
import com.giathinh.canlua.ui.viewmodel.AuthViewModel
import com.giathinh.canlua.ui.viewmodel.InitViewModel
import com.giathinh.canlua.ui.viewmodel.SettingsViewModel
import com.giathinh.canlua.util.LocaleUtil
import com.giathinh.canlua.util.PerformanceTracker
import dagger.hilt.android.AndroidEntryPoint

private const val ROOT_ROUTE_SPLASH = "splash"
private const val ROOT_ROUTE_LOGIN = "login"
private const val ROOT_ROUTE_PROFILE_SETUP = "profile_setup"
private const val ROOT_ROUTE_MAIN = "main?cardId={cardId}"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var initViewModel: InitViewModel
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        initViewModel = ViewModelProvider(this)[InitViewModel::class.java]
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Giữ Native Splash cho đến khi cả Room DB lẫn trạng thái Profile (nếu đã login) xác định xong.
        splashScreen.setKeepOnScreenCondition {
            val dataReady = initViewModel.isDataReady.value
            val authState = authViewModel.uiState.value
            val authReady = !authState.isSignedIn || authState.needsProfileSetup != null
            !(dataReady && authReady)
        }

        enableEdgeToEdge()
        PerformanceTracker.setActivity(this)
        AdConsentManager.gatherConsent(this)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val fontScale by settingsViewModel.fontScale.collectAsStateWithLifecycle(FontScale.NORMAL)
            val appThemeMode by settingsViewModel.appThemeMode.collectAsStateWithLifecycle(AppThemeMode.AUTO)
            val language by settingsViewModel.language.collectAsStateWithLifecycle()
            val authState by authViewModel.uiState.collectAsStateWithLifecycle()
            val isGuestMode by settingsViewModel.isGuestMode.collectAsStateWithLifecycle(initialValue = false)
            val isDataReady by initViewModel.isDataReady.collectAsStateWithLifecycle()
            LocaleUtil.applyLanguage(this, language)

            CanLuaTheme(appThemeMode = appThemeMode, fontScale = fontScale) {
                AppToastHost {
                    val rootNavController = rememberNavController()

                    // Cố định startDestination lần đầu tiên để NavHost KHÔNG bị dựng lại và không mất Deeplink
                    val initialDestination = remember {
                        when {
                            isGuestMode -> ROOT_ROUTE_MAIN
                            !authState.isSignedIn -> ROOT_ROUTE_LOGIN
                            authState.needsProfileSetup == null || !isDataReady -> ROOT_ROUTE_SPLASH
                            authState.needsProfileSetup == true -> ROOT_ROUTE_PROFILE_SETUP
                            else -> ROOT_ROUTE_MAIN
                        }
                    }

                    LaunchedEffect(authState.isSignedIn, authState.needsProfileSetup, isDataReady, isGuestMode) {
                        val currentRoute = rootNavController.currentDestination?.route

                        val target = when {
                            isGuestMode -> ROOT_ROUTE_MAIN
                            !authState.isSignedIn -> ROOT_ROUTE_LOGIN
                            authState.needsProfileSetup == null || !isDataReady -> null
                            authState.needsProfileSetup == true -> ROOT_ROUTE_PROFILE_SETUP
                            else -> ROOT_ROUTE_MAIN
                        } ?: return@LaunchedEffect

                        val isTargetMain = target.startsWith("main")
                        val isCurrentMain = currentRoute?.startsWith("main") == true
                        if (currentRoute != target && !(isTargetMain && isCurrentMain)) {
                            rootNavController.navigate(target) {
                                popUpTo(rootNavController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    NavHost(
                        navController = rootNavController,
                        startDestination = initialDestination
                    ) {
                        composable(ROOT_ROUTE_SPLASH) {
                            AppSplashScreen()
                        }

                        composable(ROOT_ROUTE_LOGIN) {
                            AuthScreen(
                                onSuccess = { /* Điều hướng tự động qua LaunchedEffect */ },
                                onSkipLogin = { settingsViewModel.setGuestMode(true) }
                            )
                        }

                        composable(ROOT_ROUTE_PROFILE_SETUP) {
                            ProfileSetupScreen(
                                navController = rootNavController,
                                onComplete = { authViewModel.markProfileCompleted() }
                            )
                        }

                        composable(
                            route = ROOT_ROUTE_MAIN,
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "https://canluavn.web.app/share/{cardId}" },
                                navDeepLink { uriPattern = "https://canluavn.firebaseapp.com/share/{cardId}" }
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
        PerformanceTracker.clearActivity()
        super.onDestroy()
    }
}