package com.app.missednotificationsreminder.settings.applicationselection

import com.app.missednotificationsreminder.binding.model.ViewStatePartialChanges

sealed class ApplicationItemViewStatePartialChanges : ViewStatePartialChanges<ApplicationItemViewState> {
    data class CheckedStateChange(private val newValue: Boolean) : ApplicationItemViewStatePartialChanges() {
        override fun reduce(previousState: ApplicationItemViewState): ApplicationItemViewState {
            return previousState.copy(checked = newValue)
        }
    }
}