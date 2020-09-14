package com.app.missednotificationsreminder.util.loadingstate

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

abstract class LoadingStateManager() : ManagesLoadingState {
    /**
     * The pending not processed items count
     */
    private var pendingLoadingItemsCount: Int = 0

    /**
     * The pending loading statuses
     */
    private val loadingStatuses = mutableListOf<String>()

    /**
     * The data update mutex
     */
    private val mutex = Mutex()

    /**
     * The loading state holder
     */
    protected abstract var loadingState: LoadingState

    /**
     * Decrement loading counter
     *
     * @param operationName the loading operation name
     */
    private suspend fun decrementLoading(operationName: String) {
        Timber.d("decrementLoading(): operation = %s",
                operationName)
        mutex.withLock {
            pendingLoadingItemsCount--
            if (pendingLoadingItemsCount == 0) {
                // if there are no more pending operations update loading state
                loadingState = loadingState.copy(loading = false)
            }
        }
    }

    /**
     * Increment loading counter
     *
     * @param operationName the loading operation name
     */
    private suspend fun incrementLoading(operationName: String) {
        Timber.d("incrementLoading(): operation = %s",
                operationName)
        mutex.withLock {
            if (pendingLoadingItemsCount == 0) {
                loadingState = loadingState.copy(loading = true)
            }
            pendingLoadingItemsCount++
        }
    }

    override suspend fun <T> attachLoading(operationName: String, block: suspend () -> T): T {
        try {
            incrementLoading(operationName)
            return block()
        } finally {
            decrementLoading(operationName)
        }
    }

    override suspend fun <T> attachLoadingStatus(status: String, block: suspend () -> T): T {
        try {
            setStatusKeepStack(status)
            return block()
        } finally {
            removeStatus(status)
        }
    }

    private suspend fun setStatusKeepStack(status: String) {
        mutex.withLock {
            loadingStatuses.add(status)
            loadingState = loadingState.copy(status = status)
        }
    }

    private suspend fun removeStatus(status: String) {
        mutex.withLock {
            for (i in loadingStatuses.indices) {
                if (status == loadingStatuses[i]) {
                    loadingStatuses.removeAt(i)
                    loadingState = loadingState.copy(status = if (i == 0 && loadingStatuses.isNotEmpty()) {
                        loadingStatuses.last()
                    } else {
                        ""
                    })
                    break
                }
            }
        }
    }
}