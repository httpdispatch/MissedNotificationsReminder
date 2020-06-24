package com.app.missednotificationsreminder.binding.model;

import androidx.lifecycle.ViewModel;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * The base view model with the common functionality
 *
 * @author Eugene Popovich
 */
public class BaseViewModel extends ViewModel {
    /**
     * Composite subscription used to handle subscriptions added in the
     * {@linkplain #monitor(Subscription)} monitor method
     */
    private final CompositeSubscription mSubscriptions = new CompositeSubscription();

    /**
     * Remember the subscription so it may be unsubscribed in the {@linkplain #shutdown() shutdown}
     * method
     *
     * @param subscription the subscription to remember
     */
    public void monitor(Subscription subscription) {
        mSubscriptions.add(subscription);
    }

    /**
     * Call this method when the view model is not needed anymore to cancel running background
     * operations.
     */
    public void shutdown() {
        mSubscriptions.unsubscribe();
    }
}
