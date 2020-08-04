package com.app.missednotificationsreminder.binding.model;

import androidx.lifecycle.ViewModel;

/**
 * The base view model with the common functionality
 */
public class BaseViewModel extends ViewModel {

    /**
     * Call this method when the view model is not needed anymore to cancel running background
     * operations.
     */
    public void shutdown() {
    }

    @Override protected void onCleared() {
        super.onCleared();
        shutdown();
    }
}
