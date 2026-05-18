package com.GiaThinh.canlua

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.GiaThinh.canlua.data.model.FontScale
import com.GiaThinh.canlua.ui.MainScreen
import com.GiaThinh.canlua.ui.screen.AuthScreen
import com.GiaThinh.canlua.ui.screen.ProfileSetupScreen
import com.GiaThinh.canlua.ui.theme.CanLuaTheme
import com.GiaThinh.canlua.ui.viewmodel.AuthViewModel
import com.GiaThinh.canlua.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()
            val fontScale by settingsViewModel.fontScale.collectAsState(FontScale.NORMAL)
            val authState by authViewModel.uiState.collectAsState()

            CanLuaTheme(fontScale = fontScale) {
                val rootNavController = rememberNavController()
                val startDest = if (authState.isSignedIn) "main" else "login"

                // An toàn: Khi trạng thái đăng nhập thay đổi thành false (từ bất kỳ đâu),
                // Graph gốc sẽ tự động điều hướng về login và xoá toàn bộ lịch sử màn hình.
                LaunchedEffect(authState.isSignedIn) {
                    if (!authState.isSignedIn && rootNavController.currentDestination?.route != "login") {
                        rootNavController.navigate("login") {
                            popUpTo(rootNavController.graph.id) { inclusive = true }
                        }
                    }
                }

                NavHost(
                    navController = rootNavController,
                    startDestination = startDest
                ) {
                    composable("login") {
                        AuthScreen(
                            onSuccess = {
                                rootNavController.navigate("profile_setup") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("profile_setup") {
                        ProfileSetupScreen(
                            navController = rootNavController,
                            onComplete = {
                                rootNavController.navigate("main") {
                                    popUpTo("profile_setup") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("main") {
                        MainScreen()
                    }
                }
            }
        }
    }
}