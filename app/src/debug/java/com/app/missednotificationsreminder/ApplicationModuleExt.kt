package com.app.missednotificationsreminder

import com.app.missednotificationsreminder.di.qualifiers.IsInstrumentationTest
import com.app.missednotificationsreminder.ui.DebugUiModule
import com.jakewharton.u2020.data.DebugDataModule
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(includes = [DebugUiModule::class, DebugDataModule::class])
class ApplicationModuleExt {
    @Provides
    @Singleton
    @IsInstrumentationTest
    fun provideIsInstrumentationTest(): Boolean {
        return instrumentationTest
    }

    companion object {
        // Low-tech flag to force certain debug build behaviors when running in an instrumentation test.
        // This value is used in the creation of singletons so it must be set before the graph is created.
        @JvmField
        var instrumentationTest = false
    }
}