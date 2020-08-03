package com.app.missednotificationsreminder.util.flow

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.AbstractFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.selects.select

/**
 * Implementation is taken from
 * https://dev.to/psfeng/a-story-of-building-a-custom-flow-operator-buffertimeout-4d95
 */
@FlowPreview
internal class FlowBufferTimeout<T> constructor(
        private val source: Flow<T>,
        private val size: Int,
        private val duration: Long) : AbstractFlow<List<T>>() {
    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    override suspend fun collectSafely(collector: FlowCollector<List<T>>) {
        coroutineScope {
            val events = mutableListOf<T>()
            val tickerChannel = ticker(duration)
            try {
                val upstreamValues: ReceiveChannel<T> = produce { source.collect { value -> send(value) } }

                while (isActive) {
                    var hasTimedOut = false

                    select<Unit> {
                        upstreamValues.onReceive { value ->
                            events.add(value)
                        }

                        tickerChannel.onReceive {
                            hasTimedOut = true
                        }
                    }

                    if (events.size == size || (hasTimedOut && events.isNotEmpty())) {
                        collector.emit(events)
                        events.clear()
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                // drain remaining events
                if (events.isNotEmpty()) collector.emit(events)
            } finally {
                tickerChannel.cancel()
            }
        }
    }
}