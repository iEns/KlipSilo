@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.components

import android.content.Context
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Real text measurement at the sizes Marcus retested. Density cancels out of
 * a width-in-dp versus an sp size, so a density of 1 with the font scale
 * under test matches font scale 2.0 at density 588 (about 294dp on a 1080px
 * screen) and a 320dp window.
 */
@RunWith(RobolectricTestRunner::class)
class FitTextLayoutTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun titlesFitFullyAtFontScaleTwoOnANarrowWindow() {
        val style = TextStyle(fontSize = 22.sp, lineHeight = 28.sp)
        // Back icon + check icon, plus a few dp of padding.
        for (windowDp in listOf(294f, 320f)) {
            val titleWidth = windowDp - 48f - 48f - 8f
            for (title in listOf("Formular ausfüllen", "Seiten bearbeiten", "Udfyld formular", "Anmerken")) {
                val decision =
                    measure(
                        title,
                        style,
                        widthDp = titleWidth,
                        fontScale = 2f,
                        policy = FitTextPolicy(maxLines = 2, minScale = TOP_BAR_TITLE_MIN_SCALE),
                    )
                assertFalse("$title @ ${windowDp}dp was cut: ${decision.text}", decision.ellipsized)
                assertFalse(decision.text.contains(FIT_ELLIPSIS))
                assertTrue(decision.scale >= TOP_BAR_TITLE_MIN_SCALE)
                assertTrue(decision.maxLines <= 2)
            }
        }
    }

    @Test
    fun titlesFitFullyAt320dpAndNormalScale() {
        val style = TextStyle(fontSize = 22.sp, lineHeight = 28.sp)
        val titleWidth = 320f - 48f - 48f - 8f
        for (title in listOf("Formular ausfüllen", "Seiten bearbeiten", "Udfyld formular", "Anmerken")) {
            val decision =
                measure(
                    title,
                    style,
                    widthDp = titleWidth,
                    fontScale = 1f,
                    policy = FitTextPolicy(maxLines = 2, minScale = TOP_BAR_TITLE_MIN_SCALE),
                )
            assertEquals(title, decision.text)
            assertFalse(decision.ellipsized)
        }
    }

    @Test
    fun toolLabelsShareOneUncutSizeAtFontScaleTwo() {
        val style = TextStyle(fontSize = 11.sp, lineHeight = 16.sp)
        val labels = listOf("Auswahl", "Text", "Linie", "Stift", "Marker")
        for (windowDp in listOf(294f, 320f, 360f)) {
            val slot = (windowDp - 16f) / labels.size
            val decisions =
                measureUniform(
                    labels,
                    style,
                    slotDp = slot,
                    fontScale = 2f,
                    policy = FitTextPolicy(maxLines = 1, minScale = TOOL_LABEL_MIN_SCALE),
                )
            assertEquals(1, decisions.map { it.scale }.distinct().size)
            decisions.forEach { decision ->
                assertFalse(decision.ellipsized)
                assertFalse(decision.text.contains(FIT_ELLIPSIS))
                assertFalse(decision.text.contains("\n"))
            }
            assertEquals(labels, decisions.map { it.text })
        }
    }

    @Test
    fun clipTitlesUseTwoFullSizeLines() {
        val style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
        // Compact row: padding, icon slot, gap, overflow button.
        val titleWidth = 294f - 24f - 32f - 8f - 40f - 4f
        val filename = "Event_\u200BRegistration_\u200BForm"
        val filenameDecision =
            measure(
                filename,
                style,
                widthDp = titleWidth,
                fontScale = 2f,
                policy = FitTextPolicy(maxLines = 2, minScale = CLIP_TITLE_MIN_SCALE),
            )
        assertEquals(1f, filenameDecision.scale)
        assertTrue(filenameDecision.maxLines <= 2)
        assertFalse(filenameDecision.text.startsWith(FIT_ELLIPSIS))
        val spaced =
            measure(
                "HTML from Weekend i København",
                style,
                widthDp = titleWidth,
                fontScale = 2f,
                policy = FitTextPolicy(maxLines = 2, minScale = CLIP_TITLE_MIN_SCALE),
            )
        assertEquals(1f, spaced.scale)
        if (spaced.ellipsized) {
            assertEquals(2, spaced.text.count { it == '\n' } + 1)
            assertTrue(spaced.text.trimEnd().endsWith(FIT_ELLIPSIS))
        }
        assertTrue(spaced.text.startsWith("HTML"))
    }

    @Test
    fun viewerLabelsEitherFitOnTwoLinesOrReportTheWidthTheyNeed() {
        val style = TextStyle(fontSize = 11.sp, lineHeight = 16.sp)
        val density = Density(density = 1f, fontScale = 2f)
        val measurer = measurer(density)
        for (label in listOf("Seite speichern", "OCR-Seite", "Gem side", "OCR side")) {
            val width =
                minWidthForLines(label, scale = 1f, maxLines = 2) { text, scale ->
                    textWidthPx(measurer, text, style, style.fontSize.value, scale)
                }
            val decision =
                resolveFitText(
                    label,
                    maxWidthPx = width,
                    policy = FitTextPolicy(maxLines = 2, minScale = 0.75f),
                    widthOf = { text, scale ->
                        textWidthPx(measurer, text, style, style.fontSize.value, scale)
                    },
                )
            assertFalse("$label cut inside its own min width: ${decision.text}", decision.ellipsized)
            assertTrue(decision.maxLines <= 2)
        }
    }

    @Test
    fun settingsRowLabelsFitAtFontScaleTwoWithoutAnEllipsis() {
        // #62: "Maximale Ausgaberesolution" broke mid-word as
        // "Ausgabere / solution" and "Datenschutzerklärung" as
        // "Datenschutzerkl / ärung" in the Settings rows.
        val style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
        val density = Density(density = 1f, fontScale = 2f)
        val measurer = measurer(density)
        // The slider row shares the line with the value text ("6000 px").
        val valueWidth = textWidthPx(measurer, "6000 px", style, style.fontSize.value, 1f)
        for (windowDp in listOf(294f, 320f)) {
            // 12dp screen padding, 24dp icon, 12dp gap, value text, 8dp slack.
            val labelWidth = windowDp - 24f - 24f - 12f - valueWidth - 8f
            val labels =
                listOf(
                    "Maximale Ausgaberesolution",
                    "Maks. output-opløsning",
                    "Datenschutzerklärung",
                    "Einschränkungen",
                )
            for (label in labels) {
                val decision =
                    measure(
                        label,
                        style,
                        widthDp = labelWidth,
                        fontScale = 2f,
                        policy = FitTextPolicy(maxLines = 4, minScale = 0.6f),
                    )
                assertFalse("$label @ ${windowDp}dp was cut: ${decision.text}", decision.ellipsized)
                assertFalse(decision.text.contains(FIT_ELLIPSIS))
                assertTrue(decision.maxLines <= 4)
                // The policy never starts a wrapped line inside a word; the
                // decision text is the label itself apart from added breaks.
                decision.text.split('\n').forEach { line ->
                    assertTrue(label.replace(" ", "").startsWith(line.replace(" ", "").take(1)))
                }
            }
        }
    }

    @Test
    fun selectedNavLabelsFitAtFontScaleTwoOnOneLine() {
        // #62: the selected Settings tab showed "Einstellun…" / "Indstilling…".
        val style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
        for (windowDp in listOf(294f, 320f)) {
            val slot = windowDp / 3f - 16f
            for (label in listOf("Einstellungen", "Indstillinger", "Werkzeuge")) {
                val decision =
                    measure(
                        label,
                        style,
                        widthDp = slot,
                        fontScale = 2f,
                        policy = FitTextPolicy(maxLines = 1, minScale = NAV_SELECTED_LABEL_MIN_SCALE),
                    )
                assertFalse("$label @ ${windowDp}dp was cut: ${decision.text}", decision.ellipsized)
                assertFalse(decision.text.contains(FIT_ELLIPSIS))
                assertEquals(1, decision.maxLines)
            }
        }
    }

    @Test
    fun fontCardsShareOneUncutSizeAtFontScaleTwo() {
        // #62: the 2×2 card group at large font scales. One slot is half the
        // screen minus the 12dp screen padding and the 8dp gap.
        val style = TextStyle(fontSize = 10.sp, lineHeight = 14.sp)
        // #125: the buttons are named System / Sans Serif / Serif / Mono.
        val labels = listOf("System", "Sans Serif", "Serif", "Mono")
        for (windowDp in listOf(294f, 320f)) {
            val slot = (windowDp - 24f - 8f) / 2f
            val decisions =
                measureUniform(
                    labels,
                    style,
                    slotDp = slot,
                    fontScale = 2f,
                    policy = FitTextPolicy(maxLines = 2, minScale = LABEL_FONT_FLOOR_FRACTION),
                )
            assertEquals(1, decisions.map { it.scale }.distinct().size)
            decisions.forEach { decision ->
                assertFalse(decision.ellipsized)
                assertFalse(decision.text.contains(FIT_ELLIPSIS))
                assertTrue(decision.maxLines <= 2)
            }
            assertEquals(labels, decisions.map { it.text })
        }
    }

    @Test
    fun privacyTitleFitsAtFontScaleTwoWhenThePlatformHyphenates() {
        // #62: at large font sizes the single-word title "Datenschutzerklärung"
        // ellipsized as "Datenschutzerklä…". With pattern-valid fragments the
        // floor scale renders "Datenschutz-/erklärung" instead.
        val style = TextStyle(fontSize = 22.sp, lineHeight = 28.sp)
        val measurer = measurer(Density(density = 1f, fontScale = 2f))
        val hyphenLinesOf: HyphenLinesOf = { word, _, _ ->
            if (word == "Datenschutzerklärung") listOf("Datenschutz-", "erklärung") else null
        }
        val floor = TOP_BAR_TITLE_MIN_SCALE
        val full = textWidthPx(measurer, "Datenschutzerklärung", style, style.fontSize.value, floor)
        val widestFragment =
            listOf("Datenschutz-", "erklärung").maxOf {
                textWidthPx(measurer, it, style, style.fontSize.value, floor)
            }
        // A slot between the two: the whole word is too wide at the floor,
        // the hyphenated fragments fit.
        val titleWidth = (widestFragment + full) / 2f
        val decision =
            resolveFitText(
                "Datenschutzerklärung",
                maxWidthPx = titleWidth,
                policy = FitTextPolicy(maxLines = 2, minScale = TOP_BAR_TITLE_MIN_SCALE),
                widthOf = { sample, scale ->
                    textWidthPx(measurer, sample, style, style.fontSize.value, scale)
                },
                hyphenLinesOf = hyphenLinesOf,
            )
        assertFalse("@${titleWidth}dp was cut: ${decision.text}", decision.ellipsized)
        assertEquals("Datenschutz-\nerklärung", decision.text)
        assertEquals(TOP_BAR_TITLE_MIN_SCALE, decision.scale)
    }

    @Test
    fun editorTitleReservesTheHyphenatedFragmentWidth() {
        // #62: the editor bar reserves the title slot from the hyphenated
        // fragments, not from the whole word, so the actions can stay.
        val style = TextStyle(fontSize = 22.sp, lineHeight = 28.sp)
        val measurer = measurer(Density(density = 1f, fontScale = 2f))
        val plain = titleFragmentFloorWidthPx(measurer, "Bearbeiten", style, null)
        val hyphenated =
            titleFragmentFloorWidthPx(
                measurer,
                "Bearbeiten",
                style,
            ) { word, _, maxWidthPx ->
                if (word == "Bearbeiten" && maxWidthPx > 0f) {
                    listOf("Bearb-", "eiten")
                } else {
                    null
                }
            }
        assertTrue("plain width must be positive", plain > 0f)
        assertTrue("hyphenated $hyphenated must be narrower than $plain", hyphenated < plain)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun settingsRowHyphenatesWhenThePlatformsFirstBreakIsTooShort() {
        // #62: "Maximale Ausgaberesolution" at large DE. The row's slot next
        // to the value text is about 0.9x the floor width of the long word
        // (514px vs 588px at density 588), and the platform lays a word out
        // with ONE break choice per width: for that slot the greedy choice
        // ends in the two-letter tail "…resoluti-/on", the three-letter rule
        // rejected it, and the row fell back to "Ausgaberesoluti…". The rule
        // must break earlier instead — what a narrower layout chooses
        // ("Ausgabe-/resolution").
        val style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
        val density = Density(density = 1f, fontScale = 2f)
        val measurer = measurer(density)
        val base = style.fontSize.value
        val hyphenLinesOf: HyphenLinesOf = { word, scale, maxWidthPx ->
            hyphenatedWordLines(
                measurer = measurer,
                word = word,
                style = scaledLabelStyle(style, base * scale, base),
                localeList = LocaleList("de"),
                maxLines = 4,
                maxWidthPx = maxWidthPx,
            )
        }
        val wordAtFloor = textWidthPx(measurer, "Ausgaberesolution", style, base, 0.6f)
        val slot = wordAtFloor * 0.9f
        val decision =
            resolveFitText(
                "Maximale Ausgaberesolution",
                maxWidthPx = slot,
                policy = FitTextPolicy(maxLines = 4, minScale = 0.6f),
                widthOf = { sample, scale ->
                    textWidthPx(measurer, sample, style, base, scale)
                },
                hyphenLinesOf = hyphenLinesOf,
            )
        assertFalse("cut: ${decision.text}", decision.ellipsized)
        assertFalse(decision.text.contains(FIT_ELLIPSIS))
        assertTrue(decision.text, decision.text.contains("-\n"))
        assertTrue(decision.maxLines <= 4)
    }

    private fun measure(
        text: String,
        style: TextStyle,
        widthDp: Float,
        fontScale: Float,
        policy: FitTextPolicy,
    ): FitTextDecision {
        val density = Density(density = 1f, fontScale = fontScale)
        val measurer = measurer(density)
        val base = style.fontSize.value
        return resolveFitText(text, widthDp, policy, widthOf = { sample, scale ->
            textWidthPx(measurer, sample, style, base, scale)
        })
    }

    private fun measureUniform(
        texts: List<String>,
        style: TextStyle,
        slotDp: Float,
        fontScale: Float,
        policy: FitTextPolicy,
    ): List<FitTextDecision> {
        val density = Density(density = 1f, fontScale = fontScale)
        val measurer = measurer(density)
        val base = style.fontSize.value
        return resolveUniformFit(texts, slotDp, policy) { sample, scale ->
            textWidthPx(measurer, sample, style, base, scale)
        }
    }

    private fun measurer(density: Density) =
        androidx.compose.ui.text.TextMeasurer(
            defaultFontFamilyResolver = createFontFamilyResolver(context),
            defaultDensity = density,
            defaultLayoutDirection = LayoutDirection.Ltr,
        )
}
