package com.phaprodevelop.klipsilo

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_LANG = "language"

    fun getLanguage(context: Context): String {
        return getStoredLanguage(context) ?: systemLanguage(context)
    }

    fun setLanguage(
        context: Context,
        lang: String,
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, lang).apply()
    }

    fun toggleLanguage(context: Context): String {
        val next = if (getLanguage(context) == "da") "en" else "da"
        setLanguage(context, next)
        return next
    }

    fun applyLocale(context: Context): Context {
        val stored = getStoredLanguage(context) ?: return context
        return createLocaleContext(context, Locale.forLanguageTag(stored))
    }

    private fun getStoredLanguage(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANG, null)
            ?.takeIf { it == "da" || it == "en" }
    }

    private fun systemLanguage(context: Context): String {
        val lang = context.resources.configuration.locales[0].language
        return if (lang.startsWith("da", ignoreCase = true)) "da" else "en"
    }

    private fun createLocaleContext(
        context: Context,
        locale: Locale,
    ): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
