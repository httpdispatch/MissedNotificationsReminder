package com.app.missednotificationsreminder.util.loadingstate

interface ManagesLoadingState {
    /**
     * Attach loading state update logic to the [block] operation
     */
    suspend fun <T> attachLoading(operationName: String, block: suspend () -> T): T

    /**
     * Attach loading status update logic to the [block] operation
     */
    suspend fun <T> attachLoadingStatus(status: String, block: suspend () -> T): T
}