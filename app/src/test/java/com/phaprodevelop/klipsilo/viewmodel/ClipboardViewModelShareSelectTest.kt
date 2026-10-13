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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ClipboardViewModelShareSelectTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewModelStore = ViewModelStore()
    private lateinit var application: Application
    private lateinit var dao: FakeClipboardItemDao
    private lateinit var repo: ClipboardRepository
    private lateinit var vm: ClipboardViewModel
    private lateinit var textA: ClipboardEntry
    private lateinit var imageB: ClipboardEntry
    private lateinit var textC: ClipboardEntry

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        dao = FakeClipboardItemDao()
        repo = ClipboardRepository(dao, application)
        textA = textEntry(id = 10L, text = "a", timestamp = 1_000L)
        imageB =
            ClipboardEntry(
                id = 11L,
                contentType = ContentType.IMAGE,
                textContent = null,
                filePath = null,
                previewText = "photo",
                timestamp = 2_000L,
            )
        textC = textEntry(id = 12L, text = "b", timestamp = 3_000L)
        runBlocking {
            repo.insertEntry(textA)
            repo.insertEntry(imageB)
            repo.insertEntry(textC)
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
    fun toggleShareSelection_textA_selectsAndSetsType() {
        // Arrange — empty selection from setUp

        // Act
        vm.toggleShareSelection(textA)

        // Assert
        assertEquals(listOf(textA.id), vm.shareSelectedIds.value.toList())
        assertEquals(ContentType.TEXT, vm.shareSelectionType.value)
    }

    @Test
    fun toggleShareSelection_imageWhileTextSelected_unchanged() {
        // Arrange
        vm.toggleShareSelection(textA)
        val idsBefore = vm.shareSelectedIds.value.toList()
        val typeBefore = vm.shareSelectionType.value

        // Act
        vm.toggleShareSelection(imageB)

        // Assert
        assertEquals(idsBefore, vm.shareSelectedIds.value.toList())
        assertEquals(typeBefore, vm.shareSelectionType.value)
        assertEquals(listOf(textA.id), vm.shareSelectedIds.value.toList())
        assertEquals(ContentType.TEXT, vm.shareSelectionType.value)
    }

    @Test
    fun toggleShareSelection_secondText_appendsInInsertionOrder() {
        // Arrange
        vm.toggleShareSelection(textA)

        // Act
        vm.toggleShareSelection(textC)

        // Assert
        assertEquals(listOf(textA.id, textC.id), vm.shareSelectedIds.value.toList())
        assertEquals(ContentType.TEXT, vm.shareSelectionType.value)
    }

    @Test
    fun toggleShareSelection_toggleOffA_keepsCAndTextType() {
        // Arrange
        vm.toggleShareSelection(textA)
        vm.toggleShareSelection(textC)

        // Act
        vm.toggleShareSelection(textA)

        // Assert
        assertEquals(listOf(textC.id), vm.shareSelectedIds.value.toList())
        assertEquals(ContentType.TEXT, vm.shareSelectionType.value)
    }

    @Test
    fun toggleShareSelection_lastText_clearsSelectionAndType() {
        // Arrange
        vm.toggleShareSelection(textA)
        vm.toggleShareSelection(textC)
        vm.toggleShareSelection(textA)

        // Act
        vm.toggleShareSelection(textC)

        // Assert
        assertTrue(vm.shareSelectedIds.value.isEmpty())
        assertNull(vm.shareSelectionType.value)
    }

    @Test
    fun clearShareSelection_resetsIdsAndType() {
        // Arrange
        vm.toggleShareSelection(textA)
        vm.toggleShareSelection(textC)

        // Act
        vm.clearShareSelection()

        // Assert
        assertTrue(vm.shareSelectedIds.value.isEmpty())
        assertNull(vm.shareSelectionType.value)
    }

    @Test
    fun combineToPdf_fewerThanTwo_isNoOp() {
        // Arrange
        assertFalse(vm.isConverting.value)

        // Act
        vm.combineToPdf(emptyList())
        vm.combineToPdf(listOf(imageB))
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        assertFalse(vm.isConverting.value)
        assertNull(vm.toastMessage.value)
        assertEquals(3, storedItems().size)
    }

    @Test
    fun combineTextClips_twoTexts_insertsJoined() {
        // Arrange
        val beforeIds = storedItems().map { it.id }.toSet()

        // Act
        val listRows = vm.clips.value.associateBy { it.id }
        vm.combineTextClips(listOf(listRows.getValue(textA.id), listRows.getValue(textC.id)))
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        val inserted = storedItems().single { it.id !in beforeIds }
        assertEquals(ContentType.TEXT, inserted.contentType)
        assertEquals("a\n\nb", inserted.textContent)
        assertNull(vm.navigateToNewClip.value)
        assertEquals(string(R.string.texts_combined), vm.toastMessage.value)
    }

    @Test
    fun combineTextClips_allBlank_toastsFailed_noInsert() {
        // Arrange
        val blank1 =
            textEntry(id = 100L, text = "", timestamp = 10L).copy(textContent = null)
        val blank2 = textEntry(id = 101L, text = "   ", timestamp = 11L)
        val beforeIds = storedItems().map { it.id }.toSet()

        // Act
        vm.combineTextClips(listOf(blank1, blank2))
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(string(R.string.clip_save_failed), vm.toastMessage.value)
        assertEquals(beforeIds, storedItems().map { it.id }.toSet())
    }

    private fun string(resId: Int): String = LocaleHelper.applyLocale(application).getString(resId)

    private fun storedItems(): List<ClipboardItem> = runBlocking { dao.getAllOnce() }

    private fun textEntry(
        id: Long,
        text: String,
        timestamp: Long,
    ) = ClipboardEntry(
        id = id,
        contentType = ContentType.TEXT,
        textContent = text,
        filePath = null,
        previewText = text,
        timestamp = timestamp,
    )
}
