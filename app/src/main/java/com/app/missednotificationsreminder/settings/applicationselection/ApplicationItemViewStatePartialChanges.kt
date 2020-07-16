package com.app.missednotificationsreminder.settings.applicationselection

sealed class ApplicationItemViewStatePartialChanges {
    abstract fun reduce(previousState: ApplicationItemViewState): ApplicationItemViewState

    data class CheckedStateChange(private val newValue: Boolean) : ApplicationItemViewStatePartialChanges() {
        override fun reduce(previousState: ApplicationItemViewState): ApplicationItemViewState {
            return previousState.copy(checked  = newValue)
        }
    }

}