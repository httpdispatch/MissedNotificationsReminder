package com.app.missednotificationsreminder.settings.reminder

import com.app.missednotificationsreminder.binding.model.ViewStatePartialChanges
import com.app.missednotificationsreminder.util.TimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import timber.log.Timber
import kotlin.math.abs

sealed class ReminderViewStatePartialChanges : ViewStatePartialChanges<ReminderViewState> {
    //
    object ForceUpdate : ReminderViewStatePartialChanges() {
        override fun reduce(previousState: ReminderViewState): ReminderViewState {
            return previousState.copy(forceUpdate = previousState.forceUpdate + 1)
        }
    }

    data class ReminderEnabledChange(private val newValue: Boolean) : ReminderViewStatePartialChanges() {
        override fun reduce(previousState: ReminderViewState): ReminderViewState {
            return previousState.copy(reminderEnabled = newValue)
        }
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    data class IntervalChange(private val newValue: Int) : ReminderViewStatePartialChanges() {

        override fun reduce(previousState: ReminderViewState): ReminderViewState {
            if (previousState.intervalSeconds == newValue) {
                return previousState
            }
            val correctedNewValue = when {
                newValue < previousState.minIntervalSeconds -> {
                    Timber.d("Interval reset to min")
                    previousState.minIntervalSeconds
                }
                newValue > previousState.maxIntervalSeconds -> {
                    Timber.d("Interval reset to max")
                    previousState.maxIntervalSeconds
                }
                else -> {
                    newValue
                }
            }
            val intervalMinutes = TimeUtils.secondsToMinutes(correctedNewValue)
            return previousState.copy(
                    intervalSeconds = correctedNewValue,
                    intervalMinutes = intervalMinutes,
                    forceUpdate = previousState.forceUpdate + 1,
                    forceWakeLock = when {
                        (correctedNewValue > previousState.maxIntervalForWakeLock) -> false
                        else -> true
                    })
                    .let { updatedState ->
                        correctedNewValue
                                .let {
                                    (if (it >= ReminderViewModel.preciseMaxValueSeconds)
                                        intervalMinutes + ReminderViewModel.preciseIntervalSeekBarValues
                                    else
                                        intervalMinutes * (ReminderViewModel.preciseIntervalSeekBarValues + 1)).toInt()
                                }
                                .let { value: Int -> if (value == 0) 0 else value - 1 }
                                .let { SeekIntervalChange(it).reduce(updatedState) }
                    }
        }
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    data class SeekIntervalChange(private val newValue: Int) : ReminderViewStatePartialChanges() {

        override fun reduce(previousState: ReminderViewState): ReminderViewState {
            if (previousState.seekInterval == newValue) {
                return previousState
            }
            return previousState.copy(seekInterval = newValue)
                    .let { updatedState ->
                        (newValue + 1)
                                .let { value ->
                                    if (value > ReminderViewModel.preciseIntervalSeekBarValues)
                                        (value - ReminderViewModel.preciseIntervalSeekBarValues).toDouble()
                                    else
                                        value.toDouble() / (ReminderViewModel.preciseIntervalSeekBarValues + 1)
                                }
                                .takeIf { value ->
                                    (value >= ReminderViewModel.preciseMaxValueMinutes && abs(value - previousState.intervalMinutes) >= 1 ||
                                            value < ReminderViewModel.preciseMaxValueMinutes &&
                                            abs(value - previousState.intervalMinutes) >=
                                            ReminderViewModel.preciseMaxValueMinutes.toDouble() / (ReminderViewModel.preciseIntervalSeekBarValues + 1))
                                }
                                ?.let {
                                    IntervalChange(TimeUtils.minutesToSeconds(it)).reduce(updatedState)
                                }
                                ?: updatedState.also {
                                    Timber.d("Ignore interval update")
                                }
                    }
        }
    }

    data class RepeatsChange(private val newValue: Int) : ReminderViewStatePartialChanges() {

        override fun reduce(previousState: ReminderViewState): ReminderViewState {
            if (previousState.repeats == newValue) {
                return previousState
            }
            val correctedNewValue = when {
                newValue < previousState.minRepeats -> {
                    Timber.d("Repeats reset to min")
                    previousState.minRepeats
                }
                newValue > previousState.maxRepeats -> {
                    Timber.d("Repeats reset to max")
                    previousState.maxRepeats
                }
                else -> {
                    newValue
                }
            }
            return previousState.copy(
                    repeats = correctedNewValue,
                    forceUpdate = previousState.forceUpdate + 1)
                    .let { updatedState ->
                        correctedNewValue
                                // Text field shows an actual number of reminder repetitions, but SeekBar
                                // can only take a value in [0, max] range, therefore we need to map values
                                // from [minRepeats, maxRepeats] range into [0, maxRepeats-minRepeats] range
                                // used by SeekBar.
                                .let { value: Int -> value - previousState.minRepeats }
                                .let { SeekRepeatsChange(it).reduce(updatedState) }
                    }
        }
    }

    data class SeekRepeatsChange(private val newValue: Int) : ReminderViewStatePartialChanges() {

        override fun reduce(previousState: ReminderViewState): ReminderViewState {
            if (previousState.seekRepeats == newValue) {
                return previousState
            }
            return previousState.copy(
                    seekRepeats = newValue)
                    .let { updatedState ->
                        newValue
                                // SeekBar can only take values in [0, max] range, therefore we need to
                                // convert reported value from [0, maxRepeats-minRepeats] range to
                                // [minRepeats, maxRepeats] range before showing it in the text field.
                                .let { value: Int -> value + previousState.minRepeats }
                                // This filtering is not strictly necessary since SeekBar has limits on
                                // values that it can take, but I've decided to add them anyway to err on
                                // the safe side.
                                .takeIf { it in previousState.minRepeats..previousState.maxRepeats }
                                ?.let { RepeatsChange(it).reduce(updatedState) }
                                ?: updatedState
                    }
        }
    }

    data class LimitReminderRepeatsChange(private val newValue: Boolean) : ReminderViewStatePartialChanges() {
        override fun reduce(previousState: ReminderViewState): ReminderViewState {
            return previousState.copy(limitReminderRepeats = newValue)
        }
    }

    data class CreateDismissNotificationChange(private val newValue: Boolean) : ReminderViewStatePartialChanges() {
        override fun reduce(previousState: ReminderViewState): ReminderViewState {
            return previousState.copy(createDismissNotification = newValue)
        }
    }

    data class CreateDismissNotificationImmediatelyChange(private val newValue: Boolean) : ReminderViewStatePartialChanges() {
        override fun reduce(previousState: ReminderViewState): ReminderViewState {
            return previousState.copy(createDismissNotificationImmediately = newValue)
        }
    }

    data class ForceWakeLockChange(private val newValue: Boolean) : ReminderViewStatePartialChanges() {
        override fun reduce(previousState: ReminderViewState): ReminderViewState {
            return previousState.copy(forceWakeLock = newValue)
        }
    }
}