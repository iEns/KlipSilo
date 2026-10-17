package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.phaprodevelop.klipsilo.ui.components.pdfPageCount
import com.phaprodevelop.klipsilo.ui.components.pdfPageHeightPt
import com.phaprodevelop.klipsilo.ui.components.renderPdfPage
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotation
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceEntry
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDCheckBox
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PdfOperationsTest {
    private lateinit var dao: FakeClipboardItemDao
    private lateinit var repo: ClipboardRepository
    private lateinit var context: Application
    private lateinit var twoPagePdf: File
    private var acroFormPdf: File? = null

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dao = FakeClipboardItemDao()
        repo = ClipboardRepository(dao, context)
        twoPagePdf = writeBlankPdf("blank-2pages.pdf", pageCount = 2)
        acroFormPdf = writeAcroFormPdf("simple-acroform.pdf")
    }

    @After
    fun tearDown() {
        if (!::context.isInitialized) return
        listOf("images", "pdfs", "files").forEach { name ->
            File(context.filesDir, name).deleteRecursively()
        }
    }

    @Test
    fun saveTextAsPdf_blank_returns0_noDaoInsert() =
        runTest {
            // Arrange
            val original = textEntry(id = 1L)
            repo.insertEntry(original)

            // Act
            val emptyId = repo.saveTextAsPdf("", original)
            val whitespaceId = repo.saveTextAsPdf("   \n\t", original)

            // Assert
            assertEquals(0L, emptyId)
            assertEquals(0L, whitespaceId)
            assertEquals(listOf(original.copy(textContent = null)), repo.getAllClips().first())
        }

    @Test
    fun saveTextAsPdf_hello_createsPdfFileAndNewClip_originalUnchanged() =
        runTest {
            // Arrange
            val original = textEntry(id = 2L, text = "hello", preview = "hello")
            repo.insertEntry(original)

            // Act
            val newId = repo.saveTextAsPdf("hello", original)
            val clips = repo.getAllClips().first()
            val kept = clips.single { it.id == original.id }

            // Assert
            assertEquals(original.copy(textContent = null), kept)
            if (newId == 0L) {
                // PdfDocument.nativeCreateDocument is unimplemented on Robolectric, so the
                // document is already "closed" and saveTextAsPdf swallows IllegalStateException.
                assertEquals(1, clips.size)
            } else {
                assertNotEquals(original.id, newId)
                val created = clips.single { it.id == newId }
                val pdfFile = File(created.filePath!!)
                assertEquals(ContentType.PDF, created.contentType)
                assertTrue(pdfFile.exists())
                assertTrue(pdfFile.length() > 0L)
                assertTrue(pdfFile.readBytes().copyOfRange(0, 4).contentEquals("%PDF".toByteArray()))
                assertTrue(pdfBoxPageCount(pdfFile.absolutePath) >= 1)
            }
        }

    @Test
    fun savePdfWithAnnotations_missingFile_returns0() =
        runTest {
            val missing = File(context.filesDir, "pdfs/missing-annotate.pdf")
            val original = pdfEntry(missing, id = 40L, preview = "missing")

            val newId =
                repo.savePdfWithAnnotations(
                    missing.absolutePath,
                    listOf(PdfAnnotation(0, 0.1f, 0.1f, "Gone", 0.02f)),
                    emptyList(),
                    original,
                )

            assertEquals(0L, newId)
            assertEquals(emptyList<ClipboardEntry>(), repo.getAllClips().first())
            assertTrue(File(context.filesDir, "pdfs").listFiles().orEmpty().none { it.name.startsWith("missing") })
        }

    @Test
    fun savePdfWithAnnotations_keepsOriginalText_andPlacesMark() =
        runTest {
            val source = writeTextPdf("annotate-src.pdf", listOf("KEEPME", "SECOND"))
            val original = pdfEntry(source, id = 41L, preview = "annotate-src")
            repo.insertEntry(original)

            val newId =
                repo.savePdfWithAnnotations(
                    source.absolutePath,
                    listOf(PdfAnnotation(0, 0.1f, 0.2f, "Æble", 0.02f, android.graphics.Color.RED)),
                    listOf(
                        PdfStroke(
                            pageIndex = 0,
                            points = listOf(0.2f to 0.3f, 0.4f to 0.5f),
                            widthFraction = 0.005f,
                            color = android.graphics.Color.BLUE,
                        ),
                    ),
                    original,
                )

            assertNotEquals(0L, newId)
            val created = repo.getAllClips().first().single { it.id == newId }
            val createdFile = File(created.filePath!!)
            assertEquals(ContentType.PDF, created.contentType)
            assertTrue(createdFile.length() < 1_000_000L)
            assertEquals(source.absolutePath, repo.getAllClips().first().single { it.id == original.id }.filePath)
            PDDocument.load(createdFile).use { document ->
                assertEquals(2, document.numberOfPages)
                assertTrue(pageText(document, 1).contains("KEEPME"))
                assertTrue(pageText(document, 1).contains("Æble"))
                assertTrue(pageText(document, 2).contains("SECOND"))
                assertFalse(pageText(document, 2).contains("Æble"))
                val mark = textPosition(document, 1, "Æble")
                val box = document.getPage(0).cropBox
                val paint =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 0.02f * box.height
                        typeface = Typeface.DEFAULT
                    }
                val expectedX = 0.1f * box.width
                val expectedBaseline = (1f - 0.2f) * box.height - (-paint.ascent()).coerceAtLeast(0f)
                assertEquals(expectedX, mark.textMatrix.translateX, 1.5f)
                assertEquals(expectedBaseline, mark.textMatrix.translateY, 1.5f)
            }
        }

    @Test
    fun savePdfWithAnnotations_rotation90_mapsTopLeftOfDisplayedPage() =
        runTest {
            val source = writeRotatedPdf("annotate-rot90.pdf", 90)
            val original = pdfEntry(source, id = 42L, preview = "annotate-rot90")

            val newId =
                repo.savePdfWithAnnotations(
                    source.absolutePath,
                    listOf(PdfAnnotation(0, 0.25f, 0.4f, "Corner", 0.02f)),
                    emptyList(),
                    original,
                )

            assertNotEquals(0L, newId)
            val createdFile =
                File(repo.getAllClips().first().single { it.id == newId }.filePath!!)
            PDDocument.load(createdFile).use { document ->
                val box = document.getPage(0).cropBox
                val mark = textPosition(document, 1, "Corner")
                val paint =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 0.02f * box.width
                        typeface = Typeface.DEFAULT
                    }
                val visualWidth = box.height
                val visualHeight = box.width
                val baselineFromTop = 0.4f * visualHeight + (-paint.ascent()).coerceAtLeast(0f)
                val uprightX = 0.25f * visualWidth
                val uprightY = visualHeight - baselineFromTop
                assertEquals(box.lowerLeftX + box.width - uprightY, mark.textMatrix.translateX, 1.5f)
                assertEquals(box.lowerLeftY + uprightX, mark.textMatrix.translateY, 1.5f)
                val text = pageText(document, 1).replace(Regex("\\s"), "")
                assertTrue(text, text.contains("UPRIGHT") && text.contains("Corner"))
            }
        }

    @Test
    fun savePdfPages_emptyOrder_returns0() =
        runTest {
            // Arrange
            val original = pdfEntry(twoPagePdf, id = 3L, preview = "empty-order")
            repo.insertEntry(original)

            // Act
            val newId = repo.savePdfPages(twoPagePdf.absolutePath, original, emptyList())

            // Assert
            assertEquals(0L, newId)
            assertEquals(listOf(original), repo.getAllClips().first())
        }

    @Test
    fun savePdfPages_outOfRangeOnly_returns0() =
        runTest {
            // Arrange
            val original = pdfEntry(twoPagePdf, id = 4L, preview = "out-of-range")
            repo.insertEntry(original)

            // Act
            val newId = repo.savePdfPages(twoPagePdf.absolutePath, original, listOf(-1, 99))

            // Assert
            assertEquals(0L, newId)
            assertEquals(listOf(original), repo.getAllClips().first())
        }

    @Test
    fun savePdfPages_order_1_0_producesTwoPagePdf() =
        runTest {
            // Arrange
            val original = pdfEntry(twoPagePdf, id = 5L, preview = "reorder")
            repo.insertEntry(original)

            // Act
            val newId = repo.savePdfPages(twoPagePdf.absolutePath, original, listOf(1, 0))
            val clips = repo.getAllClips().first()
            val created = clips.single { it.id == newId }

            // Assert
            assertNotEquals(0L, newId)
            assertEquals(original, clips.single { it.id == original.id })
            assertEquals(ContentType.PDF, created.contentType)
            assertTrue(File(created.filePath!!).exists())
            assertEquals(2, pdfBoxPageCount(created.filePath!!))
        }

    @Test
    fun getPdfFormFields_noAcroForm_emptyList() =
        runTest {
            // Arrange
            val plain = writeBlankPdf("no-form.pdf", pageCount = 1)

            // Act
            val fields = repo.getPdfFormFields(plain.absolutePath)

            // Assert
            assertTrue(fields.isEmpty())
        }

    @Test
    fun getPdfFormFields_simple_returnsNameAndAgree() =
        runTest {
            // Arrange
            val formFile = acroFormPdf
            assumeTrue(
                "PDFBox AcroForm creation is not feasible under this Robolectric environment",
                formFile != null,
            )

            // Act
            val fields = repo.getPdfFormFields(formFile!!.absolutePath)
            val byName = fields.associateBy { it.name }

            // Assert
            assertEquals(FormFieldType.TEXT, byName["Name"]?.type)
            assertEquals("Name", byName["Name"]?.fullyQualifiedName)
            assertEquals(FormFieldType.CHECKBOX, byName["Agree"]?.type)
            assertEquals("Agree", byName["Agree"]?.fullyQualifiedName)
            assertEquals("Name", byName["Name"]?.label)
            assertEquals("Agree", byName["Agree"]?.label)
        }

    @Test
    fun getPdfFormFields_tooltipAndNearbyText_becomeLabels() =
        runTest {
            val formFile = writeTooltipForm("tooltip-form.pdf")
            assumeTrue(
                "PDFBox AcroForm creation is not feasible under this Robolectric environment",
                formFile != null,
            )

            val fields = repo.getPdfFormFields(formFile!!.absolutePath)
            val byName = fields.associateBy { it.name }

            assertEquals("Full name", byName["name"]?.label)
            assertEquals("Phone", byName["s0"]?.label)
            assertEquals("Email", byName["email"]?.label)
            val hit = byName["name"]?.hits?.single()
            assertEquals(0, hit?.pageIndex)
            assertTrue((hit?.left ?: 0f) > 0.1f)
            assertTrue((hit?.top ?: 1f) < (hit?.bottom ?: 0f))
        }

    @Test
    fun fillPdfForm_setsValues_createsNewPdf_flattenDoesNotThrow() =
        runTest {
            // Arrange
            val formFile = acroFormPdf
            assumeTrue(
                "PDFBox AcroForm creation is not feasible under this Robolectric environment",
                formFile != null,
            )
            val original = pdfEntry(formFile!!, id = 6L, preview = "form-fill")
            repo.insertEntry(original)

            // Act
            val newId =
                repo.fillPdfForm(
                    formFile.absolutePath,
                    original,
                    mapOf("Name" to "Ada", "Agree" to "true"),
                )
            val clips = repo.getAllClips().first()
            val created = clips.single { it.id == newId }
            val createdFile = File(created.filePath!!)

            // Assert
            assertNotEquals(0L, newId)
            assertEquals(original, clips.single { it.id == original.id })
            assertEquals(ContentType.PDF, created.contentType)
            assertTrue(createdFile.exists())
            assertTrue(createdFile.length() > 0L)
            PDDocument.load(createdFile).use { document ->
                val form = document.documentCatalog.acroForm
                val nameField = form?.getField("Name")
                if (nameField != null) {
                    assertEquals("Ada", nameField.valueAsString)
                }
            }
        }

    @Test
    fun fillPdfForm_missingFile_returns0() =
        runTest {
            // Arrange
            val missing = File(context.filesDir, "pdfs/missing-form.pdf")
            val original = pdfEntry(missing, id = 7L, preview = "missing-form")
            repo.insertEntry(original)

            // Act
            val newId = repo.fillPdfForm(missing.absolutePath, original, mapOf("Name" to "Ada"))

            // Assert
            assertEquals(0L, newId)
            assertEquals(listOf(original), repo.getAllClips().first())
        }

    @Test
    fun getPdfFormFields_missingFile_emptyList() =
        runTest {
            // Arrange
            val missing = File(context.filesDir, "pdfs/missing-fields.pdf").absolutePath

            // Act
            val fields = repo.getPdfFormFields(missing)

            // Assert
            assertTrue(fields.isEmpty())
        }

    @Test
    fun fillPdfForm_noAcroForm_returns0() =
        runTest {
            // Arrange
            val original = pdfEntry(twoPagePdf, id = 8L, preview = "no-acro")
            repo.insertEntry(original)

            // Act
            val newId = repo.fillPdfForm(twoPagePdf.absolutePath, original, mapOf("Name" to "Ada"))

            // Assert
            assertEquals(0L, newId)
            assertEquals(listOf(original), repo.getAllClips().first())
        }

    @Test
    fun fillPdfForm_emptyValues_returns0_noDest() =
        runTest {
            // Arrange
            val formFile = acroFormPdf
            assumeTrue(
                "PDFBox AcroForm creation is not feasible under this Robolectric environment",
                formFile != null,
            )
            val original = pdfEntry(formFile!!, id = 10L, preview = "empty-values")
            repo.insertEntry(original)
            val before =
                File(context.filesDir, "pdfs").listFiles().orEmpty().map { it.canonicalPath }.toSet()

            // Act
            val newId = repo.fillPdfForm(formFile.absolutePath, original, emptyMap())

            // Assert
            assertEquals(0L, newId)
            assertEquals(listOf(original), repo.getAllClips().first())
            val after =
                File(context.filesDir, "pdfs").listFiles().orEmpty().map { it.canonicalPath }.toSet()
            assertEquals(before, after)
        }

    @Test
    fun pdfPageCount_missingFile_0() {
        assertEquals(0, pdfPageCount("/no/such/file.pdf"))
    }

    @Test
    fun renderPdfPage_missing_null() {
        assertNull(renderPdfPage("/no/such/file.pdf", 64))
    }

    @Test
    fun pdfPageHeightPt_missing_0f() {
        assertEquals(0f, pdfPageHeightPt("/no/such/file.pdf"), 0f)
    }

    @Test
    fun renderPdfPage_pageIndexTooLarge_clampsToLastPage() {
        // PdfRenderer is native; Robolectric typically returns null/0 instead of rendering.
        val bitmap = renderPdfPage(twoPagePdf.absolutePath, 128, pageIndex = 99)
        val rendererCount = pdfPageCount(twoPagePdf.absolutePath)
        if (rendererCount == 0) {
            assertNull(bitmap)
            assertEquals(0f, pdfPageHeightPt(twoPagePdf.absolutePath, 99), 0f)
        } else {
            assertNotNull(bitmap)
            val last = renderPdfPage(twoPagePdf.absolutePath, 128, pageIndex = rendererCount - 1)
            assertNotNull(last)
            assertEquals(last!!.width, bitmap!!.width)
            assertEquals(last.height, bitmap.height)
        }
    }

    @Test
    fun mergePdfs_oneFile_createsPdf() =
        runTest {
            // Arrange
            val source = writeBlankPdf("merge-one.pdf", pageCount = 1)
            val original = pdfEntry(source, id = 9L, preview = "merge-one")
            repo.insertEntry(original)

            // Act
            val result = repo.mergePdfs(listOf(original))
            val clips = repo.getAllClips().first()

            // Assert
            assertEquals(original, clips.single { it.id == original.id })
            val created = clips.single { it.id == result.successId() }
            assertEquals(ContentType.PDF, created.contentType)
            assertTrue(File(created.filePath!!).exists())
            assertEquals(1, pdfBoxPageCount(created.filePath!!))
        }

    @Test
    fun mergePdfs_twoTextPdfs_preservesPageCountAndText() =
        runTest {
            // Arrange
            val first = writeTextPdf("merge-a.pdf", listOf("HelloA"))
            val second = writeTextPdf("merge-b.pdf", listOf("HelloB"))
            val entryA = pdfEntry(first, id = 11L, preview = "merge-a")
            val entryB = pdfEntry(second, id = 12L, preview = "merge-b")
            repo.insertEntry(entryA)
            repo.insertEntry(entryB)

            // Act
            val result = repo.mergePdfs(listOf(entryA, entryB))
            val created = repo.getAllClips().first().single { it.id == result.successId() }

            // Assert
            assertEquals(2, pdfBoxPageCount(created.filePath!!))
            val extracted =
                PDDocument.load(File(created.filePath!!)).use { document ->
                    PDFTextStripper().getText(document)
                }
            assertTrue(extracted.contains("HelloA"))
            assertTrue(extracted.contains("HelloB"))
        }

    @Test
    fun mergePdfs_missingFile_nothingReadable_noInsert() =
        runTest {
            // Arrange
            val missing = File(File(context.filesDir, "pdfs"), "gone.pdf")
            val original = pdfEntry(missing, id = 13L, preview = "gone")
            repo.insertEntry(original)
            val before =
                File(context.filesDir, "pdfs").listFiles().orEmpty().map { it.canonicalPath }.toSet()

            // Act
            val result = repo.mergePdfs(listOf(original))

            // Assert
            assertEquals(PdfCombineResult.NothingReadable, result)
            val after =
                File(context.filesDir, "pdfs").listFiles().orEmpty().map { it.canonicalPath }.toSet()
            assertEquals(before, after)
        }

    @Test
    fun mergePdfs_embeddedFont_keepsTextAndStaysCompact() =
        runTest {
            // Arrange
            val first = writeEmbeddedFontPdf("merge-font-a.pdf", "FontA")
            val second = writeEmbeddedFontPdf("merge-font-b.pdf", "FontB")
            val entryA = pdfEntry(first, id = 19L, preview = "merge-font-a")
            val entryB = pdfEntry(second, id = 20L, preview = "merge-font-b")
            repo.insertEntry(entryA)
            repo.insertEntry(entryB)

            // Act
            val result = repo.mergePdfs(listOf(entryA, entryB))
            val created = repo.getAllClips().first().single { it.id == result.successId() }
            val merged = File(created.filePath!!)

            // Assert
            assertEquals(2, pdfBoxPageCount(merged.absolutePath))
            val extracted =
                PDDocument.load(merged).use { document ->
                    PDFTextStripper().getText(document)
                }
            assertTrue(extracted.contains("FontA"))
            assertTrue(extracted.contains("FontB"))
            // A raster page at 2400 px is megabytes. Cloning the pages stays near the inputs.
            assertTrue(merged.length() < 1024 * 1024)
        }

    @Test
    fun mergePdfs_encryptedPdf_stripsSecurityAndMerges() =
        runTest {
            // Arrange
            val first = writeEncryptedPdf("merge-encrypted.pdf", "SecretPage")
            val second = writeTextPdf("merge-plain.pdf", listOf("PlainPage"))
            val encrypted = pdfEntry(first, id = 14L, preview = "merge-encrypted")
            val plain = pdfEntry(second, id = 15L, preview = "merge-plain")
            repo.insertEntry(encrypted)
            repo.insertEntry(plain)

            // Act
            val result = repo.mergePdfs(listOf(encrypted, plain))

            // Assert
            val created = repo.getAllClips().first().single { it.id == result.successId() }
            assertEquals(2, pdfBoxPageCount(created.filePath!!))
            PDDocument.load(File(created.filePath!!)).use { document ->
                assertFalse(document.isEncrypted)
            }
        }

    @Test
    fun mergePdfs_invalidPdf_nothingReadable() =
        runTest {
            // Arrange
            val dir = File(context.filesDir, "pdfs").apply { mkdirs() }
            val broken = File(dir, "broken.pdf")
            broken.writeBytes(byteArrayOf(0x25, 0x50, 0x44, 0x46))
            val original = pdfEntry(broken, id = 16L, preview = "broken")
            repo.insertEntry(original)

            // Act
            val result = repo.mergePdfs(listOf(original))

            // Assert
            assertEquals(PdfCombineResult.NothingReadable, result)
        }

    @Test
    fun renderEntriesToRasterPdf_whenRendererAvailable_producesPages() {
        // Arrange
        val first = writeTextPdf("raster-a.pdf", listOf("RasterA"))
        val second = writeTextPdf("raster-b.pdf", listOf("RasterB"))
        val entryA = pdfEntry(first, id = 17L, preview = "raster-a")
        val entryB = pdfEntry(second, id = 18L, preview = "raster-b")

        // Act
        val bytes = repo.renderEntriesToRasterPdf(listOf(entryA, entryB))

        // Assert
        // PdfRenderer is native; Robolectric may report 0 pages, in which case the fallback yields null.
        if (pdfPageCount(first.absolutePath) == 0) {
            assertNull(bytes)
        } else {
            assertNotNull(bytes)
            val file = File(context.filesDir, "pdfs/raster-result.pdf")
            file.writeBytes(bytes!!)
            assertEquals(2, pdfBoxPageCount(file.absolutePath))
        }
    }

    private fun textEntry(
        id: Long,
        text: String = "hello",
        preview: String = text,
        timestamp: Long = 1_700_000_000_000L,
        isPinned: Boolean = false,
        isLocked: Boolean = false,
    ) = ClipboardEntry(
        id = id,
        contentType = ContentType.TEXT,
        textContent = text,
        filePath = null,
        previewText = preview,
        timestamp = timestamp,
        isPinned = isPinned,
        isLocked = isLocked,
    )

    private fun pdfEntry(
        file: File,
        id: Long,
        preview: String = file.nameWithoutExtension,
        timestamp: Long = 1_700_000_000_000L,
    ) = ClipboardEntry(
        id = id,
        contentType = ContentType.PDF,
        textContent = null,
        filePath = file.absolutePath,
        previewText = preview,
        timestamp = timestamp,
    )

    private fun writeTextPdf(
        name: String,
        pageTexts: List<String>,
    ): File {
        val dir = File(context.filesDir, "pdfs").apply { mkdirs() }
        val file = File(dir, name)
        PDDocument().use { document ->
            pageTexts.forEach { text ->
                val page = PDPage(PDRectangle.A4)
                document.addPage(page)
                PDPageContentStream(document, page).use { stream ->
                    stream.beginText()
                    stream.setFont(PDType1Font.HELVETICA, 12f)
                    stream.newLineAtOffset(50f, 750f)
                    stream.showText(text)
                    stream.endText()
                }
            }
            document.save(file)
        }
        return file
    }

    private fun writeEmbeddedFontPdf(
        name: String,
        text: String,
    ): File {
        val dir = File(context.filesDir, "pdfs").apply { mkdirs() }
        val file = File(dir, name)
        PDDocument().use { document ->
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            val font =
                context.assets.open("fonts/LiberationSans-Regular.ttf").use { input ->
                    PDType0Font.load(document, input)
                }
            PDPageContentStream(document, page).use { stream ->
                stream.beginText()
                stream.setFont(font, 12f)
                stream.newLineAtOffset(50f, 750f)
                stream.showText(text)
                stream.endText()
            }
            document.save(file)
        }
        return file
    }

    private fun writeEncryptedPdf(
        name: String,
        text: String,
    ): File {
        val dir = File(context.filesDir, "pdfs").apply { mkdirs() }
        val file = File(dir, name)
        PDDocument().use { document ->
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            PDPageContentStream(document, page).use { stream ->
                stream.beginText()
                stream.setFont(PDType1Font.HELVETICA, 12f)
                stream.newLineAtOffset(50f, 750f)
                stream.showText(text)
                stream.endText()
            }
            val policy = StandardProtectionPolicy("owner-secret", "", AccessPermission())
            policy.encryptionKeyLength = 128
            document.protect(policy)
            document.save(file)
        }
        return file
    }

    private fun writeRotatedPdf(
        name: String,
        rotation: Int,
    ): File {
        val dir = File(context.filesDir, "pdfs").apply { mkdirs() }
        val file = File(dir, name)
        PDDocument().use { document ->
            val page = PDPage(PDRectangle.A4)
            page.rotation = rotation
            document.addPage(page)
            PDPageContentStream(document, page).use { stream ->
                stream.beginText()
                stream.setFont(PDType1Font.HELVETICA, 12f)
                stream.newLineAtOffset(72f, 720f)
                stream.showText("UPRIGHT")
                stream.endText()
            }
            document.save(file)
        }
        return file
    }

    private fun pageText(
        document: PDDocument,
        pageNumber: Int,
    ): String {
        return PDFTextStripper().apply {
            startPage = pageNumber
            endPage = pageNumber
        }.getText(document)
    }

    private fun textPosition(
        document: PDDocument,
        pageNumber: Int,
        word: String,
    ): TextPosition {
        val stripper =
            object : PDFTextStripper() {
                val found = mutableListOf<TextPosition>()

                override fun writeString(
                    text: String?,
                    textPositions: List<TextPosition>?,
                ) {
                    if (text != null && text.contains(word) && textPositions != null) {
                        found.add(textPositions.first())
                    }
                    super.writeString(text, textPositions)
                }
            }
        stripper.startPage = pageNumber
        stripper.endPage = pageNumber
        stripper.getText(document)
        return stripper.found.single()
    }

    private fun writeBlankPdf(
        name: String,
        pageCount: Int,
    ): File {
        val dir = File(context.filesDir, "pdfs").apply { mkdirs() }
        val file = File(dir, name)
        PDDocument().use { document ->
            repeat(pageCount) {
                document.addPage(PDPage(PDRectangle.A4))
            }
            document.save(file)
        }
        return file
    }

    private fun writeTooltipForm(name: String): File? {
        return try {
            val dir = File(context.filesDir, "pdfs").apply { mkdirs() }
            val file = File(dir, name)
            PDDocument().use { document ->
                val page = PDPage(PDRectangle.A4)
                document.addPage(page)
                val acroForm = PDAcroForm(document)
                val resources = PDResources()
                resources.put(COSName.HELV, PDType1Font.HELVETICA)
                acroForm.defaultResources = resources
                acroForm.defaultAppearance = "/Helv 12 Tf 0 g"
                document.documentCatalog.acroForm = acroForm

                val nameField = PDTextField(acroForm)
                nameField.partialName = "name"
                nameField.alternateFieldName = "Full name"
                nameField.defaultAppearance = "/Helv 12 Tf 0 g"
                val nameWidget = nameField.widgets.first()
                nameWidget.rectangle = PDRectangle(100f, 760f, 200f, 18f)
                nameWidget.page = page
                nameWidget.isPrinted = true

                val emailField = PDTextField(acroForm)
                emailField.partialName = "email"
                emailField.defaultAppearance = "/Helv 12 Tf 0 g"
                val emailWidget = emailField.widgets.first()
                emailWidget.rectangle = PDRectangle(100f, 730f, 200f, 18f)
                emailWidget.page = page
                emailWidget.isPrinted = true

                val phoneField = PDCheckBox(acroForm)
                phoneField.partialName = "s0"
                val phoneWidget = phoneField.widgets.first()
                phoneWidget.rectangle = PDRectangle(100f, 700f, 18f, 18f)
                phoneWidget.page = page
                phoneWidget.isPrinted = true
                phoneWidget.appearance = checkboxAppearances(document)

                acroForm.fields = listOf(nameField, emailField, phoneField)
                page.annotations = listOf(nameWidget, emailWidget, phoneWidget)
                // Checkbox labels (#57) come from the text to the RIGHT of the widget.
                PDPageContentStream(document, page).use { stream ->
                    stream.beginText()
                    stream.setFont(PDType1Font.HELVETICA, 12f)
                    stream.newLineAtOffset(128f, 704f)
                    stream.showText("Phone")
                    stream.endText()
                }
                document.save(file)
            }
            file
        } catch (_: Exception) {
            null
        }
    }

    private fun writeAcroFormPdf(name: String): File? {
        return try {
            val dir = File(context.filesDir, "pdfs").apply { mkdirs() }
            val file = File(dir, name)
            PDDocument().use { document ->
                val page = PDPage(PDRectangle.A4)
                document.addPage(page)

                val acroForm = PDAcroForm(document)
                val resources = PDResources()
                resources.put(COSName.HELV, PDType1Font.HELVETICA)
                acroForm.defaultResources = resources
                acroForm.defaultAppearance = "/Helv 12 Tf 0 g"
                document.documentCatalog.acroForm = acroForm

                val textField = PDTextField(acroForm)
                textField.partialName = "Name"
                textField.defaultAppearance = "/Helv 12 Tf 0 g"
                val textWidget = textField.widgets.first()
                textWidget.rectangle = PDRectangle(100f, 700f, 200f, 20f)
                textWidget.page = page
                textWidget.isPrinted = true

                val checkBox = PDCheckBox(acroForm)
                checkBox.partialName = "Agree"
                val checkWidget = checkBox.widgets.first()
                checkWidget.rectangle = PDRectangle(100f, 660f, 20f, 20f)
                checkWidget.page = page
                checkWidget.isPrinted = true
                checkWidget.appearance = checkboxAppearances(document)

                acroForm.fields = listOf(textField, checkBox)
                page.annotations = listOf<PDAnnotation>(textWidget, checkWidget)
                document.save(file)
            }
            val loaded =
                PDDocument.load(file).use { document ->
                    document.documentCatalog.acroForm?.fields.orEmpty().mapNotNull { it.partialName }
                }
            if (loaded.containsAll(listOf("Name", "Agree"))) file else null
        } catch (_: Exception) {
            null
        }
    }

    private fun checkboxAppearances(document: PDDocument): PDAppearanceDictionary {
        val onStream = PDAppearanceStream(document).apply { bBox = PDRectangle(20f, 20f) }
        val offStream = PDAppearanceStream(document).apply { bBox = PDRectangle(20f, 20f) }
        val states = COSDictionary()
        states.setItem(COSName.YES, onStream)
        states.setItem(COSName.Off, offStream)
        return PDAppearanceDictionary().apply {
            setNormalAppearance(PDAppearanceEntry(states))
        }
    }

    private fun pdfBoxPageCount(path: String): Int = PDDocument.load(File(path)).use { it.numberOfPages }

    private fun PdfCombineResult.successId(): Long {
        assertTrue("Expected Success but was $this", this is PdfCombineResult.Success)
        return (this as PdfCombineResult.Success).id
    }
}
