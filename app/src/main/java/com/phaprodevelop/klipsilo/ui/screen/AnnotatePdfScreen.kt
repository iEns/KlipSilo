package com.phaprodevelop.klipsilo.ui.screen

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.MarkupHistory
import com.phaprodevelop.klipsilo.data.TextCommitDecision
import com.phaprodevelop.klipsilo.data.markupTextSizeStep
import com.phaprodevelop.klipsilo.data.markupTextSizeStepCount
import com.phaprodevelop.klipsilo.data.markupTextSizeStepIndex
import com.phaprodevelop.klipsilo.data.markupWidthStep
import com.phaprodevelop.klipsilo.data.markupWidthStepCount
import com.phaprodevelop.klipsilo.data.markupWidthStepIndex
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.nearestMarkupTextSize
import com.phaprodevelop.klipsilo.data.nearestMarkupWidth
import com.phaprodevelop.klipsilo.data.repository.PdfAnnotation
import com.phaprodevelop.klipsilo.data.repository.PdfStroke
import com.phaprodevelop.klipsilo.data.resolveTextCommit
import com.phaprodevelop.klipsilo.data.settings.AnnotateSettings
import com.phaprodevelop.klipsilo.data.settings.AnnotateSettingsStore
import com.phaprodevelop.klipsilo.ui.components.BitmapUseCounter
import com.phaprodevelop.klipsilo.ui.components.ButtonLabel
import com.phaprodevelop.klipsilo.ui.components.NAV_LABEL_FONT_FLOOR_FRACTION
import com.phaprodevelop.klipsilo.ui.components.PageSelector
import com.phaprodevelop.klipsilo.ui.components.TopBarFitTitle
import com.phaprodevelop.klipsilo.ui.components.TopBarSaveAction
import com.phaprodevelop.klipsilo.ui.components.pdfPageCount
import com.phaprodevelop.klipsilo.ui.components.pdfPageHeightPt
import com.phaprodevelop.klipsilo.ui.components.produceLatest
import com.phaprodevelop.klipsilo.ui.components.rememberCommonRowLabelSize
import com.phaprodevelop.klipsilo.ui.components.rememberDisplayedBitmap
import com.phaprodevelop.klipsilo.ui.components.renderPdfPage
import com.phaprodevelop.klipsilo.ui.components.scaledLabelStyle
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

private data class MarkupSnapshot(
    val annotations: Map<Int, List<PdfAnnotation>>,
    val strokes: Map<Int, List<PdfStroke>>,
)

private data class DraftAnnotation(
    val text: String,
    val sizeFraction: Float,
    val xFraction: Float,
    val yFraction: Float,
    val editingIndex: Int?,
    val color: Int = android.graphics.Color.BLACK,
    val zOrder: Long = 0L,
)

private enum class MarkupTool {
    SELECT,
    TEXT,
    LINE,
    DRAW,
    HIGHLIGHT,
}

private sealed class DrawItem(val z: Long) {
    class Text(val index: Int, val ann: PdfAnnotation, z: Long) : DrawItem(z)

