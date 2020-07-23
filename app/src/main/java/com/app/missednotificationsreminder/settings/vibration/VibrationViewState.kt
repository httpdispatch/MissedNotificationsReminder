package com.app.missednotificationsreminder.settings.vibration

/**
 * @property enabled used to handle vibration enabled state
 * @property pattern used to handle vibration pattern
 * @property lastValidPattern used to store last valid pattern
 * @property patternError used to handle pattern error information
 */
data class VibrationViewState(
        val enabled: Boolean = false,
        val pattern: String = "",
        val lastValidPattern: String = "",
        val patternError: String = "")