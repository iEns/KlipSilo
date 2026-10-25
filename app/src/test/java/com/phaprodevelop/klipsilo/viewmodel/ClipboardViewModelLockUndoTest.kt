@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.LocaleHelper
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.db.ClipboardItem
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ClipboardViewModelLockUndoTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewModelStore = ViewModelStore()
    private lateinit var application: Application
    private lateinit var dao: FakeClipboardItemDao
    private lateinit var repo: ClipboardRepository
    private lateinit var vm: ClipboardViewModel
    private lateinit var lockedText: ClipboardEntry
    private lateinit var unlockedText: ClipboardEntry

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        dao = FakeClipboardItemDao()
        repo = ClipboardRepository(dao, application)
        lockedText =
            textEntry(
                id = 1L,
                text = "locked-body",
                isLocked = true,
                timestamp = 1_000L,
            )
        unlockedText =
            textEntry(
                id = 2L,
                text = "unlocked-body",
                isLocked = false,
                timestamp = 2_000L,
            )
        runBlocking {
            repo.insertEntry(lockedText)
            repo.insertEntry(unlockedText)
        }
        val dispatcher = mainDispatcherRule.dispatcher
        vm =
            ClipboardViewModel(
                application,
                repo,
                undoTimeoutMs = 4_000L,
                ioDispatcher = dispatcher,
            )
        viewModelStore.put("vm", vm)
        mainDispatcherRule.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        if (!::application.isInitialized) return
        listOf("images", "pdfs", "files").forEach { name ->
            File(application.filesDir, name).deleteRecursively()
        }
    }

    @Test
    fun updateClip_locked_setsLockedToast_doesNotCallUpdate() {
        // Arrange
        val before = item(1L)
        val mutated =
            lockedText.copy(
                textContent = "mutated",
                previewText = "mutated",
                timestamp = 99L,
            )

        // Act
        vm.updateClip(mutated)

        // Assert
        assertEquals(string(R.string.clip_is_locked), vm.toastMessage.value)
        assertEquals(before, item(1L))
    }

    @Test
    fun updateClip_unlocked_updatesAndToastsSaved() {
        // Arrange
        val updated =
            unlockedText.copy(
                textContent = "saved-body",
                previewText = "saved-body",
            )

        // Act
        vm.updateClip(updated)
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(string(R.string.clip_saved), vm.toastMessage.value)
        assertEquals("saved-body", item(2L).textContent)
        assertEquals("saved-body", item(2L).previewText)
    }

    @Test
    fun renameClip_locked_toastsLocked() {
        // Arrange
        val originalPreview = item(1L).previewText

        // Act
        vm.renameClip(lockedText, "new-name")

        // Assert
        assertEquals(string(R.string.clip_is_locked), vm.toastMessage.value)
        assertEquals(originalPreview, item(1L).previewText)
    }

    @Test
    fun deleteClip_locked_toastsLocked_noDelete() {
        // Arrange
        val beforeIds = items().map { it.id }.toSet()

        // Act
        vm.deleteClip(lockedText)

        // Assert
        assertEquals(string(R.string.clip_is_locked), vm.toastMessage.value)
        assertEquals(beforeIds, items().map { it.id }.toSet())
        assertTrue(items().any { it.id == 1L })
    }

    @Test
    fun deleteClipWithUndo_locked_noPending() {
        // Arrange
        val before = items().associateBy { it.id }

        // Act
        vm.deleteClipWithUndo(lockedText)
        mainDispatcherRule.scheduler.advanceTimeBy(5_000)
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(string(R.string.clip_is_locked), vm.toastMessage.value)
        assertEquals(before, items().associateBy { it.id })
        assertFalse(vm.undoDelete())
    }

    @Test
    fun deleteClipWithUndo_unlocked_lazyDeletesImmediately_fileDeletedAfter4000ms() {
        // Arrange
        val file = createStoredFile("images", "photo.jpg")
        val image = imageEntry(file, id = 3L)
        runBlocking { repo.insertEntry(image) }
        mainDispatcherRule.scheduler.advanceUntilIdle()
        val entry = vm.clips.value.single { it.id == 3L }

        // Act
        vm.deleteClipWithUndo(entry)
        // Lazy-delete is due now; the 4000ms file cleanup is not.
        mainDispatcherRule.scheduler.runCurrent()

        // Assert
        assertTrue(items().none { it.id == 3L })
        assertTrue(file.exists())

        mainDispatcherRule.scheduler.advanceTimeBy(3_999)
        mainDispatcherRule.scheduler.runCurrent()
        assertTrue(file.exists())

        mainDispatcherRule.scheduler.advanceTimeBy(1)
        mainDispatcherRule.scheduler.advanceUntilIdle()
        assertFalse(file.exists())
        assertTrue(items().none { it.id == 3L })
    }

    @Test
    fun undoDelete_textClip_restoresBodyFromListRow() {
        // Arrange — list rows omit textContent
        val listRow = vm.clips.value.single { it.id == 2L }
        assertEquals(null, listRow.textContent)
        assertEquals("unlocked-body", item(2L).textContent)

        // Act
        vm.deleteClipWithUndo(listRow)
        mainDispatcherRule.scheduler.runCurrent()
        assertTrue(items().none { it.id == 2L })
        val undone = vm.undoDelete()
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        assertTrue(undone)
        assertEquals("unlocked-body", item(2L).textContent)
        assertEquals("unlocked-body", item(2L).previewText)
    }

    @Test
    fun undoDelete_withinWindow_restoresRowAndKeepsFile() {
        // Arrange
        val file = createStoredFile("images", "keep.jpg")
        val image = imageEntry(file, id = 3L)
        runBlocking { repo.insertEntry(image) }
        mainDispatcherRule.scheduler.advanceUntilIdle()
        val entry = vm.clips.value.single { it.id == 3L }

        // Act
        vm.deleteClipWithUndo(entry)
        mainDispatcherRule.scheduler.runCurrent()
        val undone = vm.undoDelete()
        mainDispatcherRule.scheduler.advanceTimeBy(5_000)
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        assertTrue(undone)
        assertTrue(file.exists())
        assertNotNull(items().singleOrNull { it.id == 3L })
        assertEquals(entry.filePath, item(3L).filePath)
    }

    @Test
    fun undoDelete_empty_returnsFalse() {
        // Arrange — no pending deletes

        // Act
        val result = vm.undoDelete()

        // Assert
        assertFalse(result)
        assertEquals(setOf(1L, 2L), items().map { it.id }.toSet())
    }

    @Test
    fun deleteClipsByType_skipsLocked_returnsCountOfDeletable() {
        // Arrange
        assertTrue(vm.clips.value.any { it.id == 1L && it.isLocked })
        assertTrue(vm.clips.value.any { it.id == 2L && !it.isLocked })

        // Act
        val deleted = vm.deleteClipsByType(ContentType.TEXT)
        mainDispatcherRule.scheduler.runCurrent()

        // Assert
        assertEquals(1, deleted)
        assertTrue(items().any { it.id == 1L && it.isLocked })
        assertTrue(items().none { it.id == 2L })
    }

    @Test
    fun deleteAllClipsWithUndo_skipsLocked() {
        // Arrange
        val lockedId = 1L

        // Act
        val deleted = vm.deleteAllClipsWithUndo()
        mainDispatcherRule.scheduler.runCurrent()

        // Assert
        assertEquals(1, deleted)
        assertEquals(listOf(lockedId), items().map { it.id })
        assertTrue(items().single().isLocked)
    }

    @Test
    fun deleteAllClips_currentlyDeletesLockedToo() {
        // Arrange
        assertTrue(items().any { it.id == 1L && it.isLocked })

        // Act
        vm.deleteAllClips()
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert — locked clips survive deleteAllClips
        assertTrue(items().any { it.id == 1L && it.isLocked })
        assertTrue(items().none { it.id == 2L })
        assertEquals(1, vm.clips.value.size)
        assertTrue(vm.clips.value.single().isLocked)
        assertEquals(string(R.string.all_deleted), vm.toastMessage.value)
    }

    @Test
    fun unlockAllClips_returnsLockedCount_andClearsFlags() {
        // Arrange
        assertEquals(1, vm.clips.value.count { it.isLocked })

        // Act
        val unlocked = vm.unlockAllClips()
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(1, unlocked)
        assertTrue(items().none { it.isLocked })
        assertTrue(vm.clips.value.none { it.isLocked })
    }

    @Test
    fun setTextClipKind_locked_doesNotChangeTextOrKind() {
        // Arrange
        val original = "locked-body"
        val locked = vm.clips.value.single { it.id == 1L }
        assertTrue(locked.isLocked)
        assertEquals(original, item(1L).textContent)

        // Act
        vm.setTextClipKind(locked, ContentType.MARKDOWN, "should-not-overwrite")
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(original, item(1L).textContent)
        assertEquals(ContentType.TEXT, item(1L).contentType)
        assertEquals(string(R.string.clip_is_locked), vm.toastMessage.value)
    }

    private fun string(resId: Int): String = LocaleHelper.applyLocale(application).getString(resId)

    private fun items(): List<ClipboardItem> = runBlocking { dao.getAllOnce() }

    private fun item(id: Long): ClipboardItem = items().single { it.id == id }

    private fun textEntry(
        id: Long,
        text: String,
        isLocked: Boolean,
        timestamp: Long,
    ) = ClipboardEntry(
        id = id,
        contentType = ContentType.TEXT,
        textContent = text,
        filePath = null,
        previewText = text,
        timestamp = timestamp,
        isLocked = isLocked,
    )

    private fun imageEntry(
        file: File,
        id: Long,
    ) = ClipboardEntry(
        id = id,
        contentType = ContentType.IMAGE,
        textContent = null,
        filePath = file.absolutePath,
        previewText = file.nameWithoutExtension,
        timestamp = 3_000L,
        isLocked = false,
    )

    private fun createStoredFile(
        subdir: String,
        name: String,
        content: ByteArray = byteArrayOf(1, 2, 3, 4),
    ): File {
        val dir = File(application.filesDir, subdir).apply { mkdirs() }
        return File(dir, name).also { it.writeBytes(content) }
    }
}
