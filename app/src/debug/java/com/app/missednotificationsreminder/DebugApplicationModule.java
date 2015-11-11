package com.app.missednotificationsreminder;

import com.app.missednotificationsreminder.data.DebugDataModule;
import com.app.missednotificationsreminder.di.qualifiers.IsInstrumentationTest;
import com.app.missednotificationsreminder.ui.DebugUiModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        addsTo = ApplicationModule.class,
        includes = {
                DebugUiModule.class,
                DebugDataModule.class
        },
        overrides = true
)
public final class DebugApplicationModule {
    // Low-tech flag to force certain debug build behaviors when running in an instrumentation test.
    // This value is used in the creation of singletons so it must be set before the graph is created.
    static boolean instrumentationTest = false;

    @Provides
    @Singleton
    @IsInstrumentationTest
    boolean provideIsInstrumentationTest() {
        return instrumentationTest;
    }
}
