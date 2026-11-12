package com.example.coordinategrabber.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ExtendedColors(
    val customButtonBackground: Color,
    val customButtonText: Color,
    val customButtonAltBackground: Color,
    val cancelButtonBackground:Color,
    val cancelButtonText:Color
)

val LightExtendedColors = ExtendedColors(
    customButtonBackground = Color(0xFF8C5A38),
    customButtonText = Color(0xFFFFF2E6),
    customButtonAltBackground = Color(0xFF9C8267),
    cancelButtonBackground = Color(0xFF570718),
    cancelButtonText = Color(0xFFFFF7F7)
)

val DarkExtendedColors = ExtendedColors(
    customButtonBackground = Color(0xFFD6B58A),
    customButtonText = Color(0xFF332416),
    customButtonAltBackground = Color(0xFF594331),
    cancelButtonBackground = Color(0xFF570718),
    cancelButtonText = Color(0xFFFFF7F7)
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
