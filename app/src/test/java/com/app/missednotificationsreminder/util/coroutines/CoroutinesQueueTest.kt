package com.app.missednotificationsreminder.util.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@ObsoleteCoroutinesApi
class CoroutinesQueueTest {
    @Test
    fun `Test coroutines queue performs sequentially`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val queue = CoroutinesQueue(scope)
        val launchedActions = AtomicInteger(0)
        val scheduledActionsCount = AtomicInteger(0)
        val scheduledActionsCountBefore = AtomicInteger(0)
        val actionsToSchedule = listOf("First", "Second", "Third", "Fourth", "Fifth")
        coroutineScope {
            val jobs = mutableListOf<Job>()
            for (c in actionsToSchedule) {
                jobs.add(launch(newSingleThreadContext("MyOwnThread@$c")) {
                    delay(100)
                    assertEquals(actionsToSchedule.size, scheduledActionsCount.get())
                    println("$c launch before ${Thread.currentThread().name}")
                    val res = kotlin.runCatching {
                        scheduledActionsCountBefore.incrementAndGet()
                        queue.launchInQueue {
                            val initialLaunchedActions = launchedActions.incrementAndGet()
                            println("$c launch inside ${Thread.currentThread().name} ${coroutineContext}")
                            assertEquals("MyOwnThread@$c", Thread.currentThread().name.substringBefore(' '))
                            delay(200)
                            assertEquals(actionsToSchedule.size, scheduledActionsCountBefore.get())
                            println("$c launch complete ${Thread.currentThread().name}")
                            assertEquals(initialLaunchedActions, launchedActions.get())
                            c
                        }
                    }.onFailure {
                        println(it)
                    }
                    assertEquals(c, res.getOrNull())
                    println("$c, received $res ${Thread.currentThread().name}")
                })
                scheduledActionsCount.incrementAndGet()
            }
            jobs.joinAll()
        }
        assertEquals(actionsToSchedule.size, launchedActions.get())
    }

    @Test
    fun `Test coroutines queue supports exceptions without breaking parent job`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val queue = CoroutinesQueue(scope)
        val launchedActions = AtomicInteger(0)
        val actionsToSchedule = listOf("First", "Second", "Third", "Fourth", "Fifth")
        coroutineScope {
            val jobs = mutableListOf<Job>()
            for (c in actionsToSchedule) {
                jobs.add(launch(newSingleThreadContext("MyOwnThread@$c")) {
                    delay(100)
                    println("$c launch before ${Thread.currentThread().name}")
                    val res = kotlin.runCatching {
                        queue.launchInQueue {
                            val initialLaunchedActions = launchedActions.incrementAndGet()
                            println("$c launch inside ${Thread.currentThread().name} ${coroutineContext}")
                            delay(200)
                            println("$c launch complete ${Thread.currentThread().name}")
                            assertEquals(initialLaunchedActions, launchedActions.get())
                            throw ArithmeticException(c)
                        }
                    }.onFailure {
                        println(it)
                    }
                    assertTrue(res.exceptionOrNull()!!.let { it is ArithmeticException && it.message == c })
                    println("$c, launch completed ${Thread.currentThread().name}")
                })
            }
            jobs.joinAll()
        }
        assertEquals(actionsToSchedule.size, launchedActions.get())
    }

    @Test
    fun `Test coroutines queue supports exceptions parent job break`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val queue = CoroutinesQueue(scope)
        val launchedActions = AtomicInteger(0)
        val actionsToSchedule = listOf("First", "Second", "Third", "Fourth", "Fifth")
        try {
            coroutineScope {
                val jobs = mutableListOf<Job>()
                for (c in actionsToSchedule) {
                    jobs.add(launch(newSingleThreadContext("MyOwnThread@$c")) {
                        delay(100)
                        println("$c launch before ${Thread.currentThread().name}")
                        queue.launchInQueue {
                            delay(20)
                            val initialLaunchedActions = launchedActions.incrementAndGet()
                            println("$c launch inside ${Thread.currentThread().name} ${coroutineContext}")
                            delay(200)
                            println("$c launch complete ${Thread.currentThread().name}")
                            assertEquals(initialLaunchedActions, launchedActions.get())
                            throw ArithmeticException(c)
                        }
                    })
                }
                jobs.joinAll()
            }
        } catch (e: Throwable) {
            assertTrue(e is ArithmeticException)
        }
        // only one action executed and it breaks all other actions
        assertEquals(1, launchedActions.get())
    }

    @Test
    fun `Test coroutines queue launching scope cancel cancels all scheduled queue jobs`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val launchingScope = CoroutineScope(Dispatchers.Default)
        val queue = CoroutinesQueue(scope)
        launch {
            delay(100)
            try {
                println("Cancel scope ${scope.coroutineContext[Job]}")
                launchingScope.cancel()
                println("Cancel scope ${scope.coroutineContext[Job]}")
            } catch (t: Throwable) {
                println(t)
            }
        }
        val launchedActions = AtomicInteger(0)
        val canceledActions = AtomicInteger(0)
        val actionsToSchedule = listOf("First", "Second", "Third", "Fourth", "Fifth")
        launchingScope.launch {
            val jobs = mutableListOf<Job>()
            for (c in actionsToSchedule) {
                jobs.add(launch(newSingleThreadContext("MyOwnThread@$c")) {
                    println("$c launch before ${Thread.currentThread().name}")
                    queue.launchInQueue {
                        try {
                            delay(20)
                            launchedActions.incrementAndGet()
                            println("$c launch inside ${Thread.currentThread().name} ${coroutineContext}")
                            delay(200)
                            println("$c launch complete ${Thread.currentThread().name} ${coroutineContext}")
                        } catch (e: Throwable) {
                            assertTrue(e is CancellationException)
                            canceledActions.incrementAndGet()
                            throw e
                        }
                    }
                })
            }
            jobs.joinAll()
        }
        launchingScope.coroutineContext[Job]?.join()
        delay(1000)
        // only one action executed and it breaks all other actions
        assertEquals(1, launchedActions.get())
        assertEquals(1, canceledActions.get())
    }

    @Test
    fun `Test coroutines queue parent scope cancel cancels all scheduled queue jobs`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.coroutineContext[Job]?.invokeOnCompletion { println("Completed $it") }
        val queue = CoroutinesQueue(scope)
        launch {
            delay(100)
            try {
                println("Cancel scope ${scope.coroutineContext[Job]}")
                scope.cancel()
                println("Cancel scope ${scope.coroutineContext[Job]}")
            } catch (t: Throwable) {
                println(t)
            }
        }
        val launchedActions = AtomicInteger(0)
        val canceledActions = AtomicInteger(0)
        val actionsToSchedule = listOf("First", "Second", "Third", "Fourth", "Fifth")
        coroutineScope {
            val jobs = mutableListOf<Job>()
            for (c in actionsToSchedule) {
                jobs.add(launch(newSingleThreadContext("MyOwnThread@$c")) {
                    println("$c launch before ${Thread.currentThread().name}")
                    queue.launchInQueue {
                        try {
                            delay(20)
                            println("$c launch inside ${Thread.currentThread().name} ${coroutineContext}")
                            delay(1000)
                            println("$c launch complete ${Thread.currentThread().name}")
                        } catch (e: Throwable) {
                            assertTrue(e is CancellationException)
                            canceledActions.incrementAndGet()
                            throw e
                        }
                    }
                })
            }
            jobs.joinAll()
            jobs.onEach { println(it) }
        }
        // only one action executed till the end (can't find a way to cancel already running execution when parent scope cancels)
        assertEquals(1, launchedActions.get())
        assertEquals(0, canceledActions.get())
    }

    @Test
    fun `Test coroutines queue close cancels all scheduled queue jobs`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.coroutineContext[Job]?.invokeOnCompletion { println("Completed $it") }
        val queue = CoroutinesQueue(scope)
        queue.close()
        val launchedActions = AtomicInteger(0)
        val canceledActions = AtomicInteger(0)
        val actionsToSchedule = listOf("First", "Second", "Third", "Fourth", "Fifth")
        coroutineScope {
            val jobs = mutableListOf<Job>()
            for (c in actionsToSchedule) {
                jobs.add(launch(newSingleThreadContext("MyOwnThread@$c")) {
                    println("$c launch before ${Thread.currentThread().name}")
                    val res = kotlin.runCatching {
                        queue.launchInQueue {
                            try {
                                delay(20)
                                println("$c launch inside ${Thread.currentThread().name} ${coroutineContext}")
                                delay(1000)
                                println("$c launch complete ${Thread.currentThread().name}")
                            } catch (e: Throwable) {
                                assertTrue(e is CancellationException)
                                canceledActions.incrementAndGet()
                                throw e
                            }
                        }
                    }
                    assertTrue(res.exceptionOrNull()!!.let { it is ClosedSendChannelException})
                })
            }
            jobs.joinAll()
        }
        // No actions executed
        assertEquals(0, launchedActions.get())
        assertEquals(0, canceledActions.get())
    }
}
