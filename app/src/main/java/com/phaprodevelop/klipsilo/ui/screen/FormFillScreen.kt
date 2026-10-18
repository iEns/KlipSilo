package com.phaprodevelop.klipsilo.ui.screen

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.FormKeyboardKind
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.repository.FormFieldHit
import com.phaprodevelop.klipsilo.data.repository.FormFieldInfo
import com.phaprodevelop.klipsilo.data.repository.FormFieldType
import com.phaprodevelop.klipsilo.ui.components.ButtonLabel
import com.phaprodevelop.klipsilo.ui.components.DialogTitleLabel
import com.phaprodevelop.klipsilo.ui.components.FORM_FIELD_FONT_FILL
import com.phaprodevelop.klipsilo.ui.components.FitTopBar
import com.phaprodevelop.klipsilo.ui.components.PASSWORD_BADGE_FONT_FILL
import com.phaprodevelop.klipsilo.ui.components.PASSWORD_BADGE_HEIGHT_FILL
import com.phaprodevelop.klipsilo.ui.components.PASSWORD_BADGE_MIN_FONT_SP
import com.phaprodevelop.klipsilo.ui.components.PREVIEW_BASE_RENDER_DIM
import com.phaprodevelop.klipsilo.ui.components.PREVIEW_MIN_SCALE
import com.phaprodevelop.klipsilo.ui.components.PageSelector
import com.phaprodevelop.klipsilo.ui.components.PreviewZoomState
import com.phaprodevelop.klipsilo.ui.components.clampPreviewZoom
import com.phaprodevelop.klipsilo.ui.components.doubleTapPreviewZoom
import com.phaprodevelop.klipsilo.ui.components.formFieldTextLayout
import com.phaprodevelop.klipsilo.ui.components.passwordBadgeLayout
import com.phaprodevelop.klipsilo.ui.components.pdfPageCount
import com.phaprodevelop.klipsilo.ui.components.previewPointToPageLocal
import com.phaprodevelop.klipsilo.ui.components.previewRenderDim
import com.phaprodevelop.klipsilo.ui.components.produceLatest
import com.phaprodevelop.klipsilo.ui.components.rememberDisplayedBitmap
import com.phaprodevelop.klipsilo.ui.components.renderPdfPage
import com.phaprodevelop.klipsilo.ui.components.transformPreviewZoom
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel
import com.phaprodevelop.klipsilo.withoutSoftHyphens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

/** Preview height while the keyboard is open, so the field list keeps the screen space. */
private val CollapsedPreviewHeight = 88.dp

