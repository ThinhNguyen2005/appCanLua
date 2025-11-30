package com.GiaThinh.canlua

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.GiaThinh.canlua.ui.navigation.CanLuaNavigation
import com.GiaThinh.canlua.ui.theme.CanLuaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CanLuaTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    CanLuaNavigation(navController = navController)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    CanLuaTheme {
        val navController = rememberNavController()
        CanLuaNavigation(navController = navController)
    }
}