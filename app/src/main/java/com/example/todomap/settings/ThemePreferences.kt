package com.example.todomap.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemePreferences {
    private const val PREFS_SETTINGS = "settings_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    fun getThemeMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun setThemeMode(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
    }

    fun applyTheme(context: Context) {
        AppCompatDelegate.setDefaultNightMode(getThemeMode(context))
    }
}

