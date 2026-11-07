@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.screen

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * #184: after Pin / Unpin / Make Markdown the toggled row re-sorts while the
 * keyed LazyColumn keeps its index anchor, so the scroll effect must bring the
 * row back into view or it looks like the clip had vanished.
 */
@RunWith(RobolectricTestRunner::class)
class ScrollToClipEffectTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun entry(
        id: Long,
        title: String,
        ts: Long,
    ): ClipboardEntry =
        ClipboardEntry(
            id = id,
            contentType = ContentType.TEXT,
            previewText = title,
            timestamp = ts,
        )

    @Test
    fun movedRowIsScrolledIntoViewAfterResort() {
        val target = entry(30L, "deeptarget", 30_000L)
        val before = (1L..29L).map { entry(it, "clip$it", it * 1_000L) } + target
        // The DAO sorts pinned rows first (by timestamp DESC), so a freshly
        // pinned target lands at index 0.
        val after = listOf(target) + before.dropLast(1)
        val holder = mutableListOf<LazyListState>()
        var clips by mutableStateOf(before)
        var scrollRequest by mutableStateOf(false)
        var scrollTarget by mutableStateOf(0L)
        var consumed = false

        composeRule.setContent {
            MaterialTheme {
                val state = rememberLazyListState()
                holder.clear()
                holder.add(state)
                LazyColumn(state = state) {
                    items(clips, key = { it.id }) { item ->
                        Text(item.previewText)
                    }
                }
                // Stand-in for the user scrolling the deep row into view.
                LaunchedEffect(scrollRequest) {
                    if (scrollRequest) {
                        state.scrollToItem(24)
                    }
                }
                ScrollToClipEffect(
                    scrollTargetId = scrollTarget,
                    clips = clips,
                    listState = state,
                    onConsumed = { consumed = true },
                )
            }
        }
        composeRule.waitForIdle()

        // Deep row in view, like a user pinning it mid-list.
        composeRule.runOnIdle { scrollRequest = true }
        composeRule.onNodeWithText("deeptarget").assertIsDisplayed()

        // Pin it (the DAO re-sorts it to the top) and surface the scroll event,
        // the way ClipboardViewModel.togglePin does.
        composeRule.runOnIdle {
            clips = after
            scrollTarget = target.id
        }
        composeRule.waitForIdle()

        // The moved row stays visible, back at the top of the pinned block.
        assertTrue(consumed)
        composeRule.onNodeWithText("deeptarget").assertIsDisplayed()
        assertEquals(0, holder.last().firstVisibleItemIndex)
    }
}
