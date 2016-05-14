package com.app.missednotificationsreminder.binding.model;

import com.app.missednotificationsreminder.binding.util.BindableBoolean;
import com.app.missednotificationsreminder.binding.util.RxBindingUtils;
import com.app.missednotificationsreminder.di.qualifiers.Vibrate;
import com.app.missednotificationsreminder.ui.view.VibrationView;
import com.f2prateek.rx.preferences.Preference;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * The view model for the vibration configuration view
 *
 * @author Eugene Popovich
 */
@Singleton
public class VibrationViewModel extends BaseViewModel {
    /**
     * Data binding field used to handle vibration enabled state
     */
    public BindableBoolean enabled = new BindableBoolean(false);

    /**
     * Preference to store vibrate enabled state
     */
    private Preference<Boolean> mVibrationEnabled;
    /**
     * The related view
     */
    private VibrationView mView;

    /**
     * @param view             the related view
     * @param vibrationEnabled preference to store/retrieve vibration interval enabled information
     */
    @Inject public VibrationViewModel(
            VibrationView view,
            @Vibrate Preference<Boolean> vibrationEnabled) {
        mView = view;
        mVibrationEnabled = vibrationEnabled;
        init();
    }

    void init() {
        monitor(RxBindingUtils.bindWithPreferences(enabled, mVibrationEnabled));

        // vibrate when user enables settings
        monitor(RxBindingUtils
                .valueChanged(enabled)
                .skip(1)
                .filter(enabled -> enabled)
                .subscribe(__ -> mView.vibrate()));
    }

}
