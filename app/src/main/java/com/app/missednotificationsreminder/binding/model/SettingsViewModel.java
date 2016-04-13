package com.app.missednotificationsreminder.binding.model;

import android.content.Context;
import android.view.View;

import com.app.missednotificationsreminder.binding.util.BindableBoolean;
import com.app.missednotificationsreminder.di.qualifiers.ForActivity;
import com.app.missednotificationsreminder.service.ReminderNotificationListenerService;
import com.app.missednotificationsreminder.service.util.ReminderNotificationListenerServiceUtils;
import com.app.missednotificationsreminder.ui.activity.ApplicationsSelectionActivity;
import com.app.missednotificationsreminder.ui.view.SettingsView;
import com.app.missednotificationsreminder.util.BatteryUtils;

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
     * The activity context
     */
    @Inject @ForActivity Context context;
    /**
     * The related view
     */
    @Inject SettingsView view;

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
     * Method which is called when the select applications button is clicked. It launches the
     * {@linkplain ApplicationsSelectionActivity applications selection activity}
     *
     * @param v
     */
    public void onSelectApplicationsButtonClicked(View v) {
        context.startActivity(ApplicationsSelectionActivity
                .getCallingIntent(context));
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
     * Method which is called when the manage access button is clicked. It launches the system
     * notification listener settings window
     *
     * @param v
     */
    public void onManageBatteryOptimizationPressed(View v) {
        context.startActivity(BatteryUtils.getBatteryOptimizationIntent(context));
    }

    /**
     * Data binding method used to determine whether to display battery optimization settings
     *
     * @return
     */
    public boolean isBatteryOptimizationSettingsVisible() {
        return BatteryUtils.isBatteryOptimizationSettingsAvailable();
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
