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
import com.app.missednotificationsreminder.di.qualifiers.ReminderRepeats;
import com.app.missednotificationsreminder.di.qualifiers.ReminderRepeatsMax;
import com.app.missednotificationsreminder.di.qualifiers.ReminderRepeatsMin;
import com.app.missednotificationsreminder.di.qualifiers.LimitReminderRepeats;
import com.app.missednotificationsreminder.ui.view.ReminderView;
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
public class ReminderViewModel extends BaseViewModel {

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
     * Data binding field used to handle whether reminder repeats are to be limited.
     */
    public BindableBoolean limitReminderRepeats = new BindableBoolean(false);
    /**
     * Data binding field used to handle repeats value information
     */
    public BindableObject<Integer> repeats = new BindableObject<>(10);
    /**
     * Data binding field used to mirror {@link #interval} field for the interval SeekBar with the
     * value adjustment such as SeekBar doesn't have minValue parameter
     */
    public BindableObject<Integer> seekInterval = new BindableObject<Integer>(0);
    /**
     * Data binding field used to mirror {@link #repeats} field for the repeats SeekBar with the
     * value adjustment such as SeekBar doesn't have minValue parameter
     */
    public BindableObject<Integer> seekRepeats = new BindableObject<Integer>(0);
    /**
     * Data binding field to provide maximum possible interval information to the interval SeekBar
     */
    public int maxInterval;
    /**
     * Data binding field to provide minimum possible interval information to the interval SeekBar
     */
    public int minInterval;
    /**
     * Data binding field to provide maximum possible repeats information to the interval SeekBar
     */
    public int maxRepeats;
    /**
     * Data binding field to provide minimum possible repeats information to the interval SeekBar
     */
    public int minRepeats;
    /**
     * Data binding field to provide maximum possible interval seekbar value
     */
    public int maxIntervalSeekBarValue;
    /**
     * Data binding field to provide maximum possible repeats seekbar value
     */
    public int maxRepeatsSeekBarValue;
    /**
     * The precise interval seekbar values (values with better accuracy)
     */
    public int preciseIntervalSeekBarValues = 3;
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
    private Preference<Integer> mReminderRepeats;
    private Preference<Boolean> mForceWakeLock;
    private Preference<Boolean> mLimitReminderRepeats;
    private ReminderView mView;


