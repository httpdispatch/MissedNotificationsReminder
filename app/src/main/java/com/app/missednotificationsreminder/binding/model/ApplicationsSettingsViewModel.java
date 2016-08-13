package com.app.missednotificationsreminder.binding.model;

import android.content.Context;
import android.view.View;

import com.app.missednotificationsreminder.binding.util.BindableBoolean;
import com.app.missednotificationsreminder.binding.util.RxBindingUtils;
import com.app.missednotificationsreminder.di.qualifiers.ForActivity;
import com.app.missednotificationsreminder.di.qualifiers.IgnorePersistentNotifications;
import com.app.missednotificationsreminder.di.qualifiers.RespectPhoneCalls;
import com.app.missednotificationsreminder.ui.activity.ApplicationsSelectionActivity;
import com.f2prateek.rx.preferences.Preference;

import javax.inject.Inject;

/**
 * The view model for the application settings
 */
public class ApplicationsSettingsViewModel extends BaseViewModel {

    /**
     * Data binding field used to handle ignore persistent notification state
     */
    public BindableBoolean ignorePersistentNotifications = new BindableBoolean(false);
    /**
     * Data binding field used to handle respect phone calls state
     */
    public BindableBoolean respectPhoneCalls = new BindableBoolean(false);

    /**
     * The ignore persistent notifications preference
     */
    Preference<Boolean> mIgnorePersistentNotifications;
    /**
     * The respect phone calls preference
     */
    Preference<Boolean> mRespectPhoneCalls;

    /**
     * The context instance
     */
    Context mContext;

    /**
     * Construct instance of the {@link ApplicationsSettingsViewModel}
     *
     * @param ignorePersistentNotificationsPref The ignore persistent notifications preference
     * @param context                           The context instance
     */
    @Inject public ApplicationsSettingsViewModel(
            @IgnorePersistentNotifications Preference<Boolean> ignorePersistentNotificationsPref,
            @RespectPhoneCalls Preference<Boolean> respectPhoneCallsPref,
            @ForActivity Context context) {
        mIgnorePersistentNotifications = ignorePersistentNotificationsPref;
        mRespectPhoneCalls = respectPhoneCallsPref;
        mContext = context;
        init();
    }

    /**
     * Perform additional model initialization
     */
    void init() {
        monitor(RxBindingUtils.bindWithPreferences(ignorePersistentNotifications, mIgnorePersistentNotifications));
        monitor(RxBindingUtils.bindWithPreferences(respectPhoneCalls, mRespectPhoneCalls));
    }

    /**
     * Method which is called when the select applications button is clicked. It launches the
     * {@linkplain ApplicationsSelectionActivity applications selection activity}
     *
     * @param v
     */
    public void onSelectApplicationsButtonClicked(View v) {
        mContext.startActivity(ApplicationsSelectionActivity
                .getCallingIntent(mContext));
    }
}