    class Stroke(val index: Int, val stroke: PdfStroke, z: Long) : DrawItem(z)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnnotatePdfScreen(
    entryId: Long,
    viewModel: ClipboardViewModel,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val clips by viewModel.clips.collectAsState()
    var loadedEntry by remember { mutableStateOf<ClipboardEntry?>(null) }
    var lookupDone by remember { mutableStateOf(false) }
    LaunchedEffect(entryId) {
        loadedEntry = viewModel.getClip(entryId)
        lookupDone = true
    }
    val entry = loadedEntry ?: clips.find { it.id == entryId }
    val navigateBack by viewModel.navigateBack.collectAsState()
    LaunchedEffect(navigateBack) {
        if (navigateBack) {
            viewModel.clearNavigateBack()
            onNavigateBack()
        }
    }
    if (!lookupDone && entry == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
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

    var pageIndex by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var baseBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var tool by remember { mutableStateOf(MarkupTool.SELECT) }
    var draft by remember { mutableStateOf<DraftAnnotation?>(null) }
    var draftEpoch by remember { mutableIntStateOf(0) }
    // TextFieldValue for the draft so the caret can be placed where the user
    // tapped when editing starts (#55). The field's layout and size are kept
    // to map taps near the active box and grow its edit-mode touch area.
    var draftField by remember { mutableStateOf(TextFieldValue("")) }
    var draftFieldSize by remember { mutableStateOf(IntSize.Zero) }
    var draftFieldLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val annotationMode = tool == MarkupTool.TEXT
    val drawMode = tool == MarkupTool.LINE || tool == MarkupTool.DRAW || tool == MarkupTool.HIGHLIGHT
    val straightMode = tool == MarkupTool.LINE
    val focusRequester = remember { FocusRequester() }
    val strokes = remember { mutableStateMapOf<Int, List<PdfStroke>>() }
    var draftStroke by remember { mutableStateOf<PdfStroke?>(null) }
    val savedAnnotate = remember { AnnotateSettingsStore.load(context) }
    var strokeWidth by remember {
        mutableFloatStateOf(nearestMarkupWidth(savedAnnotate.strokeWidthFraction))
    }
    var strokeColor by remember { mutableIntStateOf(savedAnnotate.strokeColor) }
    var textSizeFraction by remember {
        mutableFloatStateOf(nearestMarkupTextSize(savedAnnotate.textSizeFraction))
    }
    var textColor by remember { mutableIntStateOf(savedAnnotate.textColor) }
    var zSeq by remember { mutableStateOf(0L) }

    fun nextZ(): Long {
        zSeq += 1
        return zSeq
    }

    fun persistAnnotateSettings() {
        AnnotateSettingsStore.save(
            context,
            AnnotateSettings(
                textSizeFraction = textSizeFraction,
                textColor = textColor,
                strokeWidthFraction = strokeWidth,
                strokeColor = strokeColor,
            ),
        )
    }
    var movingIndex by remember { mutableIntStateOf(-1) }
    var movingDraft by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var selectedStroke by remember { mutableIntStateOf(-1) }
    var pageSize by remember { mutableStateOf(IntSize.Zero) }
    var showExitWarning by remember { mutableStateOf(false) }
    val annotations = remember { mutableStateMapOf<Int, List<PdfAnnotation>>() }
    var pageHeightPt by remember { mutableFloatStateOf(0f) }
    var savedAnnotations by remember { mutableStateOf(emptyList<PdfAnnotation>()) }
    var savedStrokes by remember { mutableStateOf(emptyList<PdfStroke>()) }

    // The commit decision for a draft text box (#50, #55): a new box left
    // empty is dropped, an edit cleared to empty text deletes the item, an
    // unchanged edit keeps it, anything else is added/replaced.
    fun draftAnnotation(d: DraftAnnotation): PdfAnnotation =
        PdfAnnotation(
            pageIndex = pageIndex,
            xFraction = d.xFraction,
            yFraction = d.yFraction,
            text = d.text,
            sizeFraction = d.sizeFraction,
            color = d.color,
            zOrder = d.zOrder,
        )

    fun draftCommitDecision(d: DraftAnnotation): TextCommitDecision =
        resolveTextCommit(
            d.editingIndex?.let { annotations[pageIndex].orEmpty().getOrNull(it) },
            draftAnnotation(d),
        )

    // A dropped new box (#50) and an unchanged edit record nothing, so they
    // do not count as a change. Deleting (empty edit) and adding/replacing do.
    val draftDirty =
        draft?.let { d ->
            when (draftCommitDecision(d)) {
                TextCommitDecision.Keep, TextCommitDecision.Drop -> false
                else -> true
            }
        } == true
    val dirty =
        annotations.values.flatten() != savedAnnotations ||
            strokes.values.flatten() != savedStrokes ||
            draftDirty ||
            draftStroke != null
    val markupHistory = remember { MarkupHistory<MarkupSnapshot>() }
    var historyTick by remember { mutableIntStateOf(0) }

    fun markupSnapshot(): MarkupSnapshot =
        MarkupSnapshot(
            annotations = annotations.mapValues { it.value.toList() },
            strokes = strokes.mapValues { it.value.toList() },
        )

    fun applyMarkupSnapshot(snapshot: MarkupSnapshot) {
        annotations.clear()
        snapshot.annotations.forEach { (page, items) -> annotations[page] = items }
        strokes.clear()
        snapshot.strokes.forEach { (page, items) -> strokes[page] = items }
    }

    fun recordMarkup() {
        markupHistory.record(markupSnapshot())
        historyTick++
    }

    fun snapshotWithDraftStroke(stroke: PdfStroke): MarkupSnapshot {
        val base = markupSnapshot()
        val pageStrokes = base.strokes[stroke.pageIndex].orEmpty() + stroke
        return base.copy(strokes = base.strokes + (stroke.pageIndex to pageStrokes))
    }

    fun undoMarkup() {
        val pendingStroke = draftStroke
        if (pendingStroke != null && pendingStroke.points.size >= 2) {
            val restored =
                markupHistory.undoOne(
                    committed = markupSnapshot(),
                    pendingDocument = snapshotWithDraftStroke(pendingStroke),
                ) ?: return
            applyMarkupSnapshot(restored)
            draftStroke = null
            selectedIndex = -1
            selectedStroke = -1
            historyTick++
            return
        }
        if (pendingStroke != null || draft != null) {
            draftStroke = null
            draft = null
            selectedIndex = -1
            selectedStroke = -1
            historyTick++
            return
        }
        val restored = markupHistory.undo(markupSnapshot()) ?: return
        applyMarkupSnapshot(restored)
        draft = null
        draftStroke = null
        selectedIndex = -1
        selectedStroke = -1
        historyTick++
    }

    fun redoMarkup() {
        val restored = markupHistory.redo(markupSnapshot()) ?: return
        applyMarkupSnapshot(restored)
        draft = null
        draftStroke = null
        selectedIndex = -1
        selectedStroke = -1
        historyTick++
    }

    fun beginDraft(
        next: DraftAnnotation,
        caret: Int = next.text.length,
    ) {
        draft = next
        // Caret where the user tapped (#55), default end of text.
        draftField = TextFieldValue(next.text, TextRange(caret.coerceIn(0, next.text.length)))
        draftFieldLayout = null
        draftFieldSize = IntSize.Zero
        draftEpoch += 1
    }

    fun replaceSelectedText(transform: (PdfAnnotation) -> PdfAnnotation) {
        val index = selectedIndex
        val list = annotations[pageIndex].orEmpty().toMutableList()
        if (index !in list.indices) return
        recordMarkup()
        list[index] = transform(list[index])
        annotations[pageIndex] = list
    }

    fun replaceSelectedStroke(transform: (PdfStroke) -> PdfStroke) {
        val index = selectedStroke
        val list = strokes[pageIndex].orEmpty().toMutableList()
        if (index !in list.indices) return
        recordMarkup()
        list[index] = transform(list[index])
        strokes[pageIndex] = list
    }

    fun deleteSelection() {
        val editing = draft?.editingIndex
        if (editing != null) {
            recordMarkup()
            val list = annotations[pageIndex].orEmpty().toMutableList()
            if (editing < list.size) {
                list.removeAt(editing)
                annotations[pageIndex] = list
            }
            draft = null
            selectedIndex = -1
            return
        }
        if (selectedIndex >= 0) {
            recordMarkup()
            val list = annotations[pageIndex].orEmpty().toMutableList()
            if (selectedIndex in list.indices) {
                list.removeAt(selectedIndex)
                annotations[pageIndex] = list
            }
            selectedIndex = -1
            return
        }
        if (selectedStroke >= 0) {
            recordMarkup()
            val list = strokes[pageIndex].orEmpty().toMutableList()
            if (selectedStroke in list.indices) {
                list.removeAt(selectedStroke)
                strokes[pageIndex] = list
            }
            selectedStroke = -1
        }
    }

    fun applyTextSize(size: Float) {
        textSizeFraction = size
        persistAnnotateSettings()
        val current = draft
        if (current != null) {
            draft = current.copy(sizeFraction = size)
        } else if (selectedIndex >= 0) {
            replaceSelectedText { it.copy(sizeFraction = size) }
        }
    }

    fun applyTextColor(color: Int) {
        textColor = color
        persistAnnotateSettings()
        val current = draft
        if (current != null) {
            draft = current.copy(color = color)
        } else if (selectedIndex >= 0) {
            replaceSelectedText { it.copy(color = color) }
        }
    }

    fun applyStrokeWidth(width: Float) {
        strokeWidth = width
        persistAnnotateSettings()
        val current = draftStroke
        if (current != null) {
            draftStroke = current.copy(widthFraction = width)
        } else if (selectedStroke >= 0) {
            replaceSelectedStroke { it.copy(widthFraction = width) }
        }
    }

    fun applyStrokeColor(color: Int) {
        strokeColor = color
        persistAnnotateSettings()
        val current = draftStroke
        if (current != null) {
            draftStroke = current.copy(color = color)
        } else if (selectedStroke >= 0) {
            replaceSelectedStroke { it.copy(color = color) }
        }
    }

    fun commitDrafts() {
        val d = draft
        val pendingStroke = draftStroke
        val decision = d?.let { draftCommitDecision(it) }
        // Add/Replace/Delete each change the markup and are one undoable
        // step; a dropped empty new box (#50) and an unchanged edit record
        // nothing.
        if (
            decision is TextCommitDecision.Add ||
            decision is TextCommitDecision.Replace ||
            decision is TextCommitDecision.Delete ||
            (pendingStroke != null && pendingStroke.points.size >= 2)
        ) {
            recordMarkup()
        }
        if (d != null) {
            when (decision) {
                is TextCommitDecision.Add -> {
                    annotations[pageIndex] =
                        annotations[pageIndex].orEmpty() + draftAnnotation(d).copy(text = decision.text)
                }
                is TextCommitDecision.Replace -> {
                    val list = annotations[pageIndex].orEmpty().toMutableList()
                    val index = d.editingIndex
                    if (index != null && index < list.size) {
                        list[index] = draftAnnotation(d).copy(text = decision.text)
                    } else {
                        list.add(draftAnnotation(d).copy(text = decision.text))
                    }
                    annotations[pageIndex] = list
                }
                is TextCommitDecision.Delete -> {
                    val index = d.editingIndex
                    val list = annotations[pageIndex].orEmpty().toMutableList()
                    if (index != null && index < list.size) {
                        list.removeAt(index)
                        annotations[pageIndex] = list
                    }
                }
                else -> {}
            }
        }
        val s = draftStroke
        if (s != null && s.points.size >= 2) {
            val list = strokes[pageIndex].orEmpty().toMutableList()
            list.add(s)
            strokes[pageIndex] = list
        }
        draft = null
        draftStroke = null
    }

    BackHandler(enabled = dirty) {
        showExitWarning = true
    }

    val filePath = entry.filePath
    val pageGate = remember { AtomicInteger(0) }
    val drawGate = remember { AtomicInteger(0) }
    val baseUse = remember { BitmapUseCounter() }
    val displayed = rememberDisplayedBitmap()
    DisposableEffect(baseUse) {
        onDispose {
            baseUse.recycleIfUnused(baseBitmap)
        }
    }
    LaunchedEffect(filePath) {
        if (filePath != null) {
            pageCount = withContext(Dispatchers.IO) { pdfPageCount(filePath) }
        }
    }
    LaunchedEffect(filePath, pageIndex) {
        if (filePath != null) {
            val loaded =
                produceLatest(
                    pageGate,
                    produce = {
                        withContext(Dispatchers.IO) {
                            pdfPageHeightPt(filePath, pageIndex) to renderPdfPage(filePath, 1600, pageIndex)
                        }
                    },
                    discard = { (_, bitmap) -> bitmap?.recycle() },
                ) ?: return@LaunchedEffect
            val previous = baseBitmap
            pageHeightPt = loaded.value.first
            baseBitmap = loaded.value.second
            baseUse.recycleIfUnused(previous)
            draft = null
            draftStroke = null
            movingIndex = -1
            movingDraft = false
            selectedIndex = -1
            selectedStroke = -1
        }
    }

    val currentAnnotations = annotations[pageIndex].orEmpty()
    val currentStrokes = strokes[pageIndex].orEmpty()
    val draftText = draft?.text.orEmpty()
    val draftSize = draft?.sizeFraction ?: 0f
    val draftX = draft?.xFraction ?: 0f
    val draftY = draft?.yFraction ?: 0f
    val draftColor = draft?.color ?: android.graphics.Color.BLACK
    val draftZ = draft?.zOrder ?: 0L
    val draftEditingIndex = draft?.editingIndex
    LaunchedEffect(
        baseBitmap,
        currentAnnotations,
        currentStrokes,
        draftText,
        draftSize,
        draftX,
        draftY,
        draftColor,
        draftEditingIndex,
        draftStroke,
        pageIndex,
        movingIndex,
        movingDraft,
        selectedIndex,
        selectedStroke,
    ) {
        val base = baseBitmap ?: return@LaunchedEffect
        baseUse.acquire(base)
        try {
            val shown = currentAnnotations.toMutableList()
            if (draftEditingIndex != null && draftEditingIndex < shown.size) {
                shown.removeAt(draftEditingIndex)
            }
            if (draftText.isNotBlank()) {
                shown.add(PdfAnnotation(pageIndex, draftX, draftY, draftText, draftSize, draftColor, draftZ))
            }
            val highlight =
                when {
                    movingIndex >= 0 -> movingIndex
                    movingDraft && draftText.isNotBlank() -> shown.lastIndex
                    draft != null && draftText.isNotBlank() -> shown.lastIndex
                    selectedIndex >= 0 -> selectedIndex
                    else -> null
                }
            val shownStrokes = currentStrokes.toMutableList()
            if (draftStroke != null) {
                shownStrokes.add(draftStroke!!)
            }
            val drawn =
                produceLatest(
                    drawGate,
                    produce = {
                        withContext(Dispatchers.IO) {
                            drawAnnotations(base, shown, shownStrokes, highlight, selectedStroke)
                        }
                    },
                    discard = { bitmap ->
                        if (bitmap !== base) bitmap.recycle()
                    },
                ) ?: return@LaunchedEffect
            displayed.update(drawn.value)
        } finally {
            baseUse.release(base, stillNeeded = base === baseBitmap)
        }
    }

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val keyboardDraftY = draft?.yFraction
    LaunchedEffect(draftEpoch, imeBottom, pageSize, baseBitmap) {
        val yFraction = keyboardDraftY ?: return@LaunchedEffect
        val bmp = baseBitmap ?: return@LaunchedEffect
        if (imeBottom <= 0 || pageSize.height == 0) return@LaunchedEffect
        val rect = fittedRect(pageSize, bmp.width, bmp.height)
        val yOnPage = rect.top + yFraction * rect.height
        val reserve = with(density) { 168.dp.toPx() }
        val visibleBottom = pageSize.height - imeBottom - reserve
        if (yOnPage + offsetY > visibleBottom) {
            offsetY = visibleBottom - yOnPage
        }
    }
    LaunchedEffect(draftEpoch) {
        if (draft != null) {
            kotlinx.coroutines.delay(50)
            try {
                focusRequester.requestFocus()
            } catch (_: IllegalStateException) {
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    TopBarFitTitle(text = context.getString(R.string.annotate_title))
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (dirty) {
                                showExitWarning = true
                            } else {
                                onNavigateBack()
                            }
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = context.getString(R.string.back),
                        )
                    }
                },
                actions = {
                    TopBarSaveAction(
                        label = context.getString(R.string.save),
                        title = context.getString(R.string.annotate_title),
                        enabled = entry.filePath != null,
                        onClick = {
                            commitDrafts()
                            val all = annotations.values.flatten()
                            val allStrokes = strokes.values.flatten()
                            viewModel.saveAnnotatedPdf(entry, all, allStrokes)
                        },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            MarkupHistoryRow(
                canUndo = historyTick >= 0 && (markupHistory.canUndo || draftStroke != null || draftDirty),
                canRedo = historyTick >= 0 && markupHistory.canRedo,
                canDelete = selectedIndex >= 0 || selectedStroke >= 0 || draft?.editingIndex != null,
                pageIndex = pageIndex,
                pageCount = pageCount,
                onUndo = { undoMarkup() },
                onRedo = { redoMarkup() },
                onDelete = { deleteSelection() },
                onPrev = {
                    if (pageIndex > 0) {
                        commitDrafts()
                        pageIndex--
                        offsetX = 0f
                        offsetY = 0f
                        scale = 1f
                    }
                },
                onNext = {
                    if (pageIndex < pageCount - 1) {
                        commitDrafts()
                        pageIndex++
                        offsetX = 0f
                        offsetY = 0f
                        scale = 1f
                    }
                },
                onGoToPage = { target ->
                    if (target != pageIndex) {
                        commitDrafts()
                        pageIndex = target
                        offsetX = 0f
                        offsetY = 0f
                        scale = 1f
                    }
                },
            )
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                        .onSizeChanged { pageSize = it },
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .pointerInput(tool, selectedIndex, baseBitmap, pageIndex) {
                                val touchSlop = viewConfiguration.touchSlop
                                if (annotationMode || tool == MarkupTool.SELECT) {
                                    var lastTapUpTime = 0L
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val downPos = down.position
                                        var targetIndex = -1
                                        var targetIsDraft = false
                                        var targetStroke = -1
                                        var fx = 0.5f
                                        var fy = 0.5f
                                        val bmp = baseBitmap
                                        if (bmp != null) {
                                            val mapped =
                                                mapTapToFraction(
                                                    downPos,
                                                    size,
                                                    bmp.width,
                                                    bmp.height,
                                                    scale,
                                                    offsetX,
                                                    offsetY,
                                                )
                                            fx = mapped.first
                                            fy = mapped.second
                                            val idx =
                                                annotations[pageIndex].orEmpty().indexOfFirst {
                                                    annotationHitTest(
                                                        fx,
                                                        fy,
                                                        it.xFraction,
                                                        it.yFraction,
                                                        it.text,
                                                        it.sizeFraction,
                                                        bmp.width,
                                                        bmp.height,
                                                    )
                                                }
                                            if (idx >= 0) {
                                                targetIndex = idx
                                                targetIsDraft = false
                                                selectedStroke = -1
                                            } else if (
                                                draft != null &&
                                                annotationHitTest(
                                                    fx,
                                                    fy,
                                                    draft!!.xFraction,
                                                    draft!!.yFraction,
                                                    draft!!.text,
                                                    draft!!.sizeFraction,
                                                    bmp.width,
                                                    bmp.height,
                                                )
                                            ) {
                                                targetIndex = -1
                                                targetIsDraft = true
                                            } else {
                                                targetStroke =
                                                    strokeHitIndex(strokes[pageIndex].orEmpty(), fx, fy)
                                            }
                                        }
                                        val targetOnText = targetIndex >= 0 || targetIsDraft

                                        // While a box is being edited (#55), taps in
                                        // its enlarged touch area belong to the text
                                        // field: they move the caret (or are handled
                                        // by the field itself) and must neither commit
                                        // the edit nor start a new box.
                                        val activeDraft = draft
                                        val fieldRect =
                                            activeDraft?.let { d ->
                                                bmp?.let {
                                                    editFieldScreenRect(
                                                        d,
                                                        size,
                                                        it.width,
                                                        it.height,
                                                        scale,
                                                        offsetX,
                                                        offsetY,
                                                        draftFieldSize.width.toFloat(),
                                                        draftFieldSize.height.toFloat(),
                                                    )
                                                }
                                            }
                                        val tapInEditField = fieldRect?.contains(downPos) == true
                                        val tapInEditArea =
                                            fieldRect
                                                ?.let {
                                                    expandedEditTapBounds(
                                                        it,
                                                        padPx = 28.dp.toPx(),
                                                        trailingPadPx = 40.dp.toPx(),
                                                        rtl = false,
                                                    )
                                                }?.contains(downPos) == true

                                        var moved = false
                                        var pinching = false
                                        var lastDist = 0f
                                        var lastCentroid = Offset.Zero
                                        var lastPos = downPos

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val pressed = event.changes.filter { it.pressed }
                                            if (pressed.isEmpty()) {
                                                if (!moved) {
                                                    val now = SystemClock.uptimeMillis()
                                                    val isDoubleTap =
                                                        lastTapUpTime > 0 && now - lastTapUpTime < 300L
                                                    if (tapInEditArea) {
                                                        // The active box owns this tap
                                                        // (#55). Inside the field itself
                                                        // the text field handles the tap
                                                        // (caret move, long-press and
                                                        // double-tap selection); in the
                                                        // surrounding padding place the
                                                        // caret at the nearest offset.
                                                        // Never commit or start a new box.
                                                        val layout = draftFieldLayout
                                                        if (!tapInEditField && layout != null) {
                                                            val d = activeDraft
                                                            val pageBmp = bmp
                                                            if (d != null && pageBmp != null) {
                                                                val fitted =
                                                                    fittedRect(size, pageBmp.width, pageBmp.height)
                                                                val layer =
                                                                    screenToPage(
                                                                        downPos,
                                                                        size,
                                                                        scale,
                                                                        offsetX,
                                                                        offsetY,
                                                                    )
                                                                val local =
                                                                    Offset(
                                                                        layer.x -
                                                                            (fitted.left + d.xFraction * fitted.width),
                                                                        layer.y -
                                                                            (fitted.top + d.yFraction * fitted.height),
                                                                    )
                                                                val offset =
                                                                    layout
                                                                        .getOffsetForPosition(local)
                                                                        .coerceIn(0, draftField.text.length)
                                                                draftField =
                                                                    draftField.copy(selection = TextRange(offset))
                                                            }
                                                        }
                                                        lastTapUpTime = 0L
                                                    } else if (
                                                        (isDoubleTap || selectedIndex == targetIndex) &&
                                                        targetOnText &&
                                                        !targetIsDraft &&
                                                        tool == MarkupTool.TEXT
                                                    ) {
                                                        val list = annotations[pageIndex].orEmpty()
                                                        if (targetIndex in list.indices) {
                                                            val a = list[targetIndex]
                                                            // Caret where the user tapped (#55),
                                                            // falling back to end of text.
                                                            val caret =
                                                                bmp?.let {
                                                                    tappedCaretOffset(fx, fy, a, it.width, it.height)
                                                                } ?: a.text.length
                                                            selectedIndex = -1
                                                            selectedStroke = -1
                                                            beginDraft(
                                                                DraftAnnotation(
                                                                    a.text,
                                                                    a.sizeFraction,
                                                                    a.xFraction,
                                                                    a.yFraction,
                                                                    targetIndex,
                                                                    a.color,
                                                                    nextZ(),
                                                                ),
                                                                caret.coerceIn(0, a.text.length),
                                                            )
                                                        }
                                                        lastTapUpTime = 0L
                                                    } else if (targetOnText && !targetIsDraft) {
                                                        if (draft != null) commitDrafts()
                                                        selectedIndex = targetIndex
                                                        selectedStroke = -1
                                                        lastTapUpTime = now
                                                    } else if (targetStroke >= 0 && tool == MarkupTool.SELECT) {
                                                        if (draft != null) commitDrafts()
                                                        selectedStroke = targetStroke
                                                        selectedIndex = -1
                                                        lastTapUpTime = 0L
                                                    } else if (tool == MarkupTool.TEXT && !targetOnText) {
                                                        if (draft != null) commitDrafts()
                                                        selectedIndex = -1
                                                        selectedStroke = -1
                                                        beginDraft(
                                                            DraftAnnotation(
                                                                "",
                                                                nearestMarkupTextSize(textSizeFraction),
                                                                fx,
                                                                fy,
                                                                null,
                                                                textColor,
                                                                nextZ(),
                                                            ),
                                                        )
                                                        lastTapUpTime = 0L
                                                    } else {
                                                        if (draft != null) commitDrafts()
                                                        selectedIndex = -1
                                                        selectedStroke = -1
                                                        lastTapUpTime = 0L
                                                    }
                                                }
                                                break
                                            }
                                            if (pressed.size >= 2) {
                                                moved = true
                                                val a = pressed[0].position
                                                val b = pressed[1].position
                                                val dist = (a - b).getDistance()
                                                val centroid = (a + b) / 2f
                                                if (!pinching) {
                                                    pinching = true
                                                    lastDist = dist
                                                    lastCentroid = centroid
                                                } else if (lastDist > 0f) {
                                                    val zoom = dist / lastDist
                                                    scale = (scale * zoom).coerceIn(1f, 8f)
                                                    lastDist = dist
                                                    val cDelta = centroid - lastCentroid
                                                    if (scale > 1f) {
                                                        offsetX += cDelta.x
                                                        offsetY += cDelta.y
                                                    }
                                                    lastCentroid = centroid
                                                }
                                                pressed.forEach { it.consume() }
                                            } else {
                                                val change = pressed.first()
                                                val pos = change.position
                                                if (!moved) {
                                                    if ((pos - downPos).getDistance() < touchSlop) {
                                                        continue
                                                    }
                                                    moved = true
                                                    if (targetOnText) {
                                                        movingIndex = targetIndex
                                                        movingDraft = targetIsDraft
                                                        if (!targetIsDraft) {
                                                            recordMarkup()
                                                            val list = annotations[pageIndex].orEmpty().toMutableList()
                                                            if (targetIndex in list.indices) {
                                                                list[targetIndex] =
                                                                    list[targetIndex].copy(zOrder = nextZ())
                                                                annotations[pageIndex] = list
                                                            }
                                                        }
                                                    }
                                                }
                                                val delta = pos - lastPos
                                                lastPos = pos
                                                if (pinching) {
                                                    pinching = false
                                                    lastDist = 0f
                                                }
                                                if (targetOnText) {
                                                    val curBmp = baseBitmap ?: break
                                                    val rect = fittedRect(size, curBmp.width, curBmp.height)
                                                    if (rect.width > 0f && rect.height > 0f && scale > 0f) {
                                                        val dfx = delta.x / (scale * rect.width)
                                                        val dfy = delta.y / (scale * rect.height)
                                                        if (targetIsDraft) {
                                                            val d = draft ?: break
                                                            draft =
                                                                d.copy(
                                                                    xFraction =
                                                                        (d.xFraction + dfx).coerceIn(0f, 0.95f),
                                                                    yFraction =
                                                                        (d.yFraction + dfy).coerceIn(0f, 0.95f),
                                                                )
                                                        } else {
                                                            val list = annotations[pageIndex].orEmpty().toMutableList()
                                                            if (targetIndex in list.indices) {
                                                                val a = list[targetIndex]
                                                                list[targetIndex] =
                                                                    a.copy(
                                                                        xFraction =
                                                                            (a.xFraction + dfx).coerceIn(0f, 0.95f),
                                                                        yFraction =
                                                                            (a.yFraction + dfy).coerceIn(0f, 0.95f),
                                                                    )
                                                                annotations[pageIndex] = list
                                                            }
                                                        }
                                                    }
                                                } else if (targetStroke >= 0 && tool == MarkupTool.SELECT) {
                                                    val curBmp = baseBitmap ?: break
                                                    val rect = fittedRect(size, curBmp.width, curBmp.height)
                                                    if (rect.width > 0f && rect.height > 0f && scale > 0f) {
                                                        val dfx = delta.x / (scale * rect.width)
                                                        val dfy = delta.y / (scale * rect.height)
                                                        val list = strokes[pageIndex].orEmpty().toMutableList()
                                                        if (targetStroke in list.indices) {
                                                            if (movingIndex < 0) {
                                                                recordMarkup()
                                                                movingIndex = targetStroke
                                                            }
                                                            val s = list[targetStroke]
                                                            list[targetStroke] =
                                                                s.copy(
                                                                    points =
                                                                        s.points.map {
                                                                            (it.first + dfx).coerceIn(0f, 1f) to
                                                                                (it.second + dfy).coerceIn(0f, 1f)
                                                                        },
                                                                )
                                                            strokes[pageIndex] = list
                                                        }
                                                    }
                                                } else if (scale > 1f) {
                                                    offsetX += delta.x
                                                    offsetY += delta.y
                                                } else {
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                }
                                                change.consume()
                                            }
                                        }
                                        movingIndex = -1
                                        movingDraft = false
                                    }
                                } else if (drawMode) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val bmp = baseBitmap
                                        var started = false
                                        var lastFraction = 0.5f to 0.5f
                                        var pinching = false
                                        var lastDist = 0f
                                        var lastCentroid = Offset.Zero
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val pressed = event.changes.filter { it.pressed }
                                            if (pressed.isEmpty()) {
                                                if (started) {
                                                    val d = draftStroke
                                                    if (d != null && d.points.size >= 2) {
                                                        recordMarkup()
                                                        val list = strokes[d.pageIndex].orEmpty().toMutableList()
                                                        list.add(d)
                                                        strokes[d.pageIndex] = list
                                                    }
                                                    draftStroke = null
                                                }
                                                break
                                            }
                                            if (pressed.size >= 2) {
                                                val a = pressed[0].position
                                                val b = pressed[1].position
                                                val dist = (a - b).getDistance()
                                                val centroid = (a + b) / 2f
                                                if (!pinching) {
                                                    pinching = true
                                                    lastDist = dist
                                                    lastCentroid = centroid
                                                } else if (lastDist > 0f) {
                                                    val zoom = dist / lastDist
                                                    scale = (scale * zoom).coerceIn(1f, 8f)
                                                    lastDist = dist
                                                    val cDelta = centroid - lastCentroid
                                                    if (scale > 1f) {
                                                        offsetX += cDelta.x
                                                        offsetY += cDelta.y
                                                    }
                                                    lastCentroid = centroid
                                                }
                                                pressed.forEach { it.consume() }
                                                continue
                                            }
                                            if (pinching) {
                                                pinching = false
                                                lastDist = 0f
                                            }
                                            val change = pressed.first()
                                            val pos = change.position
                                            if ((pos - down.position).getDistance() < touchSlop) {
                                                change.consume()
                                                continue
                                            }
                                            if (bmp != null) {
                                                val mapped =
                                                    mapTapToFraction(
                                                        pos,
                                                        size,
                                                        bmp.width,
                                                        bmp.height,
                                                        scale,
                                                        offsetX,
                                                        offsetY,
                                                    )
                                                if (!started) {
                                                    started = true
                                                    draftStroke =
                                                        PdfStroke(
                                                            pageIndex = pageIndex,
                                                            points = listOf(mapped),
                                                            widthFraction = strokeWidth,
                                                            color = strokeColor,
                                                            straight = straightMode,
                                                            zOrder = nextZ(),
                                                            highlight = tool == MarkupTool.HIGHLIGHT,
                                                        )
                                                    lastFraction = mapped
                                                } else {
                                                    val d = draftStroke ?: break
                                                    if (straightMode) {
                                                        draftStroke =
                                                            d.copy(points = listOf(d.points.first(), mapped))
                                                    } else if (
                                                        Math.abs(mapped.first - lastFraction.first) +
                                                        Math.abs(mapped.second - lastFraction.second) >
                                                        0.002f
                                                    ) {
                                                        draftStroke = d.copy(points = d.points + mapped)
                                                        lastFraction = mapped
                                                    }
                                                }
                                            }
                                            change.consume()
                                        }
                                    }
                                } else {
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
                                }
                            },
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY,
                                ),
                    ) {
                        displayed.bitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                filterQuality = FilterQuality.High,
                            )
                        }
                        val editing = draft
                        val bmp = baseBitmap
                        if (editing != null && bmp != null && pageSize.width > 0) {
                            val rect = fittedRect(pageSize, bmp.width, bmp.height)
                            val x = rect.left + editing.xFraction * rect.width
                            val y = rect.top + editing.yFraction * rect.height
                            BasicTextField(
                                value = draftField,
                                onValueChange = { value ->
                                    draftField = value
                                    draft = draft?.copy(text = value.text)
                                },
                                onTextLayout = { draftFieldLayout = it },
                                modifier =
                                    Modifier
                                        .offset { IntOffset(x.toInt(), y.toInt()) }
                                        .widthIn(min = 48.dp, max = 220.dp)
                                        .focusRequester(focusRequester)
                                        .background(
                                            if (editing.color == android.graphics.Color.WHITE) {
                                                Color(0xFF333333)
                                            } else {
                                                Color.White.copy(alpha = 0.92f)
                                            },
                                        )
                                        .border(1.dp, Color(0xFF3F51B5))
                                        .onSizeChanged { draftFieldSize = it },
                                textStyle =
                                    TextStyle(
                                        color = Color(editing.color),
                                        fontSize = with(density) { (editing.sizeFraction * rect.height).toSp() },
                                    ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { commitDrafts() }),
                            )
                        }
                    }
                }
                // Floating Edit/Move/✕ chip anchored to the selection bounds (#36):
                // above the item by default, flipped below near the top edge and
                // clamped so it stays on screen, without ever covering the selection.
                if (tool == MarkupTool.SELECT && draft == null && (selectedIndex >= 0 || selectedStroke >= 0)) {
                    val chipBmp = baseBitmap
                    val anchor =
                        if (chipBmp != null && pageSize.width > 0) {
                            val fractionBounds =
                                if (selectedIndex >= 0) {
                                    annotations[pageIndex].orEmpty().getOrNull(selectedIndex)?.let {
                                        annotationBoundsFraction(it, chipBmp.width, chipBmp.height)
                                    }
                                } else {
                                    strokes[pageIndex].orEmpty().getOrNull(selectedStroke)?.let {
                                        strokeBoundsFraction(it, chipBmp.width, chipBmp.height)
                                    }
                                }
                            fractionBounds?.let { bounds ->
                                val fitted = fittedRect(pageSize, chipBmp.width, chipBmp.height)
                                val zoom = if (scale > 0f) scale else 1f
                                val screenWidth = pageSize.width.toFloat()
                                val screenHeight = pageSize.height.toFloat()

                                fun screenX(fraction: Float): Float {
                                    val x = fitted.left + fraction * fitted.width
                                    return (x - screenWidth / 2f) * zoom + screenWidth / 2f + offsetX
                                }

                                fun screenY(fraction: Float): Float {
                                    val y = fitted.top + fraction * fitted.height
                                    return (y - screenHeight / 2f) * zoom + screenHeight / 2f + offsetY
                                }
                                SelectionAnchor(
                                    left = screenX(bounds.left),
                                    top = screenY(bounds.top),
                                    right = screenX(bounds.right),
                                    bottom = screenY(bounds.bottom),
                                )
                            }
                        } else {
                            null
                        }
                    if (anchor != null) {
                        val density = LocalDensity.current
                        var chipSize by remember { mutableStateOf(IntSize.Zero) }
                        val gapPx = with(density) { 8.dp.toPx() }
                        val marginPx = with(density) { 8.dp.toPx() }
                        val chipWidth =
                            chipSize.width.takeIf { it > 0 }?.toFloat() ?: with(density) { 212.dp.toPx() }
                        val chipHeight =
                            chipSize.height.takeIf { it > 0 }?.toFloat() ?: with(density) { 48.dp.toPx() }
                        val aboveTop = anchor.top - gapPx - chipHeight
                        val chipTop =
                            if (aboveTop >= marginPx) {
                                aboveTop
                            } else {
                                (anchor.bottom + gapPx)
                                    .coerceAtMost(pageSize.height - chipHeight - marginPx)
                                    .coerceAtLeast(marginPx)
                            }
                        val chipLeft =
                            ((anchor.left + anchor.right) / 2f - chipWidth / 2f)
                                .coerceIn(marginPx, (pageSize.width - chipWidth - marginPx).toFloat())
                        Row(
                            modifier =
                                Modifier
                                    .offset {
                                        IntOffset(
                                            chipLeft.roundToInt(),
                                            chipTop.roundToInt(),
                                        )
                                    }
                                    .onSizeChanged { chipSize = it }
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(20.dp),
                                    )
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = {
                                val index = selectedIndex
                                val list = annotations[pageIndex].orEmpty()
                                if (index in list.indices) {
                                    val item = list[index]
                                    selectedIndex = -1
                                    selectedStroke = -1
                                    beginDraft(
                                        DraftAnnotation(
                                            item.text,
                                            item.sizeFraction,
                                            item.xFraction,
                                            item.yFraction,
                                            index,
                                            item.color,
                                            nextZ(),
                                        ),
                                    )
                                }
                            }) {
                                ButtonLabel(context.getString(R.string.edit))
                            }
                            TextButton(onClick = {}) {
                                ButtonLabel(context.getString(R.string.markup_move))
                            }
                            IconButton(onClick = { deleteSelection() }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = context.getString(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    val showTextOptions =
                        tool == MarkupTool.TEXT ||
                            draft != null ||
                            (tool == MarkupTool.SELECT && selectedIndex >= 0)
                    val showStrokeOptions =
                        !showTextOptions &&
                            (drawMode || (tool == MarkupTool.SELECT && selectedStroke >= 0))
                    if (showTextOptions) {
                        val size = draft?.sizeFraction ?: textSizeFraction
                        val stepIndex = markupTextSizeStepIndex(size)
                        val pointSize =
                            if (pageHeightPt > 0f) (nearestMarkupTextSize(size) * pageHeightPt).toInt() else 0
                        MarkupChoices(
                            stepDownDescription = context.getString(R.string.markup_size_decrease),
                            stepUpDescription = context.getString(R.string.markup_size_increase),
                            valueLabel = context.getString(R.string.markup_font_size, pointSize),
                            canStepDown = stepIndex > 0,
                            canStepUp = stepIndex < markupTextSizeStepCount() - 1,
                            onStepDown = { applyTextSize(markupTextSizeStep(stepIndex - 1)) },
                            onStepUp = { applyTextSize(markupTextSizeStep(stepIndex + 1)) },
                            color = draft?.color ?: textColor,
                            includeYellow = false,
                            onColor = { applyTextColor(it) },
                        )
                    } else if (showStrokeOptions) {
                        val selected = strokes[pageIndex].orEmpty().getOrNull(selectedStroke)
                        val width = selected?.widthFraction ?: strokeWidth
                        val stepIndex = markupWidthStepIndex(width)
                        val pointSize = if (pageHeightPt > 0f) (width * pageHeightPt).toInt() else 0
                        MarkupChoices(
                            stepDownDescription = context.getString(R.string.markup_size_decrease),
                            stepUpDescription = context.getString(R.string.markup_size_increase),
                            valueLabel = context.getString(R.string.markup_stroke_size_value, pointSize),
                            canStepDown = stepIndex > 0,
                            canStepUp = stepIndex < markupWidthStepCount() - 1,
                            onStepDown = { applyStrokeWidth(markupWidthStep(stepIndex - 1)) },
                            onStepUp = { applyStrokeWidth(markupWidthStep(stepIndex + 1)) },
                            color = selected?.color ?: strokeColor,
                            includeYellow = true,
                            onColor = { applyStrokeColor(it) },
                        )
                    }
                    MarkupToolBar(tool = tool) { next ->
                        commitDrafts()
                        if (next == MarkupTool.HIGHLIGHT && tool != MarkupTool.HIGHLIGHT) {
                            strokeColor = android.graphics.Color.YELLOW
                        }
                        tool = next
                        if (next != MarkupTool.SELECT && next != MarkupTool.TEXT) {
                            selectedIndex = -1
                        }
                    }
                }
            }
        }
    }

    if (showExitWarning) {
        AlertDialog(
            onDismissRequest = { showExitWarning = false },
            title = { Text(context.getString(R.string.markup_leave_title)) },
            text = { Text(context.getString(R.string.markup_leave_message)) },
            confirmButton = {
                FlowRow {
                    TextButton(onClick = { showExitWarning = false }) {
                        ButtonLabel(context.getString(R.string.cancel))
                    }
                    TextButton(onClick = onNavigateBack) {
                        ButtonLabel(context.getString(R.string.discard))
                    }
                    TextButton(
                        onClick = {
                            commitDrafts()
                            showExitWarning = false
                            val all = annotations.values.flatten()
                            val allStrokes = strokes.values.flatten()
                            viewModel.saveAnnotatedPdf(entry, all, allStrokes)
                        },
                    ) {
                        ButtonLabel(context.getString(R.string.save))
                    }
                }
            },
            dismissButton = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkupHistoryRow(
    canUndo: Boolean,
    canRedo: Boolean,
    canDelete: Boolean,
    pageIndex: Int,
    pageCount: Int,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDelete: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onGoToPage: (Int) -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MarkupHistoryButton(
            icon = Icons.AutoMirrored.Filled.Undo,
            label = context.getString(R.string.markup_undo),
            enabled = canUndo,
            onClick = onUndo,
        )
        MarkupHistoryButton(
            icon = Icons.AutoMirrored.Filled.Redo,
            label = context.getString(R.string.markup_redo),
            enabled = canRedo,
            onClick = onRedo,
        )
        MarkupHistoryButton(
            icon = Icons.Filled.Delete,
            label = context.getString(R.string.markup_delete),
            enabled = canDelete,
            onClick = onDelete,
        )
        PageSelector(
            pageIndex = pageIndex,
            pageCount = pageCount,
            onPrev = onPrev,
            onNext = onNext,
            onGoToPage = onGoToPage,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkupHistoryButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                icon,
                contentDescription = label,
                tint =
                    if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
            )
        }
    }
}

