@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.screen

import com.phaprodevelop.klipsilo.data.db.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ClipOpenTargetTest {
    @Test
    fun defaultClipOpen_routesEachType() {
        assertEquals(ClipOpenTarget.DETAIL, defaultClipOpen(ContentType.TEXT))
        assertEquals(ClipOpenTarget.DETAIL, defaultClipOpen(ContentType.MARKDOWN))
        assertEquals(ClipOpenTarget.DETAIL, defaultClipOpen(ContentType.PDF))
        assertEquals(ClipOpenTarget.DETAIL, defaultClipOpen(ContentType.IMAGE))
        assertEquals(ClipOpenTarget.DETAIL, defaultClipOpen(ContentType.FILE))
        assertEquals(ClipOpenTarget.URL, defaultClipOpen(ContentType.URL))
        assertEquals(ClipOpenTarget.LOCATION, defaultClipOpen(ContentType.LOCATION))
    }

    @Test
    fun defaultClipOpen_neverOpensPresentation() {
        ContentType.entries.forEach { type ->
            assertNotEquals(ClipOpenTarget.PRESENTATION, defaultClipOpen(type))
        }
    }
}
