package com.app.missednotificationsreminder.settings;

import android.Manifest;
import android.content.Context;
import android.os.Vibrator;
import android.text.TextUtils;

import com.app.missednotificationsreminder.binding.model.BaseViewModel;
import com.app.missednotificationsreminder.binding.util.BindableBoolean;
import com.app.missednotificationsreminder.binding.util.BindableString;
import com.app.missednotificationsreminder.di.qualifiers.FragmentScope;
import com.app.missednotificationsreminder.service.ReminderNotificationListenerService;
import com.app.missednotificationsreminder.service.util.ReminderNotificationListenerServiceUtils;
import com.app.missednotificationsreminder.util.BatteryUtils;
import com.tbruyelle.rxpermissions.RxPermissions;

import javax.inject.Inject;

import rx.Observable;
import timber.log.Timber;

/**
 * The view model for the settings view
 *
 * @author Eugene Popovich
 */
@FragmentScope
public class SettingsViewModel extends BaseViewModel {

    /**
     * Permissions required by the application
     */
    static String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.VIBRATE,
    };

    /**
     * Data binding field used for the access initialized flag
     */
    public BindableBoolean accessInitialized = new BindableBoolean(false);
    /**
     * Data binding field used for the access enabled flag
     */
    public BindableBoolean accessEnabled = new BindableBoolean(false);
    /**
     * Data binding field used for the battery optimization disabled information flag
     */
    public BindableBoolean batteryOptimizationDisabled = new BindableBoolean(false);
    /**
     * Data binding field used for the show advanced settings flag
     */
    public BindableBoolean advancedSettingsVisible = new BindableBoolean(false);
    /**
     * Data binding field used to hold information about whether the vibration is available
     */
    public BindableBoolean vibrationSettingsVisible = new BindableBoolean(false);
    /**
     * Data binding field used to hold information about missing required permissions
     */
    public BindableString missingPermissions = new BindableString();
    /**
     * The vibrator instance
     */
    final Vibrator mVibrator;

    @Inject public SettingsViewModel(Vibrator vibrator) {
        Timber.d("SettingsViewModel: init");
        mVibrator = vibrator;
    }

    /**
     * Run the operation to check whether the notification service is enabled
     */
    public void checkServiceEnabled(Context context) {
        monitor(
                Observable
                        .just(ReminderNotificationListenerService.class)
                        .map(serviceClass -> ReminderNotificationListenerServiceUtils.isServiceEnabled(context, serviceClass))
                        .subscribe(enabled -> {
                            accessEnabled.set(enabled);
                            accessInitialized.set(true);
                        }, t -> Timber.e(t, "Unexpected"))
        );
    }

    /**
     * Run the operation to check whether the battery optimization is disabled for the application
     */
    public void checkBatteryOptimizationDisabled(Context context) {
        batteryOptimizationDisabled.set(!isBatteryOptimizationSettingsVisible() ||
                BatteryUtils.isBatteryOptimizationDisabled(context));
    }

    /**
     * Check whether all required permissions are granted
     */
    public void checkPermissions(Context context) {
        monitor(Observable
                .from(REQUIRED_PERMISSIONS)
                .filter(permission -> !RxPermissions.getInstance(context).isGranted(permission))
                .toList()
                .map(permissions -> TextUtils.join(", ", permissions))
                .subscribe(missingPermissions.asAction()));
    }

    /**
     * Check whether the vibration is allowed on device
     */
    public void checkVibrationAvailable() {
        vibrationSettingsVisible.set(mVibrator.hasVibrator());
    }

    /**
     * Grant required permissions
     */
    public void grantRequiredPermissions(Context context) {
        Timber.d("grantRequiredPermissions");
        monitor(RxPermissions
                .getInstance(context)
                .requestEach(REQUIRED_PERMISSIONS)
                .filter(permission -> !permission.granted) // skip already granted permissions
                .map(permission -> permission.name) // collect names of the not yet granted permissions
                .toList()
                .map(permissions -> TextUtils.join(", ", permissions))
                .subscribe(missingPermissions.asAction()));
    }

    /**
     * Data binding method used to determine whether to display battery optimization settings
     */
    public boolean isBatteryOptimizationSettingsVisible() {
        return BatteryUtils.isBatteryOptimizationSettingsAvailable();
    }
}