@Composable
private fun MarkupToolBar(
    tool: MarkupTool,
    onTool: (MarkupTool) -> Unit,
) {
    val context = LocalContext.current
    val items =
        listOf(
            Triple(MarkupTool.SELECT, Icons.Filled.TouchApp, R.string.markup_tool_select),
            Triple(MarkupTool.TEXT, Icons.Filled.Title, R.string.markup_tool_text),
            Triple(MarkupTool.LINE, Icons.AutoMirrored.Filled.ShowChart, R.string.markup_tool_line),
            Triple(MarkupTool.DRAW, Icons.Filled.Gesture, R.string.markup_tool_draw),
            Triple(MarkupTool.HIGHLIGHT, Icons.Filled.Highlight, R.string.markup_tool_highlight),
        )
    val labels = items.map { context.getString(it.third) }
    val labelStyle = MaterialTheme.typography.labelSmall
    // 8dp horizontal padding keeps the outermost labels off the screen edge
    // (#50; "Marker" used to touch it).
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        val slotWidth = constraints.maxWidth / items.size
        // One size for all five labels, single words shrink to the nav floor
        // (60%) instead of wrapping (#50): "Auswahl" broke as "Auswa/hl" and
        // "Marker" rendered smaller than the other labels when the row fell
        // back to per-label sizes.
        val commonSize =
            rememberCommonRowLabelSize(
                labels,
                slotWidth,
                labelStyle,
                floorFraction = NAV_LABEL_FONT_FLOOR_FRACTION,
            )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, (mode, icon, _) ->
                val label = labels[index]
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CompactIconButton(
                        onClick = { onTool(mode) },
                        icon = icon,
                        contentDescription = label,
                        active = tool == mode,
                        label = {
                            if (commonSize != null) {
                                // All five labels share ONE size: the size the
                                // longest label needs in its slot (#50).
                                Text(
                                    text = label,
                                    style =
                                        scaledLabelStyle(
                                            labelStyle,
                                            commonSize,
                                            labelStyle.fontSize.value,
                                        ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                )
                            } else {
                                // Even the 60% floor does not fit the longest
                                // label on one line: allow two lines at word
                                // boundaries, never mid-word, at the same
                                // floor (#50).
                                ButtonLabel(
                                    text = label,
                                    style = labelStyle,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    floorFraction = NAV_LABEL_FONT_FLOOR_FRACTION,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MarkupChoices(
    stepDownDescription: String,
    stepUpDescription: String,
    valueLabel: String,
    canStepDown: Boolean,
    canStepUp: Boolean,
    onStepDown: () -> Unit,
    onStepUp: () -> Unit,
    color: Int,
    includeYellow: Boolean,
    onColor: (Int) -> Unit,
) {
    val context = LocalContext.current
    val colors =
        buildList {
            add(android.graphics.Color.BLACK to R.string.markup_color_black)
            add(android.graphics.Color.WHITE to R.string.markup_color_white)
            add(android.graphics.Color.RED to R.string.markup_color_red)
            if (includeYellow) add(android.graphics.Color.YELLOW to R.string.markup_color_yellow)
        }
    // FlowRow so the stepper and the colour swatches wrap to a second line
    // instead of squeezing on narrow screens with large font scale (#50).
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        MarkupSizeStepper(
            stepDownDescription = stepDownDescription,
            stepUpDescription = stepUpDescription,
            valueLabel = valueLabel,
            canStepDown = canStepDown,
            canStepUp = canStepUp,
            onStepDown = onStepDown,
            onStepUp = onStepUp,
        )
        colors.forEach { (argb, labelRes) ->
            val selected = color == argb
            val description = context.getString(labelRes)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 3.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(28.dp)
                            .semantics { contentDescription = description }
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color =
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                shape = CircleShape,
                            )
                            .clickable { onColor(argb) },
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MarkupSizeStepper(
    stepDownDescription: String,
    stepUpDescription: String,
    valueLabel: String,
    canStepDown: Boolean,
    canStepUp: Boolean,
    onStepDown: () -> Unit,
    onStepUp: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onStepDown, enabled = canStepDown) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = stepDownDescription,
                tint =
                    if (canStepDown) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
            )
        }
        Text(
            text = valueLabel,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 48.dp),
        )
        IconButton(onClick = onStepUp, enabled = canStepUp) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stepUpDescription,
                tint =
                    if (canStepUp) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
            )
        }
    }
}

