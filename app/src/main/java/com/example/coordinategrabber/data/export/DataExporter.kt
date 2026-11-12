package com.example.coordinategrabber.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.coordinategrabber.data.model.Location
import com.example.coordinategrabber.data.model.Route
import com.example.coordinategrabber.data.network.ApiEndpoints
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

data class CoordinateDTO(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("date") val date: String,
    @SerializedName("hasImage") val hasImage: Boolean,
    @SerializedName("imageUrl") val imageUrl: String? = null
)

data class RouteDTO(
    @SerializedName("name") val name: String,
    @SerializedName("date") val date: String,
    @SerializedName("points") val points: List<CoordinateDTO>
)

data class CoordinateExportDTO(
    @SerializedName("coordinates") val coordinates: List<CoordinateDTO>,
    @SerializedName("routes") val routes: List<RouteDTO>
)

data class ImageUploadDTO(
    @SerializedName("imageId") val imageId: String,
    @SerializedName("url") val url: String,
    @SerializedName("expiresAtUtc") val expiresAtUtc: String
)

data class CreateCoordinatesResponse(
    @SerializedName("routes") val routes: List<RouteDTO>,
    @SerializedName("coordinates") val coordinates: List<CoordinateDTO>,
    @SerializedName("imageUploads") val imageUploads: List<ImageUploadDTO>
)

data class AuthPayLoad(
    val googleToken: String
)

data class AuthResult(
    val idToken: String,
    val userEmail: String
)

