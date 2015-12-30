package com.app.missednotificationsreminder.data;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;

import com.app.missednotificationsreminder.R;
import com.app.missednotificationsreminder.di.qualifiers.ForApplication;
import com.app.missednotificationsreminder.di.qualifiers.IoThreadScheduler;
import com.app.missednotificationsreminder.di.qualifiers.MainThreadScheduler;
import com.app.missednotificationsreminder.di.qualifiers.ReminderEnabled;
import com.app.missednotificationsreminder.di.qualifiers.ReminderInterval;
import com.app.missednotificationsreminder.di.qualifiers.ReminderIntervalDefault;
import com.app.missednotificationsreminder.di.qualifiers.ReminderIntervalMax;
import com.app.missednotificationsreminder.di.qualifiers.ReminderIntervalMin;
import com.app.missednotificationsreminder.di.qualifiers.ReminderRingtone;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerEnabled;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerMode;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerRangeBegin;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerRangeDefaultBegin;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerRangeDefaultEnd;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerRangeEnd;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerRangeMax;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerRangeMin;
import com.app.missednotificationsreminder.di.qualifiers.SelectedApplications;
import com.f2prateek.rx.preferences.Preference;
import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.squareup.picasso.Picasso;

import java.util.Set;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static android.content.Context.MODE_PRIVATE;

/**
 * The Dagger dependency injection module for the data layer
 */
@Module(
        complete = false,
        library = true
)
public final class DataModule {

    /**
     * Key used for the selected applications preference
     */
    static final String SELECTED_APPLICATIONS_PREF = "SELECTED_APPLICATIONS";
    /**
     * Key used for the reminder enabled preference
     */
    static final String REMINDER_ENABLED_PREF = "REMINDER_ENABLED";
    /**
     * Key used for the reminder interval preference
     */
    static final String REMINDER_INTERVAL_PREF = "REMINDER_INTERVAL";
    /**
     * Key used for the reminder ringtone preference
     */
    static final String REMINDER_RINGTONE_PREF = "REMINDER_RINGTONE";
    /**
     * Key used for the scheduler enabled preference
     */
    static final String SCHEDULER_ENABLED_PREF = "SCHEDULER_ENABLED";
    /**
     * Key used for the scheduler mode preference
     */
    static final String SCHEDULER_MODE_PREF = "SCHEDULER_MODE";
    /**
     * Key used for the scheduler range begin preference
     */
    static final String SCHEDULER_RANGE_BEGIN_PREF = "SCHEDULER_RANGE_BEGIN";
    /**
     * Key used for the scheduler range end preference
     */
    static final String SCHEDULER_RANGE_END_PREF = "SCHEDULER_RANGE_END";
    /**
     * The shared preferences name
     */
    public static final String PREFERENCES_NAME = "missingnotificationreminder";

    @Provides @Singleton @MainThreadScheduler Scheduler provideMainThreadScheduler() {
        return AndroidSchedulers.mainThread();
    }

    @Provides @Singleton @IoThreadScheduler Scheduler provideIoThreadScheduler() {
        return Schedulers.io();
    }

    @Provides @Singleton SharedPreferences provideSharedPreferences(Application app) {
        return app.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
    }

    @Provides @Singleton RxSharedPreferences provideRxSharedPreferences(SharedPreferences prefs) {
        return RxSharedPreferences.create(prefs);
    }

    @Provides @Singleton @ReminderIntervalMax int provideReminderIntervalMaximum(@ForApplication Context context) {
        return context.getResources().getInteger(R.integer.reminderIntervalMaximum);
    }

    @Provides @Singleton @ReminderIntervalMin int provideReminderIntervalMinimum(@ForApplication Context context) {
        return context.getResources().getInteger(R.integer.reminderIntervalMinimum);
    }

    @Provides @Singleton @ReminderIntervalDefault int provideReminderIntervalDefault(@ForApplication Context context) {
        return context.getResources().getInteger(R.integer.reminderIntervalDefault);
    }

    @Provides @Singleton @ReminderInterval Preference<Integer> provideReminderInterval
            (RxSharedPreferences prefs, @ReminderIntervalDefault int reminderIntervalDefault) {
        return prefs.getInteger(REMINDER_INTERVAL_PREF, reminderIntervalDefault);
    }

    @Provides @Singleton @ReminderRingtone Preference<String> provideReminderRingtone(RxSharedPreferences prefs) {
        Uri defaultRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        return prefs.getString(REMINDER_RINGTONE_PREF, defaultRingtone == null ? "" : defaultRingtone.toString());
    }

    @Provides @Singleton @SelectedApplications Preference<Set<String>> provideSelectedApplications(RxSharedPreferences prefs) {
        return prefs.getStringSet(SELECTED_APPLICATIONS_PREF);
    }

    @Provides @Singleton @ReminderEnabled Preference<Boolean> provideReminderEnabled(RxSharedPreferences prefs) {
        return prefs.getBoolean(REMINDER_ENABLED_PREF, true);
    }

    @Provides @Singleton @SchedulerEnabled Preference<Boolean> provideSchedulerEnabled(RxSharedPreferences prefs) {
        return prefs.getBoolean(SCHEDULER_ENABLED_PREF, false);
    }

    @Provides @Singleton @SchedulerMode Preference<Boolean> provideSchedulerMode(RxSharedPreferences prefs) {
        return prefs.getBoolean(SCHEDULER_MODE_PREF, true);
    }

    @Provides @Singleton @SchedulerRangeMax int provideSchedulerRangeMaximum(@ForApplication Context context) {
        return context.getResources().getInteger(R.integer.schedulerRangeMaximum);
    }

    @Provides @Singleton @SchedulerRangeMin int provideSchedulerRangeMinimum(@ForApplication Context context) {
        return context.getResources().getInteger(R.integer.schedulerRangeMinimum);
    }

    @Provides @Singleton @SchedulerRangeDefaultBegin int provideSchedulerDefaultBegin(@ForApplication Context context) {
        return context.getResources().getInteger(R.integer.schedulerRangeDefaultBegin);
    }

    @Provides @Singleton @SchedulerRangeDefaultEnd int provideSchedulerDefaultEnd(@ForApplication Context context) {
        return context.getResources().getInteger(R.integer.schedulerRangeDefaultEnd);
    }

    @Provides @Singleton @SchedulerRangeBegin Preference<Integer> provideSchedulerRangeBegin
            (RxSharedPreferences prefs, @SchedulerRangeDefaultBegin int schedulerRangeDefaultBegin) {
        return prefs.getInteger(SCHEDULER_RANGE_BEGIN_PREF, schedulerRangeDefaultBegin);
    }

    @Provides @Singleton @SchedulerRangeEnd Preference<Integer> provideSchedulerRangeEnd
            (RxSharedPreferences prefs, @SchedulerRangeDefaultEnd int schedulerRangeDefaultEnd) {
        return prefs.getInteger(SCHEDULER_RANGE_END_PREF, schedulerRangeDefaultEnd);
    }

    @Provides @Singleton PackageManager providePackageManager(Application app) {
        return app.getPackageManager();
    }

    @Provides @Singleton Picasso providePicasso(Application app) {
        return new Picasso.Builder(app)
                .listener((picasso, uri, e) -> Timber.e(e, "Failed to load image: %s", uri))
                .build();
    }

}
