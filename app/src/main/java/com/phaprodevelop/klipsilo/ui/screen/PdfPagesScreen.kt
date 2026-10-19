package com.phaprodevelop.klipsilo.ui.screen

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.ui.components.ButtonLabel
import com.phaprodevelop.klipsilo.ui.components.DialogTitleLabel
import com.phaprodevelop.klipsilo.ui.components.FitTopBar
import com.phaprodevelop.klipsilo.ui.components.pdfPageCount
import com.phaprodevelop.klipsilo.ui.components.produceLatest
import com.phaprodevelop.klipsilo.ui.components.rememberBitmapRecycler
import com.phaprodevelop.klipsilo.ui.components.rememberDisplayedBitmap
import com.phaprodevelop.klipsilo.ui.components.renderPdfPage
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel
import com.phaprodevelop.klipsilo.withoutSoftHyphens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

private data class PdfPageEntry(
    val originalIndex: Int,
    val bitmap: Bitmap?,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PdfPagesScreen(
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
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var pages by remember { mutableStateOf(listOf<PdfPageEntry>()) }
    var originalOrder by remember { mutableStateOf(listOf<Int>()) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var previewOriginalIndex by remember { mutableStateOf<Int?>(null) }
    val pageRecycler = rememberBitmapRecycler()
    val pageGate = remember { AtomicInteger(0) }
    val filePath = entry?.filePath

    val navigateBack by viewModel.navigateBack.collectAsState()
    LaunchedEffect(navigateBack) {
        if (navigateBack) {
            viewModel.clearNavigateBack()
            onNavigateBack()
        }
    }

    fun replacePages(next: List<PdfPageEntry>) {
        val previous = pages
        pages = next
        previous.forEach { entry ->
            if (next.none { it.bitmap === entry.bitmap }) {
                pageRecycler.retire(entry.bitmap)
            }
        }
    }

    LaunchedEffect(filePath, lookupDone) {
        if (filePath == null) {
            if (lookupDone) {
                loadFailed = true
                loading = false
            }
            return@LaunchedEffect
        }
        val rendered =
            produceLatest(
                pageGate,
                produce = {
                    loading = true
                    loadFailed = false
                    withContext(Dispatchers.IO) {
                        val count = pdfPageCount(filePath)
                        if (count <= 0) {
                            emptyList()
                        } else {
                            (0 until count).map { i -> PdfPageEntry(i, renderPdfPage(filePath, 400, i)) }
                        }
                    }
                },
                discard = { entries -> entries.forEach { it.bitmap?.recycle() } },
            ) ?: return@LaunchedEffect
        val entries = rendered.value
        if (entries.isEmpty()) {
            originalOrder = emptyList()
            replacePages(emptyList())
            loadFailed = true
            loading = false
            return@LaunchedEffect
        }
        originalOrder = entries.map { it.originalIndex }
        replacePages(entries)
        loading = false
    }

    DisposableEffect(Unit) {
        onDispose {
            pages.forEach { pageRecycler.retire(it.bitmap, notify = false) }
        }
    }

    val hasChanges = !loading && pages.map { it.originalIndex } != originalOrder

    fun handleBack() {
        if (hasChanges) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    fun moveUp(index: Int) {
        if (index <= 0) return
        val list = pages.toMutableList()
        val item = list.removeAt(index)
        list.add(index - 1, item)
        pages = list
    }

    fun moveDown(index: Int) {
        if (index >= pages.size - 1) return
        val list = pages.toMutableList()
        val item = list.removeAt(index)
        list.add(index + 1, item)
        pages = list
    }

    BackHandler { handleBack() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            FitTopBar(
                title = context.getString(R.string.edit_pages),
                saveLabel = context.getString(R.string.save_pdf),
                saveEnabled = pages.isNotEmpty() && filePath != null,
                onBack = { handleBack() },
                onSave = {
                    entry?.let {
                        if (hasChanges) {
                            viewModel.savePdfPages(it, pages.map { p -> p.originalIndex })
                        } else {
                            // Save with no changes (#60): just leave, like Back,
                            // instead of copying the unchanged PDF.
                            onNavigateBack()
                        }
                    }
                },
                backContentDescription = context.getString(R.string.back),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !lookupDone && entry == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                entry == null || loadFailed -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text =
                                    context.getString(
                                        if (entry == null || filePath == null) {
                                            R.string.clip_not_found
                                        } else {
                                            R.string.file_cannot_open
                                        },
                                    ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.size(16.dp))
                            TextButton(onClick = onNavigateBack) {
                                ButtonLabel(context.getString(R.string.back))
                            }
                        }
                    }
                }
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        Text(
                            text = context.getString(R.string.pages_count, pages.size, originalOrder.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        LazyColumn(Modifier.fillMaxSize()) {
                            itemsIndexed(pages, key = { _, p -> p.originalIndex }) { index, page ->
                                PageRow(
                                    entry = page,
                                    pageNumber = page.originalIndex + 1,
                                    canMoveUp = index > 0,
                                    canMoveDown = index < pages.size - 1,
                                    onMoveUp = { moveUp(index) },
                                    onMoveDown = { moveDown(index) },
                                    onDelete = {
                                        val removed = pages.getOrNull(index)
                                        pages = pages.filterIndexed { i, _ -> i != index }
                                        pageRecycler.retire(removed?.bitmap)
                                    },
                                    onPreview = { previewOriginalIndex = page.originalIndex },
                                )
                            }
                        }
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
                    TextButton(onClick = { onNavigateBack() }) {
                        ButtonLabel(context.getString(R.string.discard))
                    }
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            // Same as "Save PDF": savePdfPages navigates back itself when done.
                            entry?.let {
                                viewModel.savePdfPages(it, pages.map { page -> page.originalIndex })
                            }
                        },
                    ) {
                        ButtonLabel(context.getString(R.string.save))
                    }
                }
            },
            dismissButton = {},
        )
    }

    previewOriginalIndex?.let { previewIndex ->
        PagePreviewDialog(
            filePath = filePath,
            originalIndex = previewIndex,
            onDismiss = { previewOriginalIndex = null },
        )
    }
}

