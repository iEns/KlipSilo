package com.phaprodevelop.klipsilo.ui.screen

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.phaprodevelop.klipsilo.data.repository.FormFieldHit
import com.phaprodevelop.klipsilo.data.repository.FormFieldType.CHECKBOX
import com.phaprodevelop.klipsilo.data.repository.FormFieldType.CHOICE
import com.phaprodevelop.klipsilo.data.repository.FormFieldType.OTHER
import com.phaprodevelop.klipsilo.data.repository.FormFieldType.RADIO
import com.phaprodevelop.klipsilo.data.repository.FormFieldType.TEXT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The appearance cover behind the fill-preview overlay (#80): the PdfRenderer
 * page bitmap already paints a TEXT/CHOICE widget's own value, so the overlay
 * must hide it before drawing the live value or the texts overlap.
 */
@RunWith(RobolectricTestRunner::class)
class FormValueOverlayCoverTest {
    @Test
    fun coverAppliesToTextAndChoiceOnly() {
        assertTrue(coversAppearance(TEXT))
        assertTrue(coversAppearance(CHOICE))
        assertFalse(coversAppearance(CHECKBOX))
        assertFalse(coversAppearance(RADIO))
        assertFalse(coversAppearance(OTHER))
    }

    @Test
    fun coverRectInsetsTheWidgetRectByTheInset() {
        val rect = appearanceCoverRect(rectWidthPx = 100f, rectHeightPx = 30f, insetPx = 2f)
        assertEquals(2f, rect.left)
        assertEquals(2f, rect.top)
        assertEquals(98f, rect.right)
        assertEquals(28f, rect.bottom)
    }

    @Test
    fun coverRectClampsTheInsetOnHairlineWidgets() {
        // 4x4 px: a quarter of each dimension is the clamp floor, so the rect
        // still gets covered completely instead of inverting.
        val rect = appearanceCoverRect(rectWidthPx = 4f, rectHeightPx = 4f, insetPx = 2f)
        assertEquals(1f, rect.left)
        assertEquals(1f, rect.top)
        assertEquals(3f, rect.right)
        assertEquals(3f, rect.bottom)
    }

    @Test
    fun coverRectNeverInvertsOnZeroSizeWidgets() {
        val rect = appearanceCoverRect(rectWidthPx = 0f, rectHeightPx = 0f, insetPx = 2f)
        assertEquals(0f, rect.left)
        assertEquals(0f, rect.top)
        assertEquals(0f, rect.right)
        assertEquals(0f, rect.bottom)
    }

    @Test
    fun backgroundSampledInsideTheWidgetRect() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        val hit = FormFieldHit(pageIndex = 0, left = 0.1f, top = 0.2f, right = 0.9f, bottom = 0.3f)
        assertEquals(Color.White, sampleFieldBackground(bitmap, hit))
        bitmap.eraseColor(android.graphics.Color.rgb(240, 238, 230))
        assertEquals(Color(240, 238, 230), sampleFieldBackground(bitmap, hit))
    }

    @Test
    fun sampledBackgroundIsForcedOpaque() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        val hit = FormFieldHit(pageIndex = 0, left = 0f, top = 0f, right = 1f, bottom = 1f)
        val color = sampleFieldBackground(bitmap, hit)
        assertEquals(1f, color.alpha)
    }

    @Test
    fun backgroundIgnoresTheBorderAndTheFieldsOwnText() {
        // A 16 pt tall field like event-registration.pdf's "Ticket type": light
        // fill, grey 1 px border, dark value text on the left third.
        val fill = android.graphics.Color.rgb(249, 249, 250)
        val bitmap = Bitmap.createBitmap(200, 40, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        for (x in 10 until 190) {
            for (y in 10 until 26) {
                val border = x == 10 || x == 189 || y == 10 || y == 25
                val text = x in 14 until 70 && y in 13 until 23 && (x + y) % 3 != 0
                val color =
                    when {
                        border -> android.graphics.Color.rgb(158, 158, 158)
                        text -> android.graphics.Color.rgb(26, 26, 26)
                        else -> fill
                    }
                bitmap.setPixel(x, y, color)
            }
        }
        val hit = FormFieldHit(pageIndex = 0, left = 0.05f, top = 0.25f, right = 0.95f, bottom = 0.65f)
        assertEquals(Color(fill), sampleFieldBackground(bitmap, hit))
    }
}
