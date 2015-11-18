package com.app.missednotificationsreminder.service.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.app.missednotificationsreminder.service.ReminderNotificationListenerService;

/**
 * Various utility methods related to the {@link ReminderNotificationListenerService}
 *
 * @author Eugene Popovich
 */
public class ReminderNotificationListenerServiceUtils {
    /**
     * Get the intent which navigates to the notification listener services settings page
     *
     * @return
     */
    public static Intent getServiceEnabledManagementIntent() {
        return new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
    }

    /**
     * Check whether notification listener service is enabled for current context
     *
     * @param context
     * @param serviceClass
     * @return
     */
    public static boolean isServiceEnabled(Context context, Class<?> serviceClass) {
        ContentResolver contentResolver = context.getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        boolean result = enabledNotificationListeners != null && enabledNotificationListeners
                .contains(context.getPackageName());
        return result;
    }
}
