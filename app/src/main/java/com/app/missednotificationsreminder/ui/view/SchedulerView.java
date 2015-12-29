package com.app.missednotificationsreminder.ui.view;

import com.app.missednotificationsreminder.binding.util.BindableObject;

/**
 * Scheduler settings view interface
 *
 * @author Eugene Popovich
 */
public interface SchedulerView {

    /**
     * Start the time selection picker
     *
     * @param minutes the current minutes of day value holder
     * @param minMinutes the minimum possible minutes of day value
     * @param maxMinutes the maximum possible minutes of day value
     */
    void selectTime(BindableObject<Integer> minutes, int minMinutes, int maxMinutes);
}
