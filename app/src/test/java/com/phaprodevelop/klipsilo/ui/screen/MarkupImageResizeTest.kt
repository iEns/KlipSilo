package com.phaprodevelop.klipsilo.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Aspect-locked corner resize for images inserted in Mark up (#141). */
class MarkupImageResizeTest {
    private val startX = 0.3f
    private val startY = 0.3f
    private val startWidth = 0.4f
    private val startHeight = 0.2f
    private val epsilon = 1e-4f

    private data class Corner(val left: Boolean, val top: Boolean)

    private val corners =
        listOf(
            Corner(left = true, top = true),
            Corner(left = false, top = true),
            Corner(left = true, top = false),
            Corner(left = false, top = false),
        )

    private fun resize(
        corner: Corner,
        distance: Float,
    ): ImageCornerResize? {
        // Dragging away from the opposite corner: left/up is outward for left/top corners.
        val dragDx = if (corner.left) -distance else distance
        val dragDy = if (corner.top) -distance else distance
        return resizeImageFromCorner(
            startX = startX,
            startY = startY,
            startWidth = startWidth,
            startHeight = startHeight,
            left = corner.left,
            top = corner.top,
            dragDx = dragDx,
            dragDy = dragDy,
        )
    }

    @Test
    fun outwardDragGrowsFromEveryCorner() {
        corners.forEach { corner ->
            val result = resize(corner, distance = 0.05f)

            assertNotNull("corner $corner", result)
            result!!
            assertTrue("width grows for $corner", result.widthFraction > startWidth)
            assertTrue("height grows for $corner", result.heightFraction > startHeight)
            assertEquals(
                "aspect kept for $corner",
                startWidth / startHeight,
                result.widthFraction / result.heightFraction,
                epsilon,
            )
        }
    }

    @Test
    fun inwardDragShrinksFromEveryCorner() {
        corners.forEach { corner ->
            val result = resize(corner, distance = -0.05f)

            assertNotNull("corner $corner", result)
            result!!
            assertTrue("width shrinks for $corner", result.widthFraction < startWidth)
            assertTrue("height shrinks for $corner", result.heightFraction < startHeight)
            assertEquals(
                "aspect kept for $corner",
                startWidth / startHeight,
                result.widthFraction / result.heightFraction,
                epsilon,
            )
        }
    }

    @Test
    fun oppositeCornerStaysAnchored() {
        corners.forEach { corner ->
            val result = resize(corner, distance = 0.05f)!!

            if (corner.left) {
                assertEquals(startX + startWidth, result.xFraction + result.widthFraction, epsilon)
            } else {
                assertEquals(startX, result.xFraction, epsilon)
            }
            if (corner.top) {
                assertEquals(startY + startHeight, result.yFraction + result.heightFraction, epsilon)
            } else {
                assertEquals(startY, result.yFraction, epsilon)
            }
        }
    }

    @Test
    fun hugeInwardDragStopsAtMinimumSize() {
        corners.forEach { corner ->
            val result = resize(corner, distance = -1f)!!

            // Height is the limiting side: 0.05 of the page.
            assertEquals(0.05f, result.heightFraction, epsilon)
            assertEquals(0.1f, result.widthFraction, epsilon)
        }
    }

    @Test
    fun hugeOutwardDragStopsAtPageEdge() {
        corners.forEach { corner ->
            val result = resize(corner, distance = 1f)!!

            // Width is the limiting side: start.x + width = 0.7 of the page.
            assertEquals(0.7f, result.widthFraction, epsilon)
            assertEquals(0.35f, result.heightFraction, epsilon)
            assertTrue(result.xFraction >= 0f)
            assertTrue(result.yFraction >= 0f)
            assertTrue(result.xFraction + result.widthFraction <= 1f + epsilon)
            assertTrue(result.yFraction + result.heightFraction <= 1f + epsilon)
        }
    }

    @Test
    fun degenerateStartReturnsNull() {
        val result =
            resizeImageFromCorner(
                startX = startX,
                startY = startY,
                startWidth = 0f,
                startHeight = startHeight,
                left = false,
                top = false,
                dragDx = 0.1f,
                dragDy = 0.1f,
            )

        assertNull(result)
    }
}
