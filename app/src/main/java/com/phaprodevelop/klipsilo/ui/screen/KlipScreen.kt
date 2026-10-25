package com.phaprodevelop.klipsilo.ui.screen

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.AsyncImage
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.clipListTitle
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.db.isPlainOrMarkdown
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.withDisplayBreaks
import com.phaprodevelop.klipsilo.ui.components.ButtonLabel
import com.phaprodevelop.klipsilo.ui.components.CLIP_TITLE_MIN_SCALE
import com.phaprodevelop.klipsilo.ui.components.ClipActionSheet
import com.phaprodevelop.klipsilo.ui.components.DialogTitleLabel
import com.phaprodevelop.klipsilo.ui.components.DisplayText
import com.phaprodevelop.klipsilo.ui.components.FILE_TO_PDF_EXTENSIONS
import com.phaprodevelop.klipsilo.ui.components.LARGE_FONT_SCALE
import com.phaprodevelop.klipsilo.ui.components.SwipeToDelete
import com.phaprodevelop.klipsilo.ui.components.clipInfo
import com.phaprodevelop.klipsilo.ui.components.fileConvertToPdfAction
import com.phaprodevelop.klipsilo.ui.components.fileExtensionOf
import com.phaprodevelop.klipsilo.ui.components.formatTimestamp
import com.phaprodevelop.klipsilo.ui.components.hapticClickable
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel
import com.phaprodevelop.klipsilo.withoutSoftHyphens
import java.io.File

