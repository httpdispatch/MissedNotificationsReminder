package com.app.missednotificationsreminder.ui.debug;

import android.app.Activity;
import android.view.ViewGroup;

import com.app.missednotificationsreminder.ui.AppContainer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class DebugAppContainer implements AppContainer {
    @Inject
    public DebugAppContainer() {
    }

    @Override
    public ViewGroup bind(final Activity activity) {
        return (ViewGroup) activity.findViewById(android.R.id.content);
    }
}
