package com.app.missednotificationsreminder.settings.vibration

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.viewModelScope
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.binding.model.BaseViewStateModel
import com.app.missednotificationsreminder.binding.util.bindWithPreferences
import com.app.missednotificationsreminder.di.qualifiers.ForApplication
import com.app.missednotificationsreminder.di.qualifiers.Vibrate
import com.app.missednotificationsreminder.di.qualifiers.VibrationPattern
import com.app.missednotificationsreminder.service.ReminderNotificationListenerService
import com.app.missednotificationsreminder.util.coroutines.debounce
import com.tfcporciuncula.flow.Preference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The view model for the vibration configuration view
 */
@ExperimentalCoroutinesApi
@FlowPreview
class VibrationViewModel @Inject constructor(
        @param:Vibrate private val vibrationEnabled: Preference<Boolean>,
        @param:VibrationPattern private val vibrationPattern: Preference<String>,
        private val vibrator: Vibrator,
        @param:ForApplication private val context: Context) :
        BaseViewStateModel<VibrationViewState, VibrationViewStatePartialChanges>(VibrationViewState()) {


    private fun vibrate() {
        val pattern = ReminderNotificationListenerService.parseVibrationPattern(viewState.value.lastValidPattern)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    init {
        viewModelScope.launch {
            launch {
                _viewState.bindWithPreferences(vibrationEnabled,
                        { newValue, vs ->
                            vs.copy(enabled = newValue)
                        },
                        { it.enabled })
            }
            launch {
                _viewState.bindWithPreferences(vibrationPattern,
                        { newValue, vs ->
                            vs.copy(pattern = newValue,
                                    lastValidPattern = newValue)
                        },
                        { if (it.patternError.isEmpty()) it.pattern else it.lastValidPattern })
            }
        }
        viewState
                .map { it.enabled }
                .distinctUntilChanged()
                .drop(1)
                .filter { it }
                .onEach { vibrate() }
                .launchIn(viewModelScope)
    }

    fun enabledChanged(value: Boolean) {
        process(VibrationViewStatePartialChanges.EnabledChange(value))
    }

    fun patternChanged(text: String) {
        patternChangedDebounce(text)
    }

    private val patternChangedDebounce: (String) -> Unit = debounce(
            1000L,
            viewModelScope) { value ->
        process(VibrationViewStatePartialChanges.PatternChange(value, context.getString(R.string.vibration_pattern_error)))
    }
}