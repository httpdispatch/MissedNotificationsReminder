package com.app.missednotificationsreminder.settings.reminder

import com.app.missednotificationsreminder.util.TimeUtils
import java.util.*

/**
 * @property forceUpdate the counter to force update view state in the view when data validation error occurs
 * @property intervalError used to handle interval error information
 * @property reminderEnabled used to handle reminder enabled state
 * @property forceWakeLock used to handle use wake lock setting
 * @property intervalMinutes used to handle interval value information
 * @property intervalSeconds used to handle interval value information
 * @property minIntervalSeconds
 * @property maxIntervalSeconds
 * @property maxIntervalForWakeLock Maximum possible interval value when the force wake lock functionality
 * is available
 * @property createDismissNotification used to handle whether dismiss notification is to be generated.
 * @property createDismissNotificationImmediately used to handle whether dismiss notification should be generated immediately when conditios are met.
 * @property limitReminderRepeats used to handle whether reminder repeats are to be limited.
 * @property repeats used to handle repeats value information
 * @property minRepeats
 * @property maxRepeats
 * @property seekInterval used to mirror [.interval] field for the interval SeekBar with the
 *      value adjustment such as SeekBar doesn't have minValue parameter
 * @property seekRepeats used to mirror [.repeats] field for the repeats SeekBar with the
 *      value adjustment such as SeekBar doesn't have minValue parameter
 * @property maxIntervalSeekBarValue provide maximum possible interval seekbar value
 */
data class ReminderViewState(
        val forceUpdate: Long = 0,
        val intervalError: String = "",
        val reminderEnabled: Boolean = false,
        val forceWakeLock: Boolean = false,
        val forceWakeLockEnabled: Boolean = false,
        val intervalMinutes: Double = 0.0,
        val intervalSeconds: Int = 0,
        val minIntervalSeconds: Int,
        val maxIntervalSeconds: Int,
        val maxIntervalForWakeLock: Int = 10 * TimeUtils.SECONDS_IN_MINUTE,
        val createDismissNotification: Boolean = false,
        val createDismissNotificationImmediately: Boolean = false,
        val limitReminderRepeats: Boolean = false,
        val repeats: Int = 10,
        val minRepeats: Int,
        val maxRepeats: Int,
        val seekInterval: Int = 0,
        val seekRepeats: Int = 0,
        val maxIntervalSeekBarValue: Int) {
    val intervalMinutesString: String
        get() = "%.${2}f".format(Locale.US, intervalMinutes)

    val repeatsString: String
        get() = repeats.toString()
}