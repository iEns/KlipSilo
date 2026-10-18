package com.phaprodevelop.klipsilo.ui.presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.clipListTitle
import com.phaprodevelop.klipsilo.ui.components.HtmlClipPane
import com.phaprodevelop.klipsilo.ui.components.looksLikeHtmlDocument
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel
import com.phaprodevelop.klipsilo.withoutSoftHyphens
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextPresentationScreen(
    entryId: Long,
    clipboardViewModel: ClipboardViewModel,
    onBack: () -> Unit,
    presentationViewModel: PresentationViewModel = viewModel(),
) {
    val clips by clipboardViewModel.clips.collectAsState()
    val listEntry = clips.find { it.id == entryId }
    var fullText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(entryId, listEntry?.id) {
        val loaded = clipboardViewModel.getClip(entryId)
        fullText =
            loaded?.let { clipboardViewModel.readTextBody(it) }
                ?: loaded?.textContent
                ?: listEntry?.previewText
    }
    val entry = listEntry
    val text = fullText ?: entry?.previewText ?: ""
    // Saved HTML clips render as a page here too, like a fresh PDF-to-HTML conversion (#32).
    val isHtmlClip = looksLikeHtmlDocument(text)
    val title =
        entry?.let {
            clipListTitle(it.contentType, it.previewText).ifBlank {
                clipListTitle(it.contentType, text)
            }.ifBlank {
                stringResource(R.string.untitled_clip)
            }
        } ?: stringResource(R.string.text_presentation)

    val fontSize by presentationViewModel.fontSize.collectAsState()
    val isPageMode by presentationViewModel.isPageMode.collectAsState()
    val isDarkReader by presentationViewModel.isDarkReader.collectAsState()
    val isFullBrightness by presentationViewModel.isFullBrightness.collectAsState()

    val bgColor = if (isDarkReader) Color(0xFF121212) else Color.White
    val textColor = if (isDarkReader) Color(0xFFE0E0E0) else Color(0xFF1C1B1F)

    var isAutoScrolling by remember { mutableStateOf(false) }
    val autoScrollTrigger = remember { mutableStateOf(0L) }
    val speedLevel = remember { mutableIntStateOf(2) }

    val view = LocalView.current
    val window = (view.context as? android.app.Activity)?.window

    LaunchedEffect(isFullBrightness) {
        window?.let { w ->
            w.attributes = w.attributes.apply { screenBrightness = if (isFullBrightness) 1.0f else -1f }
        }
    }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        if (isFullBrightness) {
            window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = 1.0f } }
        }
        onDispose {
            view.keepScreenOn = false
            window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = -1f } }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.reader_back))
                    }
                },
                actions = {
                    if (!isHtmlClip) {
                        var showThemeMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showThemeMenu = true }) {
                                Icon(
                                    when {
                                        isDarkReader && isFullBrightness -> Icons.Default.DarkMode
                                        !isDarkReader && isFullBrightness -> Icons.Default.LightMode
                                        isDarkReader -> Icons.Default.DarkMode
                                        else -> Icons.Default.LightMode
                                    },
                                    contentDescription = stringResource(R.string.reader_theme).withoutSoftHyphens(),
                                )
                            }
                            DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.reader_dark)) },
                                    onClick = {
                                        showThemeMenu = false
                                        presentationViewModel.setTheme(dark = true, fullBrightness = false)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.reader_dark_full)) },
                                    onClick = {
                                        showThemeMenu = false
                                        presentationViewModel.setTheme(dark = true, fullBrightness = true)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.reader_light)) },
                                    onClick = {
                                        showThemeMenu = false
                                        presentationViewModel.setTheme(dark = false, fullBrightness = false)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.reader_light_full)) },
                                    onClick = {
                                        showThemeMenu = false
                                        presentationViewModel.setTheme(dark = false, fullBrightness = true)
                                    },
                                )
                            }
                        }
                    }
                    if (!isHtmlClip) {
                        IconButton(onClick = { presentationViewModel.togglePageMode() }) {
                            Icon(
                                if (isPageMode) Icons.Default.UnfoldMore else Icons.Default.AutoStories,
                                contentDescription =
                                    (
                                        if (isPageMode) {
                                            stringResource(
                                                R.string.reader_scroll,
                                            )
                                        } else {
                                            stringResource(R.string.reader_page_turn)
                                        }
                                    ).withoutSoftHyphens(),
                            )
                        }
                    }
                    if (!isPageMode && !isHtmlClip) {
                        val speedNames =
                            listOf(R.string.speed_2 to 1.0f, R.string.speed_3 to 0.25f, R.string.speed_4 to 0.12f)
                        var showSpeedMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = {
                                if (isAutoScrolling) isAutoScrolling = false else showSpeedMenu = true
                            }) {
                                Icon(
                                    if (isAutoScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription =
                                        (
                                            if (isAutoScrolling) {
                                                stringResource(
                                                    R.string.reader_pause,
                                                )
                                            } else {
                                                stringResource(R.string.reader_play)
                                            }
                                        ).withoutSoftHyphens(),
                                )
                            }
                            DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                                speedNames.forEachIndexed { idx, (nameRes, _) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(nameRes),
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        },
                                        onClick = {
                                            showSpeedMenu = false
                                            speedLevel.intValue = idx + 1
                                            isAutoScrolling = true
                                            autoScrollTrigger.value = System.currentTimeMillis()
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!isHtmlClip) {
                        Slider(
                            value = fontSize,
                            onValueChange = { presentationViewModel.updateFontSize(it) },
                            valueRange = 16f..40f,
                            steps = 23,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).background(bgColor),
        ) {
            when {
                isHtmlClip -> {
                    HtmlClipPane(
                        html = text,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                isPageMode -> {
                    PresentationPageMode(text = text, fontSize = fontSize, textColor = textColor)
                }
                else -> {
                    PresentationScrollMode(
                        text = text,
                        fontSize = fontSize,
                        textColor = textColor,
                        isAutoScrolling = isAutoScrolling,
                        autoScrollTrigger = autoScrollTrigger,
                        onAutoScrollStopped = { isAutoScrolling = false },
                        bgColor = bgColor,
                        speedLevel = speedLevel.intValue,
                    )
                }
            }
        }
    }
}

private const val PRESENTATION_LINE_SPACING = 1.2f

@Composable
private fun PresentationScrollMode(
    text: String,
    fontSize: Float,
    textColor: Color,
    isAutoScrolling: Boolean,
    autoScrollTrigger: androidx.compose.runtime.MutableState<Long>,
    onAutoScrollStopped: () -> Unit,
    bgColor: Color,
    speedLevel: Int,
) {
    val scrollState = rememberScrollState()
    val speedMultiplier =
        when (speedLevel) {
            1 -> 1.0f
            2 -> 0.25f
            else -> 0.12f
        }
    val stepPx = 10
    val delayPerStepMs = (((fontSize - 16f) * (-8f) + 500f) * speedMultiplier).coerceAtLeast(20f)

    LaunchedEffect(isAutoScrolling, autoScrollTrigger.value) {
        if (!isAutoScrolling) return@LaunchedEffect
        while (true) {
            val maxScroll = scrollState.maxValue
            if (maxScroll <= 0 || scrollState.value >= maxScroll) {
                onAutoScrollStopped()
                break
            }
            try {
                val target = (scrollState.value + stepPx).coerceAtMost(maxScroll)
                scrollState.animateScrollTo(
                    target,
                    animationSpec = tween(durationMillis = delayPerStepMs.toInt(), easing = LinearEasing),
                )
            } catch (_: Exception) {
                delay(50)
            }
        }
    }

    val gradientHeight = 60.dp
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = text.ifEmpty { stringResource(R.string.reader_empty) },
            fontSize = fontSize.sp,
            lineHeight = (fontSize * PRESENTATION_LINE_SPACING).sp,
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
            color = if (text.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else textColor,
        )
        if (isAutoScrolling) {
            Spacer(
                modifier =
                    Modifier.fillMaxWidth().height(gradientHeight).align(Alignment.BottomCenter)
                        .drawBehind {
                            drawRect(
                                brush =
                                    Brush.verticalGradient(
                                        colors = listOf(bgColor.copy(alpha = 0f), bgColor.copy(alpha = 0.85f)),
                                        startY = 0f,
                                        endY = size.height,
                                    ),
                            )
                        },
            )
        }
    }
}

