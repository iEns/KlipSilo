package com.phaprodevelop.klipsilo.data.repository

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.location.Geocoder
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Patterns
import androidx.core.content.FileProvider
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.phaprodevelop.klipsilo.LocaleHelper
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.FormKeyboardKind
import com.phaprodevelop.klipsilo.data.MARKDOWN_PREVIEW_FONT_SCALE
import com.phaprodevelop.klipsilo.data.MARKDOWN_PREVIEW_MARGIN_PT
import com.phaprodevelop.klipsilo.data.PdfTextRun
import com.phaprodevelop.klipsilo.data.PdfUserRect
import com.phaprodevelop.klipsilo.data.ResolvedFieldLabel
import com.phaprodevelop.klipsilo.data.db.ClipboardItem
import com.phaprodevelop.klipsilo.data.db.ClipboardItemDao
import com.phaprodevelop.klipsilo.data.db.ClipboardItemListRow
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.formKeyboardKind
import com.phaprodevelop.klipsilo.data.markdownHeadingFactor
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.nearbyFieldText
import com.phaprodevelop.klipsilo.data.nearbyFieldTextRight
import com.phaprodevelop.klipsilo.data.parseTextFieldFlags
import com.phaprodevelop.klipsilo.data.refreshedTextClipTitle
import com.phaprodevelop.klipsilo.data.resolveFormFieldLabel
import com.phaprodevelop.klipsilo.data.resolveWidgetLabel
import com.phaprodevelop.klipsilo.data.settings.PdfFont
import com.phaprodevelop.klipsilo.data.settings.PdfSettingsStore
import com.phaprodevelop.klipsilo.data.widgetVisualFractions
import com.phaprodevelop.klipsilo.ui.components.pdfPageCount
import com.phaprodevelop.klipsilo.ui.components.renderPdfPage
import com.phaprodevelop.klipsilo.withoutSoftHyphens
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDButton
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDCheckBox
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDComboBox
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDListBox
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDNonTerminalField
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDRadioButton
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Outcome of combining clips into a new PDF clip. */
sealed interface PdfCombineResult {
    /** The combined PDF was saved and inserted as a new clip. */
    data class Success(
        val id: Long,
    ) : PdfCombineResult

    /** No source pages could be read (missing, encrypted-unreadable or corrupt files). */
    data object NothingReadable : PdfCombineResult

    /** Source pages were read but the new PDF could not be saved. */
    data object SaveFailed : PdfCombineResult
}

