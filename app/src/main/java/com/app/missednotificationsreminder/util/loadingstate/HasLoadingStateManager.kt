package com.app.missednotificationsreminder.util.loadingstate

interface HasLoadingStateManager : ManagesLoadingState {
    val loadingStateManager: LoadingStateManager

    override suspend fun <T> attachLoading(operationName: String, block: suspend () -> T): T {
        return loadingStateManager.attachLoading("$operationName:${this::class.simpleName}", block)
    }

    override suspend fun <T> attachLoadingStatus(status: String, block: suspend () -> T): T {
        return loadingStateManager.attachLoadingStatus(status, block)
    }
}