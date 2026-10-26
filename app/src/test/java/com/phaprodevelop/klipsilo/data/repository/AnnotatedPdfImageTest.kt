package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * A placed image annotation is baked into the saved PDF as an image XObject (#92), so the
 * output keeps the image rather than only the text/stroke marks.
 */
@RunWith(RobolectricTestRunner::class)
class AnnotatedPdfImageTest {
    @Test
    fun savePdfWithAnnotations_embedsPlacedImage() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val dao = FakeClipboardItemDao()
            val repo = ClipboardRepository(dao, context)
            val document = PDDocument()
            document.addPage(PDPage(PDRectangle(612f, 792f)))
            val source =
                File(context.cacheDir, "image_annotation_source.pdf").apply {
                    writeBytes(ByteArrayOutputStream().also { document.save(it) }.toByteArray())
                }
            document.close()
            val image =
                File(context.cacheDir, "image_annotation.png").apply {
                    FileOutputStream(this).use { out ->
                        Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888).apply {
                            eraseColor(Color.RED)
                        }.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
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
                    annotations = emptyList(),
                    strokes = emptyList(),
                    originalEntry = entry,
                    images =
                        listOf(
                            PdfImageAnnotation(
                                pageIndex = 0,
                                xFraction = 0.2f,
                                yFraction = 0.3f,
                                widthFraction = 0.4f,
                                heightFraction = 0.3f,
                                imagePath = image.absolutePath,
                                zOrder = 1L,
                            ),
                        ),
                )

            assertNotEquals(0L, newId)
            val saved = File(dao.getById(newId)!!.filePath!!)
            PDDocument.load(saved).use { savedDocument ->
                val page = savedDocument.getPage(0)
                val xObjectNames = page.resources.xObjectNames.toList()
                assertTrue("saved page should embed the image", xObjectNames.isNotEmpty())
                assertTrue(
                    page.resources.getXObject(xObjectNames.first()) is PDImageXObject,
                )
            }
        }
}