@Composable
private fun PresentationPageMode(
    text: String,
    fontSize: Float,
    textColor: Color,
) {
    val readerEmpty = stringResource(R.string.reader_empty)
    PresentationPageModeContent(text = text, fontSize = fontSize, textColor = textColor, readerEmpty = readerEmpty)
}

@Composable
private fun PresentationPageModeContent(
    text: String,
    fontSize: Float,
    textColor: Color,
    readerEmpty: String,
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    val fontSizeSp = fontSize.sp
    val density = LocalDensity.current
    // 2 × 16 dp reader padding — must match the page padding used below (#40).
    val verticalPaddingPx = with(density) { 32.dp.toPx() }

    val linesPerPage =
        remember(layoutResult, contentSize) {
            val layout = layoutResult ?: return@remember 1
            val availableHeight = contentSize.height - verticalPaddingPx
            if ((availableHeight <= 0) || (layout.lineCount == 0)) return@remember 1
            var count = 0
            var h = 0f
            for (i in 0 until layout.lineCount) {
                val lh = layout.getLineBottom(i) - layout.getLineTop(i)
                if (h + lh > availableHeight) break
                h += lh
                count++
            }
            count.coerceAtLeast(1)
        }

    val totalPages =
        remember(layoutResult, linesPerPage) {
            val layout = layoutResult ?: return@remember 1
            ((layout.lineCount + linesPerPage - 1) / linesPerPage).coerceAtLeast(1)
        }

    val pagerState = rememberPagerState { totalPages }

    LaunchedEffect(totalPages) {
        if (pagerState.currentPage >= totalPages && totalPages > 0) {
            try {
                pagerState.scrollToPage(0)
            } catch (_: Exception) {
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().onSizeChanged { contentSize = it }) {
        if (text.isNotEmpty()) {
            Text(
                text = text,
                fontSize = fontSizeSp,
                lineHeight = (fontSize * PRESENTATION_LINE_SPACING).sp,
                onTextLayout = { layoutResult = it },
                modifier = Modifier.fillMaxSize().padding(16.dp).graphicsLayer { alpha = 0.001f },
                softWrap = true,
            )
        }
        if (text.isEmpty()) {
            Text(
                text = readerEmpty,
                fontSize = fontSizeSp,
                lineHeight = (fontSize * PRESENTATION_LINE_SPACING).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxSize().padding(16.dp),
            )
        } else {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val layout = layoutResult
                if (layout != null) {
                    val startLine = page * linesPerPage
                    val endLine = minOf(startLine + linesPerPage, layout.lineCount)
                    if (startLine < layout.lineCount) {
                        val startOffset = layout.getLineStart(startLine)
                        val endOffset = if (endLine < layout.lineCount) layout.getLineStart(endLine) else text.length
                        val pageText = text.substring(startOffset, endOffset)
                        Text(
                            text = pageText,
                            fontSize = fontSizeSp,
                            lineHeight = (fontSize * PRESENTATION_LINE_SPACING).sp,
                            modifier =
                                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                            color = textColor,
                        )
                    }
                }
            }
            if (totalPages > 1) {
                Text(
                    text = stringResource(R.string.reader_page, pagerState.currentPage + 1, totalPages),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                )
            }
        }
    }
}
