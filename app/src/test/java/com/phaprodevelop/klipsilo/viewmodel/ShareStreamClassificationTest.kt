@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.viewmodel

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.repository.ClipboardRepository
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.phaprodevelop.klipsilo.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Share-stream MIME routing (#127). The classifier is the same one
 * saveSharedContent uses, so these cases document which bucket a share lands
 * in before any ContentResolver copy happens.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ShareStreamClassificationTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: ClipboardViewModel

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel =
            ClipboardViewModel(
                application = app,
                repository = ClipboardRepository(FakeClipboardItemDao(), app),
                ioDispatcher = mainDispatcherRule.dispatcher,
            )
    }

    @Test
    fun `image mime types route to the image bucket`() {
        listOf("image/jpeg", "image/png", "image/webp", "image/gif", "image/heic").forEach { mime ->
            val uri = uri("content://share/photo")
            val (image, pdf, file) = viewModel.classifyShareStream(uri, mime)

            assertEquals("$mime image", uri, image)
            assertNull("$mime pdf", pdf)
            assertNull("$mime file", file)
        }
    }

    @Test
    fun `pdf mime or pdf file name routes to the pdf bucket`() {
        listOf("application/pdf", "", "*/*", "text/plain").forEach { mime ->
            val uri = uri("content://share/something.pdf")
            val (image, pdf, file) = viewModel.classifyShareStream(uri, mime)

            assertNull("$mime image", image)
            assertEquals("$mime pdf", uri, pdf)
            assertNull("$mime file", file)
        }
    }

    @Test
    fun `weak mime with image extension routes to the image bucket`() {
        val names = listOf("photo.jpg", "photo.jpeg", "photo.png", "photo.gif", "photo.webp")
        val weakMimes = listOf("", "*/*", "text/plain")
        names.forEach { name ->
            weakMimes.forEach { mime ->
                val uri = uri("content://share/$name")
                val (image, pdf, file) = viewModel.classifyShareStream(uri, mime)

                assertEquals("$name / '$mime' image", uri, image)
                assertNull("$name / '$mime' pdf", pdf)
                assertNull("$name / '$mime' file", file)
            }
        }
    }

    @Test
    fun `other types route to the file bucket`() {
        val cases =
            listOf(
                "content://share/page.html" to "text/html",
                "content://share/archive.zip" to "application/zip",
                "content://share/report.docx" to
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "content://share/blob" to "application/octet-stream",
                "content://share/blob" to "foo/unknown",
                "content://share/notes" to "",
                "content://share/notes.txt" to "*/*",
            )
        cases.forEach { (rawUri, mime) ->
            val uri = uri(rawUri)
            val (image, pdf, file) = viewModel.classifyShareStream(uri, mime)

            assertNull("$rawUri / '$mime' image", image)
            assertNull("$rawUri / '$mime' pdf", pdf)
            assertEquals("$rawUri / '$mime' file", uri, file)
        }
    }

    private fun uri(raw: String): Uri = Uri.parse(raw)
}
