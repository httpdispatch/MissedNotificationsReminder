package com.app.missednotificationsreminder.settings.vibration

import com.app.missednotificationsreminder.binding.model.ViewStatePartialChanges

sealed class VibrationViewStatePartialChanges : ViewStatePartialChanges<VibrationViewState> {

    data class EnabledChange(private val newValue: Boolean) : VibrationViewStatePartialChanges() {
        override fun reduce(previousState: VibrationViewState): VibrationViewState {
            return previousState.copy(enabled = newValue)
        }
    }

    data class PatternChange(private val newValue: String, private val vibrationPatternError: String) : VibrationViewStatePartialChanges() {

        private val vibrationRegexpPattern = "\\s*\\d+(\\s*,\\s*\\d+)*\\s*".toRegex()

        override fun reduce(previousState: VibrationViewState): VibrationViewState {
            val validVibrationPattern = newValue.matches(vibrationRegexpPattern)
            val filteredPattern = newValue
                    .takeIf { validVibrationPattern }
                    ?.replaceFirst("^\\s+".toRegex(), "")?.replaceFirst("\\s+$".toRegex(), "")
                    ?: newValue
            return previousState.copy(
                    pattern = filteredPattern,
                    lastValidPattern = if (validVibrationPattern) filteredPattern else previousState.lastValidPattern,
                    patternError = if (validVibrationPattern)
                        ""
                    else
                        vibrationPatternError)
        }
    }
}