package com.app.missednotificationsreminder.service.event;

import com.app.missednotificationsreminder.service.data.model.NotificationData;
import com.app.missednotificationsreminder.util.event.Event;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NotificationsUpdatedEvent implements Event {
    public final List<NotificationData> notifications;

    public NotificationsUpdatedEvent(List<NotificationData> notifications) {
        this.notifications = notifications;
    }

    @NotNull @Override public String toString() {
        return new StringBuilder()
                .append("NotificationsUpdatedEvent{")
                .append("notifications=").append(notifications)
                .append("}")
                .toString();
    }
}
