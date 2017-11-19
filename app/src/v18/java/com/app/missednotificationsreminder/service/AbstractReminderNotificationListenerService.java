package com.app.missednotificationsreminder.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import timber.log.Timber;

/**
 * The service to monitor all status bar notifications.
 *
 * @author Eugene Popovich
 */
public abstract class AbstractReminderNotificationListenerService extends NotificationListenerService implements ReminderNotificationListenerServiceInterface {
    ConcurrentLinkedQueue<String> mIgnoredNotificationKeys = new ConcurrentLinkedQueue<>();

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // such as onListenerConnected is not called on anroid prior L call onReady method explicitly
            onReady();
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) {
            // fix weird NPE on some devices
            return;
        }
        Timber.d("onNotificationPosted: for package %1$s, key %2$s", sbn.getPackageName(), notificationKey(sbn));
        mIgnoredNotificationKeys.remove(notificationKey(sbn));
        onNotificationPosted(sbn.getPackageName());
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) {
            // fix weird NPE on some devices
            return;
        }
        Timber.d("onNotificationRemoved: for package %1$s", sbn.getPackageName());
        // stop alarm and check whether it should be launched again
        onNotificationRemoved();
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
                + "|" + notification.getTag();
    }

    @Override
    public void ignoreAllCurrentNotifications() {
        mIgnoredNotificationKeys.clear();
        StatusBarNotification[] activeNotifications = getActiveNotifications();
        if (activeNotifications != null) {
            // potential NPE fix check on some devices
            for (StatusBarNotification notificationData : activeNotifications) {
                String notificationKey = notificationKey(notificationData);
                mIgnoredNotificationKeys.add(notificationKey);
                Timber.d("ignoreAllCurrentNotifications: start ignoring %s", notificationKey);
            }
        }
    }

    @Override
    public boolean checkNotificationForAtLeastOnePackageExists(Collection<String> packages, boolean ignoreOngoing) {
        boolean result = false;
        StatusBarNotification[] activeNotifications = getActiveNotifications();
        List<String> activeNotificationKeys = new ArrayList<>();
        if (activeNotifications != null) {
            // potential NPE fix check on some devices
            for (StatusBarNotification notificationData : activeNotifications) {
                String packageName = notificationData.getPackageName();
                Timber.d("checkNotificationForAtLeastOnePackageExists: checking package %1$s", packageName);
                boolean contains = packages.contains(packageName);
                if (contains && ignoreOngoing && (notificationData.getNotification().flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) {
                    Timber.d("checkNotificationForAtLeastOnePackageExists: found ongoing match which is requested to be skipped");
                    continue;
                }
                String notificationKey = notificationKey(notificationData);
                activeNotificationKeys.add(notificationKey);
                if (mIgnoredNotificationKeys.contains(notificationKey)) {
                    Timber.d("checkNotificationForAtLeastOnePackageExists: notification %s ignored", notificationKey);
                    continue;
                }
                result |= contains;
                if (result) {
                    Timber.d("checkNotificationForAtLeastOnePackageExists: found match for package %1$s", packageName);
                    break;
                }
            }
        }
        // Remove notifications that were cancelled already to prevent memory leaks.
        List<String> copy = new ArrayList<>(mIgnoredNotificationKeys);
        for (String ignoredNotificationKey : copy) {
            if (!activeNotificationKeys.contains(ignoredNotificationKey)) {
                mIgnoredNotificationKeys.remove(ignoredNotificationKey);
            }
        }
        return result;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override public void onListenerConnected() {
        super.onListenerConnected();
        onReady();
    }
}
