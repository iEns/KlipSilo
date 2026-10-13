package com.phaprodevelop.klipsilo.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class RomanAndListLetterTest {
    @Test
    fun toRoman_convertsKnownValues() {
        // Act / Assert
        assertEquals("i", toRoman(1))
        assertEquals("iv", toRoman(4))
        assertEquals("ix", toRoman(9))
        assertEquals("xliv", toRoman(44))
        assertEquals("mcmxcix", toRoman(1999))
    }

    @Test
    fun toListLetter_wrapsPastZAndHonorsCase() {
        // Act / Assert
        assertEquals("a", toListLetter(1, upper = false))
        assertEquals("z", toListLetter(26, upper = false))
        assertEquals("aa", toListLetter(27, upper = false))
        assertEquals("A", toListLetter(1, upper = true))
    }
}
