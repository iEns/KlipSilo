@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class LocaleHelperTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun getLanguage_noStored_daSystem_returnsDa() {
        // Arrange
        val daContext = contextWithLocale("da")

        // Act
        val language = LocaleHelper.getLanguage(daContext)

        // Assert
        assertEquals("da", language)
    }

    @Test
    fun getLanguage_noStored_enSystem_returnsEn() {
        // Arrange
        val enContext = contextWithLocale("en")

        // Act
        val language = LocaleHelper.getLanguage(enContext)

        // Assert
        assertEquals("en", language)
    }

    @Test
    fun getLanguage_noStored_deSystem_returnsEn() {
        // Arrange
        val deContext = contextWithLocale("de")

        // Act
        val language = LocaleHelper.getLanguage(deContext)

        // Assert
        assertEquals("en", language)
    }

    @Test
    fun setLanguage_da_getLanguageReturnsDa() {
        // Arrange
        LocaleHelper.setLanguage(context, "da")

        // Act
        val language = LocaleHelper.getLanguage(context)

        // Assert
        assertEquals("da", language)
    }

    @Test
    fun getLanguage_storedFr_fallsBackToSystem() {
        // Arrange
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, "fr")
            .commit()
        val daContext = contextWithLocale("da")
        val enContext = contextWithLocale("en")

        // Act
        val fromDa = LocaleHelper.getLanguage(daContext)
        val fromEn = LocaleHelper.getLanguage(enContext)

        // Assert
        assertEquals("da", fromDa)
        assertEquals("en", fromEn)
    }

    @Test
    fun toggleLanguage_fromDa_returnsAndPersistsEn() {
        // Arrange
        LocaleHelper.setLanguage(context, "da")

        // Act
        val next = LocaleHelper.toggleLanguage(context)

        // Assert
        assertEquals("en", next)
        assertEquals("en", LocaleHelper.getLanguage(context))
    }

    @Test
    fun toggleLanguage_fromEn_returnsAndPersistsDa() {
        // Arrange
        LocaleHelper.setLanguage(context, "en")

        // Act
        val next = LocaleHelper.toggleLanguage(context)

        // Assert
        assertEquals("da", next)
        assertEquals("da", LocaleHelper.getLanguage(context))
    }

    @Test
    fun applyLocale_storedDa_configurationLanguageIsDa() {
        // Arrange
        LocaleHelper.setLanguage(context, "da")

        // Act
        val localized = LocaleHelper.applyLocale(context)
        val language = localized.resources.configuration.locales[0].language

        // Assert
        assertTrue(language.startsWith("da"))
    }

    @Test
    fun applyLocale_noStoredLang_returnsSameContext() {
        // Arrange: empty locale_prefs from setUp

        // Act
        val result = LocaleHelper.applyLocale(context)

        // Assert
        assertSame(context, result)
    }

    private fun contextWithLocale(language: String): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(language))
        return context.createConfigurationContext(config)
    }

    private companion object {
        const val PREFS_NAME = "locale_prefs"
        const val KEY_LANG = "language"
    }
}
