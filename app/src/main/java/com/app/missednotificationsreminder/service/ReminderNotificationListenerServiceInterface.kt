package com.app.missednotificationsreminder.service

import androidx.lifecycle.LifecycleOwner
import com.app.missednotificationsreminder.service.data.model.NotificationData

/**
 * The reminder service interface for basic notification handling functionality
 */
interface ReminderNotificationListenerServiceInterface : LifecycleOwner{
    /**
     * The method which should be called when any new notification is posted
     *
     * @param notificationData the posted notification data
     */
    suspend fun onNotificationPosted(notificationData: NotificationData)

    /**
     * The method which should be called when any notification is removed
     *
     * @param notificationData the removed notification data
     */
    suspend fun onNotificationRemoved(notificationData: NotificationData)

    /**
     * The method which should be called when a notification listener service is ready
     */
    fun onReady()

    /**
     * Get the currently showing notification data
     *
     * @return
     */
    val notificationsData: List<NotificationData>

    /**
     * Get the currently ignoring notification data
     * @return
     */
    val ignoredNotificationsData: List<NotificationData>

    val createDismissNotification: Boolean

    /**
     * Actualize the notification date
     */
    suspend fun actualizeNotificationData()
}