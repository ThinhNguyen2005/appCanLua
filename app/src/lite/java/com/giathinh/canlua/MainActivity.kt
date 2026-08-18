package com.giathinh.canlua

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.canlua.data.model.AppThemeMode
import com.giathinh.canlua.data.model.FontScale
import com.giathinh.canlua.ui.MainScreen
import com.giathinh.canlua.ui.feedback.AppToastHost
import com.giathinh.canlua.ui.theme.CanLuaTheme
import com.giathinh.canlua.ui.viewmodel.InitViewModel
import com.giathinh.canlua.ui.viewmodel.SettingsViewModel
import com.giathinh.canlua.util.LocaleUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var initViewModel: InitViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        initViewModel = ViewModelProvider(this)[InitViewModel::class.java]
        splashScreen.setKeepOnScreenCondition { !initViewModel.isDataReady.value }

        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val fontScale by settingsViewModel.fontScale.collectAsStateWithLifecycle(FontScale.NORMAL)
            val appThemeMode by settingsViewModel.appThemeMode.collectAsStateWithLifecycle(AppThemeMode.AUTO)
            val language by settingsViewModel.language.collectAsStateWithLifecycle()
            LocaleUtil.applyLanguage(this, language)

            CanLuaTheme(appThemeMode = appThemeMode, fontScale = fontScale) {
                AppToastHost {
                    MainScreen()
                }
            }
        }
    }
}
