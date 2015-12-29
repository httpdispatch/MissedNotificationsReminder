package com.app.missednotificationsreminder.util;

import com.app.missednotificationsreminder.util.TimeUtils;

import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static com.google.common.truth.Truth.assertThat;

/**
 * Various test cases for the {@link TimeUtils} class
 *
 * @author Eugene Popovich
 */
public class TimeUtilsTest {

    @Test public void testGetScheduledTime() {
        Calendar cal;
        int minuteOfDay;
        long scheduledTime;

        int year, month, day, hour, minute, minute2, interval;
        year = 2015;
        month = Calendar.JANUARY;
        day = 1;
        hour = 17;
        minute = 40;
        int rangeBegin = getMinuteOfDay(7, 0);
        int rangeEnd = getMinuteOfDay(21, 0);

        {
            interval = 1;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            scheduledTime = cal.getTimeInMillis() + interval * TimeUtils.MILLIS_IN_MINUTE;
            assertThat(TimeUtils.getScheduledTime(TimeUtils.SchedulerMode.WORKING_PERIOD, rangeBegin, rangeEnd, scheduledTime)).isEqualTo(0);
        }
        {
            hour = 20;
            interval = 21;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            scheduledTime = cal.getTimeInMillis() + interval * TimeUtils.MILLIS_IN_MINUTE;
            cal.setTimeInMillis(TimeUtils.getScheduledTime(TimeUtils.SchedulerMode.WORKING_PERIOD, rangeBegin, rangeEnd, scheduledTime));
            checkCalendar(cal, year, month, day + 1, 7, 0);
        }
        {
            hour = 6;
            interval = 19;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            scheduledTime = cal.getTimeInMillis() + interval * TimeUtils.MILLIS_IN_MINUTE;
            cal.setTimeInMillis(TimeUtils.getScheduledTime(TimeUtils.SchedulerMode.WORKING_PERIOD, rangeBegin, rangeEnd, scheduledTime));
            checkCalendar(cal, year, month, day, 7, 0);
        }
        {
            hour = 6;
            interval = 20;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            scheduledTime = cal.getTimeInMillis() + interval * TimeUtils.MILLIS_IN_MINUTE;
            assertThat(TimeUtils.getScheduledTime(TimeUtils.SchedulerMode.WORKING_PERIOD, rangeBegin, rangeEnd, scheduledTime)).isEqualTo(0);
        }
        {
            hour = 20;
            interval = 1;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            scheduledTime = cal.getTimeInMillis() + interval * TimeUtils.MILLIS_IN_MINUTE;
            assertThat(TimeUtils.getScheduledTime(TimeUtils.SchedulerMode.WORKING_PERIOD, rangeBegin, rangeEnd, scheduledTime)).isEqualTo(0);
        }

        {
            interval = 1;
            hour = 7;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            scheduledTime = cal.getTimeInMillis() + interval * TimeUtils.MILLIS_IN_MINUTE;
            cal.setTimeInMillis(TimeUtils.getScheduledTime(TimeUtils.SchedulerMode.NON_WORKING_PERIOD, rangeBegin, rangeEnd, scheduledTime));
            checkCalendar(cal, year, month, day, 21, 0);
        }
        {
            interval = 20;
            hour = 6;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            scheduledTime = cal.getTimeInMillis() + interval * TimeUtils.MILLIS_IN_MINUTE;
            cal.setTimeInMillis(TimeUtils.getScheduledTime(TimeUtils.SchedulerMode.NON_WORKING_PERIOD, rangeBegin, rangeEnd, scheduledTime));
            checkCalendar(cal, year, month, day, 21, 0);
        }
        {
            interval = 19;
            hour = 6;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            scheduledTime = cal.getTimeInMillis() + interval * TimeUtils.MILLIS_IN_MINUTE;
            assertThat(TimeUtils.getScheduledTime(TimeUtils.SchedulerMode.NON_WORKING_PERIOD, rangeBegin, rangeEnd, scheduledTime)).isEqualTo(0);
        }
        {
            interval = 20;
            hour = 20;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            scheduledTime = cal.getTimeInMillis() + interval * TimeUtils.MILLIS_IN_MINUTE;
            assertThat(TimeUtils.getScheduledTime(TimeUtils.SchedulerMode.NON_WORKING_PERIOD, rangeBegin, rangeEnd, scheduledTime)).isEqualTo(0);
        }
        {
            interval = 19;
            hour = 20;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            scheduledTime = cal.getTimeInMillis() + interval * TimeUtils.MILLIS_IN_MINUTE;
            cal.setTimeInMillis(TimeUtils.getScheduledTime(TimeUtils.SchedulerMode.NON_WORKING_PERIOD, rangeBegin, rangeEnd, scheduledTime));
            checkCalendar(cal, year, month, day, 21, 0);
        }
        {
            interval = 1;
            hour = 21;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            scheduledTime = cal.getTimeInMillis() + interval * TimeUtils.MILLIS_IN_MINUTE;
            assertThat(TimeUtils.getScheduledTime(TimeUtils.SchedulerMode.NON_WORKING_PERIOD, rangeBegin, rangeEnd, scheduledTime)).isEqualTo(0);
        }
    }

