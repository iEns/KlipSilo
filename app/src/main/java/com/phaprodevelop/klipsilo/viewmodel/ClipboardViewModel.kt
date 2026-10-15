package com.phaprodevelop.klipsilo.viewmodel

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Patterns
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phaprodevelop.klipsilo.LocaleHelper
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.db.SnipDatabase
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.repository.ClipboardRepository
import com.phaprodevelop.klipsilo.data.repository.FormFieldInfo
import com.phaprodevelop.klipsilo.data.repository.PdfAnnotation
import com.phaprodevelop.klipsilo.data.repository.PdfCombineResult
import com.phaprodevelop.klipsilo.data.repository.PdfStroke
import com.phaprodevelop.klipsilo.data.repository.SaveClipResult
import com.phaprodevelop.klipsilo.data.repository.ScannedItem
import com.phaprodevelop.klipsilo.data.repository.extractDocxText
import com.phaprodevelop.klipsilo.data.repository.extractRtfText
import com.phaprodevelop.klipsilo.data.repository.extractXlsxText
import com.phaprodevelop.klipsilo.data.repository.isProbablyBinary
import com.phaprodevelop.klipsilo.data.settings.ThemeMode
import com.phaprodevelop.klipsilo.data.settings.ThemeModeStore
import com.phaprodevelop.klipsilo.data.textClipTitle
import com.phaprodevelop.klipsilo.data.textTitleLooksAutomatic
import com.phaprodevelop.klipsilo.hasLocationPermission
import com.phaprodevelop.klipsilo.isLocationEnabled
import com.phaprodevelop.klipsilo.obtainCurrentLocation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class UndoOffer(val message: String)

/** A snackbar. When [clipId] is set, [actionLabel] opens that clip. */
data class ClipNotice(
    val message: String,
    val actionLabel: String? = null,
    val clipId: Long? = null,
)

data class DuplicateShareOffer(
    val existingId: Long,
    val displayName: String,
    val imageUri: Uri?,
    val pdfUri: Uri?,
    val fileUri: Uri?,
)

