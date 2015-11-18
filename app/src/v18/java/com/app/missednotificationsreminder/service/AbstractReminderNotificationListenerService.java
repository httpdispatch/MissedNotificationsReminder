package com.app.missednotificationsreminder.service;

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

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Timber.d("onNotificationPosted: for package %1$s", sbn.getPackageName());
        onNotificationPosted();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Timber.d("onNotificationRemoved: for package %1$s", sbn.getPackageName());
        // stop alarm and check whether it should be launched again
        onNotificationRemoved();
    }

    @Override
    public boolean checkNotificationForAtLeastOnePackageExists(Collection<String> packages) {
        boolean result = false;
        for (StatusBarNotification notificationData : getActiveNotifications()) {
            String packageName = notificationData.getPackageName();
            Timber.d("checkNotificationForAtLeastOnePackageExists: checking package %1$s", packageName);
            result |= packages.contains(packageName);
            if (result) {
                Timber.d("checkNotificationForAtLeastOnePackageExists: found match for package %1$s", packageName);
                break;
            }
        }
        return result;
    }
}
