package com.app.missednotificationsreminder.util.coroutines

import com.app.missednotificationsreminder.data.ResultWrapper
import com.app.missednotificationsreminder.data.asResultWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

fun <T> debounce(
        waitMs: Long,
        coroutineScope: CoroutineScope,
        destinationFunction: (T) -> Unit
): (T) -> Unit {
    var debounceJob: Job? = null
    return { param: T ->
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(waitMs)
            destinationFunction(param)
        }
    }
}

/**
 * Retry call in case of error until [maxRetryCount] is reached. In addition log some verbose information
 */
suspend fun <T> retryCallOnError(maxRetryCount: Int, block: suspend () -> T): ResultWrapper<T> {
    require(maxRetryCount >= 0)
    var result: Result<T>? = null
    for (count in 0..maxRetryCount) {
        if (count > 0) {
            Timber.d("retryCallOnError: retry")
        }
        result = runCatching { block() }
        if (result.isFailure) {
            Timber.e(result.exceptionOrNull(), "retryCallOnError")
            Timber.d("retryCallOnError: count=%d; maxRetryCount=%d", count, maxRetryCount)
        } else {
            break
        }
    }
    require(result != null)
    if (result.isFailure) {
        Timber.d("retryCallOnError: don't retry, max retry count reached")
    }
    return result.asResultWrapper()
}