package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class EnsureUniqueFileTest {
    private lateinit var repo: ClipboardRepository
    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repo = ClipboardRepository(FakeClipboardItemDao(), context)
    }

    @After
    fun tearDown() {
        if (!::context.isInitialized) return
        File(context.filesDir, "files").deleteRecursively()
    }

    @Test
    fun ensureUniqueFile_createsExclusivePath() {
        // Arrange
        val dir = File(context.filesDir, "files").apply { mkdirs() }
        val desired = File(dir, "clip.txt")

        // Act
        val first = repo.ensureUniqueFile(desired)
        val second = repo.ensureUniqueFile(desired)

        // Assert
        assertNotNull(first)
        assertNotNull(second)
        assertTrue(first!!.exists())
        assertTrue(second!!.exists())
        assertNotEquals(first.absolutePath, second.absolutePath)
        assertEquals(desired.absolutePath, first.absolutePath)
        assertTrue(second.name.startsWith("clip_"))
    }

    @Test
    fun ensureUniqueFile_neverReturnsExistingPath() {
        // Arrange
        val dir = File(context.filesDir, "files").apply { mkdirs() }
        val existing = File(dir, "taken.txt").also { it.writeText("keep") }

        // Act
        val created = repo.ensureUniqueFile(existing)

        // Assert
        assertNotNull(created)
        assertTrue(created!!.exists())
        assertNotEquals(existing.absolutePath, created.absolutePath)
        assertEquals("keep", existing.readText())
    }

    @Test
    fun ensureUniqueFile_exhaustedUniquifier_returnsNull() {
        // Arrange
        val dir = File(context.filesDir, "files").apply { mkdirs() }
        File(dir, "clip.txt").createNewFile()
        for (i in 1..MAX_FILE_UNIQUIFIER) {
            File(dir, "clip_$i.txt").createNewFile()
        }

        // Act
        val result = repo.ensureUniqueFile(File(dir, "clip.txt"))

        // Assert
        assertNull(result)
        assertFalse(File(dir, "clip_${MAX_FILE_UNIQUIFIER + 1}.txt").exists())
    }
}
