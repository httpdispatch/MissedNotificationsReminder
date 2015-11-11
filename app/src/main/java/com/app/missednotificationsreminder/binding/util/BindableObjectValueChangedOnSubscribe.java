package com.app.missednotificationsreminder.binding.util;

import rx.Observable;
import rx.Subscriber;
import rx.subscriptions.Subscriptions;

/**
 * The action which invoked when the Observable.subscribe is called for the rx Observable created
 * for the {@link BindableObject}
 *
 * @author Eugene Popovich
 */
public class BindableObjectValueChangedOnSubscribe<T> implements Observable.OnSubscribe<T> {
    private final BindableObject<T> mObject;

    /**
     * Creates an BindableObjectValueChangedOnSubscribe for the given bindableObject
     *
     * @param bindableObject the bindable object to create
     */
    public BindableObjectValueChangedOnSubscribe(BindableObject<T> bindableObject) {
        this.mObject = bindableObject;
    }

    @Override public void call(final Subscriber<? super T> subscriber) {
        // create the property changed callback for the BindableObject which emits property value
        // to the subscriber when it is changed
        android.databinding.Observable.OnPropertyChangedCallback callback = new android.databinding.Observable.OnPropertyChangedCallback() {
            @Override public void onPropertyChanged(android.databinding.Observable sender, int propertyId) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(mObject.get());
                }
            }
        };
        mObject.addOnPropertyChangedCallback(callback);

        // when the subscription become unsubscribed remove added above property changed callback
        subscriber.add(
                Subscriptions.create(
                        () -> mObject.removeOnPropertyChangedCallback(callback)));

        // Emit initial value.
        subscriber.onNext(mObject.get());
    }
}
