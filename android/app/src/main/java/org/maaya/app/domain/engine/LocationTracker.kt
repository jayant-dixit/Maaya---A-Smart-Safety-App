package org.maaya.app.domain.engine

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class LocationTracker(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun hasLocationPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isGpsEnabled(): Boolean {
        return try {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Location? {
        if (!hasLocationPermissions()) {
            Log.w("LocationTracker", "Location permissions missing when requesting last known location")
            return null
        }

        // 1. Try Google Play Services Fused Location
        val fusedLoc: Location? = try {
            withTimeoutOrNull(2000L) {
                suspendCancellableCoroutine { continuation ->
                    fusedClient.lastLocation
                        .addOnSuccessListener { loc -> continuation.resume(loc) }
                        .addOnFailureListener { continuation.resume(null) }
                        .addOnCanceledListener { continuation.resume(null) }
                }
            }
        } catch (e: Exception) {
            Log.w("LocationTracker", "Fused last location error", e)
            null
        }

        if (fusedLoc != null && fusedLoc.latitude != 0.0 && fusedLoc.longitude != 0.0) {
            Log.i("LocationTracker", "Using fused last location: ${fusedLoc.latitude}, ${fusedLoc.longitude} (acc: ${fusedLoc.accuracy}m)")
            return fusedLoc
        }

        // 2. Fallback to Android native LocationManager (GPS, Network, Passive)
        val gpsLoc = try {
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else null
        } catch (_: Exception) { null }

        val netLoc = try {
            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else null
        } catch (_: Exception) { null }

        val passiveLoc = try {
            locationManager?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        } catch (_: Exception) { null }

        // Select the most recent and valid location fix
        val candidates = listOfNotNull(gpsLoc, netLoc, passiveLoc)
            .filter { it.latitude != 0.0 && it.longitude != 0.0 }
            .sortedByDescending { it.time }

        val best = candidates.firstOrNull()
        if (best != null) {
            Log.i("LocationTracker", "Using LocationManager fallback: ${best.latitude}, ${best.longitude} (provider: ${best.provider})")
        } else {
            Log.w("LocationTracker", "No cached location found from any provider")
        }
        return best
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentHighAccuracyLocation(): Location? {
        if (!hasLocationPermissions()) {
            Log.w("LocationTracker", "Location permissions missing when requesting high accuracy location")
            return null
        }

        // 1. Request fresh location from Google Play Services with strict 3.5s timeout
        val freshFusedLoc: Location? = try {
            withTimeoutOrNull(3500L) {
                suspendCancellableCoroutine { continuation ->
                    val priority = Priority.PRIORITY_HIGH_ACCURACY
                    fusedClient.getCurrentLocation(priority, null)
                        .addOnSuccessListener { loc -> continuation.resume(loc) }
                        .addOnFailureListener { continuation.resume(null) }
                        .addOnCanceledListener { continuation.resume(null) }
                }
            }
        } catch (e: Exception) {
            Log.w("LocationTracker", "Fresh fused location request failed/timed out", e)
            null
        }

        if (freshFusedLoc != null && freshFusedLoc.latitude != 0.0 && freshFusedLoc.longitude != 0.0) {
            Log.i("LocationTracker", "Fresh GPS fix acquired: ${freshFusedLoc.latitude}, ${freshFusedLoc.longitude}")
            return freshFusedLoc
        }

        // 2. If fresh fix timed out (e.g. indoors or satellite search delay), fallback to best last known fix
        return getLastKnownLocation()
    }

    @SuppressLint("MissingPermission")
    fun getContinuousLocationUpdates(intervalMs: Long = 3000L): Flow<Location> = callbackFlow {
        if (!hasLocationPermissions()) {
            Log.w("LocationTracker", "Cannot start continuous updates: missing permissions")
            close()
            return@callbackFlow
        }

        // 1. Fused Location Provider listener
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false)
            .build()

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (loc in result.locations) {
                    if (loc.latitude != 0.0 && loc.longitude != 0.0) {
                        trySend(loc)
                    }
                }
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.w("LocationTracker", "Could not register fused location updates", e)
        }

        // 2. Android LocationManager native GPS & Network listeners as secondary guarantee
        val nativeListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                if (loc.latitude != 0.0 && loc.longitude != 0.0) {
                    trySend(loc)
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    intervalMs,
                    0f,
                    nativeListener,
                    Looper.getMainLooper()
                )
            }
            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    intervalMs,
                    0f,
                    nativeListener,
                    Looper.getMainLooper()
                )
            }
        } catch (e: Exception) {
            Log.w("LocationTracker", "Could not register native LocationManager updates", e)
        }

        // 3. Immediately emit best cached location so listeners have an immediate coordinate without waiting
        getLastKnownLocation()?.let { cached ->
            trySend(cached)
        }

        awaitClose {
            try {
                fusedClient.removeLocationUpdates(fusedCallback)
            } catch (_: Exception) {}
            try {
                locationManager?.removeUpdates(nativeListener)
            } catch (_: Exception) {}
        }
    }
}
