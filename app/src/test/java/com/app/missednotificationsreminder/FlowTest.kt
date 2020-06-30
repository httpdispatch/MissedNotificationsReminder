package com.app.missednotificationsreminder

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Test
import java.util.concurrent.Executors

class FlowTest {
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
}