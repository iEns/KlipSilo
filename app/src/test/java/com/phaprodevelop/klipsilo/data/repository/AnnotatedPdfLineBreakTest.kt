package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * A committed multi-line markup box keeps its line break in the saved PDF (#59): each line is
 * written on its own baseline, so "Eins⏎Zwei lang" is extractable as two lines.
 */
@RunWith(RobolectricTestRunner::class)
class AnnotatedPdfLineBreakTest {
    @Test
    fun savedAnnotation_keepsTheLineBreak_andWritesEachLine() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val dao = FakeClipboardItemDao()
            val repo = ClipboardRepository(dao, context)
            val document = PDDocument()
            document.addPage(PDPage(PDRectangle(612f, 792f)))
            val source =
                File(context.cacheDir, "line_break_source.pdf").apply {
                    writeBytes(ByteArrayOutputStream().also { document.save(it) }.toByteArray())
                }
            document.close()

            val entry =
                ClipboardEntry(
                    id = 1L,
                    contentType = ContentType.PDF,
                    filePath = source.absolutePath,
                    previewText = "Source",
                    timestamp = 1L,
                )
            val newId =
                repo.savePdfWithAnnotations(
                    pdfPath = source.absolutePath,
                    annotations = listOf(PdfAnnotation(0, 0.4f, 0.1f, "Eins\nZwei lang", 0.05f)),
                    strokes = emptyList(),
                    originalEntry = entry,
                )
            assertTrue(newId > 0L)
            val savedFile = File(dao.getById(newId)!!.filePath!!)
            PDDocument.load(savedFile).use { saved ->
                val text = PDFTextStripper().getText(saved)
                assertEquals("Eins\nZwei lang", text.trim())
            }
        }

    @Test
    fun savedAnnotation_keepsVerticalSpaceForInternalBlankLines() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val dao = FakeClipboardItemDao()
            val repo = ClipboardRepository(dao, context)
            val document = PDDocument()
            document.addPage(PDPage(PDRectangle(612f, 792f)))
            val source =
                File(context.cacheDir, "blank_lines_source.pdf").apply {
                    writeBytes(ByteArrayOutputStream().also { document.save(it) }.toByteArray())
                }
            document.close()

            val entry =
                ClipboardEntry(
                    id = 1L,
                    contentType = ContentType.PDF,
                    filePath = source.absolutePath,
                    previewText = "Source",
                    timestamp = 1L,
                )
            val newId =
                repo.savePdfWithAnnotations(
                    pdfPath = source.absolutePath,
                    annotations = listOf(PdfAnnotation(0, 0.1f, 0.1f, "line1\n\n\nline4", 0.05f)),
                    strokes = emptyList(),
                    originalEntry = entry,
                )
            assertTrue(newId > 0L)
            val savedFile = File(dao.getById(newId)!!.filePath!!)
            PDDocument.load(savedFile).use { saved ->
                val stripper = LineCaptureStripper()
                stripper.getText(saved)
                assertEquals(listOf("line1", "line4"), stripper.lines.map { it.first })
                val gap = stripper.lines[1].second - stripper.lines[0].second
                // line4 sits three line advances below line1 when the two blank lines survive;
                // a collapsed wrap puts it only one advance below.
                val fontSize = 0.05f * 792f
                assertTrue("line gap was $gap", kotlin.math.abs(gap) >= 2f * fontSize)
            }
        }
}

/** Captures each extracted line's text and its device-space y position. */
private class LineCaptureStripper : PDFTextStripper() {
    val lines = mutableListOf<Pair<String, Float>>()

    override fun writeString(
        text: String,
        textPositions: List<TextPosition>,
    ) {
        if (text.isNotBlank()) lines.add(text.trim() to textPositions.first().yDirAdj)
    }
}
