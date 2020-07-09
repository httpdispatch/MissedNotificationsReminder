package com.app.missednotificationsreminder.util.flow

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select

@FlowPreview
internal class FlowAmb<T>(private val flows: Iterable<Flow<T>>) : AbstractFlow<T>() {
    @InternalCoroutinesApi
    override suspend fun collectSafely(collector: FlowCollector<T>) {
        coroutineScope {
            val channels = flows.map { it.produceIn(this@coroutineScope) }
            var selectedChannelIndex = 0
            select<Unit> {
                channels.forEachIndexed { index, channel ->
                    channel.onReceiveOrClosed { value ->
                        selectedChannelIndex = index
                        if (!value.isClosed) {
                            collector.emit(value.value)
                        }
                    }
                }
            }
            channels.forEachIndexed { index, channel ->
                if (index != selectedChannelIndex) channel.cancel()
            }
            collector.emitAll(channels[selectedChannelIndex])
        }
    }
}