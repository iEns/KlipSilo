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
import com.phaprodevelop.klipsilo.data.db.isPlainOrMarkdown
import com.phaprodevelop.klipsilo.data.db.isTextual
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.repository.ClipboardRepository
import com.phaprodevelop.klipsilo.data.repository.FormFieldInfo
import com.phaprodevelop.klipsilo.data.repository.PdfAnnotation
import com.phaprodevelop.klipsilo.data.repository.PdfCombineResult
import com.phaprodevelop.klipsilo.data.repository.PdfImageAnnotation
import com.phaprodevelop.klipsilo.data.repository.PdfStroke
import com.phaprodevelop.klipsilo.data.repository.SaveClipResult
import com.phaprodevelop.klipsilo.data.repository.ScannedItem
import com.phaprodevelop.klipsilo.data.repository.extractDocxText
import com.phaprodevelop.klipsilo.data.repository.extractRtfText
import com.phaprodevelop.klipsilo.data.repository.extractXlsxText
import com.phaprodevelop.klipsilo.data.repository.isProbablyBinary
import com.phaprodevelop.klipsilo.data.settings.PdfFont
import com.phaprodevelop.klipsilo.data.settings.ThemeMode
import com.phaprodevelop.klipsilo.data.settings.ThemeModeStore
import com.phaprodevelop.klipsilo.data.textClipTitle
import com.phaprodevelop.klipsilo.data.textTitleLooksAutomatic
import com.phaprodevelop.klipsilo.hasLocationPermission
import com.phaprodevelop.klipsilo.isLocationEnabled
import com.phaprodevelop.klipsilo.obtainCurrentLocation
import com.phaprodevelop.klipsilo.ui.components.pdfPageCount
import com.phaprodevelop.klipsilo.withoutSoftHyphens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class UndoOffer(val message: String)

/** What the action button on a [ClipNotice] snackbar does. */
sealed interface ClipNoticeAction {
    /** Open the saved clip in the reader or viewer. */
    data class OpenClip(val clipId: Long) : ClipNoticeAction

    /**
     * Retry the last failed save. The ViewModel remembers whether that was a
     * clipboard save or a share so the right content is saved again (#101).
     */
    data object RetrySaveClipboard : ClipNoticeAction
}

/** A snackbar. When [action] is set, [actionLabel] runs it. */
data class ClipNotice(
    val message: String,
    val actionLabel: String? = null,
    val action: ClipNoticeAction? = null,
)

data class DuplicateShareOffer(
    val existingId: Long,
    val displayName: String,
    val imageUri: Uri?,
    val pdfUri: Uri?,
    val fileUri: Uri?,
)

/** A multi-page PDF→images conversion waiting for the user to confirm (#175). */
data class PdfImageConfirmation(
    val entry: ClipboardEntry,
    val pageCount: Int,
)

/** What a [ClipNoticeAction.RetrySaveClipboard] snackbar should save again (#101). */
private sealed interface PendingSaveRetry {
    data object Clipboard : PendingSaveRetry

    data class Share(
        val text: String?,
        val imageUri: Uri?,
        val pdfUri: Uri?,
        val fileUri: Uri?,
    ) : PendingSaveRetry
}

/**
 * In-memory draft markup for one clip (#85). The annotate screen keeps its edit state in
 * Compose, which is lost when the activity is recreated (for example on a language change);
 * holding the draft here lets the screen rehydrate it after the recreate. Never written to disk.
 */
data class MarkupSession(
    val entryId: Long,
    val annotations: Map<Int, List<PdfAnnotation>>,
    val strokes: Map<Int, List<PdfStroke>>,
    val savedAnnotations: List<PdfAnnotation>,
    val savedStrokes: List<PdfStroke>,
    val images: Map<Int, List<PdfImageAnnotation>> = emptyMap(),
    val savedImages: List<PdfImageAnnotation> = emptyList(),
    val pageIndex: Int = 0,
)

