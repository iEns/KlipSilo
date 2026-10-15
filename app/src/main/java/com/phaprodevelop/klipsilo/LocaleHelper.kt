package com.phaprodevelop.klipsilo

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {
    private const val LEGACY_PREFS = "locale_prefs"
    private const val LEGACY_KEY = "language"

    data class AppLanguage(
        val tag: String,
        val nativeName: String,
    )

    /**
     * Native names, ordered for the picker: Latin script A–Z, then Cyrillic, then CJK.
     * Indonesian uses BCP 47 `id`. Android still loads the strings from `values-in`.
     */
    val pickerLanguages: List<AppLanguage> =
        listOf(
            AppLanguage("id", "Bahasa Indonesia"),
            AppLanguage("da", "Dansk"),
            AppLanguage("de", "Deutsch"),
            AppLanguage("en", "English"),
            AppLanguage("es", "Español"),
            AppLanguage("fr", "Français"),
            AppLanguage("it", "Italiano"),
            AppLanguage("pt-BR", "Português (Brasil)"),
            AppLanguage("tr", "Türkçe"),
            AppLanguage("ru", "Русский"),
            AppLanguage("ja", "日本語"),
            AppLanguage("ko", "한국어"),
            AppLanguage("zh-Hans", "简体中文"),
        )

    private val pickerTags: Set<String> = pickerLanguages.map { it.tag }.toSet()

    /** Tag from the per-app language API. Null means follow the device language list. */
    fun selectedLanguageTag(context: Context): String? {
        val locales = readPlatformLocales(context)
        if (locales.isEmpty) return null
        val locale = locales[0] ?: return null
        return canonicalPickerTag(locale) ?: locale.toLanguageTag()
    }

    /**
     * Language for content that is not a string resource (the Danish or English privacy text).
     * An explicit per-app locale wins. Otherwise the device locale is matched.
     * Nothing is written, so a first launch stays on the system default.
     */
    fun getLanguage(context: Context): String {
        val selected = readPlatformLocales(context)
        val locale =
            if (selected.isEmpty) {
                primaryLocale(context)
            } else {
                selected[0] ?: primaryLocale(context)
            }
        return canonicalPickerTag(locale) ?: "en"
    }

    fun setLanguage(
        context: Context,
        lang: String?,
    ) {
        val tag = lang?.let { canonicalPickerTag(Locale.forLanguageTag(it)) }
        if (lang != null && tag == null) return
        val locales =
            if (tag == null) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }
        writePlatformLocales(context, locales)
    }

    /**
     * Copies the old da/en preference into the platform locale API once.
     * A locale already set in system App Info is left alone. The preference is then removed.
     */
    fun migrateLegacyLanguagePreference(context: Context) {
        val prefs = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(LEGACY_KEY)) return
        val stored = prefs.getString(LEGACY_KEY, null)
        if (readPlatformLocales(context).isEmpty) {
            val tag = stored?.let { canonicalPickerTag(Locale.forLanguageTag(it)) }
            if (tag != null) {
                writePlatformLocales(context, LocaleListCompat.forLanguageTags(tag))
            }
        }
        prefs.edit().remove(LEGACY_KEY).commit()
    }

    fun applyLocale(context: Context): Context {
        val locales = readPlatformLocales(context)
        if (locales.isEmpty) return context
        val locale = locales[0] ?: return context
        return createLocaleContext(context, locale)
    }

    private fun readPlatformLocales(context: Context): LocaleListCompat {
        if (Build.VERSION.SDK_INT >= 33) {
            val manager = context.getSystemService(LocaleManager::class.java)
            if (manager != null) {
                return LocaleListCompat.wrap(manager.applicationLocales)
            }
        }
        return AppCompatDelegate.getApplicationLocales()
    }

    private fun writePlatformLocales(
        context: Context,
        locales: LocaleListCompat,
    ) {
        if (Build.VERSION.SDK_INT >= 33) {
            val manager = context.getSystemService(LocaleManager::class.java)
            if (manager != null) {
                manager.applicationLocales =
                    if (locales.isEmpty) {
                        LocaleList.getEmptyLocaleList()
                    } else {
                        LocaleList.forLanguageTags(locales.toLanguageTags())
                    }
                return
            }
        }
        // API 26–32: AppCompatActivity persists this through autoStoreLocales.
        AppCompatDelegate.setApplicationLocales(locales)
    }

    internal fun canonicalPickerTag(locale: Locale): String? {
        val language = locale.language.lowercase(Locale.ROOT)
        return when {
            language == "id" || language == "in" -> "id"
            language == "pt" && locale.country.equals("BR", ignoreCase = true) -> "pt-BR"
            language == "zh" && isSimplifiedChinese(locale) -> "zh-Hans"
            language in pickerTags -> language
            else -> null
        }
    }

    private fun isSimplifiedChinese(locale: Locale): Boolean {
        val script = locale.script
        if (script.equals("Hans", ignoreCase = true)) return true
        if (script.equals("Hant", ignoreCase = true)) return false
        val country = locale.country.uppercase(Locale.ROOT)
        return country.isEmpty() || country == "CN" || country == "SG"
    }

    private fun primaryLocale(context: Context): Locale = context.resources.configuration.locales[0]

    private fun createLocaleContext(
        context: Context,
        locale: Locale,
    ): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
