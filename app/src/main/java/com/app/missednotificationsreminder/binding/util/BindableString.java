package com.app.missednotificationsreminder.binding.util;

import android.databinding.BindingConversion;
import android.text.TextUtils;

/**
 * A String wrapper to make it observable. Extension of the {@link BindableObject}. Has
 * overridden {@linkplain #equals(String, String) equals()} method to use {@linkplain
 * TextUtils#equals(CharSequence, CharSequence) TextUtils.equals()} as comparator. Also has
 * {@link #isEmpty} method
 * <p>
 * This object is also necessary to use {@link BindingConversion} features such as generic {@link
 * BindableObject} doesn't work as expected.
 *
 * @author Eugene Popovich
 */
public class BindableString extends BindableObject<String> {

    /**
     * Is the wrapped value empty
     *
     * @return true if the wrapped value is empty
     */
    public boolean isEmpty() {
        return TextUtils.isEmpty(get());
    }

    @Override public boolean equals(String o1, String o2) {
        return TextUtils.equals(o1, o2);
    }
}
