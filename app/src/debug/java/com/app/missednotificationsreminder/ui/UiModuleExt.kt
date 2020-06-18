package com.app.missednotificationsreminder.ui

import com.app.missednotificationsreminder.di.qualifiers.IsInstrumentationTest
import com.app.missednotificationsreminder.ui.debug.DebugAppContainer
import com.jakewharton.u2020.ui.debug.SocketActivityHierarchyServer
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module()
class UiModuleExt {
    @Provides
    @Singleton
    fun provideAppContainer(debugAppContainer: DebugAppContainer,
                            @IsInstrumentationTest isInstrumentationTest: Boolean): AppContainer {
        // Do not add the debug controls for when we are running inside of an instrumentation test.
        return if (isInstrumentationTest) AppContainer.DEFAULT else debugAppContainer
    }

    @Provides
    @Singleton
    fun provideActivityHierarchyServer(): ActivityHierarchyServer {
        return SocketActivityHierarchyServer()
    }
}