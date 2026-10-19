package com.nannyapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import dagger.hilt.EntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class NannyApplication : Application(), Configuration.Provider, ImageLoaderFactory {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkManagerFactoryEntryPoint {
        fun workerFactory(): HiltWorkerFactory
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
            .components { add(SvgDecoder.Factory()) }
            .crossfade(true)
            .build()
}