    @Test public void testGetNearestTime() {
        Calendar cal;
        int minuteOfDay;

        int year, month, day, hour, minute, minute2;
        year = 2015;
        month = Calendar.JANUARY;
        day = 1;
        hour = 17;
        minute = 40;

        {
            minute2 = minute;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            minuteOfDay = getMinuteOfDay(hour, minute2);
            cal.setTimeInMillis(TimeUtils.getNearestFutureTime(minuteOfDay, cal.getTimeInMillis()));
            checkCalendar(cal, year, month, day, hour, minute2);
            cal.setTimeInMillis(TimeUtils.getNearestPastTime(minuteOfDay, cal.getTimeInMillis()));
            checkCalendar(cal, year, month, day, hour, minute2);
        }
        {
            minute2 = minute + 5;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            minuteOfDay = getMinuteOfDay(hour, minute2);
            cal.setTimeInMillis(TimeUtils.getNearestFutureTime(minuteOfDay, cal.getTimeInMillis()));
            checkCalendar(cal, year, month, day, hour, minute2);
        }
        {
            minute2 = minute - 5;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            minuteOfDay = getMinuteOfDay(hour, minute2);
            cal.setTimeInMillis(TimeUtils.getNearestFutureTime(minuteOfDay, cal.getTimeInMillis()));
            checkCalendar(cal, year, month, day + 1, hour, minute2);
        }
        {
            minute2 = minute + 5;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            minuteOfDay = getMinuteOfDay(hour - 1, minute2);
            cal.setTimeInMillis(TimeUtils.getNearestFutureTime(minuteOfDay, cal.getTimeInMillis()));
            checkCalendar(cal, year, month, day + 1, hour - 1, minute2);
        }
        {
            minute2 = minute - 5;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            minuteOfDay = getMinuteOfDay(hour, minute2);
            cal.setTimeInMillis(TimeUtils.getNearestPastTime(minuteOfDay, cal.getTimeInMillis()));
            checkCalendar(cal, year, month, day, hour, minute2);
        }
        {
            minute2 = minute + 5;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            minuteOfDay = getMinuteOfDay(hour, minute2);
            cal.setTimeInMillis(TimeUtils.getNearestPastTime(minuteOfDay, cal.getTimeInMillis()));
            checkCalendar(cal, year - 1, Calendar.DECEMBER, 31, hour, minute2);
        }
        {
            minute2 = minute + 5;
            cal = getCalendar(year, month, day, hour, minute, 0, 0);
            minuteOfDay = getMinuteOfDay(hour + 1, minute2);
            cal.setTimeInMillis(TimeUtils.getNearestPastTime(minuteOfDay, cal.getTimeInMillis()));
            checkCalendar(cal, year - 1, Calendar.DECEMBER, 31, hour + 1, minute2);

            minute2 = minute - 5;
            minuteOfDay = getMinuteOfDay(hour + 1, minute2);
            cal.setTimeInMillis(TimeUtils.getNearestFutureTime(minuteOfDay, cal.getTimeInMillis()));
            checkCalendar(cal, year, month, day, hour + 1, minute2);
        }
    }

    private void checkCalendar(Calendar cal, int year, int month, int day, int hour, int minute) {
        assertThat(cal.get(Calendar.YEAR)).isEqualTo(year);
        assertThat(cal.get(Calendar.MONTH)).isEqualTo(month);
        assertThat(cal.get(Calendar.DAY_OF_MONTH)).isEqualTo(day);
        assertThat(cal.get(Calendar.HOUR_OF_DAY)).isEqualTo(hour);
        assertThat(cal.get(Calendar.MINUTE)).isEqualTo(minute);
        assertThat(cal.get(Calendar.SECOND)).isEqualTo(0);
        assertThat(cal.get(Calendar.MILLISECOND)).isEqualTo(0);
    }

    int getMinuteOfDay(int hour, int minutes) {
        return hour * TimeUtils.MINUTES_IN_HOUR + minutes;
    }

    Calendar getCalendar(int year, int month, int dayOfMonth, int hour, int minute, int seconds, int millis) {
        TimeZone tz = TimeZone.getDefault();
        Calendar cal = new GregorianCalendar(tz, Locale.US);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, seconds);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }
}
