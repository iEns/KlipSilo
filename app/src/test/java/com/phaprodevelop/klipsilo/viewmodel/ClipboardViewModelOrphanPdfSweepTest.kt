@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ClipboardItem
import com.phaprodevelop.klipsilo.data.db.ClipboardItemDao
import com.phaprodevelop.klipsilo.data.repository.ClipboardRepository
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.phaprodevelop.klipsilo.util.MainDispatcherRule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.coroutines.ContinuationInterceptor

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ClipboardViewModelOrphanPdfSweepTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewModelStore = ViewModelStore()

    @After
    fun tearDown() {
        viewModelStore.clear()
    }

    @Test
    fun init_sweepsOrphanPdfsOnInjectedIoDispatcher() {
        // Arrange
        val application = ApplicationProvider.getApplicationContext<Application>()
        val inner = FakeClipboardItemDao()
        val dao = DispatcherRecordingDao(inner)
        val repo = ClipboardRepository(dao, application)
        val orphan =
            File(application.filesDir, "pdfs/orphan.pdf").also {
                it.parentFile?.mkdirs()
                it.writeBytes(byteArrayOf(1, 2, 3))
            }
        val ioDispatcher = StandardTestDispatcher(mainDispatcherRule.scheduler)

        // Act
        val viewModel =
            ClipboardViewModel(
                application = application,
                repository = repo,
                ioDispatcher = ioDispatcher,
            )
        viewModelStore.put("vm", viewModel)
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // Assert — the sweep removed the orphan and its table read ran on io.
        assertFalse(orphan.exists())
        assertTrue(dao.getAllOnceDispatchers.any { it === ioDispatcher })
    }

    private class DispatcherRecordingDao(
        private val inner: FakeClipboardItemDao,
    ) : ClipboardItemDao by inner {
        val getAllOnceDispatchers = mutableListOf<CoroutineDispatcher?>()

        override suspend fun getAllOnce(): List<ClipboardItem> {
            getAllOnceDispatchers +=
                currentCoroutineContext()[ContinuationInterceptor] as? CoroutineDispatcher
            return inner.getAllOnce()
        }
    }
}
