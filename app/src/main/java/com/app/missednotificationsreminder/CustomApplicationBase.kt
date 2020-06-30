package com.app.missednotificationsreminder

import android.content.Context
import android.util.Log
import androidx.multidex.MultiDex
import com.app.missednotificationsreminder.service.ReminderServiceJobCreator
import com.app.missednotificationsreminder.ui.ActivityHierarchyServer
import com.evernote.android.job.JobManager
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
abstract class CustomApplicationBase : DaggerApplication() {
    @Inject
    lateinit var activityHierarchyServer: ActivityHierarchyServer

    @Inject
    lateinit var lumberYard: LumberYard

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        // initialize logging
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
        lumberYard.cleanUp()
        Timber.plant(lumberYard.tree())
        AndroidThreeTen.init(this)
        registerActivityLifecycleCallbacks(activityHierarchyServer)
        JobManager.create(this).addJobCreator(ReminderServiceJobCreator())
    }

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