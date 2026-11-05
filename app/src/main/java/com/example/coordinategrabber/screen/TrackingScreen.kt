package com.example.coordinategrabber.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.coordinategrabber.components.ScaffoldTemplate

@Composable
fun TrackingScreen(
    modifier: Modifier = Modifier,
    onGetLocation: () -> Unit,
    onPickPhoto: () -> Unit = {},
    isTracking: Boolean,
    hasPermission: Boolean = true,
    hasMediaPermission: Boolean = true,
    onTrackingChange: (Boolean) -> Unit
) {
    ScaffoldTemplate(
        topBarText = "Track"
    ) {
        TrackingScreenContent(
            modifier = modifier,
            onGetLocation = onGetLocation,
            onPickPhoto = onPickPhoto,
            isTracking = isTracking,
            hasPermission = hasPermission,
            hasMediaPermission = hasMediaPermission,
            onTrackingChange = onTrackingChange
        )
    }
}

@Composable
fun TrackingScreenContent(
    modifier: Modifier = Modifier,
    onGetLocation: () -> Unit,
    onPickPhoto: () -> Unit = {},
    isTracking: Boolean,
    hasPermission: Boolean = true,
    hasMediaPermission: Boolean = true,
    onTrackingChange: (Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Automatically Track Card
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        !hasPermission -> MaterialTheme.colorScheme.errorContainer
                        isTracking -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when {
                            !hasPermission -> "Permission required"
                            isTracking -> "Tracking active"
                            else -> "Not tracking"
                        },
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                        color = when {
                            !hasPermission -> MaterialTheme.colorScheme.onErrorContainer
                            isTracking -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Text(
                    text = "Automatically Track",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when {
                        !hasPermission -> "Location permission required to track"
                        isTracking -> "Your location is being tracked"
                        else -> "Automatically track your location"
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )

                Switch(
                    checked = if (hasPermission) isTracking else false,
                    onCheckedChange = onTrackingChange
                )
            }
        }

        // Add Pin Card
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Pin",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (hasPermission) "Manually add a pin" else "Location permission required to add pin",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = onGetLocation
                ) {
                    Text("Add pin")
                }
            }
        }

        // Upload Photo Card
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Photo Location",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (hasMediaPermission) {
                        "Select a photo with EXIF location metadata to place its location on the map."
                    } else {
                        "Media location permission required to read photo EXIF location."
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedButton(
                    onClick = onPickPhoto
                ) {
                    Text(if (hasMediaPermission) "Select Photo" else "Grant Media Permission")
                }
            }
        }
    }
}
