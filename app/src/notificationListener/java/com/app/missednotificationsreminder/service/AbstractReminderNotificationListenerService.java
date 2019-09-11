package com.app.missednotificationsreminder.service;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import com.app.missednotificationsreminder.data.model.NotificationData;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import timber.log.Timber;

/**
 * The service to monitor all status bar notifications.
 *
 * @author Eugene Popovich
 */
public abstract class AbstractReminderNotificationListenerService extends NotificationListenerService implements ReminderNotificationListenerServiceInterface {

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // such as onListenerConnected is not called on android prior L call onReady method explicitly
            onReady();
        }
    }

    @Override public void actualizeNotificationData() {
        StatusBarNotification[] activeNotifications = getActiveNotifications();
        Set<NotificationData> snapshotNotifications = new HashSet<>(getNotificationsData());
        if (activeNotifications != null) {
            for (StatusBarNotification sbn : activeNotifications) {
                NotificationData notificationData = findNotificationData(sbn, snapshotNotifications);
                if (notificationData != null) {
                    snapshotNotifications.remove(notificationData);
                } else {
                    Timber.d("actualizeNotificationData() found new %s", sbn);
                    onNotificationPosted(sbn);
                }
            }
        }
        for (NotificationData notificationData : snapshotNotifications) {
            Timber.w("actualizeNotificationData() found already removed %s", notificationData);
            onNotificationRemoved(notificationData);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) {
            // fix weird NPE on some devices
            return;
        }
        Timber.d("onNotificationPosted: for package %1$s, key %2$s, when %3$s", sbn.getPackageName(), notificationKey(sbn), sbn.getNotification().when);
        NotificationData notificationData = findNotificationData(sbn);
        if (notificationData == null) {
            notificationData = new ExtendedNotificationData(sbn);
        }
        onNotificationPosted(notificationData);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) {
            // fix weird NPE on some devices
            return;
        }
        Timber.d("onNotificationRemoved: for package %1$s, key %2$s, when %3$s", sbn.getPackageName(), notificationKey(sbn), sbn.getNotification().when);
        NotificationData notificationData = findNotificationData(sbn);
        if (notificationData == null) {
            Timber.w("onNotificationRemoved: can't find internal notification data for the status bar notification %s",
                    notificationKey(sbn));
        } else {
            // stop alarm and check whether it should be launched again
            onNotificationRemoved(notificationData);
        }
    }

    ExtendedNotificationData findNotificationData(StatusBarNotification sbn) {
        return findNotificationData(sbn, getNotificationsData());
    }

    ExtendedNotificationData findNotificationData(StatusBarNotification sbn, Collection<NotificationData> snapshotNotifications) {
        ExtendedNotificationData result = null;
        for (NotificationData notificationData : snapshotNotifications) {
            ExtendedNotificationData extendedNotificationData = (ExtendedNotificationData) notificationData;
            if (extendedNotificationData.notificationKey.equals(notificationKey(sbn))) {
                result = extendedNotificationData;
                break;
            }
        }
        return result;
    }

    private String notificationKey(StatusBarNotification notification) {
        // This method re-implements StatusBarNotification.getKey() method, which is available
        // starting with API level 20, but we want to support API level 18+. The method
        // StatusBarNotification.getUserId() that we use below is deprecated, but the replacement
        // StatusBarNotification.getUser() method is only available starting with API level 21.
        return String.valueOf(
                Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ?
                        notification.getUserId()
                        : notification.getUser())
                + "|" + notification.getPackageName()
                + "|" + notification.getId()
                + "|" + notification.getTag()
                // The following part is not present in the original getKey(),
                // but needed since some IM apps, e.g. Hangouts, re-post the
                // same notification when a new message is received. As a
                // result, ignoring a notification for the first IM message by
                // deleting the dismiss notificaiton, will cause all further
                // messages to be ignored too.
                // TODO: Make this configurable in the settings as not all users
                // make like this behavior and some may choose later messages to
                // be ignored too
                + "|" + String.valueOf(notification.getNotification().when);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override public void onListenerConnected() {
        super.onListenerConnected();
        onReady();
    }

    class ExtendedNotificationData extends NotificationData {
        final String notificationKey;

        public ExtendedNotificationData(StatusBarNotification sbn) {
            this(sbn.getId(),
                    sbn.getPackageName(),
                    SystemClock.elapsedRealtime(),
                    sbn.getNotification().flags,
                    notificationKey(sbn));
        }

        public ExtendedNotificationData(int id, String packageName, long foundAtTime, int flags, String notificationKey) {
            super(Integer.toString(id), packageName, foundAtTime, flags);
            this.notificationKey = notificationKey;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            ExtendedNotificationData that = (ExtendedNotificationData) o;
            return TextUtils.equals(notificationKey, that.notificationKey);
        }

        @Override protected String fieldsAsString() {
            return new StringBuilder()
                    .append("notificationKey='").append(notificationKey).append('\'')
                    .append(", " + super.fieldsAsString())
                    .toString();
        }

        @Override protected String getClassName() {
            return "ExtendedNotificationData";
        }
    }
}