class ClipboardViewModel(
    application: Application,
    private val repository: ClipboardRepository,
    private val undoTimeoutMs: Long = UNDO_TIMEOUT_MS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val pageCountProvider: (String) -> Int = ::pdfPageCount,
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

    // #93: the Save clipboard CTA is hidden until there is pasteable content.
    private val _clipboardHasContent = MutableStateFlow(false)
    val clipboardHasContent: StateFlow<Boolean> = _clipboardHasContent.asStateFlow()

    private val primaryClipChangedListener =
        ClipboardManager.OnPrimaryClipChangedListener {
            refreshClipboardHasContent()
        }

    // Declared above init on purpose (#70): Kotlin runs property initialisers
    // and init blocks in source order, and the list collector launched in init
    // can run inside the constructor (Dispatchers.Main.immediate plus a Room
    // flow that emits synchronously on a cold start). Everything init touches
    // must already be initialised here.
    private val _clips = MutableStateFlow<List<ClipboardEntry>>(emptyList())
    val clips: StateFlow<List<ClipboardEntry>> = _clips.asStateFlow()

    private val clipGeneration = AtomicLong(0)

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
            // #127: one-shot kind split (Plain Text / Markdown, HTML text -> File).
            repository.migrateTextKinds()
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
        clipboardManager.addPrimaryClipChangedListener(primaryClipChangedListener)
        refreshClipboardHasContent()
    }

    override fun onCleared() {
        clipboardManager.removePrimaryClipChangedListener(primaryClipChangedListener)
        super.onCleared()
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

    /** Content of the last failed save, kept so Retry repeats that save and not another one. */
    private var pendingSaveRetry: PendingSaveRetry? = null

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

    /**
     * A save from outside the clipboard/share flow failed (for example saving
     * a file to Downloads). Keep offering Retry when an earlier clipboard or
     * share save is still pending, so the snackbar never loses its action.
     */
    fun showSaveFailed() {
        showSaveFailure(pendingSaveRetry)
    }

    /** Retry the last failed clipboard or share save with its remembered content (#101). */
    fun retryLastSave() {
        val pending = pendingSaveRetry ?: return
        pendingSaveRetry = null
        when (pending) {
            PendingSaveRetry.Clipboard -> saveCurrentClipboard()
            is PendingSaveRetry.Share -> saveSharedPayload(pending)
        }
    }

    /**
     * A failed save that can be retried offers Retry in the snackbar. [pending]
     * remembers exactly what to save again; null means the failure cannot be
     * retried (empty content or too large). The clipboard and any editor draft
     * are left untouched so retrying can use the same content (#101).
     */
    private fun showSaveFailure(pending: PendingSaveRetry?) {
        pendingSaveRetry = pending
        if (pending == null) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            return
        }
        postNotice(
            ClipNotice(
                message = localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed),
                actionLabel = localizedString(com.phaprodevelop.klipsilo.R.string.retry),
                action = ClipNoticeAction.RetrySaveClipboard,
            ),
        )
    }

    suspend fun getClip(id: Long): ClipboardEntry? =
        pendingNewDraft.value?.takeIf { it.id == id } ?: repository.getClip(id)

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

    // #175: a PDF with more than one page needs confirmation before it creates
    // one image per page.
    private val _pdfImageConfirmation = MutableStateFlow<PdfImageConfirmation?>(null)
    val pdfImageConfirmation: StateFlow<PdfImageConfirmation?> = _pdfImageConfirmation.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()
    private var locatingJob: Job? = null

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
        // Soft hyphens must not leak into other apps through the clipboard (#62).
        clipboardManager.setPrimaryClip(
            android.content.ClipData.newPlainText("snip", text.withoutSoftHyphens()),
        )
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

    /**
     * A "New text clip" draft that has no database row yet (#42). The row is created
     * only when non-empty content is saved (Back/Save, or ON_STOP), so closing the app
     * can never leave an empty "(untitled)" clip behind.
     */
    private val pendingNewDraft = MutableStateFlow<ClipboardEntry?>(null)

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
        // #42: the new-text editor starts from an in-memory draft. No clip row is
        // inserted up front — the row is created only when non-empty content is
        // saved (Back/Save, or ON_STOP), so closing the app can never leave an
        // empty "(untitled)" clip behind (regression of #39).
        val id = System.nanoTime()
        val draft =
            ClipboardEntry(
                id = id,
                contentType = ContentType.TEXT,
                textContent = null,
                filePath = null,
                previewText = "",
                timestamp = System.currentTimeMillis(),
            )
        pendingNewDraft.value = draft
        _recentNewClipId.value = id
        _navigateToNewClip.value = id
    }

    /**
     * #121: creates a one-page blank A4 PDF clip and hands its id back so the
     * Tools entry point can open it straight in the Mark up editor.
     */
    fun createBlankPdf(onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val displayName = localizedString(com.phaprodevelop.klipsilo.R.string.tool_create_blank_pdf)
            val id = withContext(ioDispatcher) { repository.createBlankPdfClip(displayName) }
            if (id > 0L) {
                triggerScrollToTop()
                onCreated(id)
            } else {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            }
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
     * ON_STOP while the text editor is open — the same lifecycle exit the #39 fix
     * handles (Home, app switch, stop before a process kill; configuration changes
     * are skipped by the caller) (#42):
     * - A new draft with non-blank text gets its clip row created right here. The
     *   write runs blocking so a process killed immediately after ON_STOP cannot
     *   lose the typed text.
     * - A new draft without text stays an in-memory draft: nothing is persisted
     *   and no "(untitled)" row can appear.
     * - An existing clip with unsaved edits is persisted the same way.
     * Returns true when the edit was persisted.
     */
    fun saveDraftOnLifecycleExit(
        entry: ClipboardEntry,
        text: String,
        committedText: String,
    ): Boolean {
        if (entry.isLocked) return false
        if (!entry.contentType.isTextual) return false
        // Only non-empty, actually new content is persisted. Blank edits are left
        // to the explicit Save/Back flow (an existing clip keeps its content).
        if (text.isBlank() || text == committedText) return false
        val isDraft = pendingNewDraft.value?.id == entry.id
        val incoming =
            entry.copy(
                textContent = text,
                previewText = if (isDraft) textClipTitle(text) else entry.previewText,
            )
        return persistTextEntryBlocking(incoming, isDraft)
    }

    /**
     * One blocking, non-cancellable-style write used for the ON_STOP save (#42):
     * the DB write completes even when this call happens right before the process
     * is killed.
     */
    private fun persistTextEntryBlocking(
        entry: ClipboardEntry,
        isDraft: Boolean,
    ): Boolean =
        runBlocking {
            try {
                if (isDraft) {
                    // First save: create the row exactly once, never empty.
                    pendingNewDraft.value = null
                    _recentNewClipId.value = null
                    repository.insertEntry(entry)
                } else {
                    val full = repository.getClip(entry.id) ?: return@runBlocking false
                    if (full.isLocked) return@runBlocking false
                    repository.updateClip(
                        full.copy(
                            textContent = entry.textContent ?: full.textContent,
                            previewText = previewForUpdatedClip(full, entry),
                        ),
                    )
                }
                true
            } catch (_: Exception) {
                false
            }
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

    /** Ask for confirmation when a PDF will produce more than one image (#175). */
    fun requestPdfToImages(entry: ClipboardEntry) {
        if (_isConverting.value || _pdfImageConfirmation.value != null) return
        viewModelScope.launch {
            val filePath = requireFilePath(entry) ?: return@launch
            val pageCount = withContext(ioDispatcher) { pageCountProvider(filePath) }
            if (pageCount > 1) {
                _pdfImageConfirmation.value = PdfImageConfirmation(entry, pageCount)
            } else {
                convertPdfToImages(entry)
            }
        }
    }

    fun confirmPdfToImages() {
        val confirmation = _pdfImageConfirmation.value ?: return
        _pdfImageConfirmation.value = null
        convertPdfToImages(confirmation.entry)
    }

    fun dismissPdfImageConfirmation() {
        _pdfImageConfirmation.value = null
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
        pdfFont: PdfFont? = null,
        navigateBack: Boolean = false,
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
                        repository.saveTextAsPdf(
                            body,
                            entryForPdf,
                            fontSizePt,
                            lineSpacing,
                            pdfFont,
                            markdown = entry.contentType == ContentType.MARKDOWN,
                        )
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

    /**
     * #127: turn a Plain Text clip into Markdown (or back) in place, keeping the
     * same row and body. [body] carries unsaved editor text; the action sheet
     * passes null and edits the stored clip.
     */
    fun setTextClipKind(
        entry: ClipboardEntry,
        target: ContentType,
        body: String? = null,
    ) {
        if (target != ContentType.TEXT && target != ContentType.MARKDOWN) return
        if (entry.isLocked) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_is_locked))
            return
        }
        viewModelScope.launch {
            if (pendingNewDraft.value?.id == entry.id) {
                pendingNewDraft.value = entry.copy(contentType = target)
                toast(localizedString(kindChangedToast(target)))
                return@launch
            }
            val ok =
                withContext(ioDispatcher) {
                    repository.setTextClipKind(entry, target, body?.takeIf { it.isNotBlank() })
                }
            if (ok) {
                triggerScrollToTop()
                toast(localizedString(kindChangedToast(target)))
            } else {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clip_save_failed))
            }
        }
    }

    private fun kindChangedToast(target: ContentType): Int =
        if (target == ContentType.MARKDOWN) {
            com.phaprodevelop.klipsilo.R.string.converted_to_markdown
        } else {
            com.phaprodevelop.klipsilo.R.string.converted_to_plain_text
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
            if (base.contentType.isPlainOrMarkdown &&
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
        saveEntryRow(updated)
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

    private var markupSession: MarkupSession? = null

    /** The draft markup for [entryId], or null when the screen has no unsaved marks. */
    fun markupSessionOrNull(entryId: Long): MarkupSession? = markupSession?.takeIf { it.entryId == entryId }

    fun updateMarkupSession(session: MarkupSession) {
        markupSession = session
    }

    /** Clear the held draft; when [entryId] is set, only clear that clip's session. */
    fun clearMarkupSession(entryId: Long? = null) {
        val current = markupSession ?: return
        if (entryId == null || current.entryId == entryId) {
            markupSession = null
        }
    }

    /**
     * The fill-form preview bitmap (#80): one page of a temporary, filled copy of
     * the PDF whose field appearances were regenerated — no Compose value overlay
     * is drawn on top. Returns null when the file is unreadable or has no form.
     */
    suspend fun renderFilledFormPage(
        pdfPath: String,
        values: Map<String, String>,
        pageIndex: Int,
        maxDimension: Int,
    ): Bitmap? = repository.renderFilledFormPage(pdfPath, values, pageIndex, maxDimension)

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
        pdfFont: PdfFont? = null,
        markdown: Boolean = false,
    ): List<Bitmap> {
        return withContext(ioDispatcher) {
            repository.previewTextAsPdf(text, fontSizePt, lineSpacing, pdfFont, markdown)
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
                            action = ClipNoticeAction.OpenClip(id),
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
        contentType: ContentType = ContentType.TEXT,
    ): Long {
        val id = System.nanoTime()
        val entry =
            ClipboardEntry(
                id = id,
                contentType = contentType,
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

    /** Re-check whether the clipboard currently holds text, HTML or a URI (#93). */
    fun refreshClipboardHasContent() {
        _clipboardHasContent.value =
            try {
                val clipData = clipboardManager.primaryClip
                if (clipData == null || clipData.itemCount == 0) {
                    false
                } else {
                    val item = clipData.getItemAt(0)
                    item.uri != null || !item.text.isNullOrBlank() || !item.htmlText.isNullOrBlank()
                }
            } catch (_: SecurityException) {
                false
            } catch (_: Exception) {
                false
            }
    }

    fun saveCurrentClipboard() {
        // A fresh attempt replaces whatever an older Retry would have saved.
        pendingSaveRetry = null
        try {
            val clipData = clipboardManager.primaryClip
            if (clipData == null || clipData.itemCount == 0) {
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clipboard_empty))
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
                toast(localizedString(com.phaprodevelop.klipsilo.R.string.clipboard_no_saveable_content))
                return
            }

            viewModelScope.launch {
                val result =
                    try {
                        withContext(ioDispatcher) {
                            repository.saveNewClip(text, imageUri, pdfUri, fileUri)
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        SaveClipResult.Failed
                    }
                if (result == SaveClipResult.Failed) {
                    showSaveFailure(PendingSaveRetry.Clipboard)
                } else {
                    toast(localizedString(toastForSave(result)))
                }
                if (result == SaveClipResult.Saved) {
                    triggerScrollToTop()
                }
            }
        } catch (_: Exception) {
            // Retry only when the clipboard still looks pasteable, so a
            // permission error or an empty clipboard does not loop.
            refreshClipboardHasContent()
            showSaveFailure(
                if (_clipboardHasContent.value) PendingSaveRetry.Clipboard else null,
            )
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
        // A fresh share replaces whatever an older Retry would have saved.
        pendingSaveRetry = null
        if (!hasStream && !hasText) {
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.share_no_saveable_content))
            return
        }

        saveSharedPayload(
            PendingSaveRetry.Share(
                text = if (hasText) text else null,
                imageUri = imageUri,
                pdfUri = pdfUri,
                fileUri = fileUri,
            ),
        )
    }

    /**
     * Saves a share's stream and/or text. Retry reuses the remembered payload,
     * so a failed ACTION_SEND save never falls back to the clipboard (#101).
     */
    private fun saveSharedPayload(payload: PendingSaveRetry.Share) {
        viewModelScope.launch {
            val results = mutableListOf<SaveClipResult>()
            var duplicateOffer: DuplicateShareOffer? = null
            val streamForCheck = payload.imageUri ?: payload.pdfUri ?: payload.fileUri
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
                            imageUri = payload.imageUri,
                            pdfUri = payload.pdfUri,
                            fileUri = payload.fileUri,
                        )
                }
            }
            if (duplicateOffer != null) {
                _duplicateShare.value = duplicateOffer
            } else if (streamForCheck != null) {
                results +=
                    withContext(ioDispatcher) {
                        repository.saveNewClip(null, payload.imageUri, payload.pdfUri, payload.fileUri)
                    }
            }
            if (payload.text != null) {
                results +=
                    withContext(ioDispatcher) {
                        repository.saveNewClip(payload.text, null, null, null)
                    }
            }
            if (results.isNotEmpty()) {
                val result =
                    when {
                        results.any { it == SaveClipResult.Saved } -> SaveClipResult.Saved
                        results.any { it == SaveClipResult.TooLarge } -> SaveClipResult.TooLarge
                        else -> SaveClipResult.Failed
                    }
                when (result) {
                    SaveClipResult.Failed -> showSaveFailure(payload)
                    SaveClipResult.TooLarge -> {
                        pendingSaveRetry = null
                        toast(localizedString(toastForSave(result)))
                    }
                    SaveClipResult.Saved -> {
                        pendingSaveRetry = null
                        toast(localizedString(toastForSave(result)))
                    }
                }
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

    /**
     * Persists a clip row: the first save of a #42 draft creates the row with its
     * content (never an empty one); afterwards the same row is updated, so repeated
     * saves cannot produce duplicates.
     */
    private suspend fun saveEntryRow(entry: ClipboardEntry) {
        if (pendingNewDraft.value?.id == entry.id) {
            pendingNewDraft.value = null
            _recentNewClipId.value = null
            // The row is created only when the draft has non-empty content; a draft
            // whose text was typed and then erased is discarded in memory, leaving no
            // "(untitled)" artifact behind (#42).
            if (entry.textContent.isNullOrBlank() && entry.filePath == null) return
            repository.insertEntry(entry)
            triggerScrollToTop()
        } else {
            repository.updateClip(entry)
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
                    if (entry.contentType.isTextual) {
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
                saveEntryRow(toSave)
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
        if (incoming.contentType.isPlainOrMarkdown) {
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
                        // #108: every valid clip was already present — that is a
                        // skipped restore, not a failed one.
                        loaded > 0 || (skipped > 0 && !anyInvalid) -> {
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

    /** Copies a picked image into Mark up storage (#92); null when it could not be read. */
    suspend fun importMarkupImage(uri: Uri): String? = withContext(ioDispatcher) { repository.importMarkupImage(uri) }

    /** Camera captures arrive as a cache file; copy them into Mark up storage (#92). */
    suspend fun importMarkupImageFile(path: String): String? =
        withContext(ioDispatcher) { repository.importMarkupImageFile(path) }

    fun saveAnnotatedPdf(
        entry: ClipboardEntry,
        annotations: List<PdfAnnotation>,
        strokes: List<PdfStroke> = emptyList(),
        images: List<PdfImageAnnotation> = emptyList(),
    ) {
        if (_isConverting.value) return
        viewModelScope.launch {
            _isConverting.value = true
            try {
                val filePath = requireFilePath(entry) ?: return@launch
                val newId =
                    withContext(ioDispatcher) {
                        repository.savePdfWithAnnotations(filePath, annotations, strokes, entry, images)
                    }
                if (newId > 0L) {
                    triggerScrollToTop()
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.pdf_saved))
                    clearMarkupSession(entry.id)
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
            // #127: mixed Plain + Markdown combines to Markdown, all plain stays plain.
            createTextSnip(
                combined,
                navigate = false,
                contentType =
                    if (entries.any { it.contentType == ContentType.MARKDOWN }) {
                        ContentType.MARKDOWN
                    } else {
                        ContentType.TEXT
                    },
            )
            toast(localizedString(com.phaprodevelop.klipsilo.R.string.texts_combined))
        }
    }

    fun saveCurrentLocation() {
        if (_isLocating.value) return
        val job =
            viewModelScope.launch(start = CoroutineStart.LAZY) {
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
                                action = ClipNoticeAction.OpenClip(saved.first),
                            ),
                        )
                    } else {
                        toast(localizedString(com.phaprodevelop.klipsilo.R.string.location_unavailable))
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    toast(localizedString(com.phaprodevelop.klipsilo.R.string.location_unavailable))
                } finally {
                    // #106: a cancelled job must not hide the dialog belonging
                    // to a newer locating attempt.
                    if (locatingJob === coroutineContext[Job]) {
                        locatingJob = null
                        _isLocating.value = false
                    }
                }
            }
        locatingJob = job
        job.start()
    }

    /** Dismisses the "Getting location" dialog and stops the pending lookup (#106). */
    fun cancelLocating() {
        val job = locatingJob
        locatingJob = null
        job?.cancel()
        _isLocating.value = false
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

    fun shareClipAsFile(entry: ClipboardEntry) {
        if (!repository.shareTextClipAsFile(entry)) {
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
        if (pendingNewDraft.value?.id == entry.id) {
            // #42: the draft row was never created — nothing to delete from the DB.
            pendingNewDraft.value = null
        }
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