@Composable
private fun PageRow(
    entry: PdfPageEntry,
    pageNumber: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onPreview: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        entry.bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(width = 110.dp, height = 147.dp)
                        .clickable(onClick = onPreview),
                contentScale = ContentScale.Fit,
            )
        } ?: Box(Modifier.size(width = 110.dp, height = 147.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = context.getString(R.string.page_label, pageNumber),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = {
            ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
            onMoveUp()
        }, enabled = canMoveUp) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = context.getString(R.string.move_up).withoutSoftHyphens(),
            )
        }
        IconButton(onClick = {
            ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
            onMoveDown()
        }, enabled = canMoveDown) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = context.getString(R.string.move_down).withoutSoftHyphens(),
            )
        }
        IconButton(onClick = {
            ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
            onDelete()
        }) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = context.getString(R.string.delete_page).withoutSoftHyphens(),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun PagePreviewDialog(
    filePath: String?,
    originalIndex: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val displayed = rememberDisplayedBitmap()
    val pageGate = remember { AtomicInteger(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(filePath, originalIndex) {
        val path = filePath ?: return@LaunchedEffect
        val loaded =
            produceLatest(
                pageGate,
                produce = { withContext(Dispatchers.IO) { renderPdfPage(path, 1600, originalIndex) } },
                discard = { bitmap -> bitmap?.recycle() },
            ) ?: return@LaunchedEffect
        displayed.update(loaded.value)
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .background(Color.Black.copy(alpha = 0.9f)),
        ) {
            val bmp = displayed.bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = context.getString(R.string.page_label, originalIndex + 1).withoutSoftHyphens(),
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
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
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
                    // #81: intentional White — full-screen page preview, the
                    // close floats over the page on a black scrim.
                    tint = Color.White,
                )
            }
            Text(
                text = context.getString(R.string.page_label, originalIndex + 1),
                // #81: intentional White, same black scrim as the close above.
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
            )
        }
    }
}
