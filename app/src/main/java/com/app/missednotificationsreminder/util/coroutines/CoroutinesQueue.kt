package com.app.missednotificationsreminder.util.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * The queue for the suspending calls. Only one call is active at the time, all other are scheduled sequentially
 * until the running one completes either with error or result
 */
class CoroutinesQueue(private val scope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
                      capacity: Int = 0) {
    private val queue by lazy { scope.messageActor(capacity) }

    suspend fun <T> launchInQueue(block: suspend () -> T): T {
        val supervisor = SupervisorJob(coroutineContext[Job])
        val parentContext = coroutineContext + supervisor
        try {
            val response = CompletableDeferred<T>(supervisor)
            queue.send(Message.LaunchInQueue(response) {
                withContext(parentContext) {
                    val disposableHandle = scope.coroutineContext[Job]
                            ?.invokeOnCompletion {
                                supervisor.cancel("Queue canceled", it)
                            }
                    try {
                        block()
                    } finally {
                        disposableHandle?.dispose()
                    }
                }
            })
            return response.await()
        } finally {
            supervisor.cancel()
        }
    }

    fun close() {
        queue.close()
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private fun CoroutineScope.messageActor(capacity: Int = 0) = actor<Message>(capacity = capacity) {
        for (msg in channel) { // iterate over incoming messages
            when (msg) {
                is Message.LaunchInQueue<*> -> msg.launch()
            }
        }
    }

    private sealed class Message {

        class LaunchInQueue<T>(private val response: CompletableDeferred<T>,
                               val block: suspend () -> T) : Message() {
            suspend fun launch() {
                kotlin.runCatching {
                    if (response.isActive) {
                        response.complete(block())
                    }
                }.onFailure { response.completeExceptionally(it) }
            }
        }
    }
}

fun CoroutineScope.coroutinesQueue(capacity: Int = 0) = CoroutinesQueue(this@coroutinesQueue, capacity)