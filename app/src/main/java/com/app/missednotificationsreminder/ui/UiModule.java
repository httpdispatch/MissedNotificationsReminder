package com.app.missednotificationsreminder.ui;

import com.app.missednotificationsreminder.ui.activity.ApplicationsSelectionActivity;
import com.app.missednotificationsreminder.ui.activity.SettingsActivity;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * The Dagger dependency injection module for the UI layer
 */
@Module(
        injects = {
                SettingsActivity.class,
                ApplicationsSelectionActivity.class,
        },
        complete = false,
        library = true
)
public final class UiModule {
    @Provides
    @Singleton
    AppContainer provideAppContainer() {
        return AppContainer.DEFAULT;
    }

    @Provides
    @Singleton
    ActivityHierarchyServer provideActivityHierarchyServer() {
        return ActivityHierarchyServer.NONE;
    }
}
