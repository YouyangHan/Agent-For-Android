package com.agentforandroid.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val value: Int) {
    LIGHT(0), DARK(1);

    companion object {
        fun fromValue(v: Int) = entries.find { it.value == v } ?: LIGHT
    }
}

object AppPreferences {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_LANG = "language"
    private const val KEY_APP_NAME = "app_name"

    fun getThemeMode(context: Context): ThemeMode {
        val v = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_THEME, 0)
        return ThemeMode.fromValue(v)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_THEME, mode.value).apply()
    }

    fun getLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANG, "zh") ?: "zh"
    }

    fun setLanguage(context: Context, lang: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, lang).apply()
    }

    fun getAppName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APP_NAME, "Agent Yang") ?: "Agent Yang"
    }

    fun setAppName(context: Context, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_APP_NAME, name).apply()
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E3FC),
    secondary = Color(0xFF5F6368),
    surface = Color(0xFFFFFFFF),
    background = Color(0xFFF8F9FA),
    error = Color(0xFFD93025)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF003A75),
    primaryContainer = Color(0xFF004A9F),
    secondary = Color(0xFF9AA0A6),
    surface = Color(0xFF1E1E1E),
    background = Color(0xFF121212),
    error = Color(0xFFF28B82)
)

@Composable
fun AgentForAndroidTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = themeMode == ThemeMode.DARK
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
