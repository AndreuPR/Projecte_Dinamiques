package com.example.cridar.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.cridar.ui.screens.listening.ListeningScreen
import com.example.cridar.ui.screens.shouting.ShoutingScreen
import com.example.cridar.ui.screens.welcome.WelcomeScreen
import com.example.shoutdetector.ui.screens.settings.SettingsScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Welcome.route) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onStart = { navController.navigate(Screen.Listening.route) },
                onSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Listening.route) {
            ListeningScreen(
                onShoutDetected = {
                    navController.navigate(Screen.Shouting.route) {
                        popUpTo(Screen.Listening.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Shouting.route) {
            ShoutingScreen(
                onApologize = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onRetry = {
                    navController.navigate(Screen.Listening.route) {
                        popUpTo(Screen.Listening.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}