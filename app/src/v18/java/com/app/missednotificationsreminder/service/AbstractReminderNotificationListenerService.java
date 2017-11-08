package com.app.missednotificationsreminder.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.Collection;

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
            // such as onListenerConnected is not called on anroid prior L call onReady method explicitly
            onReady();
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Timber.d("onNotificationPosted: for package %1$s", sbn.getPackageName());
        onNotificationPosted(sbn.getPackageName());
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Timber.d("onNotificationRemoved: for package %1$s", sbn.getPackageName());
        // stop alarm and check whether it should be launched again
        onNotificationRemoved();
    }

    @Override
    public boolean checkNotificationForAtLeastOnePackageExists(Collection<String> packages, boolean ignoreOngoing) {
        boolean result = false;
        StatusBarNotification[] activeNotifications = getActiveNotifications();
        if (activeNotifications != null) {
            for (StatusBarNotification notificationData : getActiveNotifications()) {
                String packageName = notificationData.getPackageName();
                Timber.d("checkNotificationForAtLeastOnePackageExists: checking package %1$s", packageName);
                boolean contains = packages.contains(packageName);
                if (contains && ignoreOngoing && (notificationData.getNotification().flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) {
                    Timber.d("checkNotificationForAtLeastOnePackageExists: found ongoing match which is requested to be skipped");
                    continue;
                }
                result |= contains;
                if (result) {
                    Timber.d("checkNotificationForAtLeastOnePackageExists: found match for package %1$s", packageName);
                    break;
                }
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
