package com.app.missednotificationsreminder.binding.model;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;

import com.app.missednotificationsreminder.binding.util.BindableBoolean;
import com.app.missednotificationsreminder.di.qualifiers.ForActivity;
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
                        .just(context.getPackageName())
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
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        context.startActivity(intent);
    }

    /**
     * The function to check whether the notification service is enabled for the specified
     * package name
     */
    private final Func1<String, Observable<Boolean>> checkAccess =
            packageName -> {
                boolean result = false;
                ContentResolver contentResolver = context.getContentResolver();
                String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
                result = enabledNotificationListeners != null && enabledNotificationListeners
                        .contains(packageName);
                return Observable.just(result);
            };
}
