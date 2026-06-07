package com.example.cridar.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Listening : Screen("listening")
    object Shouting : Screen("shouting")
    object Settings : Screen("settings")
}