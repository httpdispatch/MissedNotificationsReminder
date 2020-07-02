package com.app.missednotificationsreminder.service;

import com.app.missednotificationsreminder.service.data.model.NotificationData;

import java.util.List;

/**
 * The reminder service interface for basic notification handling functionality
 *
 * @author Eugene Popovich
 */
public interface ReminderNotificationListenerServiceInterface {

    /**
     * The method which should be called when any new notification is posted
     *
     * @param notificationData the posted notification data
     */
    void onNotificationPosted(NotificationData notificationData);

    /**
     * The method which should be called when any notification is removed
     *
     * @param notificationData the removed notification data
     */
    void onNotificationRemoved(NotificationData notificationData);

    /**
     * The method which should be called when a notification listener service is ready
     */
    void onReady();

    /**
     * Get the currently showing notification data
     *
     * @return
     */
    List<NotificationData> getNotificationsData();

    /**
     * Get the currently ignoring notification data
     * @return
     */
    List<NotificationData> getIgnoredNotificationsData();

    /**
     * Actualize the notification date
     */
    void actualizeNotificationData();
}
