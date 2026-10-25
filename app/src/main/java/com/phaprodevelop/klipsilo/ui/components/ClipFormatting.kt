package com.phaprodevelop.klipsilo.ui.components

import android.content.Context
import android.graphics.Typeface
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.settings.PdfFont
import com.phaprodevelop.klipsilo.data.settings.pdfFontAsset
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

internal fun formatTimestamp(
    context: Context,
    timestamp: Long,
): String {
    val now = System.currentTimeMillis()
    val diff = maxOf(0L, now - timestamp)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> context.getString(R.string.just_now)
        minutes < 60 -> context.getString(R.string.minutes_ago, minutes)
        hours < 24 -> context.getString(R.string.hours_ago, hours)
        else -> context.getString(R.string.days_ago, days)
    }
}

internal fun clipInfo(
    context: Context,
    entry: ClipboardEntry,
): String {
    val filePath = entry.filePath
    if (filePath == null) return typeName(context, entry.contentType)
    val file = File(filePath)
    val type = clipInfoTypeLabel(entry.contentType, filePath, typeName(context, entry.contentType))
    val size = if (file.exists()) formatSize(file.length()) else ""
    val dims =
        if (entry.contentType == ContentType.IMAGE) {
            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeFile(filePath, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                "${options.outWidth}×${options.outHeight}"
            } else {
                ""
            }
        } else {
            ""
        }
    return listOf(type, size, dims).filter { it.isNotBlank() }.joinToString(" • ")
}

/**
 * The type label in the clip meta line. Spilled Plain Text and Markdown clips
 * store their body in `clip.txt` / `clip.md`; the list must show the clip kind
 * ("Text" / "Markdown"), never the storage extension (#127).
 */
internal fun clipInfoTypeLabel(
    contentType: ContentType,
    filePath: String,
    typeLabel: String,
): String =
    when (contentType) {
        ContentType.TEXT, ContentType.MARKDOWN -> typeLabel
        else ->
            filePath.substringAfterLast('.', "").uppercase().ifBlank {
                typeLabel
            }
    }

internal fun formatSize(bytes: Long): String =
    when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

internal fun typeName(
    context: Context,
    type: ContentType,
): String {
    val res =
        when (type) {
            ContentType.TEXT -> R.string.text
            ContentType.MARKDOWN -> R.string.markdown
            ContentType.URL -> R.string.url
            ContentType.IMAGE -> R.string.image
            ContentType.PDF -> R.string.pdf
            ContentType.FILE -> R.string.file
            ContentType.LOCATION -> R.string.location
        }
    return context.getString(res)
}

internal fun typefaceForPdfFont(
    context: Context,
    font: PdfFont,
): Typeface =
    pdfFontAsset(font, bold = false)?.let { Typeface.createFromAsset(context.assets, it) }
        ?: Typeface.DEFAULT
