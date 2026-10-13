package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
class WriteStreamLimitedTest {
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
        File(context.filesDir, "limited.bin").delete()
        File(context.filesDir, "too-large.bin").delete()
    }

    @Test
    fun smallPayload_returnsOkAndWritesBytes() =
        runTest {
            // Arrange: header 4 bytes + 4 more from the stream, under maxBytes=16
            val dest = File(context.filesDir, "limited.bin")
            val headerBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
            val rest = byteArrayOf(0x05, 0x06, 0x07, 0x08)
            val header = HeaderResult(headerBytes, headerBytes.size)

            // Act
            val result =
                repo.writeStreamLimited(
                    ByteArrayInputStream(rest),
                    dest,
                    header,
                    maxBytes = 16L,
                )

            // Assert
            assertTrue(result is CopyResult.Ok)
            assertEquals(dest.absolutePath, (result as CopyResult.Ok).path)
            assertTrue(dest.exists())
            assertArrayEquals(headerBytes + rest, dest.readBytes())
        }

    @Test
    fun payloadExceedingMax_returnsTooLargeAndDeletesDest() =
        runTest {
            // Arrange: header 4 bytes + 20 more exceeds maxBytes=16
            val dest = File(context.filesDir, "too-large.bin")
            val headerBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
            val rest = ByteArray(20) { 0xAA.toByte() }
            val header = HeaderResult(headerBytes, headerBytes.size)

            // Act
            val result =
                repo.writeStreamLimited(
                    ByteArrayInputStream(rest),
                    dest,
                    header,
                    maxBytes = 16L,
                )

            // Assert
            assertEquals(CopyResult.TooLarge, result)
            assertFalse(dest.exists())
        }
}
