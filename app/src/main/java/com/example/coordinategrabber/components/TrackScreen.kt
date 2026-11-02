package com.example.coordinategrabber.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.coordinategrabber.screen.TrackingScreen
import com.example.coordinategrabber.viewModel.LocationViewModel

@Composable
fun TrackScreen(
    locationViewModel: LocationViewModel,
    onRequestPermission: () -> Unit = {}
) {
    val context = LocalContext.current
    val isTracking by locationViewModel.isTracking.collectAsStateWithLifecycle()

    fun hasPermission(): Boolean {
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

    val hasLocationPermission = hasPermission()

    TrackingScreen(
        modifier = Modifier,
        isTracking = isTracking && hasLocationPermission,
        hasPermission = hasLocationPermission,
        onTrackingChange = { enabled ->
            if (hasPermission()) {
                locationViewModel.setTracking(enabled, context)
            } else {
                locationViewModel.setTracking(false, context)
                onRequestPermission()
            }
        },
        onGetLocation = {
            if (hasPermission()) {
                locationViewModel.getLocation()
            } else {
                onRequestPermission()
            }
        }
    )
}
