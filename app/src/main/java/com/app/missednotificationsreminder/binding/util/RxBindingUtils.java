package com.app.missednotificationsreminder.binding.util;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

import rx.Observable;

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
}