class ClipboardViewModel(
    application: Application,
    private val repository: ClipboardRepository,
    private val undoTimeoutMs: Long = UNDO_TIMEOUT_MS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AndroidViewModel(application) {
    constructor(application: Application) : this(
        application,
        ClipboardRepository(
            SnipDatabase.getInstance(application).clipboardItemDao(),
            application,
        ),
    )

    private val clipboardManager =
        application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    init {
        viewModelScope.launch {
            repository.migrateLegacyNames()
        }
        viewModelScope.launch {
            // #33 leftover: once per install, title text clips that still carry a
            // stale auto title from an older build (user renames are untouched).
            repository.recomputeStaleTextTitles()
        }
        viewModelScope.launch {
            // Re-read the table on every signal. A Room emission can be stale or, after a
            // delete or an incoming share, missing entirely. The generation discards a read
            // that started before a newer write.
            repository.getAllClips().collectLatest {
                val generation = clipGeneration.get()
                val rows = withContext(ioDispatcher) { repository.listClips() }
                if (generation == clipGeneration.get()) {
                    _clips.value = rows
                }
            }
        }
    }

    private fun localizedString(
        resId: Int,
        vararg formatArgs: Any,
    ): String = LocaleHelper.applyLocale(getApplication()).getString(resId, *formatArgs)

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _toastEvents = MutableSharedFlow<ClipNotice>(extraBufferCapacity = 16)
    val toastEvents: SharedFlow<ClipNotice> = _toastEvents.asSharedFlow()

    private val queuedToasts = ArrayDeque<ClipNotice>()

    private val _undoOffer = MutableStateFlow<UndoOffer?>(null)
    val undoOffer: StateFlow<UndoOffer?> = _undoOffer.asStateFlow()

    private val _duplicateShare = MutableStateFlow<DuplicateShareOffer?>(null)
    val duplicateShare: StateFlow<DuplicateShareOffer?> = _duplicateShare.asStateFlow()

    private val undoMutex = Mutex()

    private fun toast(s: String) {
        postNotice(ClipNotice(message = s))
    }

    private fun postNotice(notice: ClipNotice) {
        _toastMessage.value = notice.message
        if (_undoOffer.value != null) {
            queuedToasts.addLast(notice)
        } else {
            _toastEvents.tryEmit(notice)
        }
    }

    fun showSaveFailed() {
        toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
    }

    suspend fun getClip(id: Long): ClipboardEntry? = withContext(ioDispatcher) { repository.getClip(id) }

    suspend fun readTextBody(entry: ClipboardEntry): String? =
        withContext(ioDispatcher) { repository.readTextBody(entry) }

    private fun setUndoOffer(offer: UndoOffer?) {
        _undoOffer.value = offer
        if (offer == null) {
            while (queuedToasts.isNotEmpty()) {
                _toastEvents.tryEmit(queuedToasts.removeFirst())
            }
        }
    }

    private suspend fun requireFilePath(entry: ClipboardEntry): String? {
        val path = entry.filePath ?: repository.getClip(entry.id)?.filePath
        if (path.isNullOrBlank()) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            return null
        }
        return path
    }

    private val _themeMode = MutableStateFlow(ThemeModeStore.load(application))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        ThemeModeStore.save(getApplication(), mode)
    }

    private val _isConverting = MutableStateFlow(false)
    val isConverting: StateFlow<Boolean> = _isConverting.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _qrResults = MutableStateFlow<List<String>?>(null)
    val qrResults: StateFlow<List<String>?> = _qrResults.asStateFlow()

    private val _qrScanning = MutableStateFlow(false)
    val qrScanning: StateFlow<Boolean> = _qrScanning.asStateFlow()
    private var qrScanJob: Job? = null

    private val _pdfLinks = MutableStateFlow<List<ScannedItem>?>(null)
    val pdfLinks: StateFlow<List<ScannedItem>?> = _pdfLinks.asStateFlow()

    private val _pdfLinksScanning = MutableStateFlow(false)
    val pdfLinksScanning: StateFlow<Boolean> = _pdfLinksScanning.asStateFlow()

    fun clearQr() {
        _qrResults.value = null
        _qrScanning.value = false
    }

    fun clearPdfLinks() {
        _pdfLinks.value = null
        _pdfLinksScanning.value = false
    }

    fun scanImageQr(entry: ClipboardEntry) {
        qrScanJob?.cancel()
        qrScanJob =
            viewModelScope.launch {
                val filePath = requireFilePath(entry) ?: return@launch
                _qrScanning.value = true
                try {
                    _qrResults.value = withContext(ioDispatcher) { repository.scanImageForQr(filePath) }
                } finally {
                    if (coroutineContext[Job] == qrScanJob) {
                        _qrScanning.value = false
                    }
                }
            }
    }

    fun scanPdfPageQr(
        entry: ClipboardEntry,
        pageIndex: Int,
    ) {
        qrScanJob?.cancel()
        qrScanJob =
            viewModelScope.launch {
                val filePath = requireFilePath(entry) ?: return@launch
                _qrScanning.value = true
                try {
                    _qrResults.value = withContext(ioDispatcher) { repository.scanPdfPageForQr(filePath, pageIndex) }
                } finally {
                    if (coroutineContext[Job] == qrScanJob) {
                        _qrScanning.value = false
                    }
                }
            }
    }

    fun scanPdfPageQrToEditor(
        entry: ClipboardEntry,
        pageIndex: Int,
    ) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val filePath = requireFilePath(entry) ?: return@launch
                val results = withContext(ioDispatcher) { repository.scanPdfPageForQr(filePath, pageIndex) }
                if (results.isNullOrEmpty()) {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.no_qr_found))
                } else {
                    val text = results.joinToString("\n")
                    createTextSnip(text)
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_saved))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun scanPdfLinks(entry: ClipboardEntry) {
        val filePath = entry.filePath ?: return
        viewModelScope.launch {
            _pdfLinksScanning.value = true
            try {
                _pdfLinks.value = withContext(ioDispatcher) { repository.scanPdfForLinks(filePath) }
            } finally {
                _pdfLinksScanning.value = false
            }
        }
    }

    fun copyText(text: String) {
        clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("snip", text))
        toast(localizedString(com.phaprodevelop.klipsilo.R.string.copied_to_clipboard))
    }

    fun saveQrAsClip(text: String) {
        viewModelScope.launch {
            try {
                val id = createTextSnip(text, navigate = false)
                if (repository.getClip(id) != null) {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_saved))
                } else {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            }
        }
    }

    private val _clips = MutableStateFlow<List<ClipboardEntry>>(emptyList())
    val clips: StateFlow<List<ClipboardEntry>> = _clips.asStateFlow()

    private val clipGeneration = AtomicLong(0)

    /** Drop the clip from the on-screen list before the database round-trip finishes. */
    private fun hideClipsNow(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        clipGeneration.incrementAndGet()
        val hide = ids.toSet()
        _clips.value = _clips.value.filter { it.id !in hide }
    }

    /** Re-read the clip list. Safe to call when a Room flow may not emit. */
    fun reloadClips() {
        viewModelScope.launch { awaitClipReload() }
    }

    private suspend fun awaitClipReload() {
        val generation = clipGeneration.incrementAndGet()
        val rows = withContext(ioDispatcher) { repository.listClips() }
        if (generation == clipGeneration.get()) {
            _clips.value = rows
        }
    }

    private val _navigateToNewClip = MutableStateFlow<Long?>(null)
    val navigateToNewClip: StateFlow<Long?> = _navigateToNewClip.asStateFlow()

    private val _recentNewClipId = MutableStateFlow<Long?>(null)
    val recentNewClipId: StateFlow<Long?> = _recentNewClipId.asStateFlow()

    fun clearRecentNewClipId() {
        _recentNewClipId.value = null
    }

    private val _navigateBack = MutableStateFlow(false)
    val navigateBack: StateFlow<Boolean> = _navigateBack.asStateFlow()

    private val _scrollToTop = MutableStateFlow(0L)
    val scrollToTop: StateFlow<Long> = _scrollToTop.asStateFlow()

    private val _shareSelectedIds = MutableStateFlow<LinkedHashSet<Long>>(LinkedHashSet())
    val shareSelectedIds: StateFlow<LinkedHashSet<Long>> = _shareSelectedIds.asStateFlow()

    private val _shareSelectionType = MutableStateFlow<ContentType?>(null)
    val shareSelectionType: StateFlow<ContentType?> = _shareSelectionType.asStateFlow()

    private fun triggerScrollToTop() {
        _scrollToTop.value = System.currentTimeMillis()
        reloadClips()
    }

    fun createEmptyClip() {
        viewModelScope.launch {
            val id = System.nanoTime()
            val entry =
                ClipboardEntry(
                    id = id,
                    contentType = ContentType.TEXT,
                    textContent = null,
                    filePath = null,
                    previewText = "",
                    timestamp = System.currentTimeMillis(),
                )
            repository.insertEntry(entry)
            _recentNewClipId.value = id
            _navigateToNewClip.value = id
            triggerScrollToTop()
        }
    }

    /** True for a freshly created new text clip that is still an empty, unsaved draft (#39). */
    fun isNewEmptyDraft(entry: ClipboardEntry?): Boolean =
        entry != null &&
            entry.id == _recentNewClipId.value &&
            !entry.isLocked &&
            entry.previewText.isBlank() &&
            entry.textContent.isNullOrBlank()

    /**
     * Leaving the editor through the lifecycle (Home, app switch, process stop) with a
     * new, still-empty draft discards the clip instead of leaving "(untitled)" behind (#39).
     * Returns true when the draft was discarded.
     */
    fun discardEmptyNewClipOnLifecycleExit(
        entry: ClipboardEntry,
        text: String,
    ): Boolean {
        if (text.isNotBlank() || !isNewEmptyDraft(entry)) return false
        _recentNewClipId.value = null
        _navigateBack.value = true
        deleteClip(entry)
        return true
    }

    fun saveScannedText(
        text: String,
        navigate: Boolean = true,
    ) {
        viewModelScope.launch {
            createTextSnip(text, navigate)
        }
    }

    fun saveImageBitmap(
        bitmap: android.graphics.Bitmap,
        toastRes: Int = com.phaprodevelop.klipsilo.R.string.image_saved,
        onResult: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val success =
                withContext(ioDispatcher) {
                    val file =
                        java.io.File(
                            getApplication<Application>().cacheDir,
                            "ocr_img_${System.currentTimeMillis()}.jpg",
                        )
                    try {
                        java.io.FileOutputStream(file).use { out ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        repository.saveNewClip(null, android.net.Uri.fromFile(file), null, null)
                    } catch (_: Exception) {
                        SaveClipResult.Failed
                    } finally {
                        file.delete()
                    }
                }
            if (success == SaveClipResult.Saved) {
                triggerScrollToTop()
            }
            toast(localizedString(toastForSave(success, toastRes)))
            onResult(success == SaveClipResult.Saved)
        }
    }

    fun notifyScannedTextSaved() {
        toast(localizedString(com.phaprodevelop.klipsilo.R.string.text_saved))
    }

    fun convertImageToPdf(entry: ClipboardEntry) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val filePath = requireFilePath(entry) ?: return@launch
                val result =
                    withContext(ioDispatcher) {
                        repository.savePdfFromImageFile(filePath, entry)
                    }
                if (result > 0L) {
                    triggerScrollToTop()
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.image_to_pdf))
                } else {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun combineToPdf(entries: List<ClipboardEntry>) {
        if (entries.size < 2) return
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val result = withContext(ioDispatcher) { repository.combineEntriesToPdf(entries) }
                handleCombineResult(result, clearSelection = false)
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun combineSelectedToPdf() {
        val ordered =
            _shareSelectedIds.value.mapNotNull { id ->
                clips.value.find { it.id == id }
            }
        if (ordered.size < 2) return
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val result = withContext(ioDispatcher) { repository.combineEntriesToPdf(ordered) }
                handleCombineResult(result, clearSelection = true)
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    private fun handleCombineResult(
        result: PdfCombineResult,
        clearSelection: Boolean,
    ) {
        when (result) {
            is PdfCombineResult.Success -> {
                triggerScrollToTop()
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.combine_to_pdf))
                if (clearSelection) clearShareSelection()
            }
            PdfCombineResult.SaveFailed -> {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            }
            PdfCombineResult.NothingReadable -> {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.combine_nothing_readable))
            }
        }
    }

    fun convertPdfToImages(entry: ClipboardEntry) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val filePath = requireFilePath(entry) ?: return@launch
                val count =
                    withContext(ioDispatcher) {
                        repository.saveImagesFromPdf(filePath, entry)
                    }
                if (count > 0) {
                    triggerScrollToTop()
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.pdf_to_image))
                } else {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun convertFileToText(entry: ClipboardEntry) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val filePath = requireFilePath(entry) ?: return@launch
                val file = java.io.File(filePath)
                val ext = file.extension.lowercase()
                val extractable = ext in setOf("docx", "xlsx", "rtf")
                if (ext !in textExtensions && !extractable) {
                    if (file.length() > MAX_TEXT_FILE_BYTES || isProbablyBinary(file)) {
                        toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                        return@launch
                    }
                }
                if (!extractable && file.length() > MAX_TEXT_FILE_BYTES) {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                    return@launch
                }
                val content =
                    withContext(ioDispatcher) {
                        try {
                            when (ext) {
                                "docx" -> extractDocxText(filePath)
                                "xlsx" -> extractXlsxText(filePath)
                                "rtf" -> extractRtfText(filePath)
                                else -> file.readText()
                            }
                        } catch (_: Throwable) {
                            null
                        }
                    }
                if (content.isNullOrBlank()) {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                    return@launch
                }
                val id = System.nanoTime()
                val textEntry =
                    ClipboardEntry(
                        id = id,
                        contentType = ContentType.TEXT,
                        textContent = content,
                        filePath = null,
                        previewText = textClipTitle(content),
                        timestamp = System.currentTimeMillis(),
                    )
                repository.insertEntry(textEntry)
                _navigateToNewClip.value = id
                triggerScrollToTop()
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.file_to_text))
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun convertTextToPdf(
        text: String,
        entry: ClipboardEntry,
        fontSizePt: Float? = null,
        lineSpacing: Float? = null,
        navigateBack: Boolean = true,
    ) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val body = text.ifBlank { repository.readTextBody(entry).orEmpty() }
                if (body.isBlank()) {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                    return@launch
                }
                val newId =
                    withContext(ioDispatcher) {
                        val effectivePreview = persistText(entry, body)
                        val entryForPdf =
                            if (entry.previewText.isBlank() && effectivePreview.isNotBlank()) {
                                entry.copy(
                                    previewText = effectivePreview,
                                )
                            } else {
                                entry
                            }
                        repository.saveTextAsPdf(body, entryForPdf, fontSizePt, lineSpacing)
                    }
                if (newId > 0L) {
                    triggerScrollToTop()
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.text_to_pdf))
                    if (navigateBack) {
                        _navigateBack.value = true
                    }
                } else {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun clearNavigateBack() {
        _navigateBack.value = false
    }

    fun convertTextToFile(
        text: String,
        entry: ClipboardEntry,
    ) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val body = text.ifBlank { repository.readTextBody(entry).orEmpty() }
                if (body.isBlank()) {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                    return@launch
                }
                val newId =
                    withContext(ioDispatcher) {
                        val effectivePreview = persistText(entry, body)
                        repository.saveTextAsFile(body, effectivePreview)
                    }
                if (newId > 0L) {
                    triggerScrollToTop()
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.text_to_txt))
                    _navigateBack.value = true
                } else {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    private suspend fun persistText(
        entry: ClipboardEntry,
        text: String,
    ): String {
        val base = repository.getClip(entry.id) ?: entry
        if (base.isLocked) {
            return base.previewText
        }
        val preview =
            if (base.contentType == ContentType.TEXT &&
                (base.previewText.isBlank() || textTitleLooksAutomatic(base.previewText))
            ) {
                textClipTitle(text).ifBlank { base.previewText }
            } else if (base.previewText.isBlank() && text.isNotBlank()) {
                textClipTitle(text)
            } else {
                base.previewText
            }
        val updated =
            base.copy(
                textContent = text.ifBlank { null },
                previewText = preview,
            )
        repository.updateClip(updated)
        if (base.previewText.isBlank() && text.isNotBlank() && _recentNewClipId.value == base.id) {
            _recentNewClipId.value = null
        }
        return preview
    }

    fun clearNavigateToNewClip() {
        _navigateToNewClip.value = null
    }

    fun convertPdfToText(entry: ClipboardEntry) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val filePath = requireFilePath(entry) ?: return@launch
                val text =
                    withContext(ioDispatcher) {
                        repository.ocrPdfToText(filePath)
                    }
                if (text.isNullOrBlank()) {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                } else {
                    createTextSnip(text)
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.pdf_to_text))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun convertPdfToHtml(entry: ClipboardEntry) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val filePath = requireFilePath(entry) ?: return@launch
                val html =
                    withContext(ioDispatcher) {
                        repository.pdfToHtml(filePath)
                    }
                if (html.isNullOrBlank()) {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                } else {
                    val id = System.nanoTime()
                    val stem = entry.previewText.replace(Regex("[_\\-]+"), " ").trim()
                    val preview =
                        localizedString(
                            com.phaprodevelop.klipsilo.R.string.html_from,
                            stem.take(60),
                        ).take(100)
                    val newEntry =
                        ClipboardEntry(
                            id = id,
                            contentType = ContentType.TEXT,
                            textContent = html,
                            filePath = null,
                            previewText = preview,
                            timestamp = System.currentTimeMillis(),
                        )
                    withContext(ioDispatcher) {
                        repository.insertEntry(newEntry)
                    }
                    _navigateToNewClip.value = id
                    triggerScrollToTop()
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.html_created))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun convertHtmlToPdf(entry: ClipboardEntry) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val body = repository.readTextBody(entry)
                val full = (repository.getClip(entry.id) ?: entry).copy(textContent = body)
                val newId =
                    withContext(ioDispatcher) {
                        repository.saveHtmlAsPdf(full)
                    }
                if (newId > 0L) {
                    triggerScrollToTop()
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.html_to_pdf_done))
                } else {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    private val _formFields = MutableStateFlow<List<FormFieldInfo>?>(null)
    val formFields: StateFlow<List<FormFieldInfo>?> = _formFields.asStateFlow()

    private val _formFieldsLoading = MutableStateFlow(false)
    val formFieldsLoading: StateFlow<Boolean> = _formFieldsLoading.asStateFlow()

    fun loadFormFields(entry: ClipboardEntry) {
        viewModelScope.launch {
            _formFieldsLoading.value = true
            try {
                val filePath = entry.filePath ?: repository.getClip(entry.id)?.filePath
                if (filePath.isNullOrBlank()) {
                    _formFields.value = emptyList()
                    return@launch
                }
                _formFields.value = withContext(ioDispatcher) { repository.getPdfFormFields(filePath) }
            } finally {
                _formFieldsLoading.value = false
            }
        }
    }

    fun clearFormFields() {
        _formFields.value = null
        _formFieldsLoading.value = false
    }

    fun fillForm(
        entry: ClipboardEntry,
        values: Map<String, String>,
    ) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val filePath = requireFilePath(entry) ?: return@launch
                val newId =
                    withContext(ioDispatcher) {
                        repository.fillPdfForm(filePath, entry, values)
                    }
                if (newId > 0L) {
                    triggerScrollToTop()
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.form_filled))
                    _navigateBack.value = true
                } else {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun savePdfPages(
        entry: ClipboardEntry,
        pageOrder: List<Int>,
    ) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val filePath = requireFilePath(entry) ?: return@launch
                val newId =
                    withContext(ioDispatcher) {
                        repository.savePdfPages(filePath, entry, pageOrder)
                    }
                if (newId > 0L) {
                    triggerScrollToTop()
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.pages_saved))
                    _navigateBack.value = true
                } else {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    suspend fun renderTextPreview(
        text: String,
        fontSizePt: Float,
        lineSpacing: Float,
    ): List<Bitmap> {
        return withContext(ioDispatcher) {
            repository.previewTextAsPdf(text, fontSizePt, lineSpacing)
        }
    }

    fun ocrCurrentPdfPage(
        entry: ClipboardEntry,
        pageIndex: Int,
    ) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val filePath = requireFilePath(entry) ?: return@launch
                val text =
                    withContext(ioDispatcher) {
                        repository.ocrPdfPage(filePath, pageIndex)
                    }
                if (text.isNullOrBlank()) {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.ocr_no_text))
                } else {
                    val id = createTextSnip(text, navigate = false)
                    postNotice(
                        ClipNotice(
                            message = localizedString(com.phaprodevelop.klipsilo.R.string.text_extracted),
                            actionLabel = localizedString(com.phaprodevelop.klipsilo.R.string.snackbar_open),
                            clipId = id,
                        ),
                    )
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    private suspend fun createTextSnip(
        text: String,
        navigate: Boolean = true,
    ): Long {
        val id = System.nanoTime()
        val entry =
            ClipboardEntry(
                id = id,
                contentType = ContentType.TEXT,
                textContent = text,
                filePath = null,
                previewText = textClipTitle(text),
                timestamp = System.currentTimeMillis(),
            )
        repository.insertEntry(entry)
        triggerScrollToTop()
        if (navigate) {
            _navigateToNewClip.value = id
        }
        return id
    }

    fun saveFileToDownloads(entry: ClipboardEntry) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val success =
                    withContext(ioDispatcher) {
                        repository.saveFileToDownloads(entry)
                    }
                toast(
                    if (success) {
                        localizedString(com.phaprodevelop.klipsilo.R.string.saved_to_downloads)
                    } else {
                        localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed)
                    },
                )
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun renameClip(
        entry: ClipboardEntry,
        newName: String,
    ) {
        if (entry.isLocked) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_is_locked))
            return
        }
        viewModelScope.launch {
            val success =
                withContext(ioDispatcher) {
                    repository.renameClip(entry, newName)
                }
            toast(
                if (success) {
                    localizedString(com.phaprodevelop.klipsilo.R.string.clip_renamed)
                } else {
                    localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed)
                },
            )
            triggerScrollToTop()
        }
    }

    fun saveCurrentClipboard() {
        try {
            val clipData = clipboardManager.primaryClip
            if (clipData == null || clipData.itemCount == 0) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                return
            }

            val item = clipData.getItemAt(0)
            var text: String? = null
            var imageUri: Uri? = null
            var pdfUri: Uri? = null
            var fileUri: Uri? = null

            if (item.uri != null) {
                val uri = item.uri
                val mime =
                    try {
                        getApplication<Application>().contentResolver.getType(uri)
                    } catch (
                        _: Exception,
                    ) {
                        null
                    } ?: ""
                when {
                    mime.startsWith("image/") -> imageUri = uri
                    mime == "application/pdf" || uri.toString().endsWith(".pdf", true) -> pdfUri = uri
                    else -> fileUri = uri
                }
            } else {
                text = item.text?.toString() ?: item.htmlText?.toString()
            }

            if (text.isNullOrBlank() && imageUri == null && pdfUri == null && fileUri == null) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                return
            }

            viewModelScope.launch {
                val result =
                    withContext(ioDispatcher) {
                        repository.saveNewClip(text, imageUri, pdfUri, fileUri)
                    }
                toast(localizedString(toastForSave(result)))
                if (result == SaveClipResult.Saved) {
                    triggerScrollToTop()
                }
            }
        } catch (_: Exception) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
        }
    }

    fun saveSharedContent(intent: Intent) {
        val streamUri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
            }
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        var imageUri: Uri? = null
        var pdfUri: Uri? = null
        var fileUri: Uri? = null

        if (streamUri != null) {
            val mime =
                try {
                    getApplication<Application>().contentResolver.getType(streamUri)
                } catch (_: Exception) {
                    null
                } ?: intent.type ?: ""
            val classified = classifyShareStream(streamUri, mime)
            imageUri = classified.first
            pdfUri = classified.second
            fileUri = classified.third
        }

        val hasStream = imageUri != null || pdfUri != null || fileUri != null
        val hasText = !text.isNullOrBlank()
        if (!hasStream && !hasText) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            return
        }

        viewModelScope.launch {
            val results = mutableListOf<SaveClipResult>()
            var duplicateOffer: DuplicateShareOffer? = null
            val streamForCheck = imageUri ?: pdfUri ?: fileUri
            if (streamForCheck != null) {
                val checked =
                    withContext(ioDispatcher) {
                        val identity = repository.sharedFileIdentity(streamForCheck) ?: return@withContext null
                        val duplicate =
                            repository.findDuplicateSharedFile(identity.first, identity.second)
                                ?: return@withContext null
                        identity to duplicate
                    }
                if (checked != null) {
                    val (identity, duplicate) = checked
                    duplicateOffer =
                        DuplicateShareOffer(
                            existingId = duplicate.id,
                            displayName = identity.first,
                            imageUri = imageUri,
                            pdfUri = pdfUri,
                            fileUri = fileUri,
                        )
                }
            }
            if (duplicateOffer != null) {
                _duplicateShare.value = duplicateOffer
            } else if (hasStream) {
                results +=
                    withContext(ioDispatcher) {
                        repository.saveNewClip(null, imageUri, pdfUri, fileUri)
                    }
            }
            if (hasText) {
                results +=
                    withContext(ioDispatcher) {
                        repository.saveNewClip(text, null, null, null)
                    }
            }
            if (results.isNotEmpty()) {
                val result =
                    when {
                        results.any { it == SaveClipResult.Saved } -> SaveClipResult.Saved
                        results.any { it == SaveClipResult.TooLarge } -> SaveClipResult.TooLarge
                        else -> SaveClipResult.Failed
                    }
                toast(localizedString(toastForSave(result)))
                if (result == SaveClipResult.Saved) {
                    triggerScrollToTop()
                }
            }
        }
    }

    fun dismissDuplicateShare() {
        _duplicateShare.value = null
    }

    fun openExistingShare() {
        val offer = _duplicateShare.value ?: return
        _duplicateShare.value = null
        _navigateToNewClip.value = offer.existingId
    }

    fun saveDuplicateAgain() {
        val offer = _duplicateShare.value ?: return
        _duplicateShare.value = null
        viewModelScope.launch {
            val result =
                withContext(ioDispatcher) {
                    repository.saveNewClip(null, offer.imageUri, offer.pdfUri, offer.fileUri)
                }
            toast(localizedString(toastForSave(result)))
            if (result == SaveClipResult.Saved) triggerScrollToTop()
        }
    }

    private fun classifyShareStream(
        uri: Uri,
        mime: String,
    ): Triple<Uri?, Uri?, Uri?> {
        val mimeLower = mime.lowercase()
        val name = (uri.lastPathSegment ?: uri.toString()).lowercase()
        val weakMime =
            mimeLower.isEmpty() || mimeLower == "*/*" || mimeLower == "text/plain"
        return when {
            mimeLower.startsWith("image/") -> Triple(uri, null, null)
            mimeLower == "application/pdf" || name.endsWith(".pdf") -> Triple(null, uri, null)
            weakMime &&
                (
                    name.endsWith(".jpg") ||
                        name.endsWith(".jpeg") ||
                        name.endsWith(".png") ||
                        name.endsWith(".gif") ||
                        name.endsWith(".webp")
                ) -> Triple(uri, null, null)
            else -> Triple(null, null, uri)
        }
    }

    fun updateClip(entry: ClipboardEntry) {
        if (isLockedEntry(entry.id)) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_is_locked))
            return
        }
        viewModelScope.launch {
            try {
                val toSave =
                    if (entry.contentType == ContentType.TEXT || entry.contentType == ContentType.URL) {
                        val full = repository.getClip(entry.id) ?: entry
                        full.copy(
                            textContent = entry.textContent ?: full.textContent,
                            previewText = previewForUpdatedClip(full, entry),
                            timestamp = entry.timestamp,
                            isPinned = entry.isPinned,
                            isLocked = entry.isLocked,
                            filePath = entry.filePath ?: full.filePath,
                            latitude = entry.latitude ?: full.latitude,
                            longitude = entry.longitude ?: full.longitude,
                        )
                    } else {
                        entry
                    }
                repository.updateClip(toSave)
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_saved))
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            }
        }
    }

    private fun previewForUpdatedClip(
        stored: ClipboardEntry,
        incoming: ClipboardEntry,
    ): String {
        val body = incoming.textContent ?: stored.textContent.orEmpty()
        val requested = incoming.previewText
        if (incoming.contentType == ContentType.TEXT) {
            if (requested.isNotBlank() &&
                requested != stored.previewText &&
                !textTitleLooksAutomatic(requested)
            ) {
                return requested
            }
            if (stored.previewText.isNotBlank() && !textTitleLooksAutomatic(stored.previewText)) {
                return stored.previewText
            }
            return textClipTitle(body).ifBlank { stored.previewText }
        }
        return requested.ifBlank { stored.previewText }
    }

    fun togglePin(entry: ClipboardEntry) {
        viewModelScope.launch {
            repository.togglePin(entry)
        }
    }

    fun toggleLock(entry: ClipboardEntry) {
        viewModelScope.launch {
            repository.setLocked(entry, !entry.isLocked)
        }
    }

    fun unlockAllClips(): Int {
        val locked = clips.value.filter { it.isLocked }
        if (locked.isEmpty()) return 0
        viewModelScope.launch {
            locked.forEach { repository.setLocked(it, false) }
        }
        return locked.size
    }

    private val _maintenanceBusy = MutableStateFlow(false)
    val maintenanceBusy: StateFlow<Boolean> = _maintenanceBusy.asStateFlow()

    fun backupClips(uri: Uri) {
        if (_maintenanceBusy.value) return
        val entries = clips.value
        if (entries.isEmpty()) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.no_clips_to_backup))
            return
        }
        viewModelScope.launch {
            _maintenanceBusy.value = true
            try {
                val result = withContext(ioDispatcher) { repository.backupClipsToZip(uri, entries) }
                toast(
                    if (result.exported > 0) {
                        val base =
                            LocaleHelper.applyLocale(getApplication())
                                .getString(com.phaprodevelop.klipsilo.R.string.backup_done, result.exported)
                        if (result.missingFiles > 0) {
                            base +
                                LocaleHelper.applyLocale(getApplication())
                                    .getString(
                                        com.phaprodevelop.klipsilo.R.string.backup_missing_files,
                                        result.missingFiles,
                                    )
                        } else {
                            base
                        }
                    } else {
                        localizedString(com.phaprodevelop.klipsilo.R.string.backup_failed)
                    },
                )
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.backup_failed))
            } finally {
                _maintenanceBusy.value = false
            }
        }
    }

    fun restoreClips(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (_maintenanceBusy.value) return
        viewModelScope.launch {
            _maintenanceBusy.value = true
            try {
                var loaded = 0
                var skipped = 0
                var anyInvalid = false
                for (uri in uris) {
                    val result = withContext(ioDispatcher) { repository.restoreClipsFromZip(uri) }
                    if (result.invalid) anyInvalid = true
                    loaded += result.loaded
                    skipped += result.skipped
                }
                toast(
                    when {
                        loaded > 0 -> {
                            val base =
                                LocaleHelper.applyLocale(getApplication())
                                    .getString(com.phaprodevelop.klipsilo.R.string.restore_done, loaded)
                            if (skipped > 0) {
                                base +
                                    LocaleHelper.applyLocale(getApplication())
                                        .getString(com.phaprodevelop.klipsilo.R.string.restore_skipped, skipped)
                            } else {
                                base
                            }
                        }
                        anyInvalid -> localizedString(com.phaprodevelop.klipsilo.R.string.restore_invalid)
                        else -> localizedString(com.phaprodevelop.klipsilo.R.string.restore_failed)
                    },
                )
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.restore_failed))
            } finally {
                _maintenanceBusy.value = false
            }
        }
    }

    fun convertFileToPdf(entry: ClipboardEntry) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val filePath = requireFilePath(entry) ?: return@launch
                val newId =
                    withContext(ioDispatcher) {
                        repository.saveTextFileAsPdf(entry.copy(filePath = filePath))
                    }
                if (newId > 0L) {
                    triggerScrollToTop()
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.pdf_saved))
                } else {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun convertRtfFileToPdf(entry: ClipboardEntry) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val filePath = requireFilePath(entry) ?: return@launch
                val newId =
                    withContext(ioDispatcher) {
                        repository.saveRtfFileAsPdf(entry.copy(filePath = filePath))
                    }
                if (newId > 0L) {
                    triggerScrollToTop()
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.pdf_saved))
                } else {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun convertDocxFileToPdf(entry: ClipboardEntry) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val filePath = requireFilePath(entry) ?: return@launch
                val newId =
                    withContext(ioDispatcher) {
                        repository.saveDocxFileAsPdf(entry.copy(filePath = filePath))
                    }
                if (newId > 0L) {
                    triggerScrollToTop()
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.pdf_saved))
                } else {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun convertXlsxFileToPdf(entry: ClipboardEntry) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val filePath = requireFilePath(entry) ?: return@launch
                val newId =
                    withContext(ioDispatcher) {
                        repository.saveXlsxFileAsPdf(entry.copy(filePath = filePath))
                    }
                if (newId > 0L) {
                    triggerScrollToTop()
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.pdf_saved))
                } else {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun extractPdfPage(
        entry: ClipboardEntry,
        pageIndex: Int,
    ) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val filePath = requireFilePath(entry) ?: return@launch
                val newId =
                    withContext(ioDispatcher) {
                        repository.savePageAsPdf(filePath, pageIndex, entry)
                    }
                if (newId > 0L) {
                    triggerScrollToTop()
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.page_saved_as_pdf))
                } else {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun extractPdfPageImages(
        entry: ClipboardEntry,
        pageIndex: Int,
    ) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val filePath = requireFilePath(entry) ?: return@launch
                val count =
                    withContext(ioDispatcher) {
                        repository.savePdfPageImagesAsClips(filePath, pageIndex, entry)
                    }
                if (count > 0) {
                    triggerScrollToTop()
                    toast(
                        LocaleHelper.applyLocale(getApplication())
                            .getString(com.phaprodevelop.klipsilo.R.string.images_extracted_count, count),
                    )
                } else {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun saveAnnotatedPdf(
        entry: ClipboardEntry,
        annotations: List<PdfAnnotation>,
        strokes: List<PdfStroke> = emptyList(),
    ) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val filePath = requireFilePath(entry) ?: return@launch
                val newId =
                    withContext(ioDispatcher) {
                        repository.savePdfWithAnnotations(filePath, annotations, strokes, entry)
                    }
                if (newId > 0L) {
                    triggerScrollToTop()
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.pdf_saved))
                    _navigateBack.value = true
                } else {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } catch (_: OutOfMemoryError) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun duplicateClip(entry: ClipboardEntry) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val newId =
                    withContext(ioDispatcher) {
                        repository.duplicateEntry(entry)
                    }
                if (newId > 0L) {
                    triggerScrollToTop()
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_duplicated))
                } else {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun combineTextClips(entries: List<ClipboardEntry>) {
        if (entries.size < 2) return
        viewModelScope.launch {
            val combined =
                entries.map { repository.readTextBody(it).orEmpty() }.joinToString("\n\n")
            if (combined.isBlank()) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
                return@launch
            }
            createTextSnip(combined, navigate = false)
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.texts_combined))
        }
    }

    fun saveCurrentLocation() {
        if (_isLocating.value) return
        viewModelScope.launch {
            val app = getApplication<Application>()
            if (!hasLocationPermission(app)) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.location_permission_denied))
                return@launch
            }
            if (!isLocationEnabled(app)) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.location_disabled))
                return@launch
            }
            _isLocating.value = true
            try {
                val location = withContext(ioDispatcher) { obtainCurrentLocation(app) }
                if (location != null) {
                    val saved =
                        withContext(ioDispatcher) {
                            val id = repository.saveLocation(location.latitude, location.longitude)
                            val address = repository.getClip(id)?.previewText.orEmpty()
                            id to address
                        }
                    triggerScrollToTop()
                    postNotice(
                        ClipNotice(
                            message =
                                localizedString(
                                    com.phaprodevelop.klipsilo.R.string.location_saved,
                                    saved.second,
                                ),
                            actionLabel = localizedString(com.phaprodevelop.klipsilo.R.string.snackbar_open),
                            clipId = saved.first,
                        ),
                    )
                } else {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.location_unavailable))
                }
            } catch (_: Exception) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.location_unavailable))
            } finally {
                _isLocating.value = false
            }
        }
    }

    fun openLocation(entry: ClipboardEntry) {
        val lat = entry.latitude ?: return
        val lng = entry.longitude ?: return
        val uri = Uri.parse("geo:0,0?q=$lat,$lng")
        val intent =
            Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        try {
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.file_cannot_open))
        }
    }

    fun copyToClipboard(entry: ClipboardEntry) {
        viewModelScope.launch {
            val ok =
                withContext(ioDispatcher) {
                    repository.copyToClipboard(entry)
                }
            toast(
                localizedString(
                    if (ok) {
                        com.phaprodevelop.klipsilo.R.string.copied_to_clipboard
                    } else {
                        com.phaprodevelop.klipsilo.R.string.file_cannot_open
                    },
                ),
            )
        }
    }

    fun shareText(text: String) {
        if (!repository.shareText(text)) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.file_cannot_open))
        }
    }

    fun shareClip(entry: ClipboardEntry) {
        if (!repository.shareClip(entry)) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.file_cannot_open))
        }
    }

    fun toggleShareSelection(entry: ClipboardEntry) {
        val current = _shareSelectedIds.value
        if (entry.id in current) {
            val updated = LinkedHashSet(current)
            updated.remove(entry.id)
            _shareSelectedIds.value = updated
            if (_shareSelectedIds.value.isEmpty()) {
                _shareSelectionType.value = null
            }
        } else {
            if (current.isEmpty()) {
                _shareSelectionType.value = entry.contentType
                _shareSelectedIds.value = linkedSetOf(entry.id)
            } else if (entry.contentType == _shareSelectionType.value) {
                val updated = LinkedHashSet(current)
                updated.add(entry.id)
                _shareSelectedIds.value = updated
            }
        }
    }

    fun clearShareSelection() {
        _shareSelectedIds.value = LinkedHashSet()
        _shareSelectionType.value = null
    }

    fun shareSelectedItems() {
        val entries = clips.value.filter { it.id in _shareSelectedIds.value }
        if (entries.isEmpty()) return
        val ok =
            if (entries.size == 1) {
                repository.shareClip(entries.first())
            } else {
                repository.shareMultipleClips(entries)
            }
        if (!ok) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.file_cannot_open))
        }
    }

    fun downloadUrlClip(entry: ClipboardEntry) {
        if (entry.contentType != ContentType.URL) return
        if (_isDownloading.value) return
        viewModelScope.launch {
            val url =
                entry.textContent?.trim().takeUnless { it.isNullOrBlank() }
                    ?: repository.readTextBody(entry)?.trim()
            if (url.isNullOrBlank() || !Patterns.WEB_URL.matcher(url).matches()) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.download_failed))
                return@launch
            }
            _isDownloading.value = true
            try {
                val result = withContext(ioDispatcher) { repository.downloadUrlToClip(url) }
                when (result) {
                    SaveClipResult.Saved -> {
                        triggerScrollToTop()
                        toast(localizedString(com.phaprodevelop.klipsilo.R.string.download_completed))
                    }
                    SaveClipResult.TooLarge ->
                        toast(
                            localizedString(com.phaprodevelop.klipsilo.R.string.file_too_large),
                        )
                    SaveClipResult.Failed -> toast(localizedString(com.phaprodevelop.klipsilo.R.string.download_failed))
                }
            } finally {
                _isDownloading.value = false
            }
        }
    }

    fun openFileInBrowser(entry: ClipboardEntry) {
        val path = entry.filePath ?: return
        val file = File(path)
        if (!file.exists()) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.file_cannot_open))
            return
        }
        try {
            val authority = "${getApplication<Application>().packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(getApplication(), authority, file)
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "text/html")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.file_cannot_open))
        }
    }

    fun openUrl(entry: ClipboardEntry) {
        viewModelScope.launch {
            val url = repository.readTextBody(entry) ?: return@launch
            if (!Patterns.WEB_URL.matcher(url).matches()) return@launch
            openHttpUri(url)
        }
    }

    fun openPhone(number: String) {
        val intent =
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${number.replace(" ", "")}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        try {
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun openUrlText(url: String) {
        openHttpUri(url)
    }

    private fun openHttpUri(raw: String) {
        val uri = httpUri(raw) ?: return
        val intent =
            Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        try {
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.file_cannot_open))
        }
    }

    internal fun httpUri(raw: String): Uri? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val schemeToken = trimmed.substringBefore(':', missingDelimiterValue = "")
        val hasExplicitScheme =
            schemeToken.isNotEmpty() &&
                schemeToken.all { it.isLetter() } &&
                trimmed.length > schemeToken.length
        if (hasExplicitScheme) {
            val explicit = schemeToken.lowercase()
            if (explicit != "http" && explicit != "https") return null
        }
        val withScheme =
            when {
                trimmed.startsWith("http://", ignoreCase = true) ||
                    trimmed.startsWith("https://", ignoreCase = true) -> trimmed
                trimmed.contains("://") -> return null
                else -> "https://$trimmed"
            }
        val uri = Uri.parse(withScheme)
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrBlank()) return null
        return uri
    }

    private val pendingDeletes = ConcurrentHashMap<Long, ClipboardEntry>()

    private fun isLockedEntry(id: Long): Boolean = clips.value.find { it.id == id }?.isLocked == true

    fun deleteClipWithUndo(entry: ClipboardEntry) {
        if (entry.isLocked) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_is_locked))
            return
        }
        hideClipsNow(setOf(entry.id))
        viewModelScope.launch {
            val full = repository.getClip(entry.id) ?: entry
            if (full.isLocked) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_is_locked))
                awaitClipReload()
                return@launch
            }
            pendingDeletes[full.id] = full
            setUndoOffer(UndoOffer(localizedString(com.phaprodevelop.klipsilo.R.string.clip_deleted)))
            undoMutex.withLock {
                repository.deleteClipLazy(full)
            }
            awaitClipReload()
            delay(undoTimeoutMs)
            undoMutex.withLock {
                if (pendingDeletes.remove(full.id) != null && repository.getClip(full.id) == null) {
                    repository.deleteClipFile(full)
                }
                if (pendingDeletes.isEmpty()) {
                    setUndoOffer(null)
                }
            }
        }
    }

    fun deleteClipsByType(type: ContentType): Int {
        val candidates = clips.value.filter { it.contentType == type }
        if (candidates.isEmpty()) return 0
        val deletable = candidates.filter { !it.isLocked }
        if (deletable.isEmpty()) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_is_locked))
            return 0
        }
        hideClipsNow(deletable.map { it.id })
        viewModelScope.launch {
            val fullEntries = deletable.map { repository.getClip(it.id) ?: it }
            val locked = fullEntries.filter { it.isLocked }
            val removable = fullEntries.filter { !it.isLocked }
            if (locked.isNotEmpty() && removable.isEmpty()) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_is_locked))
            }
            removable.forEach { pendingDeletes[it.id] = it }
            if (removable.isNotEmpty()) {
                setUndoOffer(
                    UndoOffer(
                        localizedString(com.phaprodevelop.klipsilo.R.string.clips_deleted_count, removable.size),
                    ),
                )
                undoMutex.withLock {
                    repository.deleteClipsLazy(removable)
                }
            }
            awaitClipReload()
            delay(undoTimeoutMs)
            undoMutex.withLock {
                val stillPending =
                    removable.filter {
                        pendingDeletes.remove(it.id) != null && repository.getClip(it.id) == null
                    }
                repository.deleteClipsFiles(stillPending)
                if (pendingDeletes.isEmpty()) {
                    setUndoOffer(null)
                }
            }
        }
        return deletable.size
    }

    fun deleteAllClipsWithUndo(): Int {
        val candidates = clips.value.toList()
        if (candidates.isEmpty()) return 0
        val deletable = candidates.filter { !it.isLocked }
        if (deletable.isEmpty()) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_is_locked))
            return 0
        }
        hideClipsNow(deletable.map { it.id })
        viewModelScope.launch {
            val fullEntries = deletable.map { repository.getClip(it.id) ?: it }
            val removable = fullEntries.filter { !it.isLocked }
            removable.forEach { pendingDeletes[it.id] = it }
            if (removable.isNotEmpty()) {
                setUndoOffer(
                    UndoOffer(
                        localizedString(com.phaprodevelop.klipsilo.R.string.clips_deleted_count, removable.size),
                    ),
                )
                undoMutex.withLock {
                    repository.deleteClipsLazy(removable)
                }
            }
            awaitClipReload()
            delay(undoTimeoutMs)
            undoMutex.withLock {
                val stillPending =
                    removable.filter {
                        pendingDeletes.remove(it.id) != null && repository.getClip(it.id) == null
                    }
                repository.deleteClipsFiles(stillPending)
                if (pendingDeletes.isEmpty()) {
                    setUndoOffer(null)
                }
            }
        }
        return deletable.size
    }

    fun deleteClip(entry: ClipboardEntry) {
        if (entry.isLocked) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_is_locked))
            return
        }
        hideClipsNow(setOf(entry.id))
        viewModelScope.launch {
            val full = repository.getClip(entry.id) ?: entry
            if (full.isLocked) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_is_locked))
                awaitClipReload()
                return@launch
            }
            repository.deleteClip(full)
            awaitClipReload()
        }
    }

    fun undoDelete(): Boolean {
        if (pendingDeletes.isEmpty()) return false
        viewModelScope.launch {
            val restored =
                undoMutex.withLock {
                    val toRestore = pendingDeletes.values.toList()
                    pendingDeletes.clear()
                    if (toRestore.isEmpty()) {
                        false
                    } else {
                        repository.undeleteClips(toRestore)
                        setUndoOffer(null)
                        true
                    }
                }
            awaitClipReload()
            if (restored) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_restored))
            }
        }
        return true
    }

    fun deleteAllClips() {
        val hidden = clips.value.filter { !it.isLocked }.map { it.id }
        hideClipsNow(hidden)
        viewModelScope.launch {
            repository.deleteAllClips()
            awaitClipReload()
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.all_deleted))
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    private fun toastForSave(
        result: SaveClipResult,
        successRes: Int = com.phaprodevelop.klipsilo.R.string.clip_saved,
    ): Int =
        when (result) {
            SaveClipResult.Saved -> successRes
            SaveClipResult.TooLarge -> com.phaprodevelop.klipsilo.R.string.file_too_large
            SaveClipResult.Failed -> com.phaprodevelop.klipsilo.R.string.clip_save_failed
        }

    companion object {
        const val UNDO_TIMEOUT_MS = 4000L
        const val MAX_TEXT_FILE_BYTES = 2L * 1024L * 1024L

        private val textExtensions =
            setOf(
                "txt", "html", "htm", "md", "csv", "xml", "json", "log",
                "yaml", "yml", "cfg", "ini", "css", "js", "ts", "py",
                "kt", "java", "sh", "bash", "sql", "rss", "toml",
            )
    }
}
