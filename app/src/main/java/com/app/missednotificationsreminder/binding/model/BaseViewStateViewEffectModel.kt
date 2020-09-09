package com.app.missednotificationsreminder.binding.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
open class BaseViewStateViewEffectModel<
        VIEW_STATE,
        VIEW_EFFECT,
        PARTIAL_CHANGES : ViewStatePartialChanges<VIEW_STATE>>(
        initialViewState: VIEW_STATE) : BaseViewStateModel<VIEW_STATE, PARTIAL_CHANGES>(initialViewState) {
    private val _viewEffect = MutableStateFlow<Event<VIEW_EFFECT?>>(Event(null))

    val viewEffect: Flow<VIEW_EFFECT> = _viewEffect
            .map{it.getContentIfNotHandled()}
            .filterNotNull()

    fun requestViewEffect(effect: VIEW_EFFECT) {
        _viewEffect.value = Event(effect)
    }
}