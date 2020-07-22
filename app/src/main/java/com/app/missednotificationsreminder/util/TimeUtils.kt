package com.app.missednotificationsreminder.util

import java.util.*

/**
 * Various time related utilities
 */
object TimeUtils {
    /**
     * Amount of seconds in one minute
     */
    const val SECONDS_IN_MINUTE = 60

    /**
     * Amount of minutes in one hour
     */
    const val MINUTES_IN_HOUR = 60

    /**
     * Amount of millis on one second
     */
    const val MILLIS_IN_SECOND = 1000

    /**
     * Amount of milliseconds in one minute
     */
    const val MILLIS_IN_MINUTE = SECONDS_IN_MINUTE * MILLIS_IN_SECOND

    /**
     * Convert minute of the day value to the human readable string with hours and minutes information.
     * Example 125 will be converted to "02:05"
     *
     * @param minutes
     * @return
     */
    @JvmStatic
    fun minutesToTime(minutes: Int): String {
        val hoursOfDay = minutes / MINUTES_IN_HOUR
        val minutesOfDay = minutes % MINUTES_IN_HOUR
        return String.format("%1$02d:%2$02d", hoursOfDay, minutesOfDay)
    }

    /**
     * Get the nearest past timestamp for the specified related time which has a same time as a minutesOfDay param
     *
     * @param minutesOfDay the minutes of day value for the searching timestamp
     * @param relatedTime  the timestamp the result should be nearest to
     * @return
     */
    @JvmStatic
    fun getNearestPastTime(minutesOfDay: Int, relatedTime: Long): Long {
        return getNearestTime(minutesOfDay, NearestTimeType.PAST, relatedTime)
    }

    /**
     * Get the nearest future timestamp for the specified related time which has a same time as a minutesOfDay param
     *
     * @param minutesOfDay the minutes of day value for the searching timestamp
     * @param relatedTime  the timestamp the result should be nearest to
     * @return
     */
    @JvmStatic
    fun getNearestFutureTime(minutesOfDay: Int, relatedTime: Long): Long {
        return getNearestTime(minutesOfDay, NearestTimeType.FUTURE, relatedTime)
    }

    /**
     * Get the nearest timestamp for the specified related time which has a same time as a minutesOfDay param. Depend of
     * the type parameter it may be either future or past timestamp.
     *
     * @param minutesOfDay the minutes of day value for the searching timestamp
     * @param type         either FUTURE or PAST
     * @param relatedTime  the timestamp the result should be nearest to
     * @return
     */
    fun getNearestTime(minutesOfDay: Int, type: NearestTimeType, relatedTime: Long): Long {
        val hourOfDay = minutesOfDay / MINUTES_IN_HOUR
        val minutesOfHour = minutesOfDay % MINUTES_IN_HOUR
        val cal = Calendar.getInstance()
        cal.timeInMillis = relatedTime
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0
        val calHourOfDay = cal[Calendar.HOUR_OF_DAY]
        when (type) {
            NearestTimeType.FUTURE -> if (calHourOfDay > hourOfDay || calHourOfDay == hourOfDay && cal[Calendar.MINUTE] > minutesOfHour) {
                // if related time hour of day are more than the searching timestamp should have or the related time hour is same
                // but the related minutes are more than the searching timestamp
                cal[Calendar.DAY_OF_YEAR] = cal[Calendar.DAY_OF_YEAR] + 1
            }
            NearestTimeType.PAST -> if (calHourOfDay < hourOfDay || calHourOfDay == hourOfDay && cal[Calendar.MINUTE] < minutesOfHour) {
                // if related time hour of day are less than the searching timestamp should have or the related time hour is same
                // but the related minutes are less than the searching timestamp
                cal[Calendar.DAY_OF_YEAR] = cal[Calendar.DAY_OF_YEAR] - 1
            }
        }
        cal[Calendar.HOUR_OF_DAY] = hourOfDay
        cal[Calendar.MINUTE] = minutesOfHour
        return cal.timeInMillis
    }

