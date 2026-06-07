package com.example.dinamiqapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.dinamiqapp.ui.screens.appsettings.AppSettingsScreen
import com.example.dinamiqapp.ui.screens.hearing.HearingCalcScreen
import com.example.dinamiqapp.ui.screens.measurement.MeasurementScreen
import com.example.dinamiqapp.ui.screens.meter.MeterScreen
import com.example.dinamiqapp.ui.screens.settings.SettingsMenuScreen
import com.example.dinamiqapp.ui.screens.settings.SettingsScreen
import com.example.dinamiqapp.ui.screens.welcome.WelcomeScreen
import com.example.dinamiqapp.ui.voices.VoiceManagementScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToMeasurement = { navController.navigate(Screen.Measurement.route) },
                onNavigateToMeter       = { navController.navigate(Screen.Meter.route) },
                onNavigateToSettings    = { navController.navigate(Screen.SettingsMenu.route) },
                onNavigateToVoices      = { navController.navigate(Screen.VoiceManager.route) }
            )
        }
        composable(Screen.Measurement.route) {
            MeasurementScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Meter.route) {
            MeterScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SettingsMenu.route) {
            SettingsMenuScreen(
                onNavigateToDynamicSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToHearingCalc     = { navController.navigate(Screen.HearingCalc.route) },
                onNavigateToAppSettings     = { navController.navigate(Screen.AppSettings.route) },
                onNavigateToVoices          = { navController.navigate(Screen.VoiceManager.route) },
                onBack                      = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.HearingCalc.route) {
            HearingCalcScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.AppSettings.route) {
            AppSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.VoiceManager.route) {
            VoiceManagementScreen(onBack = { navController.popBackStack() })
        }
    }
}