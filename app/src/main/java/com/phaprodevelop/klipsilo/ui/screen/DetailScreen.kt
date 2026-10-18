package com.phaprodevelop.klipsilo.ui.screen

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.repository.extractDocxText
import com.phaprodevelop.klipsilo.data.repository.extractRtfText
import com.phaprodevelop.klipsilo.data.repository.extractXlsxText
import com.phaprodevelop.klipsilo.data.repository.isProbablyBinary
import com.phaprodevelop.klipsilo.data.settings.PdfSettingsStore
import com.phaprodevelop.klipsilo.ui.components.ButtonLabel
import com.phaprodevelop.klipsilo.ui.components.DialogTitleLabel
import com.phaprodevelop.klipsilo.ui.components.FitActionBar
import com.phaprodevelop.klipsilo.ui.components.HtmlClipPane
import com.phaprodevelop.klipsilo.ui.components.PageSelector
import com.phaprodevelop.klipsilo.ui.components.PdfBarActionSpec
import com.phaprodevelop.klipsilo.ui.components.PdfViewerActionBar
import com.phaprodevelop.klipsilo.ui.components.TopBarIconAction
import com.phaprodevelop.klipsilo.ui.components.looksLikeHtmlDocument
import com.phaprodevelop.klipsilo.ui.components.pdfPageCount
import com.phaprodevelop.klipsilo.ui.components.produceLatest
import com.phaprodevelop.klipsilo.ui.components.rememberBitmapRecycler
import com.phaprodevelop.klipsilo.ui.components.rememberDisplayedBitmap
import com.phaprodevelop.klipsilo.ui.components.renderPdfPage
import com.phaprodevelop.klipsilo.ui.components.typefaceForPdfFont
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel
import com.phaprodevelop.klipsilo.withoutSoftHyphens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)
@Composable
fun DetailScreen(
    entryId: Long,
    viewModel: ClipboardViewModel,
    onNavigateBack: () -> Unit,
) {
    val clips by viewModel.clips.collectAsState()
    var fullEntry by remember(entryId) { mutableStateOf<ClipboardEntry?>(null) }
    val listEntry = clips.find { it.id == entryId }
    val entry = fullEntry ?: listEntry
    val context = LocalContext.current
    val pdfSettings = remember { PdfSettingsStore.load(context) }
    val navigateBack by viewModel.navigateBack.collectAsState()
    LaunchedEffect(navigateBack) {
        if (navigateBack) {
            onNavigateBack()
            viewModel.clearNavigateBack()
        }
    }
    LaunchedEffect(entryId) {
        val loaded = viewModel.getClip(entryId) ?: return@LaunchedEffect
        val body =
            if (loaded.contentType == ContentType.TEXT || loaded.contentType == ContentType.URL) {
                viewModel.readTextBody(loaded)
            } else {
                loaded.textContent
            }
        fullEntry = loaded.copy(textContent = body ?: loaded.textContent)
    }

    if (entry == null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = context.getString(R.string.clip_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(16.dp))
                TextButton(onClick = onNavigateBack) {
                    ButtonLabel(context.getString(R.string.back))
                }
            }
        }
        return
    }

    val isNewEmpty = viewModel.isNewEmptyDraft(entry)
    val canAutoDelete = isNewEmpty

    var editedText by rememberSaveable(
        entryId,
        stateSaver = TextFieldValue.Saver,
    ) {
        mutableStateOf(TextFieldValue(entry.textContent ?: ""))
    }
    var committedText by rememberSaveable(entryId) { mutableStateOf(entry.textContent ?: "") }
    var bodyApplied by remember(entryId) { mutableStateOf(false) }
    LaunchedEffect(fullEntry?.textContent) {
        if (bodyApplied) return@LaunchedEffect
        val body = fullEntry?.textContent ?: return@LaunchedEffect
        if (editedText.text.isEmpty()) {
            editedText = TextFieldValue(body)
            committedText = body
        }
        bodyApplied = true
    }
    var savedFlash by remember { mutableStateOf(false) }
    var undoStack by remember { mutableStateOf(listOf<TextFieldValue>()) }
    var redoStack by remember { mutableStateOf(listOf<TextFieldValue>()) }
    var fontSize by rememberSaveable { mutableStateOf(16) }
    var showPreview by remember { mutableStateOf(false) }
    var htmlSourceShown by rememberSaveable(entryId) { mutableStateOf(false) }
    // Key on the loaded body: list rows carry no textContent, so a first-only key
    // would leave HTML clips on the raw-source editor forever (#32).
    val isHtmlDocument = remember(entryId, entry.textContent) { looksLikeHtmlDocument(entry.textContent) }
    var previewPages by remember { mutableStateOf<List<Bitmap>?>(null) }
    val previewRecycler = rememberBitmapRecycler()
    val previewGate = remember { AtomicInteger(0) }
    var previewLoading by remember { mutableStateOf(false) }
    var previewZoomPage by remember { mutableStateOf<Int?>(null) }
    var selectStage by remember { mutableIntStateOf(0) }
    var showSelectMenu by remember { mutableStateOf(false) }
    val view = LocalView.current
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(savedFlash) {
        if (savedFlash) {
            delay(900)
            savedFlash = false
        }
    }
    LaunchedEffect(imeVisible) {
        if (imeVisible) {
            delay(300)
            val selection = editedText.selection
            if (selection.collapsed && selection.start > 0) {
                editedText = editedText.copy(selection = TextRange(selection.start - 1))
                delay(60)
                editedText = editedText.copy(selection = selection)
            }
        }
    }
    LaunchedEffect(showPreview, editedText.text, fontSize) {
        if (showPreview && editedText.text.isNotBlank()) {
            previewLoading = true
            val rendered =
                produceLatest(
                    previewGate,
                    produce = {
                        viewModel.renderTextPreview(
                            editedText.text,
                            fontSize.toFloat(),
                            pdfSettings.lineSpacing,
                        )
                    },
                    discard = { pages -> pages.forEach { it.recycle() } },
                ) ?: return@LaunchedEffect
            val oldPages = previewPages
            previewPages = rendered.value
            previewLoading = false
            previewRecycler.retireAll(oldPages.orEmpty())
        } else {
            val oldPages = previewPages
            previewPages = null
            previewRecycler.retireAll(oldPages.orEmpty())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            previewRecycler.retireAll(previewPages.orEmpty(), notify = false)
        }
    }

    fun doUndo() {
        undoStack.lastOrNull()?.let { previous ->
            redoStack = (redoStack + editedText).takeLast(50)
            editedText = previous
            undoStack = undoStack.dropLast(1)
        }
    }

    fun doRedo() {
        redoStack.lastOrNull()?.let { next ->
            undoStack = (undoStack + editedText).takeLast(50)
            editedText = next
            redoStack = redoStack.dropLast(1)
        }
    }

    fun paragraphStart(
        text: String,
        cursor: Int,
    ): Int {
        if (cursor <= 0) return 0
        var boundary = 0
        var search = 0
        while (true) {
            val found = text.indexOf("\n\n", search)
            if (found == -1 || found + 2 > cursor) break
            boundary = found + 2
            search = found + 2
        }
        return boundary
    }

    fun paragraphEnd(
        text: String,
        cursor: Int,
    ): Int {
        if (cursor >= text.length) return text.length
        val found = text.indexOf("\n\n", cursor)
        return if (found == -1) text.length else found
    }

    fun selectCurrentParagraph() {
        val text = editedText.text
        if (text.isEmpty()) return
        val cursor = editedText.selection.start.coerceIn(0, text.length)
        val start = paragraphStart(text, cursor)
        val end = paragraphEnd(text, cursor)
        if (end > start && !text.substring(start, end).isBlank()) {
            editedText = editedText.copy(selection = TextRange(start, end))
        }
    }

    fun selectWordAtCursor() {
        val text = editedText.text
        if (text.isEmpty()) return
        val cursor = editedText.selection.start.coerceIn(0, text.length)
        if (cursor > 0 && !text[cursor - 1].isWhitespace()) {
            var start = cursor
            while (start > 0 && !text[start - 1].isWhitespace()) start--
            val end =
                if (cursor < text.length && !text[cursor].isWhitespace()) {
                    var e = cursor + 1
                    while (e < text.length && !text[e].isWhitespace()) e++
                    e
                } else {
                    cursor
                }
            editedText = editedText.copy(selection = TextRange(start, end))
        } else {
            var fwd = cursor
            while (fwd < text.length && text[fwd].isWhitespace()) fwd++
            if (fwd >= text.length) return
            var start = fwd
            var end = fwd + 1
            while (end < text.length && !text[end].isWhitespace()) end++
            editedText = editedText.copy(selection = TextRange(start, end))
        }
    }

    fun selectSentenceAtCursor() {
        val text = editedText.text
        if (text.isEmpty()) return
        val cursor = editedText.selection.start.coerceIn(0, text.length)
        val paraStart = paragraphStart(text, cursor)
        val paraEnd = paragraphEnd(text, cursor)
        val terminators = charArrayOf('.', '!', '?')
        var start = paraStart
        var i = cursor - 1
        while (i >= paraStart) {
            if (text[i] in terminators) {
                start = i + 1
                break
            }
            i--
        }
        var end = paraEnd
        i = cursor
        while (i < paraEnd) {
            if (text[i] in terminators) {
                end = i + 1
                break
            }
            i++
        }
        if (end > start) {
            editedText = editedText.copy(selection = TextRange(start, end))
        }
    }

    fun cycleSelect() {
        if (editedText.selection.collapsed) {
            selectStage = 0
        }
        when (selectStage) {
            0 -> selectWordAtCursor()
            1 -> selectSentenceAtCursor()
            else -> selectCurrentParagraph()
        }
        selectStage = (selectStage + 1) % 3
    }

    fun insertMarkdown(
        before: String,
        after: String,
        prefixLine: Boolean,
    ) {
        val newValue: TextFieldValue? =
            try {
                val sel = editedText.selection
                val text = editedText.text
                if (prefixLine) {
                    val lineStart =
                        if (sel.start == 0) {
                            0
                        } else {
                            text.lastIndexOf(
                                '\n',
                                sel.start - 1,
                            ).let { if (it == -1) 0 else it + 1 }
                        }
                    val insertAt =
                        if (sel.collapsed) {
                            lineStart
                        } else {
                            val selLineStart =
                                if (sel.start == 0) {
                                    0
                                } else {
                                    text.lastIndexOf(
                                        '\n',
                                        sel.start - 1,
                                    ).let { if (it == -1) 0 else it + 1 }
                                }
                            selLineStart
                        }
                    val newText = text.substring(0, insertAt) + before + text.substring(insertAt)
                    val newCursor =
                        (if (sel.collapsed) sel.start + before.length else sel.end + before.length)
                            .coerceIn(0, newText.length)
                    TextFieldValue(newText, TextRange(newCursor))
                } else if (sel.collapsed) {
                    val beforePart = text.substring(0, sel.start)
                    val afterPart = text.substring(sel.start)
                    val newText = beforePart + before + after + afterPart
                    TextFieldValue(
                        newText,
                        TextRange((sel.start + before.length).coerceIn(0, newText.length)),
                    )
                } else {
                    val selected = text.substring(sel.start, sel.end)
                    val newText = text.substring(0, sel.start) + before + selected + after + text.substring(sel.end)
                    val newEnd = (sel.end + before.length + after.length).coerceIn(0, newText.length)
                    TextFieldValue(newText, TextRange(newEnd))
                }
            } catch (_: Exception) {
                null
            }
        if (newValue != null) {
            undoStack = (undoStack + editedText).takeLast(50)
            redoStack = emptyList()
            editedText = newValue
        } else {
            val safeStart = editedText.selection.start.coerceIn(0, editedText.text.length)
            editedText = editedText.copy(selection = TextRange(safeStart))
        }
    }

    val hasChanges =
        (entry.contentType == ContentType.TEXT || entry.contentType == ContentType.URL) &&
            editedText.text != committedText
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showMdDialog by remember { mutableStateOf(false) }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { DialogTitleLabel(context.getString(R.string.editor_leave_title)) },
            text = { Text(context.getString(R.string.editor_leave_message)) },
            confirmButton = {
                FlowRow {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        ButtonLabel(context.getString(R.string.cancel))
                    }
                    TextButton(onClick = {
                        showDiscardDialog = false
                        if (canAutoDelete) {
                            viewModel.deleteClip(entry)
                            viewModel.clearRecentNewClipId()
                        }
                        onNavigateBack()
                    }) {
                        ButtonLabel(context.getString(R.string.discard))
                    }
                    TextButton(onClick = {
                        showDiscardDialog = false
                        if (entry.isLocked) return@TextButton
                        if (isNewEmpty && editedText.text.isBlank()) {
                            viewModel.deleteClip(entry)
                            viewModel.clearRecentNewClipId()
                            onNavigateBack()
                            return@TextButton
                        }
                        val txt = editedText.text
                        val updated =
                            if (isNewEmpty) {
                                entry.copy(
                                    textContent = txt.ifBlank { null },
                                    previewText =
                                        com.phaprodevelop.klipsilo.data.textClipTitle(txt),
                                )
                            } else {
                                entry.copy(
                                    textContent = txt.ifBlank { null },
                                )
                            }
                        committedText = txt
                        ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                        viewModel.updateClip(updated)
                        if (isNewEmpty && txt.isNotBlank()) {
                            viewModel.clearRecentNewClipId()
                        }
                        onNavigateBack()
                    }) {
                        ButtonLabel(context.getString(R.string.save))
                    }
                }
            },
            dismissButton = {},
        )
    }

    BackHandler {
        if (canAutoDelete && editedText.text.isBlank()) {
            viewModel.deleteClip(entry)
            viewModel.clearRecentNewClipId()
            onNavigateBack()
        } else if (hasChanges) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    // Leaving the app through the lifecycle (Home, app switch, stop before process
    // kill) while the editor holds unsaved text (#42, same hook as the #39 fix):
    // a new draft with typed text gets its clip row created right here — blocking,
    // so a process killed immediately after cannot lose it — and an existing clip's
    // edits are persisted too. Blank drafts stay in memory only; configuration
    // changes are skipped since the recreated screen resumes the edit.
    val lifecycleOwner = LocalLifecycleOwner.current
    val hostActivity =
        remember(context) {
            var ctx = context
            while (ctx is ContextWrapper && ctx !is Activity) ctx = ctx.baseContext
            ctx as? Activity
        }
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP && hostActivity?.isChangingConfigurations != true) {
                    if (viewModel.saveDraftOnLifecycleExit(entry, editedText.text, committedText)) {
                        committedText = editedText.text
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showQrDialog) {
        QrResultDialog(
            viewModel = viewModel,
            onDismiss = {
                showQrDialog = false
                viewModel.clearQr()
            },
        )
    }

    if (showMdDialog) {
        MarkdownInsertDialog(
            onDismiss = { showMdDialog = false },
            onPick = { before, after, prefixLine ->
                insertMarkdown(before, after, prefixLine)
                showMdDialog = false
            },
        )
    }

    val zoomIndex = previewZoomPage
    if (zoomIndex != null) {
        val bitmap = previewPages?.getOrNull(zoomIndex)
        if (bitmap != null) {
            ZoomPreviewDialog(
                bitmap = bitmap,
                pageNumber = zoomIndex + 1,
                onDismiss = { previewZoomPage = null },
            )
        } else {
            previewZoomPage = null
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // #62: the editor top bar. The growing row gives the title the
            // same fit-text rule as the other top bars ("Bearbeiten"/
            // "Rediger" used to break as "B/ea…" at font scale 2.0 and
            // "Bearb/eiten" at 320dp), and the actions degrade labelled →
            // icon-only → ⋮ overflow so they stop crowding the title.
            FitActionBar(
                title =
                    when (entry.contentType) {
                        ContentType.TEXT, ContentType.URL -> context.getString(R.string.text_editor)
                        ContentType.PDF -> pdfFileTitle(entry)
                        else -> ""
                    },
                actions =
                    buildList {
                        val isEditableText =
                            entry.contentType == ContentType.TEXT || entry.contentType == ContentType.URL
                        if (entry.contentType == ContentType.TEXT) {
                            add(
                                TopBarIconAction(
                                    icon =
                                        if (isHtmlDocument && !htmlSourceShown) {
                                            Icons.Filled.Visibility
                                        } else if (showPreview) {
                                            Icons.Filled.VisibilityOff
                                        } else {
                                            Icons.Filled.Visibility
                                        },
                                    label =
                                        if (isHtmlDocument) {
                                            if (htmlSourceShown) {
                                                context.getString(R.string.preview)
                                            } else {
                                                context.getString(R.string.source)
                                            }
                                        } else {
                                            context.getString(R.string.preview)
                                        },
                                    enabled = editedText.text.isNotBlank(),
                                ) {
                                    ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                                    if (isHtmlDocument) {
                                        htmlSourceShown = !htmlSourceShown
                                    } else {
                                        showPreview = !showPreview
                                    }
                                },
                            )
                            add(
                                TopBarIconAction(
                                    icon = Icons.Filled.PictureAsPdf,
                                    label = context.getString(R.string.text_to_pdf),
                                    enabled = editedText.text.isNotBlank(),
                                ) {
                                    ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                                    val txt = editedText.text
                                    if (txt.isNotBlank()) {
                                        committedText = txt
                                        savedFlash = true
                                        if (isNewEmpty) {
                                            viewModel.clearRecentNewClipId()
                                        }
                                    }
                                    viewModel.convertTextToPdf(
                                        txt,
                                        entry,
                                        fontSizePt = fontSize.toFloat(),
                                        lineSpacing = pdfSettings.lineSpacing,
                                        navigateBack = false,
                                    )
                                },
                            )
                        }
                        if (isEditableText) {
                            add(
                                TopBarIconAction(
                                    icon = Icons.Filled.Share,
                                    label = context.getString(R.string.share),
                                    enabled = editedText.text.isNotBlank(),
                                ) {
                                    val textToShare = editedText.text
                                    if (textToShare.isBlank()) return@TopBarIconAction
                                    if (!entry.isLocked) {
                                        val updated =
                                            if (isNewEmpty) {
                                                val preview =
                                                    if (textToShare.isBlank()) {
                                                        ""
                                                    } else {
                                                        com.phaprodevelop.klipsilo.data.textClipTitle(textToShare)
                                                    }
                                                entry.copy(
                                                    textContent = textToShare.ifBlank { null },
                                                    previewText = preview,
                                                )
                                            } else {
                                                entry.copy(
                                                    textContent = textToShare.ifBlank { null },
                                                )
                                            }
                                        committedText = textToShare
                                        savedFlash = true
                                        ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                                        viewModel.updateClip(updated)
                                        if (isNewEmpty && textToShare.isNotBlank()) {
                                            viewModel.clearRecentNewClipId()
                                        }
                                    }
                                    ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                                    viewModel.shareText(textToShare)
                                },
                            )
                        }
                        if (isEditableText && !entry.isLocked) {
                            add(
                                TopBarIconAction(
                                    icon = Icons.Filled.Check,
                                    label = context.getString(R.string.save),
                                    tint =
                                        if (savedFlash) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            LocalContentColor.current
                                        },
                                ) {
                                    if (isNewEmpty && editedText.text.isBlank()) {
                                        viewModel.deleteClip(entry)
                                        viewModel.clearRecentNewClipId()
                                        onNavigateBack()
                                        return@TopBarIconAction
                                    }
                                    val txt = editedText.text
                                    val updated =
                                        if (isNewEmpty) {
                                            val preview =
                                                if (txt.isBlank()) {
                                                    ""
                                                } else {
                                                    com.phaprodevelop.klipsilo.data.textClipTitle(txt)
                                                }
                                            entry.copy(
                                                textContent = txt.ifBlank { null },
                                                previewText = preview,
                                            )
                                        } else {
                                            entry.copy(
                                                textContent = txt.ifBlank { null },
                                            )
                                        }
                                    committedText = txt
                                    savedFlash = true
                                    ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                                    viewModel.updateClip(updated)
                                    if (isNewEmpty && editedText.text.isNotBlank()) {
                                        viewModel.clearRecentNewClipId()
                                    }
                                },
                            )
                        }
                    },
                onBack = {
                    if (canAutoDelete && editedText.text.isBlank()) {
                        viewModel.deleteClip(entry)
                        viewModel.clearRecentNewClipId()
                        onNavigateBack()
                    } else if (hasChanges) {
                        showDiscardDialog = true
                    } else {
                        onNavigateBack()
                    }
                },
                backContentDescription = context.getString(R.string.back),
                moreContentDescription = context.getString(R.string.more_options),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (entry.contentType) {
                ContentType.TEXT, ContentType.URL -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .then(
                                    if (showPreview) {
                                        Modifier
                                    } else if (!imeVisible) {
                                        Modifier.navigationBarsPadding()
                                    } else {
                                        Modifier
                                    },
                                ),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            EditorToolButton(
                                icon = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = context.getString(R.string.undo).withoutSoftHyphens(),
                                enabled = undoStack.isNotEmpty(),
                            ) { doUndo() }
                            EditorToolButton(
                                icon = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = context.getString(R.string.redo).withoutSoftHyphens(),
                                enabled = redoStack.isNotEmpty(),
                            ) { doRedo() }
                            if (entry.contentType == ContentType.TEXT && !entry.isLocked) {
                                Spacer(Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                                        showMdDialog = true
                                    },
                                    modifier =
                                        Modifier.clearAndSetSemantics {
                                            contentDescription =
                                                context.getString(R.string.md_title).withoutSoftHyphens()
                                        },
                                    enabled = !showPreview,
                                ) {
                                    ButtonLabel(
                                        text = context.getString(R.string.md_mark),
                                        style =
                                            MaterialTheme.typography.labelLarge.copy(
                                                fontFamily = FontFamily.Monospace,
                                            ),
                                        color =
                                            if (!showPreview) {
                                                LocalContentColor.current
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            },
                                        maxLines = 1,
                                    )
                                }
                                Box {
                                    val selectEnabled =
                                        !showPreview &&
                                            editedText.text.isNotBlank()
                                    Box(
                                        modifier =
                                            Modifier
                                                .clip(CircleShape)
                                                .combinedClickable(
                                                    onClick = {
                                                        ViewCompat.performHapticFeedback(
                                                            view,
                                                            HapticFeedbackConstantsCompat.CONFIRM,
                                                        )
                                                        cycleSelect()
                                                    },
                                                    onLongClick = {
                                                        ViewCompat.performHapticFeedback(
                                                            view,
                                                            HapticFeedbackConstantsCompat.LONG_PRESS,
                                                        )
                                                        showSelectMenu = true
                                                    },
                                                    enabled = selectEnabled,
                                                )
                                                .padding(12.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.SelectAll,
                                            contentDescription =
                                                context.getString(
                                                    R.string.select_cycle,
                                                ).withoutSoftHyphens(),
                                            tint =
                                                if (selectEnabled) {
                                                    LocalContentColor.current
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                },
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showSelectMenu,
                                        onDismissRequest = { showSelectMenu = false },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(context.getString(R.string.select_word)) },
                                            onClick = {
                                                showSelectMenu = false
                                                selectWordAtCursor()
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(context.getString(R.string.select_sentence)) },
                                            onClick = {
                                                showSelectMenu = false
                                                selectSentenceAtCursor()
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(context.getString(R.string.select_paragraph)) },
                                            onClick = {
                                                showSelectMenu = false
                                                selectCurrentParagraph()
                                            },
                                        )
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            EditorToolButton(
                                icon = Icons.Filled.TextDecrease,
                                contentDescription =
                                    context.getString(
                                        R.string.decrease_text_size,
                                    ).withoutSoftHyphens(),
                                enabled = fontSize > 10,
                            ) { fontSize-- }
                            EditorToolButton(
                                icon = Icons.Filled.TextIncrease,
                                contentDescription =
                                    context.getString(
                                        R.string.increase_text_size,
                                    ).withoutSoftHyphens(),
                                enabled = fontSize < 28,
                            ) { fontSize++ }
                        }
                        if (showPreview) {
                            TextPreviewPane(
                                pages = previewPages,
                                loading = previewLoading,
                                onPageTap = { previewZoomPage = it },
                                modifier = Modifier.weight(1f),
                            )
                        } else if (isHtmlDocument && !htmlSourceShown) {
                            // Read in composition so a body that arrives after the first
                            // frame (TEXT list rows store no textContent) still loads.
                            HtmlClipPane(
                                html = editedText.text,
                                modifier = Modifier.weight(1f).fillMaxWidth().imePadding(),
                            )
                        } else {
                            OutlinedTextField(
                                value = editedText,
                                onValueChange = { new ->
                                    if (new.text != editedText.text) {
                                        undoStack = (undoStack + editedText).takeLast(50)
                                        redoStack = emptyList()
                                        editedText = new
                                    } else {
                                        editedText = new
                                    }
                                },
                                readOnly = entry.isLocked,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .imePadding()
                                        .testTag("klips_editor_text_field"),
                                textStyle =
                                    MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = fontSize.sp,
                                        lineHeight = (fontSize * pdfSettings.lineSpacing).sp,
                                        fontFamily = FontFamily(typefaceForPdfFont(context, pdfSettings.pdfFont)),
                                    ),
                            )
                        }
                    }
                }
                else -> {
                    Box(Modifier.fillMaxSize().navigationBarsPadding()) {
                        when (entry.contentType) {
                            ContentType.IMAGE ->
                                ImageViewer(
                                    filePath = entry.filePath,
                                    onScanQr = {
                                        showQrDialog = true
                                        viewModel.scanImageQr(entry)
                                    },
                                )
                            ContentType.PDF ->
                                PdfViewer(
                                    filePath = entry.filePath,
                                    context = context,
                                    onOcrPage = { page ->
                                        viewModel.ocrCurrentPdfPage(entry, page)
                                    },
                                    onSavePageAsPdf = { page ->
                                        viewModel.extractPdfPage(entry, page)
                                    },
                                    onScanQr = { page ->
                                        viewModel.scanPdfPageQrToEditor(entry, page)
                                    },
                                    onExtractImages = { page ->
                                        viewModel.extractPdfPageImages(entry, page)
                                    },
                                )
                            ContentType.FILE ->
                                FileViewer(
                                    filePath = entry.filePath,
                                    onUnsupported = {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.file_cannot_open),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        onNavigateBack()
                                    },
                                )
                            ContentType.LOCATION -> {
                                Text(
                                    text = entry.previewText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorToolButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    IconButton(onClick = {
        ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
        onClick()
    }, enabled = enabled) {
        Icon(icon, contentDescription)
    }
}

@Composable
private fun MdToolButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    val context = LocalContext.current
    IconButton(
        onClick = {
            ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
            onClick()
        },
        modifier =
            Modifier.clearAndSetSemantics {
                contentDescription = context.getString(R.string.md_title).withoutSoftHyphens()
            },
        enabled = enabled,
    ) {
        ButtonLabel(
            text = context.getString(R.string.md_mark),
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Monospace,
                ),
            color =
                if (enabled) {
                    LocalContentColor.current
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.38f,
                    )
                },
            maxLines = 1,
        )
    }
}

@Composable
private fun MarkdownInsertDialog(
    onDismiss: () -> Unit,
    onPick: (String, String, Boolean) -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitleLabel(context.getString(R.string.md_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                MarkdownOption(
                    label = context.getString(R.string.md_bold) + "  ** **",
                    onClick = { onPick("**", "**", false) },
                )
                MarkdownOption(
                    label = context.getString(R.string.md_italic) + "  * *",
                    onClick = { onPick("*", "*", false) },
                )
                MarkdownOption(
                    label = context.getString(R.string.md_strike) + "  ~~ ~~",
                    onClick = { onPick("~~", "~~", false) },
                )
                MarkdownOption(
                    label = context.getString(R.string.md_h1) + "  # ",
                    onClick = { onPick("# ", "", true) },
                )
                MarkdownOption(
                    label = context.getString(R.string.md_h2) + "  ## ",
                    onClick = { onPick("## ", "", true) },
                )
                MarkdownOption(
                    label = context.getString(R.string.md_h3) + "  ### ",
                    onClick = { onPick("### ", "", true) },
                )
                MarkdownOption(
                    label = context.getString(R.string.md_h4) + "  #### ",
                    onClick = { onPick("#### ", "", true) },
                )
                MarkdownOption(
                    label = context.getString(R.string.md_list) + "  - ",
                    onClick = { onPick("- ", "", true) },
                )
                MarkdownOption(
                    label = context.getString(R.string.md_line_break) + "  ↵",
                    onClick = { onPick("  \n", "", false) },
                )
                MarkdownOption(
                    label = context.getString(R.string.md_code) + "  ` `",
                    onClick = { onPick("`", "`", false) },
                )
                MarkdownOption(
                    label = context.getString(R.string.md_code_block) + "  ``` ```",
                    onClick = { onPick("```\n", "\n```", false) },
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                ButtonLabel(context.getString(R.string.cancel))
            }
        },
    )
}

@Composable
private fun MarkdownOption(
    label: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ButtonLabel(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ImageViewer(
    filePath: String?,
    onScanQr: () -> Unit = {},
) {
    if (filePath == null) {
        MissingFilePane()
        return
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    Box(Modifier.fillMaxSize().clipToBounds()) {
        AsyncImage(
            model = File(filePath),
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    },
            contentScale = ContentScale.Fit,
        )
        IconButton(
            onClick = onScanQr,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
        ) {
            Icon(
                Icons.Filled.QrCodeScanner,
                contentDescription = LocalContext.current.getString(R.string.scan_content).withoutSoftHyphens(),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun PdfViewer(
    filePath: String?,
    context: android.content.Context,
    onOcrPage: ((Int) -> Unit)? = null,
    onSavePageAsPdf: ((Int) -> Unit)? = null,
    onScanQr: ((Int) -> Unit)? = null,
    onExtractImages: ((Int) -> Unit)? = null,
) {
    if (filePath == null) {
        MissingFilePane()
        return
    }
    val displayed = rememberDisplayedBitmap()
    val pageGate = remember { AtomicInteger(0) }
    var pageIndex by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var countLoaded by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(filePath) {
        pageCount = withContext(Dispatchers.IO) { pdfPageCount(filePath) }
        countLoaded = true
    }
    LaunchedEffect(filePath, pageIndex) {
        val loaded =
            produceLatest(
                pageGate,
                produce = { withContext(Dispatchers.IO) { renderPdfPage(filePath, 2200, pageIndex) } },
                discard = { bitmap -> bitmap?.recycle() },
            ) ?: return@LaunchedEffect
        displayed.update(loaded.value)
    }

    if (!countLoaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (pageCount <= 0) {
        MissingFilePane(messageRes = R.string.file_cannot_open)
        return
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            displayed.bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = context.getString(R.string.pdf).withoutSoftHyphens(),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY,
                            )
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 8f)
                                    if (scale > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            },
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High,
                )
            }
            val actions =
                buildList {
                    if (onSavePageAsPdf != null) {
                        add(
                            PdfBarActionSpec(
                                icon = Icons.Filled.PictureAsPdf,
                                label = context.getString(R.string.save_page_pdf),
                                onClick = { onSavePageAsPdf(pageIndex) },
                            ),
                        )
                    }
                    if (onOcrPage != null) {
                        add(
                            PdfBarActionSpec(
                                icon = Icons.Filled.TextFields,
                                label = context.getString(R.string.ocr_page),
                                onClick = { onOcrPage(pageIndex) },
                            ),
                        )
                    }
                }
            val menu =
                buildList {
                    if (onExtractImages != null) {
                        add(
                            PdfBarActionSpec(
                                icon = Icons.Filled.Photo,
                                label = context.getString(R.string.pdf_to_image),
                                onClick = { onExtractImages(pageIndex) },
                            ),
                        )
                    }
                    if (onScanQr != null) {
                        add(
                            PdfBarActionSpec(
                                icon = Icons.Filled.QrCodeScanner,
                                label = context.getString(R.string.scan_content),
                                onClick = { onScanQr(pageIndex) },
                            ),
                        )
                    }
                }
            PdfViewerActionBar(
                pageIndex = pageIndex,
                pageCount = pageCount,
                onPrev = {
                    if (pageIndex > 0) {
                        pageIndex--
                        offsetX = 0f
                        offsetY = 0f
                        scale = 1f
                    }
                },
                onNext = {
                    if (pageIndex < pageCount - 1) {
                        pageIndex++
                        offsetX = 0f
                        offsetY = 0f
                        scale = 1f
                    }
                },
                onGoToPage = { target ->
                    if (target != pageIndex) {
                        pageIndex = target
                        offsetX = 0f
                        offsetY = 0f
                        scale = 1f
                    }
                },
                actions = actions,
                menu = menu,
                moreLabel = context.getString(R.string.more_options),
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                        .fillMaxWidth(),
            )
        }
    }
}

private fun pdfFileTitle(entry: ClipboardEntry): String {
    val path = entry.filePath
    if (!path.isNullOrBlank()) {
        val name = File(path).name
        if (name.isNotBlank()) return name
    }
    return entry.previewText
}

@Composable
private fun FileViewer(
    filePath: String?,
    onUnsupported: () -> Unit,
) {
    var textContent by remember(filePath) { mutableStateOf<String?>(null) }
    LaunchedEffect(filePath) {
        if (filePath == null) {
            onUnsupported()
            return@LaunchedEffect
        }
        val text = withContext(Dispatchers.IO) { readFileText(filePath) }
        if (text == null) {
            onUnsupported()
        } else {
            textContent = text
        }
    }
    val text = textContent
    if (text != null) {
        Text(
            text = text,
            fontSize = 14.sp,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        )
    }
}

@Composable
private fun MissingFilePane(messageRes: Int = R.string.clip_not_found) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = context.getString(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun readFileText(filePath: String): String? {
    return when (filePath.substringAfterLast('.', "").lowercase()) {
        "docx" -> extractDocxText(filePath)
        "xlsx" -> extractXlsxText(filePath)
        "rtf" -> extractRtfText(filePath) ?: readTextFileSafely(filePath)
        else -> readTextFileSafely(filePath)
    }
}

private const val MAX_TEXT_VIEW_BYTES = 8L * 1024L * 1024L

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QrResultDialog(
    viewModel: ClipboardViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scanning by viewModel.qrScanning.collectAsState()
    val results by viewModel.qrResults.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitleLabel(context.getString(R.string.scan_content)) },
        text = {
            when {
                scanning -> {
                    Text(
                        text = context.getString(R.string.analyzing),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                results.isNullOrEmpty() -> {
                    Text(
                        text = context.getString(R.string.no_qr_found),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> {
                    Column {
                        results.orEmpty().forEach { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                            FlowRow {
                                TextButton(onClick = { viewModel.copyText(message) }) {
                                    ButtonLabel(context.getString(R.string.copy))
                                }
                                TextButton(onClick = { viewModel.saveQrAsClip(message) }) {
                                    ButtonLabel(context.getString(R.string.save_to_clip))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                ButtonLabel(context.getString(R.string.cancel))
            }
        },
    )
}

@Composable
private fun TextPreviewPane(
    pages: List<Bitmap>?,
    loading: Boolean,
    onPageTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Box(modifier.fillMaxSize()) {
        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            pages.isNullOrEmpty() -> {
                Text(
                    text = context.getString(R.string.no_content_preview),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            else -> {
                val pagerState = rememberPagerState { pages.size }
                val pageScales = remember { androidx.compose.runtime.mutableStateMapOf<Int, Float>() }
                val currentScale = pageScales[pagerState.currentPage] ?: 1f
                Box(Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = currentScale <= 1.01f,
                        contentPadding = PaddingValues(horizontal = 0.dp),
                        pageSpacing = 4.dp,
                    ) { page ->
                        val bmp = pages[page]
                        PreviewPageItem(
                            bitmap = bmp,
                            pageIndex = page,
                            onTap = { onPageTap(page) },
                            onScaleChanged = { pageScales[page] = it },
                        )
                    }
                    if (pages.size > 1) {
                        val scope = androidx.compose.runtime.rememberCoroutineScope()
                        PageSelector(
                            pageIndex = pagerState.currentPage,
                            pageCount = pages.size,
                            onPrev = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            onNext = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            onGoToPage = { target ->
                                scope.launch {
                                    pagerState.animateScrollToPage(target)
                                }
                            },
                            modifier =
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(bottom = 8.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        RoundedCornerShape(8.dp),
                                    )
                                    .padding(horizontal = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewPageItem(
    bitmap: Bitmap,
    pageIndex: Int,
    onTap: () -> Unit,
    onScaleChanged: (Float) -> Unit = {},
) {
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offsetX by remember(bitmap) { mutableFloatStateOf(0f) }
    var offsetY by remember(bitmap) { mutableFloatStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(scale) { onScaleChanged(scale) }
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier =
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(bitmap) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.count { it.pressed }
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            if (pressed >= 2) {
                                val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                if (newScale != scale) {
                                    scale = newScale
                                }
                                if (scale > 1f) {
                                    offsetX += panChange.x
                                    offsetY += panChange.y
                                }
                                event.changes.forEach { it.consume() }
                            } else if (pressed == 1 && scale > 1f) {
                                offsetX += panChange.x
                                offsetY += panChange.y
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                        if (scale <= 1.01f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                },
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription =
                LocalContext.current.getString(
                    R.string.page_label,
                    pageIndex + 1,
                ).withoutSoftHyphens(),
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                    )
                    .clickable { onTap() },
            contentScale = ContentScale.Fit,
            alignment = Alignment.TopCenter,
        )
    }
}

@Composable
private fun ZoomPreviewDialog(
    bitmap: Bitmap,
    pageNumber: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .background(Color.Black.copy(alpha = 0.9f)),
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = context.getString(R.string.page_label, pageNumber).withoutSoftHyphens(),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY,
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 6f)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                contentScale = ContentScale.Fit,
            )
            IconButton(
                onClick = onDismiss,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = context.getString(R.string.cancel).withoutSoftHyphens(),
                    tint = Color.White,
                )
            }
            Text(
                text = context.getString(R.string.page_label, pageNumber),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
            )
        }
    }
}
