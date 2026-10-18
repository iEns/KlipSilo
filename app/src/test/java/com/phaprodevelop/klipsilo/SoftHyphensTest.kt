package com.phaprodevelop.klipsilo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * [String.withoutSoftHyphens] and [String.withoutInvisibleBreaks] for every
 * use of a localized string outside a visible label (#62, #64).
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

    @Test
    fun removesEverySoftHyphenAndZeroWidthSpace() {
        assertEquals(
            "Einstellungen Event_Registration_Form 1.2.0-beta.4-debug+ee33946",
            (
                "Ein\u00ADstellungen " +
                    "Event_\u200BRegistration\u200B_Form " +
                    "1.2.0-\u200Bbeta.4-\u200Bdebug+\u200Bee33946"
            ).withoutInvisibleBreaks(),
        )
    }

    @Test
    fun removesZeroWidthSpaceAlone() {
        assertEquals("Pakkeliste - weekendtur", "Pakkeliste -\u200B weekendtur".withoutInvisibleBreaks())
    }

    @Test
    fun withoutSoftHyphensAlsoStripsTheZeroWidthSpace() {
        // Every existing withoutSoftHyphens call site is a
        // contentDescription/semantics path, so it must be clean of both
        // marks (#64).
        assertEquals(
            "Aktuelle Zwischenablage speicherung",
            "Aktuelle Zwischen\u00ADablage speicher\u200Bung".withoutSoftHyphens(),
        )
    }

    @Test
    fun stringsWithoutInvisibleBreaksAreTheSameInstance() {
        val clean = "Einstellungen"
        assertSame(clean, clean.withoutInvisibleBreaks())
        assertSame(clean, clean.withoutSoftHyphens())
        assertSame("", "".withoutInvisibleBreaks())
    }

    @Test
    fun emojiAndCjkAreUntouched() {
        val mixed = "📋 日本語 한국 的 text — 😀🇩🇰"
        assertSame(mixed, mixed.withoutInvisibleBreaks())
        assertEquals(mixed, mixed.withoutInvisibleBreaks())
    }
}
