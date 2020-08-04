package com.app.missednotificationsreminder.service.data.model

/**
 * The class to store notification information
 */
open class NotificationData(
        /**
         * The notification id
         */
        val id: String,
        /**
         * The notification related application package name
         */
        val packageName: String,
        /**
         * The time when notification has been found
         */
        val foundAtTime: Long,
        /**
         * Notification specific flags
         */
        val flags: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationData

        if (id != other.id) return false
        if (packageName != other.packageName) return false
        if (foundAtTime != other.foundAtTime) return false
        if (flags != other.flags) return false

        return true
    }


    override fun toString(): String {
        return StringBuilder()
                .append(className).append("{")
                .append(fieldsAsString())
                .append("}")
                .toString()
    }

    protected open val className: String
        get() = "NotificationData"

    protected open fun fieldsAsString(): String {
        return StringBuilder()
                .append("id=").append(id)
                .append(", packageName='").append(packageName).append('\'')
                .append(", foundAtTime=").append(foundAtTime)
                .append(", flags=").append(flags)
                .toString()
    }
}