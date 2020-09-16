package com.app.missednotificationsreminder.ui

import com.app.missednotificationsreminder.di.ViewModelBuilder
import dagger.Module

/**
 * The Dagger dependency injection module for the UI layer
 */
@Module(includes = [ViewModelBuilder::class, UiModuleExt::class])
class UiModule