/** How long the preview zoom must be still before re-rendering sharper. */
private const val RESHARPEN_SETTLE_MILLIS = 250L

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class,
)
@Composable
fun FormFillScreen(
    entryId: Long,
    viewModel: ClipboardViewModel,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val clips by viewModel.clips.collectAsState()
    var loadedEntry by remember { mutableStateOf<ClipboardEntry?>(null) }
    var lookupDone by remember { mutableStateOf(false) }
    LaunchedEffect(entryId) {
        loadedEntry = viewModel.getClip(entryId)
        lookupDone = true
    }
    val entry = loadedEntry ?: clips.find { it.id == entryId }
    val formFields by viewModel.formFields.collectAsState()
    val formFieldsLoading by viewModel.formFieldsLoading.collectAsState()
    val isConverting by viewModel.isConverting.collectAsState()

    var pageIndex by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    val displayed = rememberDisplayedBitmap()
    val pageGate = remember { AtomicInteger(0) }
    var renderDim by remember { mutableIntStateOf(PREVIEW_BASE_RENDER_DIM) }
    var dirty by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var values by rememberSaveable(
        stateSaver =
            mapSaver<Map<String, String>>(
                save = { it },
                restore = { restored -> restored.mapValues { (_, v) -> v as String } },
            ),
    ) { mutableStateOf<Map<String, String>>(emptyMap()) }
    val scroll = rememberScrollState()
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val bringIntoViewRequesters = remember { mutableMapOf<String, BringIntoViewRequester>() }
    var focusedField by remember { mutableStateOf<String?>(null) }
    // Per-password-field show/hide, remembered until this screen is left (#61).
    var revealedPasswords by remember { mutableStateOf<Set<String>>(emptySet()) }
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val imeVisible = imeBottom > 0

    val navigateBack by viewModel.navigateBack.collectAsState()
    LaunchedEffect(navigateBack) {
        if (navigateBack) {
            dirty = false
            viewModel.clearFormFields()
            viewModel.clearNavigateBack()
            onNavigateBack()
        }
    }
    LaunchedEffect(imeBottom, focusedField) {
        val name = focusedField ?: return@LaunchedEffect
        if (imeBottom <= 0) return@LaunchedEffect
        bringIntoViewRequesters[name]?.bringIntoView()
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

    LaunchedEffect(entry.id) {
        viewModel.loadFormFields(entry)
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearFormFields()
        }
    }
    LaunchedEffect(formFields) {
        formFields?.forEach { field ->
            if (field.valueKey !in values) {
                val newValue =
                    when (field.type) {
                        FormFieldType.CHECKBOX -> {
                            val v = field.currentValue
                            if (v != null && v.isNotBlank() && v != "Off") "true" else "false"
                        }
                        else -> field.currentValue ?: ""
                    }
                values = values + (field.valueKey to newValue)
            }
        }
    }

    val filePath = entry.filePath
    LaunchedEffect(filePath) {
        filePath?.let { path ->
            pageCount = withContext(Dispatchers.IO) { pdfPageCount(path) }
        }
    }
    LaunchedEffect(filePath, pageIndex) {
        if (renderDim != PREVIEW_BASE_RENDER_DIM) renderDim = PREVIEW_BASE_RENDER_DIM
    }
    LaunchedEffect(filePath, pageIndex, renderDim) {
        val path = filePath ?: return@LaunchedEffect
        val dim = renderDim
        val loaded =
            produceLatest(
                pageGate,
                produce = { withContext(Dispatchers.IO) { renderPdfPage(path, dim, pageIndex) } },
                discard = { bitmap -> bitmap?.recycle() },
            ) ?: return@LaunchedEffect
        displayed.update(loaded.value)
    }

    fun leave() {
        if (dirty) {
            showDiscardDialog = true
        } else {
            viewModel.clearFormFields()
            onNavigateBack()
        }
    }

    fun focusField(name: String) {
        val field = formFields.orEmpty().firstOrNull { it.valueKey == name }
        field?.hits?.firstOrNull()?.pageIndex?.let { page -> pageIndex = page }
        focusedField = name
        val requester = focusRequesters[name] ?: return
        runCatching { requester.requestFocus() }
    }

    BackHandler { leave() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            FitTopBar(
                title = context.getString(R.string.fill_form),
                saveLabel = context.getString(R.string.save_pdf),
                saveEnabled = entry.filePath != null && !isConverting,
                onBack = { leave() },
                onSave = {
                    if (dirty) {
                        viewModel.fillForm(entry, values)
                    } else {
                        // Save with no changes (#60): just leave, like Back, instead
                        // of filling a copy of the untouched form.
                        viewModel.clearFormFields()
                        onNavigateBack()
                    }
                },
                backContentDescription = context.getString(R.string.back),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            displayed.bitmap?.let { bmp ->
                FormPdfPreview(
                    bitmap = bmp,
                    pageIndex = pageIndex,
                    fields = formFields.orEmpty(),
                    values = values,
                    revealedPasswords = revealedPasswords,
                    imeVisible = imeVisible,
                    onFieldTap = { name -> focusField(name) },
                    onZoomSettled = { scale ->
                        previewRenderDim(renderDim, scale)?.let { renderDim = it }
                    },
                )
            }
            if (pageCount > 1) {
                PageSelector(
                    pageIndex = pageIndex,
                    pageCount = pageCount,
                    onPrev = { if (pageIndex > 0) pageIndex-- },
                    onNext = { if (pageIndex < pageCount - 1) pageIndex++ },
                    onGoToPage = { pageIndex = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            when {
                formFieldsLoading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                formFields.isNullOrEmpty() -> {
                    Text(
                        text = context.getString(R.string.no_form_fields),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(16.dp),
                    )
                }
                else -> {
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .imePadding()
                                .verticalScroll(scroll),
                    ) {
                        Text(
                            text = context.getString(R.string.fill_form_fields),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        // Text rows in order: Next jumps to the next one, the last gets Done (#52).
                        val textRowIndexes =
                            formFields.orEmpty()
                                .withIndex()
                                .filter { it.value.type == FormFieldType.TEXT }
                                .map { it.index }
                        val nextTextFieldKey =
                            textRowIndexes.zipWithNext { current, next ->
                                formFields.orEmpty()[current].valueKey to
                                    formFields.orEmpty()[next].valueKey
                            }.toMap()
                        val lastTextFieldIndex = textRowIndexes.lastOrNull()
                        formFields.orEmpty().forEachIndexed { index, field ->
                            val name = field.valueKey
                            val requester = focusRequesters.getOrPut(name) { FocusRequester() }
                            FormFieldRow(
                                field = field,
                                value = values[name].orEmpty(),
                                focusRequester = requester,
                                bringIntoViewRequester =
                                    bringIntoViewRequesters.getOrPut(name) { BringIntoViewRequester() },
                                imeAction = if (index == lastTextFieldIndex) ImeAction.Done else ImeAction.Next,
                                onNextField = { nextTextFieldKey[name]?.let { nextName -> focusField(nextName) } },
                                onValueChange = {
                                    values = values + (name to it)
                                    dirty = true
                                },
                                passwordRevealed = name in revealedPasswords,
                                onTogglePasswordRevealed = {
                                    revealedPasswords =
                                        if (name in revealedPasswords) {
                                            revealedPasswords - name
                                        } else {
                                            revealedPasswords + name
                                        }
                                },
                                onFocused = {
                                    focusedField = name
                                    field.hits.firstOrNull()?.pageIndex?.let { page -> pageIndex = page }
                                },
                            )
                        }
                        Spacer(Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { DialogTitleLabel(context.getString(R.string.markup_leave_title)) },
            text = { Text(context.getString(R.string.markup_leave_message)) },
            confirmButton = {
                // FlowRow so the three choices stay reachable at font scale 2.0 (#7).
                FlowRow {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        ButtonLabel(context.getString(R.string.cancel))
                    }
                    TextButton(onClick = {
                        showDiscardDialog = false
                        viewModel.clearFormFields()
                        onNavigateBack()
                    }) {
                        ButtonLabel(context.getString(R.string.discard))
                    }
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            // Same as "Save PDF": fillForm navigates back itself when done.
                            viewModel.fillForm(entry, values)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FormPdfPreview(
    bitmap: Bitmap,
    pageIndex: Int,
    fields: List<FormFieldInfo>,
    values: Map<String, String>,
    revealedPasswords: Set<String>,
    imeVisible: Boolean,
    onFieldTap: (String) -> Unit,
    onZoomSettled: (Float) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val expandedHeight = LocalConfiguration.current.screenHeightDp.dp * 0.40f
    val previewHeight by animateDpAsState(
        targetValue = if (imeVisible) CollapsedPreviewHeight else expandedHeight,
        label = "formPreviewHeight",
    )
    var zoom by remember { mutableStateOf(PreviewZoomState()) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    // Fresh dimensions for the gesture callbacks, which stay attached across recompositions.
    val layout = remember { PreviewLayout() }

    // Pinch/pan/double-tap resets when another page is shown.
    LaunchedEffect(pageIndex) {
        zoom = PreviewZoomState()
    }
    // Re-render at a higher resolution only once the pinch has settled.
    LaunchedEffect(zoom.scale) {
        if (zoom.scale <= PREVIEW_MIN_SCALE) {
            onZoomSettled(PREVIEW_MIN_SCALE)
            return@LaunchedEffect
        }
        delay(RESHARPEN_SETTLE_MILLIS)
        onZoomSettled(zoom.scale)
    }

    Box(
        modifier = Modifier.fillMaxWidth().height(previewHeight),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        viewportSize = it
                        layout.viewportWidth = it.width.toFloat()
                        layout.viewportHeight = it.height.toFloat()
                    }
                    .clipToBounds()
                    .pointerInput(fields, pageIndex) {
                        detectTapGestures(
                            onTap = { offset ->
                                val (localX, localY) =
                                    previewPointToPageLocal(
                                        zoom,
                                        offset.x,
                                        offset.y,
                                        layout.viewportWidth,
                                        layout.viewportHeight,
                                        layout.pageWidth,
                                        layout.pageHeight,
                                    )
                                val name =
                                    hitField(
                                        fields,
                                        pageIndex,
                                        localX,
                                        localY,
                                        layout.pageWidth,
                                        layout.pageHeight,
                                    )
                                if (name != null) onFieldTap(name)
                            },
                            onDoubleTap = { offset ->
                                zoom =
                                    doubleTapPreviewZoom(
                                        zoom,
                                        offset.x - layout.viewportWidth / 2f,
                                        offset.y - layout.viewportHeight / 2f,
                                        layout.viewportWidth,
                                        layout.viewportHeight,
                                        layout.pageWidth,
                                        layout.pageHeight,
                                    )
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, gestureZoom, _ ->
                            zoom =
                                transformPreviewZoom(
                                    zoom,
                                    gestureZoom,
                                    pan.x,
                                    pan.y,
                                    centroid.x - layout.viewportWidth / 2f,
                                    centroid.y - layout.viewportHeight / 2f,
                                    layout.viewportWidth,
                                    layout.viewportHeight,
                                    layout.pageWidth,
                                    layout.pageHeight,
                                )
                        }
                    },
        ) {
            val viewportWidth = viewportSize.width.toFloat()
            val viewportHeight = viewportSize.height.toFloat()
            if (viewportWidth > 0f && viewportHeight > 0f) {
                val aspect = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
                val boxAspect = if (viewportHeight > 0f) viewportWidth / viewportHeight else aspect
                val pageWidth = if (aspect > boxAspect) viewportWidth else viewportHeight * aspect
                val pageHeight = if (aspect == 0f) viewportHeight else pageWidth / aspect
                layout.pageWidth = pageWidth
                layout.pageHeight = pageHeight

                // Keep the page inside the viewport after the preview grew or shrank.
                LaunchedEffect(viewportWidth, viewportHeight) {
                    zoom = clampPreviewZoom(zoom, viewportWidth, viewportHeight, pageWidth, pageHeight)
                }

                Box(
                    modifier =
                        Modifier
                            .size(pageWidth.toDp(density), pageHeight.toDp(density))
                            .align(Alignment.Center)
                            .graphicsLayer {
                                scaleX = zoom.scale
                                scaleY = zoom.scale
                                translationX = zoom.offsetX
                                translationY = zoom.offsetY
                            },
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = context.getString(R.string.pdf).withoutSoftHyphens(),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                    )
                    fields.forEach { field ->
                        val value = values[field.valueKey].orEmpty()
                        field.hits.filter { it.pageIndex == pageIndex }.forEach { hit ->
                            val revealed = field.valueKey in revealedPasswords
                            // Hidden passwords draw as bullets; a revealed one shows the text (#61).
                            val masked = passwordPreviewText(field.isPassword, revealed, value)
                            val shown = formOverlayText(field, hit, masked)
                            if (shown.isEmpty()) return@forEach
                            // Hidden password: the "saved as readable text" pill is
                            // drawn beside the dots, sized to stay in the rect (#61).
                            val badge =
                                if (showPasswordSavedBadge(field.isPassword, revealed, value)) {
                                    context.getString(R.string.form_password_saved_badge)
                                } else {
                                    null
                                }
                            FormValueOverlay(
                                hit = hit,
                                text = shown,
                                multiline = field.multiline,
                                imageWidth = pageWidth.toDp(density),
                                imageHeight = pageHeight.toDp(density),
                                savedBadgeText = badge,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Viewport and page dimensions in px, read by the preview's gesture callbacks. */
private class PreviewLayout {
    var viewportWidth = 0f
    var viewportHeight = 0f
    var pageWidth = 0f
    var pageHeight = 0f
}

private fun Float.toDp(density: Density): Dp = with(density) { toDp() }

@Composable
private fun FormValueOverlay(
    hit: FormFieldHit,
    text: String,
    multiline: Boolean,
    imageWidth: Dp,
    imageHeight: Dp,
    savedBadgeText: String? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Pill colours resolved at composition; the pill itself is drawn inside
    // the same drawWithCache as the value (#61).
    val badgeBackground = MaterialTheme.colorScheme.secondaryContainer
    val badgeForeground = MaterialTheme.colorScheme.onSecondaryContainer
    // Readability floor for the tiny pill, independent of the user's font scale (#61).
    val minBadgeFontPx = with(Density(density.density)) { PASSWORD_BADGE_MIN_FONT_SP.sp.toPx() }
    val width = imageWidth * (hit.right - hit.left).coerceAtLeast(0f)
    val height = imageHeight * (hit.bottom - hit.top).coerceAtLeast(0f)
    // The overlay is transparent; only the value is drawn, with the exact
    // baseline from formFieldTextLayout so it never rides on inherited
    // composable styles (the inherited line height made text ride too low).
    Box(
        modifier =
            Modifier
                .offset(x = imageWidth * hit.left, y = imageHeight * hit.top)
                .size(width, height)
                .clipToBounds()
                .clearAndSetSemantics { }
                .drawWithCache {
                    val rectWidthPx = size.width
                    val rectHeightPx = size.height
                    val pad = 2.dp.toPx()
                    // Measure the default font's real metrics at the height-fit
                    // size; nothing here inherits composable styles.
                    val fontPx = (rectHeightPx * FORM_FIELD_FONT_FILL).coerceAtLeast(1f)

                    fun measureAt(
                        size: Float,
                        wrap: Boolean,
                    ) = textMeasurer.measure(
                        AnnotatedString(text),
                        TextStyle(fontSize = size.toSp()),
                        maxLines = if (wrap) 0 else 1,
                        softWrap = wrap,
                        constraints =
                            if (wrap) {
                                Constraints(maxWidth = (rectWidthPx - 2f * pad).roundToInt())
                            } else {
                                Constraints()
                            },
                    )
                    val probe = measureAt(fontPx, wrap = false)
                    val layout =
                        formFieldTextLayout(
                            rectWidthPx = rectWidthPx,
                            rectHeightPx = rectHeightPx,
                            multiline = multiline,
                            ascentPerEmPx = probe.firstBaseline / fontPx,
                            descentPerEmPx = (probe.size.height - probe.firstBaseline) / fontPx,
                            // Multiline values wrap instead of shrinking to fit.
                            textWidthPerEmPx =
                                if (multiline || probe.size.width <= 0) {
                                    0f
                                } else {
                                    probe.size.width / fontPx
                                },
                            leftPaddingPx = pad,
                        )
                    val drawn =
                        when {
                            multiline -> measureAt(layout.fontSizePx, wrap = true)
                            layout.fontSizePx != fontPx -> measureAt(layout.fontSizePx, wrap = false)
                            else -> probe
                        }
                    // The hidden-password pill: the same pure field-rect math
                    // places it at the trailing edge beside the dots, inside the
                    // rect, and drops it when nothing readable fits (#61).
                    val badge =
                        savedBadgeText?.takeIf { it.isNotEmpty() }?.let { badgeText ->
                            val badgeRefFontPx =
                                (rectHeightPx * PASSWORD_BADGE_HEIGHT_FILL * PASSWORD_BADGE_FONT_FILL)
                                    .coerceAtLeast(1f)
                            val badgeProbe =
                                textMeasurer.measure(
                                    AnnotatedString(badgeText),
                                    TextStyle(fontSize = badgeRefFontPx.toSp()),
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            val perEm = badgeRefFontPx.coerceAtLeast(1f)
                            passwordBadgeLayout(
                                fieldWidthPx = rectWidthPx,
                                fieldHeightPx = rectHeightPx,
                                dotsWidthPx = drawn.size.width.toFloat(),
                                badgeTextWidthPerEmPx = badgeProbe.size.width / perEm,
                                badgeFontAscentPerEm = badgeProbe.firstBaseline / perEm,
                                badgeFontDescentPerEm =
                                    (badgeProbe.size.height - badgeProbe.firstBaseline) / perEm,
                                edgePaddingPx = pad,
                                minFontPx = minBadgeFontPx,
                            )?.let { badgeLayout ->
                                badgeLayout to
                                    textMeasurer.measure(
                                        AnnotatedString(badgeText),
                                        TextStyle(fontSize = badgeLayout.fontSizePx.toSp()),
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                            }
                        }
                    onDrawBehind {
                        drawText(
                            drawn,
                            color = Color.Black,
                            topLeft =
                                Offset(
                                    layout.leftPaddingPx,
                                    layout.baselinePx - drawn.firstBaseline,
                                ),
                        )
                        badge?.let { (pos, badgeDrawn) ->
                            drawRoundRect(
                                color = badgeBackground,
                                topLeft = Offset(pos.leftPx, pos.topPx),
                                size = Size(pos.widthPx, pos.heightPx),
                                cornerRadius = CornerRadius(pos.heightPx / 2f),
                            )
                            drawText(
                                badgeDrawn,
                                color = badgeForeground,
                                topLeft =
                                    Offset(
                                        pos.textLeftPx,
                                        pos.textBaselinePx - badgeDrawn.firstBaseline,
                                    ),
                            )
                        }
                    }
                },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FormFieldRow(
    field: FormFieldInfo,
    value: String,
    focusRequester: FocusRequester,
    bringIntoViewRequester: BringIntoViewRequester,
    imeAction: ImeAction,
    passwordRevealed: Boolean,
    onTogglePasswordRevealed: () -> Unit,
    onNextField: () -> Unit,
    onValueChange: (String) -> Unit,
    onFocused: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        when (field.type) {
            FormFieldType.CHECKBOX -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = value == "true",
                        onCheckedChange = { onValueChange(if (it) "true" else "false") },
                        modifier =
                            Modifier
                                .focusRequester(focusRequester)
                                .onFocusChanged { state -> if (state.isFocused) onFocused() },
                    )
                    // A long single word ("Kontrollkästchen 1") must never break mid-word at
                    // large font scales: ButtonLabel shrinks it within word boundaries (#57).
                    ButtonLabel(
                        text = field.label,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            FormFieldType.RADIO, FormFieldType.CHOICE -> {
                Text(
                    text = field.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                field.options.forEachIndexed { index, option ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onValueChange(option) }
                                .then(
                                    if (index == 0) {
                                        Modifier
                                            .focusRequester(focusRequester)
                                            .onFocusChanged { state -> if (state.isFocused) onFocused() }
                                    } else {
                                        Modifier
                                    },
                                ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == option, onClick = { onValueChange(option) })
                        Text(option, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            else -> {
                val keyboard = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current
                val limitedValueChange: (String) -> Unit = { typed ->
                    val limited = field.maxLength?.let { typed.take(it) } ?: typed
                    onValueChange(limited)
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = limitedValueChange,
                    label = { Text(field.label) },
                    // Multiline PDF text fields keep Enter as a line break (#52); the rest
                    // advance to the next field with ImeAction.Next instead.
                    singleLine = !field.multiline,
                    keyboardOptions =
                        when {
                            field.multiline -> KeyboardOptions()
                            // Password fields: password keyboard, no autocorrect (#61).
                            field.isPassword ->
                                KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = imeAction,
                                    autoCorrectEnabled = false,
                                )
                            else ->
                                KeyboardOptions(
                                    keyboardType =
                                        when (field.keyboardKind) {
                                            FormKeyboardKind.EMAIL -> KeyboardType.Email
                                            FormKeyboardKind.PHONE -> KeyboardType.Phone
                                            FormKeyboardKind.NUMBER -> KeyboardType.Number
                                            FormKeyboardKind.TEXT -> KeyboardType.Text
                                        },
                                    imeAction = imeAction,
                                )
                        },
                    // Hidden passwords are masked in the input; the eye toggle reveals them (#61).
                    visualTransformation =
                        if (field.isPassword && !passwordRevealed) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                    trailingIcon =
                        if (field.isPassword) {
                            {
                                IconButton(
                                    onClick = onTogglePasswordRevealed,
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(
                                        imageVector =
                                            if (passwordRevealed) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                        contentDescription =
                                            stringResource(
                                                if (passwordRevealed) {
                                                    R.string.hide_password
                                                } else {
                                                    R.string.show_password
                                                },
                                            ),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    // Real value warning: the saved PDF keeps the typed text as plain text (#61).
                    supportingText =
                        if (field.isPassword) {
                            { Text(stringResource(R.string.form_password_saved_note)) }
                        } else {
                            null
                        },
                    keyboardActions =
                        KeyboardActions(
                            onNext = { onNextField() },
                            onDone = {
                                keyboard?.hide()
                                focusManager.clearFocus()
                            },
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { state -> if (state.isFocused) onFocused() },
                )
            }
        }
    }
}

private fun formOverlayText(
    field: FormFieldInfo,
    hit: FormFieldHit,
    value: String,
): String {
    return when (field.type) {
        FormFieldType.CHECKBOX -> if (value == "true") "✓" else ""
        FormFieldType.RADIO -> if (!hit.option.isNullOrEmpty() && value == hit.option) "●" else ""
        else -> value
    }
}

private fun hitField(
    fields: List<FormFieldInfo>,
    pageIndex: Int,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
): String? {
    val slop = 12f
    var bestName: String? = null
    var bestArea = Float.POSITIVE_INFINITY
    for (field in fields) {
        for (hit in field.hits) {
            if (hit.pageIndex != pageIndex) continue
            val left = hit.left * width - slop
            val right = hit.right * width + slop
            val top = hit.top * height - slop
            val bottom = hit.bottom * height + slop
            if (x !in left..right || y !in top..bottom) continue
            val area = ((hit.right - hit.left) * (hit.bottom - hit.top)).coerceAtLeast(0.0001f)
            if (area < bestArea) {
                bestArea = area
                bestName = field.valueKey
            }
        }
    }
    return bestName
}

@Preview(fontScale = 2f, widthDp = 320, heightDp = 240, showBackground = true)
@Composable
private fun FormPdfPreviewPreview() {
    MaterialTheme {
        FormPdfPreview(
            bitmap = Bitmap.createBitmap(80, 104, Bitmap.Config.ARGB_8888),
            pageIndex = 0,
            fields = emptyList(),
            values = emptyMap(),
            revealedPasswords = emptySet(),
            imeVisible = false,
            onFieldTap = {},
            onZoomSettled = {},
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(fontScale = 2f, widthDp = 320, heightDp = 160, showBackground = true)
@Composable
private fun FormFieldRowCheckboxLongLabelPreview() {
    MaterialTheme {
        Column {
            FormFieldRow(
                field =
                    FormFieldInfo(
                        name = "s2",
                        fullyQualifiedName = "s2",
                        type = FormFieldType.CHECKBOX,
                        currentValue = null,
                        options = emptyList(),
                        label = "Kontrollkästchen 12",
                        valueKey = "s2",
                    ),
                value = "",
                focusRequester = FocusRequester(),
                bringIntoViewRequester = BringIntoViewRequester(),
                imeAction = ImeAction.Next,
                passwordRevealed = false,
                onTogglePasswordRevealed = {},
                onNextField = {},
                onValueChange = {},
                onFocused = {},
            )
        }
    }
}

// A hidden password field: masked input, eye toggle and the readable-text note (#61).
@OptIn(ExperimentalFoundationApi::class)
@Preview(fontScale = 2f, widthDp = 320, heightDp = 220, showBackground = true)
@Composable
private fun FormFieldRowPasswordHiddenPreview() {
    MaterialTheme {
        FormFieldRow(
            field =
                FormFieldInfo(
                    name = "pw",
                    fullyQualifiedName = "pw",
                    type = FormFieldType.TEXT,
                    currentValue = null,
                    options = emptyList(),
                    label = "Password",
                    valueKey = "pw",
                    isPassword = true,
                ),
            value = "secret",
            focusRequester = FocusRequester(),
            bringIntoViewRequester = BringIntoViewRequester(),
            imeAction = ImeAction.Done,
            passwordRevealed = false,
            onTogglePasswordRevealed = {},
            onNextField = {},
            onValueChange = {},
            onFocused = {},
        )
    }
}

// The same field with the eye toggled: the real text shows in both input and preview (#61).
@OptIn(ExperimentalFoundationApi::class)
@Preview(fontScale = 2f, widthDp = 320, heightDp = 220, showBackground = true)
@Composable
private fun FormFieldRowPasswordRevealedPreview() {
    MaterialTheme {
        FormFieldRow(
            field =
                FormFieldInfo(
                    name = "pw",
                    fullyQualifiedName = "pw",
                    type = FormFieldType.TEXT,
                    currentValue = null,
                    options = emptyList(),
                    label = "Password",
                    valueKey = "pw",
                    isPassword = true,
                ),
            value = "secret",
            focusRequester = FocusRequester(),
            bringIntoViewRequester = BringIntoViewRequester(),
            imeAction = ImeAction.Done,
            passwordRevealed = true,
            onTogglePasswordRevealed = {},
            onNextField = {},
            onValueChange = {},
            onFocused = {},
        )
    }
}

// Hidden password preview: the dots with the readable-text pill beside them,
// sized to stay inside the field rect (#61).
@Preview(fontScale = 2f, widthDp = 320, heightDp = 180, showBackground = true)
@Composable
private fun FormValueOverlayPasswordBadgePreview() {
    MaterialTheme {
        Box(Modifier.fillMaxWidth().height(72.dp)) {
            FormValueOverlay(
                hit = FormFieldHit(pageIndex = 0, left = 0.1f, top = 0.1f, right = 0.8f, bottom = 0.9f),
                text = "•••••••",
                multiline = false,
                imageWidth = 320.dp,
                imageHeight = 64.dp,
                savedBadgeText = stringResource(R.string.form_password_saved_badge),
            )
        }
    }
}
