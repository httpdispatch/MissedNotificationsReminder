package com.app.missednotificationsreminder.payment.billing.data.utls

import com.app.missednotificationsreminder.common.domain.entities.ResultWrapper
import com.app.missednotificationsreminder.common.domain.entities.ResultWrapper.Error
import com.app.missednotificationsreminder.common.domain.entities.map
import com.app.missednotificationsreminder.common.domain.entities.succeeded
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.transformWhile


/**
 * Collect the `Flow` results until last or [Error] value is emitted. Collect
 * data using [collector].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [collector] function.
 */
/**
 * Collect the `Flow` results until last or [Error] value is emitted. Collect
 * data using [collector].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [collector] function.
 */
suspend fun <T, R> Flow<ResultWrapper<T>>.collectWithLastErrorOrSuccessStatusSimple(
        defaultValue: ResultWrapper<R>,
        collector: (R, T) -> R
): ResultWrapper<R> {
    return collectWithLastErrorOrSuccessStatus(
            defaultValue,
            { it.succeeded })
    { mergedValue, value ->
        value.map {
            val mergedData = (mergedValue as ResultWrapper.Success<R>).data
            collector(mergedData, it)
        }
    }
}

/**
 * Collect the `Flow` results until last or not succeeded value is emitted. Collect
 * data using [collector].
 *
 * Note, that this function rethrows any [Throwable] exception thrown by [collector] function.
 */
suspend fun <T, R> Flow<T>.collectWithLastErrorOrSuccessStatus(
        defaultValue: R,
        succeededTest: (T) -> Boolean,
        collector: suspend (R, T) -> R
): R {
    return transformWhile {
        emit(it)
        succeededTest(it)
    }
            .fold(defaultValue, collector)
}
