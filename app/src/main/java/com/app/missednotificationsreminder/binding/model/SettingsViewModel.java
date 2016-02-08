package com.app.missednotificationsreminder.binding.model;

import android.content.Context;
import android.view.View;

import com.app.missednotificationsreminder.binding.util.BindableBoolean;
import com.app.missednotificationsreminder.di.qualifiers.ForActivity;
import com.app.missednotificationsreminder.service.ReminderNotificationListenerService;
import com.app.missednotificationsreminder.service.util.ReminderNotificationListenerServiceUtils;
import com.app.missednotificationsreminder.ui.activity.ApplicationsSelectionActivity;
import com.app.missednotificationsreminder.ui.view.SettingsView;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * The view model for the settings view
 *
 * @author Eugene Popovich
 */
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
     * The function to check whether the notification service is enabled for the specified
     * package name
     */
    private final Func1<Class<?>, Observable<Boolean>> checkAccess =
            serviceClass -> {
                boolean result = ReminderNotificationListenerServiceUtils.isServiceEnabled(context, serviceClass);
                return Observable.just(result);
            };
}
