package com.example.coordinategrabber.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

    // ActivityResultContracts.GetContent() opens the system media/document picker which preserves original EXIF GPS metadata
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            locationViewModel.addPhotoLocation(context, uri)
        }
    }

    fun hasLocationPermission(): Boolean {
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

    fun hasMediaLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_MEDIA_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val locationGranted = hasLocationPermission()
    val mediaLocationGranted = hasMediaLocationPermission()

    TrackingScreen(
        modifier = Modifier,
        isTracking = isTracking && locationGranted,
        hasPermission = locationGranted,
        hasMediaPermission = mediaLocationGranted,
        onTrackingChange = { enabled ->
            if (hasLocationPermission()) {
                locationViewModel.setTracking(enabled, context)
            } else {
                locationViewModel.setTracking(false, context)
                onRequestPermission()
            }
        },
        onGetLocation = {
            if (hasLocationPermission()) {
                locationViewModel.getLocation()
            } else {
                onRequestPermission()
            }
        },
        onPickPhoto = {
            if (hasMediaLocationPermission()) {
                imagePickerLauncher.launch("image/*")
            } else {
                onRequestPermission()
            }
        }
    )
}