class DataExporter(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val gson = Gson()

    suspend fun getIdTokenCredential(
        webClientId: String = ApiEndpoints.GOOGLE_WEB_CLIENT_ID
    ): GoogleIdTokenCredential? {
        if (webClientId.isBlank()) return null

        val credentialManager = CredentialManager.create(context)
        val rawNonce = UUID.randomUUID().toString()
        val bytes = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray())
        val hashedNonce = bytes.joinToString("") { "%02x".format(it) }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setNonce(hashedNonce)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            request = request,
            context = context
        )

        val credential = result.credential
        return GoogleIdTokenCredential.createFrom(credential.data)
    }

    suspend fun authenticate(
        serverUrl: String = ApiEndpoints.DEFAULT_BASE_URL,
        webClientId: String = ApiEndpoints.GOOGLE_WEB_CLIENT_ID
    ): Result<AuthResult> {
        return withContext(Dispatchers.IO) {
            try {
                var idToken = ""
                var userEmail = ""

                if (webClientId.isNotBlank()) {
                    try {
                        val googleIdTokenCredential = getIdTokenCredential(webClientId)
                        if (googleIdTokenCredential != null) {
                            idToken = googleIdTokenCredential.idToken
                            userEmail = googleIdTokenCredential.id
                        }
                    } catch (e: NoCredentialException) {
                        launchAddGoogleAccount(context)
                        return@withContext Result.failure(
                            Exception(
                                "No Google Account signed in on device. Opening Google Account Sign-In window...\n\n" +
                                        "Please sign into your Google account and tap 'Export Data' again once signed in."
                            )
                        )
                    } catch (e: GetCredentialException) {
                        return@withContext Result.failure(
                            Exception("Google Sign-In failed: ${e.message}")
                        )
                    }
                }

                val payload = AuthPayLoad(googleToken = idToken)
                val jsonPayload = gson.toJson(payload)
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonPayload.toRequestBody(mediaType)

                val targetUrl = ApiEndpoints.buildUrl(ApiEndpoints.AUTH_GOOGLE, serverUrl)

                val requestBuilder = Request.Builder()
                    .url(targetUrl)
                    .post(requestBody)

                if (idToken.isNotBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $idToken")
                }

                val response = client.newCall(requestBuilder.build()).execute()

                if (response.isSuccessful) {
                    Result.success(AuthResult(idToken = idToken, userEmail = userEmail))
                } else {
                    Result.failure(Exception("Authentication endpoint failed: HTTP ${response.code} ${response.message}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun exportData(
        locations: List<Location>,
        routes: List<Route>,
        existingToken: String = "",
        existingUserEmail: String = "",
        serverUrl: String = ApiEndpoints.DEFAULT_BASE_URL,
        webClientId: String = ApiEndpoints.GOOGLE_WEB_CLIENT_ID
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                var idToken = existingToken
                if (idToken.isBlank()) {
                    val authResult = authenticate(serverUrl, webClientId)
                    if (authResult.isFailure) {
                        return@withContext Result.failure(
                            authResult.exceptionOrNull() ?: Exception("Authentication failed")
                        )
                    }
                    val authData = authResult.getOrThrow()
                    idToken = authData.idToken
                }

                val dataExport = CoordinateExportDTO(
                    coordinates = locations.map {
                        CoordinateDTO(it.lat, it.lon, it.date, it.photoUri != null || !it.imageUrl.isNullOrBlank(), it.imageUrl)
                    },
                    routes = routes.map { route ->
                        RouteDTO(
                            name = route.name,
                            date = route.date,
                            points = route.points.map {
                                CoordinateDTO(it.lat, it.lon, it.date, it.photoUri != null || !it.imageUrl.isNullOrBlank(), it.imageUrl)
                            }
                        )
                    }
                )

                val jsonPayload = gson.toJson(dataExport)
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonPayload.toRequestBody(mediaType)

                val targetUrl = ApiEndpoints.buildUrl(ApiEndpoints.EXPORT_DATA, serverUrl)

                val requestBuilder = Request.Builder()
                    .url(targetUrl)
                    .post(requestBody)

                if (idToken.isNotBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $idToken")
                }

                val response = client.newCall(requestBuilder.build()).execute()
                val responseBodyString = response.body?.string() ?: ""
                Log.d("API_DEBUG", "CreateCoordinates Response JSON: $responseBodyString")

                if (response.isSuccessful) {
                    val createResponse = try {
                        gson.fromJson(responseBodyString, CreateCoordinatesResponse::class.java)
                    } catch (e: Exception) {
                        null
                    }

                    // Upload images to presigned S3 URLs if any were returned
                    if (createResponse != null && createResponse.imageUploads.isNotEmpty()) {
                        uploadImagesToPresignedUrls(
                            context = context,
                            imageUploads = createResponse.imageUploads,
                            locations = locations,
                            routes = routes,
                            idToken = idToken,
                            serverUrl = serverUrl
                        )
                    }

                    val imgUploadCount = createResponse?.imageUploads?.size ?: 0
                    val successMsg = if (imgUploadCount > 0) {
                        "Successfully uploaded ${dataExport.coordinates.size} coordinates, ${dataExport.routes.size} routes, and $imgUploadCount compressed images to S3!"
                    } else {
                        "Successfully uploaded ${dataExport.coordinates.size} coordinates & ${dataExport.routes.size} routes!"
                    }
                    Result.success(successMsg)
                } else {
                    Result.failure(Exception("CreateCoordinates endpoint returned HTTP ${response.code}: ${response.message}\n$responseBodyString"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun compressImageBytes(context: Context, uri: Uri, maxWidth: Int = 1920, maxHeight: Int = 1920, quality: Int = 80): ByteArray? {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            var inSampleSize = 1
            val height = options.outHeight
            val width = options.outWidth
            if (height > maxHeight || width > maxWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while ((halfHeight / inSampleSize) >= maxHeight && (halfWidth / inSampleSize) >= maxWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            } ?: return null

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            bitmap.recycle()
            return outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (ex: Exception) {
                ex.printStackTrace()
                return null
            }
        }
    }

    private suspend fun uploadImagesToPresignedUrls(
        context: Context,
        imageUploads: List<ImageUploadDTO>,
        locations: List<Location>,
        routes: List<Route>,
        idToken: String,
        serverUrl: String
    ): Boolean {
        if (imageUploads.isEmpty()) return true

        val photoLocations = (locations.filter { !it.photoUri.isNullOrBlank() } +
                routes.flatMap { r -> r.points.filter { !it.photoUri.isNullOrBlank() } })

        if (photoLocations.isEmpty()) return true

        val uploadedImageIds = mutableListOf<String>()

        for (i in imageUploads.indices) {
            val upload = imageUploads[i]
            val photoLoc = photoLocations.getOrNull(i) ?: continue
            val photoUriStr = photoLoc.photoUri ?: continue

            try {
                val uri = Uri.parse(photoUriStr)
                val bytes = compressImageBytes(context, uri)
                if (bytes != null && bytes.isNotEmpty()) {
                    val mediaType = "image/jpeg".toMediaType()
                    val body = bytes.toRequestBody(mediaType)
                    val putRequest = Request.Builder()
                        .url(upload.url)
                        .put(body)
                        .build()

                    var putResponse = client.newCall(putRequest).execute()
                    Log.d("API_DEBUG", "S3 PUT Upload for imageId ${upload.imageId}: code=${putResponse.code}")

                    if (putResponse.code == 301 || putResponse.code == 307) {
                        val redirectUrl = putResponse.header("Location")
                        if (redirectUrl != null) {
                            val redirectedRequest = Request.Builder()
                                .url(redirectUrl)
                                .put(bytes.toRequestBody(mediaType))
                                .build()
                            putResponse = client.newCall(redirectedRequest).execute()
                            Log.d("API_DEBUG", "S3 Redirected PUT Upload for imageId ${upload.imageId}: code=${putResponse.code}")
                        }
                    }

                    if (putResponse.isSuccessful) {
                        uploadedImageIds.add(upload.imageId)
                    } else {
                        Log.e("API_DEBUG", "S3 PUT Upload failed: ${putResponse.body?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("API_DEBUG", "Exception uploading image to S3", e)
            }
        }

        try {
            if (uploadedImageIds.isNotEmpty()) {
                val completeUrl = ApiEndpoints.buildUrl("/api/Coordinates/images/uploaded", serverUrl)
                val completePayload = gson.toJson(mapOf("imageIds" to uploadedImageIds))
                val body = completePayload.toRequestBody("application/json; charset=utf-8".toMediaType())
                val completeReq = Request.Builder()
                    .url(completeUrl)
                    .post(body)
                    .apply { if (idToken.isNotBlank()) addHeader("Authorization", "Bearer $idToken") }
                    .build()

                val completeResp = client.newCall(completeReq).execute()
                Log.d("API_DEBUG", "Images uploaded confirmation callback response: code=${completeResp.code}")
                return completeResp.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("API_DEBUG", "Exception in images uploaded callback", e)
        }

        return true
    }

    suspend fun importData(
        existingToken: String = "",
        serverUrl: String = ApiEndpoints.DEFAULT_BASE_URL,
        webClientId: String = ApiEndpoints.GOOGLE_WEB_CLIENT_ID
    ): Result<CoordinateExportDTO> {
        return withContext(Dispatchers.IO) {
            try {
                var idToken = existingToken
                if (idToken.isBlank()) {
                    val authResult = authenticate(serverUrl, webClientId)
                    if (authResult.isFailure) {
                        return@withContext Result.failure(
                            authResult.exceptionOrNull() ?: Exception("Authentication failed")
                        )
                    }
                    val authData = authResult.getOrThrow()
                    idToken = authData.idToken
                }

                val targetUrl = ApiEndpoints.buildUrl(ApiEndpoints.EXPORT_DATA, serverUrl)
                val requestBuilder = Request.Builder()
                    .url(targetUrl)
                    .get()

                if (idToken.isNotBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $idToken")
                }

                val response = client.newCall(requestBuilder.build()).execute()

                if (response.isSuccessful) {
                    val responseText = response.body?.string() ?: ""
                    val data = gson.fromJson(responseText, CoordinateExportDTO::class.java)
                    Result.success(data ?: CoordinateExportDTO(emptyList(), emptyList()))
                } else {
                    Result.failure(Exception("Import endpoint returned HTTP ${response.code}: ${response.message}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteServerData(
        existingToken: String = "",
        serverUrl: String = ApiEndpoints.DEFAULT_BASE_URL,
        webClientId: String = ApiEndpoints.GOOGLE_WEB_CLIENT_ID
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                var idToken = existingToken
                if (idToken.isBlank()) {
                    val authResult = authenticate(serverUrl, webClientId)
                    if (authResult.isFailure) {
                        return@withContext Result.failure(
                            authResult.exceptionOrNull() ?: Exception("Authentication failed")
                        )
                    }
                    val authData = authResult.getOrThrow()
                    idToken = authData.idToken
                }

                val targetUrl = ApiEndpoints.buildUrl(ApiEndpoints.EXPORT_DATA, serverUrl)
                val requestBuilder = Request.Builder()
                    .url(targetUrl)
                    .delete()

                if (idToken.isNotBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $idToken")
                }

                val response = client.newCall(requestBuilder.build()).execute()

                if (response.isSuccessful) {
                    val responseText = response.body?.string() ?: "Server data deleted"
                    Result.success("Successfully deleted server data!\nResponse: $responseText")
                } else {
                    Result.failure(Exception("Delete server data endpoint returned HTTP ${response.code}: ${response.message}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun launchAddGoogleAccount(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
