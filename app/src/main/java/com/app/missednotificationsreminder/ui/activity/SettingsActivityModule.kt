package com.app.missednotificationsreminder.ui.activity

import android.content.Context
import com.app.missednotificationsreminder.di.qualifiers.ActivityScope
import com.app.missednotificationsreminder.di.qualifiers.ForActivity
import com.app.missednotificationsreminder.ui.fragment.*
import com.app.missednotificationsreminder.ui.view.*
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector

/**
 * The Dagger dependency injection module for the settings activity
 */
@Module(
        includes = [
        ]
)
abstract class SettingsActivityModule {
    @ActivityScope
    @ContributesAndroidInjector(
            modules = [
                SettingsActivityModuleExt::class,
                SettingsFragment.Module::class,
                ReminderFragment.Module::class,
                SchedulerFragment.Module::class,
                SoundFragment.Module::class,
                VibrationFragment.Module::class
            ]
    )
    abstract fun contributeSettingsActivity(): SettingsActivity
}

@Module
class SettingsActivityModuleExt {

    @Provides
    fun provideReminderView(activity: SettingsActivity): ReminderView {
        return activity.reminderFragment
    }

    @Provides
    fun provideSchedulerView(activity: SettingsActivity): SchedulerView {
        return activity.schedulerFragment
    }

    @Provides
    fun provideSoundView(activity: SettingsActivity): SoundView {
        return activity.soundFragment
    }

    @Provides
    fun provideVibrationView(activity: SettingsActivity): VibrationView {
        return activity.vibrationFragment
    }

    @Provides
    fun provideSettingsView(activity: SettingsActivity): SettingsView {
        return activity.settingsFragment
    }

    /**
     * Allow the activity context to be injected but require that it be annotated with
     * [@ForActivity][ForActivity] to explicitly differentiate it from an application context.
     */
    @Provides
    @ForActivity
    fun provideActivityContext(activity: SettingsActivity): Context {
        return activity
    }
}