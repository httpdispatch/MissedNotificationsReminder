package com.app.missednotificationsreminder.settings.vibration;

import android.content.Context;
import android.os.Vibrator;

import com.app.missednotificationsreminder.R;
import com.app.missednotificationsreminder.binding.model.BaseViewModel;
import com.app.missednotificationsreminder.binding.util.BindableBoolean;
import com.app.missednotificationsreminder.binding.util.BindableString;
import com.app.missednotificationsreminder.binding.util.RxBindingUtils;
import com.app.missednotificationsreminder.di.qualifiers.ForApplication;
import com.app.missednotificationsreminder.di.qualifiers.Vibrate;
import com.app.missednotificationsreminder.di.qualifiers.VibrationPattern;
import com.f2prateek.rx.preferences.Preference;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * The view model for the vibration configuration view
 *
 * @author Eugene Popovich
 */
public class VibrationViewModel extends BaseViewModel {
    /**
     * Data binding field used to handle vibration enabled state
     */
    public BindableBoolean enabled = new BindableBoolean(false);

    /**
     * Data binding field used to handle pattern error information
     */
    public BindableString patternError = new BindableString();

    /**
     * Data binding field used to handle vibration pattern
     */
    public BindableString pattern = new BindableString();

    /**
     * Preference to store vibrate enabled state
     */
    private Preference<Boolean> mVibrationEnabled;
    /**
     * Preference to store vibration pattern
     */
    private Preference<String> mVibrationPattern;

    @Inject Vibrator mVibrator;
    /**
     * The activity context
     */
    private Context mContext;

    /**
     * @param vibrationEnabled preference to store/retrieve vibration interval enabled information
     * @param vibrationPattern preference to store/retrieve vibration pattern information
     * @param context          preference the activity context
     */
    @Inject public VibrationViewModel(
            @Vibrate Preference<Boolean> vibrationEnabled,
            @VibrationPattern Preference<String> vibrationPattern,
            Vibrator vibrator,
            @ForApplication Context context) {
        mVibrationEnabled = vibrationEnabled;
        mVibrationPattern = vibrationPattern;
        mVibrator = vibrator;
        mContext = context;
        init();
    }

    private boolean validateVibrationPattern(String pattern) {
        boolean valid = pattern.matches("\\s*\\d+(\\s*,\\s*\\d+)*\\s*");
        patternError.set(valid ? null : mContext.getString(R.string.vibration_pattern_error));
        return valid;
    }

    void init() {
        monitor(RxBindingUtils.bindWithPreferences(enabled, mVibrationEnabled));

        // vibrate when user enables settings
        monitor(RxBindingUtils
                .valueChanged(enabled)
                .skip(1)
                .filter(enabled -> enabled)
                .subscribe(__ -> vibrate()));

        // set the initial pattern from preferences
        pattern.set(mVibrationPattern.get());

        // subscribe preferences to the data binding fields changing events to save the modified
        // values when they represent a valid vibration pattern
        monitor(RxBindingUtils
                .valueChanged(pattern)
                .skip(1)// skip initial value emitted automatically right after the subscription
                .debounce(500, TimeUnit.MILLISECONDS)
                .filter(pattern -> validateVibrationPattern(pattern)) // validate pattern
                .map(pattern -> pattern.replaceFirst("^\\s+", "").replaceFirst("\\s+$", "")) // trim
                .subscribe(mVibrationPattern.asAction()));

        // subscribe data binding field to the preference change event
        monitor(mVibrationPattern
                .asObservable()
                .subscribe(pattern.asAction()));
    }


    public void vibrate() {
        mVibrator.vibrate(200);
    }
}
