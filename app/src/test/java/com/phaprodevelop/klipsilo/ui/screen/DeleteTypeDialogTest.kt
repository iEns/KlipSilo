@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.screen

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** #102: pick a delete type first; Delete only runs after a selection. */
@RunWith(RobolectricTestRunner::class)
class DeleteTypeDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun deleteIsDisabledUntilATypeIsSelected() {
        // Arrange
        var picked = false
        composeRule.setContent {
            MaterialTheme {
                DeleteTypeDialog(
                    clips =
                        listOf(
                            ClipboardEntry(
                                id = 1L,
                                contentType = ContentType.TEXT,
                                previewText = "note",
                                timestamp = 0L,
                            ),
                        ),
                    onPick = { picked = true },
                    onUnlock = {},
                    onDismiss = {},
                )
            }
        }

        // Assert: the dialog guides the pick and Delete cannot run yet.
        composeRule.onNodeWithText(context.getString(R.string.delete_choose_type)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.delete)).assertIsNotEnabled()

        // Act: choose the Text row, then confirm.
        composeRule.onNodeWithText(context.getString(R.string.text)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.delete)).assertIsEnabled().performClick()

        // Assert
        composeRule.runOnIdle { assertEquals(true, picked) }
    }

    @Test
    fun cancelDismissesWithoutPicking() {
        // Arrange
        var dismissed = false
        var picked = false
        composeRule.setContent {
            MaterialTheme {
                DeleteTypeDialog(
                    clips =
                        listOf(
                            ClipboardEntry(
                                id = 1L,
                                contentType = ContentType.TEXT,
                                previewText = "note",
                                timestamp = 0L,
                            ),
                        ),
                    onPick = { picked = true },
                    onUnlock = {},
                    onDismiss = { dismissed = true },
                )
            }
        }

        // Act
        composeRule.onNodeWithText(context.getString(R.string.cancel)).performClick()

        // Assert
        composeRule.runOnIdle {
            assertEquals(true, dismissed)
            assertEquals(false, picked)
        }
    }
}
