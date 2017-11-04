package com.app.missednotificationsreminder.ui.activity;

import android.content.Context;

import com.app.missednotificationsreminder.ApplicationModule;
import com.app.missednotificationsreminder.binding.model.ReminderViewModel;
import com.app.missednotificationsreminder.binding.model.SchedulerViewModel;
import com.app.missednotificationsreminder.binding.model.SettingsViewModel;
import com.app.missednotificationsreminder.binding.model.SoundViewModel;
import com.app.missednotificationsreminder.di.qualifiers.ForActivity;
import com.app.missednotificationsreminder.ui.fragment.ReminderFragment;
import com.app.missednotificationsreminder.ui.fragment.SchedulerFragment;
import com.app.missednotificationsreminder.ui.fragment.SettingsFragment;
import com.app.missednotificationsreminder.ui.fragment.SoundFragment;
import com.app.missednotificationsreminder.ui.fragment.VibrationFragment;
import com.app.missednotificationsreminder.ui.view.ReminderView;
import com.app.missednotificationsreminder.ui.view.SchedulerView;
import com.app.missednotificationsreminder.ui.view.SettingsView;
import com.app.missednotificationsreminder.ui.view.SoundView;
import com.app.missednotificationsreminder.ui.view.VibrationView;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * The Dagger dependency injection module for the settings activity
 */
@Module(
        addsTo = ApplicationModule.class,
        injects = {
                ReminderFragment.class,
                ReminderViewModel.class,
                SettingsViewModel.class,
                SettingsFragment.class,
                SchedulerViewModel.class,
                SchedulerFragment.class,
                SoundViewModel.class,
                SoundFragment.class,
                VibrationFragment.class,
        }
)
public final class SettingsActivityModule {
    private final SettingsActivity mSettingsActivity;

    SettingsActivityModule(SettingsActivity SettingsActivity) {
        this.mSettingsActivity = SettingsActivity;
    }

    @Provides @Singleton
    ReminderView provideIntervalView() {
        return mSettingsActivity.getIntervalFragment();
    }

    @Provides @Singleton SchedulerView provideSchedulerView() {
        return mSettingsActivity.getSchedulerFragment();
    }

    @Provides @Singleton SoundView provideSoundView() {
        return mSettingsActivity.getSoundFragment();
    }

    @Provides @Singleton VibrationView provideVibrationView() {
        return mSettingsActivity.getVibrationFragment();
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