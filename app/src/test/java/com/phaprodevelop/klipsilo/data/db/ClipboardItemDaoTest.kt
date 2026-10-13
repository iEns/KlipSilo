package com.phaprodevelop.klipsilo.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClipboardItemDaoTest {
    private lateinit var db: SnipDatabase
    private lateinit var dao: ClipboardItemDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = SnipDatabase.createInMemory(context)
        dao = db.clipboardItemDao()
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) {
            db.close()
        }
    }

    @Test
    fun insert_thenGetAllOnce_returnsItem() =
        runTest {
            // Arrange
            val item =
                ClipboardItem(
                    id = 1L,
                    contentType = ContentType.TEXT,
                    textContent = "hello",
                    filePath = null,
                    previewText = "hello",
                    timestamp = 1_700_000_000_000L,
                    isPinned = false,
                    isLocked = false,
                    latitude = null,
                    longitude = null,
                )

            // Act
            dao.insert(item)
            val rows = dao.getAllOnce()

            // Assert
            assertEquals(1, rows.size)
            val loaded = rows.single()
            assertEquals(1L, loaded.id)
            assertEquals(ContentType.TEXT, loaded.contentType)
            assertEquals("hello", loaded.textContent)
            assertNull(loaded.filePath)
            assertEquals("hello", loaded.previewText)
            assertEquals(1_700_000_000_000L, loaded.timestamp)
            assertFalse(loaded.isPinned)
            assertFalse(loaded.isLocked)
            assertNull(loaded.latitude)
            assertNull(loaded.longitude)
        }

    @Test
    fun getAll_ordersPinnedFirstThenNewest() =
        runTest {
            // Arrange
            dao.insert(item(id = 1L, isPinned = false, timestamp = 100L))
            dao.insert(item(id = 2L, isPinned = true, timestamp = 50L))
            dao.insert(item(id = 3L, isPinned = false, timestamp = 200L))

            // Act
            val ids = dao.getAll().first().map { it.id }

            // Assert
            assertEquals(listOf(2L, 3L, 1L), ids)
        }

    @Test
    fun getAll_emitsLocationInsertToActiveCollector() =
        runTest {
            dao.getAll().test {
                assertTrue(awaitItem().isEmpty())

                dao.insert(
                    ClipboardItem(
                        id = 99L,
                        contentType = ContentType.LOCATION,
                        previewText = "55.676098, 12.568337",
                        timestamp = 1L,
                        latitude = 55.676098,
                        longitude = 12.568337,
                    ),
                )

                val row = awaitItem().single()
                assertEquals(99L, row.id)
                assertEquals(ContentType.LOCATION, row.contentType)
                assertEquals(55.676098, row.latitude)
                assertEquals(12.568337, row.longitude)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun getAll_omitsTextContent_getByIdReturnsBody() =
        runTest {
            // Arrange
            dao.insert(item(id = 1L, previewText = "hello"))

            // Act
            val listRow = dao.getAll().first().single()
            val full = dao.getById(1L)
            val once = dao.getAllOnce().single()

            // Assert
            assertEquals(1L, listRow.id)
            assertEquals("hello", listRow.previewText)
            assertEquals("text-1", full?.textContent)
            assertEquals("text-1", once.textContent)
        }

    @Test
    fun updatePinned_trueThenFalse() =
        runTest {
            // Arrange
            dao.insert(item(id = 1L, isPinned = false))

            // Act
            dao.updatePinned(1L, true)
            val pinned = dao.getAllOnce().single()
            dao.updatePinned(1L, false)
            val unpinned = dao.getAllOnce().single()

            // Assert
            assertTrue(pinned.isPinned)
            assertFalse(unpinned.isPinned)
        }

    @Test
    fun updateLocked_true() =
        runTest {
            // Arrange
            dao.insert(item(id = 1L, isLocked = false))

            // Act
            dao.updateLocked(1L, true)
            val locked = dao.getAllOnce().single()

            // Assert
            assertTrue(locked.isLocked)
        }

    @Test
    fun deleteById_removesOnlyThatRow() =
        runTest {
            // Arrange
            dao.insert(item(id = 1L, previewText = "keep-me-not"))
            dao.insert(item(id = 2L, previewText = "keep-me"))

            // Act
            dao.deleteById(1L)
            val remaining = dao.getAllOnce()

            // Assert
            assertEquals(1, remaining.size)
            assertEquals(2L, remaining.single().id)
            assertEquals("keep-me", remaining.single().previewText)
        }

    @Test
    fun deleteAll_emptiesTable() =
        runTest {
            // Arrange
            dao.insertAll(
                listOf(
                    item(id = 1L),
                    item(id = 2L),
                ),
            )

            // Act
            dao.deleteAll()

            // Assert
            assertTrue(dao.getAllOnce().isEmpty())
            assertTrue(dao.getAll().first().isEmpty())
        }

    @Test
    fun insertAll_insertsMany() =
        runTest {
            // Arrange
            val items =
                listOf(
                    item(id = 10L, previewText = "a"),
                    item(id = 11L, previewText = "b"),
                    item(id = 12L, previewText = "c"),
                )

            // Act
            dao.insertAll(items)
            val loaded = dao.getAllOnce().associateBy { it.id }

            // Assert
            assertEquals(3, loaded.size)
            assertEquals("a", loaded.getValue(10L).previewText)
            assertEquals("b", loaded.getValue(11L).previewText)
            assertEquals("c", loaded.getValue(12L).previewText)
        }

    @Test
    fun update_changesPreviewAndText() =
        runTest {
            // Arrange
            val original =
                ClipboardItem(
                    id = 1L,
                    contentType = ContentType.TEXT,
                    textContent = "old-text",
                    filePath = null,
                    previewText = "old-preview",
                    timestamp = 50L,
                    isPinned = false,
                    isLocked = false,
                    latitude = null,
                    longitude = null,
                )
            dao.insert(original)

            // Act
            dao.update(
                original.copy(
                    textContent = "new-text",
                    previewText = "new-preview",
                ),
            )
            val updated = dao.getAllOnce().single()

            // Assert
            assertEquals("new-text", updated.textContent)
            assertEquals("new-preview", updated.previewText)
            assertEquals(1L, updated.id)
            assertEquals(50L, updated.timestamp)
        }

    @Test
    fun insert_duplicatePrimaryKey_fails() =
        runTest {
            // Arrange
            val item = item(id = 1L, previewText = "first")
            dao.insert(item)

            // Act
            val result = runCatching { dao.insert(item.copy(previewText = "second")) }

            // Assert
            val error = result.exceptionOrNull()
            assertNotNull("Inserting the same primary key twice must fail", error)
            assertTrue(
                "Expected a constraint failure documenting that IDs are not autogenerated, got $error",
                isConstraintFailure(error!!),
            )
            assertEquals(listOf("first"), dao.getAllOnce().map { it.previewText })
        }

    private fun item(
        id: Long,
        previewText: String = "preview-$id",
        timestamp: Long = id,
        isPinned: Boolean = false,
        isLocked: Boolean = false,
    ) = ClipboardItem(
        id = id,
        contentType = ContentType.TEXT,
        textContent = "text-$id",
        filePath = null,
        previewText = previewText,
        timestamp = timestamp,
        isPinned = isPinned,
        isLocked = isLocked,
        latitude = null,
        longitude = null,
    )

    private fun isConstraintFailure(error: Throwable): Boolean {
        val chain = generateSequence(error) { it.cause }.toList()
        return chain.any { throwable ->
            val name = throwable.javaClass.name
            val message = throwable.message.orEmpty()
            name.contains("SQLiteConstraintException") ||
                name.contains("SQLiteException") ||
                message.contains("UNIQUE", ignoreCase = true) ||
                message.contains("constraint", ignoreCase = true) ||
                message.contains("PRIMARY KEY", ignoreCase = true) ||
                message.contains("1555") ||
                message.contains("2067")
        }
    }
}
