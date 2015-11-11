package com.app.missednotificationsreminder.binding.util;

import android.databinding.BindingConversion;

/**
 * A boolean wrapper to make it observable. Extension of the {@link BindableObject}. Doesn't
 * have any additional logic, except passing default false value in the default constructor.
 * <p>
 * This object is necessary to use {@link BindingConversion} features such as generic {@link
 * BindableObject} doesn't work as expected. Maybe this will be fixed in future version of
 * the Android Data Binding framework and this class will be unnecessary
 *
 * @author Eugene Popovich
 */
public class BindableBoolean extends BindableObject<Boolean> {

    /**
     * Creates an BindableBoolean with the initial value of <code>false</code>.
     */
    public BindableBoolean() {
        super(false);
    }

    /**
     * Creates an BindableBoolean with the given initial value.
     *
     * @param value the initial value for the BindableBoolean
     */
    public BindableBoolean(Boolean value) {
        super(value);
    }
}
