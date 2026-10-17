package com.phaprodevelop.klipsilo.ui.screen

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.repository.FormFieldHit
import com.phaprodevelop.klipsilo.data.repository.FormFieldInfo
import com.phaprodevelop.klipsilo.data.repository.FormFieldType
import com.phaprodevelop.klipsilo.ui.components.PageSelector
import com.phaprodevelop.klipsilo.ui.components.pdfPageCount
import com.phaprodevelop.klipsilo.ui.components.produceLatest
import com.phaprodevelop.klipsilo.ui.components.rememberDisplayedBitmap
import com.phaprodevelop.klipsilo.ui.components.renderPdfPage
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
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
    val scope = rememberCoroutineScope()
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var anchors by remember { mutableStateOf(mapOf<String, Int>()) }
    var focusedField by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)

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
        val y = anchors[name] ?: return@LaunchedEffect
        scroll.animateScrollTo(y)
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
            if (field.fullyQualifiedName !in values) {
                val newValue =
                    when (field.type) {
                        FormFieldType.CHECKBOX -> {
                            val v = field.currentValue
                            if (v != null && v.isNotBlank() && v != "Off") "true" else "false"
                        }
                        else -> field.currentValue ?: ""
                    }
                values = values + (field.fullyQualifiedName to newValue)
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
        val path = filePath ?: return@LaunchedEffect
        val loaded =
            produceLatest(
                pageGate,
                produce = { withContext(Dispatchers.IO) { renderPdfPage(path, 1800, pageIndex) } },
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
        val field = formFields.orEmpty().firstOrNull { it.fullyQualifiedName == name }
        field?.hits?.firstOrNull()?.pageIndex?.let { page -> pageIndex = page }
        focusedField = name
        val requester = focusRequesters[name] ?: return
        scope.launch {
            runCatching { requester.requestFocus() }
            anchors[name]?.let { y -> scroll.animateScrollTo(y) }
        }
    }

    BackHandler { leave() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(context.getString(R.string.fill_form)) },
                navigationIcon = {
                    IconButton(onClick = { leave() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = context.getString(R.string.back))
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.fillForm(entry, values) },
                        enabled = entry.filePath != null && !isConverting,
                    ) {
                        Text(context.getString(R.string.save_pdf))
                    }
                },
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
                    onFieldTap = { name -> focusField(name) },
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
                        formFields.orEmpty().forEach { field ->
                            val name = field.fullyQualifiedName
                            val requester = focusRequesters.getOrPut(name) { FocusRequester() }
                            FormFieldRow(
                                field = field,
                                value = values[name].orEmpty(),
                                focusRequester = requester,
                                onValueChange = {
                                    values = values + (name to it)
                                    dirty = true
                                },
                                onFocused = {
                                    focusedField = name
                                    field.hits.firstOrNull()?.pageIndex?.let { page -> pageIndex = page }
                                    scope.launch {
                                        anchors[name]?.let { y -> scroll.animateScrollTo(y) }
                                    }
                                },
                                onPositioned = { y ->
                                    if (anchors[name] != y) anchors = anchors + (name to y)
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
            title = { Text(context.getString(R.string.discard_changes)) },
            text = { Text(context.getString(R.string.discard_changes_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    viewModel.clearFormFields()
                    onNavigateBack()
                }) {
                    Text(context.getString(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun FormPdfPreview(
    bitmap: Bitmap,
    pageIndex: Int,
    fields: List<FormFieldInfo>,
    values: Map<String, String>,
    onFieldTap: (String) -> Unit,
) {
    val context = LocalContext.current
    val previewHeight = LocalConfiguration.current.screenHeightDp.dp * 0.40f
    Box(
        modifier = Modifier.fillMaxWidth().height(previewHeight),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val aspect = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
            val boxAspect = if (maxHeight > 0.dp) maxWidth / maxHeight else aspect
            val imageWidth = if (aspect > boxAspect) maxWidth else maxHeight * aspect
            val imageHeight = if (aspect == 0f) maxHeight else imageWidth / aspect
            Box(
                modifier =
                    Modifier
                        .size(imageWidth, imageHeight)
                        .align(Alignment.Center)
                        .pointerInput(fields, pageIndex) {
                            detectTapGestures { offset ->
                                val name =
                                    hitField(
                                        fields,
                                        pageIndex,
                                        offset.x,
                                        offset.y,
                                        size.width.toFloat(),
                                        size.height.toFloat(),
                                    )
                                if (name != null) onFieldTap(name)
                            }
                        },
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = context.getString(R.string.pdf),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
                fields.forEach { field ->
                    val value = values[field.fullyQualifiedName].orEmpty()
                    field.hits.filter { it.pageIndex == pageIndex }.forEach { hit ->
                        val shown = formOverlayText(field, hit, value)
                        if (shown.isEmpty()) return@forEach
                        FormValueOverlay(hit, shown, imageWidth, imageHeight)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormValueOverlay(
    hit: FormFieldHit,
    text: String,
    imageWidth: Dp,
    imageHeight: Dp,
) {
    val width = imageWidth * (hit.right - hit.left).coerceAtLeast(0f)
    val height = imageHeight * (hit.bottom - hit.top).coerceAtLeast(0f)
    val fontSize =
        with(LocalDensity.current) {
            (height.toPx() * 0.62f).toSp()
        }
    Box(
        modifier =
            Modifier
                .offset(x = imageWidth * hit.left, y = imageHeight * hit.top)
                .size(width, height)
                .clipToBounds()
                .clearAndSetSemantics { },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = fontSize,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun FormFieldRow(
    field: FormFieldInfo,
    value: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onFocused: () -> Unit,
    onPositioned: (Int) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .onGloballyPositioned { coords ->
                    onPositioned(coords.positionInParent().y.roundToInt())
                },
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
                    Text(field.label, style = MaterialTheme.typography.bodyLarge)
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
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text(field.label) },
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
                bestName = field.fullyQualifiedName
            }
        }
    }
    return bestName
}
