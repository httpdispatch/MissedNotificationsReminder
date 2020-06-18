package com.jakewharton.u2020.ui.debug

import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * The Dagger dependency injection module for the settings activity
 */
@Module
abstract class DebugViewModule {
    @ContributesAndroidInjector
    abstract fun contribute(): DebugView
}