    /**
     * Get the timestamp for the same day as relatedTime has and the specified minutesOfDay value
     *
     * @param minutesOfDay the minutes of day value for the searching timestamp
     * @param relatedTime  the timestamp the result should have the same day as
     * @return
     */
    fun getDayTime(minutesOfDay: Int, relatedTime: Long): Long {
        val hourOfDay = minutesOfDay / MINUTES_IN_HOUR
        val minutesOfHour = minutesOfDay % MINUTES_IN_HOUR
        val cal = Calendar.getInstance()
        cal.timeInMillis = relatedTime
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0
        cal[Calendar.HOUR_OF_DAY] = hourOfDay
        cal[Calendar.MINUTE] = minutesOfHour
        return cal.timeInMillis
    }

    /**
     * Get the next scheduled time depend on conditions
     *
     * @param schedulerMode              either WORKING_PERIOD or NON_WORKING_PERIOD
     * @param schedulerRangeBeginMinutes the scheduler begin minutes
     * @param schedulerRangeEndMinutes   the scheduler ending minutes
     * @param nextWakeupTime             the calculated next possible wakeup time
     * @return the next scheduled time in millis if scheduler condition passed, 0 otherwise.
     * If 0 value is returned the nextWakeupTime value or schedule at interval method should be used .
     */
    @JvmStatic
    fun getScheduledTime(schedulerMode: SchedulerMode,
                         schedulerRangeBeginMinutes: Int,
                         schedulerRangeEndMinutes: Int,
                         nextWakeupTime: Long): Long {
        var scheduledTime: Long = 0
        val todayRangeBegin = getDayTime(schedulerRangeBeginMinutes, nextWakeupTime)
        val todayRangeEnd = getDayTime(schedulerRangeEndMinutes, nextWakeupTime)
        when (schedulerMode) {
            SchedulerMode.WORKING_PERIOD -> if (nextWakeupTime < todayRangeBegin) {
                // if next possible wakup time is less than the scheduler begin time
                scheduledTime = todayRangeBegin
            } else if (todayRangeEnd < nextWakeupTime) {
                // if the scheduler end time is less than the next possible wakeup time
                scheduledTime = getNearestFutureTime(schedulerRangeBeginMinutes, nextWakeupTime)
                if (scheduledTime < nextWakeupTime) {
                    // if the nearest future time is before the next possible waking time we should to keep
                    // minimum interval, so reset value
                    scheduledTime = 0
                }
            }
            SchedulerMode.NON_WORKING_PERIOD -> if (todayRangeBegin <= nextWakeupTime && nextWakeupTime < todayRangeEnd) {
                // if the scheduler begin time is less or equals to the next possible wakeup time and the next
                // possible wakeup time is less than the scheduler end time
                scheduledTime = todayRangeEnd
            }
        }
        return scheduledTime
    }

    /**
     * Convert seconds to minutes
     *
     * @param seconds   seconds to convert to minutes
     * @param roundType the round type which should be used for the conversion
     * @return
     */
    @JvmOverloads
    fun secondsToMinutes(seconds: Int, roundType: RoundType = RoundType.ROUND): Double {
        val result = seconds.toFloat() / SECONDS_IN_MINUTE
        return when (roundType) {
            RoundType.FLOOR -> (Math.floor(result * 100.toDouble()).toFloat() / 100).toDouble()
            RoundType.CEIL -> (Math.ceil(result * 100.toDouble()).toFloat() / 100).toDouble()
            RoundType.ROUND -> (Math.round(result * 100).toFloat() / 100).toDouble()
        }
    }

    /**
     * Convert minutes to seconds
     *
     * @param minutes the minutes to convert to seconds
     * @return
     */
    fun minutesToSeconds(minutes: Double): Int {
        return Math.round(minutes * SECONDS_IN_MINUTE).toInt()
    }

    /**
     * Available types used in the getNearestTime method
     */
    enum class NearestTimeType {
        FUTURE, PAST
    }

    /**
     * Available types used in the getScheduledTime method
     */
    enum class SchedulerMode {
        WORKING_PERIOD, NON_WORKING_PERIOD
    }

    /**
     * Round types available for the time units conversion
     */
    enum class RoundType {
        FLOOR, CEIL, ROUND
    }
}