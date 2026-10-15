package com.example.coordinategrabber

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.example.coordinategrabber.components.AppNavigationBar
import com.example.coordinategrabber.data.database.UserQueries
import com.example.coordinategrabber.location.LocationService
import com.example.coordinategrabber.screen.LocationScreen
import com.example.coordinategrabber.ui.theme.CoordinateGrabberTheme
import com.example.coordinategrabber.viewModel.LocationViewModel
import com.example.coordinategrabber.viewModel.LocationViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val locationService by lazy {
        LocationService(this)
    }

    private val userQueries by lazy {
        UserQueries(applicationContext)
    }

    private val locationViewModel: LocationViewModel by viewModels {
        LocationViewModelFactory(userQueries, locationService)
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineLocation || coarseLocation) {
            lifecycleScope.launch {
                locationService.getLocation()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        locationPermissionLauncher.launch(permissionsToRequest.toTypedArray())

        setContent {
            CoordinateGrabberTheme {
                AppNavigationBar(
                    modifier = Modifier.fillMaxSize(),
                    locationViewModel = locationViewModel
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CoordinateGrabberTheme {
        LocationScreen(
            modifier = Modifier,
            manualLocations = emptyList(),
            routes = emptyList()
        )
    }
}
