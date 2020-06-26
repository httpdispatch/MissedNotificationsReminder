package com.app.missednotificationsreminder.binding.model;

import com.app.missednotificationsreminder.binding.util.BindableBoolean;
import com.app.missednotificationsreminder.binding.util.BindableObject;
import com.app.missednotificationsreminder.binding.util.BindableString;
import com.app.missednotificationsreminder.binding.util.RxBindingUtils;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerEnabled;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerMode;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerRangeBegin;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerRangeEnd;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerRangeMax;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerRangeMin;
import com.app.missednotificationsreminder.util.TimeUtils;
import com.f2prateek.rx.preferences.Preference;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.observables.ConnectableObservable;
import timber.log.Timber;

/**
 * The view model for the scheduler configuration view
 *
 * @author Eugene Popovich
 */
public class SchedulerViewModel extends BaseViewModel {
    /**
     * Data binding field used to store scheduler range begin error information
     */
    public BindableString beginError = new BindableString();
    /**
     * Data binding field used to store scheduler range end error information
     */
    public BindableString endError = new BindableString();
    /**
     * Data binding field used to handle scheduler enabled state
     */
    public BindableBoolean enabled = new BindableBoolean(false);
    /**
     * Data binding field used to handle scheduler mode
     */
    public BindableBoolean mode = new BindableBoolean(true);
    /**
     * Data binding field used to store scheduler range begin information represented as human readable string
     */
    public BindableString beginTime = new BindableString();
    /**
     * Data binding field used to store scheduler range end information represented as human readable string
     */
    public BindableString endTime = new BindableString();
    /**
     * Data binding field used to handle scheduler range begin information
     */
    public BindableObject<Integer> begin = new BindableObject<>(0);
    /**
     * * Data binding field used to handle scheduler range end information
     */
    public BindableObject<Integer> end = new BindableObject<>(0);
    /**
     * Data binding field used to mirror {@link #begin} field for the RangeBar with the value
     * transformation such as RangeBar has 5 minutes interval specified
     */
    public BindableObject<Integer> rangeBegin = new BindableObject<>(0);
    /**
     * Data binding field used to mirror {@link #end} field for the RangeBar with the value
     * transformation such as RangeBar has 5 minutes interval specified
     */
    public BindableObject<Integer> rangeEnd = new BindableObject<>(0);
    /**
     * Data binding field to provide maximum possible value information to the RangeBar
     */
    public int maximum;
    /**
     * Data binding field to provide minimum possible value information to the RangeBar
     */
    public int minimum;
    /**
     * The interval used for the range bar. Currently it is 5 minutes
     */
    public int interval = 5;
    /**
     * Preference to store/retrieve scheduler enabled information
     */
    private Preference<Boolean> mSchedulerEnabled;
    /**
     * Preference to store/retrieve scheduler mode information
     */
    private Preference<Boolean> mSchedulerMode;
    /**
     * Preference to store/retrieve scheduler range begin value
     */
    private Preference<Integer> mSchedulerRangeBegin;
    /**
     * Preference to store/retrieve scheduler range end value
     */
    private Preference<Integer> mSchedulerRangeEnd;

    /**
     * @param schedulerEnabled      Preference to store/retrieve scheduler enabled information
     * @param schedulerMode         Preference to store/retrieve scheduler mode information
     * @param schedulerRangeBegin   Preference to store/retrieve scheduler range begin value
     * @param schedulerRangeEnd     Preference to store/retrieve scheduler range end value
     * @param schedulerRangeMinimum The maximum allowed scheduler range value
     * @param schedulerRangeMaximum The minimum allowed scheduler range value
     */
    @Inject public SchedulerViewModel(@SchedulerEnabled Preference<Boolean> schedulerEnabled,
                                      @SchedulerMode Preference<Boolean> schedulerMode,
                                      @SchedulerRangeBegin Preference<Integer> schedulerRangeBegin,
                                      @SchedulerRangeEnd Preference<Integer> schedulerRangeEnd,
                                      @SchedulerRangeMin int schedulerRangeMinimum,
                                      @SchedulerRangeMax int schedulerRangeMaximum) {
        mSchedulerEnabled = schedulerEnabled;
        mSchedulerMode = schedulerMode;
        mSchedulerRangeBegin = schedulerRangeBegin;
        mSchedulerRangeEnd = schedulerRangeEnd;
        this.minimum = schedulerRangeMinimum;
        this.maximum = schedulerRangeMaximum;
        init();
    }

