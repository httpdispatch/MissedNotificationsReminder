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
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.Display;

import com.app.missednotificationsreminder.binding.util.BindableBoolean;
import com.app.missednotificationsreminder.binding.util.BindableObject;
import com.app.missednotificationsreminder.binding.util.RxBindingUtils;
import com.app.missednotificationsreminder.di.Injector;
import com.app.missednotificationsreminder.di.qualifiers.ForceWakeLock;
import com.app.missednotificationsreminder.di.qualifiers.IgnorePersistentNotifications;
import com.app.missednotificationsreminder.di.qualifiers.LimitReminderRepeats;
import com.app.missednotificationsreminder.di.qualifiers.RemindWhenScreenIsOn;
import com.app.missednotificationsreminder.di.qualifiers.ReminderEnabled;
import com.app.missednotificationsreminder.di.qualifiers.ReminderInterval;
import com.app.missednotificationsreminder.di.qualifiers.ReminderIntervalMin;
import com.app.missednotificationsreminder.di.qualifiers.ReminderRepeats;
import com.app.missednotificationsreminder.di.qualifiers.ReminderRingtone;
import com.app.missednotificationsreminder.di.qualifiers.RespectPhoneCalls;
import com.app.missednotificationsreminder.di.qualifiers.RespectRingerMode;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerEnabled;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerMode;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerRangeBegin;
import com.app.missednotificationsreminder.di.qualifiers.SchedulerRangeEnd;
import com.app.missednotificationsreminder.di.qualifiers.SelectedApplications;
import com.app.missednotificationsreminder.di.qualifiers.Vibrate;
import com.app.missednotificationsreminder.util.PhoneStateUtils;
import com.app.missednotificationsreminder.util.TimeUtils;
import com.f2prateek.rx.preferences.Preference;

import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import dagger.ObjectGraph;
import rx.Observable;
import rx.Subscription;
import rx.exceptions.OnErrorThrowable;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * The service to monitor all status bar notifications. It performs periodical sound notification depend on whether
 * there are available notifications from applications which matches user selected applications. The notification interval
 * is also specified by the user in the corresponding window.
 *
 * @author Eugene Popovich
 */
public class ReminderNotificationListenerService extends AbstractReminderNotificationListenerService {
    /**
     * Action for the pending intent used by alarm manager to periodically wake the device and send broadcast with this
     * action
     */
    static final String PENDING_INTENT_ACTION = ReminderNotificationListenerService.class.getCanonicalName();
    /**
     * The constant used to identify handler message to start checking of the service waking conditions
     */
    static final int CHECK_WAKING_CONDITIONS_MSG = 0;

    @Inject @ReminderEnabled Preference<Boolean> reminderEnabled;
    @Inject @ReminderIntervalMin int reminderIntervalMinimum;
    @Inject @ReminderInterval Preference<Integer> reminderInterval;
    @Inject @ReminderRepeats Preference<Integer> reminderRepeats;
    @Inject @LimitReminderRepeats Preference<Boolean> limitReminderRepeats;
    @Inject @ForceWakeLock Preference<Boolean> forceWakeLock;
    @Inject @SelectedApplications Preference<Set<String>> selectedApplications;
    @Inject @IgnorePersistentNotifications Preference<Boolean> ignorePersistentNotifications;
    @Inject @RespectPhoneCalls Preference<Boolean> respectPhoneCalls;
    @Inject @RespectRingerMode Preference<Boolean> respectRingerMode;
    @Inject @RemindWhenScreenIsOn Preference<Boolean> remindWhenScreenIsOn;
    @Inject @SchedulerEnabled Preference<Boolean> schedulerEnabled;
    @Inject @SchedulerMode Preference<Boolean> schedulerMode;
    @Inject @SchedulerRangeBegin Preference<Integer> schedulerRangeBegin;
    @Inject @SchedulerRangeEnd Preference<Integer> schedulerRangeEnd;
    @Inject @ReminderRingtone Preference<String> reminderRingtone;
    @Inject @Vibrate Preference<Boolean> vibrate;

