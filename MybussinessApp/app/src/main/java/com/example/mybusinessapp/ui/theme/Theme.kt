package com.example.mybusinessapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun MyBusinessAppTheme(
    darkTheme: Boolean = true,
    themeAccent: ThemeAccent = ThemeAccent.SUNBURST_AMBER,
    content: @Composable () -> Unit
) {
    val colorScheme = buildColorScheme(themeAccent, darkTheme)

    val businessSuiteColors = BusinessSuiteColors(
        primary = themeAccent.primaryColor,
        secondary = themeAccent.secondaryColor,
        tertiary = themeAccent.tertiaryColor,
        background = colorScheme.background,
        surface = colorScheme.surface,
        textHigh = if (darkTheme) Color(0xFFFFE4E1) else Color(0xFF1C1B1F),
        textMed = if (darkTheme) Color(0xFFD8B4E2) else Color(0xFF49454F),
        success = Color(0xFF00E676),
        warning = Color(0xFFFFB300),
        error = Color(0xFFFF334B)
    )
    val businessSuiteShapes = BusinessSuiteShapes()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = Color.Transparent.toArgb()
                it.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalBusinessSuiteColors provides businessSuiteColors,
        LocalBusinessSuiteShapes provides businessSuiteShapes
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
