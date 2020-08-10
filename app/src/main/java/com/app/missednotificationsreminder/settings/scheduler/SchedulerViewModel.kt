package com.app.missednotificationsreminder.settings.scheduler

import androidx.lifecycle.viewModelScope
import com.app.missednotificationsreminder.binding.model.BaseViewStateModel
import com.app.missednotificationsreminder.binding.util.bindWithPreferences
import com.app.missednotificationsreminder.di.qualifiers.*
import com.app.missednotificationsreminder.util.coroutines.debounce
import com.tfcporciuncula.flow.Preference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * The view model for the scheduler configuration view
 */
@FlowPreview
@ExperimentalCoroutinesApi
class SchedulerViewModel @Inject constructor(
        /**
         * Preference to store/retrieve scheduler enabled information
         */
        @param:SchedulerEnabled private val schedulerEnabled: Preference<Boolean>,
        /**
         * Preference to store/retrieve scheduler mode information
         */
        @param:SchedulerMode private val schedulerMode: Preference<Boolean>,
        /**
         * Preference to store/retrieve scheduler range begin value
         */
        @param:SchedulerRangeBegin private val schedulerRangeBegin: Preference<Int>,
        /**
         * Preference to store/retrieve scheduler range end value
         */
        @param:SchedulerRangeEnd private val schedulerRangeEnd: Preference<Int>,
        /**
         * The minimum possible value information for scheduler minutes
         */
        @param:SchedulerRangeMin var minimum: Int,
        /**
         * The maximum possible value information for scheduler minutes
         */
        @param:SchedulerRangeMax var maximum: Int) :
        BaseViewStateModel<SchedulerViewState, SchedulerViewStatePartialChanges>(SchedulerViewState(
                minimum = minimum,
                maximum = maximum)) {

    init {
        viewModelScope.launch {
            launch {
                _viewState.bindWithPreferences(schedulerEnabled,
                        { newValue, vs ->
                            SchedulerViewStatePartialChanges.EnabledChange(newValue).reduce(vs)
                        },
                        { it.enabled })
            }
            launch {
                _viewState.bindWithPreferences(schedulerMode,
                        { newValue, vs ->
                            SchedulerViewStatePartialChanges.ModeChange(newValue).reduce(vs)
                        },
                        { it.mode })
            }
            launch {
                _viewState.bindWithPreferences(schedulerRangeBegin,
                        { newValue, vs ->
                            SchedulerViewStatePartialChanges.BeginChange(newValue).reduce(vs)
                        },
                        { it.begin })
            }
            launch {
                _viewState.bindWithPreferences(schedulerRangeEnd,
                        { newValue, vs ->
                            SchedulerViewStatePartialChanges.EndChange(newValue).reduce(vs)
                        },
                        { it.end })
            }
        }
    }

    fun enabledChanged(value: Boolean) {
        process(SchedulerViewStatePartialChanges.EnabledChange(value))
    }

    fun modeChanged(value: Boolean) {
        process(SchedulerViewStatePartialChanges.ModeChange(value))
    }

    fun onRangeChanged(left: Int, right: Int, fromUser: Boolean) {
        rangeChangedDebounce(Triple(left, right, fromUser))
    }

    private val rangeChangedDebounce: (Triple<Int, Int, Boolean>) -> Unit = debounce(
            50L,
            viewModelScope) { (left, right, fromUser) ->
        if (fromUser) {
            process(SchedulerViewStatePartialChanges.BeginChange(left))
            process(SchedulerViewStatePartialChanges.EndChange(right))
        } else {
            Timber.d("onRangeChanged: auto change $left $right")
        }
    }
}