    /**
     * Alarm manager to schedule/cancel periodical actions
     */
    AlarmManager mAlarmManager;

    /**
     * Vibrator to perform vibration when the notification is playing
     */
    @Inject Vibrator mVibrator;
    /**
     * Audio manager to check current ringer mode
     */
    @Inject AudioManager mAudioManager;
    /**
     * Current ringer mode value holder
     */
    BindableObject<Integer> mRingerMode = new BindableObject<>();
    /**
     * Current ready state value holder
     */
    BindableBoolean mReady = new BindableBoolean(false);
    /**
     * The pending intent used by alarm manager to wake the service
     */
    PendingIntent mPendingIntent;
    /**
     * Reference to the current device wake lock if exists
     */
    PowerManager.WakeLock mWakeLock;
    /**
     * Number of remaining reminder repetitions.
     */
    private int mRemainingRepeats;
    /**
     * The flag to indicate periodical notification active state
     */
    private AtomicBoolean mActive = new AtomicBoolean();
    /**
     * Composite subscription used to handle subscriptions added in this service
     */
    private CompositeSubscription mSubscriptions = new CompositeSubscription();
    /**
     * Field used to store reference to the timer subscription used when the wake lock option is specified
     */
    private Subscription mTimerSubscription;
    /**
     * Receiver used to handle actions from the pending intent used for periodical alarms
     */
    private ScheduledSoundNotificationReceiver mPendingIntentReceiver;
    /**
     * Receiver used to handle ringer mode changed events
     */
    private RingerModeChangedReceiver mRingerModeChangedReceiver;

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
        ObjectGraph appGraph = Injector.obtain(getApplicationContext());
        appGraph.inject(this);
        // TODO workaround for updated interval measurements
        if (reminderInterval.get() < reminderIntervalMinimum) {
            reminderInterval.set(TimeUtils.minutesToSeconds(reminderInterval.get()));
        }

        // initialize broadcast receiver
        mPendingIntentReceiver = new ScheduledSoundNotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(PENDING_INTENT_ACTION);
        registerReceiver(mPendingIntentReceiver, filter);

        mRingerModeChangedReceiver = new RingerModeChangedReceiver();
        filter = new IntentFilter(
                AudioManager.RINGER_MODE_CHANGED_ACTION);
        registerReceiver(mRingerModeChangedReceiver, filter);

        // initialize alarm manager and pending intent
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent i = new Intent(PENDING_INTENT_ACTION);
        mPendingIntent = PendingIntent.getBroadcast(this, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

        // initialize preferences changes listeners
        mSubscriptions.add(
                reminderEnabled.asObservable()
                        .skip(1) // skip initial value emitted right after the subscription
                        .filter(enabled -> enabled) // if reminder enabled
                        .filter(__ -> mReady.get())
                        .subscribe(b -> sendCheckWakingConditionsCommand()));
        mSubscriptions.add(
                reminderEnabled.asObservable()
                        .skip(1) // skip initial value emitted right after the subscription
                        .filter(enabled -> !enabled) // if reminder disabled
                        .subscribe(b -> stopWaking()));
        mSubscriptions.add(
                Observable.merge(
                        Arrays.asList(
                                reminderInterval.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(__ -> Timber.d("Reminder interval changed"))
                                        .map(__ -> true),
                                limitReminderRepeats.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(__ -> Timber.d("Limit reminder repeats changed"))
                                        .map(__ -> true),
                                reminderRepeats.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(__ -> Timber.d("Reminder repeats changed"))
                                        .map(__ -> true),
                                forceWakeLock.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(__ -> Timber.d("Force WakeLock changed"))
                                        .map(__ -> true),
                                selectedApplications.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(__ -> Timber.d("Selected applications changed"))
                                        .map(__ -> true),
                                ignorePersistentNotifications.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(__ -> Timber.d("Ignore persistent notifications changed"))
                                        .map(__ -> true),
                                respectPhoneCalls.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(__ -> Timber.d("Respect phone calls changed")),
                                respectRingerMode.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(__ -> Timber.d("Respect ringer mode changed")),
                                remindWhenScreenIsOn.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(__ -> Timber.d("Remind when screen is on changed")),
                                schedulerEnabled.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(__ -> Timber.d("Scheduler enabled changed"))
                                        .map(__ -> true),
                                schedulerMode.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(__ -> Timber.d("Scheduler mode changed"))
                                        .map(__ -> true),
                                schedulerRangeBegin.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(__ -> Timber.d("Scheduler range begin changed"))
                                        .map(__ -> true),
                                schedulerRangeEnd.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(__ -> Timber.d("Scheduler range end changed"))
                                        .map(__ -> true),
                                vibrate.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(__ -> Timber.d("Vibrate changed")),
                                RxBindingUtils
                                        .valueChanged(mRingerMode)
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(v -> Timber.d("Ringer mode changed to %d", v))
                                        .filter(__ -> respectRingerMode.get())
                                        .map(__ -> true)))
                        .filter(__ -> mReady.get())
                        .subscribe(data -> {
                            // restart alarm with new conditions if necessary
                            stopWaking();
                            sendCheckWakingConditionsCommand();
                        }));
        // await for the service become ready event to send check waking conditions command
        mSubscriptions.add(RxBindingUtils.valueChanged(mReady)
                .filter(ready -> ready)
                .take(1)
                .subscribe(__ -> sendCheckWakingConditionsCommand()));
    }

