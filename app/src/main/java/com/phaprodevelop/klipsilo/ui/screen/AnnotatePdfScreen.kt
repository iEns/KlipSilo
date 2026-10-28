package com.phaprodevelop.klipsilo.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.MarkupHistory
import com.phaprodevelop.klipsilo.data.TextCommitDecision
import com.phaprodevelop.klipsilo.data.db.ContentType
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
import com.phaprodevelop.klipsilo.data.repository.PdfImageAnnotation
import com.phaprodevelop.klipsilo.data.repository.PdfStroke
import com.phaprodevelop.klipsilo.data.resolveTextCommit
import com.phaprodevelop.klipsilo.data.settings.AnnotateSettings
import com.phaprodevelop.klipsilo.data.settings.AnnotateSettingsStore
import com.phaprodevelop.klipsilo.ui.components.BitmapUseCounter
import com.phaprodevelop.klipsilo.ui.components.ButtonLabel
import com.phaprodevelop.klipsilo.ui.components.DialogTitleLabel
import com.phaprodevelop.klipsilo.ui.components.FitTextPolicy
import com.phaprodevelop.klipsilo.ui.components.FitTopBar
import com.phaprodevelop.klipsilo.ui.components.PageSelector
import com.phaprodevelop.klipsilo.ui.components.TOOL_LABEL_MIN_SCALE
import com.phaprodevelop.klipsilo.ui.components.clampFittedPan
import com.phaprodevelop.klipsilo.ui.components.coveringPanLayer
import com.phaprodevelop.klipsilo.ui.components.decodeImageBitmap
import com.phaprodevelop.klipsilo.ui.components.fitTextSemantics
import com.phaprodevelop.klipsilo.ui.components.pdfPageCount
import com.phaprodevelop.klipsilo.ui.components.pdfPageHeightPt
import com.phaprodevelop.klipsilo.ui.components.previewPageRect
import com.phaprodevelop.klipsilo.ui.components.produceLatest
import com.phaprodevelop.klipsilo.ui.components.rememberDisplayedBitmap
import com.phaprodevelop.klipsilo.ui.components.rememberUniformLabelDecisions
import com.phaprodevelop.klipsilo.ui.components.renderPdfPage
import com.phaprodevelop.klipsilo.ui.components.revealOffsetY
import com.phaprodevelop.klipsilo.ui.components.scaledLabelStyle
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel
import com.phaprodevelop.klipsilo.viewmodel.MarkupSession
import com.phaprodevelop.klipsilo.withoutSoftHyphens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.roundToInt

private data class MarkupSnapshot(
    val annotations: Map<Int, List<PdfAnnotation>>,
    val strokes: Map<Int, List<PdfStroke>>,
    val images: Map<Int, List<PdfImageAnnotation>> = emptyMap(),
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
    IMAGE,
}

private sealed class DrawItem(val z: Long) {
    class Text(val index: Int, val ann: PdfAnnotation, z: Long) : DrawItem(z)

    class Stroke(val index: Int, val stroke: PdfStroke, z: Long) : DrawItem(z)

    class Image(val index: Int, val image: PdfImageAnnotation, z: Long) : DrawItem(z)
}

/**
 * Pending caret refinement (#55b): the tap, as fractions of the canvas text estimate, plus the
 * draft text it belongs to. Applied through the text field's own layout at its first render.
 */
