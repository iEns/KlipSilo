@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure fit-text and action-bar decisions. Width is 10px per character at
 * scale 1 (U+200B is free) so the cases do not depend on a typeface.
 */
class FitTextPolicyTest {
    private fun widthOf(
        text: String,
        scale: Float,
    ): Float = text.count { it != '\u200B' } * 10f * scale

    private fun resolve(
        text: String,
        width: Float,
        maxLines: Int = 2,
        minScale: Float = 1f,
    ) = resolveFitText(text, width, FitTextPolicy(maxLines, minScale), ::widthOf)

    @Test
    fun oneLineAtTheStyleSizeWhenItFits() {
        val decision = resolve("Anmerken", width = 80f)
        assertEquals(1f, decision.scale)
        assertEquals("Anmerken", decision.text)
        assertEquals(1, decision.maxLines)
        assertFalse(decision.ellipsized)
    }

    @Test
    fun wrapsAtAHyphenBeforeShrinking() {
        // "OCR-Seite" is 90px. At 50px it wraps after the hyphen, still at
        // the style size (#45).
        val decision = resolve("OCR-Seite", width = 50f, minScale = 0.6f)
        assertEquals(1f, decision.scale)
        assertEquals("OCR-\nSeite", decision.text)
        assertFalse(decision.ellipsized)
        assertFalse(decision.text.contains("OCR-Se"))
    }

    @Test
    fun wrapsGermanSavePageLabelOnTheWordBoundary() {
        val decision = resolve("Seite speichern", width = 100f, minScale = 0.75f)
        assertEquals(1f, decision.scale)
        assertEquals("Seite\nspeichern", decision.text)
        assertFalse(decision.ellipsized)
    }

    @Test
    fun wrapsDanishSavePageLabelWithoutPushingABreakInsideTheWord() {
        val decision = resolve("Gem side", width = 50f, minScale = 0.6f)
        assertEquals("Gem\nside", decision.text)
        assertFalse(decision.ellipsized)
        assertEquals(1f, decision.scale)
    }

    @Test
    fun singleWordShrinksAndIsNeverSplit() {
        // "Auswahl" is 70px. It fits at 0.5 (35px) inside a 40px slot, and
        // the text is still the whole word (#50).
        val decision = resolve("Auswahl", width = 40f, maxLines = 1, minScale = 0.45f)
        assertFalse(decision.ellipsized)
        assertEquals("Auswahl", decision.text)
        assertFalse(decision.text.contains("\n"))
        assertTrue(decision.scale <= 0.6f)
        assertTrue(decision.scale >= 0.45f)
    }

    @Test
    fun singleWordThatCannotFitIsEllipsizedNotBroken() {
        val decision = resolve("Auswahl", width = 30f, maxLines = 2, minScale = 0.5f)
        assertTrue(decision.ellipsized)
        assertTrue(decision.text.endsWith(FIT_ELLIPSIS))
        assertFalse(decision.text.contains("\n"))
        assertFalse(decision.text.contains("Auswa\n"))
    }

    @Test
    fun dotSlashAndUnderscoreAreBreakOpportunities() {
        // 40px holds "a.b/" (4 chars) and leaves "c_d" for the next line.
        val decision = resolve("a.b/c_d", width = 40f, minScale = 1f)
        assertFalse(decision.ellipsized)
        assertEquals("a.b/\nc_d", decision.text)
        assertFalse(isMidWordBreak("a.b/c_d", 4))
        assertFalse(isMidWordBreak("file.name/path_here", 4))
        assertFalse(isMidWordBreak("file.name/path_here", 10))
    }

    @Test
    fun zeroWidthSpaceIsABreakAndDoesNotCountAsWidth() {
        val zwsp = "\u200B"
        val text = "Event_$zwsp" + "Registration_$zwsp" + "Form"
        // 180px holds "Registration_" + "Form" (17 chars) on the second line.
        val decision = resolve(text, width = 180f, minScale = 1f)
        assertEquals(1f, decision.scale)
        assertFalse(decision.ellipsized)
        assertTrue(decision.text.contains("\n"))
        assertFalse(decision.text.contains(FIT_ELLIPSIS))
    }

