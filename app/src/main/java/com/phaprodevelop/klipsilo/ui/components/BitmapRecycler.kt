package com.phaprodevelop.klipsilo.ui.components

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicInteger

/**
 * Result of [produceLatest] that won the race against newer requests.
 * [produceLatest] returns null when this work was superseded; null is not used as the value.
 */
data class Latest<T>(val value: T)

/**
 * Runs [produce] and returns its value only if no newer call has started and this coroutine
 * is still active. Otherwise [discard] runs and the result is null.
 *
 * A cancelled page-load continuation can still resume on the main thread after a newer load
 * has published. Checking the ticket closes that gap; [kotlinx.coroutines.isActive] alone does not.
 */
suspend fun <T> produceLatest(
    gate: AtomicInteger,
    produce: suspend () -> T,
    discard: (T) -> Unit,
): Latest<T>? {
    val ticket = gate.incrementAndGet()
    val value = produce()
    if (ticket != gate.get() || !currentCoroutineContext().isActive) {
        discard(value)
        return null
    }
    return Latest(value)
}

/**
 * Recycles bitmaps only after Compose has had later frames to stop drawing them.
 * Recycling in the same turn as the state write crashes with
 * "Canvas: trying to use a recycled bitmap".
 */
class BitmapRecycler {
    private val queue = ArrayDeque<Pair<Long, Bitmap>>()
    private var seq = 0L

    var version by mutableIntStateOf(0)
        private set

    val latestToken: Long
        get() = seq

    fun retire(
        bitmap: Bitmap?,
        notify: Boolean = true,
    ) {
        if (bitmap == null || bitmap.isRecycled) return
        seq += 1
        queue.addLast(seq to bitmap)
        if (notify) version += 1
    }

    fun retireAll(
        bitmaps: Iterable<Bitmap?>,
        notify: Boolean = true,
    ) {
        var added = false
        bitmaps.forEach { bitmap ->
            if (bitmap != null && !bitmap.isRecycled) {
                seq += 1
                queue.addLast(seq to bitmap)
                added = true
            }
        }
        if (added && notify) version += 1
    }

    fun drainUpTo(token: Long) {
        while (queue.isNotEmpty() && queue.first().first <= token) {
            val bitmap = queue.removeFirst().second
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    fun detach(): List<Bitmap> {
        val leftover = queue.map { it.second }
        queue.clear()
        return leftover
    }
}

/**
 * Bitmap currently shown by Compose. [update] retires the previous bitmap instead of recycling it.
 */
class DisplayedBitmap(
    val recycler: BitmapRecycler,
) {
    var bitmap by mutableStateOf<Bitmap?>(null)
        private set

    fun update(next: Bitmap?) {
        val previous = bitmap
        if (previous === next) return
        bitmap = next
        recycler.retire(previous)
    }

    /** Recycle a bitmap that was never published to [bitmap]. */
    fun recycleUnpublished(bitmap: Bitmap?) {
        if (bitmap == null || bitmap === this.bitmap || bitmap.isRecycled) return
        bitmap.recycle()
    }
}

/**
 * Reference count for bitmaps that are not drawn, but are still copied on a background thread.
 * The last release recycles the bitmap when [stillNeeded] is false.
 */
class BitmapUseCounter {
    private val counts = HashMap<Bitmap, Int>()

    fun acquire(bitmap: Bitmap) {
        counts[bitmap] = (counts[bitmap] ?: 0) + 1
    }

    fun release(
        bitmap: Bitmap,
        stillNeeded: Boolean,
    ) {
        val next = (counts[bitmap] ?: 1) - 1
        if (next > 0) {
            counts[bitmap] = next
            return
        }
        counts.remove(bitmap)
        if (!stillNeeded && !bitmap.isRecycled) bitmap.recycle()
    }

    fun recycleIfUnused(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) return
        if (counts[bitmap] == null) bitmap.recycle()
    }
}

@Composable
fun rememberBitmapRecycler(): BitmapRecycler {
    val recycler = remember { BitmapRecycler() }
    LaunchBitmapRecycler(recycler)
    return recycler
}

@Composable
fun rememberDisplayedBitmap(): DisplayedBitmap {
    val displayed = remember { DisplayedBitmap(BitmapRecycler()) }
    LaunchBitmapRecycler(displayed.recycler) { listOfNotNull(displayed.bitmap) }
    return displayed
}

@Composable
private fun LaunchBitmapRecycler(
    recycler: BitmapRecycler,
    extraOnDispose: () -> List<Bitmap> = { emptyList() },
) {
    LaunchedEffect(recycler.version) {
        val token = recycler.latestToken
        if (token == 0L) return@LaunchedEffect
        // One frame may still draw the previous composition. The next frame draws the new bitmap.
        withFrameNanos { }
        withFrameNanos { }
        recycler.drainUpTo(token)
    }
    DisposableEffect(recycler) {
        onDispose {
            val leftover = (recycler.detach() + extraOnDispose()).distinct()
            if (leftover.isEmpty()) return@onDispose
            Handler(Looper.getMainLooper()).post {
                leftover.forEach { bitmap ->
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            }
        }
    }
}
