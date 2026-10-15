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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.MarkupHistory
import com.phaprodevelop.klipsilo.data.STROKE_PRESETS
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.nearestStrokePreset
import com.phaprodevelop.klipsilo.data.repository.PdfAnnotation
import com.phaprodevelop.klipsilo.data.repository.PdfStroke
import com.phaprodevelop.klipsilo.data.settings.AnnotateSettings
import com.phaprodevelop.klipsilo.data.settings.AnnotateSettingsStore
import com.phaprodevelop.klipsilo.ui.components.BitmapUseCounter
import com.phaprodevelop.klipsilo.ui.components.pdfPageCount
import com.phaprodevelop.klipsilo.ui.components.pdfPageHeightPt
import com.phaprodevelop.klipsilo.ui.components.produceLatest
import com.phaprodevelop.klipsilo.ui.components.rememberDisplayedBitmap
import com.phaprodevelop.klipsilo.ui.components.renderPdfPage
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

private data class MarkupSnapshot(
    val annotations: Map<Int, List<PdfAnnotation>>,
    val strokes: Map<Int, List<PdfStroke>>,
)

private fun markupFileName(entry: ClipboardEntry): String {
    val path = entry.filePath
    if (!path.isNullOrBlank()) {
        val name = File(path).name
        if (name.isNotBlank()) return name
    }
    return entry.previewText
}

private data class DraftAnnotation(
    val text: String,
    val sizeFraction: Float,
    val xFraction: Float,
    val yFraction: Float,
    val editingIndex: Int?,
    val color: Int = android.graphics.Color.BLACK,
    val zOrder: Long = 0L,
)

private sealed class DrawItem(val z: Long) {
    class Text(val index: Int, val ann: PdfAnnotation, z: Long) : DrawItem(z)

    class Stroke(val stroke: PdfStroke, z: Long) : DrawItem(z)
}

