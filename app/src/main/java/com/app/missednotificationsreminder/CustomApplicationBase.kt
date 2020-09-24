package com.app.missednotificationsreminder

import android.content.Context
import android.util.Log
import androidx.multidex.MultiDex
import androidx.work.Configuration
import com.app.missednotificationsreminder.ui.ActivityHierarchyServer
import com.app.missednotificationsreminder.ui.activity.common.CommonActivityLifecycleCallback
import com.jakewharton.threetenabp.AndroidThreeTen
import com.jakewharton.u2020.data.LumberYard
import dagger.android.support.DaggerApplication
import timber.log.Timber
import timber.log.Timber.DebugTree
import javax.inject.Inject

/**
 * Custom application implementation to provide additional functionality such as dependency injection, leak inspection
 *
 * @author Eugene Popovich
 */
abstract class CustomApplicationBase : DaggerApplication(), Configuration.Provider {
    @Inject
    lateinit var activityHierarchyServer: ActivityHierarchyServer

    @Inject
    lateinit var lumberYard: LumberYard

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        AndroidThreeTen.init(this)
        super.onCreate()
        // initialize logging
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
        lumberYard.cleanUp()
        Timber.plant(lumberYard.tree())
        registerActivityLifecycleCallbacks(activityHierarchyServer)
        registerActivityLifecycleCallbacks(CommonActivityLifecycleCallback())
    }

    override fun getWorkManagerConfiguration() =
            Configuration.Builder()
                    .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.VERBOSE else Log.WARN)
                    .build()

    private class CrashReportingTree : DebugTree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                // skip debug and verbose messages
                return
            }
            super.log(priority, tag, message, t)
        }
    }
}