@Composable
fun KlipScreen(
    viewModel: ClipboardViewModel,
    onOpenClip: (Long) -> Unit,
    onOpenPresentation: (Long) -> Unit = onOpenClip,
    onOcrImage: (Long) -> Unit,
    onAnnotatePdf: (Long) -> Unit,
    onFillForm: (Long) -> Unit,
    onEditPages: (Long) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val clips by viewModel.clips.collectAsState()
    val clipboardHasContent by viewModel.clipboardHasContent.collectAsState()
    val listState = rememberLazyListState()
    val scrollToTop by viewModel.scrollToTop.collectAsState()
    var handledScrollToTop by rememberSaveable { mutableStateOf(0L) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filter by remember { mutableStateOf(ClipFilter.ALL) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ClipboardEntry?>(null) }
    var sheetEntry by remember { mutableStateOf<ClipboardEntry?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameEntry by remember { mutableStateOf<ClipboardEntry?>(null) }
    var showLinksDialog by remember { mutableStateOf(false) }
    var linksEntry by remember { mutableStateOf<ClipboardEntry?>(null) }
    val isDownloading by viewModel.isDownloading.collectAsState()

    val filtered =
        remember(clips, searchQuery, filter) {
            filterClips(clips, searchQuery, filter)
        }

    LaunchedEffect(Unit) {
        viewModel.reloadClips()
        viewModel.refreshClipboardHasContent()
    }

    // The clipboard can change while the app is in the background. Android only
    // reports the listener while KlipSilo is focused, so re-check on every resume.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshClipboardHasContent()
    }

    LaunchedEffect(scrollToTop) {
        if (scrollToTop != 0L && scrollToTop != handledScrollToTop) {
            listState.scrollToItem(0)
            handledScrollToTop = scrollToTop
        }
    }

    if (isDownloading) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { DialogTitleLabel(context.getString(R.string.download_in_progress)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            },
        )
    }

    if (showDeleteConfirm && pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { DialogTitleLabel(context.getString(R.string.delete)) },
            text = {
                Text(context.resources.getQuantityString(R.plurals.delete_clip_count, 1, 1))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        val entry = pendingDelete!!
                        viewModel.deleteClipWithUndo(entry)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    ButtonLabel(context.getString(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    ButtonLabel(context.getString(R.string.cancel))
                }
            },
        )
    }

    sheetEntry?.let { entry ->
        val fileExt = fileExtensionOf(entry.filePath)
        ClipActionSheet(
            entry = entry,
            viewModel = viewModel,
            onView = { onOpenClip(entry.id) },
            onViewPresentation = { onOpenPresentation(entry.id) },
            onRename =
                if (entry.contentType == ContentType.LOCATION) {
                    null
                } else {
                    {
                        renameEntry = entry
                        showRenameDialog = true
                    }
                },
            onFindLinks =
                if (
                    entry.contentType == ContentType.TEXT ||
                    entry.contentType == ContentType.MARKDOWN ||
                    entry.contentType == ContentType.PDF
                ) {
                    {
                        linksEntry = entry
                        showLinksDialog = true
                    }
                } else {
                    null
                },
            onDownloadUrl =
                if (entry.contentType == ContentType.URL) {
                    { viewModel.downloadUrlClip(entry) }
                } else {
                    null
                },
            onOpenInBrowser =
                if (fileExt == "html" || fileExt == "htm") {
                    { viewModel.openFileInBrowser(entry) }
                } else {
                    null
                },
            onConvertToPdf =
                if (entry.contentType == ContentType.FILE) {
                    fileConvertToPdfAction(entry, viewModel)
                } else {
                    null
                },
            onConvertHtmlToPdf =
                if (
                    entry.contentType == ContentType.FILE &&
                    fileExt in setOf("html", "htm")
                ) {
                    { viewModel.convertHtmlToPdf(entry) }
                } else {
                    null
                },
            onTryAsText =
                if (
                    entry.contentType == ContentType.FILE &&
                    fileExt !in FILE_TO_PDF_EXTENSIONS
                ) {
                    { viewModel.convertFileToText(entry) }
                } else {
                    null
                },
            onDelete = {
                pendingDelete = entry
                showDeleteConfirm = true
            },
            onOcrImage =
                if (entry.contentType == ContentType.IMAGE) {
                    { onOcrImage(entry.id) }
                } else {
                    null
                },
            onAnnotatePdf =
                if (entry.contentType == ContentType.PDF) {
                    { onAnnotatePdf(entry.id) }
                } else {
                    null
                },
            onFillForm =
                if (entry.contentType == ContentType.PDF) {
                    { onFillForm(entry.id) }
                } else {
                    null
                },
            onEditPages =
                if (entry.contentType == ContentType.PDF) {
                    { onEditPages(entry.id) }
                } else {
                    null
                },
            onDismiss = { sheetEntry = null },
        )
    }

    if (showRenameDialog && renameEntry != null) {
        val entry = renameEntry!!
        val currentName = clipListTitle(entry.contentType, entry.previewText)
        var renameText by remember(entry) { mutableStateOf(currentName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { DialogTitleLabel(context.getString(R.string.rename)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    viewModel.renameClip(entry, renameText)
                }) {
                    ButtonLabel(context.getString(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    ButtonLabel(context.getString(R.string.cancel))
                }
            },
        )
    }

    if (showLinksDialog && linksEntry != null) {
        LinksDialog(
            entry = linksEntry!!,
            viewModel = viewModel,
            onDismiss = {
                showLinksDialog = false
                viewModel.clearPdfLinks()
            },
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
        ) {
            // #93: a truly empty library leads with the empty state and the +
            // FAB, so the big clipboard button keeps its demotion there.
            // #120: once there are clips, the button keeps its place even when
            // the clipboard is empty; it is disabled and says why, instead of
            // disappearing and shifting the list under the user.
            if (showSaveClipboardButton(clipboardHasContent, clips.size)) {
                Button(
                    onClick = {
                        ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                        viewModel.saveCurrentClipboard()
                    },
                    enabled = clipboardHasContent,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Icon(Icons.Filled.ContentPaste, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    // 3 lines at word boundaries instead of an ellipsis when even the
                    // floor font size cannot fit 2 lines at large font scales (#54, #45).
                    ButtonLabel(context.getString(R.string.save_current_clipboard), maxLines = 3)
                }
                if (!clipboardHasContent) {
                    ButtonLabel(
                        text = context.getString(R.string.clipboard_empty),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            CompactSearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = context.getString(R.string.search_hint),
                onClear = { searchQuery = "" },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            FilterRow(selected = filter, onSelect = { filter = it })

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                if (filtered.isEmpty()) {
                    EmptyListMessage(
                        listEmpty = clips.isEmpty(),
                        query = searchQuery,
                        filter = filter,
                        onClearSearch = { searchQuery = "" },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp),
                    ) {
                        items(filtered, key = { it.id }) { entry ->
                            val deleteWithUndo: () -> Unit = {
                                viewModel.deleteClipWithUndo(entry)
                            }
                            SwipeToDelete(
                                enabled = !entry.isLocked,
                                onDelete = deleteWithUndo,
                                onDeleteUndo = deleteWithUndo,
                            ) {
                                ClipItemRow(
                                    entry = entry,
                                    onClick = { openClip(entry, viewModel, onOpenClip, onOpenPresentation) },
                                    onDoubleTap =
                                        when (entry.contentType) {
                                            ContentType.URL -> ({ viewModel.openUrl(entry) })
                                            ContentType.LOCATION -> ({ viewModel.openLocation(entry) })
                                            else -> null
                                        },
                                    onTogglePin = { viewModel.togglePin(entry) },
                                    onOpenSheet = { sheetEntry = entry },
                                )
                            }
                        }
                    }
                }
            }
        }
        SmallFloatingActionButton(
            onClick = {
                ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                viewModel.createEmptyClip()
            },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .testTag("klips_new_text_clip_fab"),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = context.getString(R.string.new_text_clip).withoutSoftHyphens(),
            )
        }
    }
}

/**
 * #120: the Save clipboard button stays put once the library has clips, even
 * with an empty clipboard, so the list does not jump between visits. A truly
 * empty library keeps the #93 empty-state-first layout.
 */
internal fun showSaveClipboardButton(
    clipboardHasContent: Boolean,
    clipCount: Int,
): Boolean = clipboardHasContent || clipCount > 0

@Composable
private fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(40.dp),
        singleLine = true,
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.ContentCopy,
                    null,
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier =
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .hapticClickable(onClick = onClear),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = LocalContext.current.getString(R.string.clear).withoutSoftHyphens(),
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun EmptyListMessage(
    listEmpty: Boolean,
    query: String,
    filter: ClipFilter,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state = emptyListState(listEmpty = listEmpty, query = query, filter = filter)
    val title =
        when (state.title) {
            EmptyListTitle.NO_CLIPS -> stringResource(R.string.no_clips_yet)
            EmptyListTitle.SEARCH -> stringResource(R.string.search_no_results, query.trim())
            EmptyListTitle.FILTER -> stringResource(R.string.filter_no_results, filterChipLabel(context, filter))
        }
    val hint =
        when (state.hint) {
            EmptyListHint.NO_CLIPS -> stringResource(R.string.no_clips_description)
            EmptyListHint.CLEAR_SEARCH_OR_FILTER -> stringResource(R.string.empty_list_hint)
            EmptyListHint.TEXT -> stringResource(R.string.filter_text_hint)
            EmptyListHint.MARKDOWN -> stringResource(R.string.filter_markdown_hint)
            EmptyListHint.URL -> stringResource(R.string.filter_url_hint)
            EmptyListHint.IMAGE -> stringResource(R.string.filter_image_hint)
            EmptyListHint.PDF -> stringResource(R.string.filter_pdf_hint)
            EmptyListHint.FILE -> stringResource(R.string.filter_file_hint)
            EmptyListHint.LOCATION -> stringResource(R.string.filter_location_hint)
            EmptyListHint.LOCKED -> stringResource(R.string.filter_locked_hint)
        }
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (query.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onClearSearch) {
                ButtonLabel(context.getString(R.string.clear))
            }
        }
    }
}

private fun filterChipLabel(
    context: Context,
    filter: ClipFilter,
): String =
    when (filter) {
        ClipFilter.ALL -> context.getString(R.string.all)
        ClipFilter.TEXT -> context.getString(R.string.text)
        ClipFilter.MARKDOWN -> context.getString(R.string.markdown)
        ClipFilter.URL -> context.getString(R.string.url)
        ClipFilter.IMAGE -> context.getString(R.string.image)
        ClipFilter.PDF -> context.getString(R.string.pdf)
        ClipFilter.FILE -> context.getString(R.string.file)
        ClipFilter.LOCATION -> context.getString(R.string.location)
        ClipFilter.LOCKED -> context.getString(R.string.locked)
    }

@Composable
private fun FilterRow(
    selected: ClipFilter,
    onSelect: (ClipFilter) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val options =
        ClipFilter.entries.map { filter ->
            filter to
                when (filter) {
                    ClipFilter.ALL -> context.getString(R.string.all)
                    ClipFilter.TEXT -> context.getString(R.string.text)
                    ClipFilter.MARKDOWN -> context.getString(R.string.markdown)
                    ClipFilter.URL -> context.getString(R.string.url)
                    ClipFilter.IMAGE -> context.getString(R.string.image)
                    ClipFilter.PDF -> context.getString(R.string.pdf)
                    ClipFilter.FILE -> context.getString(R.string.file)
                    ClipFilter.LOCATION -> context.getString(R.string.location)
                    ClipFilter.LOCKED -> context.getString(R.string.locked)
                }
        }
    val scroll = rememberScrollState()
    // #123: < and > cues at the edges replace the old gradient fade. They sit
    // in a small gutter inside the scroll content and carry no pointer input,
    // so they never take the tap or the horizontal scroll gesture. Each cue
    // has a background pill so it stays legible over a clipped chip.
    val canScrollLeft = scroll.value > 0
    val canScrollRight = scroll.maxValue != Int.MAX_VALUE && scroll.value < scroll.maxValue
    Box(Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scroll)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { (filter, label) ->
                val active = selected == filter
                Card(
                    onClick = {
                        ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                        onSelect(if (active) ClipFilter.ALL else filter)
                    },
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                if (active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                        ),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color =
                            if (active) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
        if (canScrollLeft) {
            FilterScrollCue("<", Modifier.align(Alignment.CenterStart))
        }
        if (canScrollRight) {
            FilterScrollCue(">", Modifier.align(Alignment.CenterEnd))
        }
    }
}

/** Decorative scroll cue for the filter pill row (#123); never read aloud. */
@Composable
private fun FilterScrollCue(
    cue: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = cue,
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.background, CircleShape)
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .clearAndSetSemantics { },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

internal enum class ClipOpenTarget {
    DETAIL,
    PRESENTATION,
    URL,
    LOCATION,
}

/** Where a single tap on a list row goes. The action sheet stays on long-press and ⋮. */
internal fun defaultClipOpen(type: ContentType): ClipOpenTarget =
    when (type) {
        ContentType.TEXT, ContentType.MARKDOWN -> ClipOpenTarget.DETAIL
        ContentType.URL -> ClipOpenTarget.URL
        ContentType.LOCATION -> ClipOpenTarget.LOCATION
        ContentType.PDF, ContentType.IMAGE, ContentType.FILE -> ClipOpenTarget.DETAIL
    }

private fun openClip(
    entry: ClipboardEntry,
    viewModel: ClipboardViewModel,
    onOpenClip: (Long) -> Unit,
    onOpenPresentation: (Long) -> Unit,
) {
    when (defaultClipOpen(entry.contentType)) {
        ClipOpenTarget.PRESENTATION -> onOpenPresentation(entry.id)
        ClipOpenTarget.URL -> viewModel.openUrl(entry)
        ClipOpenTarget.LOCATION -> viewModel.openLocation(entry)
        ClipOpenTarget.DETAIL -> onOpenClip(entry.id)
    }
}

/** One fixed-width leading slot for every clip row so all titles share a left edge. */
internal val LeadingSlotSize = 48.dp

/** Icon centred inside the slot: same slot width for icons and thumbnails (#37). */
internal val LeadingIconSize = 24.dp

/** Identical gap between the leading slot and the text column in every row type. */
internal val LeadingSlotSpacing = 12.dp

/** Narrower leading slot for the reflowed clip row at large font scale (#54). */
internal val CompactLeadingSlotSize = 32.dp

/** Smaller leading icon inside the compact slot (#54). */
internal val CompactLeadingIconSize = 20.dp

/** Gap between the leading slot and the text column in the compact row (#54). */
internal val CompactLeadingSlotSpacing = 8.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClipItemRow(
    entry: ClipboardEntry,
    onClick: () -> Unit,
    onDoubleTap: (() -> Unit)? = null,
    onTogglePin: () -> Unit,
    onOpenSheet: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    // Reflow the row at large font scales (#54): the side-by-side layout squeezes
    // the title down to a few characters and wraps the meta line word by word.
    val compact = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    val openRow = {
        ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
        onClick()
    }
    val openMenu = {
        ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.LONG_PRESS)
        onOpenSheet()
    }
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .combinedClickable(onClick = openRow, onLongClick = openMenu),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = if (compact) Alignment.Top else Alignment.CenterVertically,
        ) {
            val slotSize = if (compact) CompactLeadingSlotSize else LeadingSlotSize
            val iconSize = if (compact) CompactLeadingIconSize else LeadingIconSize
            val hasThumbnail = entry.contentType == ContentType.IMAGE && entry.filePath != null
            Box(
                modifier =
                    Modifier
                        .size(slotSize)
                        .then(
                            if (onDoubleTap != null) {
                                Modifier.pointerInput(entry.id) {
                                    detectTapGestures(
                                        onTap = { openRow() },
                                        onDoubleTap = { onDoubleTap() },
                                        onLongPress = { openMenu() },
                                    )
                                }
                            } else {
                                Modifier
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                if (hasThumbnail) {
                    AsyncImage(
                        model = File(entry.filePath),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(slotSize)
                                .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = typeIcon(entry.contentType),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.width(if (compact) CompactLeadingSlotSpacing else LeadingSlotSpacing))
            // Display only (#54): the filename title gets zero-width spaces
            // after `_`, `-`, `.` and `/` so it wraps between parts instead
            // of mid-word. The stored title, rename, copy and share keep the
            // raw text.
            val title =
                withDisplayBreaks(
                    clipListTitle(entry.contentType, entry.previewText)
                        .ifBlank { context.getString(R.string.untitled_clip) },
                )
            val info = remember(entry.filePath) { clipInfo(context, entry) }
            if (compact) {
                Column(Modifier.weight(1f)) {
                    // Two lines at the body size. A title that needs a third line
                    // ends in an ellipsis; it is not shrunk onto one line (#54).
                    ButtonLabel(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        floorFraction = CLIP_TITLE_MIN_SCALE,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Type, size and age share one line that is never broken
                        // per word — the least important tail is ellipsized (#54).
                        Text(
                            text = "$info • ${formatTimestamp(context, entry.timestamp)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        CompositionLocalProvider(
                            LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
                        ) {
                            if (entry.isPinned) {
                                IconButton(
                                    onClick = {
                                        ViewCompat.performHapticFeedback(
                                            view,
                                            HapticFeedbackConstantsCompat.CONFIRM,
                                        )
                                        onTogglePin()
                                    },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.PushPin,
                                        contentDescription =
                                            context.getString(
                                                R.string.pin_or_unpin,
                                            ).withoutSoftHyphens(),
                                        tint = Color(0xFF4CAF50),
                                    )
                                }
                            }
                            if (entry.isLocked) {
                                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.Lock,
                                        contentDescription =
                                            context.getString(
                                                R.string.lock_or_unlock,
                                            ).withoutSoftHyphens(),
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.width(4.dp))
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    IconButton(
                        onClick = openMenu,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = context.getString(R.string.clip_more_actions).withoutSoftHyphens(),
                        )
                    }
                }
            } else {
                Column(Modifier.weight(1f)) {
                    // #64: the display title carries the zero-width spaces —
                    // TalkBack reads the clean source via DisplayText (the
                    // compact branch above gets the same through ButtonLabel).
                    DisplayText(
                        text = title,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                    Text(
                        text = info,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatTimestamp(context, entry.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (entry.isPinned) {
                            IconButton(
                                onClick = {
                                    ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                                    onTogglePin()
                                },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    Icons.Filled.PushPin,
                                    contentDescription = context.getString(R.string.pin_or_unpin).withoutSoftHyphens(),
                                    tint = Color(0xFF4CAF50),
                                )
                            }
                        }
                        if (entry.isLocked) {
                            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription =
                                        context.getString(
                                            R.string.lock_or_unlock,
                                        ).withoutSoftHyphens(),
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
                                onOpenSheet()
                            },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = context.getString(R.string.clip_more_actions).withoutSoftHyphens(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun typeIcon(type: ContentType) =
    when (type) {
        ContentType.TEXT -> Icons.Filled.TextFields
        ContentType.MARKDOWN -> Icons.Filled.Code
        ContentType.URL -> Icons.Filled.Link
        ContentType.IMAGE -> Icons.Filled.Image
        ContentType.PDF -> Icons.Filled.Description
        ContentType.FILE -> Icons.Filled.InsertDriveFile
        ContentType.LOCATION -> Icons.Filled.MyLocation
    }

@Composable
private fun LinksDialog(
    entry: ClipboardEntry,
    viewModel: ClipboardViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val callLabel = context.getString(R.string.call)
    val openLabel = context.getString(R.string.open_link)
    var textBody by remember(entry.id) { mutableStateOf<String?>(null) }
    val textItems =
        remember(entry.id, textBody) {
            if (entry.contentType.isPlainOrMarkdown) {
                extractLinks(textBody.orEmpty(), callLabel, openLabel)
            } else {
                emptyList()
            }
        }
    val pdfScanning by viewModel.pdfLinksScanning.collectAsState()
    val pdfItems by viewModel.pdfLinks.collectAsState()

    LaunchedEffect(entry.id) {
        when (entry.contentType) {
            ContentType.TEXT, ContentType.MARKDOWN -> textBody = viewModel.readTextBody(entry)
            ContentType.PDF -> viewModel.scanPdfLinks(entry)
            else -> Unit
        }
    }

    val isPdf = entry.contentType == ContentType.PDF
    val loading = isPdf && pdfScanning
    val items: List<String> =
        if (isPdf) {
            pdfItems.orEmpty().map { if (it.isPhone) "$callLabel ${it.value}" else "$openLabel ${it.value}" }
        } else {
            textItems
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitleLabel(context.getString(R.string.links)) },
        text = {
            when {
                loading -> {
                    Text(
                        text = context.getString(R.string.analyzing),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                items.isEmpty() -> {
                    Text(
                        text = context.getString(R.string.no_links_found),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> {
                    Column {
                        items.forEach { item ->
                            Text(
                                text = item,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onDismiss()
                                            if (item.startsWith(callLabel)) {
                                                viewModel.openPhone(item.removePrefix(callLabel).trim())
                                            } else {
                                                viewModel.openUrlText(item.removePrefix(openLabel).trim())
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                            )
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

private fun extractLinks(
    text: String,
    callLabel: String,
    openLabel: String,
): List<String> {
    val result = mutableListOf<String>()
    val phonePattern = Regex("(\\+?\\d[\\d\\s]{6,}\\d)")
    phonePattern.findAll(text).forEach { result.add("$callLabel ${it.value.trim()}") }
    val urlMatcher = android.util.Patterns.WEB_URL.matcher(text)
    while (urlMatcher.find()) {
        result.add("$openLabel ${urlMatcher.group()}")
    }
    return result.distinct()
}

@Preview(name = "clip rows - normal scale", widthDp = 360, showBackground = true)
@Composable
private fun ClipRowsNormalPreview() {
    MaterialTheme {
        Column(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ClipItemRow(
                entry = previewEntry(isPinned = true, isLocked = true),
                onClick = {},
                onTogglePin = {},
                onOpenSheet = {},
            )
            ClipItemRow(
                entry = previewEntry(title = "Kort titel"),
                onClick = {},
                onTogglePin = {},
                onOpenSheet = {},
            )
        }
    }
}

@Preview(name = "clip rows - large font", fontScale = 2f, widthDp = 320, showBackground = true)
@Preview(name = "clip rows - large font 360", fontScale = 2f, widthDp = 360, showBackground = true)
@Composable
private fun ClipRowsLargeFontPreview() {
    MaterialTheme {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Icon(Icons.Filled.ContentPaste, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                ButtonLabel("Aktuelle Zwischenablage speichern", maxLines = 3)
            }
            Spacer(Modifier.height(8.dp))
            ClipItemRow(
                entry = previewEntry(isPinned = true, isLocked = true),
                onClick = {},
                onTogglePin = {},
                onOpenSheet = {},
            )
            ClipItemRow(
                entry = previewEntry(title = "Event_Registration_Form"),
                onClick = {},
                onTogglePin = {},
                onOpenSheet = {},
            )
            ClipItemRow(
                entry = previewEntry(title = "HTML from Weekend i København"),
                onClick = {},
                onTogglePin = {},
                onOpenSheet = {},
            )
        }
    }
}

/** A long-named file clip for the row previews. */
private fun previewEntry(
    title: String = "Langt_dokument_navn_der_ikke_plads",
    isPinned: Boolean = false,
    isLocked: Boolean = false,
): ClipboardEntry =
    ClipboardEntry(
        id = 1,
        contentType = ContentType.FILE,
        filePath = "missing/preview.pdf",
        previewText = title,
        timestamp = 0,
        isPinned = isPinned,
        isLocked = isLocked,
    )
