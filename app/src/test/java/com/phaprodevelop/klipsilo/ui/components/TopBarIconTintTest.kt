@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * #81: top-bar action icons must follow the theme content colour. The editor
 * top bar's eye / PDF / share icons kept [Color.Unspecified] as their tint,
 * which makes Material3 skip the colour filter and paint the vector's own
 * black fill — invisible on a dark top bar.
 */
@RunWith(RobolectricTestRunner::class)
class TopBarIconTintTest {
    @get:org.junit.Rule
    val composeRule = createComposeRule()

    @Test
    fun unspecifiedTintResolvesToTheContentColour() {
        val onSurface = Color(0xFFE6E1E5)
        assertEquals(onSurface, resolveTopBarIconTint(Color.Unspecified, onSurface))
    }

    @Test
    fun explicitTintIsKept() {
        // The ✓ save flash passes the primary colour, error actions pass
        // red — an explicit tint must survive the resolution.
        val primary = Color(0xFFB69DFF)
        val onSurface = Color(0xFFE6E1E5)
        assertEquals(primary, resolveTopBarIconTint(primary, onSurface))
        assertEquals(Color.White, resolveTopBarIconTint(Color.White, onSurface))
    }

    @Test
    fun fitActionBarIconsResolveInADarkScheme() {
        var barContentColor = Color.Unspecified
        // Dark colorScheme, default (Unspecified) tint on every action: the
        // bar must render its icons against the dark surface (they used to
        // be painted black) and stay clickable (#81).
        composeRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                barContentColor = LocalContentColor.current
                FitActionBar(
                    title = "Bearbeiten",
                    actions =
                        listOf(
                            TopBarIconAction(Icons.Filled.Visibility, "Forhåndsvisning") {},
                            TopBarIconAction(Icons.Filled.PictureAsPdf, "Tekst til PDF") {},
                            TopBarIconAction(Icons.Filled.Share, "Del") {},
                        ),
                    onBack = {},
                    backContentDescription = "Tilbage",
                    moreContentDescription = "Flere muligheder",
                )
            }
        }
        composeRule.onNodeWithContentDescription("Forhåndsvisning").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tekst til PDF").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Del").assertIsDisplayed().performClick()
        // Resolving the actions' Unspecified tint against the bar's content
        // colour yields that colour itself — never Color.Unspecified.
        assertEquals(barContentColor, resolveTopBarIconTint(Color.Unspecified, barContentColor))
        org.junit.Assert.assertNotEquals(Color.Unspecified, barContentColor)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun editorSaveKeepsItsLabelWhenTheBarHasRoom() {
        // #95: Save is a labelled trailing control, not another ✓ in the
        // eye / PDF / share row, while the other actions stay icon-only.
        composeRule.setContent {
            MaterialTheme {
                FitActionBar(
                    title = "Text editor",
                    actions =
                        listOf(
                            TopBarIconAction(Icons.Filled.Visibility, "Preview") {},
                            TopBarIconAction(Icons.Filled.PictureAsPdf, "Text to PDF") {},
                            TopBarIconAction(Icons.Filled.Share, "Share") {},
                        ),
                    onBack = {},
                    backContentDescription = "Back",
                    moreContentDescription = "More options",
                    saveAction = TopBarIconAction(Icons.Filled.Check, "Save") {},
                )
            }
        }
        composeRule.onNodeWithText("Save").assertIsDisplayed()
    }
}
