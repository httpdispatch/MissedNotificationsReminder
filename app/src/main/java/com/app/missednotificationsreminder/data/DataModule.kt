package com.app.missednotificationsreminder.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import com.app.missednotificationsreminder.data.source.DefaultResourceDataSource
import com.app.missednotificationsreminder.data.source.ResourceDataSource
import com.app.missednotificationsreminder.settings.applicationselection.data.model.util.ApplicationIconHandler
import com.app.missednotificationsreminder.util.event.FlowEventBus
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.picasso.Picasso
import com.tfcporciuncula.flow.FlowSharedPreferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.ElementsIntoSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber
import javax.inject.Singleton

/**
 * The Dagger dependency injection module for the data layer
 */
@Module(includes = [DataModuleBinds::class])
class DataModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(app: Application): SharedPreferences {
        return app.getSharedPreferences("missingnotificationreminder", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideFlowSharedPreferences(prefs: SharedPreferences): FlowSharedPreferences {
        return FlowSharedPreferences(prefs)
    }

    @Provides
    @Singleton
    fun providePackageManager(app: Application): PackageManager {
        return app.packageManager
    }

    @Provides
    @Singleton
    fun providePicasso(app: Application, packageManager: PackageManager): Picasso {
        return Picasso.Builder(app)
                .addRequestHandler(ApplicationIconHandler(packageManager))
                .listener { _, uri, e -> Timber.e(e, "Failed to load image: %s", uri) }
                .build()
    }

    @Provides
    @Singleton
    fun provideEventBus(): FlowEventBus {
        return FlowEventBus()
    }

    @Provides
    @ElementsIntoSet
    fun provideJsonAdapters(): Set<JsonAdapter<Any>> {
        return emptySet()
    }

    @Provides
    @Singleton
    fun provideMoshi(adapters: Set<@JvmSuppressWildcards JsonAdapter<Any>>): Moshi {
        return Moshi.Builder()
                .apply {
                    adapters.forEach { add(it) }
                }
                .add(KotlinJsonAdapterFactory())
                .build()
    }
}

@Module
abstract class DataModuleBinds {

    @Singleton
    @Binds
    abstract fun bindResources(resources: DefaultResourceDataSource): ResourceDataSource
}