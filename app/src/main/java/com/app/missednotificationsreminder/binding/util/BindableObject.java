package com.app.missednotificationsreminder.binding.util;

import android.databinding.ObservableField;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

import rx.functions.Action1;

/**
 * An object wrapper to make it observable. Extension of the {@link ObservableField} with an
 * additional {@link #equals(Object, Object)} method to compare values in the {@link #set(Object)
 * } method.
 * <p>
 * Objects of such type are RxJava enabled. Use {@link #asAction()} to get the
 * rx action which stores new value to this observable.
 * <p>
 * Fields of this type should be declared final because bindings only detect changes in the
 * field's value, not of the field itself.
 * <p>
 * For a now it extends {@link BaseObservable} instead of {@link ObservableField} because of
 * issue when whe have conversion and adapter binding for the same observable type and the
 * observable extends {@link android.databinding.BaseObservable}. Looks like conversion has
 * higher priority in such case. Hope this will be fixed in future releases of the Android Data
 * Binding framework
 *
 * @param <T> The type parameter for the actual object.
 * @author Eugene Popovich
 */
public class BindableObject<T> extends BaseObservable {
    T mValue;

    /**
     * Creates an empty observable object
     */
    public BindableObject() {
    }

    /**
     * Wraps the given object and creates an observable object
     *
     * @param value The value to be wrapped as an observable.
     */
    public BindableObject(T value) {
        mValue = value;
    }

    /**
     * Set the stored value.
     */
    public void set(T value) {
        if (!equals(mValue, value)) {
            mValue = value;
            notifyChange();
        }
    }

    public T get() {
        return mValue;
    }

    /**
     * Compare two objects of the same type. Supports null comparison as well as {@linkplain
     * Object#equals(Object) Object.equals()} method.
     * <p>
     * Override this method if you need additional comparison logic, for example custom string
     * comparison.
     *
     * @param o1
     * @param o2
     * @return true if objects are equal, false otherwise
     */
    public boolean equals(T o1, T o2) {
        return (o1 == o2) || (o1 != null && o1.equals(o2));
    }

    /**
     * An action which stores a new value for this object.
     */
    @CheckResult @NonNull public Action1<? super T> asAction() {
        return new Action1<T>() {
            @Override public void call(T value) {
                set(value);
            }
        };
    }
}
