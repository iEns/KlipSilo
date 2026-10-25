@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.data

import com.phaprodevelop.klipsilo.data.db.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextClipTitleTest {
    @Test
    fun heading_dropsTheHash() {
        assertEquals("Lemon Garlic Pasta", textClipTitle("# Lemon Garlic Pasta\n\nBoil the pasta."))
    }

    @Test
    fun heading_canFollowBlankLines() {
        assertEquals("Lemon Garlic Pasta", textClipTitle("\n\n## Lemon Garlic Pasta\nbody"))
    }

    @Test
    fun noHeading_usesFirstLineWithoutMarkers() {
        assertEquals("Buy milk", textClipTitle("- [ ] Buy milk\n- [ ] Bread"))
        assertEquals("A quote", textClipTitle("> A quote\n\nmore"))
        assertEquals("Bold pasta", textClipTitle("**Bold** pasta"))
    }

    @Test
    fun plainTitle_isUnchanged() {
        assertEquals("Sunday roast", textClipTitle("Sunday roast"))
        assertFalse(textTitleLooksAutomatic("Sunday roast"))
    }

    @Test
    fun longLine_stopsNearSixtyCharacters() {
        val title = textClipTitle("word ".repeat(30))

        assertTrue(title.length <= TEXT_CLIP_TITLE_MAX)
        assertTrue(title.isNotEmpty())
    }

    @Test
    fun rawBody_looksAutomatic_shortRenameDoesNot() {
        assertTrue(textTitleLooksAutomatic("# Lemon Garlic Pasta\n\nBoil"))
        assertTrue(textTitleLooksAutomatic("# Lemon Garlic Pasta"))
        assertFalse(textTitleLooksAutomatic("Lemon Garlic Pasta"))
    }

    @Test
    fun clipListTitle_stripsTextAndMarkdownAndLeavesOtherTypes() {
        assertEquals("Lemon Garlic Pasta", clipListTitle(ContentType.TEXT, "# Lemon Garlic Pasta"))
        assertEquals("Lemon Garlic Pasta", clipListTitle(ContentType.MARKDOWN, "# Lemon Garlic Pasta"))
        assertEquals("notes.pdf", clipListTitle(ContentType.PDF, "notes.pdf"))
    }

    @Test
    fun clipPickerTitle_usesTheClipListTitleWhenDerivable() {
        assertEquals("Buy milk", clipPickerTitle(ContentType.TEXT, "- Buy milk\n- [ ] Bread", "(untitled)"))
        assertEquals("notes.pdf", clipPickerTitle(ContentType.PDF, "notes.pdf", "(untitled)"))
    }

    @Test
    fun clipPickerTitle_neverReturnsAnEmptyRow() {
        // An untitled text clip without a derivable title must show the
        // "(untitled)" fallback instead of a blank row (#43).
        assertEquals("(untitled)", clipPickerTitle(ContentType.TEXT, "", "(untitled)"))
        assertEquals("(untitled)", clipPickerTitle(ContentType.TEXT, "\n\n", "(untitled)"))
        assertEquals("(untitled)", clipPickerTitle(ContentType.IMAGE, "", "(untitled)"))
    }

    @Test
    fun setextHeading_wins_overLaterAtxHeading() {
        val recipe =
            "Citronpasta med hvidløg\n" +
                "=======================\n" +
                "\n" +
                "*Til 2 personer - klar på 20 minutter*\n" +
                "\n" +
                "## Ingredienser\n" +
                "- 200 g spaghetti\n"
        assertEquals("Citronpasta med hvidløg", textClipTitle(recipe))
    }

    @Test
    fun setextHeading_levelTwo_usesDashes() {
        assertEquals("Citronpasta", textClipTitle("Citronpasta\n>\n---\n\n## Ris"))
        assertEquals("Citronpasta", textClipTitle("Citronpasta\n---\n\n## Ris"))
    }

    @Test
    fun setextUnderline_withoutTextBefore_isSkipped() {
        assertEquals("Lemon", textClipTitle("-----\n\n# Lemon\n"))
    }

    @Test
    fun setextUnderline_afterAnotherUnderline_doesNotRebind() {
        assertEquals("=== band", textClipTitle("=== band\n====\n\n# Real"))
    }

    @Test
    fun atxHeading_wins_whenItComesFirst() {
        assertEquals("Lemon Garlic Pasta", textClipTitle("# Lemon Garlic Pasta\n\nTitle\n====="))
    }

    @Test
    fun title_fromRawStoredBody_isSingleLine() {
        val stored =
            "Ingredienser\n" +
                "- 200 g spaghetti\n" +
                "- 3 fed hvidløg i tynde skiver\n" +
                "- 1 citron, skal og saft\n"
        val title = clipListTitle(ContentType.TEXT, stored)
        assertEquals("Ingredienser", title)
        assertTrue(title.lines().size == 1)
    }

    @Test
    fun clipListTitle_neverReturnsMultiLineRaw() {
        val raw = "Weekend_i_i_København\nstray line"
        val title = clipListTitle(ContentType.PDF, raw)
        assertEquals(1, title.lines().size)
        assertFalse(title.contains('\n'))
        assertTrue(clipListTitle(ContentType.TEXT, "\n\n##\n## Lemon\nbody").contains('\n').not())
    }

    @Test
    fun markerOnlyFirstTitle_fallsBackToReadableLine() {
        assertEquals("Lemon", clipListTitle(ContentType.TEXT, "##\n\n- Lemon\nrest"))
        assertEquals("Buy milk", clipListTitle(ContentType.TEXT, "- [ ] Buy milk\n- [ ] Bread"))
    }

    @Test
    fun legacyTextClipTitle_reproducesThePreSetextRule() {
        val recipe =
            "Citronpasta med hvidløg\n" +
                "=======================\n" +
                "\n" +
                "## Ingredienser\n" +
                "- 200 g spaghetti\n"
        assertEquals("Ingredienser", legacyTextClipTitle(recipe))
        assertEquals("Lemon Garlic Pasta", legacyTextClipTitle("# Lemon Garlic Pasta\n\nBoil"))
        assertEquals("Buy milk", legacyTextClipTitle("- [ ] Buy milk\n- [ ] Bread"))
    }

    @Test
    fun legacyRawTextPreview_matchesTheOldTruncation() {
        assertEquals("short body", legacyRawTextPreview("short body"))
        val long = "word ".repeat(30)
        assertEquals(long.take(LEGACY_CLIP_PREVIEW_MAX) + "...", legacyRawTextPreview(long))
    }

    @Test
    fun refreshedTextClipTitle_recomputesLegacyDerivedTitle() {
        val recipe =
            "Citronpasta med hvidløg\n" +
                "=======================\n" +
                "\n" +
                "## Ingredienser\n" +
                "- 200 g spaghetti\n"
        assertEquals("Citronpasta med hvidløg", refreshedTextClipTitle("Ingredienser", recipe))
    }

    @Test
    fun refreshedTextClipTitle_recomputesRawBodyPreview() {
        val body =
            "Citronpasta med hvidløg\n" +
                "=======================\n" +
                "\n" +
                "## Ingredienser\n"
        assertEquals("Citronpasta med hvidløg", refreshedTextClipTitle(legacyRawTextPreview(body), body))
    }

    @Test
    fun refreshedTextClipTitle_recomputesLeadingFragmentOfFirstLine() {
        val body = "Citronpasta med hvidløg og citron\nmer tekst"
        assertEquals("Citronpasta med hvidløg og citron", refreshedTextClipTitle("Citronpasta", body))
    }

    @Test
    fun refreshedTextClipTitle_keepsUserRename() {
        val recipe =
            "Citronpasta med hvidløg\n" +
                "=======================\n" +
                "\n" +
                "## Ingredienser\n" +
                "- 200 g spaghetti\n"
        assertNull(refreshedTextClipTitle("Min pasta-opskrift", recipe))
        assertNull(refreshedTextClipTitle("   ", recipe))
        // A preview already matching the current title stays (idempotent).
        assertNull(refreshedTextClipTitle("Citronpasta med hvidløg", recipe))
        // A rename that is not reconstructible from the body stays.
        assertNull(refreshedTextClipTitle("Madplan uge 42", "Pasta med pesto\nmozzarella"))
    }

    @Test
    fun withDisplayBreaks_insertsZeroWidthSpacesAfterFilenameSeparators() {
        // Display only (#54): "Weekend_i_København" must be able to break
        // after the underscore instead of mid-word.
        val z = DISPLAY_ZERO_WIDTH_SPACE.toString()
        assertEquals("Weekend_${z}i_${z}København", withDisplayBreaks("Weekend_i_København"))
        assertEquals("a-${z}b.${z}c/${z}d", withDisplayBreaks("a-b.c/d"))
    }

    @Test
    fun withDisplayBreaks_keepsPlainTitlesUnchanged() {
        // No separators: the display text is the stored text, byte for byte.
        assertEquals("Citronpasta med hvidløg", withDisplayBreaks("Citronpasta med hvidløg"))
        assertEquals("", withDisplayBreaks(""))
    }

    @Test
    fun withDisplayBreaks_leavesTheStoredTitleUntouched() {
        // The helper is applied to the rendered string only: the title
        // derivation and the raw title used for rename/copy/share never
        // contain a zero-width space.
        val raw = "Weekend_i_København.txt"
        val title = clipListTitle(ContentType.PDF, raw)
        assertEquals("Weekend_i_København.txt", title)
        assertFalse(DISPLAY_ZERO_WIDTH_SPACE in title)
        assertTrue(DISPLAY_ZERO_WIDTH_SPACE in withDisplayBreaks(title))
    }
}