    /**
     * Send the check waing condition message to the service handler
     */
    private void sendCheckWakingConditionsCommand() {
        Timber.d("sendCheckWakingConditionsCommand");
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
            if (respectRingerMode.get()) {
                // if ringer mode should be respected
                if (mRingerMode.get() == AudioManager.RINGER_MODE_SILENT) {
                    Timber.d("checkWakingConditions: respecting silent mode, skipping");
                    return;
                }
                if (mRingerMode.get() == AudioManager.RINGER_MODE_VIBRATE && !vibrate.get()) {
                    Timber.d("checkWakingConditions: respecting vibrate mode while vibration is not enabled, skipping");
                    return;
                }
            }
            boolean schedule = checkNotificationForAtLeastOnePackageExists(selectedApplications.get(), ignorePersistentNotifications.get());

            if (schedule) {
                Timber.d("checkWakingConditions: there are notifications from selected applications. Scheduling reminder");
                // remember active state
                mActive.set(true);
                if (limitReminderRepeats.get()) {
                    mRemainingRepeats = reminderRepeats.get();
                }
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
        long scheduledTime = 0;
        if (limitReminderRepeats.get() && mRemainingRepeats-- <= 0) {
            Timber.d("scheduleNextWakup: ran out of reminder repeats, stopping");
            stopWaking();
            return;
        }

        if (schedulerEnabled.get()) {
            // if custom scheduler is enabled
            scheduledTime = TimeUtils.getScheduledTime(
                    schedulerMode.get() ? TimeUtils.SchedulerMode.WORKING_PERIOD : TimeUtils.SchedulerMode.NON_WORKING_PERIOD,
                    schedulerRangeBegin.get(), schedulerRangeEnd.get(),
                    System.currentTimeMillis() + reminderInterval.get() * TimeUtils.MILLIS_IN_SECOND);
        }

        if (scheduledTime == 0) {
            Timber.d("scheduleNextWakup: Schedule reminder for %1$d seconds",
                    reminderInterval.get());
            if (forceWakeLock.get() && mWakeLock == null) {
                // if wakelock workaround should be used
                Timber.d("scheduleNextWakup: force wake lock");
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        ReminderNotificationListenerService.class.getSimpleName());
                mWakeLock.acquire();
            }
            scheduleNextWakup(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + reminderInterval.get() * TimeUtils.MILLIS_IN_SECOND);
        } else {
            Timber.d("scheduleNextWakup: Schedule reminder for time %1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS",
                    new Date(scheduledTime));
            releaseWakeLockIfRequired();
            scheduleNextWakup(AlarmManager.RTC_WAKEUP, scheduledTime);
        }
    }

