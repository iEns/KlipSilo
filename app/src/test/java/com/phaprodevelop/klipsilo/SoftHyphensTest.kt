package com.phaprodevelop.klipsilo

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [String.withoutSoftHyphens] for every use of a localized string outside a
 * visible label (#62).
 */
class SoftHyphensTest {
    @Test
    fun removesEverySoftHyphen() {
        assertEquals(
            "Datenschutzerklärung Ausgaberesolution",
            "Datenschutz\u00ADerklärung Aus\u00ADgabe\u00ADreso\u00ADlution".withoutSoftHyphens(),
        )
    }

    @Test
    fun keepsEveryOtherCharacter() {
        // Ordinary hyphens, spaces and other separators stay untouched.
        assertEquals(
            "Maximale Ausgabe-resolution (2.0)",
            "Max\u00ADimale Ausgabe-resolution (2.0)".withoutSoftHyphens(),
        )
    }

    @Test
    fun stringsWithoutSoftHyphensAreUnchanged() {
        assertEquals("Einstellungen", "Einstellungen".withoutSoftHyphens())
        assertEquals("", "".withoutSoftHyphens())
    }
}
