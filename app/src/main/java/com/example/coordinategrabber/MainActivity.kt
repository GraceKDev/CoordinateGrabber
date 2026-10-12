package com.example.coordinategrabber

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.coordinategrabber.components.Accordion
import com.example.coordinategrabber.data.database.UserQueries
import com.example.coordinategrabber.data.model.Location
import com.example.coordinategrabber.ui.theme.CoordinateGrabberTheme

import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.collections.emptyList

class MainActivity : ComponentActivity() {
    private val locationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private var locations by mutableStateOf<List<Location>>(emptyList())
    private val userQueries by lazy { UserQueries(this) }


    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) @androidx.annotation.RequiresPermission(
        allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION]
    ) {
        permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if(fineLocation || coarseLocation) {
//            getLocation()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        getLocation()
        setContent {
            CoordinateGrabberTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LocationScreen(
                        modifier = Modifier.padding((innerPadding)),
                        onGetLocation = {
                            requestLocation()
                        },
                        locations = locations
                    )
                }
            }
        }
    }
    private fun requestLocation() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if(fineLocationGranted || coarseLocationGranted) {
            getLocation()
        } else {
            locationPermissionLauncher.launch((
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    ))
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun getLocation() {
        locationClient.lastLocation.addOnSuccessListener {
            location ->
            if(location != null) {
                println("Latitude: ${location.latitude}")
                println("Longitude: ${location.longitude}")
                println("Accuracy: ${location.accuracy}")
                val timeZone = TimeZone.getTimeZone("Asia/Bangkok")
                val calendar = Calendar.getInstance(timeZone)
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    this.timeZone = timeZone
                }
                val formattedDate = formatter.format(calendar.time)
                userQueries.addLocation(Location(lat=location.latitude, lon = location.longitude,date=formattedDate))
                locations = userQueries.getAllLocations()
            }
        }
    }
    private fun loadLocations() {
        lifecycleScope.launch {
            locations = userQueries.getAllLocations()
        }
    }
}







@Composable
fun LocationScreen(
    modifier: Modifier = Modifier,
    locations: List<Location>,
    onGetLocation: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {
        Button(
            onClick = onGetLocation,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Get Location")
        }
        Accordion(
            locations=locations,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CoordinateGrabberTheme {
        LocationScreen(
            modifier = Modifier,
            onGetLocation = {},
            locations = emptyList()
        )
    }
}