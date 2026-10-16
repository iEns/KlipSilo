@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.repository.ClipboardRepository
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.phaprodevelop.klipsilo.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A new text clip never gets an empty "(untitled)" row: the editor starts from an
 * in-memory draft and the row is created only with non-empty content, on Back/Save
 * or the ON_STOP lifecycle exit (#39 regression, #42).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ClipboardViewModelEmptyDraftTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var app: Application
    private lateinit var dao: FakeClipboardItemDao
    private lateinit var repo: ClipboardRepository
    private lateinit var viewModel: ClipboardViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        dao = FakeClipboardItemDao()
        repo = ClipboardRepository(dao, app)
        viewModel =
            ClipboardViewModel(
                application = app,
                repository = repo,
                undoTimeoutMs = 4_000L,
                ioDispatcher = mainDispatcherRule.dispatcher,
            )
        mainDispatcherRule.scheduler.advanceUntilIdle()
    }

    private fun newDraftId(): Long {
        viewModel.createEmptyClip()
        mainDispatcherRule.scheduler.advanceUntilIdle()
        return viewModel.navigateToNewClip.value ?: error("new clip id missing")
    }

    @Test
    fun newEmptyDraft_noRowInsertedUpFront() {
        // Arrange / Act
        val id = newDraftId()

        // Assert: the editor opens on the in-memory draft; no empty clip row exists.
        val draft = runBlocking { viewModel.getClip(id) }
        assertNotNull(draft)
        runBlocking { assertNull(repo.getClip(id)) }
        assertTrue(viewModel.clips.value.isEmpty())
    }

    @Test
    fun newEmptyDraft_lifecycleExitWithBlankText_persistsNothing() {
        // Arrange
        val id = newDraftId()
        val draft = runBlocking { viewModel.getClip(id) }!!

        // Act
        val saved = viewModel.saveDraftOnLifecycleExit(draft, "", "")
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        assertFalse(saved)
        runBlocking { assertNull(repo.getClip(id)) }
        assertTrue(viewModel.clips.value.isEmpty())
    }

    @Test
    fun newDraftWithText_rowCreatedOnLifecycleExit() {
        // Arrange
        val id = newDraftId()
        val draft = runBlocking { viewModel.getClip(id) }!!

        // Act
        val saved = viewModel.saveDraftOnLifecycleExit(draft, "QA draft R5", "")
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert: the row exists exactly once, with the typed text and its title.
        assertTrue(saved)
        val row = runBlocking { repo.getClip(id) }
        assertNotNull(row)
        assertEquals("QA draft R5", row!!.textContent)
        assertEquals("QA draft R5", row.previewText)
        assertEquals(1, viewModel.clips.value.size)
    }

    @Test
    fun newDraftWithText_repeatedLifecycleExits_noDuplicateRow() {
        // Arrange
        val id = newDraftId()
        val draft = runBlocking { viewModel.getClip(id) }!!
        assertTrue(viewModel.saveDraftOnLifecycleExit(draft, "first", ""))
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Act: stop again with more edits — the same row must be updated.
        val savedAgain = viewModel.saveDraftOnLifecycleExit(draft, "first and second", "first")
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        assertTrue(savedAgain)
        val rows = runBlocking { dao.getAllOnce() }
        assertEquals(1, rows.size)
        assertEquals("first and second", rows.single().textContent)
    }

    @Test
    fun existingClipEdit_persistedOnLifecycleExit() {
        // Arrange: an established text clip with unsaved edits in the editor.
        val established =
            ClipboardEntry(
                id = 42L,
                contentType = ContentType.TEXT,
                textContent = "old body",
                filePath = null,
                previewText = "old body",
                timestamp = 1_000L,
            )
        runBlocking { repo.insertEntry(established) }
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Act
        val saved = viewModel.saveDraftOnLifecycleExit(established, "edited body", "old body")
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        assertTrue(saved)
        assertEquals("edited body", runBlocking { repo.getClip(42L)!!.textContent })
    }

    @Test
    fun existingClipUnchanged_notRewrittenOnLifecycleExit() {
        // Arrange
        val established =
            ClipboardEntry(
                id = 42L,
                contentType = ContentType.TEXT,
                textContent = "same body",
                filePath = null,
                previewText = "same body",
                timestamp = 1_000L,
            )
        runBlocking { repo.insertEntry(established) }
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Act: the editor committed text matches the saved text, and a blank draft
        // of it (e.g. the user cleared the field) must not blank out the clip either.
        val unchanged = viewModel.saveDraftOnLifecycleExit(established, "same body", "same body")
        val blanked = viewModel.saveDraftOnLifecycleExit(established, "", "same body")

        // Assert
        assertFalse(unchanged)
        assertFalse(blanked)
        assertEquals("same body", runBlocking { repo.getClip(42L)!!.textContent })
    }

    @Test
    fun lockedExistingClip_neverOverwrittenOnLifecycleExit() {
        // Arrange
        val established =
            ClipboardEntry(
                id = 42L,
                contentType = ContentType.TEXT,
                textContent = "locked body",
                filePath = null,
                previewText = "locked body",
                timestamp = 1_000L,
                isLocked = true,
            )
        runBlocking { repo.insertEntry(established) }
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Act / Assert
        assertFalse(viewModel.saveDraftOnLifecycleExit(established, "edited body", "locked body"))
        assertEquals("locked body", runBlocking { repo.getClip(42L)!!.textContent })
    }

    @Test
    fun explicitSave_createsDraftRowOnceWithoutEmptyArtifacts() {
        // Arrange: the editor's explicit save path goes through updateClip.
        val id = newDraftId()
        val draft = runBlocking { viewModel.getClip(id) }!!
        val toSave =
            draft.copy(
                textContent = "saved via back",
                previewText = "saved via back",
            )

        // Act
        viewModel.updateClip(toSave)
        mainDispatcherRule.scheduler.advanceUntilIdle()
        viewModel.updateClip(toSave.copy(textContent = "saved again"))
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert: one row, updated content, no "(untitled)" artifact.
        val rows = runBlocking { dao.getAllOnce() }
        assertEquals(1, rows.size)
        assertEquals("saved again", rows.single().textContent)
        assertNull(viewModel.recentNewClipId.value)
    }

    @Test
    fun draftSaveWithErasedText_neverInsertsEmptyRow() {
        // Arrange: text typed, then erased again — the explicit save must not
        // produce an empty "(untitled)" row (#42).
        val id = newDraftId()
        val draft = runBlocking { viewModel.getClip(id) }!!

        // Act: the dialog's Save path with the field emptied out.
        viewModel.updateClip(draft)
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        runBlocking { assertNull(repo.getClip(id)) }
        assertTrue(viewModel.clips.value.isEmpty())
        assertNull(viewModel.recentNewClipId.value)
    }

    @Test
    fun backWithBlankDraft_discardsInMemoryDraftWithoutRow() {
        // Arrange
        val id = newDraftId()
        val draft = runBlocking { viewModel.getClip(id) }!!

        // Act: the Back/auto-delete path used by the editor for an empty new clip.
        viewModel.deleteClip(draft)
        viewModel.clearRecentNewClipId()
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        runBlocking { assertNull(repo.getClip(id)) }
        runBlocking { assertNull(viewModel.getClip(id)) }
        assertTrue(viewModel.clips.value.isEmpty())
    }
}
