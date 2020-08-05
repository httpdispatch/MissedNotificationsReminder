package com.app.missednotificationsreminder.service

import android.annotation.TargetApi
import android.os.Build
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.app.missednotificationsreminder.service.data.model.NotificationData
import timber.log.Timber
import java.util.*

/**
 * The service to monitor all status bar notifications.
 */
abstract class AbstractReminderNotificationListenerService : NotificationListenerService(), ReminderNotificationListenerServiceInterface {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // such as onListenerConnected is not called on android prior L call onReady method explicitly
            onReady()
        }
    }

    override fun actualizeNotificationData() {
        val activeNotifications: Array<StatusBarNotification> =
                try {
                    activeNotifications
                } catch (e: Exception) {
                    Timber.e(e)
                    emptyArray()
                }
        val snapshotNotifications = mutableSetOf<NotificationData>().apply {
            addAll(notificationsData)
        }
        val addedNotifications: MutableList<StatusBarNotification> = ArrayList()
        for (sbn in activeNotifications) {
            val notificationData: NotificationData? = findNotificationData(sbn, snapshotNotifications)
            if (notificationData != null) {
                snapshotNotifications.remove(notificationData)
            } else {
                Timber.d("actualizeNotificationData() found new %s", sbn)
                addedNotifications.add(sbn)
            }
        }
        for (notificationData in snapshotNotifications) {
            Timber.w("actualizeNotificationData() found already removed %s", notificationData)
            onNotificationRemoved(notificationData)
        }
        for (sbn in addedNotifications) {
            onNotificationPosted(sbn)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn == null) {
            // fix weird NPE on some devices
            return
        }
        Timber.d("onNotificationPosted: for package %1\$s, key %2\$s, when %3\$s", sbn.packageName, notificationKey(sbn), sbn.notification.`when`)
        var notificationData: NotificationData? = findNotificationData(sbn)
        if (notificationData == null) {
            notificationData = ExtendedNotificationData(sbn)
        }
        onNotificationPosted(notificationData)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn == null) {
            // fix weird NPE on some devices
            return
        }
        Timber.d("onNotificationRemoved: for package %1\$s, key %2\$s, when %3\$s", sbn.packageName, notificationKey(sbn), sbn.notification.`when`)
        val notificationData: NotificationData? = findNotificationData(sbn)
        if (notificationData == null) {
            Timber.w("onNotificationRemoved: can't find internal notification data for the status bar notification %s",
                    notificationKey(sbn))
        } else {
            // stop alarm and check whether it should be launched again
            onNotificationRemoved(notificationData)
        }
    }

    private fun findNotificationData(sbn: StatusBarNotification,
                                     snapshotNotifications: Collection<NotificationData> = notificationsData): ExtendedNotificationData? {
        var result: ExtendedNotificationData? = null
        val key = notificationKey(sbn)
        val `when` = sbn.notification.`when`
        for (notificationData in snapshotNotifications) {
            val extendedNotificationData = notificationData as ExtendedNotificationData
            if (extendedNotificationData.id == key && extendedNotificationData.`when` == `when`) {
                result = extendedNotificationData
                break
            }
        }
        return result
    }

    private fun notificationKey(notification: StatusBarNotification): String {
        // This method re-implements StatusBarNotification.getKey() method, which is available
        // starting with API level 20, but we want to support API level 18+. The method
        // StatusBarNotification.getUserId() that we use below is deprecated, but the replacement
        // StatusBarNotification.getUser() method is only available starting with API level 21.
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            notification.userId.toString()
        else
            notification.user.toString() + "|" + notification.packageName +
                    "|" + notification.id + "|" + notification.tag
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onListenerConnected() {
        Timber.d("onListenerConnected")
        super.onListenerConnected()
        onReady()
    }

    inner class ExtendedNotificationData(id: String?, packageName: String?, foundAtTime: Long, flags: Int, // The following part is not present in the original getKey(),
            // but needed since some IM apps, e.g. Hangouts, re-post the
            // same notification when a new message is received. As a
            // result, ignoring a notification for the first IM message by
            // deleting the dismiss notificaiton, will cause all further
            // messages to be ignored too.
            // TODO: Make this configurable in the settings as not all users
            // make like this behavior and some may choose later messages to
            // be ignored too
                                         val `when`: Long) : NotificationData(id!!, packageName!!, foundAtTime, flags) {

        constructor(sbn: StatusBarNotification) : this(notificationKey(sbn),
                sbn.packageName,
                SystemClock.elapsedRealtime(),
                sbn.notification.flags,
                sbn.notification.`when`) {
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false

            other as ExtendedNotificationData

            if (`when` != other.`when`) return false

            return true
        }

        override fun fieldsAsString(): String {
            return StringBuilder()
                    .append("when='").append(`when`).append('\'')
                    .append(", ")
                    .append(super.fieldsAsString())
                    .toString()
        }

        override val className: String
            get() = "ExtendedNotificationData"

    }
}