class ClipboardRepository(
    private val dao: ClipboardItemDao,
    private val context: Context,
) {
    companion object {
        const val MAX_PREVIEW_LENGTH = 100
        const val MAX_IMPORT_BYTES = 100L * 1024L * 1024L
        const val MAX_RESTORE_TOTAL_BYTES = 500L * 1024L * 1024L
        const val MAX_IMAGE_DECODE_PX = 4096
        const val INLINE_TEXT_MAX_BYTES = 256 * 1024

        private const val TITLE_RECOMPUTE_PREFS = "data_migration_prefs"
        private const val KEY_TEXT_TITLE_RECOMPUTE_DONE = "text_titles_recomputed"
    }

    private val restoreMutex = Mutex()

    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    private val imagesDir: File
        get() = File(context.filesDir, "images").also { it.mkdirs() }

    private val pdfsDir: File
        get() = File(context.filesDir, "pdfs").also { it.mkdirs() }

    private val filesDir: File
        get() = File(context.filesDir, "files").also { it.mkdirs() }

    fun getAllClips(): Flow<List<ClipboardEntry>> {
        return dao.getAll().map { rows ->
            rows.map { it.toEntry() }
        }
    }

    /** One-shot list read. Used to refresh the UI when a Room flow emission is missed or stale. */
    suspend fun listClips(): List<ClipboardEntry> = dao.getAllListOnce().map { it.toEntry() }

    suspend fun getClip(id: Long): ClipboardEntry? = dao.getById(id)?.toEntry()

    /**
     * One-time migration for text clips saved by older builds: their stored
     * preview can be a stale title derived with the old title rule (or the
     * raw-body truncation) that the current title logic no longer produces.
     * Recompute those titles from the stored body once, leaving previews that
     * may be user renames untouched. Guarded by a stored flag so it only runs
     * per install.
     */
    suspend fun recomputeStaleTextTitles() {
        try {
            val prefs =
                context.getSharedPreferences(TITLE_RECOMPUTE_PREFS, Context.MODE_PRIVATE)
            if (prefs.getBoolean(KEY_TEXT_TITLE_RECOMPUTE_DONE, false)) return
            dao.getAllOnce().forEach { item ->
                if (item.contentType != ContentType.TEXT) return@forEach
                val body = item.textContent ?: item.filePath?.let { readCappedUtf8File(it) }
                val fresh = body?.let { refreshedTextClipTitle(item.previewText, it) }
                fresh?.let { dao.updatePreviewText(item.id, it) }
            }
            prefs.edit().putBoolean(KEY_TEXT_TITLE_RECOMPUTE_DONE, true).apply()
        } catch (_: Exception) {
        }
    }

    suspend fun readTextBody(entry: ClipboardEntry): String? {
        val full = dao.getById(entry.id)
        if (full != null) {
            full.textContent?.let { return it }
            return full.filePath?.let { readCappedUtf8File(it) }
        }
        entry.textContent?.let { return it }
        return entry.filePath?.let { readCappedUtf8File(it) }
    }

    suspend fun migrateLegacyNames() {
        try {
            val items = dao.getAllOnce()
            items.forEach { item ->
                val path = item.filePath ?: return@forEach
                val file = File(path)
                if (!file.exists()) {
                    return@forEach
                }
                val parent = file.parentFile ?: return@forEach
                val name = file.name
                val dot = name.lastIndexOf('.')
                val base = if (dot > 0) name.substring(0, dot) else name
                val ext = if (dot > 0) name.substring(dot) else ""
                val cleanedBase = base.replace(Regex("_\\d{8,}$"), "")
                var finalPath = path
                var renamed = false
                var renamedFrom: File? = null
                var renamedTo: File? = null
                if (cleanedBase != base) {
                    val desired = File(parent, "$cleanedBase$ext")
                    if (desired.absolutePath != file.absolutePath) {
                        val target = takeUniqueRenameTarget(desired)
                        if (target != null && file.renameTo(target)) {
                            finalPath = target.absolutePath
                            renamed = true
                            renamedFrom = file
                            renamedTo = target
                        }
                    }
                }
                val newPreview = extractBaseName(File(finalPath).name)
                if (renamed || item.previewText != newPreview) {
                    try {
                        dao.update(
                            item.copy(
                                filePath = if (renamed) finalPath else item.filePath,
                                previewText = newPreview,
                            ),
                        )
                    } catch (_: Exception) {
                        if (renamedFrom != null && renamedTo != null) {
                            renamedTo.renameTo(renamedFrom)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    suspend fun saveNewClip(
        text: String?,
        imageUri: Uri?,
        pdfUri: Uri?,
        fileUri: Uri? = null,
    ): SaveClipResult {
        val contentType: ContentType
        var textContent: String? = text
        var filePath: String? = null
        var previewText: String

        when {
            imageUri != null -> {
                contentType = ContentType.IMAGE
                val originalName =
                    resolveDisplayName(imageUri) ?: imageUri.lastPathSegment?.let { Uri.decode(it) } ?: ""
                when (val copied = copyFileToStorage(imageUri, imagesDir, "img_")) {
                    is CopyResult.TooLarge -> return SaveClipResult.TooLarge
                    is CopyResult.Failed -> return SaveClipResult.Failed
                    is CopyResult.Ok -> {
                        filePath = copied.path
                        previewText =
                            extractBaseName(originalName).ifBlank { extractBaseName(File(copied.path).name) }
                    }
                }
            }
            pdfUri != null -> {
                contentType = ContentType.PDF
                val originalName =
                    resolveDisplayName(pdfUri) ?: pdfUri.lastPathSegment?.let { Uri.decode(it) } ?: ""
                when (val copied = copyFileToStorage(pdfUri, pdfsDir, "pdf_")) {
                    is CopyResult.TooLarge -> return SaveClipResult.TooLarge
                    is CopyResult.Failed -> return SaveClipResult.Failed
                    is CopyResult.Ok -> {
                        filePath = copied.path
                        previewText =
                            extractBaseName(originalName).ifBlank { extractBaseName(File(copied.path).name) }
                    }
                }
            }
            fileUri != null -> {
                contentType = ContentType.FILE
                val originalName =
                    resolveDisplayName(fileUri) ?: fileUri.lastPathSegment?.let { Uri.decode(it) } ?: ""
                when (val copied = copyFileRawToStorage(fileUri, filesDir, "file_")) {
                    is CopyResult.TooLarge -> return SaveClipResult.TooLarge
                    is CopyResult.Failed -> return SaveClipResult.Failed
                    is CopyResult.Ok -> {
                        filePath = copied.path
                        previewText =
                            extractBaseName(originalName).ifBlank { extractBaseName(File(copied.path).name) }
                    }
                }
            }
            text != null && Patterns.WEB_URL.matcher(text).matches() -> {
                contentType = ContentType.URL
                previewText = text
                when (val persisted = persistTextBody(text)) {
                    PersistTextResult.TooLarge -> return SaveClipResult.TooLarge
                    PersistTextResult.Failed -> return SaveClipResult.Failed
                    is PersistTextResult.Ok -> {
                        textContent = persisted.textContent
                        filePath = persisted.filePath
                    }
                }
            }
            text != null -> {
                contentType = ContentType.TEXT
                previewText = com.phaprodevelop.klipsilo.data.textClipTitle(text)
                when (val persisted = persistTextBody(text)) {
                    PersistTextResult.TooLarge -> return SaveClipResult.TooLarge
                    PersistTextResult.Failed -> return SaveClipResult.Failed
                    is PersistTextResult.Ok -> {
                        textContent = persisted.textContent
                        filePath = persisted.filePath
                    }
                }
            }
            else -> return SaveClipResult.Failed
        }

        val id = System.nanoTime()
        val item =
            ClipboardItem(
                id = id,
                contentType = contentType,
                textContent = textContent,
                filePath = filePath,
                previewText = previewText,
                isPinned = false,
            )

        return try {
            dao.insert(item)
            SaveClipResult.Saved
        } catch (_: Exception) {
            filePath?.let { File(it).delete() }
            SaveClipResult.Failed
        }
    }

    suspend fun downloadUrlToClip(url: String): SaveClipResult {
        return try {
            val trimmed = url.trim()
            val uri = Uri.parse(trimmed)
            if (!uri.scheme.equals("https", ignoreCase = true)) return SaveClipResult.Failed
            if (uri.host.isNullOrBlank()) return SaveClipResult.Failed

            val connection =
                (URL(trimmed).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "KlipSilo/1.0 (Android)")
                    setRequestProperty("Accept", "*/*")
                }
            try {
                val code = connection.responseCode
                if (code !in 200..299) return SaveClipResult.Failed
                if (connection.contentLengthLong > MAX_IMPORT_BYTES) return SaveClipResult.TooLarge

                val contentType =
                    connection.contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
                val dispositionName = parseContentDisposition(connection.getHeaderField("Content-Disposition"))
                val urlSegment = uri.lastPathSegment?.let { Uri.decode(it) } ?: ""
                val nameHint = dispositionName.ifBlank { urlSegment }
                val isHtml = contentType == "text/html" || contentType == "application/xhtml+xml"
                if (isHtml && dispositionName.isBlank() && !hasPlausibleFileExtension(urlSegment)) {
                    return SaveClipResult.Failed
                }

                val input = BufferedInputStream(connection.inputStream)
                input.use { stream ->
                    val header = readHeader(stream)
                    if (header.bytesRead == 0) return SaveClipResult.Failed
                    val detected = detectExtension(header)
                    val isPdf = contentType == "application/pdf" || detected == ".pdf"
                    val isImage =
                        contentType.startsWith("image/") ||
                            (detected != null && detected in setOf(".jpg", ".png", ".gif", ".webp", ".bmp", ".heic"))
                    val isPlainText = contentType.startsWith("text/plain")

                    when {
                        isPdf -> {
                            val ext = detected ?: ".pdf"
                            val name = buildFileName(nameHint.ifBlank { "download.pdf" }, "pdf_", ext)
                            val dest = ensureUniqueFile(File(pdfsDir, name)) ?: return SaveClipResult.Failed
                            when (val w = writeStreamLimited(stream, dest, header)) {
                                is CopyResult.TooLarge -> return SaveClipResult.TooLarge
                                is CopyResult.Failed -> return SaveClipResult.Failed
                                is CopyResult.Ok ->
                                    insertDownloadedClip(ContentType.PDF, null, w.path, File(w.path).name)
                            }
                        }
                        isImage -> {
                            val name = buildFileName(nameHint.ifBlank { "download.jpg" }, "img_", detected)
                            val dest = ensureUniqueFile(File(imagesDir, name)) ?: return SaveClipResult.Failed
                            when (val w = writeStreamLimited(stream, dest, header)) {
                                is CopyResult.TooLarge -> return SaveClipResult.TooLarge
                                is CopyResult.Failed -> return SaveClipResult.Failed
                                is CopyResult.Ok ->
                                    insertDownloadedClip(ContentType.IMAGE, null, w.path, File(w.path).name)
                            }
                        }
                        isPlainText -> {
                            val bytes = readLimitedBytes(stream, MAX_IMPORT_BYTES) ?: return SaveClipResult.TooLarge
                            val text = bytes.toString(Charsets.UTF_8)
                            if (text.isBlank()) return SaveClipResult.Failed
                            val preview =
                                if (text.length > MAX_PREVIEW_LENGTH) {
                                    text.take(MAX_PREVIEW_LENGTH) + "..."
                                } else {
                                    text
                                }
                            when (val persisted = persistTextBody(text)) {
                                PersistTextResult.TooLarge -> return SaveClipResult.TooLarge
                                PersistTextResult.Failed -> return SaveClipResult.Failed
                                is PersistTextResult.Ok ->
                                    insertDownloadedClip(
                                        ContentType.TEXT,
                                        persisted.textContent,
                                        persisted.filePath,
                                        preview,
                                    )
                            }
                        }
                        else -> {
                            val ext = resolveDownloadExtension(nameHint, contentType, detected, "bin")
                            val stem = extractBaseName(nameHint).ifBlank { "download" }
                            val fileName = sanitizeFileName("$stem.$ext").ifBlank { "download.$ext" }
                            val dest = ensureUniqueFile(File(filesDir, fileName)) ?: return SaveClipResult.Failed
                            when (val w = writeStreamLimited(stream, dest, header)) {
                                is CopyResult.TooLarge -> return SaveClipResult.TooLarge
                                is CopyResult.Failed -> return SaveClipResult.Failed
                                is CopyResult.Ok ->
                                    insertDownloadedClip(ContentType.FILE, null, w.path, File(w.path).name)
                            }
                        }
                    }
                    SaveClipResult.Saved
                }
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            SaveClipResult.Failed
        }
    }

    private suspend fun insertDownloadedClip(
        contentType: ContentType,
        textContent: String?,
        filePath: String?,
        preview: String,
    ) {
        val item =
            ClipboardItem(
                id = System.nanoTime(),
                contentType = contentType,
                textContent = textContent,
                filePath = filePath,
                previewText = preview.ifBlank { localizedString(R.string.untitled_clip) },
                isPinned = false,
            )
        dao.insert(item)
    }

    suspend fun updateClip(entry: ClipboardEntry) {
        val item =
            ClipboardItem(
                id = entry.id,
                contentType = entry.contentType,
                textContent = entry.textContent,
                filePath = entry.filePath,
                previewText = entry.previewText,
                timestamp = entry.timestamp,
                isPinned = entry.isPinned,
                isLocked = entry.isLocked,
                latitude = entry.latitude,
                longitude = entry.longitude,
            )
        dao.update(item)
    }

    suspend fun setLocked(
        entry: ClipboardEntry,
        locked: Boolean,
    ) {
        dao.updateLocked(entry.id, locked)
    }

    suspend fun renameFileClip(
        entry: ClipboardEntry,
        newName: String,
    ): Boolean {
        return try {
            val oldPath = entry.filePath
            if (oldPath == null) return false
            val cleanName = sanitizeFileName(newName)
            if (cleanName.isBlank()) return false
            val oldFile = File(oldPath)
            val ext = oldFile.extension
            val base = extractBaseName(cleanName)
            val finalName = if (ext.isNotBlank()) "$base.$ext" else base
            val desired = File(oldFile.parentFile ?: return false, finalName)
            if (desired.absolutePath == oldFile.absolutePath) return true
            val target = takeUniqueRenameTarget(desired) ?: return false
            if (!oldFile.renameTo(target)) return false
            try {
                dao.updateFilePathAndPreviewText(entry.id, target.absolutePath, extractBaseName(target.name))
                true
            } catch (_: Exception) {
                target.renameTo(oldFile)
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun renameClip(
        entry: ClipboardEntry,
        newName: String,
    ): Boolean {
        return try {
            when (entry.contentType) {
                ContentType.IMAGE, ContentType.PDF, ContentType.FILE -> renameFileClip(entry, newName)
                else -> {
                    val cleanName = newName.replace('\n', ' ').replace('\r', ' ').trim()
                    if (cleanName.isBlank()) return false
                    dao.updatePreviewText(entry.id, cleanName)
                    true
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteClip(entry: ClipboardEntry) {
        entry.filePath?.let { path ->
            File(path).delete()
        }
        dao.deleteById(entry.id)
    }

    suspend fun deleteClipLazy(entry: ClipboardEntry) {
        dao.deleteById(entry.id)
    }

    suspend fun deleteClipFile(entry: ClipboardEntry) {
        entry.filePath?.let { path ->
            File(path).delete()
        }
    }

    suspend fun undeleteClip(entry: ClipboardEntry) {
        val item =
            ClipboardItem(
                id = entry.id,
                contentType = entry.contentType,
                textContent = entry.textContent,
                filePath = entry.filePath,
                previewText = entry.previewText,
                timestamp = entry.timestamp,
                isPinned = entry.isPinned,
                isLocked = entry.isLocked,
                latitude = entry.latitude,
                longitude = entry.longitude,
            )
        dao.insert(item)
    }

    suspend fun deleteClipsLazy(entries: List<ClipboardEntry>) {
        entries.forEach { dao.deleteById(it.id) }
    }

    suspend fun deleteClipsFiles(entries: List<ClipboardEntry>) {
        entries.forEach { entry ->
            entry.filePath?.let { path ->
                File(path).delete()
            }
        }
    }

    suspend fun undeleteClips(entries: List<ClipboardEntry>) {
        entries.forEach { undeleteClip(it) }
    }

    suspend fun deleteAllClips() {
        val items = dao.getAllOnce()
        items.filter { !it.isLocked }.forEach { item ->
            item.filePath?.let { path ->
                File(path).delete()
            }
            dao.deleteById(item.id)
        }
        try {
            val keep =
                dao.getAllOnce()
                    .mapNotNull { it.filePath }
                    .map { File(it).canonicalPath }
                    .toSet()
            listOf("images", "pdfs", "files").forEach { name ->
                val dir = File(context.filesDir, name)
                dir.mkdirs()
                dir.listFiles()?.forEach { file ->
                    if (file.canonicalPath !in keep) {
                        file.delete()
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    suspend fun insertEntry(entry: ClipboardEntry) {
        val item = persistTextualEntry(entry) ?: return
        try {
            dao.insert(item)
        } catch (_: Exception) {
            if (entry.filePath == null) {
                item.filePath?.let { File(it).delete() }
            }
        }
    }

    suspend fun duplicateEntry(entry: ClipboardEntry): Long {
        val source = dao.getById(entry.id)?.toEntry() ?: entry
        val id = System.nanoTime()
        val sourcePath = source.filePath
        val newFilePath =
            if (!sourcePath.isNullOrBlank()) {
                val destDir =
                    when (source.contentType) {
                        ContentType.IMAGE -> imagesDir
                        ContentType.PDF -> pdfsDir
                        else -> filesDir
                    }
                duplicateFilePath(sourcePath, destDir) ?: return 0L
            } else {
                null
            }
        val item =
            ClipboardItem(
                id = id,
                contentType = source.contentType,
                textContent =
                    if (newFilePath != null &&
                        (source.contentType == ContentType.TEXT || source.contentType == ContentType.URL)
                    ) {
                        null
                    } else {
                        source.textContent
                    },
                filePath = newFilePath,
                previewText = source.previewText,
                timestamp = System.currentTimeMillis(),
                isPinned = false,
                latitude = source.latitude,
                longitude = source.longitude,
            )
        return try {
            dao.insert(item)
            id
        } catch (_: Exception) {
            newFilePath?.let { File(it).delete() }
            0L
        }
    }

    private fun duplicateFilePath(
        sourcePath: String,
        destDir: File,
    ): String? {
        val src = File(sourcePath)
        if (!src.exists()) return null
        val dest = ensureUniqueFile(File(destDir, src.name)) ?: return null
        return try {
            FileInputStream(src).use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            dest.absolutePath
        } catch (_: Exception) {
            dest.delete()
            null
        }
    }

    suspend fun togglePin(entry: ClipboardEntry) {
        dao.updatePinned(entry.id, !entry.isPinned)
    }

    suspend fun savePageAsPdf(
        pdfPath: String,
        pageIndex: Int,
        originalEntry: ClipboardEntry,
    ): Long {
        return try {
            val bitmap = renderPdfPage(pdfPath, OCR_RENDER_DIM, pageIndex) ?: return 0L
            val id = savePdfFromBitmap(bitmap, originalEntry)
            bitmap.recycle()
            id
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun savePdfPages(
        pdfPath: String,
        originalEntry: ClipboardEntry,
        pageOrder: List<Int>,
    ): Long {
        return try {
            if (pageOrder.isEmpty()) return 0L
            val file = File(pdfPath)
            if (!file.exists()) return 0L
            val source = PDDocument.load(file)
            val result = PDDocument()
            try {
                val pageCount = source.numberOfPages
                for (index in pageOrder) {
                    if (index < 0 || index >= pageCount) continue
                    result.importPage(source.getPage(index))
                }
                if (result.numberOfPages == 0) return 0L
                val pdfName = buildPdfName(originalEntry.previewText)
                val pdfFile = ensureUniqueFile(File(pdfsDir, pdfName)) ?: return 0L
                try {
                    FileOutputStream(pdfFile).use { result.save(it) }

                    val id = System.nanoTime()
                    val item =
                        ClipboardItem(
                            id = id,
                            contentType = ContentType.PDF,
                            filePath = pdfFile.absolutePath,
                            previewText = extractBaseName(pdfName),
                            isPinned = false,
                        )
                    dao.insert(item)
                    id
                } catch (e: Exception) {
                    pdfFile.delete()
                    throw e
                }
            } finally {
                result.close()
                source.close()
            }
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun savePdfWithAnnotations(
        pdfPath: String,
        annotations: List<PdfAnnotation>,
        strokes: List<PdfStroke> = emptyList(),
        originalEntry: ClipboardEntry,
    ): Long {
        val vector = savePdfWithAnnotationsVector(pdfPath, annotations, strokes, originalEntry)
        if (vector > 0L) return vector
        return savePdfWithAnnotationsRaster(pdfPath, annotations, strokes, originalEntry)
    }

    /**
     * Vector path: appends marks onto the original PDF with PDFBox (keeps selectable text).
     * Fails for some PDFs (e.g. encrypted), in which case [savePdfWithAnnotationsRaster] is used.
     */
    private suspend fun savePdfWithAnnotationsVector(
        pdfPath: String,
        annotations: List<PdfAnnotation>,
        strokes: List<PdfStroke> = emptyList(),
        originalEntry: ClipboardEntry,
    ): Long {
        val source = File(pdfPath)
        if (!source.isFile) return 0L
        val pdfName = buildPdfName(originalEntry.previewText)
        val pdfFile = ensureUniqueFile(File(pdfsDir, pdfName)) ?: return 0L
        return try {
            PDDocument.load(source, MemoryUsageSetting.setupTempFileOnly()).use { document ->
                if (document.numberOfPages == 0) {
                    pdfFile.delete()
                    return 0L
                }
                if (document.isEncrypted) {
                    try {
                        document.setAllSecurityToBeRemoved(true)
                    } catch (_: Exception) {
                    }
                }
                val pageCount = document.numberOfPages
                val hasText =
                    annotations.any { it.text.isNotBlank() && it.pageIndex in 0 until pageCount }
                val font =
                    if (hasText) {
                        context.assets.open(ANNOTATION_FONT).use { PDType0Font.load(document, it) }
                    } else {
                        null
                    }
                val markedPages =
                    (annotations.map { it.pageIndex } + strokes.map { it.pageIndex })
                        .filter { it in 0 until pageCount }
                        .distinct()
                markedPages.forEach { pageIndex ->
                    val pageAnnotations =
                        annotations.filter { it.pageIndex == pageIndex && it.text.isNotBlank() }
                    val pageStrokes =
                        strokes.filter { it.pageIndex == pageIndex && it.points.isNotEmpty() }
                    if (pageAnnotations.isEmpty() && pageStrokes.isEmpty()) return@forEach
                    appendPdfMarks(
                        document,
                        document.getPage(pageIndex),
                        font,
                        pageAnnotations,
                        pageStrokes,
                    )
                }
                FileOutputStream(pdfFile).use { document.save(it) }
            }
            val id = System.nanoTime()
            dao.insert(
                ClipboardItem(
                    id = id,
                    contentType = ContentType.PDF,
                    filePath = pdfFile.absolutePath,
                    previewText = extractBaseName(pdfName),
                    isPinned = false,
                ),
            )
            id
        } catch (_: Exception) {
            pdfFile.delete()
            0L
        } catch (_: OutOfMemoryError) {
            // OutOfMemoryError is an Error, so the Exception catch above does not see it.
            pdfFile.delete()
            0L
        }
    }

    /**
     * Raster fallback: renders each page with [android.graphics.pdf.PdfRenderer] (which handles
     * standard-encrypted PDFs) and draws the marks on the bitmap, producing a new image-based PDF.
     * Used when the vector path fails, e.g. for encrypted or otherwise unsupported PDFs.
     */
    private suspend fun savePdfWithAnnotationsRaster(
        pdfPath: String,
        annotations: List<PdfAnnotation>,
        strokes: List<PdfStroke>,
        originalEntry: ClipboardEntry,
    ): Long {
        return try {
            val pageCount = pdfPageCount(pdfPath)
            if (pageCount == 0) return 0L
            val document = PdfDocument()
            var pagesAdded = 0
            for (i in 0 until pageCount) {
                val pageBitmap = renderPdfPage(pdfPath, OCR_RENDER_DIM, i) ?: continue
                val canvas = android.graphics.Canvas(pageBitmap)
                drawMarksOnBitmap(
                    canvas,
                    pageBitmap.width,
                    pageBitmap.height,
                    annotations.filter { it.pageIndex == i },
                    strokes.filter { it.pageIndex == i },
                )
                canvas.setBitmap(null)
                val pageInfo =
                    PdfDocument.PageInfo.Builder(pageBitmap.width, pageBitmap.height, pagesAdded + 1).create()
                val page = document.startPage(pageInfo)
                pageBitmap.density = 72
                page.canvas.drawBitmap(
                    pageBitmap,
                    android.graphics.Rect(0, 0, pageBitmap.width, pageBitmap.height),
                    android.graphics.RectF(0f, 0f, pageBitmap.width.toFloat(), pageBitmap.height.toFloat()),
                    null,
                )
                document.finishPage(page)
                pagesAdded++
                pageBitmap.recycle()
            }
            if (pagesAdded == 0) {
                document.close()
                return 0L
            }
            val out = ByteArrayOutputStream()
            document.writeTo(out)
            document.close()

            writePdfClip(out.toByteArray(), originalEntry)
        } catch (_: Exception) {
            0L
        } catch (_: OutOfMemoryError) {
            0L
        }
    }

    private fun drawMarksOnBitmap(
        canvas: android.graphics.Canvas,
        width: Int,
        height: Int,
        annotations: List<PdfAnnotation>,
        strokes: List<PdfStroke>,
    ) {
        val items =
            buildList {
                annotations.forEach { add(it.zOrder to (it as Any)) }
                strokes.forEach { add(it.zOrder to (it as Any)) }
            }.sortedBy { it.first }
        for ((_, item) in items) {
            when (item) {
                is PdfAnnotation -> {
                    val paint =
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = item.color
                            textSize = item.sizeFraction * height
                            typeface = android.graphics.Typeface.DEFAULT
                        }
                    val x = item.xFraction * width
                    var y = item.yFraction * height - paint.ascent()
                    wrapAnnotationText(item.text, paint, (width * 0.95f).toInt()).forEach { line ->
                        canvas.drawText(line, x, y, paint)
                        y += paint.fontSpacing
                    }
                }
                is PdfStroke -> {
                    val paint =
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = item.color
                            strokeWidth = item.widthFraction * height
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                        }
                    val pts = item.points
                    if (pts.isNotEmpty()) {
                        val path = android.graphics.Path()
                        path.moveTo(pts.first().first * width, pts.first().second * height)
                        if (item.straight && pts.size > 1) {
                            path.lineTo(pts.last().first * width, pts.last().second * height)
                        } else {
                            pts.drop(1).forEach { (fx, fy) ->
                                path.lineTo(fx * width, fy * height)
                            }
                        }
                        canvas.drawPath(path, paint)
                    }
                }
            }
        }
    }

    /**
     * Draws marks in the same orientation [android.graphics.pdf.PdfRenderer] shows: origin at the
     * top-left of the rotated crop box, y downward. The content stream itself is unrotated PDF
     * user space, so the CTM maps upright on-screen points back onto the crop box.
     */
    private fun appendPdfMarks(
        document: PDDocument,
        page: PDPage,
        font: PDType0Font?,
        annotations: List<PdfAnnotation>,
        strokes: List<PdfStroke>,
    ) {
        val box = page.cropBox
        val rotation = normalizedPageRotation(page.rotation)
        val (visualWidth, visualHeight) =
            if (rotation == 90 || rotation == 270) {
                box.height to box.width
            } else {
                box.width to box.height
            }
        if (visualWidth <= 0f || visualHeight <= 0f) return
        PDPageContentStream(
            document,
            page,
            PDPageContentStream.AppendMode.APPEND,
            true,
            true,
        ).use { stream ->
            stream.saveGraphicsState()
            stream.transform(uprightPageMatrix(box, rotation))
            stream.setLineCapStyle(1)
            stream.setLineJoinStyle(1)
            // Stable sort: equal zOrder keeps text before strokes, matching the old stamp order.
            val marks =
                buildList {
                    annotations.forEach { add(it.zOrder to (it as Any)) }
                    strokes.forEach { add(it.zOrder to (it as Any)) }
                }.sortedBy { it.first }
            marks.forEach { (_, item) ->
                when (item) {
                    is PdfAnnotation -> {
                        if (font != null) {
                            drawPdfAnnotation(stream, font, item, visualWidth, visualHeight)
                        }
                    }
                    is PdfStroke -> drawPdfStroke(stream, item, visualWidth, visualHeight)
                }
            }
            stream.restoreGraphicsState()
        }
    }

    private fun drawPdfAnnotation(
        stream: PDPageContentStream,
        font: PDFont,
        annotation: PdfAnnotation,
        visualWidth: Float,
        visualHeight: Float,
    ) {
        val fontSize = annotation.sizeFraction * visualHeight
        if (fontSize <= 0f) return
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = fontSize
                typeface = Typeface.DEFAULT
            }
        val lines =
            wrapAnnotationText(annotation.text, paint, (visualWidth * 0.95f).toInt())
                .map { pdfSafeText(font, it) }
                .filter { it.isNotEmpty() }
        if (lines.isEmpty()) return
        val (red, green, blue) = rgbOf(annotation.color)
        stream.setNonStrokingColor(red / 255f, green / 255f, blue / 255f)
        val lineAdvance = paint.fontSpacing.let { if (it > 0f) it else fontSize }
        var baseline = (1f - annotation.yFraction) * visualHeight - (-paint.ascent()).coerceAtLeast(0f)
        val x = annotation.xFraction * visualWidth
        lines.forEach { line ->
            stream.beginText()
            stream.setFont(font, fontSize)
            stream.newLineAtOffset(x, baseline)
            stream.showText(line)
            stream.endText()
            baseline -= lineAdvance
        }
    }

    private fun drawPdfStroke(
        stream: PDPageContentStream,
        stroke: PdfStroke,
        visualWidth: Float,
        visualHeight: Float,
    ) {
        val points = stroke.points
        if (points.isEmpty()) return
        val (red, green, blue) = rgbOf(stroke.color)
        if (stroke.highlight) stream.saveGraphicsState()
        stream.setStrokingColor(red / 255f, green / 255f, blue / 255f)
        if (stroke.highlight) {
            val graphics = PDExtendedGraphicsState()
            graphics.strokingAlphaConstant = 0.35f
            stream.setGraphicsStateParameters(graphics)
        }
        val widthScale = if (stroke.highlight) 3f else 1f
        stream.setLineWidth((stroke.widthFraction * visualHeight * widthScale).coerceAtLeast(0.1f))
        val start = points.first()
        stream.moveTo(start.first * visualWidth, (1f - start.second) * visualHeight)
        if (stroke.straight && points.size > 1) {
            val end = points.last()
            stream.lineTo(end.first * visualWidth, (1f - end.second) * visualHeight)
        } else {
            points.drop(1).forEach { (xFraction, yFraction) ->
                stream.lineTo(xFraction * visualWidth, (1f - yFraction) * visualHeight)
            }
        }
        stream.stroke()
        if (stroke.highlight) stream.restoreGraphicsState()
    }

    private fun pdfSafeText(
        font: PDFont,
        text: String,
    ): String {
        // A soft hyphen must not leak into the PDF as a stray glyph (#62).
        val text = text.withoutSoftHyphens()
        val safe = StringBuilder(text.length)
        text.forEach { char ->
            val glyph = if (char == '\n' || char == '\r') " " else char.toString()
            try {
                font.encode(glyph)
                safe.append(glyph)
            } catch (_: IllegalArgumentException) {
                try {
                    font.encode("?")
                    safe.append('?')
                } catch (_: IllegalArgumentException) {
                }
            }
        }
        return safe.toString()
    }

    suspend fun savePdfFromBitmap(
        bitmap: Bitmap,
        originalEntry: ClipboardEntry,
    ): Long {
        return try {
            val maxOutputPx = PdfSettingsStore.load(context).maxOutputPx
            val bitmapToUse = scaleDownIfNeeded(bitmap, maxOutputPx)
            val pdfBytes = createPdfFromBitmap(bitmapToUse)
            if (bitmapToUse !== bitmap) bitmapToUse.recycle()

            writePdfClip(pdfBytes, originalEntry)
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun savePdfFromImageFile(
        filePath: String,
        originalEntry: ClipboardEntry,
    ): Long {
        var bitmap: Bitmap? = null
        return try {
            bitmap = loadRotatedBitmap(filePath) ?: return 0L
            savePdfFromBitmap(bitmap, originalEntry)
        } catch (_: Exception) {
            0L
        } finally {
            bitmap?.recycle()
        }
    }

    suspend fun saveImagesAsPdf(entries: List<ClipboardEntry>): PdfCombineResult {
        if (entries.isEmpty()) return PdfCombineResult.NothingReadable
        return try {
            val maxOutputPx = PdfSettingsStore.load(context).maxOutputPx
            val document = PdfDocument()
            var pagesAdded = 0
            entries.forEach { entry ->
                val filePath = entry.filePath ?: return@forEach
                val bitmap = loadRotatedBitmap(filePath) ?: return@forEach
                val bmp = scaleDownIfNeeded(bitmap, maxOutputPx)
                val pageInfo =
                    PdfDocument.PageInfo.Builder(bmp.width, bmp.height, pagesAdded + 1).create()
                val page = document.startPage(pageInfo)
                bmp.density = 72
                page.canvas.drawBitmap(
                    bmp,
                    android.graphics.Rect(0, 0, bmp.width, bmp.height),
                    android.graphics.RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat()),
                    null,
                )
                document.finishPage(page)
                pagesAdded++
                if (bmp !== bitmap) bmp.recycle()
                bitmap.recycle()
            }
            if (pagesAdded == 0) {
                document.close()
                return PdfCombineResult.NothingReadable
            }
            val out = ByteArrayOutputStream()
            document.writeTo(out)
            document.close()

            persistPdfBytes(out.toByteArray(), entries.first())
        } catch (_: Exception) {
            PdfCombineResult.SaveFailed
        } catch (_: OutOfMemoryError) {
            PdfCombineResult.SaveFailed
        }
    }

    /**
     * Combines [entries] in order into one PDF clip. A homogeneous PDF selection is merged
     * losslessly (keep selectable text); images and mixed selections are rasterised into pages.
     */
    suspend fun combineEntriesToPdf(entries: List<ClipboardEntry>): PdfCombineResult {
        if (entries.size < 2) return PdfCombineResult.NothingReadable
        return when (entries.map { it.contentType }.toSet()) {
            setOf(ContentType.PDF) -> mergePdfs(entries)
            setOf(ContentType.IMAGE) -> saveImagesAsPdf(entries)
            else -> {
                val rasterBytes = renderEntriesToRasterPdf(entries)
                if (rasterBytes != null) {
                    persistPdfBytes(rasterBytes, entries.first())
                } else {
                    PdfCombineResult.NothingReadable
                }
            }
        }
    }

    suspend fun mergePdfs(entries: List<ClipboardEntry>): PdfCombineResult {
        if (entries.isEmpty()) return PdfCombineResult.NothingReadable
        // Vector path keeps selectable text. PDFBox cannot parse every file, so fall back to
        // rasterising pages with PdfRenderer when the structural merge fails.
        val vectorBytes = mergePdfsVector(entries)
        if (vectorBytes != null) return persistPdfBytes(vectorBytes, entries.first())
        val rasterBytes = renderEntriesToRasterPdf(entries)
        if (rasterBytes != null) return persistPdfBytes(rasterBytes, entries.first())
        return PdfCombineResult.NothingReadable
    }

    /**
     * Merges [entries] by cloning pages into one PDF. [PDFMergerUtility] keeps every source open
     * until the destination is saved, so embedded fonts and images are copied instead of left
     * behind on a closed document. Returns null when a file cannot be read so the caller can retry
     * with [renderEntriesToRasterPdf]. A failure on any entry aborts the whole attempt.
     */
    private fun mergePdfsVector(entries: List<ClipboardEntry>): ByteArray? {
        val files =
            entries.mapNotNull { entry ->
                entry.filePath?.let(::File)?.takeIf { it.isFile }
            }
        if (files.isEmpty()) return null
        return try {
            val out = ByteArrayOutputStream()
            val merger = PDFMergerUtility()
            files.forEach { merger.addSource(it) }
            merger.destinationStream = out
            merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly())
            val bytes = out.toByteArray()
            if (bytes.isEmpty()) return null
            PDDocument.load(bytes).use { merged ->
                if (merged.numberOfPages == 0) return null
            }
            bytes
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    /**
     * Raster fallback that renders each entry in order with PdfRenderer (PDFs) or the image decoder
     * (images) and saves the pages as one image PDF. Handles encrypted/unsupported PDFs.
     */
    internal fun renderEntriesToRasterPdf(entries: List<ClipboardEntry>): ByteArray? {
        return try {
            val document = PdfDocument()
            var pagesAdded = 0
            entries.forEach { entry ->
                when (entry.contentType) {
                    ContentType.PDF -> {
                        val filePath = entry.filePath ?: return@forEach
                        val pageCount = pdfPageCount(filePath)
                        for (i in 0 until pageCount) {
                            val bitmap = renderPdfPage(filePath, OCR_RENDER_DIM, i) ?: continue
                            pagesAdded = addBitmapPage(document, bitmap, pagesAdded)
                        }
                    }
                    ContentType.IMAGE -> {
                        val filePath = entry.filePath ?: return@forEach
                        val bitmap = loadRotatedBitmap(filePath) ?: return@forEach
                        pagesAdded = addBitmapPage(document, bitmap, pagesAdded)
                    }
                    else -> return@forEach
                }
            }
            if (pagesAdded == 0) {
                document.close()
                return null
            }
            val out = ByteArrayOutputStream()
            document.writeTo(out)
            document.close()
            out.toByteArray()
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    private fun addBitmapPage(
        document: PdfDocument,
        bitmap: Bitmap,
        pageIndex: Int,
    ): Int {
        val pageInfo =
            PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, pageIndex + 1).create()
        val page = document.startPage(pageInfo)
        bitmap.density = 72
        page.canvas.drawBitmap(
            bitmap,
            android.graphics.Rect(0, 0, bitmap.width, bitmap.height),
            android.graphics.RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
            null,
        )
        document.finishPage(page)
        bitmap.recycle()
        return pageIndex + 1
    }

    private suspend fun persistPdfBytes(
        pdfBytes: ByteArray,
        originalEntry: ClipboardEntry,
    ): PdfCombineResult {
        val id = writePdfClip(pdfBytes, originalEntry)
        return if (id > 0L) PdfCombineResult.Success(id) else PdfCombineResult.SaveFailed
    }

    private fun loadRotatedBitmap(filePath: String): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(filePath, bounds)
            val srcW = bounds.outWidth
            val srcH = bounds.outHeight
            if (srcW <= 0 || srcH <= 0) return null
            var sample = 1
            val srcMax = max(srcW, srcH)
            while (srcMax / sample > MAX_IMAGE_DECODE_PX) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bitmap = BitmapFactory.decodeFile(filePath, opts) ?: return null
            val decodedMax = max(bitmap.width, bitmap.height)
            if (decodedMax > MAX_IMAGE_DECODE_PX) {
                val scaled = scaleDownIfNeeded(bitmap, MAX_IMAGE_DECODE_PX)
                if (scaled !== bitmap) bitmap.recycle()
                bitmap = scaled
            }
            val exif = androidx.exifinterface.media.ExifInterface(filePath)
            val orientation =
                exif.getAttributeInt(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL,
                )
            val rotation =
                when (orientation) {
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            if (rotation == 0f) {
                bitmap
            } else {
                val matrix = android.graphics.Matrix()
                matrix.postRotate(rotation)
                val rotated =
                    android.graphics.Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.width,
                        bitmap.height,
                        matrix,
                        true,
                    )
                bitmap.recycle()
                rotated
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveImagesFromPdf(
        pdfPath: String,
        originalEntry: ClipboardEntry,
    ): Int {
        return try {
            val file = File(pdfPath)
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            try {
                val renderer = PdfRenderer(pfd)
                try {
                    val pageCount = renderer.pageCount
                    if (pageCount == 0) return 0

                    val items = mutableListOf<ClipboardItem>()
                    val created = mutableListOf<File>()
                    val baseTime = System.currentTimeMillis()
                    val maxOutputPx = PdfSettingsStore.load(context).maxOutputPx
                    try {
                        for (i in 0 until pageCount) {
                            val page = renderer.openPage(i)
                            var pageBitmap: Bitmap? = null
                            try {
                                val renderScale =
                                    minOf(
                                        maxOutputPx.toFloat() / max(page.width, page.height),
                                        4f,
                                    ).coerceAtLeast(1f)
                                val w = (page.width * renderScale).toInt()
                                val h = (page.height * renderScale).toInt()
                                pageBitmap =
                                    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                pageBitmap.eraseColor(Color.WHITE)
                                val matrix = android.graphics.Matrix()
                                matrix.postScale(renderScale, renderScale)
                                page.render(pageBitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                                val imgName =
                                    buildPageImageName(
                                        originalEntry.previewText,
                                        i + 1,
                                        pageCount,
                                        localizedString(R.string.file_name_image),
                                    )
                                val imgFile = ensureUniqueFile(File(imagesDir, imgName)) ?: continue
                                created.add(imgFile)
                                FileOutputStream(imgFile).use { out ->
                                    pageBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                }

                                val id = System.nanoTime() + i
                                val pageTimestamp = baseTime - i * 1000L
                                items.add(
                                    ClipboardItem(
                                        id = id,
                                        contentType = ContentType.IMAGE,
                                        filePath = imgFile.absolutePath,
                                        previewText = extractBaseName(imgName),
                                        timestamp = pageTimestamp,
                                        isPinned = false,
                                    ),
                                )
                            } finally {
                                pageBitmap?.recycle()
                                page.close()
                            }
                        }

                        if (items.isNotEmpty()) {
                            dao.insertAll(items)
                        }
                        items.size
                    } catch (e: Exception) {
                        created.forEach { it.delete() }
                        throw e
                    }
                } finally {
                    renderer.close()
                }
            } finally {
                pfd.close()
            }
        } catch (_: Exception) {
            0
        }
    }

    suspend fun savePdfPageImagesAsClips(
        pdfPath: String,
        pageIndex: Int,
        originalEntry: ClipboardEntry,
    ): Int {
        return try {
            val document = PDDocument.load(File(pdfPath))
            try {
                if (document.numberOfPages == 0) return 0
                val index = pageIndex.coerceIn(0, document.numberOfPages - 1)
                val page = document.getPage(index)
                val bitmaps = mutableListOf<Bitmap>()
                collectPageImages(page.resources, bitmaps)
                if (bitmaps.isEmpty()) return 0

                val base = extractBaseName(originalEntry.previewText).take(30)
                val baseTime = System.currentTimeMillis()
                val items = mutableListOf<ClipboardItem>()
                val created = mutableListOf<File>()
                try {
                    bitmaps.forEachIndexed { i, bitmap ->
                        val imgName = "${base}_p${index + 1}_${i + 1}.jpg"
                        val imgFile = ensureUniqueFile(File(imagesDir, imgName))
                        if (imgFile == null) {
                            bitmap.recycle()
                            return@forEachIndexed
                        }
                        created.add(imgFile)
                        FileOutputStream(imgFile).use { out ->
                            val toWrite = if (bitmap.hasAlpha()) whiteOpaque(bitmap) else bitmap
                            toWrite.compress(Bitmap.CompressFormat.JPEG, 90, out)
                            if (toWrite !== bitmap) toWrite.recycle()
                        }
                        bitmap.recycle()
                        items.add(
                            ClipboardItem(
                                id = System.nanoTime() + i,
                                contentType = ContentType.IMAGE,
                                filePath = imgFile.absolutePath,
                                previewText = extractBaseName(imgName),
                                timestamp = baseTime - i * 1000L,
                                isPinned = false,
                            ),
                        )
                    }
                    if (items.isNotEmpty()) {
                        dao.insertAll(items)
                    }
                    items.size
                } catch (e: Exception) {
                    created.forEach { it.delete() }
                    throw e
                }
            } finally {
                document.close()
            }
        } catch (_: Exception) {
            0
        }
    }

    suspend fun saveTextFileAsPdf(entry: ClipboardEntry): Long {
        val filePath = entry.filePath ?: return 0L
        val text = readTextFileSafely(filePath) ?: return 0L
        return saveTextAsPdf(text, entry)
    }

    suspend fun saveRtfFileAsPdf(entry: ClipboardEntry): Long {
        val filePath = entry.filePath ?: return 0L
        val text = readTextFileSafely(filePath) ?: return 0L
        val html = rtfToHtml(text)
        val bytes = renderHtmlPdf(html, pdfLayout())
        if (bytes.isEmpty()) return 0L
        return writePdfClip(bytes, entry)
    }

    suspend fun saveDocxFileAsPdf(entry: ClipboardEntry): Long {
        val filePath = entry.filePath ?: return 0L
        val html = docxToHtml(filePath) ?: return 0L
        val bytes = renderHtmlPdf(html, pdfLayout())
        if (bytes.isEmpty()) return 0L
        return writePdfClip(bytes, entry)
    }

    suspend fun saveXlsxFileAsPdf(entry: ClipboardEntry): Long {
        val filePath = entry.filePath ?: return 0L
        val html = xlsxToHtml(filePath) ?: return 0L
        val bytes = renderHtmlPdf(html, pdfLayout())
        if (bytes.isEmpty()) return 0L
        return writePdfClip(bytes, entry)
    }

    private suspend fun writePdfClip(
        pdfBytes: ByteArray,
        originalEntry: ClipboardEntry,
    ): Long {
        val pdfName = buildPdfName(originalEntry.previewText)
        val pdfFile = ensureUniqueFile(File(pdfsDir, pdfName)) ?: return 0L
        return try {
            FileOutputStream(pdfFile).use { fout ->
                fout.write(pdfBytes)
            }
            val id = System.nanoTime()
            val item =
                ClipboardItem(
                    id = id,
                    contentType = ContentType.PDF,
                    filePath = pdfFile.absolutePath,
                    previewText = extractBaseName(pdfName),
                    isPinned = false,
                )
            dao.insert(item)
            id
        } catch (_: Exception) {
            pdfFile.delete()
            0L
        }
    }

    suspend fun saveHtmlAsPdf(entry: ClipboardEntry): Long {
        val html = entry.textContent ?: return 0L
        if (html.isBlank()) return 0L
        val bytes = renderHtmlPdf(html, pdfLayout())
        if (bytes.isEmpty()) return 0L
        return writePdfClip(bytes, entry)
    }

    private fun readTextFileSafely(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            if (file.length() > MAX_TEXT_VIEW_BYTES) return null
            if (isProbablyBinary(file)) return null
            file.readText()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveTextAsPdf(
        text: String,
        originalEntry: ClipboardEntry,
        fontSizePt: Float? = null,
        lineSpacing: Float? = null,
    ): Long {
        return try {
            if (text.isBlank()) return 0L
            val pdfBytes = renderTextPdfBytes(text, fontSizePt, lineSpacing)
            if (pdfBytes.isEmpty()) return 0L
            writePdfClip(
                pdfBytes,
                originalEntry.copy(previewText = originalEntry.previewText.ifBlank { text }),
            )
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun previewTextAsPdf(
        text: String,
        fontSizePt: Float,
        lineSpacing: Float,
    ): List<Bitmap> {
        return try {
            if (text.isBlank()) return emptyList()
            val pdfBytes = renderTextPdfBytes(text, fontSizePt, lineSpacing, forPreview = true)
            if (pdfBytes.isEmpty()) return emptyList()
            val tempFile = File(context.cacheDir, "preview_${System.nanoTime()}.pdf")
            FileOutputStream(tempFile).use { it.write(pdfBytes) }
            try {
                val count = pdfPageCount(tempFile.absolutePath)
                val bitmaps = mutableListOf<Bitmap>()
                for (i in 0 until count) {
                    renderPdfPage(tempFile.absolutePath, 1400, i)?.let { bitmaps.add(it) }
                }
                bitmaps
            } finally {
                tempFile.delete()
            }
        } catch (_: Exception) {
            emptyList()
        } catch (_: OutOfMemoryError) {
            emptyList()
        }
    }

    private fun renderTextPdfBytes(
        text: String,
        fontSizePt: Float?,
        lineSpacing: Float?,
        forPreview: Boolean = false,
    ): ByteArray {
        // A soft hyphen must not leak into the PDF output (#62).
        val text = text.withoutSoftHyphens()
        val baseLayout = pdfLayout()
        val layout =
            when {
                fontSizePt != null && lineSpacing != null ->
                    baseLayout.copy(fontSizePt = fontSizePt, lineSpacing = lineSpacing)
                fontSizePt != null -> baseLayout.copy(fontSizePt = fontSizePt)
                lineSpacing != null -> baseLayout.copy(lineSpacing = lineSpacing)
                else -> baseLayout
            }
        return when {
            looksLikeHtml(text) -> renderHtmlPdf(text, layout)
            looksLikeMarkdown(text) -> {
                val mdLayout =
                    if (forPreview) {
                        layout.copy(
                            marginTopBottomPt = MARKDOWN_PREVIEW_MARGIN_PT,
                            fontSizePt = layout.fontSizePt * MARKDOWN_PREVIEW_FONT_SCALE,
                        )
                    } else {
                        layout
                    }
                renderMarkdownPdf(text, mdLayout, compactHeadings = forPreview)
            }
            else -> renderPlainPdf(text, layout)
        }
    }

    private fun looksLikeHtml(text: String): Boolean {
        if (text.isBlank()) return false
        val trimmed = text.trimStart()
        return trimmed.startsWith("<html", ignoreCase = true) ||
            text.contains("<body>", ignoreCase = true)
    }

    private fun pdfLayout(): PdfLayout {
        val settings = PdfSettingsStore.load(context)
        return PdfLayout(
            marginTopBottomPt = settings.marginTopBottomPt,
            marginLeftRightPt = settings.marginLeftRightPt,
            fontSizePt = settings.fontSizePt,
            lineSpacing = settings.lineSpacing,
            font = settings.pdfFont,
        )
    }

    private fun pdfTypeface(
        font: PdfFont,
        bold: Boolean,
        mono: Boolean = false,
    ): android.graphics.Typeface {
        val family = if (mono) PdfFont.MONO else font
        return when (family) {
            PdfFont.SYSTEM ->
                if (bold) {
                    android.graphics.Typeface.DEFAULT_BOLD
                } else {
                    android.graphics.Typeface.DEFAULT
                }
            PdfFont.SANS -> assetTypeface("LiberationSans", bold)
            PdfFont.SERIF -> assetTypeface("LiberationSerif", bold)
            PdfFont.MONO -> assetTypeface("LiberationMono", bold)
        }
    }

    private fun assetTypeface(
        name: String,
        bold: Boolean,
    ): android.graphics.Typeface =
        android.graphics.Typeface.createFromAsset(
            context.assets,
            "fonts/$name-${if (bold) "Bold" else "Regular"}.ttf",
        )

    private fun renderPlainPdf(
        text: String,
        layout: PdfLayout,
    ): ByteArray {
        val document = PdfDocument()
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = layout.fontSizePt
                color = Color.BLACK
                typeface = pdfTypeface(layout.font, bold = false)
            }
        val textWidth = layout.textWidth
        val lineHeight = layout.baseLineHeight

        val lines = wrapText(text, paint, textWidth)
        val maxLinesPerPage = ((A4_HEIGHT_PT - 2 * layout.marginTopBottomPt) / lineHeight).toInt().coerceAtLeast(1)

        var pageNumber = 1
        var y = layout.marginTopBottomPt
        var page =
            document.startPage(
                PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, pageNumber).create(),
            )
        lines.forEachIndexed { i, line ->
            if (i > 0 && i % maxLinesPerPage == 0) {
                document.finishPage(page)
                pageNumber++
                page =
                    document.startPage(
                        PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, pageNumber).create(),
                    )
                y = layout.marginTopBottomPt
            }
            page.canvas.drawText(line, layout.marginLeftRightPt, y, paint)
            y += lineHeight
        }
        document.finishPage(page)

        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        return out.toByteArray()
    }

    private fun looksLikeMarkdown(text: String): Boolean {
        if (text.contains("**") || text.contains("__") || text.contains("~~") || text.contains("`")) return true
        if (Regex("""(?m)^\s*#{1,6}\s""").containsMatchIn(text)) return true
        if (Regex("""(?m)^\s*```""").containsMatchIn(text)) return true
        if (Regex("""(?m)^\s*([-*_])\1{2,}\s*$""").containsMatchIn(text)) return true
        if (Regex("""(?m)^\s*>""").containsMatchIn(text)) return true
        if (Regex("""(?m)^\s*[-*+]\s""").containsMatchIn(text)) return true
        if (Regex("""(?m)^\s*\d+\.\s""").containsMatchIn(text)) return true
        if (Regex("""\*[^*\n]+\*""").containsMatchIn(text)) return true
        if (Regex("""_[^_\n]+_""").containsMatchIn(text)) return true
        if (Regex(""" {2}\n""").containsMatchIn(text)) return true
        return false
    }

    private class MdRun(
        val text: String,
        val paint: Paint,
        val strike: Boolean = false,
    )

    private class MdLine(
        val runs: List<MdRun>,
        val lineHeight: Float,
        val indent: Float = 0f,
        val background: Int? = null,
        val quote: Boolean = false,
        val isHr: Boolean = false,
    )

    private fun renderMarkdownPdf(
        text: String,
        layout: PdfLayout,
        compactHeadings: Boolean = false,
    ): ByteArray {
        val document = PdfDocument()
        val parser =
            Parser.builder()
                .extensions(listOf(StrikethroughExtension.create()))
                .build()
        val root = parser.parse(text)
        val lines = collectMdLines(root, layout, compactHeadings)
        val firstBaseline =
            if (compactHeadings) {
                layout.marginTopBottomPt + layout.fontSizePt * 0.85f
            } else {
                layout.marginTopBottomPt
            }

        var pageNumber = 1
        var y = firstBaseline
        var page =
            document.startPage(
                PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, pageNumber).create(),
            )
        for (line in lines) {
            if (y + line.lineHeight > A4_HEIGHT_PT - layout.marginTopBottomPt) {
                document.finishPage(page)
                pageNumber++
                page =
                    document.startPage(
                        PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, pageNumber).create(),
                    )
                y = firstBaseline
            }
            drawMdLine(page.canvas, line, y, layout)
            y += line.lineHeight
        }
        document.finishPage(page)

        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        return out.toByteArray()
    }

    private fun collectMdLines(
        root: Node,
        layout: PdfLayout,
        compactHeadings: Boolean = false,
    ): List<MdLine> {
        val out = mutableListOf<MdLine>()
        val visitor =
            object : AbstractVisitor() {
                override fun visit(heading: Heading) {
                    val size = headingFontSize(heading.level, layout, compactHeadings)
                    val lineHeight = size * layout.lineSpacing
                    val runs = collectInlineRuns(heading, baseMdPaint(size, bold = true, font = layout.font))
                    val gap = lineHeight * if (compactHeadings) 0.2f else 0.5f
                    if (out.isNotEmpty() || !compactHeadings) {
                        out.add(MdLine(emptyList(), gap))
                    }
                    wrapRuns(runs, layout.textWidth).forEach {
                        out.add(MdLine(it, lineHeight))
                    }
                    out.add(MdLine(emptyList(), gap))
                }

                override fun visit(paragraph: Paragraph) {
                    val runs =
                        collectInlineRuns(paragraph, baseMdPaint(layout.fontSizePt, bold = false, font = layout.font))
                    wrapRuns(runs, layout.textWidth).forEach {
                        out.add(MdLine(it, layout.baseLineHeight))
                    }
                    out.add(MdLine(emptyList(), layout.baseLineHeight * 0.5f))
                }

                override fun visit(bulletList: BulletList) {
                    bulletList.forEachChild { child ->
                        if (child is ListItem) {
                            val runs =
                                mutableListOf(
                                    MdRun("•  ", baseMdPaint(layout.fontSizePt, bold = false, font = layout.font)),
                                )
                            runs +=
                                collectInlineRuns(
                                    child,
                                    baseMdPaint(layout.fontSizePt, bold = false, font = layout.font),
                                )
                            wrapRuns(runs, layout.textWidth).forEachIndexed { i, lineRuns ->
                                out.add(
                                    MdLine(lineRuns, layout.baseLineHeight, indent = if (i == 0) 0f else LIST_INDENT),
                                )
                            }
                        }
                    }
                }

                override fun visit(orderedList: OrderedList) {
                    var index = orderedList.markerStartNumber ?: 1
                    orderedList.forEachChild { child ->
                        if (child is ListItem) {
                            val runs =
                                mutableListOf(
                                    MdRun(
                                        "$index.  ",
                                        baseMdPaint(layout.fontSizePt, bold = false, font = layout.font),
                                    ),
                                )
                            runs +=
                                collectInlineRuns(
                                    child,
                                    baseMdPaint(layout.fontSizePt, bold = false, font = layout.font),
                                )
                            wrapRuns(runs, layout.textWidth).forEachIndexed { i, lineRuns ->
                                out.add(
                                    MdLine(lineRuns, layout.baseLineHeight, indent = if (i == 0) 0f else LIST_INDENT),
                                )
                            }
                            index++
                        }
                    }
                }

                override fun visit(fencedCodeBlock: FencedCodeBlock) {
                    val mono =
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            textSize = layout.fontSizePt * 0.9f
                            typeface = pdfTypeface(layout.font, bold = false, mono = true)
                            color = Color.BLACK
                        }
                    val lineHeight = mono.textSize * layout.lineSpacing
                    fencedCodeBlock.literal.split("\n").forEach { line ->
                        out.add(
                            MdLine(
                                runs = listOf(MdRun(line, mono)),
                                lineHeight = lineHeight,
                                indent = 8f,
                                background = CODE_BG,
                            ),
                        )
                    }
                    out.add(MdLine(emptyList(), lineHeight * 0.5f))
                }

                override fun visit(blockQuote: BlockQuote) {
                    val runs =
                        collectInlineRuns(blockQuote, baseMdPaint(layout.fontSizePt, bold = false, font = layout.font))
                    wrapRuns(runs, layout.textWidth - QUOTE_INDENT).forEach {
                        out.add(MdLine(it, layout.baseLineHeight, indent = QUOTE_INDENT, quote = true))
                    }
                    out.add(MdLine(emptyList(), layout.baseLineHeight * 0.5f))
                }

                override fun visit(thematicBreak: ThematicBreak) {
                    out.add(MdLine(emptyList(), layout.baseLineHeight, isHr = true))
                }
            }
        root.accept(visitor)
        return out
    }

    private fun collectInlineRuns(
        node: Node,
        basePaint: Paint,
        strike: Boolean = false,
    ): List<MdRun> {
        val result = mutableListOf<MdRun>()

        fun visit(
            n: Node,
            p: Paint,
            s: Boolean,
        ) {
            when (n) {
                is Text -> result.add(MdRun(n.literal, p, s))
                is Code -> {
                    val cp = Paint(p).apply { typeface = pdfTypeface(PdfFont.MONO, bold = false) }
                    result.add(MdRun(n.literal, cp, s))
                }
                is StrongEmphasis ->
                    n.forEachChild {
                        val boldPaint =
                            Paint(p).apply {
                                typeface = android.graphics.Typeface.create(p.typeface, android.graphics.Typeface.BOLD)
                            }
                        visit(it, boldPaint, s)
                    }
                is Emphasis -> n.forEachChild { visit(it, Paint(p).apply { textSkewX = -0.25f }, s) }
                is Strikethrough -> n.forEachChild { visit(it, p, true) }
                is Link -> {
                    n.forEachChild { visit(it, p, s) }
                    val url = n.destination.orEmpty()
                    if (url.isNotBlank()) {
                        val lp =
                            Paint(p).apply {
                                color = 0xFF555555.toInt()
                                textSize = p.textSize * 0.85f
                            }
                        result.add(MdRun(" ($url)", lp, s))
                    }
                }
                is Image -> {
                    val alt = (n.firstChild as? Text)?.literal.orEmpty()
                    val ip =
                        Paint(p).apply {
                            textSkewX = -0.25f
                            color = 0xFF555555.toInt()
                        }
                    result.add(MdRun(alt.ifBlank { localizedString(R.string.image_placeholder) }, ip, s))
                }
                is SoftLineBreak -> result.add(MdRun(" ", p, s))
                is HardLineBreak -> result.add(MdRun("\n", p, s))
                else -> n.forEachChild { visit(it, p, s) }
            }
        }
        visit(node, basePaint, strike)
        return result
    }

    private fun wrapRuns(
        runs: List<MdRun>,
        maxWidth: Float,
    ): List<List<MdRun>> {
        val result = mutableListOf<List<MdRun>>()
        var current = mutableListOf<MdRun>()
        var currentWidth = 0f

        fun flush() {
            if (current.isNotEmpty()) {
                result.add(current)
                current = mutableListOf()
                currentWidth = 0f
            }
        }

        for (run in runs) {
            if (run.text == "\n") {
                flush()
                continue
            }
            val w = run.paint.measureText(run.text)
            if (currentWidth + w <= maxWidth) {
                current.add(run)
                currentWidth += w
            } else if (w <= maxWidth) {
                flush()
                current.add(run)
                currentWidth = w
            } else {
                flush()
                for (part in breakTextWords(run.text, run.paint, maxWidth)) {
                    current.add(MdRun(part, run.paint, run.strike))
                    currentWidth = run.paint.measureText(part)
                    flush()
                }
            }
        }
        flush()
        return result
    }

    private fun drawMdLine(
        canvas: android.graphics.Canvas,
        line: MdLine,
        y: Float,
        layout: PdfLayout,
    ) {
        if (line.isHr) {
            val ly = y - line.lineHeight * 0.5f
            canvas.drawRect(
                layout.marginLeftRightPt,
                ly,
                A4_WIDTH_PT - layout.marginLeftRightPt,
                ly + 1.5f,
                Paint().apply { color = 0xFF888888.toInt() },
            )
            return
        }
        if (line.background != null) {
            canvas.drawRect(
                layout.marginLeftRightPt,
                y - line.lineHeight * 0.85f,
                A4_WIDTH_PT - layout.marginLeftRightPt,
                y,
                Paint().apply { color = line.background },
            )
        }
        if (line.quote) {
            val barX = layout.marginLeftRightPt + QUOTE_INDENT * 0.5f
            canvas.drawRect(
                barX,
                y - line.lineHeight * 0.85f,
                barX + 2f,
                y,
                Paint().apply { color = 0xFF888888.toInt() },
            )
        }
        var x = layout.marginLeftRightPt + line.indent
        for (run in line.runs) {
            canvas.drawText(run.text, x, y, run.paint)
            val w = run.paint.measureText(run.text)
            if (run.strike) {
                val strikeY = y - run.paint.textSize * 0.35f
                canvas.drawRect(x, strikeY, x + w, strikeY + 1f, Paint().apply { color = 0xFF000000.toInt() })
            }
            x += w
        }
    }

    private fun baseMdPaint(
        size: Float,
        bold: Boolean,
        font: PdfFont = PdfFont.SYSTEM,
    ): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            color = Color.BLACK
            typeface = pdfTypeface(font, bold)
        }

    private fun headingFontSize(
        level: Int,
        layout: PdfLayout,
        compact: Boolean = false,
    ): Float = layout.fontSizePt * markdownHeadingFactor(level, compact)

    private class HtmlLine(
        val runs: List<MdRun>,
        val lineHeight: Float,
        val image: Bitmap? = null,
        val imageW: Float = 0f,
        val imageH: Float = 0f,
        val tableRow: TableRow? = null,
    )

    private class TableRow(
        val cells: List<List<List<MdRun>>>,
        val colWidths: List<Float>,
        val rowHeight: Float,
    )

    private sealed class InlineItem {
        data class Text(val text: String, val paint: Paint) : InlineItem()

        data class ImageItem(val bitmap: Bitmap, val width: Float, val height: Float) : InlineItem()

        object Break : InlineItem()
    }

    private fun renderHtmlPdf(
        html: String,
        layout: PdfLayout,
    ): ByteArray {
        // A soft hyphen must not leak into the PDF output (#62).
        val lines = parseHtmlLines(html.withoutSoftHyphens(), layout)
        if (lines.isEmpty()) return ByteArray(0)
        val document = PdfDocument()
        try {
            val bottom = A4_HEIGHT_PT - layout.marginTopBottomPt
            var pageNumber = 1
            var y = layout.marginTopBottomPt
            var page =
                document.startPage(
                    PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, pageNumber).create(),
                )
            for (line in lines) {
                if (y + line.lineHeight > bottom && y > layout.marginTopBottomPt) {
                    document.finishPage(page)
                    pageNumber++
                    page =
                        document.startPage(
                            PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, pageNumber).create(),
                        )
                    y = layout.marginTopBottomPt
                }
                drawHtmlLine(page.canvas, line, y, layout)
                y += line.lineHeight
            }
            document.finishPage(page)
            val out = ByteArrayOutputStream()
            document.writeTo(out)
            return out.toByteArray()
        } finally {
            document.close()
            lines.forEach { it.image?.recycle() }
        }
    }

    private fun parseHtmlLines(
        html: String,
        layout: PdfLayout,
    ): List<HtmlLine> {
        val body =
            html.substringAfter("<body>", html)
                .substringBefore("</body>", html)
        val out = mutableListOf<HtmlLine>()
        var i = 0
        while (i < body.length) {
            val tagStart = body.indexOf('<', i)
            if (tagStart < 0) break
            if (body.startsWith("<p", tagStart)) {
                val end = body.indexOf("</p>", tagStart)
                val content =
                    if (end > 0) {
                        val gt = body.indexOf('>', tagStart)
                        body.substring(gt + 1, end)
                    } else {
                        ""
                    }
                parseParagraph(content, layout, out, headingLevel = 0)
                i = if (end > 0) end + 4 else tagStart + 2
            } else if (body.length > tagStart + 2 && body[tagStart + 1] == 'h') {
                val level = body.getOrNull(tagStart + 2)?.digitToIntOrNull()
                if (level != null) {
                    val end = body.indexOf("</h$level>", tagStart)
                    val gt = body.indexOf('>', tagStart)
                    val content =
                        if (end > 0 && gt > 0) body.substring(gt + 1, end) else ""
                    parseParagraph(content, layout, out, headingLevel = level)
                    i = if (end > 0) end + 5 else tagStart + 3
                } else {
                    i = tagStart + 2
                }
            } else if (body.startsWith("<table", tagStart)) {
                val end = body.indexOf("</table>", tagStart)
                val gt = body.indexOf('>', tagStart)
                val content =
                    if (end > 0 && gt > 0) body.substring(gt + 1, end) else ""
                parseTable(content, layout, out)
                i = if (end > 0) end + 8 else tagStart + 7
            } else if (body.startsWith("<img", tagStart)) {
                val gt = body.indexOf('>', tagStart)
                if (gt > 0) {
                    val tag = body.substring(tagStart, gt + 1)
                    val image = parseImageTag(tag)
                    if (image != null) {
                        val (iw, ih) = fitImage(image.width, image.height, layout)
                        out.add(
                            HtmlLine(
                                runs = emptyList(),
                                lineHeight = ih + layout.fontSizePt + 8f,
                                image = image.bitmap,
                                imageW = iw,
                                imageH = ih,
                            ),
                        )
                    }
                    i = gt + 1
                } else {
                    i = tagStart + 4
                }
            } else {
                i = tagStart + 1
            }
        }
        return out
    }

    private fun parseParagraph(
        content: String,
        layout: PdfLayout,
        out: MutableList<HtmlLine>,
        headingLevel: Int,
    ) {
        val fontSize =
            if (headingLevel > 0) headingFontSize(headingLevel, layout) else layout.fontSizePt
        val paint = baseMdPaint(fontSize, bold = headingLevel > 0, font = layout.font)
        var items = tokenizeInline(content, paint)
        if (items.isEmpty()) return
        if (items.any { it is InlineItem.ImageItem }) {
            val text =
                items
                    .filterIsInstance<InlineItem.Text>()
                    .joinToString("") { it.text }
                    .trim()
            if (text.length <= 1) {
                items = items.filter { it !is InlineItem.Text }
            }
        }

        val textWidth = layout.textWidth
        val lineHeight = fontSize * layout.lineSpacing
        var current = mutableListOf<MdRun>()
        var currentWidth = 0f

        fun flush() {
            if (current.isNotEmpty()) {
                out.add(HtmlLine(runs = current, lineHeight = lineHeight))
                current = mutableListOf()
                currentWidth = 0f
            }
        }

        for (item in items) {
            when (item) {
                is InlineItem.Text -> {
                    val mdRun = MdRun(item.text, item.paint)
                    val w = mdRun.paint.measureText(mdRun.text)
                    if (currentWidth + w <= textWidth) {
                        current.add(mdRun)
                        currentWidth += w
                    } else if (w <= textWidth) {
                        flush()
                        current.add(mdRun)
                        currentWidth = w
                    } else {
                        flush()
                        for (part in breakTextWords(item.text, item.paint, textWidth)) {
                            out.add(
                                HtmlLine(
                                    runs = listOf(MdRun(part, item.paint)),
                                    lineHeight = lineHeight,
                                ),
                            )
                        }
                    }
                }
                is InlineItem.ImageItem -> {
                    flush()
                    val (iw, ih) = fitImage(item.width, item.height, layout)
                    out.add(
                        HtmlLine(
                            runs = emptyList(),
                            lineHeight = ih + fontSize + 8f,
                            image = item.bitmap,
                            imageW = iw,
                            imageH = ih,
                        ),
                    )
                }
                InlineItem.Break -> flush()
            }
        }
        flush()
        if (headingLevel > 0) {
            out.add(HtmlLine(runs = emptyList(), lineHeight = lineHeight * 0.5f))
        } else {
            out.add(HtmlLine(runs = emptyList(), lineHeight = lineHeight * 0.4f))
        }
    }

    private fun tokenizeInline(
        content: String,
        basePaint: Paint,
    ): List<InlineItem> {
        val items = mutableListOf<InlineItem>()
        val sb = StringBuilder()
        var bold = false
        var italic = false
        var underline = false

        fun currentPaint(): Paint {
            var p = basePaint
            if (bold) {
                p =
                    Paint(p).apply {
                        typeface = android.graphics.Typeface.create(p.typeface, android.graphics.Typeface.BOLD)
                    }
            }
            if (italic) p = Paint(p).apply { textSkewX = -0.25f }
            if (underline) p = Paint(p).apply { isUnderlineText = true }
            return p
        }

        fun flush() {
            if (sb.isNotEmpty()) {
                items.add(InlineItem.Text(sb.toString(), currentPaint()))
                sb.clear()
            }
        }

        var i = 0
        while (i < content.length) {
            val c = content[i]
            if (c == '<') {
                val close = content.indexOf('>', i)
                if (close < 0) {
                    sb.append(content.substring(i))
                    break
                }
                val tag = content.substring(i + 1, close).trim()
                when {
                    tag.startsWith("img") -> {
                        flush()
                        parseImageTag(tag).let { if (it != null) items.add(it) }
                    }
                    tag == "br" || tag == "br/" -> {
                        flush()
                        items.add(InlineItem.Break)
                    }
                    tag == "b" -> {
                        flush()
                        bold = true
                    }
                    tag == "/b" -> {
                        flush()
                        bold = false
                    }
                    tag == "i" -> {
                        flush()
                        italic = true
                    }
                    tag == "/i" -> {
                        flush()
                        italic = false
                    }
                    tag == "u" -> {
                        flush()
                        underline = true
                    }
                    tag == "/u" -> {
                        flush()
                        underline = false
                    }
                }
                i = close + 1
            } else if (content.startsWith("&nbsp;", i)) {
                sb.append('\u00A0')
                i += 6
            } else if (content.startsWith("&amp;", i)) {
                sb.append('&')
                i += 5
            } else if (content.startsWith("&gt;", i)) {
                sb.append('>')
                i += 4
            } else if (content.startsWith("&lt;", i)) {
                sb.append('<')
                i += 4
            } else if (content.startsWith("&quot;", i)) {
                sb.append('"')
                i += 6
            } else {
                sb.append(c)
                i++
            }
        }
        flush()
        return items.filter {
            it is InlineItem.ImageItem || it is InlineItem.Break || (it as? InlineItem.Text)?.text?.isNotBlank() == true
        }
    }

    private fun parseImageTag(tag: String): InlineItem.ImageItem? {
        val src = Regex("""src="([^"]*)"""").find(tag)?.groupValues?.get(1) ?: return null
        if (!src.startsWith("data:image/")) return null
        val comma = src.indexOf(',')
        if (comma < 0) return null
        val b64 = src.substring(comma + 1)
        val bytes =
            try {
                Base64.decode(b64, Base64.NO_WRAP)
            } catch (_: Exception) {
                return null
            }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val w = Regex("""width:(\d+(?:\.\d+)?)px""").find(tag)?.groupValues?.get(1)?.toFloatOrNull()
        val h = Regex("""height:(\d+(?:\.\d+)?)px""").find(tag)?.groupValues?.get(1)?.toFloatOrNull()
        val width = w ?: bitmap.width.toFloat()
        val height = h ?: bitmap.height.toFloat()
        return InlineItem.ImageItem(bitmap, width, height)
    }

    private fun fitImage(
        width: Float,
        height: Float,
        layout: PdfLayout,
    ): Pair<Float, Float> {
        val maxW = layout.textWidth
        val maxH = (A4_HEIGHT_PT - 2 * layout.marginTopBottomPt - 10f).coerceAtLeast(1f)
        val scale = min(1f, min(maxW / width, maxH / height))
        return (width * scale).coerceAtLeast(1f) to (height * scale).coerceAtLeast(1f)
    }

    private fun parseTable(
        content: String,
        layout: PdfLayout,
        out: MutableList<HtmlLine>,
    ) {
        val rows = mutableListOf<List<String>>()
        var i = 0
        while (i < content.length) {
            val trStart = content.indexOf("<tr", i)
            if (trStart < 0) break
            val trEnd = content.indexOf("</tr>", trStart)
            if (trEnd < 0) break
            val rowContent = content.substring(trStart, trEnd)
            val cells = mutableListOf<String>()
            var j = 0
            while (j < rowContent.length) {
                val tdStart = rowContent.indexOf("<td", j)
                if (tdStart < 0) break
                val tdEnd = rowContent.indexOf("</td>", tdStart)
                if (tdEnd < 0) break
                val gt = rowContent.indexOf('>', tdStart)
                cells.add(rowContent.substring(gt + 1, tdEnd))
                j = tdEnd + 5
            }
            rows.add(cells)
            i = trEnd + 5
        }
        if (rows.isEmpty()) return

        val colCount = rows.maxOf { it.size }.coerceAtLeast(1)
        val padding = 4f
        val border = 0.75f
        val paint = baseMdPaint(layout.fontSizePt, bold = false, font = layout.font)
        val lineHeight = layout.baseLineHeight

        val trimmedRows = rows.map { row -> row.map { it.trim() } }

        val maxWidths = FloatArray(colCount)
        for (row in trimmedRows) {
            row.forEachIndexed { ci, cell ->
                if (ci < colCount) {
                    maxWidths[ci] = max(maxWidths[ci], paint.measureText(cell))
                }
            }
        }
        val totalContent = maxWidths.sum().coerceAtLeast(1f)
        val usableWidth = layout.textWidth - (colCount - 1) * border - padding * 2 * colCount
        val rawWidths = maxWidths.map { max(24f, usableWidth * it / totalContent) }
        val totalRaw = rawWidths.sum()
        val colScale = (usableWidth / totalRaw).coerceAtMost(1f)
        val cellW = rawWidths.map { it * colScale }
        val widths = cellW.map { it + 2 * padding + border }

        for (row in trimmedRows) {
            val cellLines =
                row.mapIndexed { ci, cell ->
                    wrapRuns(
                        tokenizeInline(cell, paint)
                            .mapNotNull {
                                when (it) {
                                    is InlineItem.Text -> MdRun(it.text, it.paint)
                                    InlineItem.Break -> MdRun("\n", paint)
                                    is InlineItem.ImageItem -> null
                                }
                            },
                        cellW[ci],
                    )
                }
            val rowHeight =
                cellLines.maxOfOrNull { it.size }?.times(lineHeight)?.plus(2 * padding + 2)
                    ?: (lineHeight + 2 * padding + 2)
            out.add(
                HtmlLine(
                    runs = emptyList(),
                    lineHeight = rowHeight,
                    tableRow = TableRow(cells = cellLines, colWidths = widths, rowHeight = rowHeight),
                ),
            )
        }
        out.add(HtmlLine(runs = emptyList(), lineHeight = lineHeight * 0.4f))
    }

    private fun drawHtmlLine(
        canvas: android.graphics.Canvas,
        line: HtmlLine,
        y: Float,
        layout: PdfLayout,
    ) {
        if (line.image != null) {
            val x = layout.marginLeftRightPt
            val top = y + 4f
            val bottom = y + 4f + line.imageH
            canvas.drawBitmap(line.image, null, RectF(x, top, x + line.imageW, bottom), Paint(Paint.FILTER_BITMAP_FLAG))
            return
        }
        val tableRow = line.tableRow
        if (tableRow != null) {
            val top = y
            val borderPaint =
                Paint().apply {
                    color = 0xFF999999.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = 0.75f
                }
            var x = layout.marginLeftRightPt
            val padding = 4f
            for ((ci, cellLines) in tableRow.cells.withIndex()) {
                val w = tableRow.colWidths[ci]
                val bottom = top + tableRow.rowHeight
                canvas.drawRect(x, top, x + w, bottom, borderPaint)
                var cy = top + padding + layout.fontSizePt
                for (cellLine in cellLines) {
                    var cx = x + padding
                    for (run in cellLine) {
                        canvas.drawText(run.text, cx, cy, run.paint)
                        cx += run.paint.measureText(run.text)
                    }
                    cy += layout.baseLineHeight
                }
                x += w
            }
            return
        }
        var x = layout.marginLeftRightPt
        for (run in line.runs) {
            canvas.drawText(run.text, x, y, run.paint)
            x += run.paint.measureText(run.text)
        }
    }

    suspend fun saveTextAsFile(
        text: String,
        originalName: String,
    ): Long {
        return try {
            if (text.isBlank()) return 0L
            val txtName =
                buildTxtName(
                    originalName.ifBlank { text },
                    localizedString(R.string.file_name_text),
                )
            val txtFile = ensureUniqueFile(File(filesDir, txtName)) ?: return 0L
            try {
                FileOutputStream(txtFile).use { out ->
                    out.write(text.toByteArray(Charsets.UTF_8))
                }

                val id = System.nanoTime()
                val item =
                    ClipboardItem(
                        id = id,
                        contentType = ContentType.FILE,
                        filePath = txtFile.absolutePath,
                        previewText = extractBaseName(txtName),
                        isPinned = false,
                    )
                dao.insert(item)
                id
            } catch (_: Exception) {
                txtFile.delete()
                0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun ocrPdfToText(pdfPath: String): String? {
        return try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val pageCount = pdfPageCount(pdfPath)
            if (pageCount == 0) {
                recognizer.close()
                return null
            }
            val parts = mutableListOf<String>()
            for (i in 0 until pageCount) {
                val bitmap = renderPdfPage(pdfPath, OCR_RENDER_DIM, i) ?: continue
                val text =
                    runCatching {
                        InputImage.fromBitmap(bitmap, 0).let { img ->
                            Tasks.await(recognizer.process(img)).text
                        }
                    }.getOrNull()
                bitmap.recycle()
                if (!text.isNullOrBlank()) {
                    parts.add(if (pageCount > 1) "--- Side ${i + 1} ---\n$text" else text)
                }
            }
            recognizer.close()
            if (parts.isEmpty()) null else parts.joinToString("\n\n")
        } catch (_: Exception) {
            null
        }
    }

    suspend fun pdfToHtml(pdfPath: String): String? {
        val pdfboxHtml =
            try {
                extractPdfHtml(pdfPath)
            } catch (_: Exception) {
                null
            }
        if (!pdfboxHtml.isNullOrBlank()) return pdfboxHtml
        val ocrText = ocrPdfToText(pdfPath) ?: return null
        return plainTextToHtml(ocrText)
    }

    private fun extractPdfHtml(pdfPath: String): String? {
        val file = File(pdfPath)
        if (!file.exists()) return null
        PDDocument.load(file).use { document ->
            val extractor =
                object : PDFTextStripper() {
                    val lines = mutableListOf<PdfHtmlLine>()

                    override fun writeString(
                        text: String,
                        textPositions: List<TextPosition>,
                    ) {
                        if (text.isBlank()) return
                        var bold = 0
                        var italic = 0
                        var maxSize = 0f
                        var count = 0
                        for (tp in textPositions) {
                            val name = tp.font?.name?.lowercase() ?: ""
                            if (name.contains("bold")) bold++
                            if (name.contains("italic") || name.contains("oblique")) italic++
                            maxSize = max(maxSize, tp.fontSizeInPt)
                            count++
                        }
                        if (count == 0) return
                        lines.add(
                            PdfHtmlLine(
                                text = text,
                                y = textPositions.firstOrNull()?.y ?: 0f,
                                fontSize = maxSize,
                                bold = bold * 2 >= count,
                                italic = italic * 2 >= count,
                            ),
                        )
                    }
                }
            extractor.sortByPosition = true
            extractor.startPage = 1
            extractor.endPage = document.numberOfPages
            extractor.getText(document)
            return buildPdfHtml(extractor.lines)
        }
    }

    private fun buildPdfHtml(lines: List<PdfHtmlLine>): String {
        if (lines.isEmpty()) return ""
        val counts = lines.groupingBy { it.fontSize.roundToInt() }.eachCount()
        val bodySize = (counts.maxByOrNull { it.value }?.key ?: lines.first().fontSize.roundToInt()).toFloat()
        val unit = estimatePdfLineHeight(lines, bodySize)
        val sb = StringBuilder()
        sb.append("<html>\n<head>\n<meta charset=\"utf-8\">\n</head>\n<body>\n")
        val paragraph = StringBuilder()

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                sb.append("<p>").append(paragraph).append("</p>\n")
                paragraph.clear()
            }
        }

        fun appendLine(
            line: PdfHtmlLine,
            level: Int,
        ) {
            if (level > 0) {
                sb.append("<h").append(level).append(">")
                    .append(escapeHtml(line.text))
                    .append("</h").append(level).append(">\n")
            } else {
                if (paragraph.isNotEmpty()) paragraph.append("<br>")
                var fragment = escapeHtml(line.text)
                if (line.italic) fragment = "<i>$fragment</i>"
                if (line.bold) fragment = "<b>$fragment</b>"
                paragraph.append(fragment)
            }
        }

        var lastY: Float? = null
        for (line in lines) {
            val level = pdfHeadingLevel(line.fontSize, bodySize)
            val ratio =
                if (lastY != null) {
                    (line.y - lastY) / unit
                } else {
                    0f
                }
            lastY = line.y

            if (ratio < 1.35f) {
                if (level > 0) flushParagraph()
                appendLine(line, level)
            } else {
                val blanks = ((ratio + 0.5f).toInt() - 1).coerceIn(1, MAX_PDF_BLANK_LINES)
                flushParagraph()
                repeat(blanks - 1) { sb.append("<p>&nbsp;</p>\n") }
                appendLine(line, level)
            }
        }
        flushParagraph()
        sb.append("</body>\n</html>\n")
        return sb.toString()
    }

    private fun estimatePdfLineHeight(
        lines: List<PdfHtmlLine>,
        fallback: Float,
    ): Float {
        val safeFallback = fallback.coerceAtLeast(1f)
        val diffs =
            lines.zipWithNext()
                .mapNotNull { (a, b) -> (b.y - a.y).takeIf { it > 0.1f } }
                .sorted()
        if (diffs.isEmpty()) return safeFallback * 1.2f
        val normal = diffs.filter { it < safeFallback * 1.8f }
        val base = if (normal.isNotEmpty()) normal[normal.size / 2] else safeFallback * 1.2f
        return base.coerceIn(safeFallback * 1.0f, safeFallback * 1.6f).coerceAtLeast(1f)
    }

    private fun pdfHeadingLevel(
        size: Float,
        bodySize: Float,
    ): Int {
        if (bodySize <= 0f) return 0
        val ratio = size / bodySize
        return when {
            ratio >= 1.4f -> 1
            ratio >= 1.2f -> 2
            ratio >= 1.05f -> 3
            else -> 0
        }
    }

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    private fun plainTextToHtml(text: String): String {
        val sb = StringBuilder()
        sb.append("<html>\n<head>\n<meta charset=\"utf-8\">\n</head>\n<body>\n")
        text.trim().split(Regex("\\n\\s*\\n")).forEach { block ->
            if (block.isNotBlank()) {
                sb.append("<p>").append(escapeHtml(block.trim().replace("\n", "<br>"))).append("</p>\n")
            }
        }
        sb.append("</body>\n</html>\n")
        return sb.toString()
    }

    private data class PdfHtmlLine(
        val text: String,
        val y: Float,
        val fontSize: Float,
        val bold: Boolean,
        val italic: Boolean,
    )

    suspend fun getPdfFormFields(pdfPath: String): List<FormFieldInfo> {
        return try {
            val file = File(pdfPath)
            if (!file.exists()) return emptyList()
            PDDocument.load(file).use { document ->
                val acroForm = document.documentCatalog.acroForm ?: return emptyList()
                val runs = extractFormTextRuns(document)
                val counters = mutableMapOf<FormFieldType, Int>()
                val terminals = acroForm.fields.flatMap { field -> terminalFields(field) }
                // Checkbox rows that share their (fully qualified) name are the boxes of one
                // group; each gets its own option label and its own value key (#57).
                val checkboxCountByFqn =
                    terminals
                        .filterIsInstance<PDCheckBox>()
                        .groupingBy { it.fullyQualifiedName.orEmpty() }
                        .eachCount()
                // Every widget rectangle, by page, so a checkbox/radio label can stop at the
                // next widget instead of pulling in its neighbour's option text (#57).
                val pageWidgets = mutableMapOf<Int, MutableList<Pair<PDField, PdfUserRect>>>()
                terminals.forEach { field ->
                    field.widgets.forEach { widget ->
                        val rect = widget.rectangle ?: return@forEach
                        pageWidgets
                            .getOrPut(pageIndexOf(document, widget)) { mutableListOf() }
                            .add(
                                field to
                                    PdfUserRect(
                                        rect.lowerLeftX,
                                        rect.lowerLeftY,
                                        rect.upperRightX,
                                        rect.upperRightY,
                                    ),
                            )
                    }
                }
                val kidSlotByFqn = mutableMapOf<String, Int>()
                terminals.mapNotNull { field ->
                    val fqn = field.fullyQualifiedName.orEmpty()
                    val kidCount =
                        if (field is PDCheckBox) checkboxCountByFqn[fqn] ?: 0 else 0
                    val kidSlot =
                        if (kidCount > 1) (kidSlotByFqn[fqn] ?: 0).also { kidSlotByFqn[fqn] = it + 1 } else 0
                    toFormFieldInfo(document, field, runs, counters, kidCount, kidSlot, pageWidgets)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fillPdfForm(
        pdfPath: String,
        originalEntry: ClipboardEntry,
        values: Map<String, String>,
    ): Long {
        return try {
            val file = File(pdfPath)
            if (!file.exists()) return 0L
            val document = PDDocument.load(file)
            try {
                val acroForm = document.documentCatalog.acroForm ?: return 0L
                var applied = 0
                values.forEach { (key, value) ->
                    applied += if (applyFillValue(acroForm, key, value)) 1 else 0
                }
                if (applied == 0) return 0L
                try {
                    acroForm.flatten()
                } catch (_: Exception) {
                }
                val pdfName = buildPdfName(originalEntry.previewText)
                val pdfFile = ensureUniqueFile(File(pdfsDir, pdfName)) ?: return 0L
                try {
                    FileOutputStream(pdfFile).use { document.save(it) }

                    val id = System.nanoTime()
                    val item =
                        ClipboardItem(
                            id = id,
                            contentType = ContentType.PDF,
                            filePath = pdfFile.absolutePath,
                            previewText = extractBaseName(pdfName),
                            isPinned = false,
                        )
                    dao.insert(item)
                    id
                } catch (e: Exception) {
                    pdfFile.delete()
                    throw e
                }
            } finally {
                document.close()
            }
        } catch (_: Exception) {
            0L
        }
    }

    internal fun applyFillValue(
        acroForm: PDAcroForm,
        key: String,
        value: String,
    ): Boolean {
        val position = key.indexOf(CHECK_KID_KEY_SEPARATOR)
        if (position > 0) {
            // "Sessions\u0000Morning_session": check just that box of the group (#57).
            val option = key.substring(position + CHECK_KID_KEY_SEPARATOR.length)
            val kid =
                checkboxFieldsNamed(acroForm, key.take(position))
                    .firstOrNull { box ->
                        try {
                            box.onValue == option
                        } catch (_: Exception) {
                            false
                        }
                    }
            return try {
                if (value == "true") kid?.check() else kid?.unCheck()
                kid != null
            } catch (_: Exception) {
                false
            }
        }
        val field = acroForm.getField(key) ?: return false
        return try {
            when {
                // Radio group: /V on the parent gets the chosen export value and /AS on
                // every kid gets the chosen one or /Off, exactly what PDButton.setValue
                // (updateByValue / updateByOption) writes (#67).
                isRadioGroupField(field) -> {
                    if (value.isEmpty() || value == "false" || value.equals("Off", ignoreCase = true)) {
                        // Untouched radio rows stay as they are; the fill screen never
                        // clears a radio choice, so NoToggleToOff groups need no extra care.
                        true
                    } else {
                        field.setValue(value)
                        true
                    }
                }
                field is PDCheckBox -> {
                    if (value == "true") field.check() else field.unCheck()
                }
                else -> field.setValue(value)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * The fill-form preview page (#80): renders [pageIndex] of a temporary, filled
     * copy of the PDF at [pdfPath] whose field appearance streams were regenerated,
     * so the bitmap shows exactly what the widgets will paint — no leftover of the
     * PDF's own stale appearance, and no Compose overlay needed on top.
     *
     * The original file is never modified; the filled copy lives in the cache dir
     * and is deleted after rendering. [values] must already be preview-safe: hidden
     * passwords arrive masked as bullets so the regenerated appearance never
     * contains the secret. Saving keeps going through [fillPdfForm], whose flatten
     * path is unchanged.
     */
    suspend fun renderFilledFormPage(
        pdfPath: String,
        values: Map<String, String>,
        pageIndex: Int,
        maxDimension: Int,
    ): Bitmap? {
        return try {
            val temp = File.createTempFile("klipsilo_form_preview_", ".pdf", context.cacheDir)
            try {
                if (!fillFormCopy(pdfPath, values, temp)) return null
                renderPdfPage(temp.absolutePath, maxDimension, pageIndex)
            } finally {
                temp.delete()
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Writes a copy of the PDF at [pdfPath] with [values] filled in and every
     * field's appearance stream regenerated to [outFile]. Returns false when the
     * source is missing, unreadable or has no AcroForm. setValue already rebuilds
     * the changed field's appearance; the refresh pass afterwards regenerates the
     * rest, one field at a time so a field that cannot rebuild its appearance
     * never blocks the others (no flattening — this is the preview path, #80).
     */
    internal fun fillFormCopy(
        pdfPath: String,
        values: Map<String, String>,
        outFile: File,
    ): Boolean {
        return try {
            val file = File(pdfPath)
            if (!file.exists()) return false
            PDDocument.load(file).use { document ->
                val acroForm = document.documentCatalog.acroForm ?: return false
                values.forEach { (key, value) ->
                    applyFillValue(acroForm, key, value)
                }
                acroForm.fields
                    .flatMap { field -> terminalFields(field) }
                    .forEach { field ->
                        try {
                            acroForm.refreshAppearances(listOf(field))
                        } catch (_: Exception) {
                        }
                    }
                FileOutputStream(outFile).use { document.save(it) }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun checkboxFieldsNamed(
        acroForm: PDAcroForm,
        fqn: String,
    ): List<PDCheckBox> {
        return acroForm.fields
            .flatMap { field -> terminalFields(field) }
            .filterIsInstance<PDCheckBox>()
            .filter { (it.fullyQualifiedName ?: it.partialName) == fqn }
    }

    /**
     * True when the field is a radio group: pdfbox maps a field with the /Ff Radio flag
     * (bit 16) to [PDRadioButton], but forms from some producers leave the flag unset and
     * only express radio-ness through the widget kids, each carrying its own /AP /N on-state
     * (both testaroo test forms do this, #67). Such a field is one parent with several widget
     * kids, so it must not fill as a single checkbox.
     */
    private fun isRadioGroupField(field: PDField): Boolean {
        if (field !is PDButton || field.isPushButton) return false
        if (field is PDRadioButton) return true
        return field.widgets.size > 1 &&
            field.widgets.mapNotNull { widgetOnValue(it) }.distinct().size > 1
    }

    /**
     * The choices of a radio group in widget-kid order: the /Opt export values when the
     * producer set them, otherwise each kid's /AP /N on-state name other than /Off (#67).
     */
    private fun radioExportValues(field: PDField): List<String> {
        val exportValues =
            try {
                if (field is PDRadioButton) field.exportValues else emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        if (exportValues.isNotEmpty() && exportValues.size == field.widgets.size) return exportValues
        return field.widgets.mapNotNull { widgetOnValue(it) }
    }

    private fun toFormFieldInfo(
        document: PDDocument,
        field: PDField,
        runs: Map<Int, List<PdfTextRun>>,
        counters: MutableMap<FormFieldType, Int>,
        sameNameCheckboxCount: Int = 0,
        kidSlot: Int = 0,
        pageWidgets: Map<Int, List<Pair<PDField, PdfUserRect>>> = emptyMap(),
    ): FormFieldInfo? {
        if (field is PDButton && field.isPushButton) return null
        val partial = field.partialName ?: field.fullyQualifiedName ?: return null
        val fqn = field.fullyQualifiedName ?: partial
        val isRadioGroup = isRadioGroupField(field)
        val type =
            when (field) {
                is PDCheckBox -> if (isRadioGroup) FormFieldType.RADIO else FormFieldType.CHECKBOX
                is PDRadioButton -> FormFieldType.RADIO
                is PDComboBox, is PDListBox -> FormFieldType.CHOICE
                is PDTextField -> FormFieldType.TEXT
                else -> FormFieldType.OTHER
            }
        val options: List<String> =
            when (field) {
                is PDComboBox, is PDListBox -> field.options
                is PDRadioButton -> radioExportValues(field)
                is PDCheckBox -> if (isRadioGroup) radioExportValues(field) else emptyList()
                else -> emptyList()
            }
        var hits = fieldHits(document, field)
        // With /Opt export values the saved /V carries the export value while each kid's
        // /AS keeps its appearance-state name; align the hits so the preview compares
        // the stored value with the same names the save writes (#67).
        if (type == FormFieldType.RADIO && options.size == hits.size) {
            hits =
                hits.mapIndexed { index, hit ->
                    options.getOrNull(index)?.let { hit.copy(option = it) } ?: hit
                }
        }
        val nearby = nearbyLabel(field, document, runs)
        val tooltip = alternateName(field)
        // Checkbox and radio widgets draw their option text to the right, which is what makes
        // even separate fields like s0/s1/s2 distinguishable (#57).
        val isCheckboxKid = field is PDCheckBox && sameNameCheckboxCount > 1
        val kidGroup = field.parent?.fullyQualifiedName ?: fqn
        val resolved =
            if (type == FormFieldType.CHECKBOX || type == FormFieldType.RADIO) {
                resolveWidgetLabel(
                    nearbyRight = nearbyRightLabel(field, document, runs, pageWidgets),
                    tooltip = tooltip,
                    onState = hits.firstNotNullOfOrNull { hit -> hit.option?.takeIf { it.isNotBlank() } },
                    partialName = partial,
                    group = if (isCheckboxKid) kidGroup else null,
                    type = type,
                    fallbackNumber = 1,
                )
            } else {
                resolveFormFieldLabel(
                    partialName = partial,
                    alternateName = tooltip,
                    type = type,
                    nearbyText = nearby,
                    fallbackNumber = 1,
                )
            }
        val labelSource =
            if (resolved is ResolvedFieldLabel.Numbered) {
                val number = (counters[type] ?: 0) + 1
                counters[type] = number
                resolved.copy(number = number)
            } else {
                resolved
            }
        val label = localizedFieldLabel(labelSource)
        var multiline = false
        var isPassword = false
        var maxLength: Int? = null
        var keyboardKind = FormKeyboardKind.TEXT
        if (field is PDTextField) {
            val flags = parseTextFieldFlags(field.cosObject.getInt(COSName.FF).toLong())
            multiline = flags.multiline
            isPassword = flags.password
            maxLength = field.cosObject.getInt(COSName.MAX_LEN).takeIf { it > 0 }
            val labelForTyping = if (resolved is ResolvedFieldLabel.Literal) label else null
            keyboardKind =
                if (multiline) {
                    FormKeyboardKind.TEXT
                } else {
                    formKeyboardKind(
                        name = partial,
                        alternateName = tooltip,
                        label = labelForTyping,
                    )
                }
        }
        return FormFieldInfo(
            name = partial,
            fullyQualifiedName = field.fullyQualifiedName ?: partial,
            type = type,
            currentValue =
                if (type == FormFieldType.CHOICE) {
                    choiceValueAsString(field)
                } else {
                    field.valueAsString
                },
            options = options,
            label = label,
            hits = hits,
            multiline = multiline,
            isPassword = isPassword,
            maxLength = maxLength,
            keyboardKind = keyboardKind,
            valueKey =
                if (isCheckboxKid) {
                    val option = hits.firstNotNullOfOrNull { hit -> hit.option?.takeIf { it.isNotBlank() } }
                    "$fqn$CHECK_KID_KEY_SEPARATOR${option ?: kidSlot.toString()}"
                } else {
                    fqn
                },
        )
    }

    /**
     * A choice field's current value as a plain export value (#80): pdfbox formats
     * choice values via Arrays.toString, so a combo with /V General reports
     * "[General]" — brackets would never match an /Opt entry in the fill rows or
     * the preview, and would be drawn into the overlay verbatim.
     */
    private fun choiceValueAsString(field: PDField): String {
        val raw =
            try {
                field.valueAsString
            } catch (_: Exception) {
                return ""
            }
        val plain = raw.removePrefix("[").removeSuffix("]")
        return plain.trim().ifBlank { "" }
    }

    private fun terminalFields(field: PDField): List<PDField> {
        if (field is PDNonTerminalField) {
            val children =
                try {
                    field.children
                } catch (_: Exception) {
                    emptyList()
                }
            if (children.isNotEmpty()) return children.flatMap { child -> terminalFields(child) }
        }
        return listOf(field)
    }

    private fun alternateName(field: PDField): String? {
        field.alternateFieldName?.takeIf { it.isNotBlank() }?.let { return it }
        return field.widgets.firstNotNullOfOrNull { widget ->
            widget.cosObject.getString(COSName.TU)?.takeIf { it.isNotBlank() }
        }
    }

    private fun nearbyLabel(
        field: PDField,
        document: PDDocument,
        runs: Map<Int, List<PdfTextRun>>,
    ): String? {
        val widget = field.widgets.firstOrNull() ?: return null
        val rect = widget.rectangle ?: return null
        val pageRuns = runs[pageIndexOf(document, widget)].orEmpty()
        if (pageRuns.isEmpty()) return null
        return nearbyFieldText(
            PdfUserRect(rect.lowerLeftX, rect.lowerLeftY, rect.upperRightX, rect.upperRightY),
            pageRuns,
        )
    }

    private fun nearbyRightLabel(
        field: PDField,
        document: PDDocument,
        runs: Map<Int, List<PdfTextRun>>,
        pageWidgets: Map<Int, List<Pair<PDField, PdfUserRect>>>,
    ): String? {
        val widget = field.widgets.firstOrNull() ?: return null
        val rect = widget.rectangle ?: return null
        val self =
            PdfUserRect(rect.lowerLeftX, rect.lowerLeftY, rect.upperRightX, rect.upperRightY)
        val otherWidgets =
            pageWidgets[pageIndexOf(document, widget)].orEmpty()
                .filter { (owner, other) -> owner !== field && other != self }
                .map { (_, other) -> other }
        return nearbyFieldTextRight(
            self,
            runs[pageIndexOf(document, widget)].orEmpty(),
            otherWidgets,
        )
    }

    private fun fieldHits(
        document: PDDocument,
        field: PDField,
    ): List<FormFieldHit> {
        return field.widgets.mapNotNull { widget ->
            val rect = widget.rectangle ?: return@mapNotNull null
            val pageIndex = pageIndexOf(document, widget)
            val page = document.getPage(pageIndex)
            val box = page.cropBox ?: return@mapNotNull null
            val visual =
                widgetVisualFractions(
                    widget = PdfUserRect(rect.lowerLeftX, rect.lowerLeftY, rect.upperRightX, rect.upperRightY),
                    pageLeft = box.lowerLeftX,
                    pageBottom = box.lowerLeftY,
                    pageWidth = box.width,
                    pageHeight = box.height,
                    rotation = page.rotation,
                ) ?: return@mapNotNull null
            FormFieldHit(
                pageIndex = pageIndex,
                left = visual.left,
                top = visual.top,
                right = visual.right,
                bottom = visual.bottom,
                option = widgetOnValue(widget),
            )
        }
    }

    private fun pageIndexOf(
        document: PDDocument,
        widget: PDAnnotationWidget,
    ): Int {
        widget.page?.let { page ->
            val index = document.pages.indexOf(page)
            if (index >= 0) return index
        }
        val target = widget.cosObject
        for (index in 0 until document.numberOfPages) {
            val annotations =
                try {
                    document.getPage(index).annotations
                } catch (_: Exception) {
                    continue
                }
            if (annotations.any { it.cosObject == target }) return index
        }
        return 0
    }

    private fun widgetOnValue(widget: PDAnnotationWidget): String? {
        return try {
            val normal = widget.appearance?.normalAppearance ?: return null
            if (!normal.isSubDictionary) return null
            normal.subDictionary.keys
                .map { it.name }
                .firstOrNull { !it.equals("Off", ignoreCase = true) }
        } catch (_: Exception) {
            null
        }
    }

    private fun localizedFieldLabel(resolved: ResolvedFieldLabel): String {
        return when (resolved) {
            is ResolvedFieldLabel.Literal -> resolved.text
            is ResolvedFieldLabel.Numbered -> {
                val res =
                    when (resolved.kind) {
                        FormFieldType.CHECKBOX -> R.string.form_field_checkbox
                        FormFieldType.RADIO -> R.string.form_field_radio
                        FormFieldType.CHOICE -> R.string.form_field_choice
                        else -> R.string.form_field_text
                    }
                context.getString(res, resolved.number)
            }
        }
    }

    private fun extractFormTextRuns(document: PDDocument): Map<Int, List<PdfTextRun>> {
        return try {
            val found = mutableMapOf<Int, MutableList<PdfTextRun>>()
            val stripper =
                object : PDFTextStripper() {
                    override fun writeString(
                        text: String,
                        textPositions: List<TextPosition>,
                    ) {
                        val value = text.trim()
                        val positions = textPositions
                        if (value.isEmpty() || positions.isEmpty()) return
                        val first = positions.first()
                        val matrix = first.textMatrix
                        val left = positions.minOf { it.textMatrix.translateX }
                        val right = positions.maxOf { max(it.endX, it.textMatrix.translateX) }
                        found.getOrPut(currentPageNo - 1) { mutableListOf() }.add(
                            PdfTextRun(
                                text = value,
                                left = left,
                                baseline = matrix.translateY,
                                width = (right - left).coerceAtLeast(1f),
                                height = positions.maxOf { it.height }.coerceAtLeast(8f),
                            ),
                        )
                    }
                }
            stripper.sortByPosition = true
            stripper.startPage = 1
            stripper.endPage = document.numberOfPages
            stripper.getText(document)
            found
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun ocrPdfPage(
        pdfPath: String,
        pageIndex: Int,
    ): String? {
        return try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val bitmap =
                renderPdfPage(pdfPath, OCR_RENDER_DIM, pageIndex)
                    ?: run {
                        recognizer.close()
                        return null
                    }
            val text =
                runCatching {
                    InputImage.fromBitmap(bitmap, 0).let { img ->
                        Tasks.await(recognizer.process(img)).text
                    }
                }.getOrNull()
            bitmap.recycle()
            recognizer.close()
            text?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun scanImageForQr(filePath: String): List<String> {
        return try {
            val bitmap = loadRotatedBitmap(filePath) ?: return emptyList()
            val result = scanBitmapForQr(bitmap)
            bitmap.recycle()
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun scanPdfPageForQr(
        pdfPath: String,
        pageIndex: Int,
    ): List<String> {
        return try {
            val bitmap = renderPdfPage(pdfPath, OCR_RENDER_DIM, pageIndex) ?: return emptyList()
            val result = scanBitmapForQr(bitmap)
            bitmap.recycle()
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun scanPdfForLinks(pdfPath: String): List<ScannedItem> {
        return try {
            val pageCount = pdfPageCount(pdfPath)
            val maxPages = minOf(pageCount, 20)
            val items = mutableListOf<ScannedItem>()
            for (i in 0 until maxPages) {
                val bitmap = renderPdfPage(pdfPath, OCR_RENDER_DIM, i) ?: continue
                items += scanBitmapForLinks(bitmap)
                bitmap.recycle()
            }
            items.distinctBy { it.value to it.isPhone }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun createPdfFromBitmap(bitmap: Bitmap): ByteArray {
        val document = PdfDocument()
        val pageInfo =
            PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = document.startPage(pageInfo)
        bitmap.density = 72
        page.canvas.drawBitmap(
            bitmap,
            android.graphics.Rect(0, 0, bitmap.width, bitmap.height),
            android.graphics.RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
            null,
        )
        document.finishPage(page)
        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        return out.toByteArray()
    }

    suspend fun saveLocation(
        latitude: Double,
        longitude: Double,
    ): Long {
        val id = System.nanoTime()
        val coordText = "%.6f, %.6f".format(Locale.US, latitude, longitude)
        val item =
            ClipboardItem(
                id = id,
                contentType = ContentType.LOCATION,
                previewText = coordText,
                latitude = latitude,
                longitude = longitude,
            )
        dao.insert(item)
        val previewText = lookupAddress(latitude, longitude)
        if (previewText != coordText) {
            dao.update(item.copy(previewText = previewText))
        }
        return id
    }

    private suspend fun lookupAddress(
        latitude: Double,
        longitude: Double,
    ): String {
        val coordText = "%.6f, %.6f".format(Locale.US, latitude, longitude)
        if (!Geocoder.isPresent()) return coordText
        return try {
            val geocoder = Geocoder(context)
            val addresses =
                withTimeoutOrNull(3_000) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine { cont ->
                            geocoder.getFromLocation(
                                latitude,
                                longitude,
                                1,
                                object : Geocoder.GeocodeListener {
                                    override fun onGeocode(addresses: MutableList<android.location.Address>) {
                                        if (cont.isActive) cont.resume(addresses)
                                    }

                                    override fun onError(errorMessage: String?) {
                                        if (cont.isActive) cont.resume(emptyList())
                                    }
                                },
                            )
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(latitude, longitude, 1).orEmpty()
                    }
                }.orEmpty()
            if (addresses.isEmpty()) {
                coordText
            } else {
                val a = addresses[0]
                listOfNotNull(
                    a.thoroughfare,
                    a.featureName,
                    a.locality,
                ).joinToString(", ").takeUnless { it.isBlank() } ?: coordText
            }
        } catch (_: Exception) {
            coordText
        }
    }

    fun copyToClipboard(entry: ClipboardEntry): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipDataForCopy(entry) ?: return false
            clipboard.setPrimaryClip(clipData)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareText(text: String): Boolean {
        if (text.isBlank()) return false
        return startShareChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text.withoutSoftHyphens())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    }

    fun shareClip(entry: ClipboardEntry): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND)
            var sharedUri: Uri? = null
            when (entry.contentType) {
                ContentType.TEXT, ContentType.URL -> {
                    val body = textBodyBlocking(entry) ?: return false
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, body.withoutSoftHyphens())
                }
                ContentType.IMAGE -> {
                    val imageFile = entry.filePath?.let { File(it) }
                    if (imageFile?.exists() != true) return false
                    sharedUri = fileProviderUri(imageFile)
                    intent.type = "image/*"
                    intent.putExtra(Intent.EXTRA_STREAM, sharedUri)
                }
                ContentType.PDF -> {
                    val pdfFile = entry.filePath?.let { File(it) }
                    if (pdfFile?.exists() != true) return false
                    sharedUri = fileProviderUri(pdfFile)
                    intent.type = "application/pdf"
                    intent.putExtra(Intent.EXTRA_STREAM, sharedUri)
                }
                ContentType.FILE -> {
                    val file = entry.filePath?.let { File(it) }
                    if (file?.exists() != true) return false
                    sharedUri = fileProviderUri(file)
                    intent.type =
                        java.net.URLConnection.guessContentTypeFromName(file.name)
                            ?: "application/octet-stream"
                    intent.putExtra(Intent.EXTRA_STREAM, sharedUri)
                }
                ContentType.LOCATION -> {
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, locationShareText(entry).withoutSoftHyphens())
                }
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            sharedUri?.let { attachGrantClipData(intent, listOf(it)) }
            startShareChooser(intent)
        } catch (_: Exception) {
            false
        }
    }

    fun shareMultipleClips(entries: List<ClipboardEntry>): Boolean {
        if (entries.isEmpty()) return false
        if (entries.size == 1) return shareClip(entries.first())
        return try {
            val firstType = entries.first().contentType
            val intent = Intent()
            when (firstType) {
                ContentType.TEXT, ContentType.URL -> {
                    val parts = entries.mapNotNull { textBodyBlocking(it) }
                    if (parts.isEmpty()) return false
                    intent.action = Intent.ACTION_SEND
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, parts.joinToString("\n\n").withoutSoftHyphens())
                }
                ContentType.LOCATION -> {
                    val combined = entries.joinToString("\n\n") { locationShareText(it) }
                    if (combined.isBlank()) return false
                    intent.action = Intent.ACTION_SEND
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, combined.withoutSoftHyphens())
                }
                ContentType.IMAGE, ContentType.PDF, ContentType.FILE -> {
                    val uris = fileProviderUris(entries)
                    if (uris.isEmpty()) return false
                    intent.action = Intent.ACTION_SEND_MULTIPLE
                    intent.type =
                        when (firstType) {
                            ContentType.IMAGE -> "image/*"
                            ContentType.PDF -> "application/pdf"
                            else -> "application/octet-stream"
                        }
                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    attachGrantClipData(intent, uris)
                }
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startShareChooser(intent)
        } catch (_: Exception) {
            false
        }
    }

    private fun clipDataForCopy(entry: ClipboardEntry): ClipData? {
        return when (entry.contentType) {
            ContentType.TEXT, ContentType.URL -> {
                val body = textBodyBlocking(entry) ?: return null
                ClipData.newPlainText("snip", body.withoutSoftHyphens())
            }
            ContentType.IMAGE -> {
                val imageFile = existingClipFile(entry) ?: return null
                ClipData.newUri(context.contentResolver, "snip_image", fileProviderUri(imageFile))
            }
            ContentType.PDF -> {
                val pdfFile = existingClipFile(entry) ?: return null
                ClipData.newUri(context.contentResolver, "snip_pdf", fileProviderUri(pdfFile))
            }
            ContentType.FILE -> {
                val file = existingClipFile(entry) ?: return null
                ClipData.newUri(context.contentResolver, "snip_file", fileProviderUri(file))
            }
            ContentType.LOCATION -> ClipData.newPlainText("snip", locationShareText(entry).withoutSoftHyphens())
        }
    }

    private fun existingClipFile(entry: ClipboardEntry): File? {
        val file = entry.filePath?.let { File(it) }
        return file?.takeIf { it.exists() }
    }

    private fun fileProviderUri(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun locationShareText(entry: ClipboardEntry): String {
        val lat = entry.latitude
        val lng = entry.longitude
        return if (lat != null && lng != null) {
            "${entry.previewText}\ngeo:$lat,$lng"
        } else {
            entry.previewText
        }
    }

    private fun startShareChooser(intent: Intent): Boolean {
        return try {
            val chooser = Intent.createChooser(intent, context.getString(com.phaprodevelop.klipsilo.R.string.share))
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun copyFileToStorage(
        sourceUri: Uri,
        destDir: File,
        prefix: String,
    ): CopyResult {
        var destFile: File? = null
        return try {
            if (uriExceedsImportLimit(sourceUri)) return CopyResult.TooLarge
            val originalName =
                resolveDisplayName(sourceUri)
                    ?: sourceUri.lastPathSegment?.let { Uri.decode(it) }
                    ?: ""
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return CopyResult.Failed
            inputStream.use { stream ->
                val header = readHeader(stream)
                val ext = detectExtension(header)
                val fileName = buildFileName(originalName, prefix, ext)
                val created = ensureUniqueFile(File(destDir, fileName)) ?: return CopyResult.Failed
                destFile = created
                writeStreamLimited(stream, created, header)
            }
        } catch (_: Exception) {
            destFile?.delete()
            CopyResult.Failed
        }
    }

    private fun copyFileRawToStorage(
        sourceUri: Uri,
        destDir: File,
        prefix: String,
    ): CopyResult {
        var destFile: File? = null
        return try {
            if (uriExceedsImportLimit(sourceUri)) return CopyResult.TooLarge
            val mimeType =
                try {
                    context.contentResolver.getType(sourceUri)
                } catch (_: Exception) {
                    null
                }
            val displayName =
                resolveDisplayName(sourceUri)
                    ?: sourceUri.lastPathSegment?.let { Uri.decode(it) }
                    ?: "${prefix}${localizedString(R.string.file_name_file)}"
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return CopyResult.Failed
            inputStream.use { stream ->
                val header = readHeader(stream)
                var base =
                    extractBaseName(displayName)
                        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                        .take(100)
                        .ifBlank { "${prefix}${localizedString(R.string.file_name_file)}" }
                base = base.replace(Regex("_\\d{8,}$"), "")
                val hasExt = displayName.lastIndexOf('.') > 0
                var ext = if (hasExt) displayName.substringAfterLast('.', "").lowercase() else ""
                if (ext.isBlank()) {
                    ext = detectExtension(header)?.removePrefix(".") ?: ""
                }
                if (ext.isBlank()) {
                    ext = mimeType?.let { mimeToExtension(it) } ?: ""
                }
                val fileName = if (ext.isNotBlank()) "$base.$ext" else base
                val created = ensureUniqueFile(File(destDir, fileName)) ?: return CopyResult.Failed
                destFile = created
                writeStreamLimited(stream, created, header)
            }
        } catch (_: Exception) {
            destFile?.delete()
            CopyResult.Failed
        }
    }

    private fun uriExceedsImportLimit(uri: Uri): Boolean {
        val size = queryUriSize(uri) ?: return false
        return size > MAX_IMPORT_BYTES
    }

    /**
     * Display name and size in bytes of the shared file behind [uri], used to detect duplicates.
     * Returns null when either piece is unknown, blank or non-positive.
     */
    fun sharedFileIdentity(uri: Uri): Pair<String, Long>? {
        val displayName =
            resolveDisplayName(uri)
                ?: uri.lastPathSegment?.let { Uri.decode(it) }
                ?: return null
        val size = queryUriSize(uri) ?: fileSizeFromPath(uri)
        if (displayName.isBlank() || size == null || size <= 0L) return null
        return displayName to size
    }

    private fun fileSizeFromPath(uri: Uri): Long? {
        if (uri.scheme != "file") return null
        val path = uri.path ?: return null
        val length = File(path).length()
        return length.takeIf { it > 0 }
    }

    suspend fun findDuplicateSharedFile(
        displayName: String,
        sizeBytes: Long,
    ): ClipboardEntry? = findDuplicateFileClip(listClips(), displayName, sizeBytes)

    private fun queryUriSize(uri: Uri): Long? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx < 0) return@use null
                cursor.getLong(idx).takeIf { it > 0 }
            }
        } catch (_: Exception) {
            null
        }
    }

    internal fun writeStreamLimited(
        stream: InputStream,
        destFile: File,
        header: HeaderResult,
        maxBytes: Long = MAX_IMPORT_BYTES,
    ): CopyResult {
        var copied = header.bytesRead.toLong()
        if (copied > maxBytes) {
            destFile.delete()
            return CopyResult.TooLarge
        }
        return try {
            var tooLarge = false
            FileOutputStream(destFile).use { output ->
                output.write(header.data, 0, header.bytesRead)
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val n = stream.read(buffer)
                    if (n < 0) break
                    copied += n
                    if (copied > maxBytes) {
                        tooLarge = true
                        break
                    }
                    output.write(buffer, 0, n)
                }
            }
            if (tooLarge) {
                destFile.delete()
                CopyResult.TooLarge
            } else {
                CopyResult.Ok(destFile.absolutePath)
            }
        } catch (e: Exception) {
            destFile.delete()
            throw e
        }
    }

    private fun readLimitedBytes(
        stream: InputStream,
        maxBytes: Long,
    ): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copied = 0L
        while (true) {
            val n = stream.read(buffer)
            if (n < 0) break
            copied += n
            if (copied > maxBytes) return null
            output.write(buffer, 0, n)
        }
        return output.toByteArray()
    }

    private fun copyZipEntryLimited(
        stream: InputStream,
        destFile: File,
        maxBytes: Long,
    ): Long {
        if (maxBytes <= 0L) return -1L
        var written = 0L
        var tooLarge = false
        FileOutputStream(destFile).use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                written += n
                if (written > maxBytes) {
                    tooLarge = true
                    break
                }
                output.write(buffer, 0, n)
            }
        }
        return if (tooLarge) -1L else written
    }

    private fun mimeToExtension(mime: String): String {
        return when {
            mime.startsWith("text/plain") -> "txt"
            mime == "text/rtf" || mime == "application/rtf" -> "rtf"
            mime == "application/msword" -> "doc"
            mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
            mime == "application/vnd.ms-excel" -> "xls"
            mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
            mime == "application/vnd.ms-powerpoint" -> "ppt"
            mime == "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx"
            mime == "application/pdf" -> "pdf"
            mime == "text/html" -> "html"
            mime == "text/csv" -> "csv"
            mime == "application/json" -> "json"
            mime == "application/xml" || mime == "text/xml" -> "xml"
            mime == "text/markdown" -> "md"
            mime == "application/vnd.android.package-archive" -> "apk"
            mime == "application/zip" || mime == "application/x-zip-compressed" -> "zip"
            mime.startsWith("image/") -> "jpg"
            else -> ""
        }
    }

    private fun hasPlausibleFileExtension(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext.length in 1..5 && ext.all { it.isLetterOrDigit() } && ext !in DOWNLOAD_EXT_BLACKLIST
    }

    private fun resolveDownloadExtension(
        nameHint: String,
        contentType: String,
        detected: String?,
        defaultExt: String,
    ): String {
        if (hasPlausibleFileExtension(nameHint)) return nameHint.substringAfterLast('.', "").lowercase()
        android.webkit.MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(contentType)
            ?.takeIf { it.isNotBlank() }
            ?.let { return it.lowercase() }
        mimeToExtension(contentType).takeIf { it.isNotBlank() }?.let { return it }
        if (detected != null) return detected.removePrefix(".")
        return defaultExt
    }

    internal fun ensureUniqueFile(file: File): File? {
        if (tryCreateNewFile(file)) return file
        val name = file.name
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        val parent = file.parent
        var count = 1
        while (count <= MAX_FILE_UNIQUIFIER) {
            val candidate = File(parent, "${base}_${count}$ext")
            if (tryCreateNewFile(candidate)) return candidate
            count++
        }
        return null
    }

    private fun tryCreateNewFile(file: File): Boolean {
        return try {
            val parent = file.parentFile
            if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory) {
                return false
            }
            file.createNewFile()
        } catch (_: Exception) {
            false
        }
    }

    private fun takeUniqueRenameTarget(desired: File): File? {
        val reserved = ensureUniqueFile(desired) ?: return null
        return if (reserved.delete()) reserved else null
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columns = listOf(OpenableColumns.DISPLAY_NAME, "_display_name")
                    for (col in columns) {
                        val idx = it.getColumnIndex(col)
                        if (idx >= 0) {
                            val name = it.getString(idx)
                            if (!name.isNullOrBlank()) return name
                        }
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun copyToMediaStore(imageFile: File): Uri? {
        val contentValues =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.name)
                put(MediaStore.Images.Media.MIME_TYPE, getMimeTypeForFile(imageFile))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
        val uri =
            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues,
            ) ?: return null
        return try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                imageFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw java.io.IOException("Could not open output stream")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
            uri
        } catch (_: Exception) {
            context.contentResolver.delete(uri, null, null)
            null
        }
    }

    private fun getMimeTypeForFile(file: File): String {
        val ext = file.extension.lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            "heic", "heif", "heix" -> "image/heic"
            else -> "image/*"
        }
    }

    private fun localizedString(resId: Int): String = LocaleHelper.applyLocale(context).getString(resId)

    private fun fileProviderUris(entries: List<ClipboardEntry>): ArrayList<Uri> {
        val uris = ArrayList<Uri>()
        entries.forEach { entry ->
            entry.filePath?.let { File(it) }?.let { file ->
                if (file.exists()) {
                    uris.add(fileProviderUri(file))
                }
            }
        }
        return uris
    }

    private fun attachGrantClipData(
        intent: Intent,
        uris: List<Uri>,
    ) {
        if (uris.isEmpty()) return
        val clip = ClipData.newUri(context.contentResolver, "snip", uris.first())
        for (i in 1 until uris.size) {
            clip.addItem(ClipData.Item(uris[i]))
        }
        intent.clipData = clip
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun copyToPublicDownloads(
        file: File,
        displayName: String,
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            copyToMediaStoreDownload(file, displayName)
        } else {
            copyToLegacyDownloads(file, displayName)
        }
    }

    private fun copyToLegacyDownloads(
        file: File,
        displayName: String,
    ): Uri? {
        return try {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloads.exists() && !downloads.mkdirs()) return null
            val dest = ensureUniqueFile(File(downloads, displayName)) ?: return null
            try {
                file.inputStream().use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output)
                    }
                }
                MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), null, null)
                Uri.fromFile(dest)
            } catch (_: Exception) {
                dest.delete()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun copyToMediaStoreDownload(
        file: File,
        displayName: String = file.name,
    ): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val contentValues =
            ContentValues().apply {
                put(
                    MediaStore.Downloads.DISPLAY_NAME,
                    displayName,
                )
                put(
                    MediaStore.Downloads.MIME_TYPE,
                    java.net.URLConnection.guessContentTypeFromName(displayName)
                        ?: java.net.URLConnection.guessContentTypeFromName(file.name)
                        ?: "application/octet-stream",
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }
        val uri =
            context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues,
            ) ?: return null
        return try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw java.io.IOException("Could not open output stream")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
            uri
        } catch (_: Exception) {
            context.contentResolver.delete(uri, null, null)
            null
        }
    }

    suspend fun saveFileToDownloads(entry: ClipboardEntry): Boolean {
        return try {
            val file = entry.filePath?.let { File(it) }
            if (file?.exists() != true) return false
            val base = extractBaseName(entry.previewText).ifBlank { extractBaseName(file.name) }
            var ext = file.extension.lowercase()
            if (ext.isBlank() && !isProbablyBinary(file)) {
                ext = "txt"
            }
            val displayName = if (ext.isNotBlank()) "$base.$ext" else base
            copyToPublicDownloads(file, displayName) != null
        } catch (_: Exception) {
            false
        }
    }

    suspend fun backupClipsToZip(
        uri: Uri,
        entries: List<ClipboardEntry>,
    ): BackupResult {
        return try {
            var missingFiles = 0
            val output = context.contentResolver.openOutputStream(uri) ?: return BackupResult(0, 0)
            ZipOutputStream(BufferedOutputStream(output)).use { zos ->
                val manifest = JSONObject()
                manifest.put("app", BACKUP_APP_NAME)
                manifest.put("version", BACKUP_FORMAT_VERSION)
                manifest.put("exported", System.currentTimeMillis())
                val clipsArray = JSONArray()
                entries.forEachIndexed { index, entry ->
                    val obj = JSONObject()
                    obj.put("id", entry.id)
                    obj.put("contentType", entry.contentType.name)
                    obj.put("previewText", entry.previewText)
                    obj.put("timestamp", entry.timestamp)
                    obj.put("isPinned", entry.isPinned)
                    obj.put("isLocked", entry.isLocked)
                    entry.latitude?.let { obj.put("latitude", it) }
                    entry.longitude?.let { obj.put("longitude", it) }
                    if (isFileBackedClip(entry.contentType, entry.filePath)) {
                        val file = entry.filePath?.let { File(it) }
                        if (file?.exists() == true) {
                            val fileName = "files/${index}_${sanitizeZipName(file.name)}"
                            obj.put("fileName", fileName)
                            writeZipEntry(zos, fileName, file)
                        } else {
                            missingFiles++
                        }
                    } else {
                        val body =
                            entry.textContent
                                ?: dao.getById(entry.id)?.textContent
                        body?.let { obj.put("textContent", it) }
                    }
                    clipsArray.put(obj)
                }
                manifest.put("clips", clipsArray)
                writeZipEntry(zos, BACKUP_MANIFEST_NAME, manifest.toString().toByteArray(Charsets.UTF_8))
            }
            BackupResult(entries.size, missingFiles)
        } catch (_: Exception) {
            BackupResult(0, 0)
        }
    }

    suspend fun restoreClipsFromZip(uri: Uri): RestoreResult {
        return restoreMutex.withLock {
            restoreClipsFromZipLocked(uri)
        }
    }

    private suspend fun restoreClipsFromZipLocked(uri: Uri): RestoreResult {
        val input = context.contentResolver.openInputStream(uri) ?: return RestoreResult(0, 0, 0)
        val tempDir = File(context.cacheDir, "backup_import_${System.nanoTime()}")
        tempDir.mkdirs()
        val copiedDests = mutableListOf<File>()
        return try {
            var manifestJson: JSONObject? = null
            ZipInputStream(BufferedInputStream(input)).use { zis ->
                var entry = zis.nextEntry
                var uncompressedTotal = 0L
                while (entry != null) {
                    val name = entry.name
                    if (isBackupManifestName(name)) {
                        val bytes =
                            readLimitedBytes(zis, MAX_IMPORT_BYTES)
                                ?: return RestoreResult(0, 0, 0, invalid = true)
                        uncompressedTotal += bytes.size
                        manifestJson = JSONObject(bytes.toString(Charsets.UTF_8))
                    } else if (name.startsWith("files/") && !entry.isDirectory) {
                        val base = name.substringAfterLast('/').replace("..", "")
                        if (base.isNotBlank()) {
                            val remaining = MAX_RESTORE_TOTAL_BYTES - uncompressedTotal
                            val dest = File(tempDir, base)
                            val written = copyZipEntryLimited(zis, dest, min(MAX_IMPORT_BYTES, remaining))
                            if (written < 0) {
                                dest.delete()
                            } else {
                                uncompressedTotal += written
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            val manifest = manifestJson ?: return RestoreResult(0, 0, 0, invalid = true)
            if (!isKnownBackupApp(manifest.optString("app", ""))) {
                return RestoreResult(0, 0, 0, invalid = true)
            }
            if (manifest.optInt("version", 0) > BACKUP_FORMAT_VERSION) {
                return RestoreResult(0, 0, 0, invalid = true)
            }
            val clipsArray = manifest.optJSONArray("clips") ?: return RestoreResult(0, 0, 0, invalid = true)
            val total = clipsArray.length()
            var loaded = 0
            var skipped = 0
            val items = mutableListOf<ClipboardItem>()
            for (i in 0 until total) {
                val obj = clipsArray.optJSONObject(i)
                if (obj == null) {
                    skipped++
                    continue
                }
                // Restoring the same ZIP again must not duplicate clips (#73): the backup
                // keeps the original id, so an existing row with that id means this clip
                // was already restored (or still exists). Skip it before copying any file.
                val backupId = obj.optLong("id", 0L)
                if (backupId > 0L && dao.getById(backupId) != null) {
                    skipped++
                    continue
                }
                val contentType =
                    runCatching { ContentType.valueOf(obj.optString("contentType")) }.getOrNull()
                if (contentType == null) {
                    skipped++
                    continue
                }
                val fileName = obj.optString("fileName", "")
                val fileBased = shouldRestoreAsFile(contentType, fileName)
                var filePath: String? = null
                if (fileBased) {
                    val tempFile =
                        if (fileName.startsWith("files/")) {
                            File(tempDir, fileName.substringAfterLast('/'))
                        } else {
                            null
                        }
                    if (tempFile == null || !tempFile.exists()) {
                        skipped++
                        continue
                    }
                    val destDir =
                        when (contentType) {
                            ContentType.IMAGE -> imagesDir
                            ContentType.PDF -> pdfsDir
                            else -> filesDir
                        }
                    val destFile = ensureUniqueFile(File(destDir, tempFile.name))
                    if (destFile == null) {
                        skipped++
                        continue
                    }
                    copiedDests.add(destFile)
                    tempFile.inputStream().use { inputStream ->
                        destFile.outputStream().use { output ->
                            inputStream.copyTo(output)
                        }
                    }
                    filePath = destFile.absolutePath
                }
                items.add(
                    ClipboardItem(
                        id = if (backupId > 0L) backupId else System.nanoTime() + i,
                        contentType = contentType,
                        textContent =
                            obj.optString("textContent", "")
                                .ifBlank { null }
                                ?.take(MAX_TEXT_VIEW_BYTES.toInt()),
                        filePath = filePath,
                        previewText = obj.optString("previewText", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isPinned = obj.optBoolean("isPinned", false),
                        isLocked = obj.optBoolean("isLocked", false),
                        latitude =
                            if (obj.has("latitude") && !obj.isNull("latitude")) {
                                obj.getDouble("latitude")
                            } else {
                                null
                            },
                        longitude =
                            if (obj.has("longitude") && !obj.isNull("longitude")) {
                                obj.getDouble("longitude")
                            } else {
                                null
                            },
                    ),
                )
                loaded++
            }
            if (items.isNotEmpty()) {
                dao.insertAll(items)
            }
            RestoreResult(loaded, skipped, total)
        } catch (_: Exception) {
            copiedDests.forEach { dest ->
                try {
                    dest.delete()
                } catch (_: Exception) {
                }
            }
            RestoreResult(0, 0, 0)
        } finally {
            try {
                input.close()
            } catch (_: Exception) {
            }
            tempDir.deleteRecursively()
        }
    }

    private fun writeZipEntry(
        zos: ZipOutputStream,
        name: String,
        bytes: ByteArray,
    ) {
        zos.putNextEntry(ZipEntry(name))
        zos.write(bytes)
        zos.closeEntry()
    }

    private fun writeZipEntry(
        zos: ZipOutputStream,
        name: String,
        file: File,
    ) {
        zos.putNextEntry(ZipEntry(name))
        FileInputStream(file).use { it.copyTo(zos) }
        zos.closeEntry()
    }

    private fun sanitizeZipName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(100).ifBlank { "file" }

    private fun ClipboardItem.toEntry() =
        ClipboardEntry(
            id = id,
            contentType = contentType,
            textContent = textContent,
            filePath = filePath,
            previewText = previewText,
            timestamp = timestamp,
            isPinned = isPinned,
            isLocked = isLocked,
            latitude = latitude,
            longitude = longitude,
        )

    private fun ClipboardItemListRow.toEntry() =
        ClipboardEntry(
            id = id,
            contentType = contentType,
            textContent = null,
            filePath = filePath,
            previewText = previewText,
            timestamp = timestamp,
            isPinned = isPinned,
            isLocked = isLocked,
            latitude = latitude,
            longitude = longitude,
        )

    private fun persistTextualEntry(entry: ClipboardEntry): ClipboardItem? {
        val textual =
            entry.contentType == ContentType.TEXT ||
                entry.contentType == ContentType.URL
        if (!textual || entry.filePath != null) {
            return ClipboardItem(
                id = entry.id,
                contentType = entry.contentType,
                textContent = entry.textContent,
                filePath = entry.filePath,
                previewText = entry.previewText,
                timestamp = entry.timestamp,
                isPinned = entry.isPinned,
                isLocked = entry.isLocked,
                latitude = entry.latitude,
                longitude = entry.longitude,
            )
        }
        val text = entry.textContent
        if (text == null) {
            return ClipboardItem(
                id = entry.id,
                contentType = entry.contentType,
                textContent = null,
                filePath = null,
                previewText = entry.previewText,
                timestamp = entry.timestamp,
                isPinned = entry.isPinned,
                isLocked = entry.isLocked,
                latitude = entry.latitude,
                longitude = entry.longitude,
            )
        }
        return when (val persisted = persistTextBody(text)) {
            PersistTextResult.TooLarge, PersistTextResult.Failed -> null
            is PersistTextResult.Ok ->
                ClipboardItem(
                    id = entry.id,
                    contentType = entry.contentType,
                    textContent = persisted.textContent,
                    filePath = persisted.filePath,
                    previewText = entry.previewText,
                    timestamp = entry.timestamp,
                    isPinned = entry.isPinned,
                    isLocked = entry.isLocked,
                    latitude = entry.latitude,
                    longitude = entry.longitude,
                )
        }
    }

    private fun persistTextBody(text: String): PersistTextResult {
        val byteCount = text.toByteArray(Charsets.UTF_8).size.toLong()
        if (byteCount > MAX_TEXT_VIEW_BYTES) return PersistTextResult.TooLarge
        if (byteCount <= INLINE_TEXT_MAX_BYTES) {
            return PersistTextResult.Ok(textContent = text, filePath = null)
        }
        val dest = ensureUniqueFile(File(filesDir, "clip.txt")) ?: return PersistTextResult.Failed
        return try {
            dest.writeText(text, Charsets.UTF_8)
            PersistTextResult.Ok(textContent = null, filePath = dest.absolutePath)
        } catch (_: Exception) {
            dest.delete()
            PersistTextResult.Failed
        }
    }

    private fun textBodyBlocking(entry: ClipboardEntry): String? = runBlocking { readTextBody(entry) }

    private fun readCappedUtf8File(path: String): String? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            if (file.length() > MAX_TEXT_VIEW_BYTES) return null
            file.readText(Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun isFileBackedClip(
        contentType: ContentType,
        filePath: String?,
    ): Boolean {
        return when (contentType) {
            ContentType.IMAGE, ContentType.PDF, ContentType.FILE -> true
            ContentType.TEXT, ContentType.URL -> filePath != null
            ContentType.LOCATION -> false
        }
    }

    private fun shouldRestoreAsFile(
        contentType: ContentType,
        fileName: String,
    ): Boolean {
        if (!fileName.startsWith("files/")) return false
        return when (contentType) {
            ContentType.IMAGE, ContentType.PDF, ContentType.FILE, ContentType.TEXT, ContentType.URL -> true
            ContentType.LOCATION -> false
        }
    }
}

private sealed class PersistTextResult {
    data class Ok(
        val textContent: String?,
        val filePath: String?,
    ) : PersistTextResult()

    data object TooLarge : PersistTextResult()

    data object Failed : PersistTextResult()
}

private fun scaleDownIfNeeded(
    bitmap: Bitmap,
    maxOutputPx: Int,
): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    val maxDim = max(w, h)
    if (maxDim <= maxOutputPx) return bitmap
    val scale = maxOutputPx.toFloat() / maxDim
    return Bitmap.createScaledBitmap(
        bitmap,
        (w * scale).toInt(),
        (h * scale).toInt(),
        true,
    )
}

private const val A4_WIDTH_PT = 595
internal const val MAX_TEXT_VIEW_BYTES = 8L * 1024L * 1024L
private const val A4_HEIGHT_PT = 842
private const val EMU_PER_PT = 12700.0
private const val OCR_RENDER_DIM = 2400
private const val MAX_PDF_BLANK_LINES = 20

private const val BACKUP_APP_NAME = "KlipSilo"
private const val BACKUP_APP_NAME_LEGACY = "Snipdock"
private const val BACKUP_MANIFEST_NAME = "klipsilo_manifest.json"
private const val BACKUP_MANIFEST_NAME_LEGACY = "snipdock_manifest.json"
private const val BACKUP_FORMAT_VERSION = 1

private fun isBackupManifestName(name: String): Boolean =
    name == BACKUP_MANIFEST_NAME || name == BACKUP_MANIFEST_NAME_LEGACY

private fun isKnownBackupApp(app: String): Boolean = app == BACKUP_APP_NAME || app == BACKUP_APP_NAME_LEGACY

data class BackupResult(
    val exported: Int,
    val missingFiles: Int,
)

data class RestoreResult(
    val loaded: Int,
    val skipped: Int,
    val total: Int,
    val invalid: Boolean = false,
)

enum class SaveClipResult {
    Saved,
    TooLarge,
    Failed,
}

internal sealed class CopyResult {
    data class Ok(val path: String) : CopyResult()

    data object TooLarge : CopyResult()

    data object Failed : CopyResult()
}

enum class FormFieldType {
    TEXT,
    CHECKBOX,
    RADIO,
    CHOICE,
    OTHER,
}

/**
 * Joins the group name and the option value for checkbox children that share one field name,
 * so each box keeps its own entry in the value map and in the saved PDF.
 */
const val CHECK_KID_KEY_SEPARATOR = "\u0000"

data class FormFieldHit(
    val pageIndex: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val option: String? = null,
)

data class FormFieldInfo(
    val name: String,
    val fullyQualifiedName: String,
    val type: FormFieldType,
    val currentValue: String?,
    val options: List<String>,
    val label: String = name,
    val hits: List<FormFieldHit> = emptyList(),
    /** False when the PDF declares the text field multiline (Enter types a line break). */
    val multiline: Boolean = false,
    /** True when the PDF declares the text field a password field (/Ff bit 14). */
    val isPassword: Boolean = false,
    /** Upper limit from the PDF's /MaxLen; null when the PDF does not limit the length. */
    val maxLength: Int? = null,
    /** Keyboard to open for a single-line text field. */
    val keyboardKind: FormKeyboardKind = FormKeyboardKind.TEXT,
    /**
     * Unique key for this row's value. The fully qualified name for everything except checkbox
     * children that share their group's name; those get "name\u0000option" so each box can be
     * filled on its own. [fillPdfForm] understands both forms.
     */
    val valueKey: String = fullyQualifiedName,
)

data class ScannedItem(
    val value: String,
    val isPhone: Boolean,
)

data class PdfAnnotation(
    val pageIndex: Int,
    val xFraction: Float,
    val yFraction: Float,
    val text: String,
    val sizeFraction: Float,
    val color: Int = android.graphics.Color.BLACK,
    val zOrder: Long = 0L,
)

data class PdfStroke(
    val pageIndex: Int,
    val points: List<Pair<Float, Float>>,
    val widthFraction: Float,
    val color: Int = android.graphics.Color.BLACK,
    val straight: Boolean = false,
    val zOrder: Long = 0L,
    val highlight: Boolean = false,
)

private const val MIN_EXTRACT_IMAGE_PX = 64

private fun collectPageImages(
    resources: PDResources?,
    out: MutableList<Bitmap>,
) {
    if (resources == null) return
    for (name in resources.xObjectNames) {
        try {
            val xobj = resources.getXObject(name) ?: continue
            when (xobj) {
                is PDImageXObject -> {
                    val bmp = xobj.image ?: continue
                    if (bmp.width >= MIN_EXTRACT_IMAGE_PX && bmp.height >= MIN_EXTRACT_IMAGE_PX) {
                        out.add(bmp)
                    } else {
                        bmp.recycle()
                    }
                }
                is PDFormXObject -> collectPageImages(xobj.resources, out)
            }
        } catch (_: Exception) {
            // Unsupported image (JBIG2/JPX etc.) — skipped
        }
    }
}

private fun whiteOpaque(bitmap: Bitmap): Bitmap {
    val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    out.eraseColor(Color.WHITE)
    Canvas(out).drawBitmap(bitmap, 0f, 0f, null)
    return out
}

private fun scanBitmapForQr(bitmap: Bitmap): List<String> {
    val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().build())
    return try {
        val result = Tasks.await(scanner.process(InputImage.fromBitmap(bitmap, 0)))
        result.mapNotNull { it.rawValue?.takeIf(String::isNotBlank) }.distinct()
    } finally {
        scanner.close()
    }
}

private fun scanBitmapForLinks(bitmap: Bitmap): List<ScannedItem> {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return try {
        val text = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0))).text
        extractLinksFromText(text)
    } finally {
        recognizer.close()
    }
}

private fun extractLinksFromText(text: String): List<ScannedItem> {
    val result = mutableListOf<ScannedItem>()
    val phonePattern = Regex("(\\+?\\d[\\d\\s]{6,}\\d)")
    phonePattern.findAll(text).forEach { result.add(ScannedItem(it.value.trim(), true)) }
    val urlMatcher = Patterns.WEB_URL.matcher(text)
    while (urlMatcher.find()) {
        result.add(ScannedItem(urlMatcher.group(), false))
    }
    return result
}

private fun normalizedPageRotation(rotation: Int): Int {
    val turns = ((rotation % 360) + 360) % 360
    return if (turns == 90 || turns == 180 || turns == 270) turns else 0
}

private fun uprightPageMatrix(
    box: PDRectangle,
    rotation: Int,
): Matrix {
    val left = box.lowerLeftX
    val bottom = box.lowerLeftY
    val width = box.width
    val height = box.height
    return when (rotation) {
        90 -> Matrix(0f, 1f, -1f, 0f, left + width, bottom)
        180 -> Matrix(-1f, 0f, 0f, -1f, left + width, bottom + height)
        270 -> Matrix(0f, -1f, 1f, 0f, left, bottom + height)
        else -> Matrix(1f, 0f, 0f, 1f, left, bottom)
    }
}

private fun rgbOf(color: Int): Triple<Int, Int, Int> {
    return Triple((color shr 16) and 0xFF, (color shr 8) and 0xFF, color and 0xFF)
}

private const val ANNOTATION_FONT = "fonts/LiberationSans-Regular.ttf"

private fun wrapAnnotationText(
    text: String,
    paint: Paint,
    maxWidthPx: Int,
): List<String> {
    // Explicit line breaks (#59) always break the annotation; each segment then word-wraps.
    // Soft hyphens never reach the PDF output (#62).
    val text = text.withoutSoftHyphens()
    return if (maxWidthPx <= 0) {
        text.split('\n')
    } else {
        text.split('\n').flatMap { wrapSegment(it, paint, maxWidthPx) }
    }
}

private fun wrapSegment(
    segment: String,
    paint: Paint,
    maxWidthPx: Int,
): List<String> {
    val lines = mutableListOf<String>()
    var current = StringBuilder()
    segment.split(" ").forEach { word ->
        val test = if (current.isEmpty()) word else "$current $word"
        if (paint.measureText(test) <= maxWidthPx || current.isEmpty()) {
            current = StringBuilder(test)
        } else {
            lines.add(current.toString())
            current = StringBuilder(word)
        }
    }
    if (current.isNotEmpty()) lines.add(current.toString())
    return lines
}

private val rtfSkipDestinations =
    setOf(
        "fonttbl", "colortbl", "expandedcolortbl", "stylesheet", "info",
        "generator", "creatim", "revtim", "printim", "datastore",
        "listtable", "listoverridetable", "liststyle", "pntext",
        "header", "headerl", "headerr", "headerf", "footer", "footerl",
        "footerr", "footerf", "footnote", "annotation", "pict", "shppict",
        "object", "themedata", "colorschememapping", "latentstyles",
        "rsidtbl", "formshade", "shape", "ole", "bkmkstart", "bkmkend",
        "filetbl", "password", "ffilenametbl", "doccomm", "userprops",
        "trackchanges", "nonshppict", "ignorable",
    )

private fun rtfToHtml(rtf: String): String {
    val sb = StringBuilder()
    sb.append("<html><head><meta charset=\"utf-8\"></head><body><p>")
    var bold = false
    var italic = false
    var underline = false
    var openB = false
    var openI = false
    var openU = false

    fun syncTags() {
        if (bold && !openB) {
            sb.append("<b>")
            openB = true
        } else if (!bold && openB) {
            sb.append("</b>")
            openB = false
        }
        if (italic && !openI) {
            sb.append("<i>")
            openI = true
        } else if (!italic && openI) {
            sb.append("</i>")
            openI = false
        }
        if (underline && !openU) {
            sb.append("<u>")
            openU = true
        } else if (!underline && openU) {
            sb.append("</u>")
            openU = false
        }
    }

    var groupDepth = 0
    var skipUntilDepth = -1
    var atGroupStart = false
    val skipping: () -> Boolean = { skipUntilDepth != -1 }

    var i = 0
    val n = rtf.length
    while (i < n) {
        val c = rtf[i]
        if (c == '\\') {
            if (i + 1 >= n) break
            val next = rtf[i + 1]
            if (next.isLetter()) {
                var j = i + 1
                while (j < n && rtf[j].isLetter()) j++
                val word = rtf.substring(i + 1, j)
                var k = j
                var neg = false
                if (k < n && rtf[k] == '-') {
                    neg = true
                    k++
                }
                var num = 0
                var hasNum = false
                while (k < n && rtf[k].isDigit()) {
                    num = num * 10 + (rtf[k] - '0')
                    hasNum = true
                    k++
                }
                if (k < n && rtf[k] == ' ') k++
                if (atGroupStart) {
                    if (word != "*") {
                        atGroupStart = false
                        if (word in rtfSkipDestinations) {
                            skipUntilDepth = groupDepth
                        }
                    }
                } else if (!skipping()) {
                    when (word) {
                        "par" -> {
                            syncTags()
                            sb.append("</p><p>")
                        }
                        "row" -> {
                            syncTags()
                            sb.append("</p><p>")
                        }
                        "line" -> sb.append("<br>")
                        "page", "sect" -> sb.append("<br>")
                        "cell" -> sb.append("   ")
                        "tab" -> sb.append("&nbsp;&nbsp;&nbsp;&nbsp;")
                        "b" -> {
                            bold = !hasNum || num != 0
                            syncTags()
                        }
                        "i" -> {
                            italic = !hasNum || num != 0
                            syncTags()
                        }
                        "ul" -> {
                            underline = !hasNum || num != 0
                            syncTags()
                        }
                        "ulnone" -> {
                            underline = false
                            syncTags()
                        }
                        "u" -> {
                            if (hasNum) {
                                var code = if (neg) -num else num
                                if (code < 0) code += 65536
                                syncTags()
                                sb.appendCodePoint(code)
                                if (k < n) k++
                            }
                        }
                        "emdash" -> sb.append('\u2014')
                        "endash" -> sb.append('\u2013')
                        "bullet" -> sb.append('\u2022')
                        "lquote" -> sb.append('\u2018')
                        "rquote" -> sb.append('\u2019')
                        "ldblquote" -> sb.append('\u201C')
                        "rdblquote" -> sb.append('\u201D')
                        else -> {}
                    }
                }
                i = k
            } else if (next == '\'') {
                if (i + 3 < n) {
                    val hex = rtf.substring(i + 2, i + 4).toIntOrNull(16)
                    if (hex != null && !skipping()) {
                        syncTags()
                        sb.append(hex.toChar())
                    }
                    i += 4
                } else {
                    i += 2
                }
            } else {
                if (next != '*') {
                    if (!skipping()) {
                        syncTags()
                        when (next) {
                            '\\' -> sb.append('\\')
                            '{' -> sb.append('{')
                            '}' -> sb.append('}')
                            '~' -> sb.append('\u00A0')
                            '-' -> sb.append('-')
                            '_' -> sb.append(' ')
                            else -> sb.append(next)
                        }
                    }
                    atGroupStart = false
                }
                i += 2
            }
        } else if (c == '{') {
            groupDepth++
            atGroupStart = true
            i++
        } else if (c == '}') {
            if (skipping() && groupDepth == skipUntilDepth) {
                skipUntilDepth = -1
            }
            groupDepth--
            atGroupStart = false
            i++
        } else {
            if (!skipping()) {
                syncTags()
                sb.append(c)
            }
            i++
        }
    }
    syncTags()
    sb.append("</p></body></html>")
    return sb.toString()
}

private fun docxToHtml(filePath: String): String? {
    return try {
        val zip = ZipFile(filePath)
        try {
            val entry = zip.getEntry("word/document.xml") ?: return null
            val xml = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val rels = readDocxImageRels(zip)
            val numbering = readDocxNumbering(zip)
            parseDocumentXml(xml, rels, zip, numbering)
        } finally {
            zip.close()
        }
    } catch (_: Exception) {
        null
    }
}

private const val MAX_PREVIEW_TEXT_CHARS = 500_000

internal fun extractDocxText(filePath: String): String? {
    val html = docxToHtml(filePath) ?: return null
    return htmlToPlainText(html)
}

internal fun extractXlsxText(filePath: String): String? {
    val html = xlsxToHtml(filePath) ?: return null
    return htmlToPlainText(html)
}

internal fun extractRtfText(filePath: String): String? {
    return try {
        val file = File(filePath)
        if (!file.exists()) return null
        if (file.length() > MAX_TEXT_VIEW_BYTES) return null
        val html = rtfToHtml(file.readText())
        htmlToPlainText(html)
    } catch (_: Exception) {
        null
    }
}

internal fun htmlToPlainText(html: String): String {
    val withoutTags =
        html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>|</h[1-6]>|</tr>|</td>|</table>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<img\\b[^>]*/?>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<[^>]+>"), "")
    val decoded =
        withoutTags
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
    val lines =
        decoded
            .lines()
            .map { it.trim() }
            .fold(mutableListOf<String>()) { acc, line ->
                if (line.isNotBlank() || acc.isEmpty() || acc.last().isNotBlank()) {
                    acc.add(line)
                }
                acc
            }
    var text = lines.joinToString("\n").trim()
    if (text.length > MAX_PREVIEW_TEXT_CHARS) {
        text = text.take(MAX_PREVIEW_TEXT_CHARS) + "\n…"
    }
    return text
}

private data class NumLevel(
    val fmt: String,
    val lvlText: String,
    val start: Int,
)

private fun readDocxNumbering(zip: ZipFile): Map<Int, Map<Int, NumLevel>> {
    val entry = zip.getEntry("word/numbering.xml") ?: return emptyMap()
    val xml = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
    val abstractLevels = mutableMapOf<Int, Map<Int, NumLevel>>()
    val abstractNumRe =
        Regex("""<w:abstractNum w:abstractNumId="(\d+)"[^>]*>(.*?)</w:abstractNum>""", RegexOption.DOT_MATCHES_ALL)
    val lvlRe = Regex("""<w:lvl w:ilvl="(\d+)"[^>]*>(.*?)</w:lvl>""", RegexOption.DOT_MATCHES_ALL)
    val startRe = Regex("""<w:start w:val="(\d+)"""")
    val fmtRe = Regex("""<w:numFmt w:val="([^"]+)"""")
    val lvlTextRe = Regex("""<w:lvlText w:val="([^"]*)"""")
    for (am in abstractNumRe.findAll(xml)) {
        val abstractId = am.groupValues[1].toInt()
        val levels = mutableMapOf<Int, NumLevel>()
        for (lm in lvlRe.findAll(am.groupValues[2])) {
            val ilvl = lm.groupValues[1].toInt()
            val body = lm.groupValues[2]
            val start = startRe.find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 1
            val fmt = fmtRe.find(body)?.groupValues?.get(1) ?: "decimal"
            val lvlText = lvlTextRe.find(body)?.groupValues?.get(1) ?: "%1."
            levels[ilvl] = NumLevel(fmt, lvlText, start)
        }
        abstractLevels[abstractId] = levels
    }
    val numRe = Regex("""<w:num w:numId="(\d+)"[^>]*>(.*?)</w:num>""", RegexOption.DOT_MATCHES_ALL)
    val absRe = Regex("""<w:abstractNumId w:val="(\d+)"""")
    val result = mutableMapOf<Int, Map<Int, NumLevel>>()
    for (nm in numRe.findAll(xml)) {
        val numId = nm.groupValues[1].toInt()
        val abstractId = absRe.find(nm.groupValues[2])?.groupValues?.get(1)?.toIntOrNull()
        if (abstractId != null) {
            abstractLevels[abstractId]?.let { result[numId] = it }
        }
    }
    return result
}

private fun listMarker(
    numbering: Map<Int, Map<Int, NumLevel>>,
    counters: MutableMap<Pair<Int, Int>, Int>,
    numId: Int,
    ilvl: Int,
): String {
    val level = numbering[numId]?.get(ilvl) ?: return ""
    val key = numId to ilvl
    val counter = counters[key] ?: level.start.coerceAtLeast(1)
    counters[key] = counter + 1
    val formatted =
        when (level.fmt) {
            "bullet" -> "•"
            "lowerLetter" -> toListLetter(counter, upper = false)
            "upperLetter" -> toListLetter(counter, upper = true)
            "lowerRoman" -> toRoman(counter)
            "upperRoman" -> toRoman(counter).uppercase()
            else -> counter.toString()
        }
    val marker = if (level.fmt == "bullet") "•" else level.lvlText.replace("%1", formatted)
    return "$marker  "
}

internal fun toListLetter(
    n: Int,
    upper: Boolean,
): String {
    val base = if (upper) 'A' else 'a'
    val sb = StringBuilder()
    var num = n
    while (num > 0) {
        val rem = (num - 1) % 26
        sb.append((base.code + rem).toChar())
        num = (num - 1) / 26
    }
    return sb.reverse().toString()
}

internal fun toRoman(n: Int): String {
    val table =
        listOf(
            1000 to "m",
            900 to "cm",
            500 to "d",
            400 to "cd",
            100 to "c",
            90 to "xc",
            50 to "l",
            40 to "xl",
            10 to "x",
            9 to "ix",
            5 to "v",
            4 to "iv",
            1 to "i",
        )
    var num = n
    val sb = StringBuilder()
    for ((value, symbol) in table) {
        while (num >= value) {
            sb.append(symbol)
            num -= value
        }
    }
    return sb.toString()
}

private val wingdingsMap =
    mapOf(
        'z' to '•',
        'n' to '■',
        'q' to '◆',
        'l' to '●',
        'S' to '☎',
        'J' to '☺',
        'a' to '✂',
        'F' to '❖',
        'E' to '✉',
    )

private fun readDocxImageRels(zip: ZipFile): Map<String, String> {
    val relsEntry = zip.getEntry("word/_rels/document.xml.rels") ?: return emptyMap()
    val xml = zip.getInputStream(relsEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
    val result = mutableMapOf<String, String>()
    try {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name.substringAfter(':') == "Relationship") {
                val id = parser.getAttributeValue(null, "Id")
                val target = parser.getAttributeValue(null, "Target")
                if (id != null && target != null && target.startsWith("media/")) {
                    result[id] = target
                }
            }
            event = parser.next()
        }
    } catch (_: Exception) {
        // ignore
    }
    return result
}

private fun parseDocumentXml(
    xml: String,
    rels: Map<String, String>,
    zip: ZipFile,
    numbering: Map<Int, Map<Int, NumLevel>>,
): String {
    val sb = StringBuilder()
    sb.append("<html><head><meta charset=\"utf-8\"></head><body>")
    val factory = XmlPullParserFactory.newInstance()
    val parser = factory.newPullParser()
    parser.setInput(StringReader(xml))
    var paraStyle: String? = null
    val paraBuffer = StringBuilder()
    var bold = false
    var italic = false
    var underline = false
    var inText = false
    var pendingExtent: Pair<Long, Long>? = null
    var legacyImageSize: Pair<Long, Long>? = null
    var numId: Int? = null
    var ilvl: Int? = null
    var runFont: String? = null
    val counters = mutableMapOf<Pair<Int, Int>, Int>()
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> {
                when (parser.name.substringAfter(':')) {
                    "p" -> {
                        if (paraBuffer.isNotEmpty()) {
                            sb.append("<p>").append(paraBuffer).append("</p>")
                        }
                        paraStyle = null
                        paraBuffer.setLength(0)
                        bold = false
                        italic = false
                        underline = false
                        numId = null
                        ilvl = null
                    }
                    "pStyle" ->
                        paraStyle =
                            parser.getAttributeValue(null, "w:val") ?: parser.getAttributeValue(null, "val")
                    "r" -> {
                        bold = false
                        italic = false
                        underline = false
                        runFont = null
                    }
                    "rFonts" -> {
                        runFont =
                            parser.getAttributeValue(null, "w:ascii")
                                ?: parser.getAttributeValue(null, "ascii")
                                ?: runFont
                    }
                    "ilvl" -> {
                        ilvl = parser.getAttributeValue(null, "w:val")?.toIntOrNull() ?: ilvl
                    }
                    "numId" -> {
                        numId = parser.getAttributeValue(null, "w:val")?.toIntOrNull() ?: numId
                    }
                    "b" -> bold = xmlAttrOn(parser)
                    "i" -> italic = xmlAttrOn(parser)
                    "u" -> underline = xmlAttrOn(parser)
                    "t" -> inText = true
                    "br" -> paraBuffer.append("<br>")
                    "tab" -> paraBuffer.append("&nbsp;&nbsp;&nbsp;&nbsp;")
                    "tbl" -> sb.append("<table style=\"border-collapse:collapse;width:100%\">")
                    "tr" -> sb.append("<tr>")
                    "tc" -> sb.append("<td style=\"border:1px solid #999;padding:4px\">")
                    "extent" -> {
                        val cx = parser.getAttributeValue(null, "cx")?.toLongOrNull()
                        val cy = parser.getAttributeValue(null, "cy")?.toLongOrNull()
                        if (cx != null && cy != null) pendingExtent = cx to cy
                    }
                    "shape", "group" -> {
                        val style = parser.getAttributeValue(null, "style") ?: ""
                        val wPt = Regex("""width:([0-9.]+)pt""").find(style)?.groupValues?.get(1)?.toFloatOrNull()
                        val hPt = Regex("""height:([0-9.]+)pt""").find(style)?.groupValues?.get(1)?.toFloatOrNull()
                        if (wPt != null && hPt != null && wPt > 0f && hPt > 0f) {
                            legacyImageSize = (wPt * EMU_PER_PT).toLong() to (hPt * EMU_PER_PT).toLong()
                        }
                    }
                    "blip" -> {
                        val embed =
                            parser.getAttributeValue(null, "r:embed")
                                ?: parser.getAttributeValue(null, "embed")
                                ?: parser.getAttributeValue(null, "r:id")
                        if (embed != null) {
                            val target = rels[embed]
                            if (target != null) {
                                val entry = zip.getEntry("word/$target") ?: zip.getEntry(target)
                                if (entry != null) {
                                    val bytes = zip.getInputStream(entry).use { it.readBytes() }
                                    paraBuffer.append(buildImageHtml(bytes, target, pendingExtent))
                                }
                            }
                        }
                        pendingExtent = null
                    }
                    "imagedata" -> {
                        val embed =
                            parser.getAttributeValue(null, "r:id")
                                ?: parser.getAttributeValue(null, "r:embed")
                        if (embed != null) {
                            val target = rels[embed]
                            if (target != null) {
                                val entry = zip.getEntry("word/$target") ?: zip.getEntry(target)
                                if (entry != null) {
                                    val bytes = zip.getInputStream(entry).use { it.readBytes() }
                                    paraBuffer.append(buildImageHtml(bytes, target, legacyImageSize))
                                }
                            }
                        }
                        pendingExtent = null
                    }
                    else -> {}
                }
            }
            XmlPullParser.END_TAG -> {
                when (parser.name.substringAfter(':')) {
                    "p" -> {
                        val prefix = StringBuilder()
                        if (numId != null) {
                            val level = ilvl ?: 0
                            repeat(level * 2) { prefix.append("&nbsp;") }
                            prefix.append(htmlEscape(listMarker(numbering, counters, numId, level)))
                        }
                        val level = paraStyle?.removePrefix("Heading")?.toIntOrNull()
                        if (level != null && level in 1..6) {
                            sb.append("<h$level>").append(prefix).append(paraBuffer).append("</h$level>")
                        } else {
                            sb.append("<p>").append(prefix).append(paraBuffer).append("</p>")
                        }
                        paraBuffer.setLength(0)
                    }
                    "t" -> inText = false
                    "tbl" -> sb.append("</table>")
                    "tr" -> sb.append("</tr>")
                    "tc" -> sb.append("</td>")
                    "drawing" -> pendingExtent = null
                    "pict" -> legacyImageSize = null
                    else -> {}
                }
            }
            XmlPullParser.TEXT -> {
                if (inText) {
                    var text = parser.text ?: ""
                    if (runFont == "Wingdings" || runFont == "Symbol") {
                        text = text.map { wingdingsMap[it] ?: it }.joinToString("")
                    }
                    val open = StringBuilder()
                    val close = StringBuilder()
                    if (bold) {
                        open.append("<b>")
                        close.append("</b>")
                    }
                    if (italic) {
                        open.append("<i>")
                        close.append("</i>")
                    }
                    if (underline) {
                        open.append("<u>")
                        close.append("</u>")
                    }
                    paraBuffer.append(open).append(htmlEscape(text)).append(close)
                }
            }
        }
        event = parser.next()
    }
    if (paraBuffer.isNotEmpty()) {
        sb.append("<p>").append(paraBuffer).append("</p>")
    }
    sb.append("</body></html>")
    return sb.toString()
}

private fun buildImageHtml(
    bytes: ByteArray,
    mediaPath: String,
    extent: Pair<Long, Long>?,
): String {
    val mime =
        when (mediaPath.substringAfterLast('.', "").lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            else -> return ""
        }
    var widthPt = extent?.first?.toDouble()?.div(EMU_PER_PT) ?: 0.0
    var heightPt = extent?.second?.toDouble()?.div(EMU_PER_PT) ?: 0.0
    if (widthPt <= 0.0 || heightPt <= 0.0) {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        if (opts.outWidth > 0 && opts.outHeight > 0) {
            widthPt = opts.outWidth * 72.0 / 96.0
            heightPt = opts.outHeight * 72.0 / 96.0
        } else {
            widthPt = A4_WIDTH_PT * 0.9
            heightPt = 300.0
        }
    }
    val maxWpt = (A4_WIDTH_PT - 2 * 8).toDouble()
    val maxHpt = (A4_HEIGHT_PT - 2 * 8).toDouble()
    val scale = min(1.0, min(maxWpt / widthPt, maxHpt / heightPt))
    val wCss = (widthPt * scale).toInt().coerceAtLeast(1)
    val hCss = (heightPt * scale).toInt().coerceAtLeast(1)
    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    return "<img src=\"data:$mime;base64,$b64\" style=\"width:${wCss}px;height:${hCss}px;display:block\"/>"
}

private fun xmlAttrOn(parser: XmlPullParser): Boolean {
    val value = parser.getAttributeValue(null, "w:val") ?: parser.getAttributeValue(null, "val")
    return value == null || value != "0" && value != "false"
}

private fun htmlEscape(text: String): String = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

private fun xlsxToHtml(filePath: String): String? {
    return try {
        val zip = ZipFile(filePath)
        try {
            val shared = parseSharedStrings(zip)
            val sb = StringBuilder()
            sb.append("<html><head><meta charset=\"utf-8\"></head><body>")
            val sheetPattern = Regex("xl/worksheets/sheet\\d+\\.xml")
            zip.entries().asSequence()
                .filter { sheetPattern.matches(it.name) }
                .sortedBy { it.name }
                .forEach { entry ->
                    val xml = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    sb.append(parseSheetXml(xml, shared))
                }
            sb.append("</body></html>")
            sb.toString()
        } finally {
            zip.close()
        }
    } catch (_: Exception) {
        null
    }
}

private fun parseSharedStrings(zip: ZipFile): List<String> {
    val entry = zip.getEntry("xl/sharedStrings.xml") ?: return emptyList()
    return try {
        val xml = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val result = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))
        val siBuffer = StringBuilder()
        var inT = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.substringAfter(':')) {
                        "si" -> siBuffer.setLength(0)
                        "t" -> inT = true
                    }
                }
                XmlPullParser.TEXT -> if (inT) siBuffer.append(parser.text ?: "")
                XmlPullParser.END_TAG -> {
                    when (parser.name.substringAfter(':')) {
                        "t" -> inT = false
                        "si" -> result.add(siBuffer.toString())
                    }
                }
            }
            event = parser.next()
        }
        result
    } catch (_: Exception) {
        emptyList()
    }
}

private fun parseSheetXml(
    xml: String,
    shared: List<String>,
): String {
    val sb = StringBuilder()
    sb.append("<table style=\"border-collapse:collapse;width:100%;margin-bottom:16pt\">")
    val factory = XmlPullParserFactory.newInstance()
    val parser = factory.newPullParser()
    parser.setInput(StringReader(xml))
    var rowCells = mutableListOf<Pair<Int, String>>()
    var colIndex = 0
    var cellType = ""
    val vBuffer = StringBuilder()
    val inlineBuffer = StringBuilder()
    var inV = false
    var inInline = false
    var inInlineText = false
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> {
                when (parser.name.substringAfter(':')) {
                    "row" -> rowCells = mutableListOf()
                    "c" -> {
                        colIndex = columnIndex(parser.getAttributeValue(null, "r") ?: "") ?: 0
                        cellType = parser.getAttributeValue(null, "t") ?: ""
                        vBuffer.setLength(0)
                        inlineBuffer.setLength(0)
                        inV = false
                        inInline = false
                        inInlineText = false
                    }
                    "v" -> inV = true
                    "is" -> inInline = true
                    "t" -> if (inInline) inInlineText = true
                }
            }
            XmlPullParser.TEXT -> {
                if (inV) {
                    vBuffer.append(parser.text ?: "")
                } else if (inInlineText) {
                    inlineBuffer.append(parser.text ?: "")
                }
            }
            XmlPullParser.END_TAG -> {
                when (parser.name.substringAfter(':')) {
                    "v" -> inV = false
                    "t" -> inInlineText = false
                    "is" -> inInline = false
                    "c" -> {
                        val value =
                            when (cellType) {
                                "s" ->
                                    vBuffer.toString().toIntOrNull()
                                        ?.let { shared.getOrNull(it) }
                                        ?: ""
                                "inlineStr" -> inlineBuffer.toString()
                                else -> vBuffer.toString().trim()
                            }
                        rowCells.add(colIndex to value)
                    }
                    "row" -> {
                        val maxCol = rowCells.maxOfOrNull { it.first } ?: -1
                        sb.append("<tr>")
                        for (ci in 0..maxCol) {
                            val cell = rowCells.firstOrNull { it.first == ci }
                            sb.append("<td style=\"border:1px solid #999;padding:4px\">")
                            sb.append(htmlEscape(cell?.second ?: ""))
                            sb.append("</td>")
                        }
                        sb.append("</tr>")
                    }
                }
            }
        }
        event = parser.next()
    }
    sb.append("</table>")
    return sb.toString()
}

internal fun columnIndex(ref: String): Int? {
    val letters = ref.takeWhile { it.isLetter() }
    if (letters.isEmpty()) return null
    var index = 0
    for (ch in letters.uppercase()) {
        index = index * 26 + (ch - 'A' + 1)
    }
    return index - 1
}

private data class PdfLayout(
    val marginTopBottomPt: Float,
    val marginLeftRightPt: Float,
    val fontSizePt: Float,
    val lineSpacing: Float,
    val font: PdfFont = PdfFont.SYSTEM,
) {
    val textWidth: Float
        get() = A4_WIDTH_PT - 2 * marginLeftRightPt
    val baseLineHeight: Float
        get() = fontSizePt * lineSpacing
}

private const val LIST_INDENT = 14f
private const val QUOTE_INDENT = 18f
private const val CODE_BG = 0xFFF2F2F2.toInt()

private fun Node.forEachChild(block: (Node) -> Unit) {
    var child = firstChild
    while (child != null) {
        val next = child.next
        block(child)
        child = next
    }
}

private fun breakTextWords(
    text: String,
    paint: Paint,
    maxWidth: Float,
): List<String> {
    val parts = mutableListOf<String>()
    var s = 0
    while (s < text.length) {
        val count = paint.breakText(text, s, text.length, true, maxWidth, null)
        if (count <= 0) {
            parts.add(text.substring(s, s + 1))
            s++
            continue
        }
        var end = s + count
        if (end < text.length) {
            val lastSpace = text.lastIndexOf(' ', end - 1)
            if (lastSpace > s) {
                end = lastSpace
            }
        }
        parts.add(text.substring(s, end))
        s = end
        if (s < text.length && text[s] == ' ') s++
    }
    return parts
}

private fun wrapText(
    text: String,
    paint: Paint,
    maxWidth: Float,
): List<String> {
    val lines = mutableListOf<String>()
    val paragraphs = text.split('\n')
    paragraphs.forEachIndexed { index, paragraph ->
        if (paragraph.isBlank()) {
            if (index > 0) lines.add("")
            return@forEachIndexed
        }
        lines += breakTextWords(paragraph, paint, maxWidth)
    }
    return lines
}

private const val MAGIC_HEADER_SIZE = 12
internal const val MAX_FILE_UNIQUIFIER = 1000

private val DOWNLOAD_EXT_BLACKLIST =
    setOf("php", "asp", "aspx", "jsp", "jspx", "cgi", "do", "action", "cfm", "pl", "phtml", "shtml")

internal fun buildFileName(
    originalName: String,
    prefix: String,
    ext: String?,
): String {
    val originalName = originalName.withoutSoftHyphens()
    if (originalName.isBlank()) {
        return "$prefix$ext"
    }
    val dot = originalName.lastIndexOf('.')
    val base =
        (
            if (dot > 0) {
                originalName.substring(
                    0,
                    dot,
                )
            } else {
                originalName
            }
        ).replace(Regex("[\\\\/:*?\"<>|]"), "_").take(30).ifBlank {
            "$prefix"
        }
    val originalExt = if (dot > 0) originalName.substring(dot + 1) else ""
    val finalExt = ext ?: if (originalExt.isNotEmpty()) ".$originalExt" else ""
    return "$base$finalExt"
}

private fun parseContentDisposition(value: String?): String {
    if (value.isNullOrBlank()) return ""
    val marker = "filename="
    val idx = value.indexOf(marker, ignoreCase = true)
    if (idx < 0) return ""
    var name = value.substring(idx + marker.length).trim()
    name =
        if (name.startsWith("\"")) {
            name.substringAfter('"').substringBefore('"')
        } else {
            name.substringBefore(';').trim()
        }
    return sanitizeFileName(name)
}

internal data class HeaderResult(val data: ByteArray, val bytesRead: Int)

internal fun readHeader(stream: InputStream): HeaderResult {
    val buffer = ByteArray(MAGIC_HEADER_SIZE)
    var offset = 0
    while (offset < buffer.size) {
        val read = stream.read(buffer, offset, buffer.size - offset)
        if (read == -1) break
        offset += read
    }
    return HeaderResult(buffer, offset)
}

internal fun detectExtension(header: HeaderResult): String? {
    return when {
        header.matches(0, 0xFF, 0xD8, 0xFF) -> ".jpg"
        header.matches(0, 0x89, 0x50, 0x4E, 0x47) -> ".png"
        header.matches(0, 0x47, 0x49, 0x46, 0x38) -> ".gif"
        header.matches(0, 0x52, 0x49, 0x46, 0x46) && header.matches(8, 0x57, 0x45, 0x42, 0x50) -> ".webp"
        header.matches(0, 0x42, 0x4D) -> ".bmp"
        header.matches(4, 0x66, 0x74, 0x79, 0x70) -> {
            when {
                header.matches(8, 0x68, 0x65, 0x69, 0x63) -> ".heic"
                header.matches(8, 0x68, 0x65, 0x69, 0x78) -> ".heic"
                header.matches(8, 0x6D, 0x69, 0x66, 0x33) -> ".heic"
                else -> null
            }
        }
        header.matches(0, 0x25, 0x50, 0x44, 0x46) -> ".pdf"
        else -> null
    }
}

internal fun HeaderResult.matches(
    offset: Int,
    vararg magic: Int,
): Boolean {
    if (offset + magic.size > bytesRead) return false
    magic.forEachIndexed { i, byte ->
        if (data[offset + i] != byte.toByte()) return false
    }
    return true
}

internal fun buildPdfName(originalName: String): String {
    val base =
        extractBaseName(originalName)
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .take(30)
            .ifBlank { "pdf" }
    return "$base.pdf"
}

internal fun sanitizeFileName(name: String): String {
    return name.withoutSoftHyphens()
        .trim()
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .take(100)
}

internal fun buildTxtName(
    originalName: String,
    defaultStem: String,
): String {
    val base = extractBaseName(originalName).replace(Regex("[\\\\/:*?\"<>|]"), "_").take(30).ifBlank { defaultStem }
    return "$base.txt"
}

internal fun buildPageImageName(
    originalName: String,
    page: Int,
    totalPages: Int,
    defaultStem: String,
): String {
    val base = extractBaseName(originalName).replace(Regex("[\\\\/:*?\"<>|]"), "_").take(30).ifBlank { defaultStem }
    val pageStr =
        if (totalPages > 1) {
            "_p$page"
        } else {
            ""
        }
    return "${base}$pageStr.jpg"
}

internal fun extractBaseName(name: String): String {
    val dot = name.lastIndexOf('.')
    return if (dot > 0) name.substring(0, dot) else name
}

/**
 * Finds an existing IMAGE, PDF or FILE clip that already holds the same file, matched by
 * file size plus a case-insensitive name match. Pure so it can be unit-tested without Android.
 */
internal fun findDuplicateFileClip(
    clips: List<ClipboardEntry>,
    displayName: String,
    sizeBytes: Long,
): ClipboardEntry? {
    if (sizeBytes <= 0L || displayName.isBlank()) return null
    return clips.firstOrNull { entry ->
        val filePath = entry.filePath ?: return@firstOrNull false
        val isFileClip =
            entry.contentType == ContentType.IMAGE ||
                entry.contentType == ContentType.PDF ||
                entry.contentType == ContentType.FILE
        if (!isFileClip) return@firstOrNull false
        val file = File(filePath)
        if (!file.exists() || file.length() != sizeBytes) return@firstOrNull false
        val storedName = file.name
        val baseDisplayName = extractBaseName(displayName)
        entry.previewText.equals(baseDisplayName, ignoreCase = true) ||
            storedName.equals(displayName, ignoreCase = true) ||
            extractBaseName(storedName).equals(baseDisplayName, ignoreCase = true)
    }
}
