@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class DeleteClipQuantityTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun english_usesSingularForOneAndPluralForSeveral() {
        val resources = contextWithLocale("en").resources

        assertEquals("Delete 1 clip?", resources.getQuantityString(R.plurals.delete_clip_count, 1, 1))
        assertEquals("Delete 3 clips?", resources.getQuantityString(R.plurals.delete_clip_count, 3, 3))
    }

    @Test
    fun danish_usesKlipForOneAndSeveral() {
        val resources = contextWithLocale("da").resources

        assertEquals("Slet 1 klip?", resources.getQuantityString(R.plurals.delete_clip_count, 1, 1))
        assertEquals("Slet 3 klip?", resources.getQuantityString(R.plurals.delete_clip_count, 3, 3))
    }

    @Test
    fun russian_usesOneFewAndMany() {
        val resources = contextWithLocale("ru").resources

        assertEquals("Удалить 1 клип?", resources.getQuantityString(R.plurals.delete_clip_count, 1, 1))
        assertEquals("Удалить 2 клипа?", resources.getQuantityString(R.plurals.delete_clip_count, 2, 2))
        assertEquals("Удалить 5 клипов?", resources.getQuantityString(R.plurals.delete_clip_count, 5, 5))
    }

    private fun contextWithLocale(language: String): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(language))
        return context.createConfigurationContext(config)
    }
}
