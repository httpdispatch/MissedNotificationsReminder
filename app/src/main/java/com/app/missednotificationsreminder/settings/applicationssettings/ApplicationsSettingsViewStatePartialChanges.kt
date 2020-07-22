package com.app.missednotificationsreminder.settings.applicationssettings

import com.app.missednotificationsreminder.binding.model.ViewStatePartialChanges

sealed class ApplicationsSettingsViewStatePartialChanges :
        ViewStatePartialChanges<ApplicationsSettingsViewState> {

    data class IgnorePersistentNotificationsChange(private val newValue: Boolean) : ApplicationsSettingsViewStatePartialChanges() {
        override fun reduce(previousState: ApplicationsSettingsViewState): ApplicationsSettingsViewState {
            return previousState.copy(ignorePersistentNotifications = newValue)
        }
    }

    data class RespectPhoneCallsChange(private val newValue: Boolean) : ApplicationsSettingsViewStatePartialChanges() {
        override fun reduce(previousState: ApplicationsSettingsViewState): ApplicationsSettingsViewState {
            return previousState.copy(respectPhoneCalls = newValue)
        }
    }

    data class RespectRingerModeChange(private val newValue: Boolean) : ApplicationsSettingsViewStatePartialChanges() {
        override fun reduce(previousState: ApplicationsSettingsViewState): ApplicationsSettingsViewState {
            return previousState.copy(respectRingerMode = newValue)
        }
    }

    data class RemindWhenScreenIsOnChange(private val newValue: Boolean) : ApplicationsSettingsViewStatePartialChanges() {
        override fun reduce(previousState: ApplicationsSettingsViewState): ApplicationsSettingsViewState {
            return previousState.copy(remindWhenScreenIsOn = newValue)
        }
    }

}