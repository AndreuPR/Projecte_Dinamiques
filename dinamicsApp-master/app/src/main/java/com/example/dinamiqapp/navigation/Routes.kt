package com.example.dinamiqapp.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Measurement : Screen("measurement")
    object Meter : Screen("meter")
    object SettingsMenu : Screen("settings_menu")
    object Settings : Screen("settings")   // sliders
    object HearingCalc : Screen("hearing_calc")
    object AppSettings   : Screen("app_settings")
    object VoiceManager  : Screen("voice_manager")
}