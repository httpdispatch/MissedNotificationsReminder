package com.app.missednotificationsreminder;

import android.app.Application;
import android.support.annotation.NonNull;

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
public class CustomApplication extends Application {

    private ObjectGraph mObjectGraph;

    @Inject ActivityHierarchyServer activityHierarchyServer;
    public RefWatcher refWatcher;


    @Override public void onCreate() {
        super.onCreate();
        // leak inspection
        refWatcher = LeakCanary.install(this);
        // Initialize dependency injection graph
        mObjectGraph = ObjectGraph.create(Modules.list(this));
        mObjectGraph.inject(this);
        // initialize logging
        Timber.plant(new Timber.DebugTree());

        registerActivityLifecycleCallbacks(activityHierarchyServer);
    }


    @Override
    public Object getSystemService(@NonNull String name) {
        if (Injector.matchesService(name)) {
            return mObjectGraph;
        }
        return super.getSystemService(name);
    }
}
