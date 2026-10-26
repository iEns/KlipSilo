package com.phaprodevelop.klipsilo.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.phaprodevelop.klipsilo.R

enum class BottomNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    KLIP("klip", R.string.clips, Icons.Filled.Inbox),
    TOOLS("vaerktojer", R.string.tools, Icons.Filled.Construction),
    SETTINGS("indstillinger", R.string.settings, Icons.Filled.Settings),
}
