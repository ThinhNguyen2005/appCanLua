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

    private lateinit var initViewModel: InitViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO
            )
        )

        initViewModel = ViewModelProvider(this)[InitViewModel::class.java]
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
            val isDataReady by initViewModel.isDataReady.collectAsStateWithLifecycle()
            LocaleUtil.applyLanguage(this, language)

            CanLuaTheme(appThemeMode = appThemeMode, fontScale = fontScale) {
                com.giathinh.canlua.ui.feedback.AppToastHost {
                    val rootNavController = rememberNavController()

                    val startDest = when {
                        isGuestMode -> "main?cardId={cardId}"
                        !authState.isSignedIn -> "login"
                        authState.needsProfileSetup == null || !isDataReady -> "splash"
                        authState.needsProfileSetup == true -> "profile_setup"
                        else -> "main?cardId={cardId}"
                    }

                    LaunchedEffect(authState.isSignedIn, authState.needsProfileSetup, isDataReady, isGuestMode) {
                        val currentRoute = rootNavController.currentDestination?.route

                        val target = when {
                            isGuestMode -> "main?cardId={cardId}"
                            !authState.isSignedIn -> "login"
                            authState.needsProfileSetup == null || !isDataReady -> null
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
