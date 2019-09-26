package com.app.missednotificationsreminder.util;

import android.content.Context;
import android.media.AudioManager;

/**
 * Various phone state related utilities
 */
public class PhoneStateUtils {
    /**
     * Check whether there is an active phone call
     * Taken from http://stackoverflow.com/a/17418732/527759
     *
     * @param context the context instance
     * @return
     */
    public static boolean isCallActive(Context context) {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int mode = manager.getMode();
        return mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION || mode == AudioManager.MODE_RINGTONE;
    }
}
