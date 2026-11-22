package dev.podcast.nativeapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class CastflowColorTokens(
    val isDark: Boolean,
    val background: Color,
    val elevated: Color,
    val muted: Color,
    val text: Color,
    val secondaryText: Color,
    val accent: Color,
    val accentContainer: Color,
    val divider: Color,
    val dividerStrong: Color,
    val glassBackground: Color,
    val glassTint: Color,
    val controlTint: Color,
    val controlFill: Color,
    val choiceSelected: Color,
    val choiceBorder: Color,
    val glassBorder: Color,
    val glassOverlay: Color,
    val shadow: Color,
    val scrim: Color,
    val selection: Color,
    val placeholder: Color,
    val error: Color,
    val playerBackground: Color
)

private val LightColors = CastflowColorTokens(
    isDark = false,
    background = Color.White,
    elevated = Color.White,
    muted = Color(0xFFF0F1F4),
    text = Color.Black,
    secondaryText = Color.Gray,
    accent = Color(0xFF008CFF),
    accentContainer = Color(0xFFE7F3FF),
    divider = Color.Black.copy(alpha = .05f),
    dividerStrong = Color.Black.copy(alpha = .12f),
    glassBackground = Color.White,
    glassTint = Color.White.copy(alpha = .48f),
    controlTint = Color(0xFFBDBDBD).copy(alpha = .50f),
    controlFill = Color(0xFFBDBDBD).copy(alpha = .40f),
    choiceSelected = Color(0xFF008CFF).copy(alpha = .075f),
    choiceBorder = Color(0xFF008CFF).copy(alpha = .16f),
    glassBorder = Color.White.copy(alpha = .70f),
    glassOverlay = Color.White.copy(alpha = .04f),
    shadow = Color.Black.copy(alpha = .08f),
    scrim = Color.Black.copy(alpha = .16f),
    selection = Color.Black.copy(alpha = .09f),
    placeholder = Color.LightGray,
    error = Color(0xFFBA2541),
    playerBackground = Color(0xFF615B68)
)

private val DarkGrayColors = CastflowColorTokens(
    isDark = true,
    background = Color(0xFF17191C),
    elevated = Color(0xFF1E2125),
    muted = Color(0xFF282C32),
    text = Color(0xFFF2F4F7),
    secondaryText = Color(0xFFA5ABB4),
    accent = Color(0xFF63B4FF),
    accentContainer = Color(0xFF17334B),
    divider = Color.White.copy(alpha = .10f),
    dividerStrong = Color.White.copy(alpha = .16f),
    glassBackground = Color(0xFF202328),
    glassTint = Color(0xFF343941).copy(alpha = .72f),
    controlTint = Color(0xFF343941).copy(alpha = .72f),
    controlFill = Color(0xFF343941).copy(alpha = .68f),
    choiceSelected = Color(0xFF63B4FF).copy(alpha = .12f),
    choiceBorder = Color(0xFF63B4FF).copy(alpha = .22f),
    glassBorder = Color.White.copy(alpha = .12f),
    glassOverlay = Color.White.copy(alpha = .025f),
    shadow = Color.Black.copy(alpha = .48f),
    scrim = Color.Black.copy(alpha = .58f),
    selection = Color.White.copy(alpha = .10f),
    placeholder = Color(0xFF30353B),
    error = Color(0xFFFF8EA0),
    playerBackground = Color(0xFF141619)
)

private val OledBlackColors = CastflowColorTokens(
    isDark = true,
    background = Color.Black,
    elevated = Color(0xFF08090A),
    muted = Color(0xFF151719),
    text = Color(0xFFF7F7F8),
    secondaryText = Color(0xFFA0A5AC),
    accent = Color(0xFF67B8FF),
    accentContainer = Color(0xFF12283B),
    divider = Color.White.copy(alpha = .14f),
    dividerStrong = Color.White.copy(alpha = .18f),
    glassBackground = Color(0xFF050606),
    glassTint = Color(0xFF17191C).copy(alpha = .78f),
    controlTint = Color(0xFF17191C).copy(alpha = .78f),
    controlFill = Color(0xFF17191C).copy(alpha = .82f),
    choiceSelected = Color(0xFF67B8FF).copy(alpha = .14f),
    choiceBorder = Color(0xFF67B8FF).copy(alpha = .26f),
    glassBorder = Color.White.copy(alpha = .14f),
    glassOverlay = Color.White.copy(alpha = .02f),
    shadow = Color.Black.copy(alpha = .90f),
    scrim = Color.Black.copy(alpha = .70f),
    selection = Color.White.copy(alpha = .12f),
    placeholder = Color(0xFF1D1F22),
    error = Color(0xFFFF8DA0),
    playerBackground = Color.Black
)

private val LocalCastflowColors = staticCompositionLocalOf { LightColors }

object CastflowTokens {
    val colors: CastflowColorTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalCastflowColors.current
}

@Composable
fun CastflowTheme(selection: ThemeSelection, content: @Composable () -> Unit) {
    val colors = resolveThemeColors(selection)
    val colorScheme = castflowColorScheme(colors)
    CompositionLocalProvider(LocalCastflowColors provides colors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

@Composable
internal fun resolveThemeColors(selection: ThemeSelection): CastflowColorTokens {
    return themeColorsFor(selection, isSystemInDarkTheme())
}

internal fun themeColorsFor(selection: ThemeSelection, systemIsDark: Boolean): CastflowColorTokens {
    val dark = selection.resolvesToDark(systemIsDark)
    return when {
        !dark -> LightColors
        selection.darkTheme == DarkTheme.OledBlack -> OledBlackColors
        else -> DarkGrayColors
    }
}

private fun castflowColorScheme(colors: CastflowColorTokens) =
    if (colors.isDark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = Color.Black,
            primaryContainer = colors.accentContainer,
            onPrimaryContainer = colors.text,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.elevated,
            onSurface = colors.text,
            surfaceVariant = colors.muted,
            onSurfaceVariant = colors.secondaryText,
            outline = colors.divider,
            error = colors.error
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = Color.White,
            primaryContainer = colors.accentContainer,
            onPrimaryContainer = colors.text,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.elevated,
            onSurface = colors.text,
            surfaceVariant = colors.muted,
            onSurfaceVariant = colors.secondaryText,
            outline = colors.divider,
            error = colors.error
        )
    }
