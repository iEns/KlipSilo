package com.phaprodevelop.klipsilo.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ClipSheetActionGroupTest {
    @Test
    fun everySheetAction_hasItsExpectedSection() {
        val expected =
            mapOf(
                SheetSection.PRIMARY to
                    setOf(
                        ClipSheetAction.VIEW,
                        ClipSheetAction.PRESENT,
                        ClipSheetAction.SHARE,
                        ClipSheetAction.SAVE_TO_DOWNLOADS,
                        ClipSheetAction.DOWNLOAD_FROM_URL,
                        ClipSheetAction.OPEN_IN_BROWSER,
                    ),
                SheetSection.CONVERT to
                    setOf(
                        ClipSheetAction.TEXT_TO_PDF,
                        ClipSheetAction.TEXT_TO_TXT,
                        ClipSheetAction.IMAGE_TO_PDF,
                        ClipSheetAction.PDF_TO_IMAGE,
                        ClipSheetAction.PDF_TO_TEXT,
                        ClipSheetAction.FILE_TO_TEXT,
                        ClipSheetAction.CONVERT_TO_PDF,
                        ClipSheetAction.CONVERT_TO_HTML,
                        ClipSheetAction.CONVERT_HTML_TO_PDF,
                        ClipSheetAction.TRY_AS_TEXT,
                        ClipSheetAction.OCR_IMAGE,
                    ),
                SheetSection.TOOLS to
                    setOf(
                        ClipSheetAction.ANNOTATE_PDF,
                        ClipSheetAction.FILL_FORM,
                        ClipSheetAction.EDIT_PAGES,
                        ClipSheetAction.FIND_LINKS,
                        ClipSheetAction.PIN,
                        ClipSheetAction.LOCK,
                    ),
                SheetSection.MANAGE to
                    setOf(
                        ClipSheetAction.RENAME,
                        ClipSheetAction.DUPLICATE,
                        ClipSheetAction.DELETE,
                    ),
            )

        val actual =
            ClipSheetAction.entries
                .groupBy { it.section }
                .mapValues { (_, actions) -> actions.toSet() }

        assertEquals(expected, actual)
    }
}
