package com.aminopy.avatek.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.aminopy.avatek.data.AccentTheme
import com.aminopy.avatek.data.UiSettings

@Immutable
data class AvatekPalette(
    val accent: Color,
    val accentDeep: Color,
    val secondary: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val text: Color = Color(0xFFF7F4EE),
    val muted: Color = Color(0xFFA9ACB5),
    val danger: Color = Color(0xFFFF5468)
)

fun paletteFor(theme: AccentTheme): AvatekPalette = when (theme) {
    AccentTheme.GOLD -> AvatekPalette(
        accent = Color(0xFFE2BE78), accentDeep = Color(0xFF8A6A2F), secondary = Color(0xFFB3263A),
        bgTop = Color(0xFF14100B), bgBottom = Color(0xFF07080A)
    )
    AccentTheme.CRIMSON -> AvatekPalette(
        accent = Color(0xFFFF5468), accentDeep = Color(0xFF8E1B2A), secondary = Color(0xFFFF9A6B),
        bgTop = Color(0xFF1A0C10), bgBottom = Color(0xFF08060A)
    )
    AccentTheme.AURORA -> AvatekPalette(
        accent = Color(0xFF5EEAD4), accentDeep = Color(0xFF0F766E), secondary = Color(0xFFA78BFA),
        bgTop = Color(0xFF0A1418), bgBottom = Color(0xFF05080C)
    )
    AccentTheme.EMERALD -> AvatekPalette(
        accent = Color(0xFF4ADE80), accentDeep = Color(0xFF166534), secondary = Color(0xFFFDE047),
        bgTop = Color(0xFF0A150E), bgBottom = Color(0xFF050906)
    )
    AccentTheme.VIOLET -> AvatekPalette(
        accent = Color(0xFFC084FC), accentDeep = Color(0xFF6B21A8), secondary = Color(0xFFF472B6),
        bgTop = Color(0xFF130B1C), bgBottom = Color(0xFF07050B)
    )
}

val LocalPalette = staticCompositionLocalOf { paletteFor(AccentTheme.GOLD) }
val LocalUi = staticCompositionLocalOf { UiSettings() }

/** Persian-first app: layout is forced right-to-left. */
@Composable
fun AvatekTheme(settings: UiSettings, content: @Composable () -> Unit) {
    val palette = paletteFor(settings.accent)
    val scheme = darkColorScheme(
        primary = palette.accent,
        onPrimary = Color(0xFF16110A),
        secondary = palette.secondary,
        background = palette.bgBottom,
        onBackground = palette.text,
        surface = palette.bgTop,
        onSurface = palette.text,
        surfaceVariant = Color(0x22FFFFFF),
        onSurfaceVariant = palette.muted,
        error = palette.danger
    )
    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalUi provides settings,
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
