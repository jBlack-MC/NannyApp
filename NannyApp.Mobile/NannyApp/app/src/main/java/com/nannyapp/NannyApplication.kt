package com.nannyapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.nannyapp.data.preferences.SessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

@HiltAndroidApp
class NannyApplication : Application(), Configuration.Provider, ImageLoaderFactory {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkManagerFactoryEntryPoint {
        fun workerFactory(): HiltWorkerFactory
        fun sessionManager(): SessionManager
    }

    override val workManagerConfiguration: Configuration
        get() {
            val factory = EntryPointAccessors
                .fromApplication(this, WorkManagerFactoryEntryPoint::class.java)
                .workerFactory()
            return Configuration.Builder()
                .setWorkerFactory(factory)
                .build()
        }

    /** Every AsyncImage uses this loader: profile photos from the database may be JPEG, PNG or SVG. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            // Media URLs point at the protected API endpoint. Coil uses its own
            // OkHttp client, so Retrofit's bearer interceptor cannot authenticate
            // these requests automatically.
            .okHttpClient {
                val sessionManager = EntryPointAccessors
                    .fromApplication(this, WorkManagerFactoryEntryPoint::class.java)
                    .sessionManager()
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val token = runBlocking { sessionManager.currentToken() }
                        chain.proceed(chain.request().newBuilder().apply {
                            if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token")
                        }.build())
                    }
                    .build()
            }
            .components { add(SvgDecoder.Factory()) }
            .crossfade(true)
            .build()
}