    /**
     * Schedule wakup alarm for the sound notification pending intent
     *
     * @alarmType the type of the alarm either @{link AlarmManager#RTC_WAKEUP} or {link AlarmManager#ELAPSED_REALTIME_WAKEUP}
     * @time the next wakeup time
     */
    private void scheduleNextWakup(int alarmType, long time) {
        Timber.d("scheduleNextWakup: called");
        if (mWakeLock != null) {
            // use the manual timer action to trigger pending intent receiver instead instead of alarm manager
            mTimerSubscription = Observable
                    .just(true)
                    .delay(time - SystemClock.elapsedRealtime(), TimeUnit.MILLISECONDS)
                    .doOnNext(__ -> Timber.d("Wake from subscription"))
                    .subscribe(__ -> mPendingIntentReceiver.onReceive(getApplicationContext(), null));
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mAlarmManager.setExactAndAllowWhileIdle(alarmType, time, mPendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (alarmType == AlarmManager.ELAPSED_REALTIME_WAKEUP) {
                    // adjust the time to the UTC time instead of elapsed time, such as setAlarmClock uses RTC_WAKUP always
                    time = time - SystemClock.elapsedRealtime() + System.currentTimeMillis();
                }
                mAlarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(time, mPendingIntent), mPendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mAlarmManager.setExact(alarmType, time, mPendingIntent);
            } else {
                mAlarmManager.set(alarmType, time, mPendingIntent);
            }
        }
    }

    /**
     * Stop scheduled wakeup alarm for the periodical sound notification
     */
    private void stopWaking() {
        stopWaking(false);
        if (mTimerSubscription != null) {
            mTimerSubscription.unsubscribe();
            mTimerSubscription = null;
        }
        releaseWakeLockIfRequired();
    }

    /**
     * Release a wakelock if exists
     */
    private void releaseWakeLockIfRequired() {
        if (mWakeLock != null) {
            Timber.d("releaseWakeLockIfRequired: release wake lock");
            mWakeLock.release();
            mWakeLock = null;
        }
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
        // unregister ringer mode changed receiver
        unregisterReceiver(mRingerModeChangedReceiver);

        mSubscriptions.unsubscribe();
    }

    @Override
    public void onNotificationPosted(String packageName) {
        Timber.d("onNotificationPosted() called with: packageName = %s",
                packageName);
        if (mReady.get() && selectedApplications.get().contains(packageName)) {
            // check waking conditions only if notification has been posted for the monitored application to prevent
            // mRemainingRepeats overcome in case reminder is already stopped but new notification arrived from any not
            // monitored app
            if (limitReminderRepeats.get()) {
                // reset reminder repeats such as new important notification has arrived
                mRemainingRepeats = reminderRepeats.get();
            }
            checkWakingConditions();
        }
    }

    @Override
    public void onNotificationRemoved() {
        Timber.d("onNotificationRemoved");
        if (mActive.get() && !checkNotificationForAtLeastOnePackageExists(selectedApplications.get(), ignorePersistentNotifications.get())) {
            // stop alarm if there are no more notifications to update
            stopWaking();
        }
    }

    @Override public void onReady() {
        mReady.set(true);
    }

    /**
     * The broadcast receiver for ringer mode changed events
     */
    class RingerModeChangedReceiver extends BroadcastReceiver {
        RingerModeChangedReceiver() {
            // update to initial value
            ringerModeUpdated();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("onReceive: %s", intent);
            ringerModeUpdated();
        }

        /**
         * Called when ringer mode is updated
         */
        private void ringerModeUpdated() {
            mRingerMode.set(mAudioManager.getRingerMode());
        }
    }

    /**
     * The broadcast receiver for the pending intent action fired by alarm manager
     */
    class ScheduledSoundNotificationReceiver extends BroadcastReceiver {
        /**
         * Media player used to play notification sound
         */
        MediaPlayer mMediaPlayer = null;
        /**
         * The lock used during media player preparation and notification sound play
         * to make sure onReceive method will be running for some time keeping the device
         * awake
         */
        CountDownLatch mLock;

