package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import android.location.Geocoder
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SaveLocationTest {
    private lateinit var dao: FakeClipboardItemDao
    private lateinit var repo: ClipboardRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        dao = FakeClipboardItemDao()
        repo = ClipboardRepository(dao, context)
    }

    @Test
    fun saveLocation_storesTypeCoordinatesAndPreview() =
        runTest {
            // Arrange
            val latitude = 55.676098
            val longitude = 12.568337
            val coordFallback = "55.676098, 12.568337"

            // Act
            repo.saveLocation(latitude, longitude)
            val item = dao.getAllOnce().single()

            // Assert: coordinates always stored as LOCATION
            assertEquals(ContentType.LOCATION, item.contentType)
            assertEquals(latitude, item.latitude)
            assertEquals(longitude, item.longitude)
            assertTrue(item.previewText.isNotBlank())
            if (!Geocoder.isPresent() || item.previewText == coordFallback) {
                assertEquals(coordFallback, item.previewText)
            }
        }

    @Test
    fun saveLocation_emitsInAlreadyCollectingGetAllClips() =
        runTest {
            repo.getAllClips().test {
                assertTrue(awaitItem().isEmpty())

                repo.saveLocation(55.676098, 12.568337)

                val last = awaitItem()
                assertEquals(1, last.size)
                assertEquals(ContentType.LOCATION, last.single().contentType)
                assertEquals(55.676098, last.single().latitude)
                assertEquals(12.568337, last.single().longitude)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
