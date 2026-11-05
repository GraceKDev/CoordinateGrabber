package com.example.coordinategrabber.location

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.example.coordinategrabber.data.model.Location
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class PhotoLocationResult(
    val location: Location,
    val isFromExif: Boolean
)

object PhotoLocationHelper {

    suspend fun processPhotoUri(
        context: Context,
        uri: Uri
    ): PhotoLocationResult? {
        var latitude: Double? = null
        var longitude: Double? = null
        var photoDate: String? = null

        // On Android 10+ (API 29+), pass uri through MediaStore.setRequireOriginal(uri)
        // alongside ACCESS_MEDIA_LOCATION permission to prevent Android from scrubbing EXIF GPS metadata.
        val originalUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                MediaStore.setRequireOriginal(uri)
            } catch (e: Exception) {
                uri
            }
        } else {
            uri
        }

        try {
            context.contentResolver.openInputStream(originalUri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val latLong = exif.latLong
                if (latLong != null && latLong.size >= 2) {
                    latitude = latLong[0]
                    longitude = latLong[1]
                }

                val exifDate = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

                if (!exifDate.isNullOrBlank()) {
                    photoDate = parseExifDate(exifDate)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If no EXIF GPS location metadata was found, FAIL immediately (do NOT use current device location)
        val latVal = latitude ?: return null
        val lonVal = longitude ?: return null

        // Copy photo to internal app storage so Coil can access the file permanently without URI permission denial
        val savedFileUri = copyUriToInternalStorage(context, originalUri) ?: originalUri
        val dateToUse = photoDate ?: getCurrentFormattedDate()

        return PhotoLocationResult(
            location = Location(
                routeId = null,
                lat = latVal,
                lon = lonVal,
                date = dateToUse,
                photoUri = savedFileUri.toString()
            ),
            isFromExif = true
        )
    }

    private fun copyUriToInternalStorage(context: Context, uri: Uri): Uri? {
        return try {
            val photosDir = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }
            val photoFile = File(photosDir, "photo_${System.currentTimeMillis()}.jpg")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(photoFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Uri.fromFile(photoFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseExifDate(exifDateStr: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
            val date = parser.parse(exifDateStr)
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("Asia/Bangkok")
            }
            if (date != null) formatter.format(date) else getCurrentFormattedDate()
        } catch (e: Exception) {
            getCurrentFormattedDate()
        }
    }

    private fun getCurrentFormattedDate(): String {
        val timeZone = TimeZone.getTimeZone("Asia/Bangkok")
        val calendar = Calendar.getInstance(timeZone)
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
            this.timeZone = timeZone
        }
        return formatter.format(calendar.time)
    }
}
