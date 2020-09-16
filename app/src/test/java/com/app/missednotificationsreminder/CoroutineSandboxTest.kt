package com.app.missednotificationsreminder

import com.app.missednotificationsreminder.util.flow.amb
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume


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

    @Test
    fun testBackpressure() {
        testFlow { conflate() }
        testFlow { onBackpressureDrop() }
        testFlow { buffer() }
    }

    @Test
    fun testBackpressureFlatMap() {
        testFlowFlatMap { conflate() }
        testFlowFlatMap { onBackpressureDrop() }
        testFlowFlatMap { buffer() }
    }

    fun <T> Flow<T>.onBackpressureDrop(): Flow<T> {
        return channelFlow {
            collect { offer(it) }
        }.buffer(capacity = 0)
    }

    private fun testFlow(limit: Int = 10, onBackpressure: Flow<Int>.() -> Flow<Int>) {

        val time = System.currentTimeMillis()

        val stringBuffer = StringBuffer()

        runBlocking {
            withContext(Dispatchers.Default) {
                flow(timeout = 100, limit = limit)
                        .flowOn(Dispatchers.IO)
                        .onBackpressure()
                        .onEach { println("Start handling $it") }
                        .map { doWorkDelay(i = it, timeout = 200) }
                        .onEach { println("Intermediate handling $it") }
                        .map { doWorkDelay(i = it, timeout = 300) }
                        .onEach { println("End handling $it") }
                        .collect {
                            stringBuffer.append("$it ")
                        }
            }
        }

        println((System.currentTimeMillis() - time))
        println(stringBuffer.toString())
    }

    private fun testFlowFlatMap(limit: Int = 10, onBackpressure: Flow<Int>.() -> Flow<Int>) {

        val time = System.currentTimeMillis()

        val stringBuffer = StringBuffer()

        runBlocking {
            withContext(Dispatchers.Default) {
                flow(timeout = 100, limit = limit)
                        .flowOn(Dispatchers.IO)
                        .onBackpressure()
                        .onEach { println("Start handling $it") }
                        .map { doWorkDelay(i = it, timeout = 200) }
                        .onEach { println("Intermediate handling $it") }
                        .flatMapMerge(concurrency = 1) {
                            flowOf(it)
                                    .map { doWorkDelay(i = it, timeout = 300) }
                        }
                        .onEach { println("End handling $it") }
                        .collect {
                            stringBuffer.append("$it ")
                        }
            }
        }

        println((System.currentTimeMillis() - time))
        println(stringBuffer.toString())
    }

    private fun flow(
            timeout: Long,
            limit: Int
    ): Flow<Int> = flow {
        for (i in 1..limit) {
            delay(timeout)
            emit(i)
        }
    }

    private suspend fun doWorkDelay(i: Int, timeout: Long): Int {
        delay(timeout)
        return i
    }

    @Test
    fun testBroadcastChannel() = runBlocking<Unit> {
        val queue = BroadcastChannel<Boolean>(BUFFERED)
        launch {
            queue.consumeEach { println("receive $it") }
        }
        launch {
            queue.consumeEach { println("receive2 $it") }
        }
        delay(1000)
        queue.send(true)
        queue.close()
    }

    @Test
    fun testFlowScopeCancelation() = runBlocking<Unit> {
        val scope = CoroutineScope(coroutineContext + Job())
        val queue = Queue(scope)
        launch(Dispatchers.Default) {
            queue.send(1)
            queue.send(2)
            queue.send(3)
            delay(200)
            println("Cancel scope")
            scope.cancel()
            delay(200)
            queue.send(4)
            delay(200)
            println("Close queue")
        }
    }

    class Queue(scope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)) {
        private val queue = Channel<Int>(Channel.BUFFERED)

        init {
            scope.launch {
                queue.consumeAsFlow()
                        .onEach { println("$it ${kotlin.coroutines.coroutineContext[Job]} ${scope.coroutineContext[Job]}") }
                        .onStart { println("Start") }
                        .catch { println("$it") }
                        .onCompletion { println("Completion") }
                        .collect {}
            }

        }

        suspend fun send(value: Int) {
            queue.send(value)
        }
    }

    @Test
    fun testMutableStateFlowThread() = runBlocking<Unit> {
        val flow = MutableStateFlow<Int>(0)

        launch {
            flow
                    .collect { println("received $it in ${Thread.currentThread().name}") }
        }
        delay(1000)
        flow.value = 1
        withContext(Dispatchers.IO) {
            flow.value = 2
        }
        withContext(Dispatchers.Default) {
            flow.value = 3
        }
    }

    @Test
    fun testContinuationState() = runBlocking<Unit> {
        val scope = CoroutineScope(Dispatchers.Default + Job(coroutineContext[Job]))
        val value = suspendCancellableCoroutine<Boolean> { continuation ->
            scope.launch {
                println("suspendCoroutineCompleted ${continuation.isCompleted}")
                continuation.resume(true)
                println("suspendCoroutine resumed")
                delay(1000)
                println("suspendCoroutineCompleted ${continuation.isCompleted}")
                scope.cancel()
            }
        }
        println("suspendCoroutineValue $value")
    }

    @Test
    fun testDisposableHandle() = runBlocking<Unit> {
        val scope = CoroutineScope(Dispatchers.Default)
        val handler = AtomicReference<DisposableHandle?>()
        val scopeCompletionListener = scope.coroutineContext[Job]?.invokeOnCompletion {
            CoroutineScope(Dispatchers.IO).launch {
                println("Completion")
                handler.get()?.dispose()
            }
        }
        handler.set(scopeCompletionListener)

        delay(1000)
        scope.cancel()
        delay(1000)
    }


    @Test
    fun testSupervisorScope() = runBlocking<Unit> {
        val scope = CoroutineScope(EmptyCoroutineContext)
        scope.launch {
            println("Inside scope $coroutineContext")
            val scope2 = CoroutineScope(coroutineContext + Job(coroutineContext[Job]))
            scope2.launch {
                println("Inside scope2 $coroutineContext")
                delay(100)
                println("Inside scope2 after delay")
            }
//            launch {
//                delay(50)
//                println("Cancel scope2")
//                scope2.cancel()
//            }
            delay(100)
            println("Inside scope after delay")
        }
        delay(50)
        println("Cancel scope")
        scope.cancel()
        delay(500)
    }

    @Test
    fun testSupervisorScope2() = runBlocking<Unit> {
        coroutineScope {
            println("Inside scope")
            launch {
                coroutineScope {
                    launch {
                        println("Inside scope2")
                        delay(100)
                        println("Inside scope2 after delay")
                    }
                    launch {
                        delay(50)
                        println("Cancel scope2")
                        this@coroutineScope.cancel()
                    }
                }
            }
            delay(100)
            println("Inside scope after delay")
        }
        delay(50)
        delay(500)
    }

    @Test
    fun testSupervisorJob() = runBlocking {
        withContext(Dispatchers.Default) {
            repeat(5) {
                launch {
                    val supervisor = SupervisorJob(coroutineContext[Job])
                    val job = launch {
                        delay(200)
                        println("done")
                    }
                    job.join()
                    supervisor.cancel()
                }
            }
        }
    }
}