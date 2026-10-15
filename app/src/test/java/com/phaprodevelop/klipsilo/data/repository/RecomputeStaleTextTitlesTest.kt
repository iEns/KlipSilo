package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ClipboardItem
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class RecomputeStaleTextTitlesTest {
    private val recipe =
        "Citronpasta med hvidløg\n" +
            "=======================\n" +
            "\n" +
            "## Ingredienser\n" +
            "- 200 g spaghetti\n"

    private lateinit var context: Application
    private lateinit var dao: FakeClipboardItemDao
    private lateinit var repo: ClipboardRepository

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
    fun recomputeStaleTextTitles_updatesThePreSetextDerivedTitle() =
        runTest {
            // Arrange: a clip saved by an older build derived its title from the
            // first ## heading and never saw the setext H1 above it.
            dao.insert(
                ClipboardItem(
                    id = 1L,
                    contentType = ContentType.TEXT,
                    textContent = recipe,
                    previewText = "Ingredienser",
                ),
            )

            // Act
            repo.recomputeStaleTextTitles()

            // Assert
            assertEquals("Citronpasta med hvidløg", dao.getById(1L)!!.previewText)
        }

    @Test
    fun recomputeStaleTextTitles_keepsUserRenames() =
        runTest {
            // Arrange: both previews were typed in Rename and are not stored
            // anywhere in the body.
            dao.insert(
                ClipboardItem(
                    id = 2L,
                    contentType = ContentType.TEXT,
                    textContent = recipe,
                    previewText = "Min pasta-opskrift",
                ),
            )
            dao.insert(
                ClipboardItem(
                    id = 3L,
                    contentType = ContentType.TEXT,
                    textContent = "Pasta med pesto\nmozzarella",
                    previewText = "Madplan uge 42",
                ),
            )

            // Act
            repo.recomputeStaleTextTitles()

            // Assert
            assertEquals("Min pasta-opskrift", dao.getById(2L)!!.previewText)
            assertEquals("Madplan uge 42", dao.getById(3L)!!.previewText)
        }

    @Test
    fun recomputeStaleTextTitles_recomputesRawBodyPreview() =
        runTest {
            // Arrange: a clip saved before the heading-title rule stored the raw
            // body, so the list preview could never show the setext title.
            val underline = "=".repeat("Citronpasta med hvidløg".length)
            val rawBody = "Citronpasta med hvidløg\n$underline\n\n## Ingredienser\n- 200 g spaghetti\n"
            dao.insert(
                ClipboardItem(
                    id = 4L,
                    contentType = ContentType.TEXT,
                    textContent = rawBody,
                    previewText = com.phaprodevelop.klipsilo.data.legacyRawTextPreview(rawBody),
                ),
            )

            // Act
            repo.recomputeStaleTextTitles()

            // Assert
            assertEquals("Citronpasta med hvidløg", dao.getById(4L)!!.previewText)
        }

    @Test
    fun recomputeStaleTextTitles_readsTheFilePersistedBody() =
        runTest {
            // Arrange: a large body lives on disk, the row has no inline text.
            val dir = File(context.filesDir, "files").apply { mkdirs() }
            val body = File(dir, "clip1.txt").apply { writeText(recipe) }
            dao.insert(
                ClipboardItem(
                    id = 5L,
                    contentType = ContentType.TEXT,
                    textContent = null,
                    filePath = body.absolutePath,
                    previewText = "Ingredienser",
                ),
            )

            // Act
            repo.recomputeStaleTextTitles()

            // Assert
            assertEquals("Citronpasta med hvidløg", dao.getById(5L)!!.previewText)
        }

    @Test
    fun recomputeStaleTextTitles_skipsNonTextClips() =
        runTest {
            // Arrange
            dao.insert(
                ClipboardItem(
                    id = 6L,
                    contentType = ContentType.URL,
                    textContent = "https://example.com/a",
                    previewText = "https://example.com/a",
                ),
            )

            // Act
            repo.recomputeStaleTextTitles()

            // Assert
            assertEquals("https://example.com/a", dao.getById(6L)!!.previewText)
        }

    @Test
    fun recomputeStaleTextTitles_runsExactlyOnce() =
        runTest {
            // Arrange
            dao.insert(
                ClipboardItem(
                    id = 7L,
                    contentType = ContentType.TEXT,
                    textContent = recipe,
                    previewText = "Ingredienser",
                ),
            )
            repo.recomputeStaleTextTitles()
            assertEquals("Citronpasta med hvidløg", dao.getById(7L)!!.previewText)
            // A clip that becomes stale afterwards must not be rewritten again.
            dao.insert(
                ClipboardItem(
                    id = 8L,
                    contentType = ContentType.TEXT,
                    textContent = recipe,
                    previewText = "Ingredienser",
                ),
            )

            // Act
            repo.recomputeStaleTextTitles()

            // Assert
            assertEquals("Ingredienser", dao.getById(8L)!!.previewText)
        }
}
