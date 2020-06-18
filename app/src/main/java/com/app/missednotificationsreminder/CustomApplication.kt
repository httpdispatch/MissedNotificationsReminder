package com.app.missednotificationsreminder

import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication

/**
 * Custom application implementation to provide additional functionality such as dependency injection, leak inspection
 *
 * @author Eugene Popovich
 */
class CustomApplication : CustomApplicationBase() {
    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerApplicationComponent.factory().create(applicationContext)
    }
}