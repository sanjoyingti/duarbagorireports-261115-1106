package com.arin.civicreport.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.arin.civicreport.model.ReportLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {

    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Last known position, may be null. */
    @SuppressLint("MissingPermission")
    suspend fun lastKnown(): Location? {
        if (!hasPermission()) return null
        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(4000) {
                suspendCancellableCoroutine { cont ->
                    client.lastLocation
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resume(null) }
                }
            }
        }
    }

    /** Requests a fresh high-accuracy fix, falling back to the last known position. */
    @SuppressLint("MissingPermission")
    suspend fun currentPosition(highAccuracy: Boolean): Location? {
        if (!hasPermission()) return null
        val priority = if (highAccuracy) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        val fresh = withContext(Dispatchers.Main) {
            withTimeoutOrNull(15_000) {
                suspendCancellableCoroutine { cont ->
                    val request = LocationRequest.Builder(priority, 3000L)
                        .setMinUpdateIntervalMillis(1000L)
                        .setMaxUpdates(1)
                        .setWaitForAccurateLocation(false)
                        .build()
                    val callback = object : com.google.android.gms.location.LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            val loc = result.lastLocation
                            if (loc != null && cont.isActive) cont.resume(loc)
                        }
                    }
                    client.requestLocationUpdates(request, callback, context.mainLooper)
                        .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                }
            }
        }
        return fresh ?: lastKnown()
    }

    /** Converts coordinates into a readable address. */
    suspend fun reverseGeocode(lat: Double, lon: Double): ReportLocation =
        withContext(Dispatchers.IO) {
            val base = ReportLocation(latitude = lat, longitude = lon)
            if (!Geocoder.isPresent()) return@withContext base
            @Suppress("DEPRECATION")
            val addresses = runCatching { Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1) }
                .getOrNull()
            val a = addresses?.firstOrNull() ?: return@withContext base
            val line = (0..a.maxAddressLineIndex).mapNotNull { a.getAddressLine(it) }.joinToString(", ")
            base.copy(
                address = line.ifBlank { a.locality.orEmpty() },
                city = a.locality ?: a.subAdminArea.orEmpty(),
                district = a.subAdminArea.orEmpty(),
                state = a.adminArea.orEmpty(),
                postcode = a.postalCode.orEmpty()
            )
        }

    fun isGpsEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
    }
}