    @Test
    fun clipTitleStaysFullSizeOnTwoLinesInsteadOfShrinkingToOne() {
        // Narrower than two of the three parts: two lines, style size, the
        // cut is on the last line (#54). "Event_Registration_Form" is 23
        // chars. 80px holds 8 chars.
        val text = "Event_Registration_Form"
        val decision = resolve(text, width = 80f, maxLines = 2, minScale = CLIP_TITLE_MIN_SCALE)
        assertEquals(1f, decision.scale)
        val lines = decision.text.split('\n')
        assertEquals(2, lines.size)
        assertTrue(lines.last().endsWith(FIT_ELLIPSIS))
        assertTrue(lines.first().startsWith("Event_"))
        assertNoInteriorBreak(text, decision.text)
    }

    @Test
    fun spacedTitleEllipsizesTheSecondFullSizeLine() {
        val text = "HTML from Weekend i København"
        val decision = resolve(text, width = 160f, maxLines = 2, minScale = 1f)
        assertEquals(1f, decision.scale)
        val lines = decision.text.split('\n')
        assertEquals(2, lines.size)
        assertEquals("HTML from", lines[0])
        assertTrue(lines[1].endsWith(FIT_ELLIPSIS))
        assertFalse(lines[1].contains("København"))
    }

    @Test
    fun longFilenameFitsOnTwoFullLinesWhenTheSlotAllowsIt() {
        // 200px holds 20 chars: "Event_Registration_" (19) + "Form".
        val decision = resolve("Event_Registration_Form", width = 200f, minScale = 1f)
        assertEquals(1f, decision.scale)
        assertEquals("Event_Registration_\nForm", decision.text)
        assertFalse(decision.ellipsized)
    }

    @Test
    fun uniformGroupUsesTheScaleTheLongestLabelNeeds() {
        val labels = listOf("Auswahl", "Text", "Linie", "Stift", "Marker")
        val decisions =
            resolveUniformFit(
                labels,
                slotWidthPx = 40f,
                policy = FitTextPolicy(maxLines = 1, minScale = TOOL_LABEL_MIN_SCALE),
                widthOf = ::widthOf,
            )
        val scale = decisions.map { it.scale }.distinct()
        assertEquals(1, scale.size)
        assertTrue(decisions.none { it.ellipsized })
        assertEquals(labels, decisions.map { it.text })
        // "Auswahl" is 70px, so the shared scale is 0.55 (38.5px) or 0.50.
        assertTrue(scale.single() < 1f)
        assertTrue(scale.single() >= TOOL_LABEL_MIN_SCALE)
    }

    @Test
    fun uniformGroupDoesNotMixSizesWhenOneLabelMustEllipsize() {
        val decisions =
            resolveUniformFit(
                listOf("Auswahl", "Text"),
                slotWidthPx = 20f,
                policy = FitTextPolicy(maxLines = 1, minScale = 0.5f),
                widthOf = ::widthOf,
            )
        assertEquals(decisions[0].scale, decisions[1].scale)
        assertEquals(0.5f, decisions[0].scale)
        assertTrue(decisions[0].ellipsized)
        assertFalse(decisions[1].ellipsized)
        assertEquals("Text", decisions[1].text)
    }

    @Test
    fun labelledActionsStayWhenTheyFitOnTwoLines() {
        val plan =
            planActionBar(
                availablePx = 400f,
                overflowPx = 40f,
                pinnedPx = 120f,
                iconOnlyPx = listOf(40f, 40f),
                labelledPxAtScale =
                    listOf(
                        1f to listOf(90f, 80f),
                    ),
            )
        assertEquals(listOf(BarItemMode.Labelled, BarItemMode.Labelled), plan.modes)
        assertEquals(1f, plan.labelScale)
    }

