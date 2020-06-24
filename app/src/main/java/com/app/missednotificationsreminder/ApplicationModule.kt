package com.app.missednotificationsreminder

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.os.Vibrator
import com.app.missednotificationsreminder.data.DataModule
import com.app.missednotificationsreminder.di.ViewModelBuilder
import com.app.missednotificationsreminder.di.qualifiers.ForApplication
import com.app.missednotificationsreminder.service.RemindJob
import com.app.missednotificationsreminder.service.ReminderNotificationListenerService
import com.app.missednotificationsreminder.ui.UiModule
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * The Dagger dependency injection module for the application
 */
@Module(includes = [
    UiModule::class,
    DataModule::class,
    ReminderNotificationListenerService.Module::class,
    RemindJob.Module::class,
    ApplicationModuleExt::class,
    ViewModelBuilder::class])
class ApplicationModule {
    @Provides
    @Singleton
    fun provideApplication(@ForApplication context: Context): Application {
        return context as Application
    }

    /**
     * Allow the application context to be injected but require that it be annotated with
     * [@ForApplication][ForApplication] to explicitly differentiate it from an activity context.
     */
    @Provides
    @Singleton
    @ForApplication
    fun provideApplicationContext(context: Context): Context {
        return context.applicationContext
    }

    @Provides
    @Singleton
    fun provideVibrator(@ForApplication context: Context): Vibrator {
        return context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    @Provides
    @Singleton
    fun provideAudioManager(@ForApplication context: Context): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
}