@Composable
private fun CompactIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    enabled: Boolean = true,
    active: Boolean = false,
    label: (@Composable () -> Unit)? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier =
                Modifier
                    .size(40.dp)
                    .background(
                        if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        },
                        CircleShape,
                    ),
        ) {
            Icon(
                icon,
                contentDescription,
                tint =
                    if (active) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
        label?.invoke()
    }
}

private fun drawAnnotations(
    base: Bitmap,
    annotations: List<PdfAnnotation>,
    strokes: List<PdfStroke>,
    highlightIndex: Int? = null,
    selectedStrokeIndex: Int = -1,
): Bitmap {
    val out = base.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(out)
    val items =
        buildList {
            annotations.forEachIndexed { index, ann -> add(DrawItem.Text(index, ann, ann.zOrder)) }
            strokes.forEachIndexed { index, stroke -> add(DrawItem.Stroke(index, stroke, stroke.zOrder)) }
        }.sortedBy { it.z }
    for (item in items) {
        when (item) {
            is DrawItem.Text -> {
                val ann = item.ann
                val paint =
                    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = ann.color
                        textSize = ann.sizeFraction * out.height
                        typeface = android.graphics.Typeface.DEFAULT
                    }
                val x = ann.xFraction * out.width
                val topY = ann.yFraction * out.height
                val lines = wrapAnnotationText(ann.text, paint, (out.width * 0.95f).toInt())
                val textHeight = lines.size * paint.fontSpacing
                val textWidth = lines.maxOfOrNull { paint.measureText(it) } ?: 0f
                if (item.index == highlightIndex) {
                    val pad = 8f
                    val highlight =
                        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = 0xFF3F51B5.toInt()
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 5f
                        }
                    canvas.drawRoundRect(
                        x - pad,
                        topY - pad,
                        x + textWidth + pad,
                        topY + textHeight + pad,
                        10f,
                        10f,
                        highlight,
                    )
                    drawHandles(canvas, x - pad, topY - pad, x + textWidth + pad, topY + textHeight + pad)
                }
                var y = topY - paint.ascent()
                lines.forEach { line ->
                    canvas.drawText(line, x, y, paint)
                    y += paint.fontSpacing
                }
            }
            is DrawItem.Stroke -> {
                val stroke = item.stroke
                val paint =
                    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = stroke.color
                        strokeWidth = stroke.widthFraction * out.height * if (stroke.highlight) 3f else 1f
                        style = android.graphics.Paint.Style.STROKE
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                        if (stroke.highlight) alpha = 90
                    }
                if (item.index == selectedStrokeIndex && stroke.points.isNotEmpty()) {
                    val bounds = strokeBounds(stroke, out.width, out.height)
                    val outline =
                        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = 0xFF3F51B5.toInt()
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 4f
                        }
                    canvas.drawRect(bounds, outline)
                    drawHandles(canvas, bounds.left, bounds.top, bounds.right, bounds.bottom)
                }
                val pts = stroke.points
                if (pts.isNotEmpty()) {
                    val path = android.graphics.Path()
                    path.moveTo(pts.first().first * out.width, pts.first().second * out.height)
                    if (stroke.straight && pts.size > 1) {
                        path.lineTo(pts.last().first * out.width, pts.last().second * out.height)
                    } else {
                        pts.drop(1).forEach { (fx, fy) ->
                            path.lineTo(fx * out.width, fy * out.height)
                        }
                    }
                    canvas.drawPath(path, paint)
                }
            }
        }
    }
    canvas.setBitmap(null)
    return out
}

