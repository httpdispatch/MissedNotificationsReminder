package com.app.missednotificationsreminder.service.event;

import com.app.missednotificationsreminder.util.event.Event;

/**
 * Available remind events
 */

public enum RemindEvents implements Event {
    /**
     * The remind request event
     */
    REMIND,
    /**
     * The reminder completed notification event
     */
    REMINDER_COMPLETED,
    /**
     * Request current notification data information via {@link NotificationsUpdatedEvent}
     */
    GET_CURRENT_NOTIFICATIONS_DATA
}
