package com.app.missednotificationsreminder;

import com.jakewharton.threetenabp.AndroidThreeTen;
import com.jakewharton.u2020.data.LumberYard;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Custom application implementation to provide additional functionality such as dependency injection, leak inspection
 *
 * @author Eugene Popovich
 */
public class CustomApplication extends CustomApplicationBase {

    @Inject LumberYard lumberYard;

    @Override public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);

        lumberYard.cleanUp();
        Timber.plant(lumberYard.tree());
    }

    @Override Object[] getModules() {
        return Modules.list(this);
    }
}