private class PendingCaret(
    val forText: String,
    val xFraction: Float,
    val yFraction: Float,
)

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
    var leaving by remember { mutableStateOf(false) }
    LaunchedEffect(navigateBack) {
        if (navigateBack) {
            leaving = true
            viewModel.clearMarkupSession(entryId)
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

    // Draft markup is held by the activity-scoped ViewModel so it survives a configuration
    // change (e.g. an app language change recreating the activity) (#85).
    val restoredSession = remember(entryId) { viewModel.markupSessionOrNull(entryId) }
    var pageIndex by remember(entryId) { mutableIntStateOf(restoredSession?.pageIndex ?: 0) }
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
    // Caret refinement (#55b): the tap position as a fraction of the canvas text
    // estimate, applied through the text field's own layout once it first renders
    // (the pre-layout estimate can still be one character off).
    var draftPendingCaret by remember { mutableStateOf<PendingCaret?>(null) }
    val annotationMode = tool == MarkupTool.TEXT
    val drawMode = tool == MarkupTool.LINE || tool == MarkupTool.DRAW || tool == MarkupTool.HIGHLIGHT
    val straightMode = tool == MarkupTool.LINE
    val focusRequester = remember { FocusRequester() }
    val strokes =
        remember(entryId) {
            mutableStateMapOf<Int, List<PdfStroke>>().apply {
                restoredSession?.strokes?.forEach { (page, items) -> this[page] = items }
            }
        }
    val images =
        remember(entryId) {
            mutableStateMapOf<Int, List<PdfImageAnnotation>>().apply {
                restoredSession?.images?.forEach { (page, items) -> this[page] = items }
            }
        }
    // Decoded once per image path; the preview must not decode on every frame.
    val imageBitmaps = remember { mutableStateMapOf<String, Bitmap>() }
    var draftStroke by remember { mutableStateOf<PdfStroke?>(null) }
    val savedAnnotate = remember { AnnotateSettingsStore.load(context) }
    var strokeWidth by remember {
        mutableFloatStateOf(nearestMarkupWidth(savedAnnotate.strokeWidthFraction))
    }
    var lineColor by remember { mutableIntStateOf(savedAnnotate.lineColor) }
    var drawColor by remember { mutableIntStateOf(savedAnnotate.drawColor) }
    var highlightColor by remember { mutableIntStateOf(savedAnnotate.highlightColor) }
    var textSizeFraction by remember {
        mutableFloatStateOf(nearestMarkupTextSize(savedAnnotate.textSizeFraction))
    }
    var textColor by remember { mutableIntStateOf(savedAnnotate.textColor) }
    var zSeq by remember(entryId) {
        mutableStateOf(
            (
                restoredSession?.annotations?.values?.flatten().orEmpty().map { it.zOrder } +
                    restoredSession?.strokes?.values?.flatten().orEmpty().map { it.zOrder } +
                    restoredSession?.images?.values?.flatten().orEmpty().map { it.zOrder }
            ).maxOrNull() ?: 0L,
        )
    }

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
                lineColor = lineColor,
                drawColor = drawColor,
                highlightColor = highlightColor,
            ),
        )
    }

    fun strokeToolFor(stroke: PdfStroke): MarkupTool =
        when {
            stroke.highlight -> MarkupTool.HIGHLIGHT
            stroke.straight -> MarkupTool.LINE
            else -> MarkupTool.DRAW
        }

    fun strokeColorFor(activeTool: MarkupTool): Int =
        when (activeTool) {
            MarkupTool.LINE -> lineColor
            MarkupTool.HIGHLIGHT -> highlightColor
            else -> drawColor
        }

    fun setStrokeColorFor(
        activeTool: MarkupTool,
        color: Int,
    ) {
        when (activeTool) {
            MarkupTool.LINE -> lineColor = color
            MarkupTool.HIGHLIGHT -> highlightColor = color
            else -> drawColor = color
        }
    }
    var movingIndex by remember { mutableIntStateOf(-1) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var selectedStroke by remember { mutableIntStateOf(-1) }
    var selectedImage by remember { mutableIntStateOf(-1) }
    var showImageSources by remember { mutableStateOf(false) }
    var showImageClips by remember { mutableStateOf(false) }
    var pageSize by remember { mutableStateOf(IntSize.Zero) }
    var showExitWarning by remember { mutableStateOf(false) }
    val annotations =
        remember(entryId) {
            mutableStateMapOf<Int, List<PdfAnnotation>>().apply {
                restoredSession?.annotations?.forEach { (page, items) -> this[page] = items }
            }
        }
    var pageHeightPt by remember { mutableFloatStateOf(0f) }
    var savedAnnotations by remember(entryId) {
        mutableStateOf(restoredSession?.savedAnnotations ?: emptyList())
    }
    var savedStrokes by remember(entryId) {
        mutableStateOf(restoredSession?.savedStrokes ?: emptyList())
    }
    var savedImages by remember(entryId) {
        mutableStateOf(restoredSession?.savedImages ?: emptyList())
    }

    // Persist the draft session on every markup/page change so a recreate can rehydrate it.
    val persistedAnnotations = annotations.mapValues { it.value.toList() }
    val persistedStrokes = strokes.mapValues { it.value.toList() }
    val persistedImages = images.mapValues { it.value.toList() }
    LaunchedEffect(
        entryId,
        leaving,
        persistedAnnotations,
        persistedStrokes,
        persistedImages,
        savedAnnotations,
        savedStrokes,
        savedImages,
        pageIndex,
    ) {
        if (leaving) return@LaunchedEffect
        viewModel.updateMarkupSession(
            MarkupSession(
                entryId = entryId,
                annotations = persistedAnnotations,
                strokes = persistedStrokes,
                images = persistedImages,
                savedAnnotations = savedAnnotations,
                savedStrokes = savedStrokes,
                savedImages = savedImages,
                pageIndex = pageIndex,
            ),
        )
    }

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
            images.values.flatten() != savedImages ||
            draftDirty ||
            draftStroke != null
    val markupHistory = remember { MarkupHistory<MarkupSnapshot>() }
    var historyTick by remember { mutableIntStateOf(0) }

    fun markupSnapshot(): MarkupSnapshot =
        MarkupSnapshot(
            annotations = annotations.mapValues { it.value.toList() },
            strokes = strokes.mapValues { it.value.toList() },
            images = images.mapValues { it.value.toList() },
        )

    fun applyMarkupSnapshot(snapshot: MarkupSnapshot) {
        annotations.clear()
        snapshot.annotations.forEach { (page, items) -> annotations[page] = items }
        strokes.clear()
        snapshot.strokes.forEach { (page, items) -> strokes[page] = items }
        images.clear()
        snapshot.images.forEach { (page, items) -> images[page] = items }
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
            selectedImage = -1
            historyTick++
            return
        }
        val pendingDraft = draft
        val draftWouldChangeMarkup =
            pendingDraft != null &&
                when (draftCommitDecision(pendingDraft)) {
                    TextCommitDecision.Keep, TextCommitDecision.Drop -> false
                    else -> true
                }
        if (pendingStroke != null || draftWouldChangeMarkup) {
            draftStroke = null
            draft = null
            selectedIndex = -1
            selectedStroke = -1
            selectedImage = -1
            historyTick++
            return
        }
        // A draft that would change nothing (#50/#55: a new empty box, an unchanged edit) must
        // not consume the undo press (#55): drop it and undo the last recorded step in one go.
        draftStroke = null
        draft = null
        selectedIndex = -1
        selectedStroke = -1
        selectedImage = -1
        val restored = markupHistory.undo(markupSnapshot()) ?: return
        applyMarkupSnapshot(restored)
        historyTick++
    }

    fun redoMarkup() {
        val restored = markupHistory.redo(markupSnapshot()) ?: return
        applyMarkupSnapshot(restored)
        draft = null
        draftStroke = null
        selectedIndex = -1
        selectedStroke = -1
        selectedImage = -1
        historyTick++
    }

    fun beginDraft(
        next: DraftAnnotation,
        caret: Int = next.text.length,
        tapTextFraction: Pair<Float, Float>? = null,
    ) {
        draft = next
        // Caret where the user tapped (#55), default end of text.
        draftField = TextFieldValue(next.text, TextRange(caret.coerceIn(0, next.text.length)))
        draftFieldLayout = null
        draftFieldSize = IntSize.Zero
        draftEpoch += 1
        // The estimate from the canvas metrics is refined by the text field's own
        // layout as soon as it renders (#55b); nothing to refine for a new box.
        draftPendingCaret =
            tapTextFraction?.let { PendingCaret(next.text, it.first, it.second) }
    }

    /**
     * Applies a pending caret refinement (#55b): the tap is mapped through the text field's own
     * layout with [androidx.compose.ui.text.TextLayoutResult.getOffsetForPosition], which puts
     * the caret at the nearest glyph boundary (horizontal midpoints) with the field's real line
     * wrapping. Uses the laid-out line widths rather than the field box, because the box also
     * holds the caret overhang (#153) and starts at the wrap cap for existing text. Skipped when
     * the text changed, the text block is empty or nothing is pending.
     */
    fun refineDraftCaret(layout: TextLayoutResult) {
        val pending = draftPendingCaret ?: return
        val textWidth =
            (0 until layout.lineCount).maxOfOrNull { line ->
                layout.getLineRight(line) - layout.getLineLeft(line)
            } ?: 0f
        val textHeight = layout.size.height.toFloat()
        if (textWidth <= 0f || textHeight <= 0f) return
        draftPendingCaret = null
        if (pending.forText != draftField.text) return
        val position =
            localCaretPosition(
                xFraction = pending.xFraction,
                yFraction = pending.yFraction,
                widthPx = textWidth,
                heightPx = textHeight,
            )
        val offset = layout.getOffsetForPosition(position).coerceIn(0, draftField.text.length)
        draftField = draftField.copy(selection = TextRange(offset))
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
            return
        }
        if (selectedImage >= 0) {
            recordMarkup()
            val list = images[pageIndex].orEmpty().toMutableList()
            if (selectedImage in list.indices) {
                list.removeAt(selectedImage)
                images[pageIndex] = list
            }
            selectedImage = -1
        }
    }

    /**
     * Places [sourcePath] (a copy in app storage) centred on the current page (#92). The
     * default width is 40% of the page, shrunk when a tall image would exceed 60% of the
     * page height. Width and height fractions stay aspect-locked from here on.
     */
    fun placeImage(sourcePath: String) {
        val dims = orientedImageDimensions(sourcePath)
        if (dims == null) {
            Toast.makeText(context, R.string.markup_image_import_failed, Toast.LENGTH_SHORT).show()
            return
        }
        val pageAspect =
            baseBitmap
                ?.takeIf { it.height > 0 }
                ?.let { it.width.toFloat() / it.height }
                ?: (612f / 792f)
        var widthFraction = 0.4f
        val ratio = pageAspect * dims.second / dims.first
        var heightFraction = widthFraction * ratio
        if (heightFraction > 0.6f) {
            heightFraction = 0.6f
            widthFraction = heightFraction / ratio
        }
        if (widthFraction > 0.9f) {
            widthFraction = 0.9f
            heightFraction = widthFraction * ratio
        }
        val x = ((1f - widthFraction) / 2f).coerceIn(0f, 1f - widthFraction)
        val y = ((1f - heightFraction) / 2f).coerceIn(0f, 1f - heightFraction)
        recordMarkup()
        val list = images[pageIndex].orEmpty().toMutableList()
        list.add(
            PdfImageAnnotation(
                pageIndex = pageIndex,
                xFraction = x,
                yFraction = y,
                widthFraction = widthFraction,
                heightFraction = heightFraction,
                imagePath = sourcePath,
                zOrder = nextZ(),
            ),
        )
        images[pageIndex] = list
        selectedImage = list.lastIndex
        selectedIndex = -1
        selectedStroke = -1
        tool = MarkupTool.SELECT
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
        val activeTool =
            when {
                tool == MarkupTool.LINE || tool == MarkupTool.DRAW || tool == MarkupTool.HIGHLIGHT -> tool
                else -> strokes[pageIndex].orEmpty().getOrNull(selectedStroke)?.let(::strokeToolFor) ?: MarkupTool.DRAW
            }
        setStrokeColorFor(activeTool, color)
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

    fun leave() {
        if (dirty) {
            showExitWarning = true
        } else {
            leaving = true
            viewModel.clearMarkupSession(entryId)
            onNavigateBack()
        }
    }

    BackHandler { leave() }

    // Image insertion sources (#92): every source ends up as a copy in app storage before
    // it is placed, so Save still works after a gallery/FileProvider URI is revoked.
    val imageScope = rememberCoroutineScope()
    var pendingCameraPath by remember { mutableStateOf<String?>(null) }

    fun importImageUri(uri: Uri) {
        imageScope.launch {
            val path = viewModel.importMarkupImage(uri)
            if (path != null) {
                placeImage(path)
            } else {
                Toast.makeText(context, R.string.markup_image_import_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun importImageFile(path: String) {
        imageScope.launch {
            val copied = viewModel.importMarkupImageFile(path)
            if (copied != null) {
                placeImage(copied)
            } else {
                Toast.makeText(context, R.string.markup_image_import_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) importImageUri(uri)
        }
    val imageFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) importImageUri(uri)
        }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val path = pendingCameraPath
            pendingCameraPath = null
            if (success && path != null) importImageFile(path)
        }

    fun startCameraCapture() {
        val dir = File(context.cacheDir, "markup").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCameraPath = file.absolutePath
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCameraCapture()
            } else {
                Toast.makeText(context, R.string.markup_image_camera_denied, Toast.LENGTH_SHORT).show()
            }
        }

    fun openCameraSource() {
        val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) {
            startCameraCapture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
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
            selectedIndex = -1
            selectedStroke = -1
            selectedImage = -1
        }
    }

    val currentAnnotations = annotations[pageIndex].orEmpty()
    val currentStrokes = strokes[pageIndex].orEmpty()
    val currentImages = images[pageIndex].orEmpty()
    // Snapshot for the IO draw; reading the state map here also recomposes when a decode lands.
    val imageBitmapSnapshot = imageBitmaps.toMap()
    val currentImagePaths = currentImages.map { it.imagePath }.distinct()
    LaunchedEffect(currentImagePaths) {
        currentImagePaths.forEach { path ->
            if (path !in imageBitmaps) {
                val bitmap =
                    withContext(Dispatchers.IO) {
                        decodeImageBitmap(path, MARKUP_IMAGE_PREVIEW_MAX_PX, MARKUP_IMAGE_PREVIEW_MAX_PX)
                    }
                if (bitmap != null) imageBitmaps[path] = bitmap
            }
        }
    }
    val draftOpen = draft != null
    val draftEditingIndex = draft?.editingIndex
    LaunchedEffect(
        baseBitmap,
        currentAnnotations,
        currentStrokes,
        currentImages,
        imageBitmapSnapshot,
        // The draft is not drawn into the page while its field is open (#138), so typing
        // only needs the field itself to recompose. The page is redrawn when the draft
        // opens/closes or when it hides/reveals the annotation being edited.
        draftOpen,
        draftEditingIndex,
        draftStroke,
        pageIndex,
        movingIndex,
        selectedIndex,
        selectedStroke,
        selectedImage,
    ) {
        val base = baseBitmap ?: return@LaunchedEffect
        baseUse.acquire(base)
        try {
            val shown = currentAnnotations.toMutableList()
            if (draftEditingIndex != null && draftEditingIndex < shown.size) {
                shown.removeAt(draftEditingIndex)
            }
            // No draft is added to the page: the text field is the preview while editing
            // (#138). Drawing the draft here produced a clipped one-line copy under a
            // field that word-wraps, which looked broken.
            val highlight =
                when {
                    movingIndex >= 0 -> movingIndex
                    draftOpen -> null
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
                            drawAnnotations(
                                base,
                                shown,
                                shownStrokes,
                                currentImages,
                                imageBitmapSnapshot,
                                highlight,
                                selectedStroke,
                                selectedImage,
                            )
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
    // While the soft keyboard is up for the Text tool (or an active text draft), the canvas is
    // shorter. Fit the page to the canvas width instead of shrinking it to fit the short
    // height, so the text being typed stays readable (#161).
    val fitWidthForIme = imeBottom > 0 && (tool == MarkupTool.TEXT || draft != null)
    var preImeOffsetX by remember { mutableFloatStateOf(0f) }
    var preImeOffsetY by remember { mutableFloatStateOf(0f) }
    var preImeScale by remember { mutableFloatStateOf(1f) }
    var imeFitActive by remember { mutableStateOf(false) }
    LaunchedEffect(fitWidthForIme) {
        if (fitWidthForIme) {
            if (!imeFitActive) {
                preImeOffsetX = offsetX
                preImeOffsetY = offsetY
                preImeScale = scale
                imeFitActive = true
            }
        } else if (imeFitActive) {
            // Keyboard dismissed or the Text tool left: go back to the pre-IME pan instead of
            // keeping the fit-width pan (which would leave the page pushed off screen, #161).
            imeFitActive = false
            val keepScale = scale == preImeScale
            val wantX = if (keepScale) preImeOffsetX else offsetX
            val wantY = if (keepScale) preImeOffsetY else offsetY
            val bmp = baseBitmap
            if (bmp != null && pageSize.width > 0 && pageSize.height > 0) {
                val restored =
                    clampFittedPan(
                        wantX,
                        wantY,
                        scale,
                        pageSize.width.toFloat(),
                        pageSize.height.toFloat(),
                        bmp.width.toFloat(),
                        bmp.height.toFloat(),
                    )
                offsetX = restored.offsetX
                offsetY = restored.offsetY
            } else {
                offsetX = wantX
                offsetY = wantY
            }
        }
    }
    val keyboardDraftY = draft?.yFraction
    LaunchedEffect(draftEpoch, imeBottom, pageSize, baseBitmap, draftFieldSize, fitWidthForIme) {
        val yFraction = keyboardDraftY ?: return@LaunchedEffect
        val bmp = baseBitmap ?: return@LaunchedEffect
        if (!fitWidthForIme || pageSize.height == 0) return@LaunchedEffect
        // Fit-width geometry: the page may be taller than the canvas, so pan it until the
        // whole text field sits above the keyboard (#161).
        val rect = fittedRect(pageSize, bmp.width, bmp.height, fitWidth = true)
        val yOnPage = rect.top + yFraction * rect.height
        val margin = with(density) { 16.dp.toPx() }
        val s = if (scale > 0f) scale else 1f
        offsetY =
            revealOffsetY(
                currentOffsetY = offsetY,
                targetTop = yOnPage,
                targetBottom = yOnPage + draftFieldSize.height,
                scale = s,
                viewportHeight = pageSize.height.toFloat(),
                scaledPageHeight = rect.height * s,
                topMargin = margin,
                bottomMargin = margin,
            )
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
            FitTopBar(
                title = context.getString(R.string.annotate_title),
                saveLabel = context.getString(R.string.save),
                saveEnabled = entry.filePath != null,
                onBack = { leave() },
                onSave = {
                    if (dirty) {
                        commitDrafts()
                        val all = annotations.values.flatten()
                        val allStrokes = strokes.values.flatten()
                        val allImages = images.values.flatten()
                        viewModel.saveAnnotatedPdf(entry, all, allStrokes, allImages)
                    } else {
                        // Save with no changes (#60): just leave, like Back, instead
                        // of creating an identical copy clip.
                        leaving = true
                        viewModel.clearMarkupSession(entryId)
                        onNavigateBack()
                    }
                },
                backContentDescription = context.getString(R.string.back),
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
                canDelete =
                    selectedIndex >= 0 ||
                        selectedStroke >= 0 ||
                        selectedImage >= 0 ||
                        draft?.editingIndex != null,
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
            // Page-vs-canvas chrome (#146): a soft tonal wash behind the page keeps a
            // white sheet from blending into a white canvas when zoomed out.
            val scheme = MaterialTheme.colorScheme
            val canvasBrush =
                Brush.verticalGradient(
                    listOf(
                        lerp(scheme.surface, scheme.surfaceVariant, 0.7f),
                        lerp(scheme.surface, scheme.surfaceVariant, 0.3f),
                        lerp(scheme.surface, scheme.surfaceVariant, 0.7f),
                    ),
                )
            val darkCanvas = scheme.surface.luminance() < 0.5f
            val pageShadowColor =
                scheme.onSurface.copy(alpha = if (darkCanvas) 0.18f else 0.3f)
            val pageFrameColor =
                scheme.outline.copy(alpha = if (darkCanvas) 0.55f else 0.45f)
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(canvasBrush)
                        .clipToBounds()
                        .onSizeChanged { pageSize = it },
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .pointerInput(tool, selectedIndex, baseBitmap, pageIndex, fitWidthForIme) {
                                val touchSlop = viewConfiguration.touchSlop
                                if (annotationMode || tool == MarkupTool.SELECT) {
                                    var lastTapUpTime = 0L
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val downPos = down.position
                                        var targetIndex = -1
                                        var targetIsDraft = false
                                        var targetStroke = -1
                                        var targetImage = -1
                                        var resizeCorner: ImageCorner? = null
                                        var resizeStart: PdfImageAnnotation? = null
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
                                                    fitWidthForIme,
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
                                            val imageIdx =
                                                if (tool == MarkupTool.SELECT) {
                                                    images[pageIndex].orEmpty().indexOfLast {
                                                        imageHitTest(fx, fy, it)
                                                    }
                                                } else {
                                                    -1
                                                }
                                            // A corner handle sticks out of the image, so a tap just
                                            // outside a selected image's corner still grabs it.
                                            val handleImageIdx =
                                                if (
                                                    imageIdx < 0 &&
                                                    tool == MarkupTool.SELECT &&
                                                    selectedImage >= 0
                                                ) {
                                                    val selected =
                                                        images[pageIndex].orEmpty().getOrNull(selectedImage)
                                                    if (selected != null) {
                                                        val handleRect =
                                                            imageScreenRect(
                                                                selected,
                                                                fittedRect(size, bmp.width, bmp.height, fitWidthForIme),
                                                                size,
                                                                scale,
                                                                offsetX,
                                                                offsetY,
                                                            )
                                                        if (
                                                            nearestImageCorner(
                                                                handleRect,
                                                                downPos,
                                                                with(density) { 28.dp.toPx() },
                                                            ) != null
                                                        ) {
                                                            selectedImage
                                                        } else {
                                                            -1
                                                        }
                                                    } else {
                                                        -1
                                                    }
                                                } else {
                                                    -1
                                                }
                                            val hitImage = if (imageIdx >= 0) imageIdx else handleImageIdx
                                            // Images participate in the same z-order as text, so
                                            // tapping where both overlap selects what is on top.
                                            if (idx >= 0 && hitImage >= 0) {
                                                val textZ =
                                                    annotations[pageIndex].orEmpty()[idx].zOrder
                                                val imageZ =
                                                    images[pageIndex].orEmpty()[hitImage].zOrder
                                                if (imageZ > textZ) {
                                                    targetImage = hitImage
                                                } else {
                                                    targetIndex = idx
                                                }
                                                targetIsDraft = false
                                                selectedStroke = -1
                                            } else if (idx >= 0) {
                                                targetIndex = idx
                                                targetIsDraft = false
                                                selectedStroke = -1
                                            } else if (hitImage >= 0) {
                                                targetImage = hitImage
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
                                            // A corner of the already selected image starts an
                                            // aspect-locked resize instead of a body move.
                                            if (targetImage >= 0 && targetImage == selectedImage) {
                                                val image =
                                                    images[pageIndex].orEmpty().getOrNull(targetImage)
                                                if (image != null) {
                                                    val fitted = fittedRect(size, bmp.width, bmp.height, fitWidthForIme)
                                                    val screenRect =
                                                        imageScreenRect(
                                                            image,
                                                            fitted,
                                                            size,
                                                            scale,
                                                            offsetX,
                                                            offsetY,
                                                        )
                                                    resizeCorner =
                                                        nearestImageCorner(
                                                            screenRect,
                                                            downPos,
                                                            with(density) { 28.dp.toPx() },
                                                        )
                                                    if (resizeCorner != null) resizeStart = image
                                                }
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
                                                        fitWidthForIme,
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
                                                            val pageBmp = bmp ?: return@awaitEachGesture
                                                            if (d != null) {
                                                                val fitted =
                                                                    fittedRect(
                                                                        size,
                                                                        pageBmp.width,
                                                                        pageBmp.height,
                                                                        fitWidthForIme,
                                                                    )
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
                                                            // falling back to end of text, then
                                                            // refined by the field's own layout
                                                            // once it renders (#55b).
                                                            val fraction =
                                                                bmp?.let { pageBmp ->
                                                                    val basis =
                                                                        annotationCanvasLines(
                                                                            a,
                                                                            pageBmp.width,
                                                                            pageBmp.height,
                                                                        ) ?: return@let null
                                                                    val textWidthPx =
                                                                        basis.lines
                                                                            .maxOfOrNull(basis.paint::measureText)
                                                                            ?: 0f
                                                                    val textHeightPx =
                                                                        basis.lines.size * basis.paint.fontSpacing
                                                                    caretFractionInText(
                                                                        fx = fx,
                                                                        fy = fy,
                                                                        annotationXFraction = a.xFraction,
                                                                        annotationYFraction = a.yFraction,
                                                                        textWidthPx = textWidthPx,
                                                                        textHeightPx = textHeightPx,
                                                                        bitmapWidth = pageBmp.width,
                                                                        bitmapHeight = pageBmp.height,
                                                                    )
                                                                }
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
                                                                fraction,
                                                            )
                                                        }
                                                        lastTapUpTime = 0L
                                                    } else if (targetOnText && !targetIsDraft) {
                                                        if (draft != null) commitDrafts()
                                                        selectedIndex = targetIndex
                                                        selectedStroke = -1
                                                        selectedImage = -1
                                                        lastTapUpTime = now
                                                    } else if (targetImage >= 0) {
                                                        if (draft != null) commitDrafts()
                                                        selectedImage = targetImage
                                                        selectedIndex = -1
                                                        selectedStroke = -1
                                                        lastTapUpTime = 0L
                                                    } else if (targetStroke >= 0 && tool == MarkupTool.SELECT) {
                                                        if (draft != null) commitDrafts()
                                                        selectedStroke = targetStroke
                                                        selectedIndex = -1
                                                        selectedImage = -1
                                                        lastTapUpTime = 0L
                                                    } else if (tool == MarkupTool.TEXT && !targetOnText) {
                                                        if (draft != null) commitDrafts()
                                                        selectedIndex = -1
                                                        selectedStroke = -1
                                                        selectedImage = -1
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
                                                        selectedImage = -1
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
                                                    val nextScale = (scale * zoom).coerceIn(1f, 8f)
                                                    scale = nextScale
                                                    lastDist = dist
                                                    val cDelta = centroid - lastCentroid
                                                    if ((nextScale > 1f || fitWidthForIme) && bmp != null) {
                                                        val clamped =
                                                            clampFittedPan(
                                                                offsetX + cDelta.x,
                                                                offsetY + cDelta.y,
                                                                nextScale,
                                                                size.width.toFloat(),
                                                                size.height.toFloat(),
                                                                bmp.width.toFloat(),
                                                                bmp.height.toFloat(),
                                                                fitWidthForIme,
                                                            )
                                                        offsetX = clamped.offsetX
                                                        offsetY = clamped.offsetY
                                                    } else if (nextScale <= 1f) {
                                                        offsetX = 0f
                                                        offsetY = 0f
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
                                                        if (!targetIsDraft) {
                                                            recordMarkup()
                                                            val list = annotations[pageIndex].orEmpty().toMutableList()
                                                            if (targetIndex in list.indices) {
                                                                list[targetIndex] =
                                                                    list[targetIndex].copy(zOrder = nextZ())
                                                                annotations[pageIndex] = list
                                                            }
                                                        }
                                                    } else if (targetImage >= 0 && tool == MarkupTool.SELECT) {
                                                        recordMarkup()
                                                        selectedImage = targetImage
                                                        selectedIndex = -1
                                                        selectedStroke = -1
                                                        val list = images[pageIndex].orEmpty().toMutableList()
                                                        if (targetImage in list.indices) {
                                                            list[targetImage] =
                                                                list[targetImage].copy(zOrder = nextZ())
                                                            images[pageIndex] = list
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
                                                    val rect =
                                                        fittedRect(size, curBmp.width, curBmp.height, fitWidthForIme)
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
                                                } else if (targetImage >= 0 && tool == MarkupTool.SELECT) {
                                                    val curBmp = baseBitmap ?: break
                                                    val rect =
                                                        fittedRect(size, curBmp.width, curBmp.height, fitWidthForIme)
                                                    if (rect.width > 0f && rect.height > 0f && scale > 0f) {
                                                        val list = images[pageIndex].orEmpty().toMutableList()
                                                        if (targetImage in list.indices) {
                                                            val current = list[targetImage]
                                                            val start = resizeStart
                                                            val corner = resizeCorner
                                                            if (start != null && corner != null) {
                                                                val totalDx =
                                                                    (pos.x - downPos.x) /
                                                                        (scale * rect.width)
                                                                val totalDy =
                                                                    (pos.y - downPos.y) /
                                                                        (scale * rect.height)
                                                                val resized =
                                                                    resizeImageFromCorner(
                                                                        startX = start.xFraction,
                                                                        startY = start.yFraction,
                                                                        startWidth = start.widthFraction,
                                                                        startHeight = start.heightFraction,
                                                                        left = corner.left,
                                                                        top = corner.top,
                                                                        dragDx = totalDx,
                                                                        dragDy = totalDy,
                                                                    )
                                                                if (resized != null) {
                                                                    list[targetImage] =
                                                                        current.copy(
                                                                            xFraction = resized.xFraction,
                                                                            yFraction = resized.yFraction,
                                                                            widthFraction = resized.widthFraction,
                                                                            heightFraction = resized.heightFraction,
                                                                        )
                                                                }
                                                            } else {
                                                                val dfx = delta.x / (scale * rect.width)
                                                                val dfy = delta.y / (scale * rect.height)
                                                                list[targetImage] =
                                                                    current.copy(
                                                                        xFraction =
                                                                            (current.xFraction + dfx).coerceIn(
                                                                                0f,
                                                                                (1f - current.widthFraction)
                                                                                    .coerceAtLeast(0f),
                                                                            ),
                                                                        yFraction =
                                                                            (current.yFraction + dfy).coerceIn(
                                                                                0f,
                                                                                (1f - current.heightFraction)
                                                                                    .coerceAtLeast(0f),
                                                                            ),
                                                                    )
                                                            }
                                                            images[pageIndex] = list
                                                        }
                                                    }
                                                } else if (targetStroke >= 0 && tool == MarkupTool.SELECT) {
                                                    val curBmp = baseBitmap ?: break
                                                    val rect =
                                                        fittedRect(size, curBmp.width, curBmp.height, fitWidthForIme)
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
                                                } else if (scale > 1f || fitWidthForIme) {
                                                    if (bmp != null) {
                                                        val clamped =
                                                            clampFittedPan(
                                                                offsetX + delta.x,
                                                                offsetY + delta.y,
                                                                scale,
                                                                size.width.toFloat(),
                                                                size.height.toFloat(),
                                                                bmp.width.toFloat(),
                                                                bmp.height.toFloat(),
                                                                fitWidthForIme,
                                                            )
                                                        offsetX = clamped.offsetX
                                                        offsetY = clamped.offsetY
                                                    }
                                                } else {
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                }
                                                change.consume()
                                            }
                                        }
                                        movingIndex = -1
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
                                                    val nextScale = (scale * zoom).coerceIn(1f, 8f)
                                                    scale = nextScale
                                                    lastDist = dist
                                                    val cDelta = centroid - lastCentroid
                                                    if ((nextScale > 1f || fitWidthForIme) && bmp != null) {
                                                        val clamped =
                                                            clampFittedPan(
                                                                offsetX + cDelta.x,
                                                                offsetY + cDelta.y,
                                                                nextScale,
                                                                size.width.toFloat(),
                                                                size.height.toFloat(),
                                                                bmp.width.toFloat(),
                                                                bmp.height.toFloat(),
                                                                fitWidthForIme,
                                                            )
                                                        offsetX = clamped.offsetX
                                                        offsetY = clamped.offsetY
                                                    } else if (nextScale <= 1f) {
                                                        offsetX = 0f
                                                        offsetY = 0f
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
                                                        fitWidthForIme,
                                                    )
                                                if (!started) {
                                                    started = true
                                                    draftStroke =
                                                        PdfStroke(
                                                            pageIndex = pageIndex,
                                                            points = listOf(mapped),
                                                            widthFraction = strokeWidth,
                                                            color = strokeColorFor(tool),
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
                                        val nextScale = (scale * zoom).coerceIn(1f, 8f)
                                        scale = nextScale
                                        if (nextScale > 1f || fitWidthForIme) {
                                            val pageBmp = baseBitmap
                                            if (pageBmp != null) {
                                                val clamped =
                                                    clampFittedPan(
                                                        offsetX + pan.x,
                                                        offsetY + pan.y,
                                                        nextScale,
                                                        size.width.toFloat(),
                                                        size.height.toFloat(),
                                                        pageBmp.width.toFloat(),
                                                        pageBmp.height.toFloat(),
                                                        fitWidthForIme,
                                                    )
                                                offsetX = clamped.offsetX
                                                offsetY = clamped.offsetY
                                            }
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    }
                                }
                            },
                ) {
                    val pageBitmap = displayed.bitmap ?: baseBitmap
                    val pageRect =
                        if (pageBitmap != null && pageSize.width > 0 && pageSize.height > 0) {
                            fittedRect(pageSize, pageBitmap.width, pageBitmap.height, fitWidthForIme)
                        } else {
                            null
                        }
                    val editing = draft
                    if (pageRect != null) {
                        // The transformed layer must contain everything it draws (#161). While
                        // fit-width is active for the IME the page is taller than the canvas, and
                        // content drawn above (or below) a canvas-sized layer could not be
                        // composited when a pan moved it into view. Grow the layer around the
                        // canvas centre until it holds the whole page and the open text field.
                        // requiredSize centres that oversized layer (apparentToRealOffset).
                        // coveringPanLayer therefore does not add the same shift again:
                        // doing so left the top of the page about half an overflow above the
                        // canvas and a matching grey band under the bottom (Beta 11).
                        var contentLeft = pageRect.left
                        var contentTop = pageRect.top
                        var contentRight = pageRect.right
                        var contentBottom = pageRect.bottom
                        if (editing != null) {
                            val fieldLeft = pageRect.left + editing.xFraction * pageRect.width
                            val fieldTop = pageRect.top + editing.yFraction * pageRect.height
                            contentLeft = minOf(contentLeft, fieldLeft)
                            contentTop = minOf(contentTop, fieldTop)
                            contentRight = maxOf(contentRight, fieldLeft + draftFieldSize.width)
                            contentBottom = maxOf(contentBottom, fieldTop + draftFieldSize.height)
                        }
                        val frame =
                            coveringPanLayer(
                                containerWidth = pageSize.width.toFloat(),
                                containerHeight = pageSize.height.toFloat(),
                                pageLeft = pageRect.left,
                                pageTop = pageRect.top,
                                pageRight = pageRect.right,
                                pageBottom = pageRect.bottom,
                                panX = offsetX,
                                panY = offsetY,
                                // Leave room for the page shadow so it stays inside the layer too.
                                padding = with(density) { 8.dp.toPx() },
                                contentLeft = contentLeft,
                                contentTop = contentTop,
                                contentRight = contentRight,
                                contentBottom = contentBottom,
                            )
                        val layerRect = frame.layer
                        Box(
                            modifier =
                                Modifier
                                    .requiredSize(
                                        width = with(density) { layerRect.width.toDp() },
                                        height = with(density) { layerRect.height.toDp() },
                                    ).graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = frame.translationX,
                                        translationY = frame.translationY,
                                    ),
                        ) {
                            val pageLeft = frame.pageLeft
                            val pageTop = frame.pageTop
                            // The page occupies the fitted rect inside the (possibly larger) layer;
                            // the bitmap, shadow, frame and draft field share that geometry.
                            // placePx uses place(), not offset { }'s placeWithLayer, so the shadow's
                            // own graphicsLayer does not pick up a second copy of this position.
                            val pageChromeModifier =
                                Modifier
                                    .placePx(pageLeft, pageTop)
                                    .requiredSize(
                                        width = with(density) { pageRect.width.toDp() },
                                        height = with(density) { pageRect.height.toDp() },
                                    )
                            // Shadow sits under the page bitmap; the frame is drawn over it.
                            Box(
                                modifier =
                                    pageChromeModifier.shadow(
                                        elevation = 6.dp,
                                        shape = RectangleShape,
                                        clip = false,
                                        ambientColor = pageShadowColor,
                                        spotColor = pageShadowColor,
                                    ),
                            )
                            displayed.bitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    // The bitmap must occupy the same fitted rect as the chrome
                                    // and the draft field; a viewport-sized FillWidth image crops
                                    // the top and bottom of the page and leaves taps in grey
                                    // (#161 reopen).
                                    modifier = pageChromeModifier,
                                    contentScale = ContentScale.Fit,
                                    filterQuality = FilterQuality.High,
                                )
                            }
                            Box(
                                modifier =
                                    pageChromeModifier.border(
                                        width = 1.dp,
                                        color = pageFrameColor,
                                        shape = RectangleShape,
                                    ),
                            )
                            val bmp = baseBitmap
                            if (editing != null && bmp != null) {
                                val x = pageLeft + editing.xFraction * pageRect.width
                                val y = pageTop + editing.yFraction * pageRect.height
                                // Committed annotations wrap at 95% of the page bitmap width
                                // (wrapAnnotationText), so the edit field can never grow past
                                // that same cap (#138).
                                val maxFieldWidth = with(density) { (pageRect.width * 0.95f).toDp() }
                                // 16.dp of empty field past the last glyph is enough for a
                                // caret-placement tap without making short text look boxy.
                                val caretOverhang = 16.dp
                                // Size the field to the text (longest unwrapped line) plus the
                                // overhang after the last character, so there is still room to
                                // tap and place the caret without a huge empty box (#153).
                                // Existing text starts at the wrap cap so its first layout
                                // matches the committed wrapping; empty/short drafts keep a
                                // small minimum.
                                val measuredWidth =
                                    draftFieldLayout
                                        ?.let { layout ->
                                            with(density) { layout.multiParagraph.maxIntrinsicWidth.toDp() }
                                        }?.plus(caretOverhang)
                                        ?: if (draftField.text.isEmpty()) caretOverhang else maxFieldWidth
                                val fieldWidth =
                                    measuredWidth
                                        .coerceAtMost(maxFieldWidth)
                                        .coerceAtLeast(48.dp)
                                BasicTextField(
                                    value = draftField,
                                    onValueChange = { value ->
                                        draftField = value
                                        draft = draft?.copy(text = value.text)
                                    },
                                    onTextLayout = { layout ->
                                        draftFieldLayout = layout
                                        refineDraftCaret(layout)
                                    },
                                    modifier =
                                        Modifier
                                            .placePx(x, y)
                                            .width(fieldWidth)
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
                                            fontSize =
                                                with(density) {
                                                    (editing.sizeFraction * pageRect.height).toSp()
                                                },
                                        ),
                                    // Multi-line text (#138, also #91): Enter inserts a newline;
                                    // commit with Save or by tapping outside the field.
                                    singleLine = false,
                                )
                            }
                        }
                    }
                }
                // Floating Edit/Move/✕ chip anchored to the selection bounds (#36):
                // above the item by default, flipped below near the top edge and
                // clamped so it stays on screen, without ever covering the selection.
                if (
                    tool == MarkupTool.SELECT &&
                    draft == null &&
                    (selectedIndex >= 0 || selectedStroke >= 0 || selectedImage >= 0)
                ) {
                    val chipBmp = baseBitmap
                    val anchor =
                        if (chipBmp != null && pageSize.width > 0) {
                            val fractionBounds =
                                when {
                                    selectedIndex >= 0 ->
                                        annotations[pageIndex].orEmpty().getOrNull(selectedIndex)?.let {
                                            annotationBoundsFraction(it, chipBmp.width, chipBmp.height)
                                        }
                                    selectedImage >= 0 ->
                                        images[pageIndex].orEmpty().getOrNull(selectedImage)?.let {
                                            Rect(
                                                it.xFraction,
                                                it.yFraction,
                                                (it.xFraction + it.widthFraction).coerceAtMost(1f),
                                                (it.yFraction + it.heightFraction).coerceAtMost(1f),
                                            )
                                        }
                                    else ->
                                        strokes[pageIndex].orEmpty().getOrNull(selectedStroke)?.let {
                                            strokeBoundsFraction(it, chipBmp.width, chipBmp.height)
                                        }
                                }
                            fractionBounds?.let { bounds ->
                                val fitted = fittedRect(pageSize, chipBmp.width, chipBmp.height, fitWidthForIme)
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
                            if (selectedIndex >= 0) {
                                TextButton(onClick = {
                                    val index = selectedIndex
                                    val list = annotations[pageIndex].orEmpty()
                                    if (index in list.indices) {
                                        val item = list[index]
                                        selectedIndex = -1
                                        selectedStroke = -1
                                        selectedImage = -1
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
                            }
                            TextButton(onClick = {}) {
                                ButtonLabel(context.getString(R.string.markup_move))
                            }
                            IconButton(onClick = { deleteSelection() }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = context.getString(R.string.delete).withoutSoftHyphens(),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
            Column(
                Modifier
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
                        onColor = { applyTextColor(it) },
                    )
                } else if (showStrokeOptions) {
                    val selected = strokes[pageIndex].orEmpty().getOrNull(selectedStroke)
                    val activeStrokeTool =
                        when (tool) {
                            MarkupTool.LINE, MarkupTool.DRAW, MarkupTool.HIGHLIGHT -> tool
                            else -> selected?.let(::strokeToolFor) ?: MarkupTool.DRAW
                        }
                    val selectedColor = selected?.color?.takeIf { tool == MarkupTool.SELECT }
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
                        color = selectedColor ?: strokeColorFor(activeStrokeTool),
                        onColor = { applyStrokeColor(it) },
                    )
                }
                MarkupToolBar(tool = tool) { next ->
                    if (next == MarkupTool.IMAGE) {
                        commitDrafts()
                        showImageSources = true
                        return@MarkupToolBar
                    }
                    commitDrafts()
                    tool = next
                    if (next != MarkupTool.SELECT && next != MarkupTool.TEXT) {
                        selectedIndex = -1
                    }
                    if (next != MarkupTool.SELECT) {
                        selectedStroke = -1
                        selectedImage = -1
                    }
                }
            }
        }
    }

    if (showExitWarning) {
        AlertDialog(
            onDismissRequest = { showExitWarning = false },
            title = { DialogTitleLabel(context.getString(R.string.markup_leave_title)) },
            text = { Text(context.getString(R.string.markup_leave_message)) },
            confirmButton = {
                FlowRow {
                    TextButton(onClick = { showExitWarning = false }) {
                        ButtonLabel(context.getString(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            showExitWarning = false
                            leaving = true
                            viewModel.clearMarkupSession(entryId)
                            onNavigateBack()
                        },
                    ) {
                        ButtonLabel(context.getString(R.string.discard))
                    }
                    TextButton(
                        onClick = {
                            commitDrafts()
                            showExitWarning = false
                            val all = annotations.values.flatten()
                            val allStrokes = strokes.values.flatten()
                            val allImages = images.values.flatten()
                            viewModel.saveAnnotatedPdf(entry, all, allStrokes, allImages)
                        },
                    ) {
                        ButtonLabel(context.getString(R.string.save))
                    }
                }
            },
            dismissButton = {},
        )
    }

    if (showImageSources) {
        AlertDialog(
            onDismissRequest = { showImageSources = false },
            title = { DialogTitleLabel(context.getString(R.string.markup_image_source_title)) },
            text = {
                Column {
                    TextButton(onClick = {
                        showImageSources = false
                        showImageClips = true
                    }) {
                        ButtonLabel(context.getString(R.string.markup_image_from_clips))
                    }
                    TextButton(onClick = {
                        showImageSources = false
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }) {
                        ButtonLabel(context.getString(R.string.markup_image_from_gallery))
                    }
                    TextButton(onClick = {
                        showImageSources = false
                        openCameraSource()
                    }) {
                        ButtonLabel(context.getString(R.string.markup_image_from_camera))
                    }
                    TextButton(onClick = {
                        showImageSources = false
                        imageFileLauncher.launch(arrayOf("image/*"))
                    }) {
                        ButtonLabel(context.getString(R.string.markup_image_from_files))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageSources = false }) {
                    ButtonLabel(context.getString(R.string.cancel))
                }
            },
        )
    }

    if (showImageClips) {
        val imageClips = clips.filter { it.contentType == ContentType.IMAGE && it.filePath != null }
        AlertDialog(
            onDismissRequest = { showImageClips = false },
            title = { DialogTitleLabel(context.getString(R.string.markup_image_from_clips)) },
            text = {
                if (imageClips.isEmpty()) {
                    Text(context.getString(R.string.markup_image_no_clips))
                } else {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(imageClips, key = { it.id }) { clip ->
                            TextButton(onClick = {
                                showImageClips = false
                                clip.filePath?.let { importImageFile(it) }
                            }) {
                                Text(clip.previewText.ifBlank { context.getString(R.string.untitled_clip) })
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageClips = false }) {
                    ButtonLabel(context.getString(R.string.cancel))
                }
            },
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
                contentDescription = label.withoutSoftHyphens(),
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
            Triple(MarkupTool.IMAGE, Icons.Filled.Image, R.string.markup_tool_image),
        )
    val labels = items.map { context.getString(it.third) }
    val labelStyle = MaterialTheme.typography.labelSmall
    // 8dp horizontal padding keeps the outermost labels off the screen edge
    // (#50; "Marker" used to touch it).
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        val slotWidth = constraints.maxWidth / items.size
        // One scale for all five. Per-label fallback rendered "Auswahl",
        // "Text" and "Marker" at three sizes, and the shorter label pulled
        // its circle down because the row centered columns of different
        // heights (#50).
        val decisions =
            rememberUniformLabelDecisions(
                labels = labels,
                slotWidthPx = slotWidth,
                style = labelStyle,
                policy = FitTextPolicy(maxLines = 2, minScale = TOOL_LABEL_MIN_SCALE),
            )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            items.forEachIndexed { index, (mode, icon, _) ->
                val label = labels[index]
                val decision = decisions[index]
                Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                    CompactIconButton(
                        onClick = { onTool(mode) },
                        icon = icon,
                        contentDescription = label.withoutSoftHyphens(),
                        active = tool == mode,
                        label = {
                            Text(
                                text = decision.text,
                                style =
                                    scaledLabelStyle(
                                        labelStyle,
                                        labelStyle.fontSize.value * decision.scale,
                                        labelStyle.fontSize.value,
                                    ),
                                color = MaterialTheme.colorScheme.onSurface,
                                // Semantics from the source label, not the
                                // drawn lines (#64).
                                modifier = Modifier.fitTextSemantics(label),
                                textAlign = TextAlign.Center,
                                softWrap = false,
                                maxLines = decision.maxLines,
                                overflow = TextOverflow.Clip,
                            )
                        },
                    )
                }
            }
        }
    }
}

/**
 * Solid markup colours shared by text and stroke. Yellow stays available for
 * highlight strokes; there is no separate "Automatic" mode or Office theme
 * shade grid (#90).
 */
private val markupPalette: List<Pair<Int, Int>> =
    listOf(
        android.graphics.Color.BLACK to R.string.markup_color_black,
        android.graphics.Color.WHITE to R.string.markup_color_white,
        android.graphics.Color.GRAY to R.string.markup_color_gray,
        android.graphics.Color.RED to R.string.markup_color_red,
        android.graphics.Color.rgb(255, 127, 0) to R.string.markup_color_orange,
        android.graphics.Color.YELLOW to R.string.markup_color_yellow,
        android.graphics.Color.rgb(0, 150, 64) to R.string.markup_color_green,
        android.graphics.Color.rgb(0, 128, 128) to R.string.markup_color_teal,
        android.graphics.Color.BLUE to R.string.markup_color_blue,
        android.graphics.Color.rgb(75, 0, 130) to R.string.markup_color_indigo,
        android.graphics.Color.rgb(128, 0, 128) to R.string.markup_color_purple,
        android.graphics.Color.rgb(255, 105, 180) to R.string.markup_color_pink,
        android.graphics.Color.rgb(139, 69, 19) to R.string.markup_color_brown,
        android.graphics.Color.rgb(0, 188, 212) to R.string.markup_color_cyan,
    )

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
    onColor: (Int) -> Unit,
) {
    val context = LocalContext.current
    var paletteOpen by remember { mutableStateOf(false) }
    // The current colour name doubles as the button's accessibility label;
    // a colour loaded from an older build may not be in the palette (#90).
    val currentColorName =
        markupPalette.firstOrNull { (argb, _) -> argb == color }
            ?.let { (_, labelRes) -> context.getString(labelRes) }
            ?: context.getString(R.string.color)
    // FlowRow so the stepper and the colour button wrap to a second line
    // instead of squeezing on narrow screens with large font scale (#50).
    // Each line is centered, so the wrapped row still looks balanced (#65).
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
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
        // One 48dp button filled with the selected colour opens the palette.
        // The palette is a DropdownMenu popover so it stays anchored to the
        // markup bar instead of covering the page with a bottom sheet (#90).
        Box {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { paletteOpen = true }
                        .semantics {
                            contentDescription = currentColorName.withoutSoftHyphens()
                        },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                            ),
                )
            }
            DropdownMenu(expanded = paletteOpen, onDismissRequest = { paletteOpen = false }) {
                Text(
                    text = currentColorName,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                // 14 solid colours in rows of five: reds, oranges, yellows,
                // greens, blues, purple, plus black/white/grey. The selected
                // swatch gets a primary ring and a contrast check (#90).
                FlowRow(
                    modifier =
                        Modifier
                            .width(280.dp)
                            .selectableGroup()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    maxItemsInEachRow = 5,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    markupPalette.forEach { (argb, labelRes) ->
                        val selected = color == argb
                        val description = context.getString(labelRes)
                        Box(
                            modifier =
                                Modifier
                                    .size(48.dp)
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = {
                                            paletteOpen = false
                                            onColor(argb)
                                        },
                                    ).semantics { contentDescription = description.withoutSoftHyphens() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(32.dp)
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
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint =
                                            if (Color(argb).luminance() > 0.5f) {
                                                Color.Black
                                            } else {
                                                Color.White
                                            },
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
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
                contentDescription = stepDownDescription.withoutSoftHyphens(),
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
                contentDescription = stepUpDescription.withoutSoftHyphens(),
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
                // Same box for every tool. The highlight glyph is a pen, but
                // the circle behind it is this 40dp button; a shorter label
                // used to drop that circle when the row centered the columns.
                modifier = Modifier.size(24.dp),
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
    images: List<PdfImageAnnotation> = emptyList(),
    imageBitmaps: Map<String, Bitmap> = emptyMap(),
    highlightIndex: Int? = null,
    selectedStrokeIndex: Int = -1,
    selectedImageIndex: Int = -1,
): Bitmap {
    val out = base.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(out)
    val items =
        buildList {
            annotations.forEachIndexed { index, ann -> add(DrawItem.Text(index, ann, ann.zOrder)) }
            strokes.forEachIndexed { index, stroke -> add(DrawItem.Stroke(index, stroke, stroke.zOrder)) }
            images.forEachIndexed { index, image -> add(DrawItem.Image(index, image, image.zOrder)) }
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
            is DrawItem.Image -> {
                val image = item.image
                val left = image.xFraction * out.width
                val top = image.yFraction * out.height
                val right = (image.xFraction + image.widthFraction) * out.width
                val bottom = (image.yFraction + image.heightFraction) * out.height
                val bitmap = imageBitmaps[image.imagePath]
                if (bitmap != null && right > left && bottom > top) {
                    canvas.drawBitmap(
                        bitmap,
                        null,
                        android.graphics.RectF(left, top, right, bottom),
                        android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG),
                    )
                }
                if (item.index == selectedImageIndex) {
                    val outline =
                        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = 0xFF3F51B5.toInt()
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 4f
                        }
                    canvas.drawRect(left, top, right, bottom, outline)
                    drawHandles(canvas, left, top, right, bottom)
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
    // Soft hyphens never reach the drawn annotation text (#62).
    val text = text.withoutSoftHyphens()
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
    // A blank line is content (#172): keep it so internal line breaks survive wrapping.
    if (segment.isEmpty()) return listOf("")
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

/** Layout placement in px that does not promote a graphics layer (see [coveringPanLayer]). */
private fun Modifier.placePx(
    x: Float,
    y: Float,
): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(x.roundToInt(), y.roundToInt())
        }
    }

private fun fittedRect(
    container: IntSize,
    bmpW: Int,
    bmpH: Int,
    fitWidth: Boolean = false,
): Rect {
    val placed =
        previewPageRect(
            container.width.toFloat(),
            container.height.toFloat(),
            bmpW.toFloat(),
            bmpH.toFloat(),
            fitWidth,
        )
    return Rect(placed.left, placed.top, placed.left + placed.width, placed.top + placed.height)
}

private const val ANNOTATION_HIT_PAD = 0.05f

/** Long-edge decode bound for the on-screen preview of a placed image (#92). */
private const val MARKUP_IMAGE_PREVIEW_MAX_PX = 1200

/** Smallest allowed width/height of a placed image, as page fractions. */
private const val MARKUP_IMAGE_MIN_FRACTION = 0.05f

private fun imageHitTest(
    fx: Float,
    fy: Float,
    image: PdfImageAnnotation,
): Boolean {
    val pad = 0.01f
    return fx >= image.xFraction - pad &&
        fx <= image.xFraction + image.widthFraction + pad &&
        fy >= image.yFraction - pad &&
        fy <= image.yFraction + image.heightFraction + pad
}

/** Where an image's corners land on screen after the page scale/pan transform. */
private fun imageScreenRect(
    image: PdfImageAnnotation,
    fitted: Rect,
    container: IntSize,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
): Rect {
    val zoom = if (scale > 0f) scale else 1f
    val screenWidth = container.width.toFloat()
    val screenHeight = container.height.toFloat()

    fun screenX(fraction: Float): Float {
        val x = fitted.left + fraction * fitted.width
        return (x - screenWidth / 2f) * zoom + screenWidth / 2f + offsetX
    }

    fun screenY(fraction: Float): Float {
        val y = fitted.top + fraction * fitted.height
        return (y - screenHeight / 2f) * zoom + screenHeight / 2f + offsetY
    }

    return Rect(
        screenX(image.xFraction),
        screenY(image.yFraction),
        screenX(image.xFraction + image.widthFraction),
        screenY(image.yFraction + image.heightFraction),
    )
}

private data class ImageCorner(val left: Boolean, val top: Boolean)

/** Page-fraction rectangle produced by [resizeImageFromCorner]. */
internal data class ImageCornerResize(
    val xFraction: Float,
    val yFraction: Float,
    val widthFraction: Float,
    val heightFraction: Float,
)

/**
 * Aspect-locked resize of an image dragged by one corner (#141). [dragDx]/[dragDy] are
 * the total drag since the gesture started, in page fractions. A drag away from the
 * opposite corner grows the image for every corner; a drag towards it shrinks. The
 * opposite corner stays anchored, and the size is clamped to [minFraction] and the
 * page edges. Returns null for a degenerate start rectangle.
 */
internal fun resizeImageFromCorner(
    startX: Float,
    startY: Float,
    startWidth: Float,
    startHeight: Float,
    left: Boolean,
    top: Boolean,
    dragDx: Float,
    dragDy: Float,
    minFraction: Float = MARKUP_IMAGE_MIN_FRACTION,
): ImageCornerResize? {
    if (startWidth <= 0f || startHeight <= 0f) return null
    // Dragging left/up is outward for a left/top corner but inward for a right/bottom
    // one, so sign the drag by corner before deriving a single scale factor.
    val signedDx = if (left) -dragDx else dragDx
    val signedDy = if (top) -dragDy else dragDy
    // Use the dominant drag direction, then scale width and height by the same factor
    // so corners never distort the image.
    val scaleDelta =
        if (
            abs(signedDx) / startWidth >=
            abs(signedDy) / startHeight
        ) {
            signedDx / startWidth
        } else {
            signedDy / startHeight
        }
    val availableW = if (left) startX + startWidth else 1f - startX
    val availableH = if (top) startY + startHeight else 1f - startY
    val minScale = maxOf(minFraction / startWidth, minFraction / startHeight)
    val maxScale =
        minOf(availableW / startWidth, availableH / startHeight)
            .coerceAtLeast(minScale)
    val factor = (1f + scaleDelta).coerceIn(minScale, maxScale)
    val newW = startWidth * factor
    val newH = startHeight * factor
    return ImageCornerResize(
        xFraction = if (left) startX + startWidth - newW else startX,
        yFraction = if (top) startY + startHeight - newH else startY,
        widthFraction = newW,
        heightFraction = newH,
    )
}

/** The image corner within [thresholdPx] of [position], or null for a body move. */
private fun nearestImageCorner(
    bounds: Rect,
    position: Offset,
    thresholdPx: Float,
): ImageCorner? {
    val corners =
        listOf(
            ImageCorner(left = true, top = true) to Offset(bounds.left, bounds.top),
            ImageCorner(left = false, top = true) to Offset(bounds.right, bounds.top),
            ImageCorner(left = true, top = false) to Offset(bounds.left, bounds.bottom),
            ImageCorner(left = false, top = false) to Offset(bounds.right, bounds.bottom),
        )
    return corners
        .map { (corner, point) -> corner to (point - position).getDistance() }
        .filter { (_, distance) -> distance <= thresholdPx }
        .minByOrNull { (_, distance) -> distance }
        ?.first
}

/**
 * Intrinsic, EXIF-corrected pixel dimensions of an image file (#92). The preview and PDF
 * bake decode through [decodeImageBitmap], which applies the same orientation.
 */
private fun orientedImageDimensions(path: String): Pair<Int, Int>? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    var width = options.outWidth
    var height = options.outHeight
    val orientation =
        try {
            ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    val swapped =
        orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
            orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE
    if (swapped) {
        val tmp = width
        width = height
        height = tmp
    }
    return width to height
}

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
    fitWidth: Boolean = false,
): Rect? {
    if (bmpWidth <= 0 || bmpHeight <= 0) return null
    val fitted = fittedRect(container, bmpWidth, bmpHeight, fitWidth)
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
    // The measured field size is in page-layer pixels; the graphics layer scales it on screen.
    val s = if (scale > 0f) scale else 1f
    return Rect(topLeft.x, topLeft.y, topLeft.x + width * s, topLeft.y + height * s)
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

/**
 * The canvas text estimate of an annotation (#55b): its wrapped lines and the paint that
 * measured them, at page bitmap resolution. Used for the pre-layout caret mapping and its
 * refinement through the text field's own layout.
 */
private class AnnotationCanvasBasis(
    val lines: List<String>,
    val paint: android.graphics.Paint,
)

private fun annotationCanvasLines(
    annotation: PdfAnnotation,
    bitmapWidth: Int,
    bitmapHeight: Int,
): AnnotationCanvasBasis? {
    if (bitmapWidth <= 0 || bitmapHeight <= 0 || annotation.text.isEmpty()) return null
    val paint =
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = annotation.sizeFraction * bitmapHeight
            typeface = android.graphics.Typeface.DEFAULT
        }
    return AnnotationCanvasBasis(
        wrapAnnotationText(annotation.text, paint, (bitmapWidth * 0.95f).toInt()),
        paint,
    )
}

private fun mapTapToFraction(
    tap: Offset,
    container: IntSize,
    bmpW: Int,
    bmpH: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    fitWidth: Boolean = false,
): Pair<Float, Float> {
    val rect = fittedRect(container, bmpW, bmpH, fitWidth)
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

@Preview(fontScale = 2f, widthDp = 320, heightDp = 140, showBackground = true)
@Preview(fontScale = 2f, widthDp = 360, heightDp = 140, showBackground = true)
@Composable
internal fun MarkupToolBarPreview() {
    MaterialTheme {
        MarkupToolBar(tool = MarkupTool.SELECT, onTool = {})
    }
}

@Preview(fontScale = 2f, widthDp = 320, heightDp = 160, showBackground = true)
@Preview(fontScale = 1f, widthDp = 480, heightDp = 120, showBackground = true)
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
            onColor = {},
        )
    }
}
