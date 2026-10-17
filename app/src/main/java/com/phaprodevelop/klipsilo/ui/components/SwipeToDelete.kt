package com.phaprodevelop.klipsilo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeToDelete(
    onDelete: () -> Unit,
    onDeleteUndo: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val offsetX = remember { Animatable(0f) }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (!enabled) {
            // A locked clip cannot be swipe-deleted (#63). With no drag
            // detector installed, nothing consumed the horizontal movement,
            // so the row's tap detector still fired onTap on pointer-up
            // after a swipe and opened the clip. Install a drag detector
            // that consumes the drag, with a small elastic nudge that
            // springs back. Taps, double-taps and long-presses still work:
            // they never cross the horizontal touch slop, so the detector
            // consumes nothing for them.
            val nudgeX = remember { Animatable(0f) }
            val nudgeMaxPx = with(density) { LOCKED_SWIPE_NUDGE_MAX_DP.toPx() }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(nudgeX.value.roundToInt(), 0) }
                        .pointerInput(nudgeMaxPx) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        nudgeX.snapTo(
                                            lockedSwipeNudgeOffset(
                                                nudgeX.value,
                                                dragAmount,
                                                nudgeMaxPx,
                                            ),
                                        )
                                    }
                                },
                                onDragEnd = { scope.launch { nudgeX.animateTo(0f) } },
                                onDragCancel = { scope.launch { nudgeX.animateTo(0f) } },
                            )
                        },
            ) {
                content()
            }
            return@BoxWithConstraints
        }
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val revealWidthPx = with(density) { 80.dp.toPx() }

        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(Color(0xFFD32F2F))
                    .clickable {
                        scope.launch { offsetX.animateTo(0f) }
                        onDeleteUndo()
                    },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = null,
                tint = Color.White,
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-maxWidthPx, 0f))
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    when (swipeDeleteEndAction(offsetX.value, maxWidthPx, revealWidthPx)) {
                                        SwipeDeleteEnd.DELETE -> {
                                            offsetX.animateTo(-maxWidthPx)
                                            onDelete()
                                        }
                                        SwipeDeleteEnd.REVEAL -> {
                                            offsetX.animateTo(-revealWidthPx)
                                        }
                                        SwipeDeleteEnd.RESET -> {
                                            offsetX.animateTo(0f)
                                        }
                                    }
                                }
                            },
                        )
                    },
        ) {
            content()
        }
    }
}

internal enum class SwipeDeleteEnd { DELETE, REVEAL, RESET }

/** A locked row nudges sideways by at most this much while a swipe is consumed (#63). */
private val LOCKED_SWIPE_NUDGE_MAX_DP = 8.dp

/** A locked row follows the drag at this fraction, so the nudge stays elastic (#63). */
private const val LOCKED_SWIPE_NUDGE_FRACTION = 0.25f

/**
 * The sideways offset of a locked row during a consumed swipe (#63): a small
 * elastic nudge within [-maxPx, maxPx] that the caller springs back to 0.
 */
internal fun lockedSwipeNudgeOffset(
    currentX: Float,
    dragAmount: Float,
    maxPx: Float,
): Float = (currentX + dragAmount * LOCKED_SWIPE_NUDGE_FRACTION).coerceIn(-maxPx, maxPx)

internal fun swipeDeleteEndAction(
    offsetX: Float,
    maxWidthPx: Float,
    revealWidthPx: Float,
): SwipeDeleteEnd =
    when {
        offsetX < -maxWidthPx * 0.6f -> SwipeDeleteEnd.DELETE
        offsetX < -revealWidthPx * 0.5f -> SwipeDeleteEnd.REVEAL
        else -> SwipeDeleteEnd.RESET
    }
