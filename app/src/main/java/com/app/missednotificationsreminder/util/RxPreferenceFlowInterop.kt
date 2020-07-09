package com.app.missednotificationsreminder.util

import com.f2prateek.rx.preferences.Preference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
fun <T : Any> Preference<T>.asFlow(): Flow<T> = this.asObservable().asFlow()