@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityShareIntentTest {
    @Test
    fun actionSend_isShareSendTrue() {
        // Arrange
        val intent = Intent(Intent.ACTION_SEND)

        // Act
        val shareSend = intent.isShareSend()

        // Assert
        assertTrue(shareSend)
    }

    @Test
    fun actionMain_isShareSendFalse() {
        // Arrange
        val intent = Intent(Intent.ACTION_MAIN)

        // Act
        val shareSend = intent.isShareSend()

        // Assert
        assertFalse(shareSend)
    }

    @Test
    fun nullAction_isShareSendFalse() {
        // Arrange
        val intent = Intent()

        // Act
        val shareSend = intent.isShareSend()

        // Assert
        assertNull(intent.action)
        assertFalse(shareSend)
    }
}
