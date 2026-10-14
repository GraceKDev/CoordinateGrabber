
package com.example.coordinategrabber.location

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await


class LocationService(context: Context) {

    private val locationClient =
        LocationServices.getFusedLocationProviderClient(context)

    @RequiresPermission(
        anyOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ]
    )
    suspend fun getLocation(): android.location.Location? {
        return locationClient.lastLocation.await()
    }
}