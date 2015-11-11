package com.app.missednotificationsreminder.binding.util;

import android.databinding.BindingConversion;

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
        return observable.get();
    }
}
