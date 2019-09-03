package com.app.missednotificationsreminder.service.event;

import com.app.missednotificationsreminder.data.model.NotificationData;
import com.app.missednotificationsreminder.util.event.Event;

import java.util.List;

public class NotificationsUpdatedEvent implements Event {
    public final List<NotificationData> notifications;

    public NotificationsUpdatedEvent(List<NotificationData> notifications) {
        this.notifications = notifications;
    }

    @Override public String toString() {
        return new StringBuilder()
                .append("NotificationsUpdatedEvent{")
                .append("notifications=").append(notifications)
                .append("}")
                .toString();
    }
}
