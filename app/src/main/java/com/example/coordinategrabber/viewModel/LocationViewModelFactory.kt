package com.example.coordinategrabber.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.coordinategrabber.data.database.UserQueries
import com.example.coordinategrabber.location.LocationService

class LocationViewModelFactory(
    private val userQueries: UserQueries,
    private val locationService: LocationService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(
                LocationViewModel::class.java
            )
        ) {
            @Suppress("UNCHECKED_CAST")
            return LocationViewModel(
                userQueries,
                locationService
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}