package com.app.missednotificationsreminder.ui

import com.jakewharton.u2020.ui.debug.DebugViewModule
import dagger.Module

@Module(
        includes = [
            DebugViewModule::class
        ]
)
class DebugUiModule {
}