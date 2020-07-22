package com.app.missednotificationsreminder.binding.model

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@ExperimentalCoroutinesApi
open class BaseViewStateModel<
        VIEW_STATE,
        PARTIAL_CHANGES : ViewStatePartialChanges<VIEW_STATE>>(
        initialViewState: VIEW_STATE) : BaseViewModel() {
    protected val _viewState = MutableStateFlow(initialViewState)

    val viewState: StateFlow<VIEW_STATE> = _viewState

    fun process(event: PARTIAL_CHANGES) {
        Timber.d("process() called: with event=${event}")
        viewModelScope.launch {
            _viewState.apply { value = event.reduce(value) }
        }
    }

}