@OptIn(ExperimentalMaterial3Api::class)
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
                    Text(context.getString(R.string.back))
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
    var annotationMode by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf<DraftAnnotation?>(null) }
    var drawMode by remember { mutableStateOf(false) }
    var straightMode by remember { mutableStateOf(false) }
    val strokes = remember { mutableStateMapOf<Int, List<PdfStroke>>() }
    var draftStroke by remember { mutableStateOf<PdfStroke?>(null) }
    val savedAnnotate = remember { AnnotateSettingsStore.load(context) }
    var strokeWidth by remember { mutableFloatStateOf(savedAnnotate.strokeWidthFraction) }
    var strokeColor by remember { mutableIntStateOf(savedAnnotate.strokeColor) }
    var textSizeFraction by remember { mutableFloatStateOf(savedAnnotate.textSizeFraction) }
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
    var showClearStrokesConfirm by remember { mutableStateOf(false) }
    var strokeSavedFlash by remember { mutableStateOf(false) }
    LaunchedEffect(strokeSavedFlash) {
        if (strokeSavedFlash) {
            kotlinx.coroutines.delay(1200)
            strokeSavedFlash = false
        }
    }
    var movingIndex by remember { mutableIntStateOf(-1) }
    var movingDraft by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showExitWarning by remember { mutableStateOf(false) }
    val annotations = remember { mutableStateMapOf<Int, List<PdfAnnotation>>() }
    var pageHeightPt by remember { mutableFloatStateOf(0f) }
    var savedAnnotations by remember { mutableStateOf(emptyList<PdfAnnotation>()) }
    var savedStrokes by remember { mutableStateOf(emptyList<PdfStroke>()) }
    var generatedPdf by remember { mutableStateOf(false) }
    val dirty =
        annotations.values.flatten() != savedAnnotations ||
            strokes.values.flatten() != savedStrokes ||
            draft != null ||
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

    fun undoMarkup() {
        val restored = markupHistory.undo(markupSnapshot()) ?: return
        applyMarkupSnapshot(restored)
        draft = null
        draftStroke = null
        selectedIndex = -1
        historyTick++
    }

    fun redoMarkup() {
        val restored = markupHistory.redo(markupSnapshot()) ?: return
        applyMarkupSnapshot(restored)
        draft = null
        draftStroke = null
        selectedIndex = -1
        historyTick++
    }

    fun commitDrafts() {
        val d = draft
        val pendingStroke = draftStroke
        if ((d != null && d.text.isNotBlank()) || (pendingStroke != null && pendingStroke.points.size >= 2)) {
            recordMarkup()
        }
        if (d != null && d.text.isNotBlank()) {
            val list = annotations[pageIndex].orEmpty().toMutableList()
            val newAnn =
                PdfAnnotation(
                    pageIndex = pageIndex,
                    xFraction = d.xFraction,
                    yFraction = d.yFraction,
                    text = d.text.trim(),
                    sizeFraction = d.sizeFraction,
                    color = d.color,
                    zOrder = d.zOrder,
                )
            if (d.editingIndex != null && d.editingIndex < list.size) {
                list[d.editingIndex] = newAnn
            } else {
                list.add(newAnn)
            }
            annotations[pageIndex] = list
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
            showDeleteConfirm = false
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
                            drawAnnotations(base, shown, shownStrokes, highlight)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = context.getString(R.string.annotate_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val fileName = markupFileName(entry)
                        if (fileName.isNotBlank()) {
                            Text(
                                text = fileName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (dirty) {
                            showExitWarning = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = context.getString(R.string.back),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            commitDrafts()
                            val all = annotations.values.flatten()
                            val allStrokes = strokes.values.flatten()
                            viewModel.saveAnnotatedPdf(entry, all, allStrokes)
                        },
                        enabled = entry.filePath != null,
                    ) {
                        Text(context.getString(R.string.save_pdf))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars)),
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .pointerInput(annotationMode, drawMode, straightMode, baseBitmap, pageIndex) {
                                val touchSlop = viewConfiguration.touchSlop
                                if (annotationMode) {
                                    var lastTapUpTime = 0L
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val downPos = down.position
                                        var targetIndex = -1
                                        var targetIsDraft = false
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
                                            }
                                        }
                                        val targetOnText = targetIndex >= 0 || targetIsDraft

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
                                                    if (isDoubleTap && targetOnText && !targetIsDraft) {
                                                        val list = annotations[pageIndex].orEmpty()
                                                        if (targetIndex in list.indices) {
                                                            val a = list[targetIndex]
                                                            selectedIndex = -1
                                                            draft =
                                                                DraftAnnotation(
                                                                    a.text,
                                                                    a.sizeFraction,
                                                                    a.xFraction,
                                                                    a.yFraction,
                                                                    targetIndex,
                                                                    a.color,
                                                                    nextZ(),
                                                                )
                                                        }
                                                        lastTapUpTime = 0L
                                                    } else {
                                                        if (targetOnText && !targetIsDraft) {
                                                            selectedIndex = targetIndex
                                                        } else {
                                                            if (selectedIndex < 0 && draft == null && !targetOnText) {
                                                                draft =
                                                                    DraftAnnotation(
                                                                        "",
                                                                        textSizeFraction,
                                                                        fx,
                                                                        fy,
                                                                        null,
                                                                        textColor,
                                                                        nextZ(),
                                                                    )
                                                            } else {
                                                                selectedIndex = -1
                                                            }
                                                        }
                                                        lastTapUpTime = if (targetOnText && !targetIsDraft) now else 0L
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
                                                    if (d != null && d.points.size < 2) {
                                                        draftStroke = null
                                                    }
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
                    }
                }
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    if (annotationMode && selectedIndex >= 0 && draft == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        ) {
                            TextButton(onClick = {
                                val list = annotations[pageIndex].orEmpty()
                                if (selectedIndex in list.indices) {
                                    val a = list[selectedIndex]
                                    draft =
                                        DraftAnnotation(
                                            a.text,
                                            a.sizeFraction,
                                            a.xFraction,
                                            a.yFraction,
                                            selectedIndex,
                                            a.color,
                                            nextZ(),
                                        )
                                }
                                selectedIndex = -1
                            }) {
                                Text(context.getString(R.string.edit))
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { showDeleteConfirm = true }) {
                                Text(
                                    context.getString(R.string.delete),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    if (annotationMode && draft != null) {
                        AnnotationPanel(
                            draft = draft!!,
                            pointSize = if (pageHeightPt > 0f) draft!!.sizeFraction * pageHeightPt else 0f,
                            onDraftChange = { newDraft ->
                                if (newDraft.sizeFraction != draft?.sizeFraction) {
                                    textSizeFraction = newDraft.sizeFraction
                                }
                                if (newDraft.color != draft?.color) {
                                    textColor = newDraft.color
                                }
                                draft = newDraft
                            },
                            onCommit = { persistAnnotateSettings() },
                            onConfirm = { commitDrafts() },
                        )
                    }
                    if (drawMode) {
                        StrokePanel(
                            thickness = strokeWidth,
                            color = strokeColor,
                            onThicknessChange = { strokeWidth = it },
                            onColorChange = {
                                strokeColor = it
                                draftStroke = draftStroke?.copy(color = it)
                            },
                            onCommit = { persistAnnotateSettings() },
                            saved = strokeSavedFlash,
                        )
                    }
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 40.dp) {
                    if (pageCount > 1) {
                        CompactIconButton(
                            onClick = {
                                if (pageIndex > 0) {
                                    pageIndex--
                                    offsetX = 0f
                                    offsetY = 0f
                                    scale = 1f
                                }
                            },
                            enabled = pageIndex > 0,
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("${pageIndex + 1} / $pageCount", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(4.dp))
                        CompactIconButton(
                            onClick = {
                                if (pageIndex < pageCount - 1) {
                                    pageIndex++
                                    offsetX = 0f
                                    offsetY = 0f
                                    scale = 1f
                                }
                            },
                            enabled = pageIndex < pageCount - 1,
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    CompactIconButton(
                        onClick = {
                            commitDrafts()
                            annotationMode = true
                            drawMode = false
                        },
                        icon = Icons.Filled.Edit,
                        contentDescription = context.getString(R.string.add_text_annotation),
                        active = annotationMode,
                    )
                    Spacer(Modifier.width(8.dp))
                    CompactIconButton(
                        onClick = {
                            commitDrafts()
                            if (drawMode && straightMode) {
                                drawMode = false
                            } else {
                                drawMode = true
                                straightMode = true
                                annotationMode = false
                                selectedIndex = -1
                            }
                        },
                        icon = Icons.Filled.ShowChart,
                        contentDescription = context.getString(R.string.straight_line),
                        active = drawMode && straightMode,
                    )
                    Spacer(Modifier.width(8.dp))
                    CompactIconButton(
                        onClick = {
                            commitDrafts()
                            if (drawMode && !straightMode) {
                                drawMode = false
                            } else {
                                drawMode = true
                                straightMode = false
                                annotationMode = false
                                selectedIndex = -1
                            }
                        },
                        icon = Icons.Filled.Gesture,
                        contentDescription = context.getString(R.string.freehand),
                        active = drawMode && !straightMode,
                    )
                    Spacer(Modifier.width(8.dp))
                    CompactIconButton(
                        onClick = { undoMarkup() },
                        icon = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = context.getString(R.string.undo),
                        enabled = historyTick >= 0 && markupHistory.canUndo,
                    )
                    Spacer(Modifier.width(8.dp))
                    CompactIconButton(
                        onClick = { redoMarkup() },
                        icon = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = context.getString(R.string.redo),
                        enabled = historyTick >= 0 && markupHistory.canRedo,
                    )
                    Spacer(Modifier.width(8.dp))
                    CompactIconButton(
                        onClick = {
                            val editing = draft?.editingIndex
                            if (editing != null) {
                                recordMarkup()
                                val list = annotations[pageIndex].orEmpty().toMutableList()
                                if (editing < list.size) {
                                    list.removeAt(editing)
                                    annotations[pageIndex] = list
                                }
                                draft = null
                            } else if (selectedIndex >= 0) {
                                showDeleteConfirm = true
                            } else if (drawMode && strokes.values.any { it.isNotEmpty() }) {
                                showClearStrokesConfirm = true
                            }
                        },
                        icon = Icons.Filled.Delete,
                        contentDescription = context.getString(R.string.delete),
                        enabled =
                            selectedIndex >= 0 ||
                                draft?.editingIndex != null ||
                                (drawMode && strokes.values.any { it.isNotEmpty() }),
                    )
                    Spacer(Modifier.width(8.dp))
                    CompactIconButton(
                        onClick = {
                            val hadStroke = draftStroke != null
                            commitDrafts()
                            if (hadStroke) {
                                strokeSavedFlash = true
                            }
                            annotationMode = false
                            drawMode = false
                            selectedIndex = -1
                            showDeleteConfirm = false
                        },
                        icon = Icons.Filled.Check,
                        contentDescription = context.getString(R.string.done),
                        enabled = annotationMode || drawMode || draft != null || draftStroke != null,
                    )
                }
            }
        }
    }

    if (showClearStrokesConfirm) {
        AlertDialog(
            onDismissRequest = { showClearStrokesConfirm = false },
            title = { Text(context.getString(R.string.clear_all_strokes)) },
            text = { Text(context.getString(R.string.clear_all_strokes_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    recordMarkup()
                    strokes.clear()
                    showClearStrokesConfirm = false
                }) {
                    Text(
                        context.getString(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearStrokesConfirm = false }) {
                    Text(context.getString(R.string.cancel))
                }
            },
        )
    }

    if (showExitWarning) {
        AlertDialog(
            onDismissRequest = { showExitWarning = false },
            title = {
                Text(
                    if (generatedPdf) {
                        context.getString(R.string.discard_changes)
                    } else {
                        context.getString(R.string.pdf_not_generated)
                    },
                )
            },
            text = {
                Text(
                    if (generatedPdf) {
                        context.getString(R.string.discard_changes_message)
                    } else {
                        context.getString(R.string.pdf_not_generated_message)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = onNavigateBack) {
                    Text(context.getString(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitWarning = false }) {
                    Text(context.getString(R.string.cancel))
                }
            },
        )
    }

    if (showDeleteConfirm && selectedIndex >= 0) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(context.getString(R.string.delete)) },
            text = { Text(context.getString(R.string.delete_annotation_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    recordMarkup()
                    val list = annotations[pageIndex].orEmpty().toMutableList()
                    if (selectedIndex in list.indices) {
                        list.removeAt(selectedIndex)
                        annotations[pageIndex] = list
                    }
                    selectedIndex = -1
                    showDeleteConfirm = false
                }) {
                    Text(
                        context.getString(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(context.getString(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun CompactIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    enabled: Boolean = true,
    active: Boolean = false,
) {
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
}

@Composable
private fun AnnotationPanel(
    draft: DraftAnnotation,
    pointSize: Float,
    onDraftChange: (DraftAnnotation) -> Unit,
    onCommit: () -> Unit,
    onConfirm: () -> Unit,
) {
    val context = LocalContext.current
    val colors =
        listOf(
            android.graphics.Color.BLACK to Color.Black,
            android.graphics.Color.WHITE to Color.White,
            android.graphics.Color.RED to Color.Red,
        )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft.text,
                onValueChange = { onDraftChange(draft.copy(text = it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(context.getString(R.string.enter_text)) },
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onConfirm) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = context.getString(R.string.ok),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.FormatSize,
                contentDescription = context.getString(R.string.font_size),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Slider(
                value = draft.sizeFraction,
                onValueChange = { onDraftChange(draft.copy(sizeFraction = it)) },
                onValueChangeFinished = onCommit,
                valueRange = 0.005f..0.05f,
                steps = 8,
                modifier = Modifier.weight(1f),
            )
            colors.forEach { (argb, composeColor) ->
                val selected = draft.color == argb
                Box(
                    modifier =
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(composeColor)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color =
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                shape = CircleShape,
                            )
                            .clickable {
                                onDraftChange(draft.copy(color = argb))
                                onCommit()
                            },
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = String.format(java.util.Locale.US, "%.0f pt", pointSize),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun StrokePanel(
    thickness: Float,
    color: Int,
    onThicknessChange: (Float) -> Unit,
    onColorChange: (Int) -> Unit,
    onCommit: () -> Unit,
    saved: Boolean,
) {
    val context = LocalContext.current
    val colors =
        listOf(
            android.graphics.Color.BLACK to Color.Black,
            android.graphics.Color.WHITE to Color.White,
            android.graphics.Color.RED to Color.Red,
        )
    val selectedColor = colors.firstOrNull { it.first == color }?.second ?: Color.Black
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (saved) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Text(
                    text = context.getString(R.string.stroke_saved),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        val presetLabels =
            listOf(
                R.string.stroke_thin,
                R.string.stroke_medium,
                R.string.stroke_thick,
                R.string.stroke_heavy,
            )
        val dotSizes = listOf(6.dp, 10.dp, 16.dp, 22.dp)
        val selected = nearestStrokePreset(thickness)
        Row(verticalAlignment = Alignment.CenterVertically) {
            STROKE_PRESETS.forEachIndexed { index, width ->
                val chosen = kotlin.math.abs(selected - width) < 0.0001f
                Box(
                    modifier =
                        Modifier
                            .padding(end = 4.dp)
                            .size(36.dp)
                            .semantics { contentDescription = context.getString(presetLabels[index]) }
                            .clickable {
                                onThicknessChange(width)
                                onCommit()
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(dotSizes[index])
                                .clip(CircleShape)
                                .background(selectedColor)
                                .border(
                                    width = if (chosen) 2.dp else 1.dp,
                                    color =
                                        if (chosen) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    shape = CircleShape,
                                ),
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            colors.forEach { (argb, composeColor) ->
                val selected = color == argb
                Box(
                    modifier =
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(composeColor)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color =
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                shape = CircleShape,
                            )
                            .clickable {
                                onColorChange(argb)
                                onCommit()
                            },
                )
            }
        }
    }
}

private fun drawAnnotations(
    base: Bitmap,
    annotations: List<PdfAnnotation>,
    strokes: List<PdfStroke>,
    highlightIndex: Int? = null,
): Bitmap {
    val out = base.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(out)
    val items =
        buildList {
            annotations.forEachIndexed { index, ann -> add(DrawItem.Text(index, ann, ann.zOrder)) }
            strokes.forEach { add(DrawItem.Stroke(it, it.zOrder)) }
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
                        strokeWidth = stroke.widthFraction * out.height
                        style = android.graphics.Paint.Style.STROKE
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
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
    if (maxWidthPx <= 0) return listOf(text)
    val lines = mutableListOf<String>()
    var current = StringBuilder()
    text.split(" ").forEach { word ->
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
