package com.phaprodevelop.klipsilo.data.repository

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class ExtractOfficeTextTest {
    private lateinit var tempDir: File
    private lateinit var helloDocx: File
    private lateinit var helloXlsx: File

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "klipsilo-office-${System.nanoTime()}")
        assertTrue(tempDir.mkdirs())

        helloDocx = File(tempDir, "hello.docx")
        writeZip(
            helloDocx,
            "[Content_Types].xml" to DOCX_CONTENT_TYPES,
            "word/document.xml" to DOCX_DOCUMENT_XML,
        )

        helloXlsx = File(tempDir, "hello.xlsx")
        writeZip(
            helloXlsx,
            "xl/sharedStrings.xml" to XLSX_SHARED_STRINGS_XML,
            "xl/worksheets/sheet1.xml" to XLSX_SHEET1_XML,
        )
    }

    @After
    fun tearDown() {
        if (::tempDir.isInitialized) {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun extractRtfText_minimalHello_containsHello() {
        // Arrange: classpath fixture copied to a temp path extractRtfText can open
        val file = File(tempDir, "hello.rtf")
        javaClass.getResourceAsStream("/office/hello.rtf").use { stream ->
            assertNotNull(stream)
            file.outputStream().use { out -> stream!!.copyTo(out) }
        }

        // Act
        val result = extractRtfText(file.absolutePath)

        // Assert
        assertNotNull(result)
        assertTrue(result!!.contains("Hello"))
    }

    @Test
    fun extractRtfText_boldControlWords_plainBold() {
        // Arrange
        val file = writeRtf("{\\rtf1\\ansi \\b Bold\\b0}")

        // Act
        val result = extractRtfText(file.absolutePath)

        // Assert
        assertEquals("Bold", result)
        assertFalse(result!!.contains("\\b"))
        assertFalse(result.contains("rtf1"))
        assertFalse(result.contains("ansi"))
    }

    @Test
    fun extractRtfText_parBecomesNewline() {
        // Arrange
        val file = writeRtf("{\\rtf1\\ansi First\\par Second}")

        // Act
        val result = extractRtfText(file.absolutePath)

        // Assert: rtfToHtml emits </p><p>; htmlToPlainText maps </p> to newline
        assertEquals("First\nSecond", result)
    }

    @Test
    fun extractRtfText_missingFile_returnsNull() {
        // Arrange
        val missing = File(tempDir, "does-not-exist.rtf")

        // Act
        val result = extractRtfText(missing.absolutePath)

        // Assert
        assertFalse(missing.exists())
        assertNull(result)
    }

    @Test
    fun extractDocxText_helloFixture_containsHelloKlipSilo() {
        // Act
        val result = extractDocxText(helloDocx.absolutePath)

        // Assert
        assertNotNull(result)
        assertTrue(result!!.contains("Hello KlipSilo"))
    }

    @Test
    fun extractDocxText_corruptZip_returnsNull() {
        // Arrange: PK local-file signature then truncated payload
        val file = File(tempDir, "corrupt.docx")
        file.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x01))

        // Act
        val result = extractDocxText(file.absolutePath)

        // Assert
        assertNull(result)
    }

    @Test
    fun extractDocxText_notAZip_returnsNull() {
        // Arrange
        val file = File(tempDir, "plain.docx")
        file.writeText("this is not a zip")

        // Act
        val result = extractDocxText(file.absolutePath)

        // Assert
        assertNull(result)
    }

    @Test
    fun extractXlsxText_helloFixture_containsHelloAndWorld() {
        // Act
        val result = extractXlsxText(helloXlsx.absolutePath)

        // Assert
        assertNotNull(result)
        assertTrue(result!!.contains("Hello"))
        assertTrue(result.contains("World"))
    }

    @Test
    fun extractXlsxText_corruptZip_returnsNull() {
        // Arrange
        val file = File(tempDir, "corrupt.xlsx")
        file.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x01))

        // Act
        val result = extractXlsxText(file.absolutePath)

        // Assert
        assertNull(result)
    }

    private fun writeRtf(content: String): File {
        val file = File(tempDir, "sample-${System.nanoTime()}.rtf")
        file.writeText(content)
        return file
    }

    private fun writeZip(
        file: File,
        vararg entries: Pair<String, String>,
    ) {
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    private companion object {
        // Parser only requires word/document.xml; [Content_Types].xml is the usual OOXML sibling.
        private val DOCX_CONTENT_TYPES =
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/word/document.xml"
                ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
            """.trimIndent()

        private val DOCX_DOCUMENT_XML =
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p>
                  <w:r>
                    <w:t>Hello KlipSilo</w:t>
                  </w:r>
                </w:p>
              </w:body>
            </w:document>
            """.trimIndent()

        private val XLSX_SHARED_STRINGS_XML =
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="2" uniqueCount="2">
              <si><t>Hello</t></si>
              <si><t>World</t></si>
            </sst>
            """.trimIndent()

        private val XLSX_SHEET1_XML =
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>
                <row r="1">
                  <c r="A1" t="s"><v>0</v></c>
                  <c r="B1" t="s"><v>1</v></c>
                </row>
              </sheetData>
            </worksheet>
            """.trimIndent()
    }
}
