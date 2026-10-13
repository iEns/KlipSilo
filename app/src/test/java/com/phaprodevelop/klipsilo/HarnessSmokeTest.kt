package com.phaprodevelop.klipsilo

import com.phaprodevelop.klipsilo.data.db.ClipboardItem
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class HarnessSmokeTest {
    @Test
    fun fakeDao_insert_roundTrips() =
        runBlocking {
            val dao = FakeClipboardItemDao()
            val item =
                ClipboardItem(
                    id = 1L,
                    contentType = ContentType.TEXT,
                    textContent = "hello",
                    previewText = "hello",
                    timestamp = 100L,
                )
            assertEquals(1L, dao.insert(item))
            val listRow = dao.getAll().first().single()
            assertEquals(1L, listRow.id)
            assertEquals("hello", listRow.previewText)
            assertEquals("hello", dao.getById(1L)?.textContent)
        }
}
