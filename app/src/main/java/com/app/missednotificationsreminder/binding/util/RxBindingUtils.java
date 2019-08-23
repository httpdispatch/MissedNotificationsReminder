package com.app.missednotificationsreminder.binding.util;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import com.f2prateek.rx.preferences.Preference;

import rx.Observable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * Static factory methods for creating {@linkplain Observable observables} for {@link BindableObject}.
 *
 * @author Eugene Popovich
 */
public class RxBindingUtils {
    /**
     * Create an observable for value changes on {@code object}.
     * <p>
     * <em>Warning:</em> The created observable keeps a strong reference to {@code object}.
     * Unsubscribe to free this reference.
     * <p>
     * <em>Note:</em> A value will be emitted immediately on subscribe.
     *
     * @param object
     * @param <T>
     * @return
     */
    @CheckResult @NonNull public static <T> Observable<T> valueChanged(@NonNull BindableObject<T> object) {
        return Observable.create(new BindableObjectValueChangedOnSubscribe<T>(object));
    }

    /**
     * Initialized binding object with the initial value from preferences and subscribe the preference to the value
     * changes events of the binding object and vice versa
     *
     * @param object     the binding object
     * @param preference the preference to bind with
     * @param <T>        parameter type of the wrapped by binding object and preference value
     * @return a {@link Subscription} of the preferences to the binding object change
     * events and the binding object to the preferences change event
     */
    @NonNull public static <T> Subscription bindWithPreferences(@NonNull BindableObject<T> object,
                                                                @NonNull Preference<T> preference) {
        CompositeSubscription result = new CompositeSubscription();
        // set the initial value from preferences
        object.set(preference.get());
        // subscribe preferences to the data binding fields changing events to save the modified
        // values
        result.add(RxBindingUtils
                .valueChanged(object)
                .skip(1)// skip initial value emitted automatically right after the subscription
                .subscribe(preference.asAction()));
        // subscribe data binding field to the preference change event
        result.add(preference
                .asObservable()
                .subscribe(object.asAction()));
        return result;
    }
}
