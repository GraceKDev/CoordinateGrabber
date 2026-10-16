
package com.example.coordinategrabber.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar as MaterialNavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.example.coordinategrabber.R
import com.example.coordinategrabber.screen.LocationScreen
import com.example.coordinategrabber.screen.MapScreen
import com.example.coordinategrabber.screen.TrackingScreen
import com.example.coordinategrabber.viewModel.LocationViewModel

@Composable
fun AppNavigationBar(
    modifier: Modifier = Modifier,
    locationViewModel: LocationViewModel
) {
    val navController = rememberNavController()
    val startDestination = Destination.MAP

    var selectedDestination by rememberSaveable {
        mutableIntStateOf(startDestination.ordinal)
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            MaterialNavigationBar(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                windowInsets = NavigationBarDefaults.windowInsets,
            ) {
                Destination.entries.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedDestination == index,
                        onClick = {
                            selectedDestination = index

                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(destination.icon),
                                contentDescription =
                                    destination.contentDescription
                            )
                        },
                        label = {
                            Text(
                                text = destination.label,
                                fontWeight = if (selectedDestination == index) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { contentPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
            locationViewModel = locationViewModel
        )
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    locationViewModel: LocationViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Destination.MAP.route,
        modifier = modifier
    ) {
        composable(Destination.MAP.route) {
            val locations by locationViewModel.locations
                .collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                locationViewModel.loadLocations()
            }

            MapScreen(
                locations = locations
            )
        }

        composable(Destination.TRACK.route) {
            TrackScreen(locationViewModel =  locationViewModel)
        }

        composable(Destination.HISTORY.route) {
            HistoryScreen(locationViewModel =  locationViewModel)
        }
    }
}



//@Composable
//fun MapScreen() {
//    ScaffoldTemplate("Map") {
//        PlaceholderScreen("Map Screen")
//    }
//}



@Composable
fun HistoryScreen(
    locationViewModel: LocationViewModel
) {
    val manualLocations by locationViewModel.manualLocations
        .collectAsStateWithLifecycle()
    val routes by locationViewModel.routes
        .collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        locationViewModel.loadLocations()
    }
    LocationScreen(
        manualLocations = manualLocations,
        routes = routes,
        onDeleteLocation = { locationViewModel.deleteLocation(it) },
        onDeleteRoute = { locationViewModel.deleteRoute(it) }
    )
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(title)
    }
}

enum class Destination(
    val route: String,
    val label: String,
    val icon: Int,
    val contentDescription: String
) {

    MAP("map", "Map", R.drawable.globe, "Map"),
    TRACK("track", "Track", R.drawable.track, "Track"),
    HISTORY("history", "History", R.drawable.history, "History"),

}