    /**
     * @param view             the related view
     * @param reminderEnabled  preference to store/retrieve reminder interval enabled information
     * @param reminderInterval preference to store/retrieve reminder interval value
     * @param reminderRepeats  preference to store/retrieve number of reminder repetitions
     * @param maxInterval      the maximum allowed reminder interval value
     * @param minInterval      the minimum allowed reminder interval value
     */
    @Inject public ReminderViewModel(
            ReminderView view,
            @ReminderEnabled Preference<Boolean> reminderEnabled,
            @ForceWakeLock Preference<Boolean> forceWakeLock,
            @LimitReminderRepeats Preference<Boolean> limitReminderRepeats,
            @ReminderInterval Preference<Integer> reminderInterval,
            @ReminderRepeats Preference<Integer> reminderRepeats,
            @ReminderIntervalMax int maxInterval,
            @ReminderIntervalMin int minInterval,
            @ReminderRepeatsMax int maxRepeats,
            @ReminderRepeatsMin int minRepeats
    ) {
        mView = view;
        mReminderEnabled = reminderEnabled;
        mForceWakeLock = forceWakeLock;
        mReminderInterval = reminderInterval;
        mReminderRepeats = reminderRepeats;
        mLimitReminderRepeats = limitReminderRepeats;
        this.maxInterval = maxInterval;
        this.minInterval = minInterval;
        this.maxRepeats = maxRepeats;
        this.minRepeats = minRepeats;
        maxIntervalSeekBarValue = (int)TimeUtils.secondsToMinutes(maxInterval - minInterval, TimeUtils.RoundType.CEIL) + preciseIntervalSeekBarValues;
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
        repeats.set(mReminderRepeats.get());
        limitReminderRepeats.set(mLimitReminderRepeats.get());
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
        monitor(
                RxBindingUtils
                        .valueChanged(limitReminderRepeats)
                        .skip(1)// skip initial value emitted automatically right after the subsription
                        .subscribe(mLimitReminderRepeats.asAction()));

        // Interval changed connectable observable with value transformed from minutes to seconds
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
                        // the value could be changed again within that one millisecond delay
                        .filter(__ -> interval.get() < minInterval)
                        .subscribe(interval.asAction()));
        monitor(
                intervalChanged
                        .filter(value -> value > maxInterval)
                        .map(value -> maxInterval)
                        .doOnNext(value -> Timber.d("Interval reset to max"))
                        .map(value -> TimeUtils.secondsToMinutes(value, TimeUtils.RoundType.FLOOR))
                        .delay(1, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        // the value could be changed again within that one millisecond delay
                        .filter(__ -> interval.get() > maxInterval)
                        .subscribe(interval.asAction()));

        // link interval and seekInterval data binding fields to automatically adjust each other
        // when the value gets changed. SeekBar doesn't have minValue so this is a workaround to
        // provide such functionality
        monitor(
                intervalChanged
                        .filter(value -> value >= minInterval)
                        .filter(value -> value <= maxInterval)
                        .map(value -> (int) (value >= preciseMaxValueSeconds ?
                                TimeUtils.secondsToMinutes(value) + preciseIntervalSeekBarValues :
                                TimeUtils.secondsToMinutes(value) * (preciseIntervalSeekBarValues + 1)))
                        .map(value -> value == 0 ? 0 : value - 1)
                        .subscribe(seekInterval.asAction()));
        monitor(
                RxBindingUtils
                        .valueChanged(seekInterval)
                        .skip(1) // skip initial
                        .map(value -> value + 1)
                        .map(value -> value > preciseIntervalSeekBarValues ?
                                (float) (value - preciseIntervalSeekBarValues) :
                                (float) value / (preciseIntervalSeekBarValues + 1))
                        .filter(value -> value >= preciseMaxValueMinutes && Math.abs(value - interval.get()) >= 1
                                || value < preciseMaxValueMinutes && Math.abs(value - interval.get()) >= (float) preciseMaxValueMinutes / (preciseIntervalSeekBarValues + 1))
                        .subscribe(interval.asAction()));

        intervalChanged.connect();

        // Prepare repeats changed observable to be able to add all the rules and activate them
        // together with a call to .connect below.
        ConnectableObservable<Integer> repeatsChanged = RxBindingUtils
                .valueChanged(repeats)
                .publish();

        // Link repeats and seekRepeats data binding fields to automatically adjust each other
        // when the value gets changed. SeekBar doesn't have minValue so we need mapping logic
        // below.
        monitor(
                RxBindingUtils
                        .valueChanged(seekRepeats)
                        // Skip initial value emitted automatically right after the subsription.
                        .skip(1)
                        .doOnNext(value -> Timber.d("seekRepeats changed to %d", value))
                        // SeekBar can only take values in [0, max] range, therefore we need to
                        // convert reported value from [0, maxRepeats-minRepeats] range to
                        // [minRepeats, maxRepeats] range before showing it in the text field.
                        .map(value -> value + minRepeats)
                        // This filtering is not strictly necessary since SeekBar has limits on
                        // values that it can take, but I've decided to add them anyway to err on
                        // the safe side.
                        .filter(value -> value >= minRepeats)
                        .filter(value -> value <= maxRepeats)
                        .subscribe(repeats.asAction()));

        monitor(
                repeatsChanged
                        // Skip initial value emitted automatically right after the subsription.
                        .skip(1)
                        .doOnNext(value -> Timber.d("repeats changed to %d", value))
                        // Make sure that the value set in the text field is within allowed
                        // boundaries.
                        .filter(value -> value >= minRepeats)
                        .filter(value -> value <= maxRepeats)
                        // Text field shows an actual number of reminder repetitions, but SeekBar
                        // can only take a value in [0, max] range, therefore we need to map values
                        // from [minRepeats, maxRepeats] range into [0, maxRepeats-minRepeats] range
                        // used by SeekBar.
                        .map(value -> value - minRepeats)
                        .subscribe(seekRepeats.asAction()));

        // Do not allow repeats to exceed min and max limits.
        monitor(
                repeatsChanged
                        .filter(value -> value < minRepeats)
                        .doOnNext(value -> Timber.d("Repeats reset to min"))
                        .map(value -> minRepeats)
                        .delay(1, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        // the value could be changed again within that one millisecond delay
                        .filter(__ -> repeats.get() < minRepeats)
                        .subscribe(repeats.asAction()));
        monitor(
                repeatsChanged
                        .filter(value -> value > maxRepeats)
                        .doOnNext(value -> Timber.d("Repeats reset to max"))
                        .map(value -> maxRepeats)
                        .delay(1, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        // the value could be changed again within that one millisecond delay
                        .filter(__ -> repeats.get() > maxRepeats)
                        .subscribe(repeats.asAction()));

        // Link any changes in the repeats to the reminderRepeats app preference.
        monitor(
                repeatsChanged
                        // Skip initial value emitted automatically right after the subsription.
                        .skip(1)
                        // Make sure that the value set in the text field is within allowed
                        // boundaries.
                        .filter(value -> value >= minRepeats)
                        .filter(value -> value <= maxRepeats)
                        // SeekBar may change the value very quickly, therefore we use the debounce
                        // function for the timeout based processing.
                        .debounce(500, TimeUnit.MILLISECONDS)
                        .doOnNext(value -> Timber.d("reminderRepeats preference updated"))
                        .subscribe(mReminderRepeats.asAction()));

        repeatsChanged.connect();
    }

}
