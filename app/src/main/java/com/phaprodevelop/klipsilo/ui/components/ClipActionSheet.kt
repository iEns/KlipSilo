package com.phaprodevelop.klipsilo.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pages
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.clipListTitle
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipActionSheet(
    entry: ClipboardEntry,
    viewModel: ClipboardViewModel,
    onView: () -> Unit,
    onViewPresentation: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onFindLinks: (() -> Unit)? = null,
    onDownloadUrl: (() -> Unit)? = null,
    onOpenInBrowser: (() -> Unit)? = null,
    onConvertToPdf: (() -> Unit)? = null,
    onConvertToHtml: (() -> Unit)? = null,
    onConvertHtmlToPdf: (() -> Unit)? = null,
    onTryAsText: (() -> Unit)? = null,
    onOcrImage: (() -> Unit)? = null,
    onAnnotatePdf: (() -> Unit)? = null,
    onFillForm: (() -> Unit)? = null,
    onEditPages: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var pendingDownload by remember { mutableStateOf<ClipboardEntry?>(null) }
    val writeStorageLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            val target = pendingDownload
            pendingDownload = null
            if (granted && target != null) {
                viewModel.saveFileToDownloads(target)
                onDismiss()
            } else if (!granted) {
                viewModel.showSaveFailed()
            }
        }

    fun saveToDownloads(target: ClipboardEntry) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            viewModel.saveFileToDownloads(target)
            onDismiss()
            return
        }
        val granted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.saveFileToDownloads(target)
            onDismiss()
        } else {
            pendingDownload = target
            writeStorageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text =
                        clipListTitle(entry.contentType, entry.previewText)
                            .ifBlank { context.getString(R.string.untitled_clip) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    text = clipInfo(context, entry),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (entry.contentType == ContentType.TEXT) {
                SheetRow(Icons.Filled.Visibility, context.getString(R.string.text_presentation)) {
                    onDismiss()
                    (onViewPresentation ?: onView).invoke()
                }
                SheetRow(
                    Icons.Filled.Edit,
                    context.getString(R.string.text_editor),
                    enabled = !entry.isLocked,
                ) {
                    onView()
                    onDismiss()
                }
            } else {
                SheetRow(Icons.Filled.Visibility, context.getString(R.string.view_content)) {
                    onView()
                    onDismiss()
                }
            }

            SheetRow(Icons.Filled.Share, context.getString(R.string.share)) {
                onDismiss()
                viewModel.shareClip(entry)
            }
            if (
                entry.contentType == ContentType.FILE ||
                entry.contentType == ContentType.PDF ||
                entry.contentType == ContentType.IMAGE
            ) {
                SheetRow(Icons.Filled.Download, context.getString(R.string.save_to_downloads)) {
                    saveToDownloads(entry)
                }
            }

            if (onDownloadUrl != null) {
                SheetRow(Icons.Filled.Download, context.getString(R.string.download_url)) {
                    onDismiss()
                    onDownloadUrl()
                }
            }
            if (onOpenInBrowser != null) {
                SheetRow(Icons.Filled.Html, context.getString(R.string.open_in_browser)) {
                    onDismiss()
                    onOpenInBrowser()
                }
            }

            val conversions = conversionRows(entry, viewModel, context)
            conversions.forEach { (icon, label, action) ->
                SheetRow(icon, label) {
                    onDismiss()
                    action()
                }
            }

            if (onConvertToPdf != null) {
                SheetRow(Icons.Filled.PictureAsPdf, context.getString(R.string.convert_to_pdf)) {
                    onDismiss()
                    onConvertToPdf()
                }
            }
            if (onConvertToHtml != null) {
                SheetRow(Icons.Filled.Html, context.getString(R.string.pdf_to_html)) {
                    onDismiss()
                    onConvertToHtml()
                }
            }
            if (onConvertHtmlToPdf != null) {
                SheetRow(Icons.Filled.PictureAsPdf, context.getString(R.string.html_to_pdf)) {
                    onDismiss()
                    onConvertHtmlToPdf()
                }
            }
            if (onTryAsText != null) {
                SheetRow(Icons.Filled.TextFields, context.getString(R.string.try_as_text)) {
                    onDismiss()
                    onTryAsText()
                }
            }

            if (onOcrImage != null) {
                SheetRow(Icons.Filled.Image, context.getString(R.string.tool_ocr_image)) {
                    onDismiss()
                    onOcrImage()
                }
            }

            if (onAnnotatePdf != null) {
                SheetRow(Icons.Filled.Edit, context.getString(R.string.tool_annotate_pdf)) {
                    onDismiss()
                    onAnnotatePdf()
                }
            }

            if (onFillForm != null) {
                SheetRow(Icons.Filled.FactCheck, context.getString(R.string.fill_form)) {
                    onDismiss()
                    onFillForm()
                }
            }

            if (onEditPages != null) {
                SheetRow(Icons.Filled.Pages, context.getString(R.string.edit_pages)) {
                    onDismiss()
                    onEditPages()
                }
            }

            if (onRename != null) {
                SheetRow(
                    Icons.Filled.Edit,
                    context.getString(R.string.rename),
                    enabled = !entry.isLocked,
                ) {
                    onDismiss()
                    onRename()
                }
            }
            SheetRow(Icons.Filled.ContentCopy, context.getString(R.string.duplicate)) {
                onDismiss()
                viewModel.duplicateClip(entry)
            }
            if (onFindLinks != null) {
                SheetRow(Icons.Filled.Link, context.getString(R.string.find_links)) {
                    onDismiss()
                    onFindLinks()
                }
            }
            SheetRow(Icons.Filled.PushPin, context.getString(R.string.pin_or_unpin)) {
                viewModel.togglePin(entry)
                onDismiss()
            }
            SheetRow(
                icon = if (entry.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                label = context.getString(R.string.lock_or_unlock),
            ) {
                viewModel.toggleLock(entry)
                onDismiss()
            }
            SheetRow(
                Icons.Filled.Delete,
                context.getString(R.string.delete),
                enabled = !entry.isLocked,
            ) {
                onDismiss()
                onDelete()
            }
        }
    }
}

@Composable
private fun SheetRow(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.38f)
                .hapticClickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun conversionRows(
    entry: ClipboardEntry,
    viewModel: ClipboardViewModel,
    context: android.content.Context,
): List<Triple<ImageVector, String, () -> Unit>> {
    return when (entry.contentType) {
        ContentType.TEXT ->
            listOf(
                Triple(Icons.Filled.PictureAsPdf, context.getString(R.string.text_to_pdf)) {
                    viewModel.convertTextToPdf(entry.textContent.orEmpty(), entry)
                },
                Triple(Icons.Filled.InsertDriveFile, context.getString(R.string.text_to_txt)) {
                    viewModel.convertTextToFile(entry.textContent.orEmpty(), entry)
                },
            )
        ContentType.IMAGE ->
            listOf(
                Triple(Icons.Filled.PictureAsPdf, context.getString(R.string.image_to_pdf)) {
                    viewModel.convertImageToPdf(entry)
                },
            )
        ContentType.PDF ->
            listOf(
                Triple(Icons.Filled.Image, context.getString(R.string.pdf_to_image)) {
                    viewModel.convertPdfToImages(entry)
                },
                Triple(Icons.Filled.TextFields, context.getString(R.string.pdf_to_text)) {
                    viewModel.convertPdfToText(entry)
                },
            )
        ContentType.FILE ->
            listOf(
                Triple(Icons.Filled.TextFields, context.getString(R.string.file_to_text)) {
                    viewModel.convertFileToText(entry)
                },
            )
        else -> emptyList()
    }
}
