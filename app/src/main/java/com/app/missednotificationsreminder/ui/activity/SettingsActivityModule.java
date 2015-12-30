package com.app.missednotificationsreminder.ui.activity;

import android.content.Context;

import com.app.missednotificationsreminder.ApplicationModule;
import com.app.missednotificationsreminder.binding.model.IntervalViewModel;
import com.app.missednotificationsreminder.binding.model.SchedulerViewModel;
import com.app.missednotificationsreminder.binding.model.SettingsViewModel;
import com.app.missednotificationsreminder.binding.model.SoundViewModel;
import com.app.missednotificationsreminder.di.qualifiers.ForActivity;
import com.app.missednotificationsreminder.ui.fragment.IntervalFragment;
import com.app.missednotificationsreminder.ui.fragment.SchedulerFragment;
import com.app.missednotificationsreminder.ui.fragment.SettingsFragment;
import com.app.missednotificationsreminder.ui.fragment.SoundFragment;
import com.app.missednotificationsreminder.ui.view.IntervalView;
import com.app.missednotificationsreminder.ui.view.SchedulerView;
import com.app.missednotificationsreminder.ui.view.SettingsView;
import com.app.missednotificationsreminder.ui.view.SoundView;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * The Dagger dependency injection module for the settings activity
 */
@Module(
        addsTo = ApplicationModule.class,
        injects = {
                IntervalFragment.class,
                IntervalViewModel.class,
                SettingsViewModel.class,
                SettingsFragment.class,
                SchedulerViewModel.class,
                SchedulerFragment.class,
                SoundViewModel.class,
                SoundFragment.class,
        }
)
public final class SettingsActivityModule {
    private final SettingsActivity mSettingsActivity;

    SettingsActivityModule(SettingsActivity SettingsActivity) {
        this.mSettingsActivity = SettingsActivity;
    }

    @Provides @Singleton IntervalView provideIntervalView() {
        return mSettingsActivity.getIntervalFragment();
    }

    @Provides @Singleton SchedulerView provideSchedulerView() {
        return mSettingsActivity.getSchedulerFragment();
    }
    
    @Provides @Singleton SoundView provideSoundView() {
        return mSettingsActivity.getSoundFragment();
    }

    @Provides @Singleton SettingsView provideSettingsView() {
        return mSettingsActivity.getSettingsFragment();
    }

    /**
     * Allow the activity context to be injected but require that it be annotated with
     * {@link ForActivity @ForActivity} to explicitly differentiate it from an application context.
     */
    @Provides @Singleton @ForActivity Context provideActivityContext() {
        return mSettingsActivity;
    }

}