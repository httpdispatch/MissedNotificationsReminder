package com.app.missednotificationsreminder

import com.app.missednotificationsreminder.util.flow.amb
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
import org.junit.Test
import java.util.concurrent.Executors

class CoroutineSandboxTest {
    @ExperimentalCoroutinesApi
    @Test
    fun testStateFlow() {
        runBlocking {
            val state = MutableStateFlow<Int>(1)
            val job = state
                    .take(1)
                    .onEach { log("First ${Thread.currentThread().name}") }
                    .flowOn(Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "Thread1") }.asCoroutineDispatcher())
                    .onEach { log("Second ${Thread.currentThread().name}") }
                    .flowOn(Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "Thread2") }.asCoroutineDispatcher())
                    .onEach { log("Third ${Thread.currentThread().name}") }
                    .launchIn(CoroutineScope(Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "Thread3") }.asCoroutineDispatcher()))
            log("Emit ${Thread.currentThread().name}")
            state.value = 1
            job.join()
        }
    }

    fun log(s: String) {
        System.out.println(s)
    }

    @Test
    fun testCoroutinesHelloWorld() = runBlocking {
        launch {
            println("World!")
        }
        println("Hello")
    }

    @Test
    fun testException() = runBlocking {
        val job = GlobalScope.launch { // root coroutine with launch
            println("Throwing exception from launch")
            throw IndexOutOfBoundsException() // Will be printed to the console by Thread.defaultUncaughtExceptionHandler
        }
        job.join()
        println("Joined failed job")
        val deferred = GlobalScope.async { // root coroutine with async
            println("Throwing exception from async")
            throw ArithmeticException() // Nothing is printed, relying on user to call await
        }
        try {
            deferred.await()
            println("Unreached")
        } catch (e: ArithmeticException) {
            println("Caught ArithmeticException")
        }
    }

    fun foo(): Flow<Int> = flow {
        for (i in 1..3) {
            delay(100)
            println("Emitting $i")
            emit(i)
        }
    }

    @Test
    fun testCancellation() = runBlocking<Unit> {
        val job = Job()
        launch(job) {
            withTimeoutOrNull(250) {
                foo().collect { value -> println(value) }
            }
            println("Completed")
        }
        println("Cancelling")
        delay(400)
        launch(job) {
            withTimeoutOrNull(250) {
                foo().collect { value -> println(value) }
            }
            println("Completed2")
        }
        delay(300)
        println("Done")
    }


    @Test
    fun testCoroutineWinner() = runBlocking {
        suspend fun <T> amb(vararg jobs: Deferred<T>): T = select {
            fun cancelAll() = jobs
                    .forEach { it.cancel() }

            for (deferred in jobs) {
                deferred.onAwait {
                    cancelAll()
                    it
                }
            }
        }

        amb(
                async(start = CoroutineStart.LAZY) {
                    delay(100)
                    println("I'm winner")
                },
                async(start = CoroutineStart.LAZY) {
                    delay(200)
                    println("I'm looser, should not be printed")
                }
        )
        delay(300)
    }

    @FlowPreview
    @InternalCoroutinesApi
    @Test
    fun testFlowWinner() = runBlocking {
        fun testFlow(name: String, timeout: Long): Flow<Int> = flow {
            for (i in 1..3) {
                delay(timeout)
                println("Emitting $i from ${name}")
                emit(i)
            }
        }
        listOf(
                flow<Int> {
                    delay(10)
                },
                testFlow("First", 60),
                testFlow("Second", 50),
                testFlow("Third", 70))
                .amb()
                .collect { println("Received $it") }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun testFlowCancellation() = runBlocking {
        val job = launch {
            callbackFlow<Unit> {
                println("Started parent")
                launch {
                    callbackFlow<Unit> {
                        println("Started child")
                        launch {
                            delay(400)
                            println("Completing child")
                            close()
                        }
                        awaitClose {
                            println("Closing child")
                        }
                    }
                            .collect()
                    println("Completed child")
                    println("Completing parent")
                    close()
                }
                awaitClose {
                    println("Closing parent")
                }
            }
                    .collect()
            println("Completed parent")
        }
        delay(100)
        job.cancel()
        delay(500)
    }

    @Test
    fun testCoroutineCancellation() = runBlocking {
        val job = launch {
            try {
                repeat(1000) { i ->
                    println("job: I'm sleeping $i ...")
                    delay(500L)
                }
            } finally {
                println("Job status: completed: ${coroutineContext[Job]?.isCompleted}")
                println("Job status: cancelled: ${coroutineContext[Job]?.isCancelled}")
                println("job: I'm running finally")
            }
        }
        delay(1300L) // delay a bit
        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion
        println("main: Now I can quit.")
    }
}