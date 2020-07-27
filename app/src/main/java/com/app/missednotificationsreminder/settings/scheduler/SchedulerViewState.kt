package com.app.missednotificationsreminder.settings.scheduler

import com.app.missednotificationsreminder.util.TimeUtils

/**
 * @property enabled used to handle scheduler enabled state
 * @property mode used to handle scheduler mode
 * @property begin used to handle scheduler range begin information
 * @property end used to handle scheduler range end information
 * @property rangeBegin used to mirror [.begin] field for the RangeBar with the value
 * transformation such as RangeBar has 5 minutes interval specified
 * @property rangeEnd used to mirror [.end] field for the RangeBar with the value
 * transformation such as RangeBar has 5 minutes interval specified
 * @property maximum used to provide maximum possible value information to the RangeBar
 * @property minimum used to provide minimum possible value information to the RangeBar
 */
data class SchedulerViewState(
        val enabled: Boolean = false,
        val mode: Boolean = true,
        val begin: Int = 0,
        val end: Int = 0,
        val rangeBegin: Int = 0,
        val rangeEnd: Int = 0,
        val maximum: Int,
        val minimum: Int) {
    /**
     * used to return scheduler range begin information represented as human readable string
     */
    val beginTime: String
        get() = TimeUtils.minutesToTime(begin)

    /**
     * used to return scheduler range end information represented as human readable string
     */
    val endTime: String
        get() = TimeUtils.minutesToTime(end)
}

