package com.app.missednotificationsreminder.util;

import java.util.Calendar;

/**
 * Various time related utilities
 *
 * @author Eugene Popovich
 */
public class TimeUtils {
    /**
     * Amount of seconds in one minute
     */
    public static final int SECONDS_IN_MINUTE = 60;
    /**
     * Amount of minutes in one hour
     */
    public static final int MINUTES_IN_HOUR = 60;
    /**
     * Amount of millis on one second
     */
    public static final int MILLIS_IN_SECOND = 1000;
    /**
     * Amount of milliseconds in one minute
     */
    public static final int MILLIS_IN_MINUTE = SECONDS_IN_MINUTE * MILLIS_IN_SECOND;

    /**
     * Available types used in the getNearestTime method
     */
    public static enum NearestTimeType {
        FUTURE,
        PAST
    }

    /**
     * Available types used in the getScheduledTime method
     */
    public static enum SchedulerMode {
        WORKING_PERIOD,
        NON_WORKING_PERIOD
    }

    /**
     * Convert minute of the day value to the human readable string with hours and minutes information.
     * Example 125 will be converted to "02:05"
     *
     * @param minutes
     * @return
     */
    public static String minutesToTime(int minutes) {
        int hoursOfDay = minutes / MINUTES_IN_HOUR;
        int minutesOfDay = minutes % MINUTES_IN_HOUR;
        return String.format("%1$02d:%2$02d", hoursOfDay, minutesOfDay);
    }

    /**
     * Get the nearest past timestamp for the specified related time which has a same time as a minutesOfDay param
     *
     * @param minutesOfDay the minutes of day value for the searching timestamp
     * @param relatedTime  the timestamp the result should be nearest to
     * @return
     */
    public static long getNearestPastTime(int minutesOfDay, long relatedTime) {
        return getNearestTime(minutesOfDay, NearestTimeType.PAST, relatedTime);
    }

    /**
     * Get the nearest future timestamp for the specified related time which has a same time as a minutesOfDay param
     *
     * @param minutesOfDay the minutes of day value for the searching timestamp
     * @param relatedTime  the timestamp the result should be nearest to
     * @return
     */
    public static long getNearestFutureTime(int minutesOfDay, long relatedTime) {
        return getNearestTime(minutesOfDay, NearestTimeType.FUTURE, relatedTime);
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
    public static long getNearestTime(int minutesOfDay, NearestTimeType type, long relatedTime) {
        int hourOfDay = minutesOfDay / MINUTES_IN_HOUR;
        int minutesOfHour = minutesOfDay % MINUTES_IN_HOUR;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(relatedTime);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        int calHourOfDay = cal.get(Calendar.HOUR_OF_DAY);
        switch (type) {
            case FUTURE:
                if (calHourOfDay > hourOfDay || (calHourOfDay == hourOfDay && cal.get(Calendar.MINUTE) > minutesOfHour)) {
                    // if related time hour of day are more than the searching timestamp should have or the related time hour is same
                    // but the related minutes are more than the searching timestamp
                    cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) + 1);
                }
                break;
            case PAST:
                if (calHourOfDay < hourOfDay || (calHourOfDay == hourOfDay && cal.get(Calendar.MINUTE) < minutesOfHour)) {
                    // if related time hour of day are less than the searching timestamp should have or the related time hour is same
                    // but the related minutes are less than the searching timestamp
                    cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) - 1);
                }
                break;
        }
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minutesOfHour);
        return cal.getTimeInMillis();
    }

    /**
     * Get the timestamp for the same day as relatedTime has and the specified minutesOfDay value
     *
     * @param minutesOfDay the minutes of day value for the searching timestamp
     * @param relatedTime  the timestamp the result should have the same day as
     * @return
     */
    public static long getDayTime(int minutesOfDay, long relatedTime) {
        int hourOfDay = minutesOfDay / MINUTES_IN_HOUR;
        int minutesOfHour = minutesOfDay % MINUTES_IN_HOUR;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(relatedTime);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minutesOfHour);
        return cal.getTimeInMillis();
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
    public static long getScheduledTime(SchedulerMode schedulerMode,
                                        int schedulerRangeBeginMinutes,
                                        int schedulerRangeEndMinutes,
                                        long nextWakeupTime) {
        long scheduledTime = 0;
        long todayRangeBegin = TimeUtils.getDayTime(schedulerRangeBeginMinutes, nextWakeupTime);
        long todayRangeEnd = TimeUtils.getDayTime(schedulerRangeEndMinutes, nextWakeupTime);
        switch (schedulerMode) {
            case WORKING_PERIOD:
                if (nextWakeupTime < todayRangeBegin) {
                    // if next possible wakup time is less than the scheduler begin time
                    scheduledTime = todayRangeBegin;
                } else if (todayRangeEnd < nextWakeupTime) {
                    // if the scheduler end time is less than the next possible wakeup time
                    scheduledTime = TimeUtils.getNearestFutureTime(schedulerRangeBeginMinutes, nextWakeupTime);
                    if (scheduledTime < nextWakeupTime) {
                        // if the nearest future time is before the next possible waking time we should to keep
                        // minimum interval, so reset value
                        scheduledTime = 0;
                    }
                }
                break;
            case NON_WORKING_PERIOD:
                if (todayRangeBegin <= nextWakeupTime && nextWakeupTime < todayRangeEnd) {
                    // if the scheduler begin time is less or equals to the next possible wakeup time and the next
                    // possible wakeup time is less than the scheduler end time
                    scheduledTime = todayRangeEnd;
                }
                break;
        }
        return scheduledTime;
    }

    /**
     * Convert seconds to minutes
     *
     * @param seconds seconds to convert to minutes
     * @return
     */
    public static float secondsToMinutes(int seconds) {
        return secondsToMinutes(seconds, RoundType.ROUND);
    }

    /**
     * Convert seconds to minutes
     *
     * @param seconds   seconds to convert to minutes
     * @param roundType the round type which should be used for the conversion
     * @return
     */
    public static float secondsToMinutes(int seconds, RoundType roundType) {
        float result = (float) seconds / SECONDS_IN_MINUTE;
        switch (roundType) {
            case FLOOR:
                return (float) Math.floor(result * 100) / 100;
            case CEIL:
                return (float) Math.ceil(result * 100) / 100;
            case ROUND:
                return (float) Math.round(result * 100) / 100;
            default:
                return result;
        }
    }

    /**
     * Convert minutes to seconds
     *
     * @param minutes the minutes to convert to seconds
     * @return
     */
    public static int minutesToSeconds(float minutes) {
        return Math.round(minutes * SECONDS_IN_MINUTE);
    }

    /**
     * Round types available for the time units conversion
     */
    public enum RoundType {
        FLOOR,
        CEIL,
        ROUND
    }
}
