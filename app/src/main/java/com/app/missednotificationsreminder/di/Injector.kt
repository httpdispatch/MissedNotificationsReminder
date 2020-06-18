package com.app.missednotificationsreminder.di

import android.content.Context
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication

/**
 * Utility class which allows to obtain [AndroidInjector] related to [Context]
 */
class Injector private constructor() {
    companion object {
        /**
         * Obtain the [AndroidInjector] related to the specified context.
         *
         * @param context
         * @return
         */
        // Explicitly doing a custom service.
        @JvmStatic
        fun obtain(context: Context): AndroidInjector<Any>? {
            val app = context.applicationContext;
            return if (app is DaggerApplication) app.androidInjector() else null
        }
    }

    init {
        throw AssertionError("No instances.")
    }
}