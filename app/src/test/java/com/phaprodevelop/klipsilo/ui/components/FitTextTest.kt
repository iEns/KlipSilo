@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.components

import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FitTextTest {
    @Test
    fun fittedFontSize_keepsTheBaseSizeWhenItFits() {
        assertEquals(14f, fittedFontSize(14f) { true })
    }

    @Test
    fun fittedFontSize_stepsDownOneSpAtATimeUntilItFits() {
        // Everything above 11sp overflows.
        assertEquals(11f, fittedFontSize(14f) { it <= 11f })
        assertEquals(13f, fittedFontSize(14f) { it <= 13f })
    }

    @Test
    fun fittedFontSize_neverGoesBelowSeventyFivePercentOfTheBase() {
        val floor = 14f * LABEL_FONT_FLOOR_FRACTION
        // Only the floor itself fits: it is still returned, no ellipsis fallback.
        assertEquals(floor, fittedFontSize(14f) { it <= floor + 0.0001f })
    }

    @Test
    fun fittedFontSize_returnsNullWhenEvenTheFloorDoesNotFit() {
        assertNull(fittedFontSize(14f) { it < 14f * LABEL_FONT_FLOOR_FRACTION })
        assertNull(fittedFontSize(14f) { false })
    }

    @Test
    fun fittedFontSize_landsExactlyOnAnOddFloor() {
        // Base 9sp → floor 6.75sp, stepping produces 9, 8, 7, 6.75.
        assertEquals(6.75f, fittedFontSize(9f) { it <= 6.75f })
    }

    @Test
    fun commonFittedSize_picksTheSizeNeededByTheLongestLabel() {
        // All labels in a row share one size: the smallest fitted one (#50).
        assertEquals(9f, commonFittedSize(listOf(12f, 9f, 11f)))
        assertEquals(12f, commonFittedSize(listOf(12f, 12f)))
    }

    @Test
    fun commonFittedSize_isNullWhenAnyLabelDoesNotFit() {
        // One label too wide → the whole row falls back (two lines) instead
        // of rendering mixed sizes (#50).
        assertNull(commonFittedSize(listOf(12f, null, 11f)))
        assertNull(commonFittedSize(emptyList()))
    }

    @Test
    fun isMidWordBreak_rejectsLineStartsAtWordBoundaries() {
        val text = "Auswahl speichern"
        assertFalse(isMidWordBreak(text, 0))
        assertFalse(isMidWordBreak(text, text.length))
        assertFalse(isMidWordBreak(text, 8)) // after "Auswahl "
    }

    @Test
    fun isMidWordBreak_detectsBreaksInsideAWord() {
        val text = "Text speichern"
        // Second line starts mid-word inside "speichern".
        assertTrue(isMidWordBreak(text, 6))
        assertTrue(isMidWordBreak("speichern", 4))
    }

    @Test
    fun isMidWordBreak_acceptsBreaksAfterAHyphen() {
        // A hyphen is a word boundary: "OCR-Seite" may wrap after "OCR-" in
        // the PDF viewer toolbar at large font scales (#45) instead of being
        // clipped or ellipsized.
        assertFalse(isMidWordBreak("OCR-Seite", 4))
        assertFalse(isMidWordBreak("SVG-Datei", 4))
        // But the characters on either side of the break must be the
        // separator — inside "OCR" or "Seite" it is still a mid-word break.
        assertTrue(isMidWordBreak("OCR-Seite", 2))
        assertTrue(isMidWordBreak("OCR-Seite", 6))
    }

    @Test
    fun isMidWordBreak_flagsTheWerkzeugeBottomNavBreak() {
        // "Werkzeuge" wrapped as "Werkzeu"/"ge" in a too-narrow bottom
        // navigation slot at font scale 2.0 (#45) — a single-line label can
        // never produce this break.
        assertTrue(isMidWordBreak("Werkzeuge", 7))
        assertTrue(isMidWordBreak("Einstellungen", 9))
        assertFalse(isMidWordBreak("Werkzeuge", 0))
        assertFalse(isMidWordBreak("Werkzeuge", "Werkzeuge".length))
    }

    @Test
    fun singleWordLabels_canNeverBeAcceptedAsWrapped() {
        // A single word ("Auswahl", "Marker", #50) has no word boundary, so
        // every possible line start inside it is a mid-word break. fit()
        // rejects all of them, so a single-word label can only render on one
        // line (shrunk to the floor) or ellipsized — it never soft-wraps.
        for (word in listOf("Auswahl", "Marker", "Auswa", "hl", "Highlight")) {
            for (start in 1 until word.length) {
                assertTrue(isMidWordBreak(word, start))
            }
            assertFalse(isMidWordBreak(word, 0))
            assertFalse(isMidWordBreak(word, word.length))
        }
    }

    @Test
    fun toolRowCommonSize_usesTheSingleWordFloorBeforeFallingBack() {
        // The tool rail's common size uses the ~60% single-word floor (#50):
        // the fitted size stops at the floor, and only past it does the row
        // fall back (null) to two lines.
        val floor = 11f * NAV_LABEL_FONT_FLOOR_FRACTION
        assertEquals(floor, fittedFontSize(11f, NAV_LABEL_FONT_FLOOR_FRACTION) { it <= floor })
        assertNull(fittedFontSize(11f, NAV_LABEL_FONT_FLOOR_FRACTION) { it < floor })
    }

    @Test
    fun isMidWordBreak_treatsZeroWidthSpaceAndUnderscoreAsBoundaries() {
        val zwsp = '\u200B'
        // Display titles get a zero-width space after '_' (#54), so a wrap
        // right after one is between parts, not mid-word.
        assertFalse(isMidWordBreak("Weekend_${zwsp}København", 9))
        assertFalse(isMidWordBreak("Weekend_i_Køb$zwsp", "Weekend_i_Køb$zwsp".length - 1))
        // Underscores themselves read as separators (#54): "Event_" ends at
        // index 6, "Registration_" starts the next line at index 19.
        assertFalse(isMidWordBreak("Event_Registration_Form", 6))
        assertFalse(isMidWordBreak("Event_Registration_Form", 19))
        // Inside a part it is still mid-word.
        assertTrue(isMidWordBreak("Event_Registration_Form", 4))
    }

    @Test
    fun isMidWordBreak_acceptsCJKPerCharacterBreaks() {
        // #62: ja/ko/zh wrap per character — a line start between two CJK
        // characters is the normal line breaking there, not a mid-word
        // break.
        val text = "プライバシーポリシー"
        assertFalse(isMidWordBreak(text, 1))
        assertFalse(isMidWordBreak(text, 6))
        assertFalse(isMidWordBreak("おはよう。こんにちは", 5))
        // A Latin word next to CJK characters still breaks only at its own
        // boundaries.
        assertTrue(isMidWordBreak("Bearbeiten", 4))
        assertFalse(isMidWordBreak("テキストBearbeiten", 4))
    }

    @Test
    fun fittedFontSize_usesTheCustomFloorPercentage() {
        // Bottom navigation labels may shrink to ~60% of the base size (#45).
        val floor = 14f * NAV_LABEL_FONT_FLOOR_FRACTION
        val floorFit =
            fittedFontSize(14f, floorPercentage = NAV_LABEL_FONT_FLOOR_FRACTION) {
                it <= floor + 0.0001f
            }
        assertEquals(floor, floorFit)
        assertNull(fittedFontSize(14f, floorPercentage = NAV_LABEL_FONT_FLOOR_FRACTION) { false })
    }

    @Test
    fun scaledLabelStyle_scalesTheLineHeightInProportion() {
        val style = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
        val scaled = scaledLabelStyle(style, 7f, 14f)
        assertEquals(7.sp, scaled.fontSize)
        assertEquals(10.sp, scaled.lineHeight)
    }

    @Test
    fun scaledLabelStyle_keepsAnUnspecifiedLineHeight() {
        val style = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        val scaled = scaledLabelStyle(style, 7f, 14f)
        assertEquals(7.sp, scaled.fontSize)
        assertFalse(scaled.lineHeight.isSp)
    }

    @Test
    fun fitTextSemanticsText_stripsTheInvisibleBreaksTheDisplayKeeps() {
        // #64: the display string of a fitted label keeps its break
        // characters — the soft hyphen draws as a hyphen at the break
        // (#62), the zero-width space gives the wrap opportunity (#54) —
        // while the semantics get the clean string.
        val display = "Ein\u00ADstellungen Event_\u200BRegistration\u200B_Form"
        assertTrue(display.contains('\u00AD'))
        assertTrue(display.contains('\u200B'))
        assertEquals(
            "Einstellungen Event_Registration_Form",
            fitTextSemanticsText(display),
        )
    }

    @Test
    fun fitTextSemanticsText_isCleanForEverySourceLabel() {
        // #64: whatever the fit-text rule decides to draw, the semantics —
        // built from the source label — never carry U+00AD or U+200B.
        val labels =
            listOf(
                "Datenschutz\u00ADerklärung",
                "Event_\u200BRegistration\u200B_Form",
                "1.2.0-\u200Bbeta.4-\u200Bdebug+\u200Bee33946",
                "Ind\u00ADstillinger",
            )
        for (label in labels) {
            val semantics = fitTextSemanticsText(label)
            assertFalse(semantics.contains('\u00AD'))
            assertFalse(semantics.contains('\u200B'))
        }
    }

    @Test
    fun fitTextSemanticsText_readsTheSourceLabelNotTheDrawnLines() {
        // #64 follow-up: the drawn form joins its lines with "\n" — a
        // per-character CJK wrap inserts them mid-word — and a soft-hyphen
        // break draws as a visible hyphen (drawnLinePiece). Both leaked
        // into the semantics ("Datenschutz-\nerklärung", "プライバシー\n
        // ポリシー"): braille read the line break and the hyphen. The
        // semantics string is the SOURCE label, stripped of the invisible
        // breaks: no "\n", no inserted "-", no U+00AD/U+200B.
        val de = "Datenschutz\u00ADerklärung"
        val deDecision =
            resolveFitText(
                de,
                125f,
                FitTextPolicy(maxLines = 2),
                widthOf = { s, _ -> s.length * 10f },
            )
        // The drawn form really is what must not reach the semantics.
        assertTrue(deDecision.text.contains('-'))
        assertTrue(deDecision.text.contains('\n'))
        val deSemantics = fitTextSemanticsText(de)
        assertEquals(de.replace("\u00AD", ""), deSemantics)
        assertFalse(deSemantics.contains('\u00AD'))
        assertFalse(deSemantics.contains('\n'))
        assertFalse(deSemantics.contains('-'))

        val ja = "プライバシーポリシー"
        val jaDecision =
            resolveFitText(
                ja,
                55f,
                FitTextPolicy(maxLines = 2),
                widthOf = { s, _ -> s.length * 10f },
            )
        assertTrue(jaDecision.text.contains('\n'))
        val jaSemantics = fitTextSemanticsText(ja)
        assertEquals(ja, jaSemantics)
        assertFalse(jaSemantics.contains('\n'))
        assertFalse(jaSemantics.contains('-'))
    }
}
