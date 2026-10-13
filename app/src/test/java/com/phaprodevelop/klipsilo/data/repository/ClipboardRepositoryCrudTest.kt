package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ClipboardItem
import com.phaprodevelop.klipsilo.data.db.ClipboardItemDao
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ClipboardRepositoryCrudTest {
    private lateinit var dao: FakeClipboardItemDao
    private lateinit var repo: ClipboardRepository
    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dao = FakeClipboardItemDao()
        repo = ClipboardRepository(dao, context)
    }

    @After
    fun tearDown() {
        if (!::context.isInitialized) return
        listOf("images", "pdfs", "files").forEach { name ->
            File(context.filesDir, name).deleteRecursively()
        }
    }

    @Test
    fun insertEntry_emitsInGetAllClips() =
        runTest {
            // Arrange
            val entry =
                textEntry(
                    id = 42L,
                    text = "clip text",
                    isPinned = true,
                    isLocked = true,
                    latitude = 1.5,
                    longitude = 2.5,
                )

            // Act
            repo.insertEntry(entry)
            val clips = repo.getAllClips().first()

            // Assert
            assertEquals(listOf(entry.copy(textContent = null)), clips)
            assertTrue(clips.single().isLocked)
            assertTrue(clips.single().isPinned)
            assertEquals(entry, repo.getClip(entry.id))
        }

    @Test
    fun updateClip_overwritesAllFields() =
        runTest {
            // Arrange
            val original = textEntry(id = 7L, text = "before")
            repo.insertEntry(original)
            val updated =
                ClipboardEntry(
                    id = 7L,
                    contentType = ContentType.URL,
                    textContent = "https://example.com",
                    filePath = "/tmp/clip",
                    previewText = "example",
                    timestamp = 9_000L,
                    isPinned = true,
                    isLocked = true,
                    latitude = 12.3,
                    longitude = 45.6,
                )

            // Act
            repo.updateClip(updated)

            // Assert
            assertEquals(updated.copy(textContent = null), repo.getAllClips().first().single())
            assertEquals(updated, repo.getClip(updated.id))
        }

    @Test
    fun setLocked_trueAndFalse() =
        runTest {
            // Arrange
            val entry = textEntry(id = 3L, isLocked = false)
            repo.insertEntry(entry)

            // Act / Assert
            repo.setLocked(entry, true)
            assertTrue(dao.getAllOnce().single().isLocked)

            repo.setLocked(entry, false)
            assertFalse(dao.getAllOnce().single().isLocked)
        }

    @Test
    fun togglePin_invertsFlag() =
        runTest {
            // Arrange
            val entry = textEntry(id = 4L, isPinned = false)
            repo.insertEntry(entry)

            // Act / Assert
            repo.togglePin(entry)
            assertTrue(dao.getAllOnce().single().isPinned)

            val pinned = repo.getAllClips().first().single()
            repo.togglePin(pinned)
            assertFalse(dao.getAllOnce().single().isPinned)
        }

    @Test
    fun renameClip_text_trimsAndSetsPreview() =
        runTest {
            // Arrange
            val entry = textEntry(id = 5L, text = "body", preview = "old")
            repo.insertEntry(entry)

            // Act
            val renamed = repo.renameClip(entry, "  Hello  ")

            // Assert
            assertTrue(renamed)
            assertEquals("Hello", repo.getAllClips().first().single().previewText)
        }

    @Test
    fun renameClip_text_blank_returnsFalse_andDoesNotUpdate() =
        runTest {
            // Arrange
            val entry = textEntry(id = 6L, text = "body", preview = "keep-me")
            repo.insertEntry(entry)

            // Act
            val renamed = repo.renameClip(entry, "   ")

            // Assert
            assertFalse(renamed)
            assertEquals(entry.copy(textContent = null), repo.getAllClips().first().single())
            assertEquals(entry, repo.getClip(entry.id))
        }

    @Test
    fun renameClip_file_sanitizesAndRenamesOnDisk() =
        runTest {
            // Arrange
            val oldFile = createStoredFile("images", "old.jpg", byteArrayOf(1, 2, 3))
            val entry = imageEntry(oldFile, id = 8L, preview = "old")
            repo.insertEntry(entry)

            // Act
            val renamed = repo.renameClip(entry, "new:name")
            val updated = repo.getAllClips().first().single()
            val newFile = File(updated.filePath!!)

            // Assert
            assertTrue(renamed)
            assertTrue(newFile.exists())
            assertFalse(oldFile.exists())
            assertFalse(newFile.name.contains(':'))
            assertTrue(newFile.name.contains('_'))
            assertEquals("new_name.jpg", newFile.name)
            assertEquals("new_name", updated.previewText)
            assertEquals(newFile.absolutePath, updated.filePath)
            assertEquals(File(context.filesDir, "images").canonicalPath, newFile.parentFile!!.canonicalPath)
        }

    @Test
    fun renameClip_file_blankSanitize_returnsFalse() =
        runTest {
            // Arrange
            val file = createStoredFile("images", "keep.jpg")
            val entry = imageEntry(file, id = 9L, preview = "keep")
            repo.insertEntry(entry)

            // Act
            val renamed = repo.renameClip(entry, "   ")

            // Assert
            assertFalse(renamed)
            assertTrue(file.exists())
            val stored = repo.getAllClips().first().single()
            assertEquals(file.absolutePath, stored.filePath)
            assertEquals("keep", stored.previewText)
        }

    @Test
    fun duplicateEntry_text_newIdUnpinnedUnlocked_sameText() =
        runTest {
            // Arrange
            val source =
                textEntry(
                    id = 11L,
                    text = "same body",
                    isPinned = true,
                    isLocked = true,
                )
            repo.insertEntry(source)

            // Act
            val newId = repo.duplicateEntry(source)
            val clips = repo.getAllClips().first()
            val duplicate = clips.single { it.id == newId }

            // Assert
            assertNotEquals(0L, newId)
            assertNotEquals(source.id, newId)
            assertEquals(null, duplicate.textContent)
            assertEquals("same body", repo.getClip(newId)?.textContent)
            assertFalse(duplicate.isPinned)
            assertFalse(duplicate.isLocked)
            val original = clips.single { it.id == source.id }
            assertTrue(original.isPinned)
            assertTrue(original.isLocked)
        }

    @Test
    fun duplicateEntry_image_copiesFileToImagesDir() =
        runTest {
            // Arrange
            val sourceBytes = "abc".toByteArray()
            val sourceFile = createStoredFile("images", "photo.jpg", sourceBytes)
            val source = imageEntry(sourceFile, id = 12L, preview = "photo")
            repo.insertEntry(source)

            // Act
            val newId = repo.duplicateEntry(source)
            val duplicate = repo.getAllClips().first().single { it.id == newId }
            val destFile = File(duplicate.filePath!!)

            // Assert
            assertNotEquals(0L, newId)
            assertTrue(destFile.exists())
            assertNotEquals(sourceFile.absolutePath, destFile.absolutePath)
            assertEquals(
                File(context.filesDir, "images").canonicalPath,
                destFile.parentFile!!.canonicalPath,
            )
            assertArrayEquals(sourceBytes, destFile.readBytes())

            val missing =
                source.copy(
                    id = 13L,
                    filePath = File(context.filesDir, "images/missing.jpg").absolutePath,
                )
            assertEquals(0L, repo.duplicateEntry(missing))
            assertEquals(2, repo.getAllClips().first().size)
        }

    @Test
    fun deleteClip_removesDbAndFile() =
        runTest {
            // Arrange
            val file = createStoredFile("images", "gone.jpg")
            val entry = imageEntry(file, id = 14L)
            repo.insertEntry(entry)

            // Act
            repo.deleteClip(entry)

            // Assert
            assertTrue(dao.getAllOnce().isEmpty())
            assertFalse(file.exists())
        }

    @Test
    fun deleteClipLazy_removesDbKeepsFile() =
        runTest {
            // Arrange
            val file = createStoredFile("images", "lazy.jpg")
            val entry = imageEntry(file, id = 15L)
            repo.insertEntry(entry)

            // Act
            repo.deleteClipLazy(entry)

            // Assert
            assertTrue(dao.getAllOnce().isEmpty())
            assertTrue(file.exists())
        }

    @Test
    fun deleteClipFile_deletesFileOnly() =
        runTest {
            // Arrange
            val file = createStoredFile("images", "file-only.jpg")
            val entry = imageEntry(file, id = 16L)
            repo.insertEntry(entry)

            // Act
            repo.deleteClipFile(entry)

            // Assert
            assertFalse(file.exists())
            assertEquals(listOf(entry), repo.getAllClips().first())
        }

    @Test
    fun undeleteClip_reinsertsSameId() =
        runTest {
            // Arrange
            val entry =
                textEntry(
                    id = 17L,
                    text = "restored",
                    isPinned = true,
                    isLocked = true,
                    timestamp = 55L,
                )
            repo.insertEntry(entry)
            repo.deleteClipLazy(entry)
            assertTrue(dao.getAllOnce().isEmpty())

            // Act
            repo.undeleteClip(entry)

            // Assert
            assertEquals(listOf(entry.copy(textContent = null)), repo.getAllClips().first())
            assertEquals(entry, repo.getClip(entry.id))
            assertEquals(entry.id, repo.getAllClips().first().single().id)
        }

    @Test
    fun deleteAllClips_clearsDaoAndRecreatesEmptyDirs() =
        runTest {
            // Arrange
            repo.insertEntry(textEntry(id = 18L))
            val leftover = createStoredFile("images", "junk.jpg")
            assertTrue(leftover.exists())

            // Act
            repo.deleteAllClips()

            // Assert
            assertTrue(dao.getAllOnce().isEmpty())
            val imagesDir = File(context.filesDir, "images")
            assertTrue(imagesDir.isDirectory)
            assertTrue(imagesDir.listFiles().orEmpty().isEmpty())
        }

    @Test
    fun deleteAllClips_skipsLockedRowsAndFiles() =
        runTest {
            // Arrange
            val lockedFile = createStoredFile("images", "locked.jpg")
            val unlockedFile = createStoredFile("images", "free.jpg")
            repo.insertEntry(imageEntry(lockedFile, id = 30L).copy(isLocked = true))
            repo.insertEntry(imageEntry(unlockedFile, id = 31L))

            // Act
            repo.deleteAllClips()
            val remaining = dao.getAllOnce()

            // Assert
            assertEquals(1, remaining.size)
            assertEquals(30L, remaining.single().id)
            assertTrue(remaining.single().isLocked)
            assertTrue(lockedFile.exists())
            assertFalse(unlockedFile.exists())
        }

    @Test
    fun migrateLegacyNames_missingFile_keepsRowIncludingLocked() =
        runTest {
            // Arrange
            val survivor = textEntry(id = 19L, text = "keep")
            val missingPath = File(context.filesDir, "images/ghost.jpg").absolutePath
            val orphan =
                ClipboardEntry(
                    id = 20L,
                    contentType = ContentType.IMAGE,
                    filePath = missingPath,
                    previewText = "ghost",
                    timestamp = 1L,
                )
            val lockedMissing =
                ClipboardEntry(
                    id = 22L,
                    contentType = ContentType.IMAGE,
                    filePath = File(context.filesDir, "images/locked-ghost.jpg").absolutePath,
                    previewText = "locked-ghost",
                    timestamp = 2L,
                    isLocked = true,
                )
            repo.insertEntry(survivor)
            repo.insertEntry(orphan)
            repo.insertEntry(lockedMissing)
            assertFalse(File(missingPath).exists())
            assertFalse(File(lockedMissing.filePath!!).exists())

            // Act
            repo.migrateLegacyNames()

            // Assert
            val ids = repo.getAllClips().first().map { it.id }.toSet()
            assertEquals(setOf(19L, 20L, 22L), ids)
            assertEquals(survivor, repo.getClip(survivor.id))
            assertTrue(dao.getById(22L)!!.isLocked)
        }

    @Test
    fun renameClip_updateFailure_renamesFileBack() =
        runTest {
            // Arrange
            val oldFile = createStoredFile("images", "old.jpg", byteArrayOf(1, 2, 3))
            val entry = imageEntry(oldFile, id = 80L, preview = "old")
            repo.insertEntry(entry)
            val failingRepo = ClipboardRepository(ThrowingUpdateDao(dao), context)

            // Act
            val renamed = failingRepo.renameClip(entry, "new_name")

            // Assert
            assertFalse(renamed)
            assertTrue(oldFile.exists())
            assertEquals(oldFile.absolutePath, dao.getAllOnce().single().filePath)
            assertFalse(File(oldFile.parentFile, "new_name.jpg").exists())
        }

    @Test
    fun migrateLegacyNames_stripsTrailingUnderscoreDigits() =
        runTest {
            // Arrange
            val legacy = createStoredFile("images", "photo_1700000000.jpg")
            val entry = imageEntry(legacy, id = 21L, preview = "photo_1700000000")
            repo.insertEntry(entry)

            // Act
            repo.migrateLegacyNames()
            val updated = repo.getAllClips().first().single()
            val renamed = File(updated.filePath!!)

            // Assert
            assertEquals("photo", updated.previewText)
            assertEquals("photo.jpg", renamed.name)
            assertTrue(renamed.exists())
            assertFalse(legacy.exists())
        }

    private fun textEntry(
        id: Long,
        text: String = "hello",
        preview: String = text,
        timestamp: Long = 1_700_000_000_000L,
        isPinned: Boolean = false,
        isLocked: Boolean = false,
        latitude: Double? = null,
        longitude: Double? = null,
    ) = ClipboardEntry(
        id = id,
        contentType = ContentType.TEXT,
        textContent = text,
        filePath = null,
        previewText = preview,
        timestamp = timestamp,
        isPinned = isPinned,
        isLocked = isLocked,
        latitude = latitude,
        longitude = longitude,
    )

    private fun imageEntry(
        file: File,
        id: Long,
        preview: String = file.nameWithoutExtension,
        timestamp: Long = 1_700_000_000_000L,
        isPinned: Boolean = false,
        isLocked: Boolean = false,
    ) = ClipboardEntry(
        id = id,
        contentType = ContentType.IMAGE,
        textContent = null,
        filePath = file.absolutePath,
        previewText = preview,
        timestamp = timestamp,
        isPinned = isPinned,
        isLocked = isLocked,
    )

    private fun createStoredFile(
        subdir: String,
        name: String,
        content: ByteArray = "abc".toByteArray(),
    ): File {
        val dir = File(context.filesDir, subdir).apply { mkdirs() }
        return File(dir, name).also { it.writeBytes(content) }
    }

    private class ThrowingUpdateDao(
        private val inner: ClipboardItemDao,
    ) : ClipboardItemDao by inner {
        override suspend fun update(item: ClipboardItem) {
            throw IllegalStateException("update failed")
        }

        override suspend fun updatePreviewText(
            id: Long,
            previewText: String,
        ) {
            throw IllegalStateException("update failed")
        }

        override suspend fun updateFilePathAndPreviewText(
            id: Long,
            filePath: String,
            previewText: String,
        ) {
            throw IllegalStateException("update failed")
        }
    }
}
