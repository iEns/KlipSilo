@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.screen

import android.app.Application
import android.net.Uri
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.repository.ClipboardRepository
import com.phaprodevelop.klipsilo.data.repository.textViewFileText
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.phaprodevelop.klipsilo.ui.components.decodeImageBitmap
import com.phaprodevelop.klipsilo.ui.components.pdfPageCount
import com.phaprodevelop.klipsilo.ui.components.renderPdfPage
import com.phaprodevelop.klipsilo.ui.components.renderableClipHtml
import com.phaprodevelop.klipsilo.util.MainDispatcherRule
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The viewer load paths behind [defaultClipOpen] DETAIL (#127): every clip
 * type that opens the detail screen must produce usable content on the JVM.
 * Robolectric cannot drive a real PdfRenderer or WebView, so those two checks
 * assert the no-crash contract and leave the visual render to a device.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ClipOpenLoadPathTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var app: Application
    private lateinit var repo: ClipboardRepository
    private lateinit var viewModel: ClipboardViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        repo = ClipboardRepository(FakeClipboardItemDao(), app)
        viewModel =
            ClipboardViewModel(
                application = app,
                repository = repo,
                ioDispatcher = mainDispatcherRule.dispatcher,
            )
    }

    @After
    fun tearDown() {
        if (!::app.isInitialized) return
        File(app.cacheDir, "clip-open-load-test").deleteRecursively()
    }

    @Test
    fun textAndMarkdown_loadStoredBody() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            val text =
                ClipboardEntry(
                    id = 1L,
                    contentType = ContentType.TEXT,
                    textContent = "A tiny plain-text body",
                    previewText = "A tiny plain-text body",
                    timestamp = 1L,
                )
            val markdown =
                ClipboardEntry(
                    id = 2L,
                    contentType = ContentType.MARKDOWN,
                    textContent = "# A tiny markdown body",
                    previewText = "A tiny markdown body",
                    timestamp = 2L,
                )
            repo.insertEntry(text)
            repo.insertEntry(markdown)

            // Act
            val textBody = repo.readTextBody(text)
            val markdownBody = repo.readTextBody(markdown)

            // Assert
            assertEquals(ClipOpenTarget.DETAIL, defaultClipOpen(ContentType.TEXT))
            assertEquals(ClipOpenTarget.DETAIL, defaultClipOpen(ContentType.MARKDOWN))
            assertEquals("A tiny plain-text body", textBody)
            assertEquals("# A tiny markdown body", markdownBody)
        }

    @Test
    fun image_decodeImageBitmap_returnsTinyBitmap() {
        // Arrange — a 1×1 transparent PNG, small enough to keep in the test.
        val png =
            Base64.decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==",
                Base64.DEFAULT,
            )
        val image = File(testDir(), "tiny.png").apply { writeBytes(png) }

        // Act
        val bitmap = decodeImageBitmap(image.absolutePath, dstWidthPx = 64, dstHeightPx = 64)

        // Assert
        assertEquals(ClipOpenTarget.DETAIL, defaultClipOpen(ContentType.IMAGE))
        assertNotNull(bitmap)
        assertEquals(1, bitmap!!.width)
        assertEquals(1, bitmap.height)
        bitmap.recycle()
    }

    @Test
    fun pdf_pageCountAndRender_doNotCrash() {
        // Arrange — a simple, non-form PDF rather than the larger form fixtures.
        val pdf = writeSimplePdf(File(testDir(), "simple.pdf"))

        // Act
        val rendererCount = pdfPageCount(pdf.absolutePath)
        val bitmap = renderPdfPage(pdf.absolutePath, maxDimension = 128, pageIndex = 0)

        // Assert — PdfRenderer is native; Robolectric may report 0 pages. A
        // generated 1-page PDF must never crash the load path, and when the
        // renderer is available it must render something usable.
        assertEquals(ClipOpenTarget.DETAIL, defaultClipOpen(ContentType.PDF))
        val pdfBoxCount = PDDocument.load(pdf).use { it.numberOfPages }
        assertEquals(1, pdfBoxCount)
        assertTrue("renderer page count must not be negative", rendererCount >= 0)
        if (rendererCount > 0) {
            assertNotNull(bitmap)
            assertTrue(bitmap!!.width > 0 && bitmap.height > 0)
            bitmap.recycle()
        }
    }

    @Test
    fun fileViewer_txtHtmlAndUnknownBinary() {
        // Arrange
        val txt = File(testDir(), "notes.txt").apply { writeText("hello file viewer") }
        val html =
            File(testDir(), "page.html").apply {
                writeText("<!DOCTYPE html><html><body><p>Hello HTML</p></body></html>")
            }
        val htm = File(testDir(), "page.htm").apply { writeText("<html><body><p>Hello HTM</p></body></html>") }
        val binary = File(testDir(), "archive.zip").apply { writeBytes(byteArrayOf(0x50, 0x4B, 0x00, 0x01)) }

        // Act
        val txtBody = textViewFileText(txt.absolutePath)
        val htmlBody = textViewFileText(html.absolutePath)
        val htmBody = textViewFileText(htm.absolutePath)
        val binaryBody = textViewFileText(binary.absolutePath)

        // Assert
        assertEquals(ClipOpenTarget.DETAIL, defaultClipOpen(ContentType.FILE))
        assertEquals("hello file viewer", txtBody)
        assertEquals(
            "<!DOCTYPE html><html><body><p>Hello HTML</p></body></html>",
            htmlBody,
        )
        assertEquals("<html><body><p>Hello HTM</p></body></html>", htmBody)
        assertNull(binaryBody)
        assertTrue(renderableClipHtml(htmlBody!!).contains("Hello HTML"))
        assertTrue(renderableClipHtml(htmBody!!).contains("Hello HTM"))
    }

    @Test
    fun urlAndLocation_routeAndStoredValuesParse() {
        // Arrange
        val url =
            ClipboardEntry(
                id = 3L,
                contentType = ContentType.URL,
                textContent = "https://example.com/path",
                previewText = "example.com",
                timestamp = 3L,
            )
        val location =
            ClipboardEntry(
                id = 4L,
                contentType = ContentType.LOCATION,
                previewText = "55.676098, 12.568337",
                timestamp = 4L,
                latitude = 55.676098,
                longitude = 12.568337,
            )

        // Act
        val httpUri = viewModel.httpUri(url.textContent!!)
        val geoUri = Uri.parse("geo:0,0?q=${location.latitude},${location.longitude}")

        // Assert — no browser or maps Activity is started in this JVM test.
        assertEquals(ClipOpenTarget.URL, defaultClipOpen(ContentType.URL))
        assertEquals(ClipOpenTarget.LOCATION, defaultClipOpen(ContentType.LOCATION))
        assertNotNull(httpUri)
        assertEquals("https", httpUri!!.scheme)
        assertEquals("geo", geoUri.scheme)
    }

    @Test
    fun defaultClipOpen_everyType_mapsToAConcreteTarget() {
        val allowed = setOf(ClipOpenTarget.DETAIL, ClipOpenTarget.URL, ClipOpenTarget.LOCATION)

        ContentType.entries.forEach { type ->
            val target = defaultClipOpen(type)
            assertTrue("$type should not open presentation", target != ClipOpenTarget.PRESENTATION)
            assertTrue("$type has an unexpected target $target", target in allowed)
        }
    }

    private fun testDir(): File = File(app.cacheDir, "clip-open-load-test").apply { mkdirs() }

    private fun writeSimplePdf(outFile: File): File {
        PDDocument().use { document ->
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            PDPageContentStream(document, page).use { stream ->
                stream.beginText()
                stream.setFont(PDType1Font.HELVETICA, 12f)
                stream.newLineAtOffset(72f, 720f)
                stream.showText("KlipSilo test PDF")
                stream.endText()
            }
            document.save(outFile)
        }
        return outFile
    }
}