    void init() {
        // pass preferences values to the data binding fields
        enabled.set(mSchedulerEnabled.get());
        mode.set(mSchedulerMode.get());
        begin.set(mSchedulerRangeBegin.get());
        end.set(mSchedulerRangeEnd.get());
        rangeBegin.set(mSchedulerRangeBegin.get() / interval);
        rangeEnd.set(mSchedulerRangeEnd.get() / interval);
        // subscribe preferences to the data binding fields changing events to save the modified
        // values
        monitor(
                RxBindingUtils
                        .valueChanged(enabled)
                        .skip(1)// skip initial value emitted automatically right after the
                        // subsription
                        .subscribe(mSchedulerEnabled.asAction()));
        monitor(
                RxBindingUtils
                        .valueChanged(mode)
                        .skip(1)// skip initial value emitted automatically right after the
                        // subsription
                        .subscribe(mSchedulerMode.asAction()));

        // can't use shared observable because of unexpected behaviour with skip call, so using ConnectableObservable
        ConnectableObservable<Integer> beginChanged = RxBindingUtils
                .valueChanged(begin)
                .doOnEach(value -> Timber.d("Begin value changed: %s", value.getValue()))
                .publish();

        monitor(
                beginChanged
                        .map(minutes -> TimeUtils.minutesToTime(minutes))
                        .subscribe(beginTime.asAction()));
        monitor(
                beginChanged
                        .map(minutes -> minutes / interval)
                        .subscribe(rangeBegin.asAction()));
        monitor(
                beginChanged
                        .skip(1)// skip initial value emitted automatically right after the
                        // subsription
                        .debounce(500, TimeUnit.MILLISECONDS)// such as range bar may change the
                        // value very quickly use the
                        // debounce function for the timeout
                        // based processing
                        .doOnEach(value -> Timber.d("Begin value changed 2: %s", value.getValue()))
                        .subscribe(mSchedulerRangeBegin.asAction()));

        beginChanged.connect();

        // link begin and rangeBegin data binding fields to automatically adjust each other
        // when the value gets changed. RangeBar has an interval specified so the value should be transformed

        monitor(
                RxBindingUtils
                        .valueChanged(rangeBegin)
                        .map(v -> v * interval)
                        .doOnEach(value -> Timber.d("Range begin value changed: %s", value.getValue()))
                        .filter(minutes -> Math.abs(minutes - begin.get()) > interval) // user may enter value manually within interval by using time picker dialog,
                        // so ignore range bar changes in such case
                        .doOnEach(value -> Timber.d("Range begin filtered value changed: %s", value.getValue()))
                        .subscribe(begin.asAction()));

        // can't use shared observable because of unexpected behaviour with skip call, so using ConnectableObservable
        ConnectableObservable<Integer> endChanged = RxBindingUtils
                .valueChanged(end)
                .doOnEach(value -> Timber.d("End value changed: %s", value.getValue()))
                .publish();

        monitor(
                endChanged
                        .map(minutes -> TimeUtils.minutesToTime(minutes))
                        .subscribe(endTime.asAction()));
        monitor(
                endChanged
                        .map(minutes -> minutes / interval)
                        .subscribe(rangeEnd.asAction()));
        monitor(
                endChanged
                        .skip(1)// skip initial value emitted automatically right after the
                        // subsription
                        .debounce(500, TimeUnit.MILLISECONDS)// such as seek bar may change the
                        // interval value very quickly use the
                        // debounce function for the timeout
                        // based processing
                        .doOnEach(value -> Timber.d("End value changed 2: %s", value.getValue()))
                        .subscribe(mSchedulerRangeEnd.asAction()));
        endChanged.connect();

        // link end and rangeEnd data binding fields to automatically adjust each other
        // when the value gets changed. RangeBar has an interval specified so the value should be transformed

        monitor(
                RxBindingUtils
                        .valueChanged(rangeEnd)
                        .map(v -> v * interval)
                        .doOnEach(value -> Timber.d("Range end value changed: %s", value.getValue()))
                        .filter(minutes -> Math.abs(minutes - end.get()) > interval) // user may enter value manually within interval by using time picker dialog,
                        // so ignore range bar changes in such case
                        .doOnEach(value -> Timber.d("Range end filtered value changed: %s", value.getValue()))
                        .subscribe(end.asAction()));
    }
}
