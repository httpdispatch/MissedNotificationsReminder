package com.app.missednotificationsreminder.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import rx.Observable
import rx.Subscriber
import rx.Subscription
import rx.subscriptions.Subscriptions
import java.util.concurrent.atomic.AtomicReference

@ExperimentalCoroutinesApi
fun <T : Any> Observable<T>.asFlow(): Flow<T> = callbackFlow {
    val disposableRef = AtomicReference<Subscription>()
    val observer = object : Subscriber<T>() {
        override fun onError(e: Throwable) {
            close(e)
        }

        override fun onNext(t: T) {
            sendBlocking(t)
        }

        override fun onCompleted() {
            close()
        }

        override fun onStart() {
            if (!disposableRef.compareAndSet(null, this)) this.unsubscribe()
        }
    }
    subscribe(observer)
    awaitClose { disposableRef.getAndSet(Subscriptions.unsubscribed())?.unsubscribe() }
}