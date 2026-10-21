package com.example.mybusinessapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ==================================================================
// 🎨 THEME ACCENTS ENUM
// ==================================================================
enum class ThemeAccent(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color
) {
    SUNBURST_AMBER(
        displayName = "Sunburst Amber",
        primaryColor = Color(0xFFFF4D4D),
        secondaryColor = Color(0xFFC026D3),
        tertiaryColor = Color(0xFFFFB74D)
    ),
    CYBER_VIOLET(
        displayName = "Cyber Violet",
        primaryColor = Color(0xFFA855F7),
        secondaryColor = Color(0xFFEC4899),
        tertiaryColor = Color(0xFF06B6D4)
    ),
    EMERALD_MATRIX(
        displayName = "Emerald Matrix",
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF14B8A6),
        tertiaryColor = Color(0xFF34D399)
    )
}

// ==================================================================
// 💎 CUSTOM DESIGN SYSTEM: BUSINESS SUITE COLORS & SHAPES
// ==================================================================
@Immutable
data class BusinessSuiteColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val textHigh: Color,
    val textMed: Color,
    val success: Color = Color(0xFF00E676),
    val warning: Color = Color(0xFFFFB300),
    val error: Color = Color(0xFFFF334B),
    val cardBorder: Color = Color.White.copy(alpha = 0.15f)
)

@Immutable
data class BusinessSuiteShapes(
    val cardCorner: Dp = 24.dp,
    val buttonCorner: Dp = 14.dp,
    val pillCorner: Dp = 12.dp
)

val LocalBusinessSuiteColors = staticCompositionLocalOf {
    BusinessSuiteColors(
        primary = ThemeAccent.SUNBURST_AMBER.primaryColor,
        secondary = ThemeAccent.SUNBURST_AMBER.secondaryColor,
        tertiary = ThemeAccent.SUNBURST_AMBER.tertiaryColor,
        background = Color(0xFF0F0518),
        surface = Color(0xFF1A0B2E),
        textHigh = Color(0xFFFFE4E1),
        textMed = Color(0xFFD8B4E2)
    )
}

val LocalBusinessSuiteShapes = staticCompositionLocalOf { BusinessSuiteShapes() }

object BusinessSuiteTheme {
    val colors: BusinessSuiteColors
        @Composable
        get() = LocalBusinessSuiteColors.current

    val shapes: BusinessSuiteShapes
        @Composable
        get() = LocalBusinessSuiteShapes.current
}

// Convert ThemeAccent to Material3 ColorScheme
fun buildColorScheme(accent: ThemeAccent, isDark: Boolean): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = accent.primaryColor,
            secondary = accent.secondaryColor,
            tertiary = accent.tertiaryColor,
            background = Color(0xFF0F0518),
            surface = Color(0xFF1A0B2E),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color(0xFF1A0B2E),
            onBackground = Color(0xFFFFE4E1),
            onSurface = Color(0xFFFFE4E1),
            onSurfaceVariant = Color(0xFFD8B4E2),
            error = Color(0xFFFF334B)
        )
    } else {
        lightColorScheme(
            primary = accent.primaryColor,
            secondary = accent.secondaryColor,
            tertiary = accent.tertiaryColor,
            background = Color(0xFFF9F6FE),
            surface = Color(0xFFFFFFFF),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color(0xFF1C1B1F),
            onSurface = Color(0xFF1C1B1F),
            onSurfaceVariant = Color(0xFF49454F),
            error = Color(0xFFB00020)
        )
    }
}

fun getAccentGradient(accent: ThemeAccent): Brush {
    return Brush.linearGradient(
        colors = listOf(accent.primaryColor, accent.secondaryColor)
    )
}
