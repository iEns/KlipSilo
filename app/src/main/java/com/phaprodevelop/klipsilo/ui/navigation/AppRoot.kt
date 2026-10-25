package com.phaprodevelop.klipsilo.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.ui.components.ButtonLabel
import com.phaprodevelop.klipsilo.ui.components.DialogTitleLabel
import com.phaprodevelop.klipsilo.ui.components.NAV_LABEL_FONT_FLOOR_FRACTION
import com.phaprodevelop.klipsilo.ui.components.NAV_SELECTED_LABEL_MIN_SCALE
import com.phaprodevelop.klipsilo.ui.components.scaledLabelStyle
import com.phaprodevelop.klipsilo.ui.presentation.TextPresentationScreen
import com.phaprodevelop.klipsilo.ui.screen.AboutScreen
import com.phaprodevelop.klipsilo.ui.screen.AnnotatePdfScreen
import com.phaprodevelop.klipsilo.ui.screen.CameraScanScreen
import com.phaprodevelop.klipsilo.ui.screen.DetailScreen
import com.phaprodevelop.klipsilo.ui.screen.FormFillScreen
import com.phaprodevelop.klipsilo.ui.screen.IndstillingerScreen
import com.phaprodevelop.klipsilo.ui.screen.KlipScreen
import com.phaprodevelop.klipsilo.ui.screen.LimitationsScreen
import com.phaprodevelop.klipsilo.ui.screen.PdfPagesScreen
import com.phaprodevelop.klipsilo.ui.screen.PrivacyPolicyScreen
import com.phaprodevelop.klipsilo.ui.screen.ScanMode
import com.phaprodevelop.klipsilo.ui.screen.TipsScreen
import com.phaprodevelop.klipsilo.ui.screen.UseCasesScreen
import com.phaprodevelop.klipsilo.ui.screen.VaerktøjerScreen
import com.phaprodevelop.klipsilo.ui.screen.WhyScreen
import com.phaprodevelop.klipsilo.viewmodel.ClipNoticeAction
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel
import com.phaprodevelop.klipsilo.withoutInvisibleBreaks
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppRoot(viewModel: ClipboardViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val navigateToNewClip by viewModel.navigateToNewClip.collectAsState()
    val isFullScreenRoute =
        currentRoute?.startsWith("scanner/") == true ||
            currentRoute?.startsWith("detail/") == true ||
            currentRoute?.startsWith("annotate/") == true ||
            currentRoute?.startsWith("presentation/") == true ||
            currentRoute?.startsWith("form/") == true ||
            currentRoute?.startsWith("pages/") == true

    val isConverting by viewModel.isConverting.collectAsState()
    var showConverting by remember { mutableStateOf(false) }
    LaunchedEffect(isConverting) {
        if (isConverting) {
            delay(300)
            showConverting = true
        } else {
            showConverting = false
        }
    }

    val maintenanceBusy by viewModel.maintenanceBusy.collectAsState()
    var showMaintenance by remember { mutableStateOf(false) }
    LaunchedEffect(maintenanceBusy) {
        if (maintenanceBusy) {
            delay(300)
            showMaintenance = true
        } else {
            showMaintenance = false
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val undoOffer by viewModel.undoOffer.collectAsState()
    val duplicateShare by viewModel.duplicateShare.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(undoOffer) {
        val offer = undoOffer
        if (offer != null) {
            val result =
                snackbarHostState.showSnackbar(
                    message = offer.message,
                    actionLabel = context.getString(R.string.undo),
                    duration = SnackbarDuration.Indefinite,
                )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            }
        } else {
            viewModel.toastEvents.collect { notice ->
                val result =
                    snackbarHostState.showSnackbar(
                        message = notice.message,
                        actionLabel = notice.actionLabel,
                        duration =
                            if (notice.actionLabel != null) {
                                SnackbarDuration.Long
                            } else {
                                SnackbarDuration.Short
                            },
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    when (val action = notice.action) {
                        is ClipNoticeAction.OpenClip -> navController.navigate("detail/${action.clipId}")
                        ClipNoticeAction.RetrySaveClipboard -> viewModel.retryLastSave()
                        null -> Unit
                    }
                }
            }
        }
    }

    LaunchedEffect(navigateToNewClip) {
        navigateToNewClip?.let { id ->
            navController.navigate("detail/$id")
            viewModel.clearNavigateToNewClip()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier =
                    Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        // Clear the + FAB in KlipScreen on the main tabs too, not only
                        // the full-screen routes (#104). Undo snackbars stay usable.
                        .padding(bottom = 72.dp),
            )
        },
        contentWindowInsets =
            if (isFullScreenRoute) {
                WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            } else {
                ScaffoldDefaults.contentWindowInsets
            },
        bottomBar = {
            if (!isFullScreenRoute) {
                BoxWithConstraints {
                    // Per-item slot for measuring whether the label fits on
                    // one line at the nav floor (#45); a small safety margin
                    // covers the item's internal padding.
                    val slotWidthPx =
                        constraints.maxWidth / BottomNavItem.entries.size -
                            with(LocalDensity.current) { 16.dp.toPx() }.toInt()
                    NavigationBar {
                        BottomNavItem.entries.forEach { item ->
                            val label = stringResource(item.labelRes)
                            val selected = currentRoute == item.route
                            val labelFits = navLabelFits(label, slotWidthPx)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (item == BottomNavItem.VÆRKTØJER) {
                                        if (
                                            currentRoute != BottomNavItem.VÆRKTØJER.route &&
                                            !navController.popBackStack(BottomNavItem.VÆRKTØJER.route, false)
                                        ) {
                                            navController.navigate(BottomNavItem.VÆRKTØJER.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    } else {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    // When the label is hidden (icon-only
                                    // unselected item, #45) the label must
                                    // still be exposed to TalkBack; when the
                                    // label is shown it would be announced
                                    // twice, so the icon stays silent there.
                                    Icon(
                                        item.icon,
                                        contentDescription =
                                            if (labelFits || selected) {
                                                null
                                            } else {
                                                // Icon-only unselected item: the
                                                // label string carries the soft
                                                // hyphen (#64) — TalkBack must
                                                // not read it.
                                                label.withoutInvisibleBreaks()
                                            },
                                    )
                                },
                                alwaysShowLabel = labelFits,
                                label = {
                                    // Single line, never broken inside a word:
                                    // shrink to the nav floor before the item
                                    // hides its label (#45). A selected item
                                    // always shows its label, so it may shrink
                                    // a little further than the unselected
                                    // floor instead of an ellipsis (#62).
                                    ButtonLabel(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        floorFraction =
                                            if (selected) {
                                                NAV_SELECTED_LABEL_MIN_SCALE
                                            } else {
                                                NAV_LABEL_FONT_FLOOR_FRACTION
                                            },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.KLIP.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(BottomNavItem.KLIP.route) {
                KlipScreen(
                    viewModel = viewModel,
                    onOpenClip = { id -> navController.navigate("detail/$id") },
                    onOpenPresentation = { id -> navController.navigate("presentation/$id") },
                    onOcrImage = { id -> navController.navigate("scanner/image/$id") },
                    onAnnotatePdf = { id -> navController.navigate("annotate/$id") },
                    onFillForm = { id -> navController.navigate("form/$id") },
                    onEditPages = { id -> navController.navigate("pages/$id") },
                )
            }
            composable(BottomNavItem.VÆRKTØJER.route) {
                VaerktøjerScreen(
                    viewModel = viewModel,
                    onNavigateToScanner = { navController.navigate("scanner/qr") },
                    onNavigateToOcr = { navController.navigate("scanner/ocr") },
                    onNavigateToAnnotate = { id -> navController.navigate("annotate/$id") },
                    onNavigateToClips = {
                        navController.navigate(BottomNavItem.KLIP.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(BottomNavItem.INDSTILLINGER.route) {
                IndstillingerScreen(
                    viewModel,
                    onNavigateToTips = { navController.navigate("tips") },
                    onNavigateToLimitations = { navController.navigate("limitations") },
                    onNavigateToWhy = { navController.navigate("why") },
                    onNavigateToUseCases = { navController.navigate("use-cases") },
                    onNavigateToAbout = { navController.navigate("about") },
                    onNavigateToPrivacy = { navController.navigate("privacy") },
                )
            }
            composable("scanner/qr") {
                CameraScanScreen(
                    mode = ScanMode.QR,
                    onNavigateBack = { navController.popBackStack() },
                    onScanned = { text ->
                        navController.popBackStack()
                        viewModel.saveScannedText(text)
                    },
                )
            }
            composable("scanner/ocr") {
                CameraScanScreen(
                    mode = ScanMode.OCR,
                    onNavigateBack = { navController.popBackStack() },
                    onScanned = { text ->
                        navController.popBackStack()
                        viewModel.saveScannedText(text)
                    },
                    onSaveImage = { bitmap, onResult ->
                        viewModel.saveImageBitmap(bitmap, onResult = onResult)
                    },
                )
            }
            composable(
                route = "scanner/image/{entryId}",
                arguments = listOf(navArgument("entryId") { type = NavType.LongType }),
            ) { entry ->
                val entryId = entry.arguments?.getLong("entryId") ?: return@composable
                val clips by viewModel.clips.collectAsState()
                val path = clips.find { it.id == entryId }?.filePath
                CameraScanScreen(
                    mode = ScanMode.OCR,
                    onNavigateBack = { navController.popBackStack() },
                    onScanned = { text ->
                        navController.popBackStack()
                        viewModel.saveScannedText(text)
                    },
                    onSaveCrop = { bitmap, onResult ->
                        viewModel.saveImageBitmap(
                            bitmap,
                            com.phaprodevelop.klipsilo.R.string.selection_saved,
                            onResult,
                        )
                    },
                    onSaveTextAsClip = { text ->
                        viewModel.saveScannedText(text, navigate = false)
                        viewModel.notifyScannedTextSaved()
                    },
                    onOpenTextInEditor = { text ->
                        navController.popBackStack()
                        viewModel.saveScannedText(text)
                    },
                    autoBackOnNoText = false,
                    imagePath = path,
                    existingImage = true,
                )
            }
            composable(
                route = "detail/{entryId}",
                arguments = listOf(navArgument("entryId") { type = NavType.LongType }),
            ) { entry ->
                val entryId = entry.arguments?.getLong("entryId") ?: return@composable
                DetailScreen(
                    entryId = entryId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOcrImage = { id -> navController.navigate("scanner/image/$id") },
                )
            }
            composable(
                route = "presentation/{entryId}",
                arguments = listOf(navArgument("entryId") { type = NavType.LongType }),
            ) { entry ->
                val entryId = entry.arguments?.getLong("entryId") ?: return@composable
                TextPresentationScreen(
                    entryId = entryId,
                    clipboardViewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "annotate/{entryId}",
                arguments = listOf(navArgument("entryId") { type = NavType.LongType }),
            ) { entry ->
                val entryId = entry.arguments?.getLong("entryId") ?: return@composable
                AnnotatePdfScreen(
                    entryId = entryId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "form/{entryId}",
                arguments = listOf(navArgument("entryId") { type = NavType.LongType }),
            ) { entry ->
                val entryId = entry.arguments?.getLong("entryId") ?: return@composable
                FormFillScreen(
                    entryId = entryId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "pages/{entryId}",
                arguments = listOf(navArgument("entryId") { type = NavType.LongType }),
            ) { entry ->
                val entryId = entry.arguments?.getLong("entryId") ?: return@composable
                PdfPagesScreen(
                    entryId = entryId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable("tips") {
                TipsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("limitations") {
                LimitationsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("why") {
                WhyScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("use-cases") {
                UseCasesScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("about") {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPrivacy = { navController.navigate("privacy") },
                )
            }
            composable("privacy") {
                PrivacyPolicyScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }

    if (showConverting) {
        AlertDialog(
            onDismissRequest = {},
            title = { DialogTitleLabel(stringResource(R.string.converting)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.converting))
                }
            },
            confirmButton = {},
        )
    }

    if (showMaintenance) {
        AlertDialog(
            onDismissRequest = {},
            title = { DialogTitleLabel(stringResource(R.string.maintenance_busy)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.maintenance_busy))
                }
            },
            confirmButton = {},
        )
    }

    duplicateShare?.let { offer ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicateShare() },
            title = { DialogTitleLabel(stringResource(R.string.already_saved)) },
            text = { Text(stringResource(R.string.already_saved_message, offer.displayName)) },
            confirmButton = {
                TextButton(onClick = { viewModel.openExistingShare() }) {
                    ButtonLabel(stringResource(R.string.open_existing))
                }
            },
            dismissButton = {
                FlowRow {
                    TextButton(onClick = { viewModel.saveDuplicateAgain() }) {
                        ButtonLabel(stringResource(R.string.save_again))
                    }
                    TextButton(onClick = { viewModel.dismissDuplicateShare() }) {
                        ButtonLabel(stringResource(R.string.cancel))
                    }
                }
            },
        )
    }
}

/**
 * True when a bottom navigation label fits its [slotWidthPx] slot on one
 * line at the [NAV_LABEL_FONT_FLOOR_FRACTION] floor (#45). When it does not,
 * the item shows its label only while selected (alwaysShowLabel = false)
 * instead of wrapping it mid-word or clipping it at full size.
 */
@Composable
private fun navLabelFits(
    label: String,
    slotWidthPx: Int,
): Boolean {
    val style = MaterialTheme.typography.labelMedium
    val base = style.fontSize
    if (!base.isSp || base.value <= 0f || slotWidthPx <= 0) return true
    val measurer = rememberTextMeasurer()
    val fontScale = LocalDensity.current.fontScale
    return remember(label, slotWidthPx, style, fontScale) {
        val floor = base.value * NAV_LABEL_FONT_FLOOR_FRACTION
        val result =
            measurer.measure(
                text = label,
                style = scaledLabelStyle(style, floor, base.value),
                softWrap = false,
                maxLines = 1,
                constraints = Constraints(maxWidth = slotWidthPx),
            )
        !result.didOverflowWidth
    }
}

@Preview(fontScale = 2f, widthDp = 320, heightDp = 120, showBackground = true)
@Composable
private fun BottomNavLargeScalePreview() {
    // German labels from #45/#62: "Werkzeuge"/"Einstellungen" must stay on one
    // line (shrunk, never broken mid-word) at font scale 2.0, and the SELECTED
    // "Einstellungen" must fit too — it used to show as "Einstellun…" (#62).
    MaterialTheme {
        BoxWithConstraints {
            val slotWidthPx =
                constraints.maxWidth / 3 - with(LocalDensity.current) { 16.dp.toPx() }.toInt()
            NavigationBar {
                listOf("Clips", "Werkzeuge", "Einstellungen").forEachIndexed { index, label ->
                    val selected = index == 2
                    NavigationBarItem(
                        selected = selected,
                        onClick = {},
                        icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                        alwaysShowLabel = navLabelFits(label, slotWidthPx),
                        label = {
                            ButtonLabel(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                floorFraction =
                                    if (selected) {
                                        NAV_SELECTED_LABEL_MIN_SCALE
                                    } else {
                                        NAV_LABEL_FONT_FLOOR_FRACTION
                                    },
                            )
                        },
                    )
                }
            }
        }
    }
}
