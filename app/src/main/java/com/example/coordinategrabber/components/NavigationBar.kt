
package com.example.coordinategrabber.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar as MaterialNavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.example.coordinategrabber.R
import com.example.coordinategrabber.viewModel.LocationViewModel

@Composable
fun AppNavigationBar(
    modifier: Modifier = Modifier,
    locationViewModel: LocationViewModel
) {
    val navController = rememberNavController()
    val startDestination = Destination.HOME

    var selectedDestination by rememberSaveable {
        mutableIntStateOf(startDestination.ordinal)
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            MaterialNavigationBar(
                windowInsets = NavigationBarDefaults.windowInsets
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
                            Text(destination.label)
                        }
                    )
                }
            }
        }
    ) { contentPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(contentPadding),
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
        startDestination = Destination.HOME.route,
        modifier = modifier
    ) {
        composable(Destination.HOME.route) {
            HomeScreen()
        }

        composable(Destination.MAP.route) {
            MapScreen()
        }

        composable(Destination.HISTORY.route) {
            HistoryScreen(locationViewModel =  locationViewModel)
        }
    }
}

@Composable
fun HomeScreen() {
    PlaceholderScreen("Home Screen")
}

@Composable
fun MapScreen() {
    PlaceholderScreen("Map Screen")
}



@Composable
fun HistoryScreen(
    locationViewModel: LocationViewModel
) {
    val locations by locationViewModel.locations
        .collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        locationViewModel.getLocation()
    }

    LocationScreen(
        locations = locations,
        onGetLocation = {
            locationViewModel.getLocation()
        }
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
    HOME("home", "Home", R.drawable.home, "Home"),
    MAP("map", "Map", R.drawable.globe, "Map"),
    HISTORY("history", "History", R.drawable.history, "History")
}
