package com.phaprodevelop.klipsilo.ui.screen

import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import org.junit.Assert.assertEquals
import org.junit.Test

/** The file info card's action rows (#66, #71). */
class FileInfoActionsTest {
    private fun entry(
        contentType: ContentType,
        filePath: String?,
    ) = ClipboardEntry(
        id = 1L,
        contentType = contentType,
        filePath = filePath,
        previewText = "sample",
        timestamp = 0L,
    )

    @Test
    fun fileInfoCardOffersMoreAsTheLastRow() {
        val kinds = fileInfoActionKinds(entry(ContentType.FILE, "/storage/document.docx"))

        assertEquals(
            listOf(
                FileInfoActionKind.SHARE,
                FileInfoActionKind.SAVE_TO_DOWNLOADS,
                FileInfoActionKind.CONVERT_TO_PDF,
                FileInfoActionKind.MORE,
            ),
            kinds,
        )
    }

    @Test
    fun unknownFileKeepsTryAsTextBeforeMore() {
        val kinds = fileInfoActionKinds(entry(ContentType.FILE, "/storage/archive.zip"))

        assertEquals(
            listOf(
                FileInfoActionKind.SHARE,
                FileInfoActionKind.SAVE_TO_DOWNLOADS,
                FileInfoActionKind.TRY_AS_TEXT,
                FileInfoActionKind.MORE,
            ),
            kinds,
        )
    }

    @Test
    fun undecodableImageOffersConvertAndMore() {
        val kinds = fileInfoActionKinds(entry(ContentType.IMAGE, "/storage/photo.jpg"))

        assertEquals(
            listOf(
                FileInfoActionKind.SHARE,
                FileInfoActionKind.SAVE_TO_DOWNLOADS,
                FileInfoActionKind.CONVERT_TO_PDF,
                FileInfoActionKind.MORE,
            ),
            kinds,
        )
    }
}
