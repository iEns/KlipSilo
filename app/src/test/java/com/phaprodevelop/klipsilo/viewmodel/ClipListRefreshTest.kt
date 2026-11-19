@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import org.junit.Assert.assertFalse
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

    @Test
    fun pin_updatesClipsWhenRoomFlowDoesNotEmit() {
        val entry = textEntry()
        runBlocking { repo.insertEntry(entry) }
        mainDispatcherRule.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.clips.value.size)
        dao.emitFlowUpdates = false

        viewModel.togglePin(viewModel.clips.value.single())
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // #184: Pin writes must not rely on the (possibly missing) Room flow
        // emission, and the moved row must be surfaced to the UI.
        assertTrue(viewModel.clips.value.single().isPinned)
        assertEquals(entry.id, viewModel.scrollToClipId.value)
    }

    @Test
    fun unpin_updatesClipsWhenRoomFlowDoesNotEmit() {
        val entry = textEntry().copy(isPinned = true)
        runBlocking { repo.insertEntry(entry) }
        mainDispatcherRule.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.clips.value.size)
        dao.emitFlowUpdates = false

        viewModel.togglePin(viewModel.clips.value.single())
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // #184: Unpin refreshes the list the same way.
        assertFalse(viewModel.clips.value.single().isPinned)
        assertEquals(entry.id, viewModel.scrollToClipId.value)
    }

    @Test
    fun lock_updatesClipsWhenRoomFlowDoesNotEmit() {
        val entry = textEntry()
        runBlocking { repo.insertEntry(entry) }
        mainDispatcherRule.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.clips.value.size)
        dao.emitFlowUpdates = false

        viewModel.toggleLock(viewModel.clips.value.single())
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // #184: Lock/Unlock has the same refresh gap as Pin.
        assertTrue(viewModel.clips.value.single().isLocked)
    }

    @Test
    fun makeMarkdown_updatesClipsWithoutRoomEmission() {
        val entry = textEntry()
        runBlocking { repo.insertEntry(entry) }
        mainDispatcherRule.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.clips.value.size)
        dao.emitFlowUpdates = false

        viewModel.setTextClipKind(viewModel.clips.value.single(), ContentType.MARKDOWN)
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // #184: converting the kind must not depend on the Room flow, and the
        // converted row stays surfaced instead of scrolling off to the top.
        assertEquals(ContentType.MARKDOWN, viewModel.clips.value.single().contentType)
        assertEquals(entry.id, viewModel.scrollToClipId.value)
    }

    @Test
    fun duplicateClip_revealsNewClipWhenRoomFlowDoesNotEmit() {
        val entry = textEntry()
        runBlocking { repo.insertEntry(entry) }
        mainDispatcherRule.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.clips.value.size)
        dao.emitFlowUpdates = false

        viewModel.duplicateClip(viewModel.clips.value.single())
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // #184: the duplicate is a new row and must be surfaced by its own id,
        // not by scrolling the old list to the top.
        val clips = viewModel.clips.value
        assertEquals(2, clips.size)
        val duplicate = clips.single { it.id != entry.id }
        assertTrue(clips.any { it.id == entry.id })
        assertEquals(duplicate.id, viewModel.scrollToClipId.value)
        // #188: the copy gets its own display name.
        assertEquals(
            entry.previewText + app.getString(com.phaprodevelop.klipsilo.R.string.clip_copy_suffix),
            duplicate.previewText,
        )
    }

    @Test
    fun clearScrollToClipId_resetsTheScrollEvent() {
        val entry = textEntry()
        runBlocking { repo.insertEntry(entry) }
        mainDispatcherRule.scheduler.advanceUntilIdle()

        viewModel.togglePin(viewModel.clips.value.single())
        mainDispatcherRule.scheduler.advanceUntilIdle()
        assertEquals(entry.id, viewModel.scrollToClipId.value)

        viewModel.clearScrollToClipId()
        assertEquals(0L, viewModel.scrollToClipId.value)
    }

    @Test
    fun refreshClipboardHasContent_tracksTextAndUriButNotEmptyText() {
        // #93: the Save clipboard button is shown only for pasteable content.
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        clipboard.setPrimaryClip(ClipData.newPlainText("text", "hello"))
        viewModel.refreshClipboardHasContent()
        assertTrue(viewModel.clipboardHasContent.value)

        clipboard.setPrimaryClip(ClipData.newPlainText("empty", ""))
        viewModel.refreshClipboardHasContent()
        assertFalse(viewModel.clipboardHasContent.value)

        clipboard.setPrimaryClip(
            ClipData.newUri(app.contentResolver, "file", Uri.parse("content://example.test/file.txt")),
        )
        viewModel.refreshClipboardHasContent()
        assertTrue(viewModel.clipboardHasContent.value)
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
