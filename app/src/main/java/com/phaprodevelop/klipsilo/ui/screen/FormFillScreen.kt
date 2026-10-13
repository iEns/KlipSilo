package com.phaprodevelop.klipsilo.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.repository.FormFieldInfo
import com.phaprodevelop.klipsilo.data.repository.FormFieldType
import com.phaprodevelop.klipsilo.ui.components.pdfPageCount
import com.phaprodevelop.klipsilo.ui.components.produceLatest
import com.phaprodevelop.klipsilo.ui.components.rememberDisplayedBitmap
import com.phaprodevelop.klipsilo.ui.components.renderPdfPage
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

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

    val navigateBack by viewModel.navigateBack.collectAsState()
    LaunchedEffect(navigateBack) {
        if (navigateBack) {
            dirty = false
            viewModel.clearFormFields()
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

    LaunchedEffect(entry?.id) {
        entry?.let { viewModel.loadFormFields(it) }
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

    val filePath = entry?.filePath
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

    BackHandler {
        if (dirty) {
            showDiscardDialog = true
        } else {
            viewModel.clearFormFields()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.fill_form)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (dirty) {
                            showDiscardDialog = true
                        } else {
                            viewModel.clearFormFields()
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = context.getString(R.string.back))
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.fillForm(entry, values)
                        },
                        enabled = entry.filePath != null && !isConverting,
                    ) {
                        Text(context.getString(R.string.save_pdf))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
        ) {
            displayed.bitmap?.let { bmp ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = context.getString(R.string.pdf),
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            if (pageCount > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { if (pageIndex > 0) pageIndex-- },
                        enabled = pageIndex > 0,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                    }
                    Text(
                        text = "${pageIndex + 1} / $pageCount",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    IconButton(
                        onClick = { if (pageIndex < pageCount - 1) pageIndex++ },
                        enabled = pageIndex < pageCount - 1,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                    }
                }
            }

            when {
                formFieldsLoading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                formFields.isNullOrEmpty() -> {
                    Text(
                        text = context.getString(R.string.no_form_fields),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                else -> {
                    Text(
                        text = context.getString(R.string.fill_form_fields),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    formFields.orEmpty().forEach { field ->
                        FormFieldRow(
                            field = field,
                            value = values[field.fullyQualifiedName].orEmpty(),
                            onValueChange = {
                                values = values + (field.fullyQualifiedName to it)
                                dirty = true
                            },
                        )
                    }
                    Spacer(Modifier.size(16.dp))
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
private fun FormFieldRow(
    field: FormFieldInfo,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = field.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (field.type) {
            FormFieldType.CHECKBOX -> {
                Checkbox(
                    checked = value == "true",
                    onCheckedChange = { onValueChange(if (it) "true" else "false") },
                )
            }
            FormFieldType.RADIO, FormFieldType.CHOICE -> {
                field.options.forEach { option ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onValueChange(option) },
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
