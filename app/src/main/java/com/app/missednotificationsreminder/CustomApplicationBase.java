package com.app.missednotificationsreminder;

import android.app.Application;
import androidx.annotation.NonNull;
import android.util.Log;

import com.app.missednotificationsreminder.di.Injector;
import com.app.missednotificationsreminder.service.ReminderServiceJobCreator;
import com.app.missednotificationsreminder.ui.ActivityHierarchyServer;
import com.evernote.android.job.JobManager;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.jakewharton.u2020.data.LumberYard;

import javax.inject.Inject;

import dagger.ObjectGraph;
import timber.log.Timber;

/**
 * Custom application implementation to provide additional functionality such as dependency injection, leak inspection
 *
 * @author Eugene Popovich
 */
public abstract class CustomApplicationBase extends Application {

    private ObjectGraph mObjectGraph;

    @Inject ActivityHierarchyServer activityHierarchyServer;
    @Inject LumberYard lumberYard;

    public CustomApplicationBase() {
        // Initialize dependency injection graph
        mObjectGraph = ObjectGraph.create(getModules());
    }

    @Override public void onCreate() {
        super.onCreate();
        // leak inspection
        mObjectGraph.inject(this);
        // initialize logging
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashReportingTree());
        }

        lumberYard.cleanUp();
        Timber.plant(lumberYard.tree());

        AndroidThreeTen.init(this);

        registerActivityLifecycleCallbacks(activityHierarchyServer);
        JobManager.create(this).addJobCreator(new ReminderServiceJobCreator());
    }

    abstract Object[] getModules();

    @Override
    public Object getSystemService(@NonNull String name) {
        if (Injector.matchesService(name)) {
            return mObjectGraph;
        }
        return super.getSystemService(name);
    }

    private static class CrashReportingTree extends Timber.DebugTree {
        @Override protected void log(int priority, String tag, String message, Throwable t) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                // skip debug and verbose messages
                return;
            }
            super.log(priority, tag, message, t);
        }
    }
}