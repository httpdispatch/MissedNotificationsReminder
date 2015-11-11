package com.app.missednotificationsreminder.di;

import android.content.Context;

import dagger.ObjectGraph;

/**
 * Utility class which allows to obtain {@link ObjectGraph} related to {@link Context}
 */
public final class Injector {
    private static final String INJECTOR_SERVICE = Injector.class.getName();

    /**
     * Obtain the {@link ObjectGraph} related to the specified context.
     *
     * @param context
     * @return
     */
    @SuppressWarnings({"ResourceType", "WrongConstant"}) // Explicitly doing a custom service.
    public static ObjectGraph obtain(Context context) {
        return (ObjectGraph) context.getSystemService(INJECTOR_SERVICE);
    }

    public static boolean matchesService(String name) {
        return INJECTOR_SERVICE.equals(name);
    }

    private Injector() {
        throw new AssertionError("No instances.");
    }
}