private fun wrapAnnotationText(
    text: String,
    paint: android.graphics.Paint,
    maxWidthPx: Int,
): List<String> {
    // Explicit line breaks (#59) always break the annotation; each segment then word-wraps.
    return if (maxWidthPx <= 0) {
        text.split('\n')
    } else {
        text.split('\n').flatMap { wrapSegment(it, paint, maxWidthPx) }
    }
}

private fun wrapSegment(
    segment: String,
    paint: android.graphics.Paint,
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

private fun fittedRect(
    container: IntSize,
    bmpW: Int,
    bmpH: Int,
): Rect {
    val sw = container.width.toFloat()
    val sh = container.height.toFloat()
    if (sw <= 0f || sh <= 0f || bmpW <= 0 || bmpH <= 0) {
        return Rect(0f, 0f, sw, sh)
    }
    val aspect = bmpW.toFloat() / bmpH
    val screenAspect = sw / sh
    return if (aspect > screenAspect) {
        val dw = sw
        val dh = sw / aspect
        Rect(0f, (sh - dh) / 2f, dw, (sh - dh) / 2f + dh)
    } else {
        val dw = sh * aspect
        val dh = sh
        Rect((sw - dw) / 2f, 0f, (sw - dw) / 2f + dw, dh)
    }
}

private const val ANNOTATION_HIT_PAD = 0.05f

private fun annotationHitTest(
    fx: Float,
    fy: Float,
    xFraction: Float,
    yFraction: Float,
    text: String,
    sizeFraction: Float,
    bitmapWidth: Int,
    bitmapHeight: Int,
): Boolean {
    if (text.isBlank() || bitmapWidth <= 0 || bitmapHeight <= 0) return false
    val paint =
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizeFraction * bitmapHeight
            typeface = android.graphics.Typeface.DEFAULT
        }
    val lines = wrapAnnotationText(text, paint, (bitmapWidth * 0.95f).toInt())
    val widthPx = lines.maxOfOrNull { paint.measureText(it) } ?: 0f
    val heightPx = lines.size * paint.fontSpacing
    val left = xFraction - ANNOTATION_HIT_PAD
    val top = yFraction - ANNOTATION_HIT_PAD
    val right = xFraction + widthPx / bitmapWidth + ANNOTATION_HIT_PAD
    val bottom = yFraction + heightPx / bitmapHeight + ANNOTATION_HIT_PAD
    return fx >= left && fx <= right && fy >= top && fy <= bottom
}

