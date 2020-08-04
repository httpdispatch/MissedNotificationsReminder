package com.app.missednotificationsreminder.settings.reminder

import androidx.lifecycle.viewModelScope
import com.app.missednotificationsreminder.binding.model.BaseViewStateModel
import com.app.missednotificationsreminder.binding.util.bindWithPreferences
import com.app.missednotificationsreminder.di.qualifiers.*
import com.app.missednotificationsreminder.util.TimeUtils
import com.app.missednotificationsreminder.util.coroutines.debounce
import com.tfcporciuncula.flow.Preference
import hu.akarnokd.kotlin.flow.publish
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * The view model for the interval configuration view
 *
 * @param reminderEnabled  preference to store/retrieve enabled information
 * @param reminderInterval preference to store/retrieve reminder interval value
 * @param reminderRepeats  preference to store/retrieve number of reminder repetitions
 * @param maxInterval      the maximum allowed reminder interval value
 * @param minInterval      the minimum allowed reminder interval value
 */
@ExperimentalCoroutinesApi
@FlowPreview
class ReminderViewModel @Inject constructor(
        @param:ReminderEnabled private val reminderEnabled: Preference<Boolean>,
        @param:ForceWakeLock private val forceWakeLock: Preference<Boolean>,
        @param:LimitReminderRepeats private val limitReminderRepeats: Preference<Boolean>,
        @param:CreateDismissNotification private val createDismissNotification: Preference<Boolean>,
        @param:CreateDismissNotificationImmediately private val createDismissNotificationImmediately: Preference<Boolean>,
        @param:ReminderInterval private val reminderInterval: Preference<Int>,
        @param:ReminderRepeats private val reminderRepeats: Preference<Int>,
        @param:ReminderIntervalMax val maxInterval: Int,
        @param:ReminderIntervalMin val minInterval: Int,
        @param:ReminderRepeatsMax val maxRepeats: Int,
        @param:ReminderRepeatsMin val minRepeats: Int
) : BaseViewStateModel<ReminderViewState, ReminderViewStatePartialChanges>(ReminderViewState(
        minIntervalSeconds = minInterval,
        maxIntervalSeconds = maxInterval,
        minRepeats = minRepeats,
        maxRepeats = maxRepeats,
        maxIntervalSeekBarValue = TimeUtils.secondsToMinutes(maxInterval - minInterval, TimeUtils.RoundType.CEIL).toInt() + preciseIntervalSeekBarValues)) {

    companion object {
        /**
         * The precise interval seekbar values (values with better accuracy)
         */
        const val preciseIntervalSeekBarValues = 3

        /**
         * The maximum value in seconds below which the precise configuration can be used
         */
        const val preciseMaxValueSeconds = TimeUtils.SECONDS_IN_MINUTE

        /**
         * The maximum value in minutes below which the precise configuration can be used
         */
        val preciseMaxValueMinutes = TimeUtils.secondsToMinutes(preciseMaxValueSeconds).toInt()
    }


    init {
        viewModelScope.launch {
            launch {
                _viewState.bindWithPreferences(reminderEnabled,
                        { newValue, vs ->
                            vs.copy(reminderEnabled = newValue)
                        },
                        { it.reminderEnabled })
            }
            launch {
                _viewState.bindWithPreferences(forceWakeLock,
                        { newValue, vs ->
                            vs.copy(forceWakeLock = newValue)
                        },
                        { it.forceWakeLock })
            }
            // workaround for updated interval measurements
            if (reminderInterval.get() < minInterval) {
                reminderInterval.set(TimeUtils.minutesToSeconds(reminderInterval.get().toDouble()))
            }
            launch {
                _viewState.bindWithPreferences(reminderInterval,
                        { newValue, vs ->
                            ReminderViewStatePartialChanges.IntervalChange(newValue).reduce(vs)
                        },
                        {
                            it.intervalSeconds
                        })
            }
            launch {
                _viewState.bindWithPreferences(reminderRepeats,
                        { newValue, vs ->
                            ReminderViewStatePartialChanges.RepeatsChange(newValue).reduce(vs)
                        },
                        { it.repeats })
            }
            launch {
                _viewState.bindWithPreferences(limitReminderRepeats,
                        { newValue, vs ->
                            vs.copy(limitReminderRepeats = newValue)
                        },
                        { it.limitReminderRepeats })
            }
            launch {
                _viewState.bindWithPreferences(createDismissNotification,
                        { newValue, vs ->
                            vs.copy(createDismissNotification = newValue)
                        },
                        { it.createDismissNotification })
            }
            launch {
                _viewState.bindWithPreferences(createDismissNotificationImmediately,
                        { newValue, vs ->
                            vs.copy(createDismissNotificationImmediately = newValue)
                        },
                        { it.createDismissNotificationImmediately })
            }
            // Interval changed flow
            val intervalChanged = viewState
                    .map { it.intervalSeconds }
                    .distinctUntilChanged()
                    .onEach { Timber.d("Interval value changed: $it") }
                    .publish { it }
            // do not allow interval to exceed min and max limits
            launch {
                intervalChanged
                        .filter { it < minInterval }
                        .map {
                            Timber.d("Interval reset to min")
                            minInterval
                        }
                        .debounce(1000)
                        // the value could be changed again within that one millisecond delay
                        .filter { viewState.value.intervalSeconds < minInterval }
                        .collect { process(ReminderViewStatePartialChanges.IntervalChange(it)) }
            }
            launch {
                intervalChanged
                        .filter { it > maxInterval }
                        .map {
                            Timber.d("Interval reset to max")
                            maxInterval
                        }
                        .debounce(1000)
                        // the value could be changed again within that one millisecond delay
                        .filter { viewState.value.intervalSeconds > maxInterval }
                        .collect { process(ReminderViewStatePartialChanges.IntervalChange(it)) }
            }
            // Prepare repeats changed flow to be able to add all the rules and activate them
            // together with a call to .connect below.
            val repeatsChanged = viewState
                    .map { it.repeats }
                    .distinctUntilChanged()
                    .onEach { Timber.d("Repeats value changed: $it") }
                    .publish { it }
            // do not allow repeats to exceed min and max limits
            launch {
                repeatsChanged
                        .filter { it < minRepeats }
                        .map {
                            Timber.d("Repeats reset to min")
                            minRepeats
                        }
                        .debounce(1000)
                        // the value could be changed again within that one millisecond delay
                        .filter { viewState.value.repeats < minRepeats }
                        .collect { process(ReminderViewStatePartialChanges.RepeatsChange(it)) }
            }
            launch {
                repeatsChanged
                        .filter { it > maxRepeats }
                        .map {
                            Timber.d("Repeats reset to max")
                            maxRepeats
                        }
                        .debounce(1000)
                        // the value could be changed again within that one millisecond delay
                        .filter { viewState.value.repeats > maxRepeats }
                        .collect { process(ReminderViewStatePartialChanges.RepeatsChange(it)) }
            }
        }
    }

    fun reminderEnabledChanged(value: Boolean) {
        process(ReminderViewStatePartialChanges.ReminderEnabledChange(value))
    }

    fun seekIntervalChanged(progress: Int, fromUser: Boolean) {
        seekIntervalChangedDebounce(Pair(progress, fromUser))
    }

    private val seekIntervalChangedDebounce: (Pair<Int, Boolean>) -> Unit = debounce(
            50L,
            viewModelScope) { (progress, fromUser) ->
        if (fromUser) {
            process(ReminderViewStatePartialChanges.SeekIntervalChange(progress))
        } else {
            Timber.d("seekIntervalChanged: auto change $progress")
        }
    }

    fun intervalMinutesChanged(text: String) {
        intervalMinutesChangedDebounce(text)
    }

    private val intervalMinutesChangedDebounce: (String) -> Unit = debounce(
            1000L,
            viewModelScope) { value ->
        value.toDoubleOrNull()
                ?.let { process(ReminderViewStatePartialChanges.IntervalChange(TimeUtils.minutesToSeconds(it))) }
                ?: run {
                    process(ReminderViewStatePartialChanges.ForceUpdate)
                }
    }

    fun limitReminderRepeatsChanged(value: Boolean) {
        process(ReminderViewStatePartialChanges.LimitReminderRepeatsChange(value))
    }

    fun repeatsChanged(text: String) {
        repeatsChangedDebounce(text)
    }

    private val repeatsChangedDebounce: (String) -> Unit = debounce(
            1000L,
            viewModelScope)
    { value ->
        value.toIntOrNull()
                ?.let { process(ReminderViewStatePartialChanges.RepeatsChange(it)) }
                ?: run {
                    process(ReminderViewStatePartialChanges.ForceUpdate)
                }
    }

    fun seekRepeatsChanged(progress: Int, fromUser: Boolean) {
        seekRepeatsChangedDebounce(Pair(progress, fromUser))
    }

    private val seekRepeatsChangedDebounce: (Pair<Int, Boolean>) -> Unit = debounce(
            50L,
            viewModelScope) { (progress, fromUser) ->
        if (fromUser) {
            process(ReminderViewStatePartialChanges.SeekRepeatsChange(progress))
        } else {
            Timber.d("seekRepeatsChanged: auto change $progress")
        }
    }

    fun createDismissNotificationChanged(value: Boolean) {
        process(ReminderViewStatePartialChanges.CreateDismissNotificationChange(value))
    }

    fun createDismissNotificationImmediatelyChanged(value: Boolean) {
        process(ReminderViewStatePartialChanges.CreateDismissNotificationImmediatelyChange(value))
    }

    fun forceWakeLockChanged(value: Boolean) {
        process(ReminderViewStatePartialChanges.ForceWakeLockChange(value))
    }
}