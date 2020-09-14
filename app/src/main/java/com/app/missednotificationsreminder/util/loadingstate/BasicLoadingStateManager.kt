package com.app.missednotificationsreminder.util.loadingstate

class BasicLoadingStateManager() : LoadingStateManager() {
    @Volatile
    override var loadingState: LoadingState = LoadingState()
}