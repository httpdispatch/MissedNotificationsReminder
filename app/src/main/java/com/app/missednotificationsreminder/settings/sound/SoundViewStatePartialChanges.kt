package com.app.missednotificationsreminder.settings.sound

import android.net.Uri

sealed class SoundViewStatePartialChanges {
    abstract fun reduce(previousState: SoundViewState): SoundViewState

    data class RingtoneChange(private val newValue: Uri) : SoundViewStatePartialChanges() {
        override fun reduce(previousState: SoundViewState): SoundViewState {
            return previousState.copy(ringtone = newValue.toString())
        }
    }

    data class RingtoneTitleChange(private val newValue: String) : SoundViewStatePartialChanges() {
        override fun reduce(previousState: SoundViewState): SoundViewState {
            return previousState.copy(ringtoneName = newValue)
        }
    }
}