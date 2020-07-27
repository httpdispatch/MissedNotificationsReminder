package com.app.missednotificationsreminder.settings.scheduler

import com.app.missednotificationsreminder.binding.model.ViewStatePartialChanges

sealed class SchedulerViewStatePartialChanges : ViewStatePartialChanges<SchedulerViewState> {

    data class EnabledChange(private val newValue: Boolean) : SchedulerViewStatePartialChanges() {
        override fun reduce(previousState: SchedulerViewState): SchedulerViewState {
            return previousState.copy(enabled = newValue)
        }
    }

    data class ModeChange(private val newValue: Boolean) : SchedulerViewStatePartialChanges() {
        override fun reduce(previousState: SchedulerViewState): SchedulerViewState {
            return previousState.copy(mode = newValue)
        }
    }

    data class BeginChange(private val newValue: Int) : SchedulerViewStatePartialChanges() {
        override fun reduce(previousState: SchedulerViewState): SchedulerViewState {
            if (newValue == previousState.begin) {
                return previousState
            }
            return previousState.copy(begin = newValue)
        }
    }

    data class EndChange(private val newValue: Int) : SchedulerViewStatePartialChanges() {
        override fun reduce(previousState: SchedulerViewState): SchedulerViewState {
            if (newValue == previousState.end) {
                return previousState
            }
            return previousState.copy(end = newValue)
        }
    }

}