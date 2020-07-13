package com.app.missednotificationsreminder.settings.applicationssettings

import androidx.lifecycle.viewModelScope
import com.app.missednotificationsreminder.binding.model.BaseViewModel
import com.app.missednotificationsreminder.binding.util.bindWithPreferences
import com.app.missednotificationsreminder.di.qualifiers.IgnorePersistentNotifications
import com.app.missednotificationsreminder.di.qualifiers.RemindWhenScreenIsOn
import com.app.missednotificationsreminder.di.qualifiers.RespectPhoneCalls
import com.app.missednotificationsreminder.di.qualifiers.RespectRingerMode
import com.f2prateek.rx.preferences.Preference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * The view model for the application settings
 * @property ignorePersistentNotifications The ignore persistent notifications preference
 * @property respectPhoneCalls The respect phone calls preference
 * @property respectRingerMode The respect ringer mode preference
 * @property remindWhenScreenIsOn The remind when screen is on preference
 */
@FlowPreview
@ExperimentalCoroutinesApi
class ApplicationsSettingsViewModel @Inject constructor(
        @param:IgnorePersistentNotifications var ignorePersistentNotifications: Preference<Boolean>,
        @param:RespectPhoneCalls var respectPhoneCalls: Preference<Boolean>,
        @param:RespectRingerMode var respectRingerMode: Preference<Boolean>,
        @param:RemindWhenScreenIsOn var remindWhenScreenIsOn: Preference<Boolean>) : BaseViewModel() {
    private val _viewState = MutableStateFlow(ApplicationsSettingsViewState(
            ignorePersistentNotifications = false,
            respectPhoneCalls = false,
            respectRingerMode = false,
            remindWhenScreenIsOn = false))

    val viewState: StateFlow<ApplicationsSettingsViewState> = _viewState

    /**
     * Perform additional model initialization
     */
    fun init() {
        viewModelScope.launch {
            launch {
                _viewState.bindWithPreferences(ignorePersistentNotifications,
                        { newValue, vs ->
                            vs.copy(ignorePersistentNotifications = newValue)
                        },
                        { it.ignorePersistentNotifications })
            }
            launch {
                _viewState.bindWithPreferences(respectPhoneCalls,
                        { newValue, vs ->
                            vs.copy(respectPhoneCalls = newValue)
                        },
                        { it.respectPhoneCalls })
            }
            launch {
                _viewState.bindWithPreferences(respectRingerMode,
                        { newValue, vs ->
                            vs.copy(respectRingerMode = newValue)
                        },
                        { it.respectRingerMode })
            }
            launch {
                _viewState.bindWithPreferences(remindWhenScreenIsOn,
                        { newValue, vs ->
                            vs.copy(remindWhenScreenIsOn = newValue)
                        },
                        { it.remindWhenScreenIsOn })
            }
            launch {
                _viewState.collect { Timber.d("ViewState updated $it") }
            }
        }
    }

    init {
        init()
    }

    fun process(event: ApplicationsSettingsViewStatePartialChanges) {
        Timber.d("process() called: with event=${event}")
        _viewState.value = event.reduce(_viewState.value)
    }
}