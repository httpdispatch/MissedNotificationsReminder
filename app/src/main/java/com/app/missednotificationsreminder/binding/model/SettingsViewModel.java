package com.app.missednotificationsreminder.binding.model;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.View;

import com.app.missednotificationsreminder.binding.util.BindableBoolean;
import com.app.missednotificationsreminder.binding.util.BindableString;
import com.app.missednotificationsreminder.di.qualifiers.ForActivity;
import com.app.missednotificationsreminder.service.ReminderNotificationListenerService;
import com.app.missednotificationsreminder.service.util.ReminderNotificationListenerServiceUtils;
import com.app.missednotificationsreminder.ui.view.SettingsView;
import com.app.missednotificationsreminder.util.BatteryUtils;
import com.tbruyelle.rxpermissions.RxPermissions;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * The view model for the settings view
 *
 * @author Eugene Popovich
 */
@Singleton
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
     * The activity context
     */
    @Inject @ForActivity Context context;
    /**
     * The related view
     */
    @Inject SettingsView view;
    /**
     * The vibrator instance
     */
    @Inject Vibrator mVibrator;
    /**
     * The nested applications settings view model
     */
    @Inject public ApplicationsSettingsViewModel applicationsSettingsModel;

    /**
     * Run the operation to check whether the notification service is enabled
     */
    public void checkServiceEnabled() {
        monitor(
                Observable
                        .just(ReminderNotificationListenerService.class)
                        .flatMap(checkAccess)
                        .subscribe(enabled -> {
                            accessEnabled.set(enabled);
                            accessInitialized.set(true);
                        }, t -> Timber.e(t, "Unexpected"))
        );
    }

    /**
     * Run the operation to check whether the battery optimization is disabled for the application
     */
    public void checkBatteryOptimizationDisabled() {
        monitor(
                Observable
                        .just(true)
                        .map(checkBatteryOptimizationDisabled)
                        .subscribe(v -> batteryOptimizationDisabled.set(v),
                                t -> Timber.e(t, "Unexpected"))
        );
    }

    /**
     * Check whether all required permissions are granted
     */
    public void checkPermissions() {
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
     * Method which is called when the manage access button is clicked. It launches the system
     * notification listener settings window
     *
     * @param v
     */
    public void onManageAccessButtonPressed(View v) {
        context.startActivity(ReminderNotificationListenerServiceUtils.getServiceEnabledManagementIntent());
    }

    /**
     * Method which is called when the grant permissions button is clicked. It launches the grant permission dialog
     *
     * @param v
     */
    public void onGrantPermissionsPressed(View v) {
        Timber.d("onGrantPermissionsPressed");
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
     * Method which is called when the manage access button is clicked. It launches the system
     * notification listener settings window
     *
     * @param v
     */
    public void onManageBatteryOptimizationPressed(View v) {
        try {
            context.startActivity(BatteryUtils.getBatteryOptimizationIntent(context));
        } catch (ActivityNotFoundException ex) {
            // possibly Oppo phone
            Timber.e(ex);
            // TODO notify view
        }
    }

    /**
     * Data binding method used to determine whether to display battery optimization settings
     *
     * @return
     */
    public boolean isBatteryOptimizationSettingsVisible() {
        return BatteryUtils.isBatteryOptimizationSettingsAvailable();
    }

    @Override public void shutdown() {
        super.shutdown();
        applicationsSettingsModel.shutdown();
    }

    /**
     * The function to check whether the notification service is enabled for the specified
     * package name
     */
    private final Func1<Class<?>, Observable<Boolean>> checkAccess =
            serviceClass -> {
                boolean result = ReminderNotificationListenerServiceUtils.isServiceEnabled(context, serviceClass);
                return Observable.just(result);
            };

    /**
     * The function to check whether the battery optimization is disabled for the application
     */
    private final Func1<Boolean, Boolean> checkBatteryOptimizationDisabled =
            __ -> isBatteryOptimizationSettingsVisible() ?
                    BatteryUtils.isBatteryOptimizationDisabled(context) : true;
}
