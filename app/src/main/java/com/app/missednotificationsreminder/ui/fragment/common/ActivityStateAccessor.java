package com.app.missednotificationsreminder.ui.fragment.common;

/**
 * The interface for the activity state accessor to provide various the activity lifecycle
 * related information
 */
public interface ActivityStateAccessor {
    /**
     * Is the activity alive
     *
     * @return
     */
    public boolean isActivityAlive();

    /**
     * Is the activity active and resumed
     *
     * @return
     */
    public boolean isActivityResumed();
}