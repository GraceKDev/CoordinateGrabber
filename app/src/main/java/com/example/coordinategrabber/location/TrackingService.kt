package com.example.coordinategrabber.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.coordinategrabber.MainActivity
import com.example.coordinategrabber.R
import com.example.coordinategrabber.data.database.UserQueries
import com.example.coordinategrabber.data.model.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration.Companion.milliseconds

class TrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var trackingJob: Job? = null
    private var currentRouteId: Int? = null

    private lateinit var userQueries: UserQueries
    private lateinit var locationService: LocationService

    override fun onCreate() {
        super.onCreate()
        userQueries = UserQueries(applicationContext)
        locationService = LocationService(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        if (_isTracking.value) return
        _isTracking.value = true

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val formattedDate = getCurrentFormattedDate()
        val existingRouteCount = userQueries.getAllRoutesWithPoints().size
        currentRouteId = userQueries.createRoute("Route #${existingRouteCount + 1}", formattedDate)

        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            while (isActive && _isTracking.value) {
                recordCurrentLocation(currentRouteId)
                delay(30_000L.milliseconds)
            }
        }
    }

    private fun stopTracking() {
        _isTracking.value = false
        trackingJob?.cancel()
        currentRouteId = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private suspend fun recordCurrentLocation(routeId: Int?) {
        try {
            val deviceLocation = locationService.getLocation()
            if (deviceLocation != null) {
                val formattedDate = getCurrentFormattedDate()
                userQueries.addLocation(
                    Location(
                        routeId = routeId,
                        lat = deviceLocation.latitude,
                        lon = deviceLocation.longitude,
                        date = formattedDate
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification for active location tracking service"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Tracking Active")
            .setContentText("Recording location every 30 seconds...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun getCurrentFormattedDate(): String {
        val timeZone = TimeZone.getTimeZone("Asia/Bangkok")
        val calendar = Calendar.getInstance(timeZone)
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
            this.timeZone = timeZone
        }
        return formatter.format(calendar.time)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTracking()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1001

        private val _isTracking = MutableStateFlow(false)
        val isTracking = _isTracking.asStateFlow()
    }
}
