@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class LocaleHelperTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        legacyPrefs().edit().clear().commit()
        LocaleHelper.setLanguage(context, null)
    }

    @After
    fun tearDown() {
        LocaleHelper.setLanguage(context, null)
        legacyPrefs().edit().clear().commit()
    }

    @Test
    fun getLanguage_noOverride_daSystem_returnsDa() {
        val language = LocaleHelper.getLanguage(contextWithLocale("da"))

        assertEquals("da", language)
    }

    @Test
    fun getLanguage_noOverride_enSystem_returnsEn() {
        val language = LocaleHelper.getLanguage(contextWithLocale("en"))

        assertEquals("en", language)
    }

    @Test
    fun getLanguage_noOverride_deSystem_returnsDe() {
        val language = LocaleHelper.getLanguage(contextWithLocale("de"))

        assertEquals("de", language)
    }

    @Test
    fun getLanguage_noOverride_unsupportedSystem_returnsEn() {
        val language = LocaleHelper.getLanguage(contextWithLocale("nl"))

        assertEquals("en", language)
    }

    @Test
    fun getLanguage_noOverride_ptBr_returnsPtBr() {
        val language = LocaleHelper.getLanguage(contextWithLocale("pt-BR"))

        assertEquals("pt-BR", language)
    }

    @Test
    fun getLanguage_noOverride_europeanPortuguese_returnsEn() {
        // pt-PT is not a picker tag; privacy and strings fall back to English.
        val language = LocaleHelper.getLanguage(contextWithLocale("pt-PT"))

        assertEquals("en", language)
    }

    @Test
    fun getLanguage_noOverride_simplifiedChinese_returnsZhHans() {
        assertEquals("zh-Hans", LocaleHelper.getLanguage(contextWithLocale("zh-Hans")))
        assertEquals("zh-Hans", LocaleHelper.getLanguage(contextWithLocale("zh-CN")))
    }

    @Test
    fun getLanguage_noOverride_traditionalChinese_returnsEn() {
        val language = LocaleHelper.getLanguage(contextWithLocale("zh-TW"))

        assertEquals("en", language)
    }

    @Test
    fun setLanguage_da_usesPlatformApiAndDoesNotWriteLegacyPrefs() {
        LocaleHelper.setLanguage(context, "da")

        assertEquals("da", LocaleHelper.getLanguage(context))
        assertEquals("da", LocaleHelper.selectedLanguageTag(context))
        assertFalse(legacyPrefs().contains(LEGACY_KEY))
    }

    @Test
    fun setLanguage_null_followsSystem() {
        LocaleHelper.setLanguage(context, "es")
        LocaleHelper.setLanguage(context, null)

        assertNull(LocaleHelper.selectedLanguageTag(context))
        assertEquals("en", LocaleHelper.getLanguage(contextWithLocale("en")))
        assertFalse(legacyPrefs().contains(LEGACY_KEY))
    }

    @Test
    fun migrate_legacyDanish_movesToPlatformAndDeletesPreference() {
        legacyPrefs().edit().putString(LEGACY_KEY, "da").commit()

        LocaleHelper.migrateLegacyLanguagePreference(context)

        assertEquals("da", LocaleHelper.selectedLanguageTag(context))
        assertFalse(legacyPrefs().contains(LEGACY_KEY))
    }

    @Test
    fun migrate_unknownLegacyValue_deletesPreferenceAndFollowsSystem() {
        legacyPrefs().edit().putString(LEGACY_KEY, "xx").commit()

        LocaleHelper.migrateLegacyLanguagePreference(context)

        assertNull(LocaleHelper.selectedLanguageTag(context))
        assertFalse(legacyPrefs().contains(LEGACY_KEY))
        assertEquals("da", LocaleHelper.getLanguage(contextWithLocale("da")))
    }

    @Test
    fun migrate_keepsSystemAppInfoChoiceAndDeletesPreference() {
        LocaleHelper.setLanguage(context, "fr")
        legacyPrefs().edit().putString(LEGACY_KEY, "da").commit()

        LocaleHelper.migrateLegacyLanguagePreference(context)

        assertEquals("fr", LocaleHelper.selectedLanguageTag(context))
        assertFalse(legacyPrefs().contains(LEGACY_KEY))
    }

    @Test
    fun applyLocale_selectedDa_configurationLanguageIsDa() {
        LocaleHelper.setLanguage(context, "da")

        val localized = LocaleHelper.applyLocale(context)
        val language = localized.resources.configuration.locales[0].language

        assertTrue(language.startsWith("da"))
    }

    @Test
    fun applyLocale_systemDefault_returnsSameContext() {
        val result = LocaleHelper.applyLocale(context)

        assertSame(context, result)
    }

    @Test
    @Config(sdk = [28])
    fun setLanguage_beforeAndroid13_updatesAppCompatDelegate() {
        LocaleHelper.setLanguage(context, "de")

        assertEquals("de", AppCompatDelegate.getApplicationLocales().toLanguageTags())

        LocaleHelper.setLanguage(context, null)

        assertTrue(AppCompatDelegate.getApplicationLocales().isEmpty)
    }

    @Test
    fun resources_followSelectedLanguage() {
        assertEquals("Ajustes", stringFor("es", R.string.settings))
        // Soft hyphen U+00AD at the compound boundary (#62).
        assertEquals("Ein\u00ADstellungen", stringFor("de", R.string.settings))
        assertEquals("Configurações", stringFor("pt-BR", R.string.settings))
        assertEquals("Setelan", stringFor("id", R.string.settings))
        assertEquals("设置", stringFor("zh-Hans", R.string.settings))
        assertEquals("Настройки", stringFor("ru", R.string.settings))
        assertEquals("設定", stringFor("ja", R.string.settings))
        assertEquals("설정", stringFor("ko", R.string.settings))
        assertEquals("Ayarlar", stringFor("tr", R.string.settings))
        assertEquals("Paramètres", stringFor("fr", R.string.settings))
        assertEquals("Impostazioni", stringFor("it", R.string.settings))
    }

    @Test
    fun resources_simplifiedChineseRegion_usesHansFolder() {
        assertEquals("设置", stringFor("zh-CN", R.string.settings))
    }

    @Test
    fun resources_europeanPortuguese_usesBrazilianPortuguese() {
        // No values-pt-rPT folder. Android's resource match resolves pt-PT to values-pt-rBR.
        assertEquals("Configurações", stringFor("pt-PT", R.string.settings))
    }

    @Test
    fun languageFlag_systemIsGlobe_andEveryPickerLanguageHasAFlag() {
        assertEquals("🌐", LocaleHelper.languageFlag(null))
        assertEquals("🇬🇧", LocaleHelper.languageFlag("en"))
        assertEquals("🇪🇸", LocaleHelper.languageFlag("es"))
        assertEquals("🇧🇷", LocaleHelper.languageFlag("pt-BR"))
        assertEquals("🇨🇳", LocaleHelper.languageFlag("zh-Hans"))
        assertEquals("🌐", LocaleHelper.languageFlag("nl"))

        LocaleHelper.pickerLanguages.forEach { language ->
            val flag = LocaleHelper.languageFlag(language.tag)
            assertTrue(language.tag, flag.isNotBlank())
            assertTrue(language.tag, flag != "🌐")
        }
    }

    @Test
    fun resources_keepPlaceholders() {
        val localized = contextWithLocale("es")
        assertEquals("Página 2 / 5", localized.getString(R.string.reader_page, 2, 5))
    }

    private fun stringFor(
        language: String,
        resId: Int,
    ): String = contextWithLocale(language).getString(resId)

    private fun contextWithLocale(language: String): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(language))
        return context.createConfigurationContext(config)
    }

    private fun legacyPrefs() = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)

    private companion object {
        const val LEGACY_PREFS = "locale_prefs"
        const val LEGACY_KEY = "language"
    }
}
