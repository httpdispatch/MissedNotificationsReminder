package com.app.missednotificationsreminder.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

/**
 * Various battery usage related utilities
 */
public class BatteryUtils {

    /**
     * Check whether the battery optimization is disabled for the application
     *
     * @param context
     * @return
     */
    @TargetApi(Build.VERSION_CODES.M) //
    public static boolean isBatteryOptimizationDisabled(Context context) {
        String packageName = context.getPackageName();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm.isIgnoringBatteryOptimizations(packageName);
    }

    /**
     * Get the intent to either request ignore battery optimization or open battery optimization settings
     * depend on whether the battery optimization is already disabled for the application
     *
     * @param context
     * @return
     */
    @TargetApi(Build.VERSION_CODES.M) //
    public static Intent getBatteryOptimizationIntent(Context context) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//   TODO google didn't allow to use this for the MissedNotificationsReminder app
//        String packageName = context.getPackageName();
//        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//        if (pm.isIgnoringBatteryOptimizations(packageName)) {
//            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//        }
//        else {
//            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//            intent.setData(Uri.parse("package:" + packageName));
//        }
        return intent;
    }

    /**
     * Check whether the battery optimization settings available in the current OS version
     *
     * @return
     */
    public static boolean isBatteryOptimizationSettingsAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
}
