@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.viewmodel

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ClipboardItemListRow
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.repository.ClipboardRepository
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.phaprodevelop.klipsilo.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ClipListRefreshTest {
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

    @After
    fun tearDown() {
        if (!::app.isInitialized) return
        listOf("images", "pdfs", "files").forEach { name ->
            File(app.filesDir, name).deleteRecursively()
        }
    }

    @Test
    fun saveSharedContent_updatesClipsWhenRoomFlowDoesNotEmit() {
        dao.emitFlowUpdates = false

        viewModel.saveSharedContent(Intent().putExtra(Intent.EXTRA_TEXT, "fresh share"))
        mainDispatcherRule.scheduler.advanceUntilIdle()

        val clips = viewModel.clips.value
        assertEquals(1, clips.size)
        assertEquals("fresh share", clips.single().previewText)
    }

    @Test
    fun delete_removesClipFromListWhenRoomFlowDoesNotEmit() {
        val entry = textEntry()
        runBlocking { repo.insertEntry(entry) }
        mainDispatcherRule.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.clips.value.size)
        dao.emitFlowUpdates = false

        viewModel.deleteClipWithUndo(viewModel.clips.value.single())
        mainDispatcherRule.scheduler.runCurrent()

        assertTrue(viewModel.clips.value.none { it.id == entry.id })
    }

    @Test
    fun staleRoomEmission_doesNotRestoreDeletedClip() {
        val entry = textEntry()
        runBlocking { repo.insertEntry(entry) }
        mainDispatcherRule.scheduler.advanceUntilIdle()
        val stale =
            listOf(
                ClipboardItemListRow(
                    id = entry.id,
                    contentType = ContentType.TEXT,
                    previewText = entry.previewText,
                    timestamp = entry.timestamp,
                ),
            )
        dao.emitFlowUpdates = false
        viewModel.deleteClipWithUndo(viewModel.clips.value.single())
        mainDispatcherRule.scheduler.runCurrent()

        dao.emitRows(stale)
        mainDispatcherRule.scheduler.runCurrent()

        assertTrue(viewModel.clips.value.none { it.id == entry.id })
        assertTrue(runBlocking { dao.getAllOnce() }.none { it.id == entry.id })
    }

    private fun textEntry(): ClipboardEntry =
        ClipboardEntry(
            id = 42L,
            contentType = ContentType.TEXT,
            textContent = "body",
            previewText = "body",
            timestamp = 1_000L,
        )
}
