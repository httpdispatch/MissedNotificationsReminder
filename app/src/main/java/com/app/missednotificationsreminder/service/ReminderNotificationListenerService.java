package com.app.missednotificationsreminder.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.view.Display;

import com.app.missednotificationsreminder.di.Injector;
import com.app.missednotificationsreminder.di.qualifiers.ReminderEnabled;
import com.app.missednotificationsreminder.di.qualifiers.ReminderInterval;
import com.app.missednotificationsreminder.di.qualifiers.SelectedApplications;
import com.f2prateek.rx.preferences.Preference;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import dagger.ObjectGraph;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * The service to monitor all status bar notifications. It performs periodical sound notification depend on whether
 * there are available notifications from applications which matches user selected applications. The notification interval
 * is also specified by the user in the corresponding window.
 *
 * @author Eugene Popovich
 */
public class ReminderNotificationListenerService extends NotificationListenerService {
    /**
     * Action for the pending intent used by alarm manager to periodically wake the device and send broadcast with this
     * action
     */
    static final String PENDING_INTENT_ACTION = ReminderNotificationListenerService.class.getCanonicalName();
    /**
     * Amount of milliseconds in one minute
     */
    static final int MILLIS_IN_MINUTE = 60 * 1000;
    /**
     * The constant used to identify handler message to start checking of the service waking conditions
     */
    static final int CHECK_WAKING_CONDITIONS_MSG = 0;

    @Inject @ReminderEnabled Preference<Boolean> reminderEnabled;
    @Inject @ReminderInterval Preference<Integer> reminderInterval;
    @Inject @SelectedApplications Preference<Set<String>> selectedApplications;

    /**
     * Alarm manager to schedule/cancel periodical actions
     */
    AlarmManager mAlarmManager;
    /**
     * The pending intent used by alarm manager to wake the service
     */
    PendingIntent mPendingIntent;

    /**
     * The flag to indicate periodical notification active state
     */
    private AtomicBoolean mActive = new AtomicBoolean();
    /**
     * Composite subscription used to handle subscriptions added in this service
     */
    private CompositeSubscription mSubscriptions = new CompositeSubscription();
    /**
     * Receiver used to handle actions from the pending intent used for periodical alarms
     */
    private ScheduledSoundNotificationReceiver mPendingIntentReceiver;
    /**
     * The handler used to process various service related messages
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CHECK_WAKING_CONDITIONS_MSG:
                    Timber.d("CHECK_WAKING_CONDITIONS_MSG message received");
                    checkWakingConditions();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("onCreate");

        // inject dependencies
        ObjectGraph appGraph = Injector.obtain(getApplication());
        appGraph.inject(this);

        // initialize broadcast receiver
        mPendingIntentReceiver = new ScheduledSoundNotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(PENDING_INTENT_ACTION);
        registerReceiver(mPendingIntentReceiver, filter);

        // initialize alarm manager and pending intent
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent i = new Intent(PENDING_INTENT_ACTION);
        mPendingIntent = PendingIntent.getBroadcast(this, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

        // initialize preferences changes listeners
        mSubscriptions.add(
                reminderEnabled.asObservable()
                        .skip(1) // skip initial value emitted right after the subscription
                        .filter(enabled -> enabled) // if reminder enabled
                        .subscribe(b -> sendCheckWakingConditionsCommand()));
        mSubscriptions.add(
                reminderEnabled.asObservable()
                        .skip(1) // skip initial value emitted right after the subscription
                        .filter(enabled -> !enabled) // if reminder disabled
                        .subscribe(b -> stopWaking()));
        mSubscriptions.add(
                reminderInterval.asObservable()
                        .skip(1) // skip initial value emitted right after the subscription
                        .subscribe(data -> {
                            Timber.d("Reminder interval changed");
                            // restart alarm with new conditions if necessary
                            stopWaking();
                            sendCheckWakingConditionsCommand();
                        }));
        mSubscriptions.add(
                selectedApplications.asObservable()
                        .skip(1) // skip initial value emitted right after the subscription
                        .subscribe(data -> {
                            Timber.d("Selected applications changed");
                            // restart alarm with new conditions if necessary
                            stopWaking();
                            sendCheckWakingConditionsCommand();
                        }));
        sendCheckWakingConditionsCommand();
    }

    /**
     * Send the check waing condition message to the service handler
     */
    private void sendCheckWakingConditionsCommand() {
        mHandler.sendMessage(mHandler.obtainMessage(CHECK_WAKING_CONDITIONS_MSG));
    }

