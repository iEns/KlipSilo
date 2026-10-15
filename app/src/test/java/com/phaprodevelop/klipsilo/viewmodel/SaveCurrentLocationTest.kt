package com.phaprodevelop.klipsilo.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.LocaleHelper
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.repository.ClipboardRepository
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.phaprodevelop.klipsilo.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class SaveCurrentLocationTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var app: Application
    private lateinit var dao: FakeClipboardItemDao
    private lateinit var viewModel: ClipboardViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        dao = FakeClipboardItemDao()
        Shadows.shadowOf(app).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        viewModel =
            ClipboardViewModel(
                application = app,
                repository = ClipboardRepository(dao, app),
                ioDispatcher = mainDispatcherRule.dispatcher,
            )
    }

    @Test
    fun saveCurrentLocation_withoutPermission_toastsDenied_isLocatingFalse() =
        runTest(mainDispatcherRule.dispatcher) {
            // Arrange
            assertFalse(viewModel.isLocating.value)

            // Act
            viewModel.saveCurrentLocation()
            mainDispatcherRule.scheduler.advanceUntilIdle()

            // Assert
            assertEquals(
                LocaleHelper.applyLocale(app).getString(R.string.location_permission_denied),
                viewModel.toastMessage.value,
            )
            assertFalse(viewModel.isLocating.value)
            assertTrue(dao.getAllOnce().isEmpty())
        }

    @Test
    fun saveCurrentLocation_withFix_updatesClipsFlow() =
        runTest(mainDispatcherRule.dispatcher) {
            Shadows.shadowOf(app).grantPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
            val manager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val shadowManager = Shadows.shadowOf(manager)
            shadowManager.setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
            shadowManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
            val location =
                Location(LocationManager.NETWORK_PROVIDER).apply {
                    latitude = 55.676098
                    longitude = 12.568337
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                    accuracy = 10f
                }
            shadowManager.setLastKnownLocation(LocationManager.NETWORK_PROVIDER, location)

            viewModel.saveCurrentLocation()
            mainDispatcherRule.scheduler.advanceUntilIdle()

            val clips = viewModel.clips.value
            assertEquals(1, clips.size)
            assertEquals(ContentType.LOCATION, clips.single().contentType)
            assertEquals(55.676098, clips.single().latitude)
            assertEquals(12.568337, clips.single().longitude)
            assertEquals(
                LocaleHelper.applyLocale(app).getString(
                    R.string.location_saved,
                    "55.676098, 12.568337",
                ),
                viewModel.toastMessage.value,
            )
            assertEquals(null, viewModel.navigateToNewClip.value)
            assertFalse(viewModel.isLocating.value)
        }
}
