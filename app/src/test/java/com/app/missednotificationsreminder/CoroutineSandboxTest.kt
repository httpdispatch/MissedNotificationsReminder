package com.app.missednotificationsreminder

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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

    @Test fun testCoroutinesHelloWorld() = runBlocking {
        launch{
            println("World!")
        }
        println("Hello")
    }

    @Test fun testException() = runBlocking {
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
}