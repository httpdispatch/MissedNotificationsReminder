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
     * Rule to convert {@link BindableString} object to {@link String}
     *
     * @param observable the observable string to convert
     * @return the String value wrapped by the observable parameter
     */
    @BindingConversion public static String convertBindableToString(
            BindableString observable) {
        return observable.get();
    }

    /**
     * Rule to convert {@link BindableBoolean} object to {@link Boolean}
     *
     * @param observable the observable boolean to convert
     * @return the Boolean value wrapped by the observable parameter
     */
    @BindingConversion public static Boolean convertBindableToBoolean(
            BindableBoolean observable) {
        return observable == null ? false : observable.get();
    }


    /**
     * Rule to convert {@link BindableBoolean} object to int visibility code:
     * either {@link View#VISIBLE} or {@link View#GONE}
     *
     * @param observable the observable boolean to convert
     * @return the the visibility code depend on visible value wrapped by the observable parameter
     */
    @BindingConversion public static int convertBindingToVisibility(BindableBoolean observable) {
        return convertBooleanToVisibilityState(observable == null ? null : observable.get());
    }

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
