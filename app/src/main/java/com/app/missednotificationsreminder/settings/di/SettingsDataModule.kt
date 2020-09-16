package com.app.missednotificationsreminder.settings.di

import android.content.Context
import android.media.RingtoneManager
import android.os.Vibrator
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.data.model.NightMode
import com.app.missednotificationsreminder.di.qualifiers.*
import com.app.missednotificationsreminder.service.data.model.NotificationData
import com.app.missednotificationsreminder.service.event.NotificationsUpdatedEvent
import com.app.missednotificationsreminder.service.event.RemindEvents
import com.app.missednotificationsreminder.settings.di.qualifiers.*
import com.app.missednotificationsreminder.util.event.Event
import com.app.missednotificationsreminder.util.event.FlowEventBus
import com.tfcporciuncula.flow.FlowSharedPreferences
import com.tfcporciuncula.flow.Preference
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Singleton

/**
 * The Dagger dependency injection module for the data layer
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Module
class SettingsDataModule {

    @Provides
    @Singleton
    @ReminderIntervalMax
    fun provideReminderIntervalMaximum(@ForApplication context: Context): Int {
        return context.resources.getInteger(R.integer.reminderIntervalMaximum)
    }

    @Provides
    @Singleton
    @ReminderIntervalMin
    fun provideReminderIntervalMinimum(@ForApplication context: Context): Int {
        return context.resources.getInteger(R.integer.reminderIntervalMinimum)
    }

    @Provides
    @Singleton
    @ReminderIntervalDefault
    fun provideReminderIntervalDefault(@ForApplication context: Context): Int {
        return context.resources.getInteger(R.integer.reminderIntervalDefault)
    }

    @Provides
    @Singleton
    fun provideNightMode(prefs: FlowSharedPreferences): Preference<NightMode> {
        return prefs.getEnum("NIGHT_MODE", NightMode.FOLLOW_SYSTEM)
    }

    @Provides
    @Singleton
    @LimitReminderRepeats
    fun provideLimitReminderRepeats(prefs: FlowSharedPreferences): Preference<Boolean> {
        return prefs.getBoolean("LIMIT_REMINDER_REPEATS", false)
    }

    @Provides
    @Singleton
    @ReminderInterval
    fun provideReminderInterval(prefs: FlowSharedPreferences, @ReminderIntervalDefault reminderIntervalDefault: Int): Preference<Int> {
        return prefs.getInt("REMINDER_INTERVAL", reminderIntervalDefault)
    }

    @Provides
    @Singleton
    @ReminderRepeats
    fun provideReminderRepeats(prefs: FlowSharedPreferences, @ReminderRepeatsDefault reminderRepeatsDefault: Int): Preference<Int> {
        return prefs.getInt("REMINDER_REPEATS", reminderRepeatsDefault)
    }

    @Provides
    @Singleton
    @ReminderRepeatsDefault
    fun provideReminderRepeatsDefault(@ForApplication context: Context): Int {
        return context.resources.getInteger(R.integer.reminderRepeatsDefault)
    }

    @Provides
    @Singleton
    @ReminderRepeatsMax
    fun provideReminderRepeatsMaximum(@ForApplication context: Context): Int {
        return context.resources.getInteger(R.integer.reminderRepeatsMaximum)
    }

    @Provides
    @Singleton
    @ReminderRepeatsMin
    fun provideReminderRepeatsMinimum(@ForApplication context: Context): Int {
        return context.resources.getInteger(R.integer.reminderRepeatsMinimum)
    }

    @Provides
    @Singleton
    @CreateDismissNotification
    fun provideCreateDismissNotification(prefs: FlowSharedPreferences): Preference<Boolean> {
        return prefs.getBoolean("CREATE_DISMISS_NOTIFICATION", true)
    }

    @Provides
    @Singleton
    @CreateDismissNotificationImmediately
    fun provideCreateDismissNotificationImmediately(prefs: FlowSharedPreferences): Preference<Boolean> {
        return prefs.getBoolean("CREATE_DISMISS_NOTIFICATION_IMMEDIATELY", true)
    }

    @Provides
    @Singleton
    @ForceWakeLock
    fun provideForceWakeLock(prefs: FlowSharedPreferences): Preference<Boolean> {
        return prefs.getBoolean(ForceWakeLock::class.java.simpleName, false)
    }

    @Provides
    @Singleton
    @ReminderRingtone
    fun provideReminderRingtone(prefs: FlowSharedPreferences): Preference<String> {
        val defaultRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        return prefs.getString("REMINDER_RINGTONE", defaultRingtone?.toString() ?: "")
    }

    @Provides
    @Singleton
    @Vibrate
    fun provideVibrate(prefs: FlowSharedPreferences, vibrator: Vibrator): Preference<Boolean> {
        return prefs.getBoolean(Vibrate::class.java.simpleName, vibrator.hasVibrator())
    }

    @Provides
    @Singleton
    @VibrationPatternDefault
    fun provideVibrationPatternDefault(@ForApplication context: Context): String {
        return context.resources.getString(R.string.vibrationPatternDefault)
    }

    @Provides
    @Singleton
    @VibrationPattern
    fun provideVibrationPattern(prefs: FlowSharedPreferences, @VibrationPatternDefault vibrationPatternDefault: String): Preference<String> {
        return prefs.getString("VIBRATION_PATTERN", vibrationPatternDefault)
    }

    @Provides
    @Singleton
    @SelectedApplications
    fun provideSelectedApplications(prefs: FlowSharedPreferences): Preference<Set<String>> {
        return prefs.getStringSet("SELECTED_APPLICATIONS")
    }

    @Provides
    @Singleton
    @IgnorePersistentNotifications
    fun provideIgnorePersistentNotifications(prefs: FlowSharedPreferences): Preference<Boolean> {
        return prefs.getBoolean(IgnorePersistentNotifications::class.java.name, true)
    }

    @Provides
    @Singleton
    @RespectPhoneCalls
    fun provideRespectPhoneCalls(prefs: FlowSharedPreferences): Preference<Boolean> {
        return prefs.getBoolean(RespectPhoneCalls::class.java.name, true)
    }

    @Provides
    @Singleton
    @RespectRingerMode
    fun provideRespectRingerMode(prefs: FlowSharedPreferences): Preference<Boolean> {
        return prefs.getBoolean(RespectRingerMode::class.java.name, true)
    }

    @Provides
    @Singleton
    @RemindWhenScreenIsOn
    fun provideRemindWhenScreenIsOn(prefs: FlowSharedPreferences): Preference<Boolean> {
        return prefs.getBoolean(RemindWhenScreenIsOn::class.java.name, true)
    }

    @Provides
    @Singleton
    @ReminderEnabled
    fun provideReminderEnabled(prefs: FlowSharedPreferences): Preference<Boolean> {
        return prefs.getBoolean("REMINDER_ENABLED", true)
    }

    @Provides
    @Singleton
    @SchedulerEnabled
    fun provideSchedulerEnabled(prefs: FlowSharedPreferences): Preference<Boolean> {
        return prefs.getBoolean("SCHEDULER_ENABLED", false)
    }

    @Provides
    @Singleton
    @SchedulerMode
    fun provideSchedulerMode(prefs: FlowSharedPreferences): Preference<Boolean> {
        return prefs.getBoolean("SCHEDULER_MODE", true)
    }

    @Provides
    @Singleton
    @SchedulerRangeMax
    fun provideSchedulerRangeMaximum(@ForApplication context: Context): Int {
        return context.resources.getInteger(R.integer.schedulerRangeMaximum)
    }

    @Provides
    @Singleton
    @SchedulerRangeMin
    fun provideSchedulerRangeMinimum(@ForApplication context: Context): Int {
        return context.resources.getInteger(R.integer.schedulerRangeMinimum)
    }

    @Provides
    @Singleton
    @SchedulerRangeDefaultBegin
    fun provideSchedulerDefaultBegin(@ForApplication context: Context): Int {
        return context.resources.getInteger(R.integer.schedulerRangeDefaultBegin)
    }

    @Provides
    @Singleton
    @SchedulerRangeDefaultEnd
    fun provideSchedulerDefaultEnd(@ForApplication context: Context): Int {
        return context.resources.getInteger(R.integer.schedulerRangeDefaultEnd)
    }

    @Provides
    @Singleton
    @SchedulerRangeBegin
    fun provideSchedulerRangeBegin(prefs: FlowSharedPreferences, @SchedulerRangeDefaultBegin schedulerRangeDefaultBegin: Int): Preference<Int> {
        return prefs.getInt("SCHEDULER_RANGE_BEGIN", schedulerRangeDefaultBegin)
    }

    @Provides
    @Singleton
    @SchedulerRangeEnd
    fun provideSchedulerRangeEnd(prefs: FlowSharedPreferences, @SchedulerRangeDefaultEnd schedulerRangeDefaultEnd: Int): Preference<Int> {
        return prefs.getInt("SCHEDULER_RANGE_END", schedulerRangeDefaultEnd)
    }

    @FlowPreview
    @Provides
    fun provideNotificationDataFlow(eventBus: FlowEventBus): Flow<List<NotificationData>> {
        Timber.d("provideNotificationDataFlow() called with: eventBus = %s",
                eventBus)
        return eventBus.toFlow()
                .filter { event: Event -> event is NotificationsUpdatedEvent }
                .map { event: Event -> (event as NotificationsUpdatedEvent).notifications }
                .onStart {
                    emit(emptyList<NotificationData>())
                    eventBus.send(RemindEvents.GET_CURRENT_NOTIFICATIONS_DATA)
                }
                .onEach { data: List<NotificationData> -> Timber.d("notificationDataFlow: %d", data.size) }
                .debounce(500)
    }
}