package com.phaprodevelop.klipsilo.ui.components

import android.graphics.Bitmap
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class BitmapRecyclerTest {
    @Test
    fun updateKeepsPreviousBitmapUntilDrain() {
        val recycler = BitmapRecycler()
        val displayed = DisplayedBitmap(recycler)
        val first = bitmap()
        val second = bitmap()

        displayed.update(first)
        displayed.update(second)

        assertSame(second, displayed.bitmap)
        assertFalse(first.isRecycled)
        assertFalse(second.isRecycled)

        recycler.drainUpTo(recycler.latestToken)

        assertTrue(first.isRecycled)
        assertFalse(second.isRecycled)
    }

    @Test
    fun drainUpToLeavesBitmapsRetiredAfterTheToken() {
        val recycler = BitmapRecycler()
        val first = bitmap()
        val second = bitmap()

        recycler.retire(first)
        val token = recycler.latestToken
        recycler.retire(second)
        recycler.drainUpTo(token)

        assertTrue(first.isRecycled)
        assertFalse(second.isRecycled)

        recycler.drainUpTo(recycler.latestToken)
        assertTrue(second.isRecycled)
    }

    @Test
    fun recycleUnpublishedDoesNotRecycleTheBitmapOnScreen() {
        val displayed = DisplayedBitmap(BitmapRecycler())
        val shown = bitmap()
        val extra = bitmap()
        displayed.update(shown)

        displayed.recycleUnpublished(extra)
        displayed.recycleUnpublished(shown)

        assertTrue(extra.isRecycled)
        assertFalse(shown.isRecycled)
    }

    @Test
    fun detachReturnsQueuedBitmapsWithoutRecyclingThem() {
        val recycler = BitmapRecycler()
        val bitmap = bitmap()
        recycler.retire(bitmap, notify = false)

        val leftover = recycler.detach()

        assertEquals(listOf(bitmap), leftover)
        assertFalse(bitmap.isRecycled)
        assertTrue(recycler.detach().isEmpty())
    }

    @Test
    fun useCounterRecyclesOnlyTheLastReleaseWhenUnneeded() {
        val counter = BitmapUseCounter()
        val bitmap = bitmap()
        counter.acquire(bitmap)
        counter.acquire(bitmap)

        counter.release(bitmap, stillNeeded = false)
        assertFalse(bitmap.isRecycled)

        counter.release(bitmap, stillNeeded = true)
        assertFalse(bitmap.isRecycled)

        counter.acquire(bitmap)
        counter.release(bitmap, stillNeeded = false)
        assertTrue(bitmap.isRecycled)
    }

    @Test
    fun recycleIfUnusedSkipsBitmapsStillAcquired() {
        val counter = BitmapUseCounter()
        val bitmap = bitmap()
        counter.acquire(bitmap)

        counter.recycleIfUnused(bitmap)
        assertFalse(bitmap.isRecycled)

        counter.release(bitmap, stillNeeded = false)
        assertTrue(bitmap.isRecycled)
    }

    @Test
    fun produceLatestDropsAResultSupersededDuringWork() =
        runTest {
            val gate = AtomicInteger(0)
            var discarded: String? = null

            val result =
                produceLatest(
                    gate,
                    produce = {
                        gate.incrementAndGet()
                        "old"
                    },
                    discard = { discarded = it },
                )

            assertNull(result)
            assertEquals("old", discarded)
        }

    @Test
    fun produceLatestReturnsTheLatestResult() =
        runTest {
            val result =
                produceLatest(
                    AtomicInteger(0),
                    produce = { "new" },
                    discard = { },
                )

            assertEquals("new", result?.value)
        }

    private fun bitmap(): Bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
}
