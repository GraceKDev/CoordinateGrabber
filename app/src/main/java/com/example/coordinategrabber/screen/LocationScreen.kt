package com.example.coordinategrabber.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.coordinategrabber.components.Accordion
import com.example.coordinategrabber.components.RouteAccordion
import com.example.coordinategrabber.components.ScaffoldTemplate
import com.example.coordinategrabber.data.model.Location
import com.example.coordinategrabber.data.model.Route

@Composable
fun LocationScreen(
    modifier: Modifier = Modifier,
    manualLocations: List<Location> = emptyList(),
    routes: List<Route> = emptyList(),
    onDeleteLocation: (Int) -> Unit = {},
    onDeleteRoute: (Int) -> Unit = {}
) {
    ScaffoldTemplate("History") {
        LocationScreenContent(
            modifier = modifier,
            manualLocations = manualLocations,
            routes = routes,
            onDeleteLocation = onDeleteLocation,
            onDeleteRoute = onDeleteRoute
        )
    }
}

@Composable
fun LocationScreenContent(
    modifier: Modifier = Modifier,
    manualLocations: List<Location> = emptyList(),
    routes: List<Route> = emptyList(),
    onDeleteLocation: (Int) -> Unit = {},
    onDeleteRoute: (Int) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Accordion(
            locations = manualLocations,
            onDeleteLocation = onDeleteLocation,
            modifier = Modifier.fillMaxWidth()
        )

        RouteAccordion(
            routes = routes,
            onDeleteRoute = onDeleteRoute,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
