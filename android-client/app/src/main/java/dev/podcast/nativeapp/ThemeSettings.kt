package dev.podcast.nativeapp

import android.content.Context
import androidx.core.content.edit

enum class ThemeMode(val storedValue: String) {
    FollowSystem("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun fromStored(value: String?): ThemeMode =
            entries.firstOrNull { it.storedValue == value } ?: FollowSystem
    }
}

enum class DarkTheme(val storedValue: String) {
    DarkGray("dark-gray"),
    OledBlack("oled-black");

    companion object {
        fun fromStored(value: String?): DarkTheme =
            entries.firstOrNull { it.storedValue == value } ?: DarkGray
    }
}

data class ThemeSelection(
    val mode: ThemeMode = ThemeMode.FollowSystem,
    val darkTheme: DarkTheme = DarkTheme.DarkGray
) {
    fun resolvesToDark(systemIsDark: Boolean): Boolean = when (mode) {
        ThemeMode.FollowSystem -> systemIsDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
}

class ThemePreferences(context: Context) {
    private val preferences = context.getSharedPreferences("app-theme", Context.MODE_PRIVATE)

    fun read(): ThemeSelection = ThemeSelection(
        mode = ThemeMode.fromStored(preferences.getString("themeMode", null)),
        darkTheme = DarkTheme.fromStored(preferences.getString("darkTheme", null))
    )

    fun write(selection: ThemeSelection) {
        preferences.edit {
            putString("themeMode", selection.mode.storedValue)
            putString("darkTheme", selection.darkTheme.storedValue)
        }
    }
}
