package com.example.coordinategrabber.data.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

data class CloudFrontCookies(
    @SerializedName("policy") val policy: String,
    @SerializedName("signature") val signature: String,
    @SerializedName("keyPairId") val keyPairId: String,
    @SerializedName("expiresAt") val expiresAt: Long
)

object CloudFrontManager {
    private var cachedCookies: CloudFrontCookies? = null

    suspend fun fetchCookies(authToken: String, serverUrl: String): CloudFrontCookies? {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()

                val targetUrl = ApiEndpoints.buildUrl("/api/images/access", serverUrl)
                val request = Request.Builder()
                    .url(targetUrl)
                    .get()
                    .apply {
                        if (authToken.isNotBlank()) {
                            addHeader("Authorization", "Bearer $authToken")
                        }
                    }
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = response.body?.string() ?: return@withContext null

                    val cookies = Gson().fromJson(json, CloudFrontCookies::class.java)
                    Log.d("IMAGE_SOURCE_DEBUG", "${cookies}")
                    cachedCookies = cookies
                    Log.d("IMAGE_SOURCE_DEBUG", "Successfully fetched new CloudFront cookies from /api/images/access")
                    return@withContext cookies
                } else {
                    Log.e("IMAGE_SOURCE_DEBUG", "Failed to fetch CloudFront cookies: HTTP ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("IMAGE_SOURCE_DEBUG", "Exception fetching CloudFront cookies", e)
            }
            cachedCookies
        }
    }

    fun getCachedCookies(): CloudFrontCookies? {
        val cookies = cachedCookies
        if (cookies != null && cookies.expiresAt * 1000L > System.currentTimeMillis() + 60000L) {
            return cookies
        }
        return null
    }
}

class CloudFrontInterceptor(
    private val authTokenFlow: () -> String,
    private val serverUrlFlow: () -> String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val urlString = request.url.toString()

        if (urlString.contains("cloudfront.net")) {
            Log.d("IMAGE_SOURCE_DEBUG", "=> Request intercepted for CLOUDFRONT S3 URL: $urlString")
            var cookies = CloudFrontManager.getCachedCookies()
            if (cookies == null) {
                Log.d("IMAGE_SOURCE_DEBUG", "=> CloudFront cookies expired or missing. Fetching new cookies...")
                val token = authTokenFlow()
                val server = serverUrlFlow()
                runBlocking(Dispatchers.IO) {
                    cookies = CloudFrontManager.fetchCookies(token, server)
                }
            }

            if (cookies != null) {
                Log.d("IMAGE_SOURCE_DEBUG", "=> Attached CloudFront signed cookies to request for: $urlString")
                val cookieHeader = "CloudFront-Policy=${cookies.policy}; " +
                    "CloudFront-Signature=${cookies.signature}; " +
                    "CloudFront-Key-Pair-Id=${cookies.keyPairId}"
                val modifiedRequest = request.newBuilder()
                    .header("Cookie", cookieHeader)
                    .build()
                return chain.proceed(modifiedRequest)
            } else {
                Log.e("IMAGE_SOURCE_DEBUG", "=> ERROR: Could not obtain CloudFront signed cookies for: $urlString")
            }
        } else {
            Log.d("IMAGE_SOURCE_DEBUG", "=> Request is LOCAL or non-CloudFront URL: $urlString")
        }
        return chain.proceed(request)
    }
}
