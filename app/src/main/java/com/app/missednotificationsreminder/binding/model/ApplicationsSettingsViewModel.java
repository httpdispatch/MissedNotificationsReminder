package com.app.missednotificationsreminder.binding.model;

import com.app.missednotificationsreminder.binding.util.BindableBoolean;
import com.app.missednotificationsreminder.binding.util.RxBindingUtils;
import com.app.missednotificationsreminder.di.qualifiers.IgnorePersistentNotifications;
import com.app.missednotificationsreminder.di.qualifiers.RemindWhenScreenIsOn;
import com.app.missednotificationsreminder.di.qualifiers.RespectPhoneCalls;
import com.app.missednotificationsreminder.di.qualifiers.RespectRingerMode;
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
     * Data binding field used to handle respect ringer mode state
     */
    public BindableBoolean respectRingerMode = new BindableBoolean(false);
    /**
     * Data binding field used to handle remind when screen is on state
     */
    public BindableBoolean remindWhenScreenIsOn = new BindableBoolean(false);

    /**
     * The ignore persistent notifications preference
     */
    Preference<Boolean> mIgnorePersistentNotifications;
    /**
     * The respect phone calls preference
     */
    Preference<Boolean> mRespectPhoneCalls;
    /**
     * The respect ringer mode preference
     */
    Preference<Boolean> mRespectRingerMode;
    /**
     * The remind when screen is on preference
     */
    Preference<Boolean> mRemindWhenScreenIsOn;

    /**
     * Construct instance of the {@link ApplicationsSettingsViewModel}
     */
    @Inject public ApplicationsSettingsViewModel(
            @IgnorePersistentNotifications Preference<Boolean> ignorePersistentNotificationsPref,
            @RespectPhoneCalls Preference<Boolean> respectPhoneCallsPref,
            @RespectRingerMode Preference<Boolean> respectRingerModePref,
            @RemindWhenScreenIsOn Preference<Boolean> remindWhenScreenIsOnPref) {
        mIgnorePersistentNotifications = ignorePersistentNotificationsPref;
        mRespectPhoneCalls = respectPhoneCallsPref;
        mRespectRingerMode = respectRingerModePref;
        mRemindWhenScreenIsOn = remindWhenScreenIsOnPref;
        init();
    }

    /**
     * Perform additional model initialization
     */
    void init() {
        monitor(RxBindingUtils.bindWithPreferences(ignorePersistentNotifications, mIgnorePersistentNotifications));
        monitor(RxBindingUtils.bindWithPreferences(respectPhoneCalls, mRespectPhoneCalls));
        monitor(RxBindingUtils.bindWithPreferences(respectRingerMode, mRespectRingerMode));
        monitor(RxBindingUtils.bindWithPreferences(remindWhenScreenIsOn, mRemindWhenScreenIsOn));
    }
}
