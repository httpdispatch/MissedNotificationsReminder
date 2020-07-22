package com.app.missednotificationsreminder.settings.applicationselection

import com.app.missednotificationsreminder.binding.model.BaseViewStateModel
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber

/**
 * The view model for the single application item
 *
 * @property applicationItem                        the current application item
 * @property picasso
 * state changed event
 */
@ExperimentalCoroutinesApi
class ApplicationItemViewModel(
        private val applicationItem: ApplicationItemViewState,
        private val picasso: Picasso) : BaseViewStateModel<ApplicationItemViewState, ApplicationItemViewStatePartialChanges>(applicationItem) {

    /**
     * Get the application icon request
     *
     * @return
     */
    val icon: RequestCreator by lazy {
        Timber.d("getIcon for %1\$s", toString())
        picasso.load(applicationItem.iconUri)
                .fit()
    }

    /**
     * Reverse checked state. Called when the application item clicked. Method binded directly in
     * the layout xml
     */
    fun onItemClicked() {
        Timber.d("onItemClicked for %1\$s", toString())
        process(ApplicationItemViewStatePartialChanges.CheckedStateChange(!_viewState.value.checked))
    }

    override fun toString(): String {
        return "ApplicationItemViewModel: state = ${_viewState.value}"
    }
}