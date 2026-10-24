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

    @Test
    fun unlockRowAsksForConfirmationBeforeUnlocking() {
        // Arrange
        var unlocked = 0
        composeRule.setContent {
            MaterialTheme {
                DeleteTypeDialog(
                    clips = listOf(lockedTextEntry(1L), lockedTextEntry(2L)),
                    onPick = {},
                    onUnlock = { unlocked++ },
                    onDismiss = {},
                )
            }
        }

        // Act
        composeRule.onNodeWithText(context.getString(R.string.locked_clips)).performClick()

        // Assert
        composeRule.onNodeWithText(context.getString(R.string.unlock_all)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.unlock_all_confirm_count, 2)).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, unlocked) }
    }

    @Test
    fun cancelOnUnlockConfirmationKeepsLocks() {
        // Arrange
        var unlocked = 0
        composeRule.setContent {
            MaterialTheme {
                DeleteTypeDialog(
                    clips = listOf(lockedTextEntry(1L), lockedTextEntry(2L)),
                    onPick = {},
                    onUnlock = { unlocked++ },
                    onDismiss = {},
                )
            }
        }

        // Act
        composeRule.onNodeWithText(context.getString(R.string.locked_clips)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.cancel)).performClick()

        // Assert
        composeRule.onNodeWithText(context.getString(R.string.delete_choose_type)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.unlock_all)).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, unlocked) }
    }

    @Test
    fun confirmingUnlockAllUnlocks() {
        // Arrange
        var unlocked = 0
        composeRule.setContent {
            MaterialTheme {
                DeleteTypeDialog(
                    clips = listOf(lockedTextEntry(1L), lockedTextEntry(2L)),
                    onPick = {},
                    onUnlock = { unlocked++ },
                    onDismiss = {},
                )
            }
        }

        // Act
        composeRule.onNodeWithText(context.getString(R.string.locked_clips)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.unlock)).performClick()

        // Assert
        composeRule.runOnIdle { assertEquals(1, unlocked) }
    }

    private fun lockedTextEntry(id: Long) =
        ClipboardEntry(
            id = id,
            contentType = ContentType.TEXT,
            previewText = "locked note",
            timestamp = 0L,
            isLocked = true,
        )
}
