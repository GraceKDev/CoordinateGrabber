package com.example.coordinategrabber.viewModel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coordinategrabber.data.database.UserQueries
import com.example.coordinategrabber.data.model.Location
import com.example.coordinategrabber.data.model.Route
import com.example.coordinategrabber.location.LocationService
import com.example.coordinategrabber.location.TrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class LocationViewModel(
    private val userQueries: UserQueries,
    private val locationService: LocationService
) : ViewModel() {

    private val _locations = MutableStateFlow<List<Location>>(emptyList())
    val locations: StateFlow<List<Location>> = _locations.asStateFlow()

    private val _manualLocations = MutableStateFlow<List<Location>>(emptyList())
    val manualLocations: StateFlow<List<Location>> = _manualLocations.asStateFlow()

    private val _routes = MutableStateFlow<List<Route>>(emptyList())
    val routes: StateFlow<List<Route>> = _routes.asStateFlow()

    val isTracking: StateFlow<Boolean> = TrackingService.isTracking

    fun setTracking(enabled: Boolean, context: Context) {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = if (enabled) TrackingService.ACTION_START else TrackingService.ACTION_STOP
        }
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            context.startService(intent)
        }
    }

    fun loadLocations() {
        viewModelScope.launch {
            _locations.value = userQueries.getAllLocations()
            _manualLocations.value = userQueries.getManualLocations()
            _routes.value = userQueries.getAllRoutesWithPoints()
        }
    }

    fun deleteLocation(id: Int) {
        viewModelScope.launch {
            userQueries.deleteLocation(id)
            loadLocations()
        }
    }

    fun deleteRoute(routeId: Int) {
        viewModelScope.launch {
            userQueries.deleteRoute(routeId)
            loadLocations()
        }
    }

    @RequiresPermission(
        anyOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ]
    )
    fun getLocation() {
        viewModelScope.launch {
            recordCurrentLocation()
        }
    }

    @RequiresPermission(
        anyOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ]
    )
    private suspend fun recordCurrentLocation() {
        val deviceLocation = locationService.getLocation()

        if (deviceLocation != null) {
            val formattedDate = getCurrentFormattedDate()

            userQueries.addLocation(
                Location(
                    routeId = null,
                    lat = deviceLocation.latitude,
                    lon = deviceLocation.longitude,
                    date = formattedDate
                )
            )

            loadLocations()
        }
    }

    private fun getCurrentFormattedDate(): String {
        val timeZone = TimeZone.getTimeZone("Asia/Bangkok")
        val calendar = Calendar.getInstance(timeZone)

        val formatter = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).apply {
            this.timeZone = timeZone
        }

        return formatter.format(calendar.time)
    }
}
