package com.phaprodevelop.klipsilo.data.repository

import android.graphics.Paint
import android.graphics.Typeface
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Blank lines between annotation content lines survive wrapping (#172). The saved text already
 * keeps them; wrapping must not collapse the empty segments into consecutive text lines.
 */
@RunWith(RobolectricTestRunner::class)
class AnnotatedPdfBlankLinesTest {
    private fun paint(): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            typeface = Typeface.DEFAULT
        }

    @Test
    fun internalBlankLines_surviveWrapping() {
        assertEquals(
            listOf("line1", "", "", "line4"),
            wrapAnnotationText("line1\n\n\nline4", paint(), maxWidthPx = 10_000),
        )
    }

    @Test
    fun emptySegment_isAnEmptyLine() {
        assertEquals(listOf(""), wrapSegment("", paint(), maxWidthPx = 10_000))
    }

    @Test
    fun trailingBlankLines_surviveWrapping_butCommitTrimsThem() {
        // Wrapping keeps every blank line; resolveTextCommit is the trim boundary (#50/#55).
        assertEquals(
            listOf("line1", "", ""),
            wrapAnnotationText("line1\n\n", paint(), maxWidthPx = 10_000),
        )
    }

    @Test
    fun lineBreaksStillWrapLongSegments() {
        // #59/#91/#138: explicit breaks still split text, and long segments still word-wrap.
        val lines = wrapAnnotationText("Eins\nZwei lang", paint(), maxWidthPx = 1)
        assertEquals(listOf("Eins", "Zwei", "lang"), lines)
    }
}
