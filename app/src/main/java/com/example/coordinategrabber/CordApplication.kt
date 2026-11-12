package com.example.coordinategrabber

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.coordinategrabber.data.network.ApiEndpoints
import com.example.coordinategrabber.data.network.CloudFrontInterceptor
import com.example.coordinategrabber.data.settings.SettingsRepository
import com.example.coordinategrabber.data.settings.appDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

/**
 * Coil resolves ImageLoaderFactory from the application context, so the
 * CloudFront-aware OkHttp client must be configured here. Implementing the
 * factory on MainActivity has no effect on Coil's singleton image loader.
 */
class CordApplication : Application(), ImageLoaderFactory {

    private val settingsRepository by lazy { SettingsRepository(appDataStore) }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(
                CloudFrontInterceptor(
                    authTokenFlow = {
                        runBlocking { settingsRepository.authTokenFlow.first() }
                    },
                    serverUrlFlow = { ApiEndpoints.DEFAULT_BASE_URL }
                )
            )
            .build()

        return ImageLoader.Builder(applicationContext)
            .okHttpClient(okHttpClient)
            .build()
    }
}