    @Test
    fun actionsBecomeIconOnlyWhenLabelsDoNotFit() {
        // Room after the pager and the overflow button is 140px. Two German
        // labels need 200px even at the floor; two icons need 80px (#45).
        val plan =
            planActionBar(
                availablePx = 300f,
                overflowPx = 40f,
                pinnedPx = 120f,
                iconOnlyPx = listOf(40f, 40f),
                labelledPxAtScale =
                    listOf(
                        1f to listOf(120f, 110f),
                        0.75f to listOf(100f, 100f),
                    ),
            )
        assertEquals(listOf(BarItemMode.IconOnly, BarItemMode.IconOnly), plan.modes)
        assertNull(plan.labelScale)
    }

    @Test
    fun leftoverActionsMoveIntoTheOverflowMenu() {
        // Room is 70px: the first icon fits, the second does not. The
        // overflow button is already reserved, so it stays on screen.
        val plan =
            planActionBar(
                availablePx = 200f,
                overflowPx = 40f,
                pinnedPx = 90f,
                iconOnlyPx = listOf(50f, 50f),
                labelledPxAtScale = listOf(1f to listOf(80f, 80f)),
            )
        assertEquals(listOf(BarItemMode.IconOnly, BarItemMode.Overflow), plan.modes)
        val visible = 90f + 50f + 40f
        assertTrue(visible <= 200f)
    }

    @Test
    fun nothingIsPlacedPastTheReservedOverflowButton() {
        val available = 294f
        val overflow = 48f
        val pinned = 180f
        val plan =
            planActionBar(
                availablePx = available,
                overflowPx = overflow,
                pinnedPx = pinned,
                iconOnlyPx = listOf(48f, 48f),
                labelledPxAtScale = listOf(1f to listOf(160f, 140f), 0.6f to listOf(120f, 100f)),
            )
        var used = pinned + overflow
        plan.modes.forEachIndexed { index, mode ->
            if (mode == BarItemMode.IconOnly) used += 48f
            if (mode == BarItemMode.Labelled) error("labels do not fit this bar")
        }
        assertTrue(used <= available + 0.5f)
        assertTrue(plan.modes.any { it == BarItemMode.Overflow })
    }

    @Test
    fun deAndDaLabelsPreferASharedScaleThenIcons() {
        // Monospace stand-in for the viewer bar at a large font: the Danish
        // pair wraps, the German pair is wider than the room at every scale.
        val deRoom =
            planActionBar(
                availablePx = 294f,
                overflowPx = 40f,
                pinnedPx = 170f,
                iconOnlyPx = listOf(40f, 40f),
                labelledPxAtScale =
                    listOf(
                        1f to listOf(widthOf("Seite speichern", 1f), widthOf("OCR-Seite", 1f)),
                        0.6f to listOf(widthOf("Seite speichern", 0.6f), widthOf("OCR-Seite", 0.6f)),
                    ),
            )
        assertEquals(listOf(BarItemMode.IconOnly, BarItemMode.IconOnly), deRoom.modes)

        val daWrapped =
            listOf("Gem side", "OCR side").map { label ->
                val lines = wrapLines(label, maxWidthPx = 50f, scale = 1f, maxLines = 2, ::widthOf)
                assertEquals(listOf(label.substringBefore(' '), label.substringAfter(' ')), lines)
                50f
            }
        val daPlan =
            planActionBar(
                availablePx = 320f,
                overflowPx = 40f,
                pinnedPx = 150f,
                iconOnlyPx = listOf(40f, 40f),
                labelledPxAtScale = listOf(1f to daWrapped),
            )
        assertEquals(listOf(BarItemMode.Labelled, BarItemMode.Labelled), daPlan.modes)
        assertEquals(1f, daPlan.labelScale)
    }

    private fun assertNoInteriorBreak(
        original: String,
        shown: String,
    ) {
        var index = 0
        val lines = shown.removeSuffix(FIT_ELLIPSIS).split('\n')
        for ((lineIndex, line) in lines.withIndex()) {
            val start = original.indexOf(line, index)
            assertTrue("line '$line' not in '$original' from $index", start >= 0)
            if (lineIndex > 0 && start > 0) {
                assertFalse(isMidWordBreak(original, start))
            }
            index = start + line.length
        }
    }
}
