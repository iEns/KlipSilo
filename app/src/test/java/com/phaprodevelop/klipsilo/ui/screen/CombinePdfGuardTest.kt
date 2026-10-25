package com.phaprodevelop.klipsilo.ui.screen

import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** #131: Combine PDFs needs at least two image/PDF clips before it opens a picker. */
class CombinePdfGuardTest {
    @Test
    fun emptyLibraryCannotCombineToPdf() {
        assertFalse(hasTwoCombinableClips(emptyList()))
    }

    @Test
    fun oneImageCannotCombineToPdf() {
        assertFalse(hasTwoCombinableClips(listOf(clip(1L, ContentType.IMAGE))))
    }

    @Test
    fun onePdfCannotCombineToPdf() {
        assertFalse(hasTwoCombinableClips(listOf(clip(1L, ContentType.PDF))))
    }

    @Test
    fun imagePlusTextCannotCombineToPdf() {
        assertFalse(
            hasTwoCombinableClips(
                listOf(
                    clip(1L, ContentType.IMAGE),
                    clip(2L, ContentType.TEXT),
                ),
            ),
        )
    }

    @Test
    fun imagePlusPdfCanCombineToPdf() {
        assertTrue(
            hasTwoCombinableClips(
                listOf(
                    clip(1L, ContentType.IMAGE),
                    clip(2L, ContentType.PDF),
                ),
            ),
        )
    }

    @Test
    fun twoPdfsCanCombineToPdfEvenWithOtherTypes() {
        assertTrue(
            hasTwoCombinableClips(
                listOf(
                    clip(1L, ContentType.PDF),
                    clip(2L, ContentType.TEXT),
                    clip(3L, ContentType.PDF),
                ),
            ),
        )
    }

    private fun clip(
        id: Long,
        type: ContentType,
    ) = ClipboardEntry(
        id = id,
        contentType = type,
        previewText = "clip $id",
        timestamp = 0L,
    )
}
