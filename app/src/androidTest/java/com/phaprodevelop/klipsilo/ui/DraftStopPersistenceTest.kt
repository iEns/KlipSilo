package com.phaprodevelop.klipsilo.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.phaprodevelop.klipsilo.MainActivity
import com.phaprodevelop.klipsilo.data.db.SnipDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * #42: typing text into a new clip and dropping the activity to CREATED (the state
 * reached by Home / swipe-away, right before a possible process kill) must persist
 * the draft as exactly one clip row with the typed text — never an empty
 * "(untitled)" artifact, never a duplicate.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DraftStopPersistenceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule(MainActivity::class.java)

    private fun dao() =
        SnipDatabase
            .getInstance(
                InstrumentationRegistry.getInstrumentation().targetContext,
            ).clipboardItemDao()

    @Before
    fun clearClips() {
        runBlocking { dao().deleteAll() }
    }

    @After
    fun clearClipsAgain() {
        runBlocking { dao().deleteAll() }
    }

    @Test
    fun typedDraftText_survivesStopToCreated() {
        // Arrange: open the new-clip editor and type into the text field.
        composeRule
            .onNodeWithTag("klips_new_text_clip_fab")
            .assertExists()
            .performClick()
        composeRule.waitForIdle()
        composeRule
            .onNodeWithTag("klips_editor_text_field")
            .performClick()
            .performTextInput("QA draft R5")
        composeRule.waitForIdle()

        // Act: close the app from the background; ON_STOP must persist the draft.
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)

        // Assert: exactly one clip row, carrying the typed text and its title.
        runBlocking {
            val rows = dao().getAllOnce()
            assertEquals(1, rows.size)
            val row = rows.single()
            assertEquals("QA draft R5", row.textContent)
            assertEquals("QA draft R5", row.previewText)
        }

        // Reopening the app must not duplicate the clip; resume and leave it intact.
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
        runBlocking { assertEquals(1, dao().getAllOnce().size) }
    }
}
