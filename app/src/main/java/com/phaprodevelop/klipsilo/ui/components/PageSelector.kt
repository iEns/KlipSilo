package com.phaprodevelop.klipsilo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.phaprodevelop.klipsilo.R

/**
 * Parses and validates a page number typed into the "go to page" dialog.
 *
 * Returns the 0-based page index when [raw] is a whole number within 1..[pageCount],
 * and null when the input is empty, not a number, or outside the valid range
 * (the dialog shows an error instead of jumping). A [pageCount] below 1 is
 * treated as a single page.
 */
fun parsePageInput(
    raw: String,
    pageCount: Int,
): Int? {
    val count = pageCount.coerceAtLeast(1)
    val value = raw.trim().toIntOrNull() ?: return null
    if (value < 1 || value > count) return null
    return value - 1
}

/**
 * Clamps a 0-based page index into the valid 0..pageCount-1 range
 * (a [pageCount] below 1 is treated as a single page).
 */
fun clampPageIndex(
    pageIndex: Int,
    pageCount: Int,
): Int = pageIndex.coerceIn(0, pageCount.coerceAtLeast(1) - 1)

/**
 * Shared "‹  12 / 250  ›" page selector used by every screen with page navigation.
 *
 * The indicator between the arrows is a tappable button (issue #49): tapping it
 * opens a small dialog with a numeric field prefilled with (and selecting) the
 * current page; OK/Done jumps, Cancel closes, and out-of-range or non-numeric
 * input shows an error instead of jumping.
 *
 * The indicator label stays on a single line and the buttons meet the 48 dp
 * minimum touch target, so nothing wraps or overflows at font scale 2.0 with
 * 3-digit page counts.
 */
@Composable
fun PageSelector(
    pageIndex: Int,
    pageCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onGoToPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var jumpDialogOpen by remember { mutableStateOf(false) }
    val displayPageCount = pageCount.coerceAtLeast(1)
    val indicatorDescription =
        stringResource(R.string.page_selector_indicator_cd, pageIndex + 1, displayPageCount)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev, enabled = pageIndex > 0) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.markup_prev_page),
            )
        }
        TextButton(
            onClick = { jumpDialogOpen = true },
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            ButtonLabel(
                text = "${pageIndex + 1} / $displayPageCount",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                modifier =
                    Modifier.semantics {
                        contentDescription = indicatorDescription
                    },
            )
        }
        IconButton(onClick = onNext, enabled = pageCount > 1 && pageIndex < pageCount - 1) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.markup_next_page),
            )
        }
    }

    if (jumpDialogOpen) {
        PageJumpDialog(
            pageIndex = clampPageIndex(pageIndex, displayPageCount),
            pageCount = displayPageCount,
            onDismiss = { jumpDialogOpen = false },
            onGoToPage = {
                jumpDialogOpen = false
                onGoToPage(clampPageIndex(it, displayPageCount))
            },
        )
    }
}

@Composable
private fun PageJumpDialog(
    pageIndex: Int,
    pageCount: Int,
    onDismiss: () -> Unit,
    onGoToPage: (Int) -> Unit,
) {
    val initial = (pageIndex + 1).toString()
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val requestedPage = parsePageInput(value.text, pageCount)

    LaunchedEffect(Unit) {
        // Give the dialog window a frame to lay out before we grab focus.
        withFrameNanos {}
        focusRequester.requestFocus()
        keyboard?.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { next ->
                    val digitsOnly = next.text.filter { it.isDigit() }
                    value =
                        if (digitsOnly.length < next.text.length) {
                            // Digits were dropped (paste with junk): park the caret at the end.
                            TextFieldValue(digitsOnly, TextRange(digitsOnly.length))
                        } else {
                            next.copy(text = digitsOnly)
                        }
                },
                keyboardOptions =
                    KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            requestedPage?.let(onGoToPage)
                        },
                    ),
                isError = requestedPage == null,
                supportingText = {
                    if (requestedPage == null) {
                        Text(stringResource(R.string.page_selector_error, pageCount))
                    }
                },
                singleLine = true,
                modifier = Modifier.focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(
                enabled = requestedPage != null,
                onClick = { requestedPage?.let(onGoToPage) },
            ) {
                ButtonLabel(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                ButtonLabel(stringResource(R.string.cancel))
            }
        },
    )
}

@Preview(fontScale = 2f, widthDp = 320, heightDp = 120, showBackground = true)
@Composable
private fun PageSelectorPreview() {
    MaterialTheme {
        PageSelector(
            pageIndex = 11,
            pageCount = 250,
            onPrev = {},
            onNext = {},
            onGoToPage = {},
        )
    }
}

@Preview(fontScale = 2f, widthDp = 320, showBackground = true)
@Composable
private fun PageJumpDialogPreview() {
    MaterialTheme {
        PageJumpDialog(
            pageIndex = 11,
            pageCount = 250,
            onDismiss = {},
            onGoToPage = {},
        )
    }
}
