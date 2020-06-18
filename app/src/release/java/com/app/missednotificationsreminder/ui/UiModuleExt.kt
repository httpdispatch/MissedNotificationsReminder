package com.app.missednotificationsreminder.ui

import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module()
class UiModuleExt {
    @Provides
    @Singleton
    fun provideAppContainer(): AppContainer {
        return AppContainer.DEFAULT
    }

    @Provides
    @Singleton
    fun provideActivityHierarchyServer(): ActivityHierarchyServer {
        return ActivityHierarchyServer.NONE
    }
}