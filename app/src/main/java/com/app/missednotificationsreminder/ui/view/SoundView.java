package com.app.missednotificationsreminder.ui.view;

/**
 * Sound settings view interface
 *
 * @author Eugene Popovich
 */
public interface SoundView {
    /**
     * Start the ringtone picker
     *
     * @param currentRingtoneUri the current ringtone URI
     */
    void selectRingtone(String currentRingtoneUri);
}
