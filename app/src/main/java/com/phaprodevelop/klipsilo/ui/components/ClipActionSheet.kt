package com.phaprodevelop.klipsilo.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.phaprodevelop.klipsilo.data.settings.PdfSettingsStore
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel

internal enum class SheetSection {
    PRIMARY,
    CONVERT,
    TOOLS,
    MANAGE,
}

/** One action in the sheet, classified so the UI can group it without losing any action. */
internal enum class ClipSheetAction(val section: SheetSection) {
    VIEW(SheetSection.PRIMARY),
    PRESENT(SheetSection.PRIMARY),
    SHARE(SheetSection.PRIMARY),
    SAVE_TO_DOWNLOADS(SheetSection.PRIMARY),
    DOWNLOAD_FROM_URL(SheetSection.PRIMARY),
    OPEN_IN_BROWSER(SheetSection.PRIMARY),
    TEXT_TO_PDF(SheetSection.CONVERT),
    TEXT_TO_TXT(SheetSection.CONVERT),
    IMAGE_TO_PDF(SheetSection.CONVERT),
    PDF_TO_IMAGE(SheetSection.CONVERT),
    PDF_TO_TEXT(SheetSection.CONVERT),
    FILE_TO_TEXT(SheetSection.CONVERT),
    CONVERT_TO_PDF(SheetSection.CONVERT),
    CONVERT_TO_HTML(SheetSection.CONVERT),
    CONVERT_HTML_TO_PDF(SheetSection.CONVERT),
    TRY_AS_TEXT(SheetSection.CONVERT),
    OCR_IMAGE(SheetSection.CONVERT),
    ANNOTATE_PDF(SheetSection.TOOLS),
    FILL_FORM(SheetSection.TOOLS),
    EDIT_PAGES(SheetSection.TOOLS),
    FIND_LINKS(SheetSection.TOOLS),
    PIN(SheetSection.TOOLS),
    LOCK(SheetSection.TOOLS),
    RENAME(SheetSection.MANAGE),
    DUPLICATE(SheetSection.MANAGE),
    DELETE(SheetSection.MANAGE),
}

