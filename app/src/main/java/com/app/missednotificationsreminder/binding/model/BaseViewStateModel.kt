package com.app.missednotificationsreminder.binding.model

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
open class BaseViewStateModel<
        VIEW_STATE,
        PARTIAL_CHANGES : ViewStatePartialChanges<VIEW_STATE>>(
        initialViewState: VIEW_STATE) : BaseViewModel() {
    protected val _viewState = MutableStateFlow(initialViewState)

    val viewState: StateFlow<VIEW_STATE> = _viewState

    fun process(event: PARTIAL_CHANGES) {
        Timber.d("process() called: with event=${event}")
        viewModelScope.launch {
            processSync(event)
        }
    }

    protected fun processSync(event: PARTIAL_CHANGES) {
        _viewState.apply { value = event.reduce(value) }
    }

    override fun toString(): String {
        return "${this::class.simpleName}: state = ${_viewState.value}"
    }
}