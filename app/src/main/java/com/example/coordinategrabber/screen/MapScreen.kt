package com.example.coordinategrabber.screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.coordinategrabber.components.ScaffoldTemplate
import com.example.coordinategrabber.data.model.Location
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.tan

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

    var selectedPhotoLocation by remember { mutableStateOf<Location?>(null) }
    var showStandardPins by remember { mutableStateOf(true) }
    var showPhotoPins by remember { mutableStateOf(true) }

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

    // Compute merged fog holes asynchronously in background thread to avoid UI main thread lag
    val fogHoles by produceState<List<List<LatLng>>>(initialValue = emptyList(), key1 = locations) {
        value = withContext(Dispatchers.Default) {
            val downsampled = downsampleLocationsForFog(locations, minDistanceMeters = 50.0)
            createMergedFogHoles(downsampled, radiusMeters = 350.0)
        }
    }

    // Filter locations for marker display to avoid creating thousands of Compose Marker nodes
    val markerLocations = remember(locations) {
        downsampleLocationsForMarkers(locations, minDistanceMeters = 150.0)
    }

    val filteredMarkerLocations = remember(markerLocations, showStandardPins, showPhotoPins) {
        markerLocations.filter { loc ->
            val isPhoto = !loc.photoUri.isNullOrBlank()
            if (isPhoto) showPhotoPins else showStandardPins
        }
    }

    val pathPoints = remember(locations) {
        locations.map { LatLng(it.lat, it.lon) }
    }

    ScaffoldTemplate(topBarText = "Map") {
        Box(modifier = modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    minZoomPreference = 8f
                ),
                googleMapOptionsFactory = {
                    GoogleMapOptions()
                        .mapId("1c5ec58774833d997149311a")
                        .minZoomPreference(8f)
                }
            ) {
                val brownCircleIcon = remember {
                    createBrownCircleMarkerIcon()
                }
                val blueCircleIcon = remember {
                    createBlueCircleMarkerIcon()
                }

                val cameraTarget = cameraPositionState.position.target
                val fogBounds = remember(cameraTarget) {
                    val lat = cameraTarget.latitude
                    val lon = cameraTarget.longitude
                    val radius = 5.0

                    listOf(
                        LatLng((lat - radius).coerceAtLeast(-89.0), (lon - radius).coerceAtLeast(-179.0)),
                        LatLng((lat - radius).coerceAtLeast(-89.0), (lon + radius).coerceAtMost(179.0)),
                        LatLng((lat + radius).coerceAtMost(89.0), (lon + radius).coerceAtMost(179.0)),
                        LatLng((lat + radius).coerceAtMost(89.0), (lon - radius).coerceAtLeast(-179.0))
                    )
                }

                // Solid opaque Fog of War layer with asynchronously calculated cutout holes
                Polygon(
                    points = fogBounds,
                    holes = fogHoles,
                    fillColor = Color(0xFF231A14), // Solid opaque dark mocha fog
                    strokeColor = Color.Transparent,
                    strokeWidth = 0f,
                    zIndex = 10f
                )

                if (pathPoints.size >= 2) {
                    Polyline(
                        points = pathPoints,
                        color = Color(0xFF8C5A38),
                        width = 10f,
                        zIndex = 15f
                    )
                }

                filteredMarkerLocations.forEach { location ->
                    val position = LatLng(location.lat, location.lon)
                    val isPhoto = !location.photoUri.isNullOrBlank()
                    val icon = if (isPhoto) blueCircleIcon else brownCircleIcon

                    Marker(
                        state = rememberMarkerState(position = position),
                        title = if (isPhoto) "Photo (${location.lat}, ${location.lon})" else "Location (${location.lat}, ${location.lon})",
                        snippet = location.date,
                        icon = icon,
                        anchor = Offset(0.5f, 0.5f),
                        onClick = {
                            if (isPhoto) {
                                selectedPhotoLocation = location
                                true
                            } else {
                                false
                            }
                        },
                        zIndex = if (isPhoto) 25f else 20f
                    )
                }
            }

            // Floating Pin Filter Chips
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showStandardPins,
                    onClick = { showStandardPins = !showStandardPins },
                    label = { Text("Standard Pins") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF8C5A38),
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = showPhotoPins,
                    onClick = { showPhotoPins = !showPhotoPins },
                    label = { Text("Photo Pins") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1E88E5),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }

    selectedPhotoLocation?.let { photoLoc ->
        AlertDialog(
            onDismissRequest = { selectedPhotoLocation = null },
            title = { Text("Photo Location") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = photoLoc.photoUri,
                        contentDescription = "Location Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Text(
                        text = "Lat: ${photoLoc.lat}, Lon: ${photoLoc.lon}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Date: ${photoLoc.date}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPhotoLocation = null }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Spatial downsampling for Fog Holes: removes redundant duplicate/nearby points
 * within minDistanceMeters to reduce 5,000+ points into ~150-200 representative circles.
 */
private fun downsampleLocationsForFog(
    locations: List<Location>,
    minDistanceMeters: Double = 50.0
): List<Location> {
    if (locations.isEmpty()) return emptyList()
    val result = mutableListOf<Location>()
    var lastLoc: Location? = null

    for (loc in locations) {
        if (lastLoc == null || distanceMeters(lastLoc.lat, lastLoc.lon, loc.lat, loc.lon) >= minDistanceMeters || loc.routeId == null || !loc.photoUri.isNullOrBlank()) {
            result.add(loc)
            lastLoc = loc
        }
    }
    return result
}

/**
 * Spatial downsampling for Markers: limits visible markers on screen to prevent
 * instantiating thousands of Compose Marker nodes on the UI thread.
 * Always includes photo locations (photoUri != null).
 */
private fun downsampleLocationsForMarkers(
    locations: List<Location>,
    minDistanceMeters: Double = 150.0
): List<Location> {
    if (locations.isEmpty()) return emptyList()
    val result = mutableListOf<Location>()
    var lastLoc: Location? = null

    for (loc in locations) {
        if (!loc.photoUri.isNullOrBlank() || loc.routeId == null || lastLoc == null || distanceMeters(lastLoc.lat, lastLoc.lon, loc.lat, loc.lon) >= minDistanceMeters) {
            result.add(loc)
            lastLoc = loc
        }
    }
    return result
}

private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

private fun createMergedFogHoles(
    locations: List<Location>,
    radiusMeters: Double = 350.0
): List<List<LatLng>> {
    if (locations.isEmpty()) return emptyList()

    val uniqueLocations = locations.distinctBy { Pair(it.lat, it.lon) }
    val mergedPath = Path()

    uniqueLocations.forEach { loc ->
        val circlePath = Path()
        val centerX = lngToX(loc.lon)
        val centerY = latToY(loc.lat)
        val radius = (radiusMeters / 6371000.0 * 100000.0).toFloat()

        circlePath.addCircle(centerX, centerY, radius, Path.Direction.CW)
        mergedPath.op(circlePath, Path.Op.UNION)
    }

    val holes = mutableListOf<List<LatLng>>()
    val pathMeasure = PathMeasure(mergedPath, false)

    do {
        val length = pathMeasure.length
        if (length > 0) {
            val contourPoints = mutableListOf<LatLng>()
            val steps = (length / 5.0f).toInt().coerceIn(36, 120)
            val stepSize = length / steps
            val pos = FloatArray(2)

            for (i in steps - 1 downTo 0) {
                val distance = i * stepSize
                pathMeasure.getPosTan(distance, pos, null)
                val lng = xToLng(pos[0])
                val lat = yToLat(pos[1])
                contourPoints.add(LatLng(lat, lng))
            }
            if (contourPoints.size >= 3) {
                holes.add(contourPoints)
            }
        }
    } while (pathMeasure.nextContour())

    return holes
}

private fun latToY(latDeg: Double): Float {
    val latRad = Math.toRadians(latDeg.coerceIn(-85.0, 85.0))
    val y = ln(tan(Math.PI / 4.0 + latRad / 2.0))
    return (y * 100000.0).toFloat()
}

private fun yToLat(y: Float): Double {
    val yNorm = y / 100000.0
    val latRad = 2.0 * atan(exp(yNorm)) - Math.PI / 2.0
    return Math.toDegrees(latRad)
}

private fun lngToX(lngDeg: Double): Float {
    val lngRad = Math.toRadians(lngDeg)
    return (lngRad * 100000.0).toFloat()
}

private fun xToLng(x: Float): Double {
    val lngRad = x / 100000.0
    return Math.toDegrees(lngRad)
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

private fun createBlueCircleMarkerIcon(
    color: Color = Color(0xFF1E88E5),
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
