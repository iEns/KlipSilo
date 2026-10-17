@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeDeleteEndActionTest {
    // enabled=false now consumes the drag with an elastic nudge (#63); the
    // nudge offset is tested below, the end action itself only runs for
    // unlocked rows.

    @Test
    fun lockedNudge_followsTheDragAtAFraction() {
        // Arrange
        val maxPx = 20f

        // Act
        val offset = lockedSwipeNudgeOffset(currentX = 0f, dragAmount = -40f, maxPx = maxPx)

        // Assert
        assertEquals(-10f, offset)
    }

    @Test
    fun lockedNudge_neverExceedsTheMaxOnEitherSide() {
        assertEquals(20f, lockedSwipeNudgeOffset(20f, 500f, 20f))
        assertEquals(-20f, lockedSwipeNudgeOffset(-20f, -500f, 20f))
        assertEquals(0f, lockedSwipeNudgeOffset(0f, 0f, 20f))
    }

    @Test
    fun offsetZero_returnsReset() {
        // Arrange
        val offsetX = 0f

        // Act
        val action = endAction(offsetX)

        // Assert
        assertEquals(SwipeDeleteEnd.RESET, action)
    }

    @Test
    fun offsetMinus599_justAboveDeleteThreshold_returnsReveal() {
        // Arrange
        val offsetX = -599f

        // Act
        val action = endAction(offsetX)

        // Assert
        assertEquals(SwipeDeleteEnd.REVEAL, action)
    }

    @Test
    fun offsetMinus601_returnsDelete() {
        // Arrange
        val offsetX = -601f

        // Act
        val action = endAction(offsetX)

        // Assert
        assertEquals(SwipeDeleteEnd.DELETE, action)
    }

    @Test
    fun offsetMinus600_atDeleteThreshold_returnsReveal() {
        // Arrange
        val offsetX = -600f

        // Act
        val action = endAction(offsetX)

        // Assert
        assertEquals(SwipeDeleteEnd.REVEAL, action)
    }

    @Test
    fun offsetMinus50_returnsReveal() {
        // Arrange
        val offsetX = -50f

        // Act
        val action = endAction(offsetX)

        // Assert
        assertEquals(SwipeDeleteEnd.REVEAL, action)
    }

    @Test
    fun offsetMinus39_returnsReset() {
        // Arrange
        val offsetX = -39f

        // Act
        val action = endAction(offsetX)

        // Assert
        assertEquals(SwipeDeleteEnd.RESET, action)
    }

    private fun endAction(offsetX: Float): SwipeDeleteEnd =
        swipeDeleteEndAction(
            offsetX = offsetX,
            maxWidthPx = MAX_WIDTH_PX,
            revealWidthPx = REVEAL_WIDTH_PX,
        )

    companion object {
        private const val MAX_WIDTH_PX = 1000f
        private const val REVEAL_WIDTH_PX = 80f
    }
}
