package com.app.missednotificationsreminder.binding.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asLiveData
import com.app.missednotificationsreminder.util.asFlow
import com.f2prateek.rx.preferences.Preference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import rx.Subscription
import timber.log.Timber

/**
 * Initialized binding object with the initial value from preferences and subscribe the preference to the value
 * changes events of the binding object and vice versa
 *
 * @param preference the preference to bind with
 * @param <T>        parameter type of the wrapped by binding object and preference value
 **/
@ExperimentalCoroutinesApi
suspend fun <T : Any> MutableStateFlow<T>.bindWithPreferences(preference: Preference<T>) {
    try {
        coroutineScope {
            Timber.d("bindWithPreferences: start for ${preference.key()}")
            val stateFlow = this@bindWithPreferences
            // set the initial value from preferences
            stateFlow.value = preference.get()!!
            currentCoroutineContext()
            // subscribe preferences to the mutable state flow changing events to save the modified
            // values
            launch {
                Timber.d("bindWithPreferences: job1 start")
                stateFlow
                        .drop(1)
                        .onEach { Timber.d("bindWithPreferences: value $it") }
                        .collect { preference.set(it) }
                Timber.d("bindWithPreferences: job1 end")
            }
            launch {
                Timber.d("bindWithPreferences: job2 start")
                preference.asFlow()
                        .collect { stateFlow.value = it }
                Timber.d("bindWithPreferences: job2 end")
            }
        }
    } finally {
        Timber.d("bindWithPreferences: complete")
    }
}

@ExperimentalCoroutinesApi
fun <T> MutableStateFlow<T>.asMediatorLiveData(): MediatorLiveData<T> {
    Timber.d("asMediatorLiveData() called")
    val stateFlow = this@asMediatorLiveData
    val result = stateFlow.asLiveData() as MediatorLiveData<T>

    result.observeForever { stateFlow.value = it }
    return result
}