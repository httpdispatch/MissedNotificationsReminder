package com.app.missednotificationsreminder;

import android.app.Application;
import android.content.Context;
import android.media.AudioManager;
import android.os.Vibrator;

import com.app.missednotificationsreminder.data.DataModule;
import com.app.missednotificationsreminder.di.qualifiers.ForApplication;
import com.app.missednotificationsreminder.service.RemindJob;
import com.app.missednotificationsreminder.service.ReminderNotificationListenerService;
import com.app.missednotificationsreminder.ui.UiModule;
import com.squareup.leakcanary.RefWatcher;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * The Dagger dependency injection module for the application
 */
@Module(
        injects = {
                CustomApplication.class,
                ReminderNotificationListenerService.class,
                RemindJob.class
        },
        includes = {
                UiModule.class,
                DataModule.class
        },
        library = true
)
public class ApplicationModule {
    private final CustomApplication mApp;

    public ApplicationModule(CustomApplication app) {
        mApp = app;
    }

    @Provides @Singleton Application provideApplication() {
        return mApp;
    }

    /**
     * Allow the application context to be injected but require that it be annotated with
     * {@link ForApplication @ForApplication} to explicitly differentiate it from an activity context.
     */
    @Provides @Singleton @ForApplication Context provideApplicationContext() {
        return mApp;
    }


    @Provides @Singleton RefWatcher provideRefWatcher() {
        return mApp.refWatcher;
    }

    @Provides @Singleton Vibrator provideVibrator() {
        return (Vibrator) mApp.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Provides @Singleton AudioManager provideAudioManager() {
        return (AudioManager)mApp.getSystemService(Context.AUDIO_SERVICE);
    }
}
