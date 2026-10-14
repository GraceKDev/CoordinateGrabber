
package com.example.coordinategrabber.viewModel

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coordinategrabber.data.database.UserQueries
import com.example.coordinategrabber.data.model.Location
import com.example.coordinategrabber.location.LocationService
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val _locations =
        MutableStateFlow<List<Location>>(emptyList())

    val locations = _locations.asStateFlow()

    fun loadLocations() {
        viewModelScope.launch {
            _locations.value = userQueries.getAllLocations()
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
            val deviceLocation = locationService.getLocation()

            if (deviceLocation != null) {
                val timeZone = TimeZone.getTimeZone("Asia/Bangkok")
                val calendar = Calendar.getInstance(timeZone)

                val formatter = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).apply {
                    this.timeZone = timeZone
                }

                val formattedDate = formatter.format(calendar.time)

                userQueries.addLocation(
                    Location(
                        lat = deviceLocation.latitude,
                        lon = deviceLocation.longitude,
                        date = formattedDate
                    )
                )

                loadLocations()
            }
        }
    }
}