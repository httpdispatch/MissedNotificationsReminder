package com.app.missednotificationsreminder;

import android.app.Application;
import android.support.annotation.NonNull;
import android.util.Log;

import com.app.missednotificationsreminder.di.Injector;
import com.app.missednotificationsreminder.ui.ActivityHierarchyServer;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

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
    public RefWatcher refWatcher;


    @Override public void onCreate() {
        super.onCreate();
        // leak inspection
        refWatcher = LeakCanary.install(this);
        // Initialize dependency injection graph
        mObjectGraph = ObjectGraph.create(getModules());
        mObjectGraph.inject(this);
        // initialize logging
        if(BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashReportingTree());
        }

        registerActivityLifecycleCallbacks(activityHierarchyServer);
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