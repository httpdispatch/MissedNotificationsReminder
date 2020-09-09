package com.app.missednotificationsreminder.ui.widget.recyclerview

import com.app.missednotificationsreminder.binding.model.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import timber.log.Timber

abstract class LifecycleAdapterWithViewEffect<VH : LifecycleViewHolder, VIEW_EFFECT> : LifecycleAdapter<VH>() {
    private val _viewEffect = MutableStateFlow<Event<VIEW_EFFECT?>>(Event(null))

    val viewEffect: Flow<VIEW_EFFECT> = _viewEffect
            .map { it.getContentIfNotHandled() }
            .filterNotNull()

    fun requestViewEffect(effect: VIEW_EFFECT) {
        Timber.d("requestViewEffect() called with: effect = $effect")
        _viewEffect.value = Event(effect)
    }
}