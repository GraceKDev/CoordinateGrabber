package com.example.coordinategrabber.location

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
    suspend fun getLocation(): Location? {
        return try {
            val lastLoc = locationClient.lastLocation.await()
            if (lastLoc != null) {
                lastLoc
            } else {
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build()
                locationClient.getCurrentLocation(request, null).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build()
                locationClient.getCurrentLocation(request, null).await()
            } catch (fallbackEx: Exception) {
                fallbackEx.printStackTrace()
                null
            }
        }
    }
}
