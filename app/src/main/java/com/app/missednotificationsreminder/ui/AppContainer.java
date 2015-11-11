package com.app.missednotificationsreminder.ui;

import android.app.Activity;
import android.view.ViewGroup;

/**
 * An indirection which allows controlling the root container used for each activity.
 */
public interface AppContainer {
    /**
     * The root {@link ViewGroup} into which the activity should place its contents.
     */
    ViewGroup bind(Activity activity);

    /**
     * An {@link AppContainer} which returns the normal activity content view.
     */
    AppContainer DEFAULT = activity -> (ViewGroup) activity.findViewById(android.R.id.content);
}