private data class SheetItem(
    val action: ClipSheetAction,
    val icon: ImageVector,
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

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
    var convertExpanded by remember(entry.id) { mutableStateOf(false) }
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

            val actions =
                buildList {
                    if (entry.contentType == ContentType.TEXT) {
                        add(
                            SheetItem(
                                action = ClipSheetAction.VIEW,
                                icon = Icons.Filled.Edit,
                                label = context.getString(R.string.text_editor),
                                enabled = !entry.isLocked,
                            ) {
                                onView()
                                onDismiss()
                            },
                        )
                        add(
                            SheetItem(
                                action = ClipSheetAction.PRESENT,
                                icon = Icons.Filled.Visibility,
                                label = context.getString(R.string.text_presentation),
                            ) {
                                onDismiss()
                                (onViewPresentation ?: onView).invoke()
                            },
                        )
                    } else {
                        add(
                            SheetItem(
                                action = ClipSheetAction.VIEW,
                                icon = Icons.Filled.Visibility,
                                label = context.getString(R.string.view_content),
                            ) {
                                onView()
                                onDismiss()
                            },
                        )
                    }

                    add(
                        SheetItem(
                            action = ClipSheetAction.SHARE,
                            icon = Icons.Filled.Share,
                            label = context.getString(R.string.share),
                        ) {
                            onDismiss()
                            viewModel.shareClip(entry)
                        },
                    )
                    if (
                        entry.contentType == ContentType.FILE ||
                        entry.contentType == ContentType.PDF ||
                        entry.contentType == ContentType.IMAGE
                    ) {
                        add(
                            SheetItem(
                                action = ClipSheetAction.SAVE_TO_DOWNLOADS,
                                icon = Icons.Filled.Download,
                                label = context.getString(R.string.save_to_downloads),
                            ) {
                                saveToDownloads(entry)
                            },
                        )
                    }
                    onDownloadUrl?.let { download ->
                        add(
                            SheetItem(
                                action = ClipSheetAction.DOWNLOAD_FROM_URL,
                                icon = Icons.Filled.Download,
                                label = context.getString(R.string.download_url),
                            ) {
                                onDismiss()
                                download()
                            },
                        )
                    }
                    onOpenInBrowser?.let { openInBrowser ->
                        add(
                            SheetItem(
                                action = ClipSheetAction.OPEN_IN_BROWSER,
                                icon = Icons.Filled.Html,
                                label = context.getString(R.string.open_in_browser),
                            ) {
                                onDismiss()
                                openInBrowser()
                            },
                        )
                    }

                    addAll(conversionRows(entry, viewModel, context))
                    onConvertToPdf?.let { convert ->
                        add(
                            SheetItem(
                                action = ClipSheetAction.CONVERT_TO_PDF,
                                icon = Icons.Filled.PictureAsPdf,
                                label = context.getString(R.string.convert_to_pdf),
                            ) {
                                // The Convert section wrapper dismisses the sheet (#128).
                                convert()
                            },
                        )
                    }
                    onConvertToHtml?.let { convert ->
                        add(
                            SheetItem(
                                action = ClipSheetAction.CONVERT_TO_HTML,
                                icon = Icons.Filled.Html,
                                label = context.getString(R.string.pdf_to_html),
                            ) {
                                convert()
                            },
                        )
                    }
                    onConvertHtmlToPdf?.let { convert ->
                        add(
                            SheetItem(
                                action = ClipSheetAction.CONVERT_HTML_TO_PDF,
                                icon = Icons.Filled.PictureAsPdf,
                                label = context.getString(R.string.html_to_pdf),
                            ) {
                                convert()
                            },
                        )
                    }
                    onTryAsText?.let { convert ->
                        add(
                            SheetItem(
                                action = ClipSheetAction.TRY_AS_TEXT,
                                icon = Icons.Filled.TextFields,
                                label = context.getString(R.string.try_as_text),
                            ) {
                                convert()
                            },
                        )
                    }
                    onOcrImage?.let { ocr ->
                        add(
                            SheetItem(
                                action = ClipSheetAction.OCR_IMAGE,
                                icon = Icons.Filled.Image,
                                label = context.getString(R.string.tool_ocr_image),
                            ) {
                                ocr()
                            },
                        )
                    }

                    onAnnotatePdf?.let { annotate ->
                        add(
                            SheetItem(
                                action = ClipSheetAction.ANNOTATE_PDF,
                                icon = Icons.Filled.Edit,
                                label = context.getString(R.string.tool_annotate_pdf),
                            ) {
                                onDismiss()
                                annotate()
                            },
                        )
                    }
                    onFillForm?.let { fillForm ->
                        add(
                            SheetItem(
                                action = ClipSheetAction.FILL_FORM,
                                icon = Icons.Filled.FactCheck,
                                label = context.getString(R.string.fill_form),
                            ) {
                                onDismiss()
                                fillForm()
                            },
                        )
                    }
                    onEditPages?.let { editPages ->
                        add(
                            SheetItem(
                                action = ClipSheetAction.EDIT_PAGES,
                                icon = Icons.Filled.Pages,
                                label = context.getString(R.string.edit_pages),
                            ) {
                                onDismiss()
                                editPages()
                            },
                        )
                    }
                    onFindLinks?.let { findLinks ->
                        add(
                            SheetItem(
                                action = ClipSheetAction.FIND_LINKS,
                                icon = Icons.Filled.Link,
                                label = context.getString(R.string.find_links),
                            ) {
                                onDismiss()
                                findLinks()
                            },
                        )
                    }
                    add(
                        SheetItem(
                            action = ClipSheetAction.PIN,
                            icon = Icons.Filled.PushPin,
                            label = context.getString(R.string.pin_or_unpin),
                        ) {
                            viewModel.togglePin(entry)
                            onDismiss()
                        },
                    )
                    add(
                        SheetItem(
                            action = ClipSheetAction.LOCK,
                            icon = if (entry.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            label = context.getString(R.string.lock_or_unlock),
                        ) {
                            viewModel.toggleLock(entry)
                            onDismiss()
                        },
                    )

                    onRename?.let { rename ->
                        add(
                            SheetItem(
                                action = ClipSheetAction.RENAME,
                                icon = Icons.Filled.Edit,
                                label = context.getString(R.string.rename),
                                enabled = !entry.isLocked,
                            ) {
                                onDismiss()
                                rename()
                            },
                        )
                    }
                    add(
                        SheetItem(
                            action = ClipSheetAction.DUPLICATE,
                            icon = Icons.Filled.ContentCopy,
                            label = context.getString(R.string.duplicate),
                        ) {
                            onDismiss()
                            viewModel.duplicateClip(entry)
                        },
                    )
                    add(
                        SheetItem(
                            action = ClipSheetAction.DELETE,
                            icon = Icons.Filled.Delete,
                            label = context.getString(R.string.delete),
                            enabled = !entry.isLocked,
                        ) {
                            onDismiss()
                            onDelete()
                        },
                    )
                }

            val primaryActions = actions.filter { it.action.section == SheetSection.PRIMARY }
            val convertActions = actions.filter { it.action.section == SheetSection.CONVERT }
            val toolActions = actions.filter { it.action.section == SheetSection.TOOLS }
            val manageActions = actions.filter { it.action.section == SheetSection.MANAGE }

            primaryActions.forEach { item ->
                SheetRow(
                    icon = item.icon,
                    label = item.label,
                    enabled = item.enabled,
                    onClick = item.onClick,
                )
            }

            if (convertActions.isNotEmpty()) {
                SheetDivider()
                SheetRow(
                    icon = Icons.Filled.Transform,
                    label = context.getString(R.string.action_convert),
                    trailingIcon = if (convertExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                ) {
                    convertExpanded = !convertExpanded
                }
                AnimatedVisibility(visible = convertExpanded) {
                    Column(Modifier.fillMaxWidth()) {
                        convertActions.forEach { item ->
                            SheetRow(
                                icon = item.icon,
                                label = item.label,
                                enabled = item.enabled,
                                // #128: every conversion shows a toast when it
                                // finishes, and a modal sheet would hide it.
                                // Close the sheet before the action runs.
                                onClick = {
                                    onDismiss()
                                    item.onClick()
                                },
                            )
                        }
                    }
                }
            }

            if (toolActions.isNotEmpty()) {
                SheetDivider()
                toolActions.forEach { item ->
                    SheetRow(
                        icon = item.icon,
                        label = item.label,
                        enabled = item.enabled,
                        onClick = item.onClick,
                    )
                }
            }

            if (manageActions.isNotEmpty()) {
                SheetDivider()
                SheetSectionLabel(context.getString(R.string.action_manage))
                manageActions.forEach { item ->
                    SheetRow(
                        icon = item.icon,
                        label = item.label,
                        enabled = item.enabled,
                        onClick = item.onClick,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SheetRow(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
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
        // #64: the DE/DA strings may carry a soft hyphen — TalkBack reads the
        // clean source through DisplayText.
        DisplayText(text = label, style = MaterialTheme.typography.bodyLarge)
        if (trailingIcon != null) {
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Icon(trailingIcon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SheetSectionLabel(label: String) {
    DisplayText(
        text = label,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun SheetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** Lowercased file extension of [filePath], or "" (no path or extension). */
internal fun fileExtensionOf(filePath: String?): String = filePath?.substringAfterLast('.', "")?.lowercase() ?: ""

/** File extensions with a direct "Convert to PDF" action (#66, #75). */
internal val FILE_TO_PDF_EXTENSIONS = setOf("txt", "rtf", "docx", "xlsx")

/**
 * The "Convert to PDF" action for a file clip by its extension — the same
 * mapping the action sheet shows, reused by the viewer's file info card
 * (#66) so the conversion logic is not duplicated.
 */
internal fun fileConvertToPdfAction(
    entry: ClipboardEntry,
    viewModel: ClipboardViewModel,
): (() -> Unit)? =
    when (fileExtensionOf(entry.filePath)) {
        "txt" -> ({ viewModel.convertFileToPdf(entry) })
        "rtf" -> ({ viewModel.convertRtfFileToPdf(entry) })
        "docx" -> ({ viewModel.convertDocxFileToPdf(entry) })
        "xlsx" -> ({ viewModel.convertXlsxFileToPdf(entry) })
        else -> null
    }

private fun conversionRows(
    entry: ClipboardEntry,
    viewModel: ClipboardViewModel,
    context: android.content.Context,
): List<SheetItem> =
    when (entry.contentType) {
        ContentType.TEXT ->
            listOf(
                SheetItem(
                    action = ClipSheetAction.TEXT_TO_PDF,
                    icon = Icons.Filled.PictureAsPdf,
                    label = context.getString(R.string.text_to_pdf),
                ) {
                    // #126: read the Settings font when the action runs, so a
                    // change made in Settings is picked up without a restart.
                    viewModel.convertTextToPdf(
                        entry.textContent.orEmpty(),
                        entry,
                        pdfFont = PdfSettingsStore.load(context).pdfFont,
                    )
                },
                SheetItem(
                    action = ClipSheetAction.TEXT_TO_TXT,
                    icon = Icons.Filled.InsertDriveFile,
                    label = context.getString(R.string.text_to_txt),
                ) {
                    viewModel.convertTextToFile(entry.textContent.orEmpty(), entry)
                },
            )
        ContentType.IMAGE ->
            listOf(
                SheetItem(
                    action = ClipSheetAction.IMAGE_TO_PDF,
                    icon = Icons.Filled.PictureAsPdf,
                    label = context.getString(R.string.image_to_pdf),
                ) {
                    viewModel.convertImageToPdf(entry)
                },
            )
        ContentType.PDF ->
            listOf(
                SheetItem(
                    action = ClipSheetAction.PDF_TO_IMAGE,
                    icon = Icons.Filled.Image,
                    label = context.getString(R.string.pdf_to_image),
                ) {
                    viewModel.convertPdfToImages(entry)
                },
                SheetItem(
                    action = ClipSheetAction.PDF_TO_TEXT,
                    icon = Icons.Filled.TextFields,
                    label = context.getString(R.string.pdf_to_text),
                ) {
                    viewModel.convertPdfToText(entry)
                },
            )
        ContentType.FILE ->
            listOf(
                SheetItem(
                    action = ClipSheetAction.FILE_TO_TEXT,
                    icon = Icons.Filled.TextFields,
                    label = context.getString(R.string.file_to_text),
                ) {
                    viewModel.convertFileToText(entry)
                },
            )
        else -> emptyList()
    }
