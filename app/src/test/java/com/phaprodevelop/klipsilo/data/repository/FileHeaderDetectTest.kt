package com.phaprodevelop.klipsilo.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

class FileHeaderDetectTest {
    @Test
    fun jpeg() {
        // Arrange
        val header = headerOf(0xFF, 0xD8, 0xFF)

        // Act
        val ext = detectExtension(header)

        // Assert
        assertEquals(".jpg", ext)
    }

    @Test
    fun png() {
        // Arrange
        val header = headerOf(0x89, 0x50, 0x4E, 0x47)

        // Act
        val ext = detectExtension(header)

        // Assert
        assertEquals(".png", ext)
    }

    @Test
    fun gif() {
        // Arrange
        val header = headerOf(0x47, 0x49, 0x46, 0x38)

        // Act
        val ext = detectExtension(header)

        // Assert
        assertEquals(".gif", ext)
    }

    @Test
    fun webp() {
        // Arrange: RIFF at 0, WEBP at 8 (matches uses bytesRead, not array padding)
        val header =
            headerOf(
                0x52, 0x49, 0x46, 0x46,
                0x00, 0x00, 0x00, 0x00,
                0x57, 0x45, 0x42, 0x50,
            )

        // Act
        val ext = detectExtension(header)

        // Assert
        assertEquals(".webp", ext)
    }

    @Test
    fun bmp() {
        // Arrange
        val header = headerOf(0x42, 0x4D)

        // Act
        val ext = detectExtension(header)

        // Assert
        assertEquals(".bmp", ext)
    }

    @Test
    fun heic_heic() {
        // Arrange: ftyp at offset 4, brand heic at offset 8
        val header =
            headerOf(
                0x00, 0x00, 0x00, 0x00,
                0x66, 0x74, 0x79, 0x70,
                0x68, 0x65, 0x69, 0x63,
            )

        // Act
        val ext = detectExtension(header)

        // Assert
        assertEquals(".heic", ext)
    }

    @Test
    fun heic_heix() {
        // Arrange: ftyp at offset 4, brand heix at offset 8
        val header =
            headerOf(
                0x00, 0x00, 0x00, 0x00,
                0x66, 0x74, 0x79, 0x70,
                0x68, 0x65, 0x69, 0x78,
            )

        // Act
        val ext = detectExtension(header)

        // Assert
        assertEquals(".heic", ext)
    }

    @Test
    fun heic_mif3() {
        // Arrange: ftyp at offset 4, brand mif3 at offset 8
        val header =
            headerOf(
                0x00, 0x00, 0x00, 0x00,
                0x66, 0x74, 0x79, 0x70,
                0x6D, 0x69, 0x66, 0x33,
            )

        // Act
        val ext = detectExtension(header)

        // Assert
        assertEquals(".heic", ext)
    }

    @Test
    fun heic_unknownBrand() {
        // Arrange: ftyp at offset 4, brand mp42 at offset 8
        val header =
            headerOf(
                0x00, 0x00, 0x00, 0x00,
                0x66, 0x74, 0x79, 0x70,
                0x6D, 0x70, 0x34, 0x32,
            )

        // Act
        val ext = detectExtension(header)

        // Assert
        assertNull(ext)
    }

    @Test
    fun pdf() {
        // Arrange
        val header = headerOf(0x25, 0x50, 0x44, 0x46)

        // Act
        val ext = detectExtension(header)

        // Assert
        assertEquals(".pdf", ext)
    }

    @Test
    fun tooShort() {
        // Arrange: JPEG SOI is 3 bytes; 2 bytes cannot match
        val header = headerOf(0xFF, 0xD8)

        // Act
        val ext = detectExtension(header)

        // Assert
        assertNull(ext)
    }

    @Test
    fun unknown() {
        // Arrange
        val header = headerOf(0x00, 0x00, 0x00, 0x00)

        // Act
        val ext = detectExtension(header)

        // Assert
        assertNull(ext)
    }

    @Test
    fun readHeader_readsAtMost12() {
        // Arrange
        val stream = ByteArrayInputStream(ByteArray(100) { 0x41 })

        // Act
        val header = readHeader(stream)

        // Assert
        assertEquals(12, header.bytesRead)
    }

    @Test
    fun readHeader_shortStream() {
        // Arrange
        val stream = ByteArrayInputStream(byteArrayOf(0x01, 0x02, 0x03))

        // Act
        val header = readHeader(stream)

        // Assert
        assertEquals(3, header.bytesRead)
    }

    private fun headerOf(vararg bytes: Int): HeaderResult {
        val data = bytes.map { it.toByte() }.toByteArray()
        return HeaderResult(data, bytes.size)
    }
}