        ScheduledSoundNotificationReceiver() {
            // initialize media player
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setOnPreparedListener(__ -> Timber.d("MediaPlayer prepared"));
            mMediaPlayer.setOnCompletionListener(__ -> {
                Timber.d("MediaPlayer completed playing");
                cancelVibration();
            });
            mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Timber.d("MediaPlayer error %1$d %2$d", what, extra);
                cancelVibration();
                return false;
            });
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("onReceive");
            if (!mActive.get()) {
                Timber.w("onReceive: Invalid service activity state, stopping reminder");
                stopWaking(true);
                return;
            }
            if (!remindWhenScreenIsOn.get() && isScreenOn(context)) {
                Timber.d("onReceive: The screen is on and remind when screen is on is not specified, skip notification");
            } else if (PhoneStateUtils.isCallActive(getApplicationContext()) && respectPhoneCalls.get()) {
                Timber.d("onReceive: The phone call is active and respect phone calls setting is specified, skip notification");
            } else {
                Timber.d("onReceive: The screen is off, notify");
                try {
                    // Start without a delay
                    // Each element then alternates between vibrate, sleep, vibrate, sleep...
                    if (vibrate.get() && (!respectRingerMode.get() || mRingerMode.get() != AudioManager.RINGER_MODE_SILENT)) {
                        // if vibration is turned on and phone is not in silent mode or respect ringer mode option is disabled
                        long[] pattern = {0, 100, 50, 100, 50, 100, 200};
                        mVibrator.vibrate(pattern, 0);
                    }
                    if (mMediaPlayer.isPlaying()) {
                        Timber.d("onReceive: Media player is playing. Stopping...");
                        mMediaPlayer.stop();
                    }
                    mMediaPlayer.reset();
                    // use alternative stream if respect ringer mode is disabled
                    mMediaPlayer.setAudioStreamType(respectRingerMode.get() ? AudioManager.STREAM_NOTIFICATION : AudioManager.STREAM_MUSIC);
                    if (respectRingerMode.get() && (mRingerMode.get() == AudioManager.RINGER_MODE_VIBRATE || mRingerMode.get() == AudioManager.RINGER_MODE_SILENT)) {
                        // mute sound explicitly for silent ringer modes because some user claims that sound is not muted on their devices in such cases
                        mMediaPlayer.setVolume(0f, 0f);
                    } else {
                        mMediaPlayer.setVolume(1f, 1f);
                    }
                    // create lock object with timeout
                    mLock = new CountDownLatch(1);
                    Timber.d("onReceive: current thread %1$s", Thread.currentThread().getName());
                    Observable.just(mMediaPlayer)
                            .observeOn(Schedulers.io())
                            .doOnError(__ -> cancelVibration())
                            .subscribe(mp -> {
                                try {
                                    Timber.d("onReceive subscription: current thread %1$s", Thread.currentThread().getName());
                                    // get the selected notification sound URI
                                    String ringtone = reminderRingtone.get();
                                    if (TextUtils.isEmpty(ringtone)) {
                                        cancelVibration();
                                        Timber.w("The reminder ringtone is not specified. Skip playing");
                                        return;
                                    }
                                    Timber.d("onReceive: ringtone %1$s", ringtone);
                                    Uri notification = Uri.parse(ringtone);
                                    mp.setDataSource(getApplicationContext(), notification);
                                    mp.prepare();
                                    mp.start();
                                    mLock.countDown();
                                } catch (Exception ex) {
                                    cancelVibration();
                                    throw OnErrorThrowable.from(ex);
                                }
                            }, ex -> Timber.e(ex, null));
                    // give max 5 seconds to play notification sound
                    mLock.await(5, TimeUnit.SECONDS);
                    if (mLock.getCount() > 0) {
                        Timber.w("onReceive: media player initializes too long, didn't receive onComplete for 5 seconds.");
                    }
                } catch (Exception ex) {
                    Timber.e(ex, null);
                    throw new RuntimeException(ex);
                }
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

        /**
         * Cancel the vibration
         */
        private void cancelVibration() {
            mVibrator.cancel();
        }
    }

}
