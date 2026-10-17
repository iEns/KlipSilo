package com.phaprodevelop.klipsilo.ui.screen

/**
 * The bullet the preview draws instead of each character of a hidden password (#61).
 */
internal const val PASSWORD_BULLET = "•"

/**
 * What the preview shows for a text field: a hidden password becomes one bullet per
 * character. The saved PDF always gets the real value, never the bullets (#61).
 */
internal fun passwordPreviewText(
    isPassword: Boolean,
    revealed: Boolean,
    value: String,
): String =
    if (isPassword && !revealed) {
        PASSWORD_BULLET.repeat(value.length)
    } else {
        value
    }

/**
 * The "Saved as readable text" badge only appears on a filled password field while it
 * is hidden — when the text is revealed the user can already read it in the preview (#61).
 */
internal fun showPasswordSavedBadge(
    isPassword: Boolean,
    revealed: Boolean,
    value: String,
): Boolean = isPassword && !revealed && value.isNotEmpty()