    /**
     * Check whether the waking alarm should be scheduled or no
     */
    private void checkWakingConditions() {
        try {
            Timber.d("checkWakingConditions");
            if (mActive.get()) {
                Timber.d("checkWakingConditions: already active, skipping");
                return;
            }
            if (!reminderEnabled.get()) {
                Timber.d("checkWakingConditions: disabled, skipping");
                return;
            }
            boolean schedule = false;

            for (StatusBarNotification notification : getActiveNotifications()) {
                schedule |= checkNotification(notification);
                if (schedule) {
                    break;
                }
            }

            if (schedule) {
                Timber.d("checkWakingConditions: Schedule reminder for %1$d minutes",
                        reminderInterval.get());
                // remember active state
                mActive.set(true);
                scheduleNextWakup();

            } else {
                Timber.d("checkWakingConditions: there are no notifications from selected applications to periodically remind");
            }
        } catch (Throwable t) {
            Timber.e(t, "Unexpected failure");
        }
    }

    /**
     * Schedule wakup alarm for the sound notification pending intent
     */
    private void scheduleNextWakup() {
        Timber.d("scheduleNextWakup: called");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + reminderInterval.get() * MILLIS_IN_MINUTE,
                    mPendingIntent);
        } else {
            mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + reminderInterval.get() * MILLIS_IN_MINUTE,
                    mPendingIntent);
        }
    }

    /**
     * Check whether the notification matches to any selected application
     *
     * @param notification the notification to check
     * @return true if found same package name as notification has in the list of selected applications
     */
    private boolean checkNotification(StatusBarNotification notification) {
        Timber.d("checkNotification: called for package %1$s", notification.getPackageName());
        boolean result = false;
        for (String packageName : selectedApplications.get()) {
            if (TextUtils.equals(notification.getPackageName(), packageName)) {
                Timber.d("checkNotification: found match with selected applications");
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * Stop scheduled wakeup alarm for the periodical sound notification
     */
    private void stopWaking() {
        stopWaking(false);
    }

    /**
     * Stop scheduled wakeup alarm for the periodical sound notification
     *
     * @param force whether to do force cancel independently of the active flag value. Needed for active development
     *              when the pending intent may be changed or action scheduled by previous app run.
     */
    private void stopWaking(boolean force) {
        Timber.d("stopWaking");
        if (mActive.compareAndSet(true, false) || force) {
            Timber.d("stopWaking: cancel reminder");
            mAlarmManager.cancel(mPendingIntent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("onDestroy");
        // stop any scheduled alarms
        stopWaking();
        // unregister pending intent receiver
        unregisterReceiver(mPendingIntentReceiver);

        mSubscriptions.unsubscribe();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Timber.d("onNotificationPosted: for package %1$s", sbn.getPackageName());
        sendCheckWakingConditionsCommand();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Timber.d("onNotificationRemoved: for package %1$s", sbn.getPackageName());
        // stop alarm and check whether it should be launched again
        stopWaking();
        sendCheckWakingConditionsCommand();
    }

    /**
     * The broadcast receiver for the pending intent action fired by alarm manager
     */
    class ScheduledSoundNotificationReceiver extends BroadcastReceiver {
        /**
         * Media player used to play notification sound
         */
        MediaPlayer mMediaPlayer = null;

        ScheduledSoundNotificationReceiver() {
            // initialize media player
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            mMediaPlayer.setOnPreparedListener(mp -> mp.start());
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("onReceive");
            if (!mActive.get()) {
                Timber.w("Invalid service activity state, stopping reminder");
                stopWaking(true);
                return;
            }
            if (!isScreenOn(context)) {
                Timber.d("The screen is off, notify");
                try {
                    if (mMediaPlayer.isPlaying()) {
                        Timber.d("Media player is playing. Stopping...");
                        mMediaPlayer.stop();
                    }
                    mMediaPlayer.reset();
                    // get the default notification sound URI
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    mMediaPlayer.setDataSource(getApplicationContext(), notification);
                    mMediaPlayer.prepareAsync();
                } catch (Exception ex) {
                    Timber.e(ex, null);
                    throw new RuntimeException(ex);
                }
            } else {
                Timber.d("The screen is on, skip notification");
            }
            scheduleNextWakup();
        }

        /**
         * Is the screen of the device on.
         *
         * @param context the context
         * @return true when (at least one) screen is on
         */
        public boolean isScreenOn(Context context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
                boolean screenOn = false;
                for (Display display : dm.getDisplays()) {
                    if (display.getState() != Display.STATE_OFF) {
                        screenOn = true;
                    }
                }
                return screenOn;
            } else {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                //noinspection deprecation
                return pm.isScreenOn();
            }
        }
    }

}