/**
 * On-screen bounds of the text field shown for the box being edited (#55):
 * the measured field size when available, otherwise the canvas text metrics
 * used for the saved rendering.
 */
private fun editFieldScreenRect(
    d: DraftAnnotation,
    container: IntSize,
    bmpWidth: Int,
    bmpHeight: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    fieldWidth: Float,
    fieldHeight: Float,
): Rect? {
    if (bmpWidth <= 0 || bmpHeight <= 0) return null
    val fitted = fittedRect(container, bmpWidth, bmpHeight)
    if (fitted.width <= 0f || fitted.height <= 0f) return null
    var width = fieldWidth
    var height = fieldHeight
    if (width <= 0f || height <= 0f) {
        val paint =
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textSize = d.sizeFraction * bmpHeight
                typeface = android.graphics.Typeface.DEFAULT
            }
        val lines = wrapAnnotationText(d.text, paint, (bmpWidth * 0.95f).toInt())
        width = (lines.maxOfOrNull { paint.measureText(it) } ?: 0f) / bmpWidth * fitted.width
        height = lines.size * paint.fontSpacing / bmpHeight * fitted.height
    }
    val topLeft =
        pageToScreen(
            Offset(fitted.left + d.xFraction * fitted.width, fitted.top + d.yFraction * fitted.height),
            container,
            scale,
            offsetX,
            offsetY,
        )
    return Rect(topLeft.x, topLeft.y, topLeft.x + width, topLeft.y + height)
}

