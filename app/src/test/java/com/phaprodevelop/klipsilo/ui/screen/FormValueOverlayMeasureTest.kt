package com.phaprodevelop.klipsilo.ui.screen

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.ui.components.formOverlayMaxLines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The live preview overlay's value measuring. maxLines = 0 used to be passed for
 * multiline values; Compose rejects it ("maxLines should be greater than 0") the
 * moment a multi-line field gets its first character, crashing the screen (#68).
 */
@RunWith(RobolectricTestRunner::class)
class FormValueOverlayMeasureTest {
    private lateinit var textMeasurer: TextMeasurer

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        textMeasurer =
            TextMeasurer(
                defaultFontFamilyResolver = createFontFamilyResolver(context),
                defaultDensity = Density(1f),
                defaultLayoutDirection = LayoutDirection.Ltr,
            )
    }

    @Test
    fun maxLinesRule_neverZero() {
        assertEquals(1, formOverlayMaxLines(multiline = false))
        assertEquals(Int.MAX_VALUE, formOverlayMaxLines(multiline = true))
    }

    @Test
    fun maxLinesZero_isRejectedByCompose_theOldCrash() {
        val thrown =
            runCatching {
                textMeasurer.measure(
                    AnnotatedString("a"),
                    TextStyle(fontSize = 20.sp, color = Color.Black),
                    maxLines = 0,
                    softWrap = true,
                    constraints = Constraints(maxWidth = 500),
                )
            }.exceptionOrNull()
        assertTrue(thrown is IllegalArgumentException)
    }

    @Test
    fun multilineValue_laysOutWithTheFixedRule() {
        val laidOut: TextLayoutResult =
            textMeasurer.measure(
                AnnotatedString("a"),
                TextStyle(fontSize = 20.sp, color = Color.Black),
                maxLines = formOverlayMaxLines(multiline = true),
                softWrap = true,
                constraints = Constraints(maxWidth = 500),
            )
        assertEquals(1, laidOut.lineCount)
    }
}
