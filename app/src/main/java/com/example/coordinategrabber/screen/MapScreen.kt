package com.example.coordinategrabber.screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.coordinategrabber.components.ScaffoldTemplate
import com.example.coordinategrabber.data.model.Location
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    locations: List<Location> = emptyList()
) {
    val defaultLocation = locations.lastOrNull()?.let {
        LatLng(it.lat, it.lon)
    } ?: LatLng(13.7563, 100.5018)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    LaunchedEffect(locations) {
        locations.lastOrNull()?.let { lastLocation ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(lastLocation.lat, lastLocation.lon),
                    12f
                )
            )
        }
    }

    ScaffoldTemplate(topBarText = "Map") {
        GoogleMap(
            modifier = modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            googleMapOptionsFactory = {
                GoogleMapOptions()
                    .mapId("1c5ec58774833d997149311a")
            }
        ) {
            val brownCircleIcon = remember {
                createBrownCircleMarkerIcon()
            }

            val pathPoints = remember(locations) {
                locations.map { LatLng(it.lat, it.lon) }
            }

            if (pathPoints.size >= 2) {
                Polyline(
                    points = pathPoints,
                    color = Color(0xFF8C5A38),
                    width = 10f
                )
            }

            locations.forEach { location ->
                val position = LatLng(location.lat, location.lon)
                Marker(
                    state = rememberMarkerState(position = position),
                    title = "Location (${location.lat}, ${location.lon})",
                    snippet = location.date,
                    icon = brownCircleIcon,
                    anchor = Offset(0.5f, 0.5f)
                )
            }
        }
    }
}

private fun createBrownCircleMarkerIcon(
    color: Color = Color(0xFF8C5A38),
    sizePx: Int = 48
): BitmapDescriptor {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)


    val borderPaint = Paint().apply {
        isAntiAlias = true
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, borderPaint)


    val fillPaint = Paint().apply {
        isAntiAlias = true
        this.color = color.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, (sizePx / 2f) - 4f, fillPaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
