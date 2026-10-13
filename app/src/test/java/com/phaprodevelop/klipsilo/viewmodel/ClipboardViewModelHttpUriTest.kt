@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.repository.ClipboardRepository
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.phaprodevelop.klipsilo.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ClipboardViewModelHttpUriTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewModelStore = ViewModelStore()
    private lateinit var application: Application
    private lateinit var vm: ClipboardViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        val dao = FakeClipboardItemDao()
        val dispatcher = mainDispatcherRule.dispatcher
        vm =
            ClipboardViewModel(
                application,
                ClipboardRepository(dao, application),
                undoTimeoutMs = 4_000L,
                ioDispatcher = dispatcher,
            )
        viewModelStore.put("vm", vm)
        mainDispatcherRule.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        if (!::application.isInitialized) return
        listOf("images", "pdfs", "files").forEach { name ->
            File(application.filesDir, name).deleteRecursively()
        }
    }

    @Test
    fun httpUri_emptyAndBlank_returnsNull() {
        // Arrange / Act / Assert
        assertNull(vm.httpUri(""))
        assertNull(vm.httpUri("   "))
    }

    @Test
    fun httpUri_httpsExample_schemeHttpsAndHost() {
        // Arrange
        val raw = "https://example.com"

        // Act
        val uri = vm.httpUri(raw)

        // Assert
        assertEquals("https", uri?.scheme?.lowercase())
        assertEquals("example.com", uri?.host)
    }

    @Test
    fun httpUri_httpUppercase_schemeHttp() {
        // Arrange
        val raw = "HTTP://Example.COM/x"

        // Act
        val uri = vm.httpUri(raw)

        // Assert
        assertEquals("http", uri?.scheme?.lowercase())
        assertEquals("example.com", uri?.host?.lowercase())
    }

    @Test
    fun httpUri_schemeLess_defaultsToHttps() {
        // Arrange
        val raw = "example.com/path"

        // Act
        val uri = vm.httpUri(raw)

        // Assert
        assertEquals("https", uri?.scheme?.lowercase())
        assertEquals("example.com", uri?.host)
        assertEquals("/path", uri?.path)
        assertEquals("https://example.com/path", uri.toString())
    }

    @Test
    fun httpUri_javascript_returnsNull() {
        // Arrange / Act / Assert
        assertNull(vm.httpUri("javascript://alert(1)"))
        assertNull(vm.httpUri("javascript:alert(1)"))
        assertNull(vm.httpUri("JAVASCRIPT:alert(1)"))
    }

    @Test
    fun httpUri_dataUri_returnsNull() {
        // Arrange / Act / Assert
        assertNull(vm.httpUri("data://text/html,hi"))
        assertNull(vm.httpUri("data:text/html,hi"))
    }

    @Test
    fun httpUri_ftp_returnsNull() {
        // Arrange / Act / Assert
        assertNull(vm.httpUri("ftp://files.example.com"))
    }

    @Test
    fun httpUri_httpsBlankHost_returnsNull() {
        // Arrange / Act / Assert
        assertNull(vm.httpUri("https://"))
    }

    @Test
    fun httpUri_trimmedHttps() {
        // Arrange
        val raw = "  https://ex.com  "

        // Act
        val uri = vm.httpUri(raw)

        // Assert
        assertEquals("https", uri?.scheme?.lowercase())
        assertEquals("ex.com", uri?.host)
    }

    @Test
    fun openUrl_notAUrl_doesNotStartActivity() {
        // Arrange
        val entry =
            ClipboardEntry(
                id = 99L,
                contentType = ContentType.URL,
                textContent = "not a url",
                filePath = null,
                previewText = "not a url",
                timestamp = 1L,
            )

        // Act
        vm.openUrl(entry)

        // Assert
        assertNull(Shadows.shadowOf(application as Application).nextStartedActivity)
    }
}
