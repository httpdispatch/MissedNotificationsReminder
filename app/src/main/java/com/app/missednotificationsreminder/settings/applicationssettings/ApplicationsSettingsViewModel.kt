package com.app.missednotificationsreminder.settings.applicationssettings

import androidx.lifecycle.viewModelScope
import com.app.missednotificationsreminder.binding.model.BaseViewStateModel
import com.app.missednotificationsreminder.binding.util.bindWithPreferences
import com.app.missednotificationsreminder.settings.di.qualifiers.IgnorePersistentNotifications
import com.app.missednotificationsreminder.settings.di.qualifiers.RemindWhenScreenIsOn
import com.app.missednotificationsreminder.settings.di.qualifiers.RespectPhoneCalls
import com.app.missednotificationsreminder.settings.di.qualifiers.RespectRingerMode
import com.tfcporciuncula.flow.Preference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
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
        @param:RemindWhenScreenIsOn var remindWhenScreenIsOn: Preference<Boolean>) :
        BaseViewStateModel<ApplicationsSettingsViewState, ApplicationsSettingsViewStatePartialChanges>(ApplicationsSettingsViewState(
                ignorePersistentNotifications = false,
                respectPhoneCalls = false,
                respectRingerMode = false,
                remindWhenScreenIsOn = false)) {

    /**
     * Perform additional model initialization
     */
    fun init() {
        viewModelScope.launch {
            launch {
                _viewState.bindWithPreferences(ignorePersistentNotifications,
                        { newValue, vs ->
                            ApplicationsSettingsViewStatePartialChanges.IgnorePersistentNotificationsChange(newValue).reduce(vs)
                        },
                        { it.ignorePersistentNotifications })
            }
            launch {
                _viewState.bindWithPreferences(respectPhoneCalls,
                        { newValue, vs ->
                            ApplicationsSettingsViewStatePartialChanges.RespectPhoneCallsChange(newValue).reduce(vs)
                        },
                        { it.respectPhoneCalls })
            }
            launch {
                _viewState.bindWithPreferences(respectRingerMode,
                        { newValue, vs ->
                            ApplicationsSettingsViewStatePartialChanges.RespectRingerModeChange(newValue).reduce(vs)
                        },
                        { it.respectRingerMode })
            }
            launch {
                _viewState.bindWithPreferences(remindWhenScreenIsOn,
                        { newValue, vs ->
                            ApplicationsSettingsViewStatePartialChanges.RemindWhenScreenIsOnChange(newValue).reduce(vs)
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
}