/**
 * Caret offset inside the rendered text of [annotation] for a tap at page
 * bitmap fractions ([fx], [fy]) (#55); null when the tap cannot be mapped, in
 * which case the caller falls back to the end of the text.
 */
private fun tappedCaretOffset(
    fx: Float,
    fy: Float,
    annotation: PdfAnnotation,
    bitmapWidth: Int,
    bitmapHeight: Int,
): Int? {
    if (bitmapWidth <= 0 || bitmapHeight <= 0 || annotation.text.isEmpty()) return null
    val paint =
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = annotation.sizeFraction * bitmapHeight
            typeface = android.graphics.Typeface.DEFAULT
        }
    val lines = wrapAnnotationText(annotation.text, paint, (bitmapWidth * 0.95f).toInt())
    val caret =
        caretOffsetForTap(
            (fx - annotation.xFraction) * bitmapWidth,
            (fy - annotation.yFraction) * bitmapHeight,
            lines,
            paint.fontSpacing,
            paint::measureText,
        )
    return caret.coerceIn(0, annotation.text.length)
}

private fun mapTapToFraction(
    tap: Offset,
    container: IntSize,
    bmpW: Int,
    bmpH: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
): Pair<Float, Float> {
    val rect = fittedRect(container, bmpW, bmpH)
    val sw = container.width.toFloat()
    val sh = container.height.toFloat()
    if (sw <= 0f || sh <= 0f || rect.width <= 0f || rect.height <= 0f) {
        return 0.5f to 0.5f
    }
    val s = if (scale > 0f) scale else 1f
    val x1 = tap.x - offsetX
    val y1 = tap.y - offsetY
    val cx = (x1 - sw / 2f) / s + sw / 2f
    val cy = (y1 - sh / 2f) / s + sh / 2f
    val px = ((cx - rect.left) / rect.width * bmpW).coerceIn(0f, bmpW.toFloat())
    val py = ((cy - rect.top) / rect.height * bmpH).coerceIn(0f, bmpH.toFloat())
    return px / bmpW to py / bmpH
}

