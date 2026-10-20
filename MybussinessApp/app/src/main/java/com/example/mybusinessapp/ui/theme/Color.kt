package com.example.mybusinessapp.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==================================================================
// 🌅 1. THEME: DEEP EVENING SUNBURST
// ==================================================================
object SunburstTheme {
    val BgDeep = Color(0xFF0F0518)
    val BgTwilight = Color(0xFF2D0828)
    val Surface = Color(0xFF1A0B2E)

    val SolarOrange = Color(0xFFFF4D4D)
    val HorizonGold = Color(0xFFFFB74D)
    val DeepMagenta = Color(0xFFC026D3)
    val ElectricBlue = Color(0xFF4F46E5)

    val TextHigh = Color(0xFFFFE4E1)
    val TextMed = Color(0xFFD8B4E2)
}

// ==================================================================
// 🎨 2. THEME GRADIENTS
// ==================================================================
val GradientBurst = Brush.linearGradient(
    colors = listOf(SunburstTheme.SolarOrange, SunburstTheme.DeepMagenta)
)
val GradientSunset = Brush.verticalGradient(
    colors = listOf(SunburstTheme.HorizonGold, SunburstTheme.SolarOrange)
)
val GradientGlassBorder = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.3f),
        SunburstTheme.SolarOrange.copy(alpha = 0.5f),
        SunburstTheme.DeepMagenta.copy(alpha = 0.3f)
    )
)
