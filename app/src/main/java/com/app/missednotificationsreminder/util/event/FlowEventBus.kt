package com.app.missednotificationsreminder.util.event

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import timber.log.Timber

/**
 * Implementation of the event bus based on the Kotlin Flow technology.
 * courtesy: https://gist.github.com/takahirom/f2dbcc3053adfd87ac7e321d95a23021
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class FlowEventBus {
    // If multiple threads are going to emit events to this
    // then it must be made thread-safe like this instead
    private val _bus = MutableStateFlow<Event>(object : Event {})

    /**
     * Send the event to all the subscribers
     *
     * @param event the event to send
     */
    fun send(event: Event) {
        Timber.d("send() called with: event = %s",
                event)
        _bus.value = event
    }

    /**
     * Get the flow from the current event bus object
     *
     * @return
     */
    fun toFlow(): Flow<Event> = _bus.drop(1)
}