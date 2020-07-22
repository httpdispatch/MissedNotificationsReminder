package com.app.missednotificationsreminder.settings.sound

import android.net.Uri
import com.app.missednotificationsreminder.binding.model.ViewStatePartialChanges

sealed class SoundViewStatePartialChanges : ViewStatePartialChanges<SoundViewState> {

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