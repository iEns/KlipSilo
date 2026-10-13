package com.phaprodevelop.klipsilo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat

@Composable
fun Modifier.hapticClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val view = LocalView.current
    return this.clickable(enabled = enabled) {
        ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
        onClick()
    }
}
