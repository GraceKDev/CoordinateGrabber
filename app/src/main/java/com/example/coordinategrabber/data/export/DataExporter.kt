package com.example.coordinategrabber.data.export

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

data class CoordinateDTO(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("date") val date: String
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
                        CoordinateDTO(it.lat, it.lon, it.date)
                    },
                    routes = routes.map { route ->
                        RouteDTO(
                            name = route.name,
                            date = route.date,
                            points = route.points.map {
                                CoordinateDTO(it.lat, it.lon, it.date)
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

                if (response.isSuccessful) {
                    Result.success("Successfully uploaded ${dataExport.coordinates.size} coordinates and ${dataExport.routes.size} routes!")
                } else {
                    Result.failure(Exception("CreateCoordinates endpoint returned HTTP ${response.code}: ${response.message}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
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
