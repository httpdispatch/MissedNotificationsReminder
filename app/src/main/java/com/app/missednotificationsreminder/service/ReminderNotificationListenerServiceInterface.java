package com.app.missednotificationsreminder.service;

import java.util.Collection;

/**
 * The reminder service interface for basic notification handling functionality
 *
 * @author Eugene Popovich
 */
public interface ReminderNotificationListenerServiceInterface {

    /**
     * The method which should be called when any new notification is posted
     */
    void onNotificationPosted();

    /**
     * The method which should be called when any notification is removed
     */
    void onNotificationRemoved();

    /**
     * Check whether the at least one notification for specified packages is present in the status bar
     *
     * @param packages      the collection of packages to check
     * @param ignoreOngoing whether the ongoing notifications should be ignored
     * @return true if notification for at least one package is found, false otherwise
     */
    boolean checkNotificationForAtLeastOnePackageExists(Collection<String> packages, boolean ignoreOngoing);

    /**
     * The method which should be called when a notification listener service is ready
     */
    void onReady();
}
