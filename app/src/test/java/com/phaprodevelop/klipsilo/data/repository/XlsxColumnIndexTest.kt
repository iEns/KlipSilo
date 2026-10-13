package com.phaprodevelop.klipsilo.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XlsxColumnIndexTest {
    @Test
    fun columnIndex_letterRefsAreZeroBased() {
        // Act / Assert
        assertEquals(0, columnIndex("A1"))
        assertEquals(1, columnIndex("B1"))
        assertEquals(25, columnIndex("Z1"))
        assertEquals(26, columnIndex("AA1"))
        assertEquals(27, columnIndex("AB2"))
    }

    @Test
    fun columnIndex_digitsOnly_isNull() {
        // Arrange
        val ref = "123"

        // Act
        val result = columnIndex(ref)

        // Assert
        assertNull(result)
    }

    @Test
    fun columnIndex_lowercaseLetters() {
        // Arrange
        val ref = "c3"

        // Act
        val result = columnIndex(ref)

        // Assert
        assertEquals(2, result)
    }
}
