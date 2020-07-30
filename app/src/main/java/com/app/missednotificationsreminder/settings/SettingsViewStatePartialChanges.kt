package com.app.missednotificationsreminder.settings

import com.app.missednotificationsreminder.binding.model.ViewStatePartialChanges

sealed class SettingsViewStatePartialChanges : ViewStatePartialChanges<SettingsViewState> {

    data class AccessEnabledChange(private val newValue: Boolean) : SettingsViewStatePartialChanges() {
        override fun reduce(previousState: SettingsViewState): SettingsViewState {
            return previousState.copy(accessEnabled = newValue,
                    accessInitialized = true)
        }
    }

    data class BatteryOptimizationDisabledChanged(private val newValue: Boolean) : SettingsViewStatePartialChanges() {
        override fun reduce(previousState: SettingsViewState): SettingsViewState {
            return previousState.copy(batteryOptimizationDisabled = newValue)
        }
    }

    data class MissingPermissionsChanged(private val newValue: String) : SettingsViewStatePartialChanges() {
        override fun reduce(previousState: SettingsViewState): SettingsViewState {
            return previousState.copy(missingPermissions = newValue)
        }
    }

    data class VibrationSettingsAvailableChanged(private val newValue: Boolean) : SettingsViewStatePartialChanges() {
        override fun reduce(previousState: SettingsViewState): SettingsViewState {
            return previousState.copy(vibrationSettingsAvailable = newValue)
        }
    }

    data class AdvancedSettingsVisibleChanged(private val newValue: Boolean) : SettingsViewStatePartialChanges() {
        override fun reduce(previousState: SettingsViewState): SettingsViewState {
            return previousState.copy(advancedSettingsVisible = newValue)
        }
    }
}