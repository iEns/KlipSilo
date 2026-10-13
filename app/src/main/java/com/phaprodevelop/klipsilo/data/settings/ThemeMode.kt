package com.phaprodevelop.klipsilo.data.settings

import android.content.Context

enum class ThemeMode { SYSTEM, LIGHT, DARK }

object ThemeModeStore {
    private const val PREFS_NAME = "theme_mode"
    private const val KEY_MODE = "mode"

    fun load(context: Context): ThemeMode {
        return runCatching {
            ThemeMode.valueOf(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(KEY_MODE, null) ?: ThemeMode.SYSTEM.name,
            )
        }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun save(
        context: Context,
        mode: ThemeMode,
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .apply()
    }
}
