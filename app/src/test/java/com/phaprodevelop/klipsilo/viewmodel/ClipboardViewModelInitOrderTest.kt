@file:Suppress("ktlint:standard:function-naming")

package com.phaprodevelop.klipsilo.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.data.repository.ClipboardRepository
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * #70: on a cold start the Room flow can emit synchronously while the
 * ViewModel is still being constructed (Dispatchers.Main.immediate). The
 * init-block collector then reads the clip-list state before Kotlin has run
 * the property initialisers declared below init. Dispatchers.Unconfined
 * as Main runs the collector and its nested flow coroutines inline, inside
 * the constructor, which reproduces that ordering on the JVM. The NPE
 * lands in viewModelScope and kills the list collector, so the visible
 * symptom checked here is a list that never shows the stored clip.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ClipboardViewModelInitOrderTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun construction_withSynchronousRoomEmission_loadsTheClipList() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = ClipboardRepository(FakeClipboardItemDao(), app)
        runBlocking {
            repo.insertEntry(
                ClipboardEntry(
                    id = 7L,
                    contentType = ContentType.TEXT,
                    textContent = "stored before cold start",
                    previewText = "stored before cold start",
                    timestamp = 1_000L,
                ),
            )
        }

        val viewModel =
            ClipboardViewModel(
                application = app,
                repository = repo,
                ioDispatcher = Dispatchers.Unconfined,
            )

        assertEquals(listOf(7L), viewModel.clips.value.map { it.id })
    }
}
