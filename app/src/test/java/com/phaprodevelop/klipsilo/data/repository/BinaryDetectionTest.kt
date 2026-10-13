package com.phaprodevelop.klipsilo.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BinaryDetectionTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun missingFile_isBinary() {
        // Arrange
        val missing = File(tmp.root, "does-not-exist.bin")
        assertFalse(missing.exists())

        // Act / Assert
        assertTrue(isProbablyBinary(missing))
    }

    @Test
    fun emptyFile_isNotBinary() {
        // Empty file: buffer.size == 0, so controlCount > 0 is false → NOT binary.
        val empty = tmp.newFile("empty.bin")
        assertTrue(empty.exists())
        assertEquals(0L, empty.length())

        assertFalse(isProbablyBinary(empty))
    }

    @Test
    fun fileContainingNul_isBinary() {
        // Arrange
        val file = tmp.newFile("nul.bin")
        file.writeBytes(byteArrayOf('h'.code.toByte(), 0x00, 'i'.code.toByte()))

        // Act / Assert
        assertTrue(isProbablyBinary(file))
    }

    @Test
    fun utf8TextWithNewlineAndTab_isNotBinary() {
        // Arrange: tab (9) and newline (10) are excluded from the control-byte count
        val file = tmp.newFile("hello.txt")
        file.writeText("hello\n\tworld", Charsets.UTF_8)

        // Act / Assert
        assertFalse(isProbablyBinary(file))
    }

    @Test
    fun hundredControlBytes_isBinary() {
        // Arrange: 100 × 0x01 → controlCount=100 > 10
        val file = tmp.newFile("all-control.bin")
        file.writeBytes(ByteArray(100) { 0x01.toByte() })

        // Act / Assert
        assertTrue(isProbablyBinary(file))
    }

    @Test
    fun tenPercentControl_isNotBinary_elevenIsBinary() {
        // 10 control bytes in 100: 10 > 10 is false → not binary.
        // 11 control bytes in 100: 11 > 10 is true → binary.
        val ten = tmp.newFile("ten-control.bin")
        ten.writeBytes(bytesWithControlPrefix(controlCount = 10, total = 100))
        assertFalse(isProbablyBinary(ten))

        val eleven = tmp.newFile("eleven-control.bin")
        eleven.writeBytes(bytesWithControlPrefix(controlCount = 11, total = 100))
        assertTrue(isProbablyBinary(eleven))
    }

    @Test
    fun danishUtf8HeavyText_isNotBinary() {
        // Arrange: æøå is C3 A6 / C3 B8 / C3 A5 — all high bytes; repeats exceed 10%.
        val file = tmp.newFile("danish.txt")
        file.writeText("æøå".repeat(200), Charsets.UTF_8)

        // Act / Assert
        assertFalse(isProbablyBinary(file))
    }

    @Test
    fun first4KiBOverTenPercent0xE6_isNotBinary() {
        // Arrange: 0xE6 is unsigned 230, not a control byte (and not NUL).
        val file = tmp.newFile("high-e6.bin")
        val bytes =
            ByteArray(4096) { index ->
                if (index < 500) 0xE6.toByte() else 'A'.code.toByte()
            }
        file.writeBytes(bytes)

        // Act / Assert
        assertFalse(isProbablyBinary(file))
    }

    @Test
    fun mixedAsciiWithOneNul_isBinary() {
        // Arrange
        val file = tmp.newFile("ascii-nul.bin")
        val bytes =
            ByteArray(100) { index ->
                if (index == 50) 0x00 else 'A'.code.toByte()
            }
        file.writeBytes(bytes)

        // Act / Assert
        assertTrue(isProbablyBinary(file))
    }

    private fun bytesWithControlPrefix(
        controlCount: Int,
        total: Int,
    ): ByteArray =
        ByteArray(total) { index ->
            if (index < controlCount) 0x01.toByte() else 'A'.code.toByte()
        }
}
