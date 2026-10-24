package com.phaprodevelop.klipsilo.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.clipPickerTitle
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.hasLocationPermission
import com.phaprodevelop.klipsilo.isLocationEnabled
import com.phaprodevelop.klipsilo.markLocationPermissionAsked
import com.phaprodevelop.klipsilo.ui.components.ButtonLabel
import com.phaprodevelop.klipsilo.ui.components.DialogTitleLabel
import com.phaprodevelop.klipsilo.ui.components.hapticClickable
import com.phaprodevelop.klipsilo.ui.components.typeName
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel
import com.phaprodevelop.klipsilo.wasLocationPermissionAsked
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaerktøjerScreen(
    viewModel: ClipboardViewModel,
    onNavigateToScanner: () -> Unit = {},
    onNavigateToOcr: () -> Unit = {},
    onNavigateToClips: () -> Unit = {},
) {
    val context = LocalContext.current
    val clips by viewModel.clips.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCombineDialog by remember { mutableStateOf(false) }
    var showCombineTextsDialog by remember { mutableStateOf(false) }
    var showDeleteTypeDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingDeleteType by remember { mutableStateOf<ContentType?>(null) }
    var showBackupWarning by remember { mutableStateOf(false) }
    var showLocationDisclosure by remember { mutableStateOf(false) }
    var showLocationDisabled by remember { mutableStateOf(false) }
    var showLocationSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val activity = context as? Activity

    fun startLocationCapture() {
        if (!hasLocationPermission(context)) {
            val asked = wasLocationPermissionAsked(context)
            val rationale =
                activity?.shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) == true ||
                    activity?.shouldShowRequestPermissionRationale(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ) == true
            if (asked && !rationale) {
                showLocationSettings = true
            } else {
                showLocationDisclosure = true
            }
            return
        }
        if (!isLocationEnabled(context)) {
            showLocationDisabled = true
            return
        }
        viewModel.saveCurrentLocation()
    }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { grants ->
            val granted =
                grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                if (!isLocationEnabled(context)) {
                    showLocationDisabled = true
                } else {
                    viewModel.saveCurrentLocation()
                }
            } else {
                val rationale =
                    activity?.shouldShowRequestPermissionRationale(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    ) == true ||
                        activity?.shouldShowRequestPermissionRationale(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ) == true
                if (!rationale && wasLocationPermissionAsked(context)) {
                    showLocationSettings = true
                }
            }
        }

    val backupFileName =
        remember {
            "KlipSilo_" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".zip"
        }
    val backupLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip"),
        ) { uri ->
            uri?.let { viewModel.backupClips(it) }
        }
    val restoreLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments(),
        ) { uris ->
            if (uris.isNotEmpty()) {
                viewModel.restoreClips(uris)
            }
        }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { scaffoldPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 12.dp)
                    .padding(horizontal = 12.dp),
        ) {
            Text(
                text = context.getString(R.string.tools),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = context.getString(R.string.tools_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.size(16.dp))

            ToolGroup(title = context.getString(R.string.tool_group_create)) {
                ToolRow(Icons.Filled.TextFields, context.getString(R.string.tool_create_text)) {
                    viewModel.createEmptyClip()
                }
                ToolRow(Icons.Filled.MyLocation, context.getString(R.string.tool_location)) {
                    startLocationCapture()
                }
            }

            Spacer(Modifier.size(16.dp))

            ToolGroup(title = context.getString(R.string.tool_group_convert)) {
                ToolRow(Icons.Filled.PictureAsPdf, context.getString(R.string.tool_combine)) {
                    showCombineDialog = true
                }
                ToolRow(Icons.Filled.TextFields, context.getString(R.string.tool_combine_texts)) {
                    showCombineTextsDialog = true
                }
            }

            Spacer(Modifier.size(16.dp))

            ToolGroup(title = context.getString(R.string.tool_group_ocr)) {
                ToolRow(
                    icon = Icons.Filled.QrCodeScanner,
                    title = context.getString(R.string.tool_qr),
                ) { onNavigateToScanner() }
                ToolRow(
                    icon = Icons.Filled.Image,
                    title = context.getString(R.string.tool_ocr_camera),
                ) { onNavigateToOcr() }
            }

            Spacer(Modifier.size(16.dp))

            ToolGroup(title = context.getString(R.string.tool_group_maintenance)) {
                ToolRow(Icons.Filled.Archive, context.getString(R.string.maintenance_backup)) {
                    showBackupWarning = true
                }
                ToolRow(Icons.Filled.Unarchive, context.getString(R.string.maintenance_restore)) {
                    restoreLauncher.launch(arrayOf("application/zip"))
                }
                ToolRow(Icons.Filled.Delete, context.getString(R.string.maintenance_delete)) {
                    showDeleteTypeDialog = true
                }
            }
        }
    }

    if (showLocationDisclosure) {
        AlertDialog(
            onDismissRequest = { showLocationDisclosure = false },
            title = { DialogTitleLabel(context.getString(R.string.location_disclosure_title)) },
            text = { Text(context.getString(R.string.location_disclosure_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationDisclosure = false
                        markLocationPermissionAsked(context)
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    },
                ) {
                    ButtonLabel(context.getString(R.string.continue_label))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDisclosure = false }) {
                    ButtonLabel(context.getString(R.string.cancel))
                }
            },
        )
    }

    if (showLocationDisabled) {
        AlertDialog(
            onDismissRequest = { showLocationDisabled = false },
            title = { DialogTitleLabel(context.getString(R.string.location_disabled_title)) },
            text = { Text(context.getString(R.string.location_disabled_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationDisabled = false
                        try {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } catch (_: Exception) {
                        }
                    },
                ) {
                    ButtonLabel(context.getString(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDisabled = false }) {
                    ButtonLabel(context.getString(R.string.cancel))
                }
            },
        )
    }

    if (showLocationSettings) {
        AlertDialog(
            onDismissRequest = { showLocationSettings = false },
            title = { DialogTitleLabel(context.getString(R.string.location_permission_denied_title)) },
            text = { Text(context.getString(R.string.location_permission_denied_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationSettings = false
                        try {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null),
                                ),
                            )
                        } catch (_: Exception) {
                        }
                    },
                ) {
                    ButtonLabel(context.getString(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationSettings = false }) {
                    ButtonLabel(context.getString(R.string.cancel))
                }
            },
        )
    }

    if (showBackupWarning) {
        AlertDialog(
            onDismissRequest = { showBackupWarning = false },
            title = { DialogTitleLabel(context.getString(R.string.maintenance_backup)) },
            text = { Text(context.getString(R.string.backup_unencrypted_warning)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackupWarning = false
                        backupLauncher.launch(backupFileName)
                    },
                ) {
                    ButtonLabel(context.getString(R.string.backup_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupWarning = false }) {
                    ButtonLabel(context.getString(R.string.cancel))
                }
            },
        )
    }

    if (showCombineDialog) {
        val combinable =
            clips.filter {
                it.contentType == ContentType.PDF || it.contentType == ContentType.IMAGE
            }
        val excludedFromPdf =
            clips.any {
                it.contentType != ContentType.PDF && it.contentType != ContentType.IMAGE
            }
        CombineDialog(
            title = context.getString(R.string.tool_combine),
            entries = combinable,
            hint =
                if (excludedFromPdf) {
                    context.getString(R.string.combine_pdf_images_only)
                } else {
                    null
                },
            onDismiss = { showCombineDialog = false },
            onConfirm = { selected ->
                showCombineDialog = false
                viewModel.combineToPdf(selected)
            },
        )
    }

    if (showCombineTextsDialog) {
        val textClips = clips.filter { it.contentType == ContentType.TEXT }
        CombineDialog(
            title = context.getString(R.string.tool_combine_texts),
            entries = textClips,
            onDismiss = { showCombineTextsDialog = false },
            onConfirm = { selected ->
                showCombineTextsDialog = false
                viewModel.combineTextClips(selected)
                onNavigateToClips()
            },
        )
    }

    if (showDeleteTypeDialog) {
        DeleteTypeDialog(
            clips = clips,
            onPick = { type ->
                showDeleteTypeDialog = false
                pendingDeleteType = type
                showDeleteConfirm = true
            },
            onUnlock = {
                showDeleteTypeDialog = false
                val n = viewModel.unlockAllClips()
                if (n > 0) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.clips_unlocked, n),
                        )
                    }
                }
            },
            onDismiss = { showDeleteTypeDialog = false },
        )
    }

    if (showDeleteConfirm) {
        val type = pendingDeleteType
        val count =
            if (type == null) {
                clips.count { !it.isLocked }
            } else {
                clips.count { it.contentType == type && !it.isLocked }
            }
        val message =
            if (type == null) {
                context.getString(R.string.delete_all_confirm_count, count)
            } else {
                context.getString(R.string.delete_type_confirm, count, typeName(context, type))
            }
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { DialogTitleLabel(context.getString(R.string.delete)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    val deletedCount =
                        if (type == null) {
                            viewModel.deleteAllClipsWithUndo()
                        } else {
                            viewModel.deleteClipsByType(type)
                        }
                    if (deletedCount == 0) return@TextButton
                }) {
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

    val isLocating by viewModel.isLocating.collectAsState()
    if (isLocating) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelLocating() },
            title = { DialogTitleLabel(context.getString(R.string.location_getting_title)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(context.getString(R.string.location_getting_body))
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.cancelLocating() }) {
                    ButtonLabel(context.getString(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ColumnScope.ToolGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.size(4.dp))
    content()
}

@Composable
private fun ToolRow(
    icon: ImageVector,
    title: String,
    note: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .then(
                    if (onClick != null) {
                        Modifier.hapticClickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                // Titles and notes wrap only at word boundaries at large font
                // scales (#45): "Clips wiederherstellen" broke as
                // "wiederherstelle/n" in a plain soft-wrapping Text.
                ButtonLabel(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                )
                if (note != null) {
                    ButtonLabel(
                        text = note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
            if (note == null && onClick != null) {
                Text(
                    text = "›",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DeleteTypeDialog(
    clips: List<ClipboardEntry>,
    onPick: (ContentType?) -> Unit,
    onUnlock: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lockedCount = clips.count { it.isLocked }
    val options =
        listOf<Pair<ContentType?, String>>(
            null to context.getString(R.string.all),
            ContentType.TEXT to typeName(context, ContentType.TEXT),
            ContentType.URL to typeName(context, ContentType.URL),
            ContentType.IMAGE to typeName(context, ContentType.IMAGE),
            ContentType.PDF to typeName(context, ContentType.PDF),
            ContentType.FILE to typeName(context, ContentType.FILE),
            ContentType.LOCATION to typeName(context, ContentType.LOCATION),
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitleLabel(context.getString(R.string.maintenance_delete)) },
        text = {
            Column {
                val lockedEnabled = lockedCount > 0
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = lockedEnabled) { onUnlock() }
                            .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        null,
                        Modifier.size(24.dp),
                        tint =
                            if (lockedEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = context.getString(R.string.locked_clips),
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (lockedEnabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "$lockedCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                options.forEach { (type, label) ->
                    val count =
                        if (type == null) {
                            clips.count { !it.isLocked }
                        } else {
                            clips.count { it.contentType == type && !it.isLocked }
                        }
                    val enabled = count > 0
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = enabled) { onPick(type) }
                                .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                if (enabled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
private fun CombineDialog(
    title: String,
    entries: List<ClipboardEntry>,
    onDismiss: () -> Unit,
    onConfirm: (List<ClipboardEntry>) -> Unit,
    hint: String? = null,
) {
    val context = LocalContext.current
    var selectedIds by remember { mutableStateOf(listOf<Long>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitleLabel(title) },
        text = {
            if (entries.isEmpty()) {
                Text(
                    text = hint ?: context.getString(R.string.no_clips_yet),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column {
                    LazyColumn(Modifier.fillMaxWidth()) {
                        items(entries, key = { it.id }) { entry ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedIds =
                                                if (entry.id in selectedIds) {
                                                    selectedIds - entry.id
                                                } else {
                                                    selectedIds + entry.id
                                                }
                                        }
                                        .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = entry.id in selectedIds,
                                    onCheckedChange = {
                                        selectedIds =
                                            if (entry.id in selectedIds) {
                                                selectedIds - entry.id
                                            } else {
                                                selectedIds + entry.id
                                            }
                                    },
                                )
                                val order = selectedIds.indexOf(entry.id)
                                if (order >= 0) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = (order + 1).toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text =
                                        clipPickerTitle(
                                            entry.contentType,
                                            entry.previewText,
                                            context.getString(R.string.untitled_clip),
                                        ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.size(4.dp))
                    if (hint != null) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.size(4.dp))
                    }
                    Text(
                        text = context.getString(R.string.select_at_least_two),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedIds.size >= 2,
                onClick = {
                    val selected = selectedIds.mapNotNull { id -> entries.find { it.id == id } }
                    onConfirm(selected)
                },
            ) {
                ButtonLabel(context.getString(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                ButtonLabel(context.getString(R.string.cancel))
            }
        },
    )
}
