package com.example.cridar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────
// Dynamic level colours (light + dark variants)
// ──────────────────────────────────────────────

data class DynamicColors(
    val background: Color,
    val onBackground: Color,  // text / symbol colour
    val ring: Color
)

object DynamicsColorPalette {
    // PP – soft teal
    val PP_LIGHT = DynamicColors(Color(0xFFE1F5EE), Color(0xFF085041), Color(0xFF5DCAA5))
    val PP_DARK  = DynamicColors(Color(0xFF085041), Color(0xFF9FE1CB), Color(0xFF5DCAA5))

    // P – soft blue
    val P_LIGHT  = DynamicColors(Color(0xFFE6F1FB), Color(0xFF0C447C), Color(0xFF85B7EB))
    val P_DARK   = DynamicColors(Color(0xFF0C447C), Color(0xFFB5D4F4), Color(0xFF85B7EB))

    // MF – amber
    val MF_LIGHT = DynamicColors(Color(0xFFFAEEDA), Color(0xFF412402), Color(0xFFEF9F27))
    val MF_DARK  = DynamicColors(Color(0xFF633806), Color(0xFFFAC775), Color(0xFFEF9F27))

    // F – coral
    val F_LIGHT  = DynamicColors(Color(0xFFFAECE7), Color(0xFF4A1B0C), Color(0xFFD85A30))
    val F_DARK   = DynamicColors(Color(0xFF712B13), Color(0xFFF5C4B3), Color(0xFFD85A30))

    // FF – red
    val FF_LIGHT = DynamicColors(Color(0xFFFCEBEB), Color(0xFF501313), Color(0xFFE24B4A))
    val FF_DARK  = DynamicColors(Color(0xFF791F1F), Color(0xFFF7C1C1), Color(0xFFE24B4A))
}

// App colour scheme
private val LightColors = lightColorScheme(
    primary   = Color(0xFF185FA5),
    background = Color(0xFFF6F6F4),
    surface    = Color(0xFFFFFFFF),
    onSurface  = Color(0xFF1A1A18),
)
private val DarkColors = darkColorScheme(
    primary    = Color(0xFF85B7EB),
    background = Color(0xFF111110),
    surface    = Color(0xFF1E1E1C),
    onSurface  = Color(0xFFDEDDD6),
)

@Composable
fun DynamicsAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}