package com.example.coordinategrabber.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.coordinategrabber.R
import androidx.navigation.compose.rememberNavController
import java.lang.reflect.Modifier

@Composable
fun NavigationBar(modifier: Modifier) {
    val navController = rememberNavController()
    val startDestination = Destination.HOME
    var selectedDestination by rememberSaveable{mutableIntStateOf(startDestination.ordinal)) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                Destination.entries.forEachIndexed { index, destination ->
                    NavigationBarItem(selectedDestination == index,
                        onClick = {
                            navController.navigate(route = destination.route)
                            selectedDestination = index
                        },
                        icon = {
                            Icon(
                                destination.icon,
                                contentDescription = destination.contentDescription
                            )
                        },
                        label =  {Text(destination.label) }
                    )
                }
            }
        }
    ) { contentPadding ->
        AppNavHost(navController,startDestination,modifier = modifier.padding(contentPadding))
    }
}

enum class Destination(
    val route:String,
    val label:String,
    val icon: Int,
    val contentDescription: String
) {
    HOME("home","Home", R.drawable.home,"Home"),
    MAP("map","Map",R.drawable.globe,"Map"),
    History("history","History",R.drawable.history,"History")
}