private fun drawHandles(
    canvas: android.graphics.Canvas,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
) {
    val paint =
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF3F51B5.toInt()
            style = android.graphics.Paint.Style.FILL
        }
    val half = 8f
    listOf(left to top, right to top, left to bottom, right to bottom).forEach { (x, y) ->
        canvas.drawRect(x - half, y - half, x + half, y + half, paint)
    }
}

private fun strokeBounds(
    stroke: PdfStroke,
    width: Int,
    height: Int,
): android.graphics.RectF {
    val pts =
        if (stroke.straight && stroke.points.size > 1) {
            listOf(stroke.points.first(), stroke.points.last())
        } else {
            stroke.points
        }
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = 0f
    var maxY = 0f
    pts.forEach { (fx, fy) ->
        val x = fx * width
        val y = fy * height
        minX = minOf(minX, x)
        minY = minOf(minY, y)
        maxX = maxOf(maxX, x)
        maxY = maxOf(maxY, y)
    }
    val pad = 12f
    return android.graphics.RectF(minX - pad, minY - pad, maxX + pad, maxY + pad)
}

private fun strokeHitIndex(
    strokes: List<PdfStroke>,
    fx: Float,
    fy: Float,
): Int {
    for (index in strokes.indices.reversed()) {
        if (strokeContains(strokes[index], fx, fy)) return index
    }
    return -1
}

private fun strokeContains(
    stroke: PdfStroke,
    fx: Float,
    fy: Float,
): Boolean {
    val pts = stroke.points
    if (pts.isEmpty()) return false
    val pad = (if (stroke.highlight) 0.04f else 0.02f) + stroke.widthFraction
    val seq = if (stroke.straight && pts.size > 1) listOf(pts.first(), pts.last()) else pts
    if (seq.size == 1) {
        val point = seq.first()
        return kotlin.math.abs(point.first - fx) + kotlin.math.abs(point.second - fy) <= pad
    }
    for (index in 0 until seq.size - 1) {
        if (distanceToSegment(fx, fy, seq[index], seq[index + 1]) <= pad) return true
    }
    return false
}

private fun distanceToSegment(
    fx: Float,
    fy: Float,
    start: Pair<Float, Float>,
    end: Pair<Float, Float>,
): Float {
    val dx = end.first - start.first
    val dy = end.second - start.second
    val len2 = dx * dx + dy * dy
    if (len2 <= 0.0000001f) {
        return kotlin.math.abs(fx - start.first) + kotlin.math.abs(fy - start.second)
    }
    val t = (((fx - start.first) * dx + (fy - start.second) * dy) / len2).coerceIn(0f, 1f)
    val px = start.first + t * dx
    val py = start.second + t * dy
    val ox = fx - px
    val oy = fy - py
    return kotlin.math.sqrt(ox * ox + oy * oy)
}

/** On-screen bounds of the selected annotation or stroke, used to anchor the chip. */
private data class SelectionAnchor(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/** Same text metrics as the hit test, expressed as fractions of the page bitmap. */
private fun annotationBoundsFraction(
    annotation: PdfAnnotation,
    bitmapWidth: Int,
    bitmapHeight: Int,
): Rect? {
    if (annotation.text.isBlank() || bitmapWidth <= 0 || bitmapHeight <= 0) return null
    val paint =
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = annotation.sizeFraction * bitmapHeight
            typeface = android.graphics.Typeface.DEFAULT
        }
    val lines = wrapAnnotationText(annotation.text, paint, (bitmapWidth * 0.95f).toInt())
    val widthPx = lines.maxOfOrNull { paint.measureText(it) } ?: 0f
    val heightPx = lines.size * paint.fontSpacing
    return Rect(
        annotation.xFraction,
        annotation.yFraction,
        (annotation.xFraction + widthPx / bitmapWidth).coerceAtMost(1f),
        (annotation.yFraction + heightPx / bitmapHeight).coerceAtMost(1f),
    )
}

private fun strokeBoundsFraction(
    stroke: PdfStroke,
    bitmapWidth: Int,
    bitmapHeight: Int,
): Rect? {
    if (bitmapWidth <= 0 || bitmapHeight <= 0 || stroke.points.isEmpty()) return null
    val bounds = strokeBounds(stroke, bitmapWidth, bitmapHeight)
    return Rect(
        (bounds.left / bitmapWidth).coerceIn(0f, 1f),
        (bounds.top / bitmapHeight).coerceIn(0f, 1f),
        (bounds.right / bitmapWidth).coerceIn(0f, 1f),
        (bounds.bottom / bitmapHeight).coerceIn(0f, 1f),
    )
}

@Preview(fontScale = 2f, widthDp = 320, heightDp = 120, showBackground = true)
@Composable
internal fun MarkupHistoryRowPreview() {
    MaterialTheme {
        MarkupHistoryRow(
            canUndo = true,
            canRedo = false,
            canDelete = true,
            pageIndex = 0,
            pageCount = 250,
            onUndo = {},
            onRedo = {},
            onDelete = {},
            onPrev = {},
            onNext = {},
            onGoToPage = {},
        )
    }
}

@Preview(fontScale = 2f, widthDp = 320, heightDp = 140, showBackground = true)
@Composable
internal fun MarkupSizeStepperPreview() {
    MaterialTheme {
        MarkupSizeStepper(
            stepDownDescription = "Decrease size",
            stepUpDescription = "Increase size",
            valueLabel = "Aa 76 pt",
            canStepDown = true,
            canStepUp = false,
            onStepDown = {},
            onStepUp = {},
        )
    }
}

@Preview(fontScale = 2f, widthDp = 320, heightDp = 120, showBackground = true)
@Composable
internal fun MarkupToolBarPreview() {
    MaterialTheme {
        MarkupToolBar(tool = MarkupTool.SELECT, onTool = {})
    }
}

@Preview(fontScale = 2f, widthDp = 320, heightDp = 160, showBackground = true)
@Composable
internal fun MarkupChoicesPreview() {
    MaterialTheme {
        MarkupChoices(
            stepDownDescription = "Decrease size",
            stepUpDescription = "Increase size",
            valueLabel = "17 pt",
            canStepDown = true,
            canStepUp = true,
            onStepDown = {},
            onStepUp = {},
            color = android.graphics.Color.BLACK,
            includeYellow = true,
            onColor = {},
        )
    }
}
