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

    @Test
    fun cjkTitleWrapsPerCharacterInsteadOfEllipsizing() {
        // #62: ja/ko/zh are written without spaces, so a line may start
        // between any two CJK characters. "プライバシーポリシー" used to
        // fall to "プライバシーポリシ…" at large JA; it must wrap instead.
        val decision =
            resolveFitText(
                "プライバシーポリシー",
                maxWidthPx = 60f,
                policy = FitTextPolicy(maxLines = 2, minScale = 0.6f),
                widthOf = ::widthOf,
            )
        assertFalse(decision.ellipsized)
        assertFalse(decision.text.contains(FIT_ELLIPSIS))
        assertEquals("プライバシー\nポリシー", decision.text)
        // The prolonged sound mark is pushed to the next line by the
        // kinsoku rule, never left alone at a line start.
        decision.text.split('\n').forEach { line -> assertFalse(line.startsWith("ー")) }
    }

    @Test
    fun cjkWrapNeverStartsALineWithClosingPunctuation() {
        // Kinsoku (#62): "おはよう。こんにちは" at 65px must not start a
        // line with "。" — the break moves past it instead.
        val decision =
            resolveFitText(
                "おはよう。こんにちは",
                maxWidthPx = 65f,
                policy = FitTextPolicy(maxLines = 2, minScale = 0.6f),
                widthOf = ::widthOf,
            )
        assertFalse(decision.ellipsized)
        decision.text.split('\n').forEach { line ->
            assertFalse("line starts with closing punctuation: $line", line.startsWith("。"))
        }
        // No characters lost or reordered.
        assertEquals("おはよう。こんにちは", decision.text.replace("\n", ""))
    }

    @Test
    fun latinLettersNextToCjkAreNeverSplit() {
        // The per-character rule is for CJK characters only (#62): a Latin
        // run next to them never splits inside itself, while the CJK run
        // wraps per character ("OCRガイド" at 40px wraps "OCRガ"/"イド",
        // never "OC/R…").
        val decision =
            resolveFitText(
                "OCRガイド",
                maxWidthPx = 40f,
                policy = FitTextPolicy(maxLines = 2, minScale = 0.6f),
                widthOf = ::widthOf,
            )
        assertFalse(decision.ellipsized)
        assertEquals("OCRガ\nイド", decision.text)
    }

    @Test
    fun koreanWithSpacesDetectsOnlyHangulWithWhitespace() {
        // #64: the keep-all rule is for Korean labels that use spaces.
        assertTrue(isKoreanWithSpaces("개인정보 처리방침"))
        assertTrue(isKoreanWithSpaces("최대 출력 해상도"))
        // Korean without spaces has no word boundary to prefer.
        assertFalse(isKoreanWithSpaces("개인정보처리방침"))
        // Japanese and Chinese are written without spaces — and even with
        // one, they are not Korean (#64).
        assertFalse(isKoreanWithSpaces("プライバシー ポリシー"))
        assertFalse(isKoreanWithSpaces("隐私 政策"))
        assertFalse(isKoreanWithSpaces("Datenschutzerklärung"))
    }

    @Test
    fun koreanLabelBreaksAtTheSpacesBeforePerCharacterWrap() {
        // #64: the greedy per-character wrap fills the first line into the
        // next word ("개인정보 처리방/침"). The keep-all style breaks at the
        // space instead.
        val decision =
            resolveFitText(
                "개인정보 처리방침",
                maxWidthPx = 80f,
                policy = FitTextPolicy(maxLines = 2, minScale = 1f),
                widthOf = ::widthOf,
            )
        assertFalse(decision.ellipsized)
        assertEquals("개인정보\n처리방침", decision.text)
    }

    @Test
    fun koreanLabelBreaksAtEverySpaceTheWidthAllows() {
        // #64: "최대 출력 해상도" at 55px — "최대 출력" fits, the rest
        // wraps whole ("최대 출력 / 해상도", never "해/상도").
        val decision =
            resolveFitText(
                "최대 출력 해상도",
                maxWidthPx = 55f,
                policy = FitTextPolicy(maxLines = 2, minScale = 1f),
                widthOf = ::widthOf,
            )
        assertFalse(decision.ellipsized)
        assertEquals("최대 출력\n해상도", decision.text)
    }

    @Test
    fun koreanSingleWordStillWrapsPerCharacter() {
        // #64: a Korean word with no space has no word boundary —
        // per-character wrapping stays the line breaking there.
        val decision =
            resolveFitText(
                "개인정보처리방침",
                maxWidthPx = 45f,
                policy = FitTextPolicy(maxLines = 2, minScale = 1f),
                widthOf = ::widthOf,
            )
        assertFalse(decision.ellipsized)
        assertEquals("개인정보\n처리방침", decision.text)
    }

    @Test
    fun koreanWordTooWideForTheLineFallsBackToPerCharacterWrap() {
        // #64: "가 나다라마바사" in a 45px slot — the space wrap cannot fit
        // "나다라마바사" (60px), so the label falls back to the general
        // breaks and the too-wide word wraps per character.
        val decision =
            resolveFitText(
                "가 나다라마바사",
                maxWidthPx = 45f,
                policy = FitTextPolicy(maxLines = 3, minScale = 1f),
                widthOf = ::widthOf,
            )
        assertFalse(decision.ellipsized)
        assertEquals("가 나다\n라마바사", decision.text)
    }

    @Test
    fun japaneseAndChineseKeepPerCharacterBreaking() {
        // #64: no Hangul — the keep-all rule never applies, per-character
        // wrapping is unchanged.
        assertFalse(isKoreanWithSpaces("プライバシー ポリシー"))
        val ja =
            resolveFitText(
                "プライバシーポリシー",
                maxWidthPx = 60f,
                policy = FitTextPolicy(maxLines = 2, minScale = 1f),
                widthOf = ::widthOf,
            )
        assertFalse(ja.ellipsized)
        assertEquals("プライバシー\nポリシー", ja.text)
        val zh =
            resolveFitText(
                "隐私政策",
                maxWidthPx = 25f,
                policy = FitTextPolicy(maxLines = 2, minScale = 1f),
                widthOf = ::widthOf,
            )
        assertFalse(zh.ellipsized)
        assertEquals("隐私\n政策", zh.text)
    }

    @Test
    fun aSoftHyphenIsABreakOpportunityThatShowsAHyphen() {
        // #62: Grok Build inserts U+00AD into long DE/DA strings. The rule
        // must wrap at it, draw a visible hyphen at the line end and never
        // carry the character itself to the next line.
        val decision =
            resolveFitText(
                "Datenschutz\u00ADerklärung",
                maxWidthPx = 80f,
                policy = FitTextPolicy(maxLines = 2, minScale = 0.6f),
                widthOf = ::widthOf,
            )
        assertFalse(decision.ellipsized)
        assertEquals("Datenschutz-\nerklärung", decision.text)
        // The break is a word boundary for the mid-word check.
        assertFalse(isMidWordBreak(decision.text, 12))
    }

    @Test
    fun aSoftHyphenBreakIsPreferredOverPatternHyphenation() {
        // #62: when the string carries a soft hyphen the platform
        // hyphenation is not consulted at all — the U+00AD split happens
        // first.
        val askedWords = mutableListOf<String>()
        val decision =
            resolveFitText(
                "Ausgabe\u00ADresolution",
                maxWidthPx = 60f,
                policy = FitTextPolicy(maxLines = 2, minScale = 0.6f),
                widthOf = ::widthOf,
                hyphenLinesOf = { word, _, _ ->
                    askedWords += word
                    listOf("Aus-", "gabe-resolution")
                },
            )
        assertFalse(decision.ellipsized)
        assertEquals("Ausgabe-\nresolution", decision.text)
        assertTrue("platform hyphenation must not be asked: $askedWords", askedWords.isEmpty())
    }

    @Test
    fun softHyphensStayInvisibleWhenTheLineDoesNotBreakThere() {
        // #62: a soft hyphen that is not used as a break stays in the drawn
        // text (the platform renders it invisibly); the pieces around it
        // wrap normally.
        val decision =
            resolveFitText(
                "OCR-Seite speichern",
                maxWidthPx = 100f,
                policy = FitTextPolicy(maxLines = 2, minScale = 0.6f),
                widthOf = ::widthOf,
            )
        assertFalse(decision.ellipsized)
        assertEquals("OCR-Seite\nspeichern", decision.text)
    }

    @Test
    fun hyphenatesALongWordAtTheFloorInsteadOfEllipsizing() {
        // "Ausgaberesolution" is 170px; in an 80px slot it is wider than the
        // line at every scale down to the 0.5 floor (#62). The platform
        // hyphenation breaks it at a pattern-valid point, so step d renders
        // "Ausgabe-/resolution" instead of "Ausgaberesoluti…".
        val decision =
            resolveFitText(
                "Ausgaberesolution",
                maxWidthPx = 80f,
                policy = FitTextPolicy(maxLines = 4, minScale = 0.5f),
                widthOf = ::widthOf,
                hyphenLinesOf = { word, _, _ ->
                    if (word == "Ausgaberesolution") listOf("Ausgabe-", "resolution") else null
                },
            )
        assertFalse(decision.ellipsized)
        assertEquals("Ausgabe-\nresolution", decision.text)
        assertEquals(0.5f, decision.scale)
        // The break is the pattern-valid one: in the rebuilt word the hyphen
        // is drawn at the line end, so the mid-word check accepts it.
        assertFalse(isMidWordBreak("Ausgabe-resolution", 8))
    }

    @Test
    fun aRawBreakWithoutAHyphenIsStillRejected() {
        // Without a hyphen at the line end the same split is a raw break the
        // rule must never draw (#62).
        assertTrue(isMidWordBreak("Ausgaberesolution", 8))
        assertFalse(isMidWordBreak("Ausgabe-resolution", 8))
    }

    @Test
    fun withoutPatternLocaleTheLongWordStillEllipsizes() {
        // No platform hyphenation available (no patterns): step e applies
        // and the word is cut with an ellipsis, never broken (#62).
        val decision =
            resolveFitText(
                "Ausgaberesolution",
                maxWidthPx = 80f,
                policy = FitTextPolicy(maxLines = 4, minScale = 0.5f),
                widthOf = ::widthOf,
                hyphenLinesOf = { _, _, _ -> null },
            )
        assertTrue(decision.ellipsized)
        assertTrue(decision.text.endsWith(FIT_ELLIPSIS))
        assertFalse(decision.text.contains("\n"))
    }

    @Test
    fun fragmentsShorterThanThreeLettersAreRejected() {
        // "Au-" would leave only two letters before the hyphen (#62).
        val rejected =
            resolveFitText(
                "Ausgaberesolution",
                maxWidthPx = 80f,
                policy = FitTextPolicy(maxLines = 4, minScale = 0.5f),
                widthOf = ::widthOf,
                hyphenLinesOf = { _, _, _ -> listOf("Au-", "sgaberesolution") },
            )
        assertTrue(rejected.ellipsized)
        assertFalse(rejected.text.contains("-\n"))
        // Three letters before the hyphen are accepted.
        val accepted =
            resolveFitText(
                "Ausgaberesolution",
                maxWidthPx = 80f,
                policy = FitTextPolicy(maxLines = 4, minScale = 0.5f),
                widthOf = ::widthOf,
                hyphenLinesOf = { _, _, _ -> listOf("Aus-", "gaberesolution") },
            )
        assertFalse(accepted.ellipsized)
        assertEquals("Aus-\ngaberesolution", accepted.text)
    }

    @Test
    fun hyphenationIsOnlyConsultedAtTheFloorScale() {
        // Steps a–c run untouched: the hyphenation is asked for the word at
        // the floor only (#62). The earlier-break retries (#62) re-ask at the
        // same floor scale, never at a higher one.
        val askedScales = mutableListOf<Float>()
        val decision =
            resolveFitText(
                "Ausgaberesolution",
                maxWidthPx = 80f,
                policy = FitTextPolicy(maxLines = 4, minScale = 0.5f),
                widthOf = ::widthOf,
                hyphenLinesOf = { _, scale, _ ->
                    askedScales += scale
                    listOf("Ausgabe-", "resolution")
                },
            )
        assertFalse(decision.ellipsized)
        assertEquals(listOf(0.5f), askedScales.distinct())
    }

    @Test
    fun aTooShortPlatformFragmentRetriesAtANarrowerWidth() {
        // The platform lays a word out with one break choice per width. When
        // that choice ends in a fragment with fewer than three letters
        // ("…resoluti-/on"), the rule used to give up and the row fell back
        // to the ellipsis at large DE (#62). It must instead break earlier,
        // the way a narrower layout does ("Ausgabe-/resolution").
        val widths = mutableListOf<Float>()
        val decision =
            resolveFitText(
                "Maximale Ausgaberesolution",
                maxWidthPx = 80f,
                policy = FitTextPolicy(maxLines = 4, minScale = 0.5f),
                widthOf = ::widthOf,
                hyphenLinesOf = { word, _, maxWidthPx ->
                    widths += maxWidthPx
                    // The platform's choice for the full width: the greedy
                    // ti/on break with its two-letter tail.
                    if (word == "Ausgaberesolution" && maxWidthPx >= 79f) {
                        listOf("Ausgaberesoluti-", "on")
                    } else if (word == "Ausgaberesolution") {
                        listOf("Ausgabe-", "resolution")
                    } else {
                        null
                    }
                },
            )
        assertFalse(decision.ellipsized)
        assertEquals("Maximale\nAusgabe-\nresolution", decision.text)
        // The first ask is at the row width; at least one retry is narrower.
        assertTrue(widths.size > 1)
        assertTrue(widths.last() < widths.first())
    }

    @Test
    fun aTooShortPlatformFragmentThatNeverImprovesKeepsTheEllipsis() {
        // When no width yields valid fragments the fallback stays step e (#62).
        val decision =
            resolveFitText(
                "Ausgaberesolution",
                maxWidthPx = 80f,
                policy = FitTextPolicy(maxLines = 4, minScale = 0.5f),
                widthOf = ::widthOf,
                hyphenLinesOf = { _, _, _ -> listOf("Ausgaberesoluti-", "on") },
            )
        assertTrue(decision.ellipsized)
        assertTrue(decision.text.endsWith(FIT_ELLIPSIS))
    }

    @Test
    fun onlyTheTooWideWordIsHyphenatedInTheRestOfTheTitle() {
        // "Maximale Ausgaberesolution": "Maximale" wraps, the long word
        // hyphenates, the other text is untouched (#62).
        val decision =
            resolveFitText(
                "Maximale Ausgaberesolution",
                maxWidthPx = 80f,
                policy = FitTextPolicy(maxLines = 4, minScale = 0.5f),
                widthOf = ::widthOf,
                hyphenLinesOf = { word, _, _ ->
                    if (word == "Ausgaberesolution") listOf("Ausgabe-", "resolution") else null
                },
            )
        assertFalse(decision.ellipsized)
        assertEquals("Maximale\nAusgabe-\nresolution", decision.text)
        assertEquals(3, decision.maxLines)
    }

    @Test
    fun hyphenatedWordThatStillDoesNotFitKeepsTheEllipsis() {
        // Fragments that do not fit the line either way fall back to step e.
        val decision =
            resolveFitText(
                "Ausgaberesolution",
                maxWidthPx = 40f,
                policy = FitTextPolicy(maxLines = 2, minScale = 0.5f),
                widthOf = ::widthOf,
                hyphenLinesOf = { _, _, _ -> listOf("Ausgabe-", "resolution") },
            )
        assertTrue(decision.ellipsized)
        assertTrue(decision.text.endsWith(FIT_ELLIPSIS))
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
