package com.GiaThinh.canlua

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

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

                // Tính start destination dựa trên cả 2 flag.
                // Khi needsProfileSetup == null (đang load) → "splash" để tránh flash sai màn.
                val startDest = when {
                    !authState.isSignedIn -> "login"
                    authState.needsProfileSetup == null -> "splash"
                    authState.needsProfileSetup == true -> "profile_setup"
                    else -> "main"
                }

                // Khi state thay đổi (login mới, profile vừa save, signOut), điều hướng lại.
                LaunchedEffect(authState.isSignedIn, authState.needsProfileSetup) {
                    val target = when {
                        !authState.isSignedIn -> "login"
                        authState.needsProfileSetup == null -> null // chờ
                        authState.needsProfileSetup == true -> "profile_setup"
                        else -> "main"
                    } ?: return@LaunchedEffect

                    if (rootNavController.currentDestination?.route != target) {
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
                        // Loading screen ngắn trong khi AuthViewModel đọc Profile từ Room.
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    composable("login") {
                        AuthScreen(
                            // Không hard-code đích đến — LaunchedEffect ở trên sẽ điều hướng đúng
                            // dựa trên needsProfileSetup được AuthViewModel cập nhật sau sign-in.
                            onSuccess = { /* no-op */ }
                        )
                    }

                    composable("profile_setup") {
                        ProfileSetupScreen(
                            navController = rootNavController,
                            onComplete = {
                                // Sau khi save profile, refresh flag để LaunchedEffect đẩy vào main.
                                authViewModel.markProfileCompleted()
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