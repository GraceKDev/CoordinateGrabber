package com.example.coordinategrabber.components

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.coordinategrabber.screen.TrackingScreen
import com.example.coordinategrabber.viewModel.LocationViewModel

@SuppressLint("MissingPermission")
@Composable
fun TrackScreen(locationViewModel: LocationViewModel) {
    val context = LocalContext.current
    val isTracking by locationViewModel.isTracking.collectAsStateWithLifecycle()

    TrackingScreen(
        modifier = Modifier,
        isTracking = isTracking,
        onTrackingChange = { enabled ->
            locationViewModel.setTracking(enabled, context)
        },
        onGetLocation = {
            locationViewModel.getLocation()
        }
    )
}
