package com.app.missednotificationsreminder.binding.util

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asLiveData
import com.app.missednotificationsreminder.util.asFlow
import com.f2prateek.rx.preferences.Preference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Initialized mutable state flow with the initial value from preferences and subscribe the preference to the value
 * changes events of the mutable state flow and vice versa
 *
 * @param preference the preference to bind with
 **/
@FlowPreview
@ExperimentalCoroutinesApi
suspend fun <T : Any> MutableStateFlow<T>.bindWithPreferences(preference: Preference<T>) {
    this@bindWithPreferences.bindWithPreferences(
            preference,
            { v, _ -> v },
            { v -> v })
}

/**
 * Update [MutableStateFlow] with the initial value from [Preference]s and subscribe the [Preference] to the value
 * changes events of the [MutableStateFlow] and vice versa
 *
 * @param preference the preference to bind with
 * @param preferenceToStateReducer the reducer of the state value using preference value
 * @param stateToPreference The transformer of the state value to the preference value
 **/
@FlowPreview
@ExperimentalCoroutinesApi
suspend fun <T : Any, P : Any> MutableStateFlow<T>.bindWithPreferences(
        preference: Preference<P>,
        preferenceToStateReducer: (P, T) -> T,
        stateToPreference: (T) -> P) {
    try {
        coroutineScope {
            Timber.d("bindWithPreferences: start for ${preference.key()}")
            val stateFlow = this@bindWithPreferences
            // set the initial value from preferences
            stateFlow.value = preferenceToStateReducer(preference.get()!!, stateFlow.value)
            // subscribe preferences to the mutable state flow changing events to save the modified
            // values
            launch {
                Timber.d("bindWithPreferences: job1 start")
                stateFlow
                        .drop(1)
                        .map { stateToPreference(it) }
                        .distinctUntilChanged()
                        .onEach { Timber.d("bindWithPreferences: value $it") }
                        .debounce(100)
                        .collect { preference.set(it) }
                Timber.d("bindWithPreferences: job1 end")
            }
            launch {
                Timber.d("bindWithPreferences: job2 start")
                preference.asFlow()
                        .collect { stateFlow.value = preferenceToStateReducer(it, stateFlow.value) }
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