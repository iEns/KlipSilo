@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.data

import com.phaprodevelop.klipsilo.data.db.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun clipListTitle_stripsTextAndLeavesOtherTypes() {
        assertEquals("Lemon Garlic Pasta", clipListTitle(ContentType.TEXT, "# Lemon Garlic Pasta"))
        assertEquals("notes.pdf", clipListTitle(ContentType.PDF, "notes.pdf"))
    }
}
