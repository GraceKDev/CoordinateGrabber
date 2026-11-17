package com.example.coordinategrabber.data.network

/**
 * Dedicated API endpoints configuration file for backend network calls.
 */
object ApiEndpoints {
    const val DEFAULT_BASE_URL = "http://192.168.1.181:5104"
    const val GOOGLE_WEB_CLIENT_ID = "163527060698-dfs9ug3l4nhpbefte4lpsrvc7539h9gu.apps.googleusercontent.com"

    // Endpoint Paths
    const val AUTH_GOOGLE = "/api/Auth/google"
    const val EXPORT_DATA = "/api/coordinates"
    const val IMAGE_ACCESS = "api/images/access"

    fun buildUrl(endpointPath: String, baseUrl: String = DEFAULT_BASE_URL): String {
        val cleanBase = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        val cleanEndpoint = if (endpointPath.startsWith("/")) endpointPath else "/$endpointPath"
        return "$cleanBase$cleanEndpoint"
    }
}
