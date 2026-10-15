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
 * Leaving the editor through the lifecycle with a new, still-empty draft must never
 * leave an empty "(untitled)" clip in the list (#39).
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

    @Test
    fun newEmptyDraft_discardedOnLifecycleExit() {
        // Arrange
        viewModel.createEmptyClip()
        mainDispatcherRule.scheduler.advanceUntilIdle()
        val id = viewModel.navigateToNewClip.value ?: error("new clip id missing")
        val draft = runBlocking { repo.getClip(id) }
        assertNotNull(draft)

        // Act
        val discarded = viewModel.discardEmptyNewClipOnLifecycleExit(draft!!, "")
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        assertTrue(discarded)
        assertNull(viewModel.recentNewClipId.value)
        assertTrue(viewModel.navigateBack.value)
        runBlocking { assertNull(repo.getClip(id)) }
    }

    @Test
    fun whitespaceOnlyNewDraft_discardedOnLifecycleExit() {
        // Arrange
        viewModel.createEmptyClip()
        mainDispatcherRule.scheduler.advanceUntilIdle()
        val id = viewModel.navigateToNewClip.value ?: error("new clip id missing")
        val draft = runBlocking { repo.getClip(id) }
        assertNotNull(draft)

        // Act
        val discarded = viewModel.discardEmptyNewClipOnLifecycleExit(draft!!, "   ")
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        assertTrue(discarded)
        runBlocking { assertNull(repo.getClip(id)) }
    }

    @Test
    fun nonEmptyDraft_keptOnLifecycleExit() {
        // Arrange
        viewModel.createEmptyClip()
        mainDispatcherRule.scheduler.advanceUntilIdle()
        val id = viewModel.navigateToNewClip.value ?: error("new clip id missing")
        val draft = runBlocking { repo.getClip(id) }
        assertNotNull(draft)

        // Act
        val discarded = viewModel.discardEmptyNewClipOnLifecycleExit(draft!!, "hello")
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        assertFalse(discarded)
        runBlocking { assertNotNull(repo.getClip(id)) }
    }

    @Test
    fun establishedClip_neverDiscardedOnLifecycleExit() {
        // Arrange
        val established =
            ClipboardEntry(
                id = 42L,
                contentType = ContentType.TEXT,
                textContent = null,
                filePath = null,
                previewText = "Established clip",
                timestamp = 1_000L,
            )
        runBlocking { repo.insertEntry(established) }
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Act
        val discarded =
            viewModel.discardEmptyNewClipOnLifecycleExit(established, "")
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        assertFalse(discarded)
        runBlocking { assertNotNull(repo.getClip(42L)) }
    }
}
