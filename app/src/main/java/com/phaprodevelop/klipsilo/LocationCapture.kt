package com.phaprodevelop.klipsilo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val PREFS = "klipsilo_perms"
private const val KEY_LOCATION_ASKED = "location_permission_requested"
private const val LOCATION_TIMEOUT_MS = 12_000L
private const val FRESH_LOCATION_MAX_AGE_MS = 30_000L

fun hasLocationPermission(context: Context): Boolean {
    val fine =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    val coarse =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

fun isLocationEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        manager.isLocationEnabled
    } else {
        @Suppress("DEPRECATION")
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

fun wasLocationPermissionAsked(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_LOCATION_ASKED, false)

fun markLocationPermissionAsked(context: Context) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_LOCATION_ASKED, true)
        .apply()
}

@SuppressLint("MissingPermission")
suspend fun obtainCurrentLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val fineGranted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    val recent = bestLastKnown(manager, fineGranted)
    if (recent != null && System.currentTimeMillis() - recent.time <= FRESH_LOCATION_MAX_AGE_MS) {
        return recent
    }

    val providers = linkedSetOf<String>()
    if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        providers.add(LocationManager.NETWORK_PROVIDER)
    }
    if (fineGranted && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        providers.add(LocationManager.GPS_PROVIDER)
    }
    if (providers.isEmpty() && manager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
        providers.add(LocationManager.PASSIVE_PROVIDER)
    }

    for (provider in providers) {
        val location = awaitProviderLocation(manager, provider, LOCATION_TIMEOUT_MS)
        if (location != null) return location
    }
    return bestLastKnown(manager, fineGranted)
}

@SuppressLint("MissingPermission")
private fun bestLastKnown(
    manager: LocationManager,
    fineGranted: Boolean,
): Location? {
    val candidates = mutableListOf<Location>()
    try {
        if (fineGranted) {
            manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { candidates.add(it) }
        }
        manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let { candidates.add(it) }
        manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)?.let { candidates.add(it) }
    } catch (_: SecurityException) {
        return null
    }
    return candidates.maxByOrNull { it.time }
}

@SuppressLint("MissingPermission")
private suspend fun awaitProviderLocation(
    manager: LocationManager,
    provider: String,
    timeoutMs: Long,
): Location? =
    withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { cont ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                try {
                    manager.getCurrentLocation(
                        provider,
                        signal,
                        Runnable::run,
                    ) { location ->
                        if (cont.isActive) cont.resume(location)
                    }
                } catch (_: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            } else {
                val listener =
                    object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            try {
                                manager.removeUpdates(this)
                            } catch (_: Exception) {
                            }
                            if (cont.isActive) cont.resume(location)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(
                            provider: String?,
                            status: Int,
                            extras: Bundle?,
                        ) {
                        }

                        override fun onProviderEnabled(provider: String) {}

                        override fun onProviderDisabled(provider: String) {}
                    }
                cont.invokeOnCancellation {
                    try {
                        manager.removeUpdates(listener)
                    } catch (_: Exception) {
                    }
                }
                try {
                    @Suppress("DEPRECATION")
                    manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                } catch (_: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }
