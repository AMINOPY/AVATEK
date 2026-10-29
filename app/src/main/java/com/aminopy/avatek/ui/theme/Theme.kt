package com.aminopy.avatek.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AvatekDarkScheme = darkColorScheme(
    primary = Color(0xFFD9B56D),
    onPrimary = Color(0xFF201707),
    secondary = Color(0xFF9E2638),
    onSecondary = Color.White,
    background = Color(0xFF0A0B0D),
    onBackground = Color(0xFFF5F2EC),
    surface = Color(0xFF17191E),
    onSurface = Color(0xFFF5F2EC)
)

@Composable
fun AvatekTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AvatekDarkScheme,
        content = content
    )
}
