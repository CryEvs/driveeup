package ru.driveeup.mobile.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import org.osmdroid.util.GeoPoint

/** Последняя известная позиция (GPS/сеть). */
fun getLastKnownGeoPoint(context: Context): GeoPoint? {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
        val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
        return GeoPoint(loc.latitude, loc.longitude)
    }
    return null
}
