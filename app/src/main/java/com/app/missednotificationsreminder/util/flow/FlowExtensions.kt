package com.app.missednotificationsreminder.util.flow

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow

/**
 * Mirrors the one [Flow] in an [Iterable] of several [Flow]s that first either emits an item or sends
 * a termination notification.
 */
@FlowPreview
fun <T> Iterable<Flow<T>>.amb(): Flow<T> = FlowAmb<T>(this@amb)

/**
 *  Mirrors the current  [Flow] or the other  [Flow] provided of which the first either emits an item or sends a termination
 * notification.
 */
@FlowPreview
fun <T> Flow<T>.ambWith(other: Flow<T>): Flow<T> = FlowAmb(listOf(this@ambWith, other))

@FlowPreview
fun <T> Flow<T>.bufferTimeout(duration: Long, size: Int = Int.MAX_VALUE): Flow<List<T>> =
        FlowBufferTimeout(this@bufferTimeout, size, duration)