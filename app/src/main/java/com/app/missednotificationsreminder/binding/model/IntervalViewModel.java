package com.app.missednotificationsreminder.binding.model;

import com.app.missednotificationsreminder.binding.util.BindableBoolean;
import com.app.missednotificationsreminder.binding.util.BindableObject;
import com.app.missednotificationsreminder.binding.util.BindableString;
import com.app.missednotificationsreminder.binding.util.RxBindingUtils;
import com.app.missednotificationsreminder.di.qualifiers.ForceWakeLock;
import com.app.missednotificationsreminder.di.qualifiers.ReminderEnabled;
import com.app.missednotificationsreminder.di.qualifiers.ReminderInterval;
import com.app.missednotificationsreminder.di.qualifiers.ReminderIntervalMax;
import com.app.missednotificationsreminder.di.qualifiers.ReminderIntervalMin;
import com.app.missednotificationsreminder.ui.view.IntervalView;
import com.f2prateek.rx.preferences.Preference;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;
import timber.log.Timber;

/**
 * The view model for the interval configuration view
 *
 * @author Eugene Popovich
 */
public class IntervalViewModel extends BaseViewModel {

    /**
     * Data binding field used to handle interval error information
     */
    public BindableString intervalError = new BindableString();
    /**
     * Data binding field used to handle interval enabled state
     */
    public BindableBoolean enabled = new BindableBoolean(false);
    /**
     * Data binding field used to handle use wake lock setting
     */
    public BindableBoolean forceWakeLock = new BindableBoolean(false);
    /**
     * Data binding field used to handle interval value information
     */
    public BindableObject<Integer> interval = new BindableObject<Integer>(0);
    /**
     * Data binding field used to mirror {@link #interval} field for the SeekBar with the value
     * adjustment such as SeekBar doesn't have minValue parameter
     */
    public BindableObject<Integer> seekInterval = new BindableObject<Integer>(0);
    /**
     * Data binding field to provide maximum possible interval information to the SeekBar
     */
    public int maxInterval;
    /**
     * Data binding field to provide minimum possible interval information to the SeekBar
     */
    public int minInterval;
    /**
     * Data binding field to provide maximum possible interval value when the force wake lock functionality
     * is available
     */
    public int maxIntervalForWakeLock = 10;

    private Preference<Boolean> mReminderEnabled;
    private Preference<Integer> mReminderInterval;
    private Preference<Boolean> mForceWakeLock;
    private IntervalView mView;


    /**
     * @param view             the related view
     * @param reminderEnabled  preference to store/retrieve reminder interval enabled information
     * @param reminderInterval preference to store/retrieve reminder interval value
     * @param maxInterval      the maximum allowed reminder interval value
     * @param minInterval      the minimum allowed reminder interval value
     */
    @Inject public IntervalViewModel(
            IntervalView view, @ReminderEnabled Preference<Boolean> reminderEnabled,
            @ForceWakeLock Preference<Boolean> forceWakeLock,
            @ReminderInterval Preference<Integer> reminderInterval,
            @ReminderIntervalMax int maxInterval,
            @ReminderIntervalMin int minInterval
    ) {
        mView = view;
        mReminderEnabled = reminderEnabled;
        mForceWakeLock = forceWakeLock;
        mReminderInterval = reminderInterval;
        this.maxInterval = maxInterval;
        this.minInterval = minInterval;
        init();
    }

    void init() {
        // pass preferences values to the data binding fields
        enabled.set(mReminderEnabled.get());
        forceWakeLock.set(mForceWakeLock.get());
        interval.set(mReminderInterval.get());
        seekInterval.set(interval.get() - minInterval);
        // subscribe preferences to the data binding fields changing events to save the modified
        // values
        monitor(
                RxBindingUtils
                        .valueChanged(enabled)
                        .skip(1)// skip initial value emitted automatically right after the
                                // subsription
                        .subscribe(mReminderEnabled.asAction()));
        monitor(
                RxBindingUtils
                        .valueChanged(forceWakeLock)
                        .skip(1)// skip initial value emitted automatically right after the
                                // subsription
                        .subscribe(mForceWakeLock.asAction()));
        monitor(
                RxBindingUtils
                        .valueChanged(interval)
                        .skip(1)// skip initial value emitted automatically right after the
                                // subsription
                        .debounce(500, TimeUnit.MILLISECONDS)// such as seek bar may change the
                                // interval value very quickly use the
                                // debounce function for the timeout
                                // based processing
                        .subscribe(mReminderInterval.asAction()));

        // set wakelock parameter value to false when the interval value become higher when the maxIntervalForWakeLock
        monitor(
                RxBindingUtils
                        .valueChanged(interval)
                        .skip(1)// skip initial value emitted automatically right after the
                                // subsription
                        .filter(v -> v > maxIntervalForWakeLock)
                        .map(__ -> false)
                        .subscribe(forceWakeLock.asAction()));

        Observable<Integer> intervalChanged = RxBindingUtils
                .valueChanged(interval)
                .map(value -> {
                    Timber.d("Interval value changed: "+value);
                    return value;
                })
                .share();

        // do not allow interval to exceed min and max limits
        monitor(
                intervalChanged
                        .filter(value -> value < minInterval)
                        .map(value -> minInterval)
                        .map(value -> {
                            Timber.d("Interval reset to min");
                            return value;
                        })
                        .subscribe(interval.asAction()));
        monitor(
                intervalChanged
                        .filter(value -> value > maxInterval)
                        .map(value -> maxInterval)
                        .map(value -> {
                            Timber.d("Interval reset to max");
                            return value;
                        })
                        .subscribe(interval.asAction()));

        // link interval and seekInterval data binding fields to automatically adjust each other
        // when the value gets changed. SeekBar doesn't have minValue so this is a workaround to
        // provide such functionality
        monitor(
                intervalChanged
                        .filter(value -> value >= minInterval)
                        .filter(value -> value <= maxInterval)
                        .map(value -> value - minInterval)
                        .subscribe(seekInterval.asAction()));
        monitor(
                RxBindingUtils
                        .valueChanged(seekInterval)
                        .map(value -> value + minInterval)
                        .subscribe(interval.asAction()));

    }

}
