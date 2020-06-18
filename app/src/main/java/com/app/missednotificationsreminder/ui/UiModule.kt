package com.app.missednotificationsreminder.ui

import com.app.missednotificationsreminder.ui.activity.ApplicationsSelectionActivityModule
import com.app.missednotificationsreminder.ui.activity.SettingsActivityModule
import dagger.Module

/**
 * The Dagger dependency injection module for the UI layer
 */
@Module(includes = [SettingsActivityModule::class, ApplicationsSelectionActivityModule::class, UiModuleExt::class])
class UiModule