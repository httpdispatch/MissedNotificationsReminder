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
import com.app.missednotificationsreminder.util.TimeUtils;
import com.f2prateek.rx.preferences.Preference;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import rx.observables.ConnectableObservable;
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
    public BindableObject<Float> interval = new BindableObject<>(0f);
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
     * Data binding field to provide maximum possible seekbar value
     */
    public int maxSeekBarValue;
    /**
     * The precise seekbar values (values with better accuracy)
     */
    public int preciseSeekBarValues = 3;
    /**
     * The maximum value in seconds below which the precise configuration can be used
     */
    public int preciseMaxValueSeconds = TimeUtils.SECONDS_IN_MINUTE;
    /**
     * The maximum value in minutes below which the precise configuration can be used
     */
    public int preciseMaxValueMinutes = (int) TimeUtils.secondsToMinutes(preciseMaxValueSeconds);
    /**
     * Data binding field to provide maximum possible interval value when the force wake lock functionality
     * is available
     */
    public int maxIntervalForWakeLock = 10 * TimeUtils.SECONDS_IN_MINUTE;

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
        maxSeekBarValue = (int)TimeUtils.secondsToMinutes(maxInterval - minInterval, TimeUtils.RoundType.CEIL) + preciseSeekBarValues;
        init();
    }

    void init() {
        // pass preferences values to the data binding fields
        enabled.set(mReminderEnabled.get());
        forceWakeLock.set(mForceWakeLock.get());
        // TODO workaround for updated interval measurements
        if(mReminderInterval.get() < minInterval){
            mReminderInterval.set(TimeUtils.minutesToSeconds(mReminderInterval.get()));
        }
        interval.set(TimeUtils.secondsToMinutes(mReminderInterval.get()));
        monitor(
                RxBindingUtils
                        .valueChanged(enabled)
                        .skip(1)// skip initial value emitted automatically right after the subsription
                        .subscribe(mReminderEnabled.asAction()));
        monitor(
                RxBindingUtils
                        .valueChanged(forceWakeLock)
                        .skip(1)// skip initial value emitted automatically right after the subsription
                        .subscribe(mForceWakeLock.asAction()));

        // Inteval changed connectable observable with value transformed from minutes to seconds
        ConnectableObservable<Integer> intervalChanged = RxBindingUtils
                .valueChanged(interval)
                .doOnNext(value -> Timber.d("Interval value changed: %1$f", value))
                .map(value -> TimeUtils.minutesToSeconds(value))
                .doOnNext(value -> Timber.d("Interval value transformed: %1$d", value))
                .publish();

        monitor(
                intervalChanged
                        .skip(1)// skip initial value emitted automatically right after the subsription
                        .debounce(500, TimeUnit.MILLISECONDS)// such as seek bar may change the
                        // interval value very quickly use the debounce function for the timeout
                        // based processing
                        .subscribe(mReminderInterval.asAction()));

        // set wakelock parameter value to false when the interval value become higher when the maxIntervalForWakeLock
        monitor(
                intervalChanged
                        .skip(1)// skip initial value emitted automatically right after the subsription
                        .filter(v -> v > maxIntervalForWakeLock)
                        .map(__ -> false)
                        .subscribe(forceWakeLock.asAction()));

        // do not allow interval to exceed min and max limits
        monitor(
                intervalChanged
                        .filter(value -> value < minInterval)
                        .map(value -> minInterval)
                        .doOnNext(value -> Timber.d("Interval reset to min"))
                        .map(value -> TimeUtils.secondsToMinutes(value, TimeUtils.RoundType.CEIL))
                        .delay(1, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(interval.asAction()));
        monitor(
                intervalChanged
                        .filter(value -> value > maxInterval)
                        .map(value -> maxInterval)
                        .doOnNext(value -> Timber.d("Interval reset to max"))
                        .map(value -> TimeUtils.secondsToMinutes(value, TimeUtils.RoundType.FLOOR))
                        .delay(1, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(interval.asAction()));

        // link interval and seekInterval data binding fields to automatically adjust each other
        // when the value gets changed. SeekBar doesn't have minValue so this is a workaround to
        // provide such functionality
        monitor(
                intervalChanged
                        .filter(value -> value >= minInterval)
                        .filter(value -> value <= maxInterval)
                        .map(value -> (int) (value >= preciseMaxValueSeconds ?
                                TimeUtils.secondsToMinutes(value) + preciseSeekBarValues :
                                TimeUtils.secondsToMinutes(value) * (preciseSeekBarValues + 1)))
                        .map(value -> value == 0 ? 0 : value - 1)
                        .subscribe(seekInterval.asAction()));
        monitor(
                RxBindingUtils
                        .valueChanged(seekInterval)
                        .skip(1) // skip initial
                        .map(value -> value + 1)
                        .map(value -> value > preciseSeekBarValues ?
                                (float) (value - preciseSeekBarValues) :
                                (float) value / (preciseSeekBarValues + 1))
                        .filter(value -> value >= preciseMaxValueMinutes && Math.abs(value - interval.get()) >= 1
                                || value < preciseMaxValueMinutes && Math.abs(value - interval.get()) >= (float) preciseMaxValueMinutes / (preciseSeekBarValues + 1))
                        .subscribe(interval.asAction()));

        intervalChanged.connect();
    }

}
