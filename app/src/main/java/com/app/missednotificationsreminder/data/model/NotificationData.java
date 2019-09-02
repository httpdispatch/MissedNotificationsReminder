package com.app.missednotificationsreminder.data.model;

import androidx.core.util.ObjectsCompat;

/**
 * The class to store notification information
 */

public class NotificationData {
    /**
     * The notification related application package name
     */
    public final String packageName;
    /**
     * The time when notification has been found
     */
    public final long foundAtTime;
    /**
     * Notification specific flags
     */
    public final int flags;

    public NotificationData(String packageName, long foundAtTime, int flags) {
        this.packageName = packageName;
        this.foundAtTime = foundAtTime;
        this.flags = flags;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationData that = (NotificationData) o;
        return foundAtTime == that.foundAtTime &&
                flags == that.flags &&
                ObjectsCompat.equals(packageName, that.packageName);
    }

    @Override public String toString() {
        return new StringBuilder()
                .append(getClassName() + "{")
                .append(fieldsAsString())
                .append("}")
                .toString();
    }

    protected String getClassName() {
        return "NotificationData";
    }

    protected String fieldsAsString() {
        return new StringBuilder()
                .append("packageName='").append(packageName).append('\'')
                .append(", foundAtTime=").append(foundAtTime)
                .append(", flags=").append(flags)
                .toString();
    }
}
