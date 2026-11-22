package dev.podcast.nativeapp

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeSettingsTest {
    @Test fun defaultsFollowSystemWithDarkGray() {
        assertEquals(ThemeMode.FollowSystem, ThemeMode.fromStored(null))
        assertEquals(DarkTheme.DarkGray, DarkTheme.fromStored(null))
        assertEquals(ThemeMode.FollowSystem, ThemeSelection().mode)
        assertEquals(DarkTheme.DarkGray, ThemeSelection().darkTheme)
    }

    @Test fun unknownStoredValuesFallBackSafely() {
        assertEquals(ThemeMode.FollowSystem, ThemeMode.fromStored("monet"))
        assertEquals(DarkTheme.DarkGray, DarkTheme.fromStored("midnight"))
    }

    @Test fun selectionResolvesSystemAndManualModes() {
        assertTrue(ThemeSelection(ThemeMode.FollowSystem).resolvesToDark(true))
        assertFalse(ThemeSelection(ThemeMode.FollowSystem).resolvesToDark(false))
        assertFalse(ThemeSelection(ThemeMode.Light).resolvesToDark(true))
        assertTrue(ThemeSelection(ThemeMode.Dark).resolvesToDark(false))
    }

    @Test fun storedValuesRoundTrip() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromStored(it.storedValue)) }
        DarkTheme.entries.forEach { assertEquals(it, DarkTheme.fromStored(it.storedValue)) }
    }

    @Test fun fixedPalettesDoNotUseTheSystemDynamicColor() {
        val light = themeColorsFor(ThemeSelection(ThemeMode.Light), systemIsDark = true)
        val gray = themeColorsFor(ThemeSelection(ThemeMode.Dark, DarkTheme.DarkGray), systemIsDark = false)
        val oled = themeColorsFor(ThemeSelection(ThemeMode.Dark, DarkTheme.OledBlack), systemIsDark = false)

        assertFalse(light.isDark)
        assertEquals(Color.White, light.background)
        assertTrue(gray.isDark)
        assertFalse(gray.background == Color.Black)
        assertTrue(oled.isDark)
        assertEquals(Color.Black, oled.background)
    }
}
