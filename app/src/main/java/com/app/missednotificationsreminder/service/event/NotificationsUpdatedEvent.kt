package com.app.missednotificationsreminder.service.event

import com.app.missednotificationsreminder.service.data.model.NotificationData
import com.app.missednotificationsreminder.util.event.Event

data class NotificationsUpdatedEvent(val notifications: List<NotificationData>) : Event