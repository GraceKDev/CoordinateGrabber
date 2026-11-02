package com.example.coordinategrabber

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.preferencesDataStore
import com.example.coordinategrabber.components.AppNavigationBar
import com.example.coordinategrabber.data.database.UserQueries
import com.example.coordinategrabber.data.settings.SettingsRepository
import com.example.coordinategrabber.location.LocationService
import com.example.coordinategrabber.screen.LocationScreen
import com.example.coordinategrabber.ui.theme.CoordinateGrabberTheme
import com.example.coordinategrabber.viewModel.LocationViewModel
import com.example.coordinategrabber.viewModel.LocationViewModelFactory
import com.example.coordinategrabber.viewModel.SettingsViewModel
import com.example.coordinategrabber.viewModel.SettingsViewModelFactory

class MainActivity : ComponentActivity() {

    private var hasRequestedPermissions = false

    private val locationService by lazy {
        LocationService(this)
    }

    private val userQueries by lazy {
        UserQueries(applicationContext)
    }

    private val locationViewModel: LocationViewModel by viewModels {
        LocationViewModelFactory(userQueries, locationService)
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(SettingsRepository(dataStore))
    }

    private val Context.dataStore by preferencesDataStore(
        name = "settings"
    )

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if ((fineLocation || coarseLocation) && hasLocationPermission(this)) {
            try {
                locationViewModel.getLocation()
            } catch (e: SecurityException) {
                e.printStackTrace()
                locationViewModel.setTracking(false, this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Location permission was denied or restricted
            locationViewModel.setTracking(false, this)
        }
    }

    private fun requestLocationPermissions() {
        val ungrantedPermissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ungrantedPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ungrantedPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ungrantedPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (ungrantedPermissions.isNotEmpty()) {
            val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            if (!showRationale && hasRequestedPermissions) {
                // Permissions permanently denied or dialog cannot show, open System Settings
                openAppSettings()
            } else {
                hasRequestedPermissions = true
                locationPermissionLauncher.launch(ungrantedPermissions.toTypedArray())
            }
        } else {
            // Permissions are already granted!
            try {
                locationViewModel.getLocation()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestLocationPermissions()

        setContent {
            val darkMode by settingsViewModel.darkMode.collectAsState()

            CoordinateGrabberTheme(darkTheme = darkMode) {
                AppNavigationBar(
                    modifier = Modifier.fillMaxSize(),
                    locationViewModel = locationViewModel,
                    settingsViewModel = settingsViewModel,
                    onRequestPermission = { requestLocationPermissions() }
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
