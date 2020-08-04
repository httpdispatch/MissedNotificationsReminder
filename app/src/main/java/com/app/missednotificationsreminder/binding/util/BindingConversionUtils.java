package com.app.missednotificationsreminder.binding.util;

import androidx.databinding.BindingConversion;
import android.view.View;

/**
 * Utility class which stores Android Data Binding framework related binding conversion rules
 *
 * @author Eugene Popovich
 */
public class BindingConversionUtils {

    /**
     * Rule to convert {@link Boolean} object to int visibility code:
     * either {@link View#VISIBLE}  or {@link View#GONE}
     *
     * @param visible the boolean value to convert
     * @return the visibility code depend on visible value
     */
    @BindingConversion public static int convertBooleanToVisibilityState(Boolean visible) {
        return visible != null && visible ? View.VISIBLE : View.GONE;
    }
}
