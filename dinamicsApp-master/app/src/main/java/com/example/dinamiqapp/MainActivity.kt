package com.example.dinamiqapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.dinamiqapp.data.ScaleConverter
import com.example.dinamiqapp.data.SettingsRepository
import com.example.dinamiqapp.navigation.AppNavGraph
import com.example.dinamiqapp.ui.theme.DynamicsAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dins de MainActivity.onCreate()
        val repository = SettingsRepository(this)
        lifecycleScope.launch {
            ScaleConverter.minDb = repository.appMinDb.first()
            ScaleConverter.maxDb = repository.appMaxDb.first()
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DynamicsAppTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}