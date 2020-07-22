package com.app.missednotificationsreminder.binding.model

interface ViewStatePartialChanges<VIEW_STATE> {
    fun reduce(previousState: VIEW_STATE): VIEW_STATE
}