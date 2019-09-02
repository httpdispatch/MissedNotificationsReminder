package com.app.missednotificationsreminder.service;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import androidx.core.app.NotificationCompat;
import androidx.core.content.res.ResourcesCompat;
import android.text.TextUtils;
import android.view.Display;

import com.app.missednotificationsreminder.R;
import com.app.missednotificationsreminder.binding.util.BindableBoolean;
import com.app.missednotificationsreminder.binding.util.BindableObject;
import com.app.missednotificationsreminder.binding.util.RxBindingUtils;
import com.app.missednotificationsreminder.di.Injector;
import com.app.missednotificationsreminder.di.qualifiers.CreateDismissNotification;
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
import com.app.missednotificationsreminder.di.qualifiers.VibrationPattern;
import com.app.missednotificationsreminder.util.PhoneStateUtils;
import com.app.missednotificationsreminder.util.TimeUtils;
import com.app.missednotificationsreminder.util.event.RxEventBus;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.f2prateek.rx.preferences.Preference;

import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import dagger.ObjectGraph;
import rx.Completable;
import rx.Emitter;
import rx.Observable;
import rx.Scheduler;
import rx.Single;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
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
     * Action for the pending intent sent when dismiss notification has been cancelled.
     */
    static final String STOP_REMINDERS_INTENT_ACTION =
            ReminderNotificationListenerService.class.getCanonicalName() + ".STOP_REMINDERS_INTENT";

    /**
     * Notification id for the dismiss notification. It must be unique in an app, but since we only
     * generate this notification and there could be only one of them, it is a constant.
     */
    static final int DISMISS_NOTIFICATION_ID = 42;

    @Inject @ReminderEnabled Preference<Boolean> reminderEnabled;
    @Inject @ReminderIntervalMin int reminderIntervalMinimum;
    @Inject @ReminderInterval Preference<Integer> reminderInterval;
    @Inject @ReminderRepeats Preference<Integer> reminderRepeats;
    @Inject @LimitReminderRepeats Preference<Boolean> limitReminderRepeats;
    @Inject @CreateDismissNotification Preference<Boolean> createDismissNotification;
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
    @Inject @VibrationPattern Preference<String> vibrationPattern;
    @Inject RxEventBus mEventBus;

    /**
     * Alarm manager to schedule/cancel periodical actions
     */
    AlarmManager mAlarmManager;
    /**
     * The power manager to acquire wake locks for the reminder
     */
    PowerManager mPowerManager;

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
     * Current DND mode enabled value holder
     */
    BindableBoolean mDndEnabled = new BindableBoolean(false);
    /**
     * Current ready state value holder
     */
    BindableBoolean mReady = new BindableBoolean(false);
    /**
     * The pending intent used by alarm manager to wake the service
     */
    PendingIntent mPendingIntent;
    /**
     * The pending intent sent when dismiss notification is cancelled.
     */
    PendingIntent mStopRemindersIntent;
    /**
     * Reference to the current device wake lock if exists
     */
    PowerManager.WakeLock mWakeLock;
    /**
     * Number of remaining reminder repetitions.
     */
    private int mRemainingRepeats;
    /**
     * Notification manager for creating/removing dismiss notification.
     */
    NotificationManager mNotificationManager;
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
     * Observer used to handle DND mode changes events
     */
    private ZenModeObserver mZenModeObserver;
    /**
     * Receiver used to handle cancellation of the dismiss message.
     */
    private StopRemindersReceiver mStopRemindersReceiver;
    /**
     * The notification large icon cache
     */
    Bitmap mNotificationLargeIcon;
    /**
     * Whether the application is initializing
     */
    volatile boolean mInitializing = true;

    /**
     * The handler used to process various service related messages
     */
    private Handler mHandler = new Handler(Looper.getMainLooper());
    /**
     * The scheduler which runs jobs in the <code>mHandler</code>
     */
    private Scheduler mScheduler = AndroidSchedulers.from(mHandler.getLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("onCreate");
        initialize();

    }

    private void initialize() {
        // inject dependencies
        ObjectGraph appGraph = Injector.obtain(getApplicationContext());
        if (appGraph == null) {
            Log.e("ReminderService", "application is not available");
            mHandler.postDelayed(() -> initialize(), 1000);
            return;
        }
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

        mZenModeObserver = new ZenModeObserver(mHandler);
        getApplicationContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, mZenModeObserver);

        // initialize dismiss notification service and receiver
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mStopRemindersReceiver = new StopRemindersReceiver();
        mStopRemindersIntent = PendingIntent.getBroadcast(
                this.getApplicationContext(), 0, new Intent(STOP_REMINDERS_INTENT_ACTION), 0);
        registerReceiver(mStopRemindersReceiver, new IntentFilter(STOP_REMINDERS_INTENT_ACTION));

        // initialize alarm, power managers and pending intent
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        Intent i = new Intent(PENDING_INTENT_ACTION);
        mPendingIntent = PendingIntent.getBroadcast(this, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

        // initialize preferences changes listeners
        mSubscriptions.add(
                reminderEnabled.asObservable()
                        .skip(1) // skip initial value emitted right after the subscription
                        .filter(enabled -> enabled) // if reminder enabled
                        .filter(__ -> mReady.get())
                        .observeOn(mScheduler)
                        .subscribe(b -> checkWakingConditions()));
        mSubscriptions.add(
                reminderEnabled.asObservable()
                        .skip(1) // skip initial value emitted right after the subscription
                        .filter(enabled -> !enabled) // if reminder disabled
                        .observeOn(mScheduler)
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
                                createDismissNotification.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(__ -> Timber.d("Create dismiss notification changed"))
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
                                vibrationPattern.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(__ -> Timber.d("Vibration pattern changed")),
                                RxBindingUtils
                                        .valueChanged(mRingerMode)
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(v -> Timber.d("Ringer mode changed to %d", v))
                                        .filter(__ -> respectRingerMode.get())
                                        .map(__ -> true),
                                RxBindingUtils
                                        .valueChanged(mDndEnabled)
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext(v -> Timber.d("DND mode changed to %b", v))
                                        .filter(__ -> respectRingerMode.get())))
                        .filter(__ -> mReady.get())
                        .observeOn(mScheduler)
                        .subscribe(data -> {
                            // restart alarm with new conditions if necessary
                            stopWaking();
                            checkWakingConditions();
                        }));
        // await for the service become ready event to send check waking conditions command
        mSubscriptions.add(RxBindingUtils.valueChanged(mReady)
                .filter(ready -> ready)
                .take(1)
                .observeOn(mScheduler)
                .subscribe(__ -> checkWakingConditions()));
        // monitor for the remind events sent via event bus
        mSubscriptions.add(mEventBus.toObserverable()
                .filter(event -> event == RemindEvents.REMIND)
                .subscribe(__ -> mPendingIntentReceiver.onReceive(getApplicationContext(), null)));
        mInitializing = false;
    }

    /**
     * Send the check waing condition message to the service handler
     */
    private void sendCheckWakingConditionsCommand() {
        Timber.d("sendCheckWakingConditionsCommand");
        mHandler.sendMessage(mHandler.obtainMessage(CHECK_WAKING_CONDITIONS_MSG));
    }

    private void sendStopWakingCommand() {
        Timber.d("sendStopReminderCommand");
        mHandler.sendMessage(mHandler.obtainMessage(STOP_WAKING_MSG));
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
                if (mDndEnabled.get()) {
                    Timber.d("checkWakingConditions: respecting DND mode, skipping");
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
                scheduleNextWakeup();
            } else {
                Timber.d("checkWakingConditions: there are no notifications from selected applications to periodically remind");
            }
        } catch (Throwable t) {
            Timber.e(t, "Unexpected failure");
        }
    }

    /**
     * Cancel dismiss notification if one is present.
     */
    private void cancelDismissNotification() {
        Timber.d("cancelDismissNotification() called");
        // This will not send mStopRemindersIntent. Only user actions do.
        mNotificationManager.cancel(DISMISS_NOTIFICATION_ID);
    }

    /**
     * Create dismiss notification unless one is already present.
     */
    private void createDismissNotification() {
        Timber.d("createDismissNotification() called");
        if (mNotificationLargeIcon == null) {
            mNotificationLargeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        }
        String channelId = "MNR dismiss notification";
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notification)  // this is custom icon, looks betetr
                        .setLargeIcon(mNotificationLargeIcon)
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .setContentTitle(getText(R.string.dismiss_notification_title))
                        .setContentText(getText(R.string.dismiss_notification_text))
                        // main color of the logo
                        .setColor(ResourcesCompat.getColor(getResources(), R.color.logo_color, getTheme()))
                        .setDeleteIntent(mStopRemindersIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    getText(R.string.dismiss_notification_title),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(null, null);
            channel.enableVibration(false);
            mNotificationManager.createNotificationChannel(channel);
        }
        mNotificationManager.notify(DISMISS_NOTIFICATION_ID, builder.build());
    }

    /**
     * Schedule wakeup alarm for the sound notification pending intent
     */
    private void scheduleNextWakeup() {
        long scheduledTime = 0;
        if (limitReminderRepeats.get() && mRemainingRepeats-- <= 0) {
            Timber.d("scheduleNextWakeup: ran out of reminder repeats, stopping");
            stopWaking();
            return;
        }

        if (createDismissNotification.get()) {
            createDismissNotification();
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
                mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        ReminderNotificationListenerService.class.getSimpleName());
                mWakeLock.acquire();
            }
            scheduleNextWakeupForOffset(reminderInterval.get() * TimeUtils.MILLIS_IN_SECOND);
        } else {
            Timber.d("scheduleNextWakup: Schedule reminder for time %1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS",
                    new Date(scheduledTime));
            releaseWakeLockIfRequired();
            scheduleNextWakeupForOffset(scheduledTime - System.currentTimeMillis());
        }
    }

    /**
     * Schedule wakeup alarm for the sound notification pending intent
     *
     * @time the next wakeup time offset
     */
    private void scheduleNextWakeupForOffset(long timeOffset) {
        Timber.d("scheduleNextWakup: called");
        if (mWakeLock != null) {
            // use the manual timer action to trigger pending intent receiver instead instead of alarm manager
            mTimerSubscription = Observable
                    .just(true)
                    .delay(timeOffset, TimeUnit.MILLISECONDS)
                    .doOnNext(__ -> Timber.d("Wake from subscription"))
                    .subscribe(__ -> mPendingIntentReceiver.onReceive(getApplicationContext(), null));
        } else {
            new JobRequest.Builder(RemindJob.TAG)
                    .setExact(timeOffset)
                    .build()
                    .schedule();
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
        // cancel any pending remind jobs
        JobManager.instance().cancelAllForTag(RemindJob.TAG);
        mPendingIntentReceiver.interruptReminderIfActive();
        releaseWakeLockIfRequired();
        cancelDismissNotification();
    }

    /**
     * Release a wakelock if exists
     */
    private void releaseWakeLockIfRequired() {
        if (mWakeLock != null) {
            Timber.d("releaseWakeLockIfRequired: release wake lock");
            try {
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
            } catch (Exception ex) {
                Timber.e(ex);
            }
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
        // unregister zen mode changed observer
        getApplicationContext().getContentResolver().unregisterContentObserver(mZenModeObserver);
        // unregister dismiss notification receiver
        unregisterReceiver(mStopRemindersReceiver);

        mHandler.removeCallbacksAndMessages(null);

        mSubscriptions.unsubscribe();
    }

    @Override
    public void onNotificationPosted(String packageName) {
        mHandler.post(() -> {
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
                if (!mInitializing) {
                    checkWakingConditions();
                }
            }
        });
    }

    @Override
    public void onNotificationRemoved() {
        mHandler.post(() -> {
            Timber.d("onNotificationRemoved");
            if (mActive.get() && !checkNotificationForAtLeastOnePackageExists(selectedApplications.get(), ignorePersistentNotifications.get())) {
                // stop alarm if there are no more notifications to update
                stopWaking();
            }
        });
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
     * The content observer for the DND mode changes
     */
    private class ZenModeObserver extends ContentObserver {
        final int DND_OFF = 0;

        ZenModeObserver(Handler handler) {
            super(handler);
            zenModeUpdated();
        }

        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            zenModeUpdated();
        }

        private void zenModeUpdated() {
            Timber.d("zenModeUpdated() called");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                try {
                    int zen_mode = Settings.Global.getInt(getContentResolver(), "zen_mode");
                    Timber.d("zenModeUpdated: %d", zen_mode);
                    mDndEnabled.set(zen_mode != DND_OFF);
                } catch (Settings.SettingNotFoundException e) {
                    Timber.e(e);
                }
            }
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
         * The reminder subscription
         */
        CompositeSubscription mReminderSubscription = new CompositeSubscription();
        /**
         * Reference to the current device wake lock used while vibrator is active
         */
        PowerManager.WakeLock mVibrationWakeLock;

        ScheduledSoundNotificationReceiver() {
            // initialize media player
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            mHandler.post(() -> {
                Timber.d("onReceive: current thread %1$s", Thread.currentThread().getName());
                if (!mActive.get()) {
                    Timber.w("onReceive: Invalid service activity state, stopping reminder");
                    stopWaking(true);
                    return;
                }
                if (!remindWhenScreenIsOn.get() && isScreenOn(getApplicationContext())) {
                    Timber.d("onReceive: The screen is on and remind when screen is on is not specified, skip notification");
                } else if (PhoneStateUtils.isCallActive(getApplicationContext()) && respectPhoneCalls.get()) {
                    Timber.d("onReceive: The phone call is active and respect phone calls setting is specified, skip notification");
                } else {
                    Timber.d("onReceive: The screen is off, notify");
                    interruptReminderIfActive();
                    Completable playbackCompleted = playReminderCompletable();
                    Completable vibrationCompletedAtLeastOnce;
                    // Start without a delay
                    // Each element then alternates between vibrate, sleep, vibrate, sleep...
                    if (vibrate.get() && (!respectRingerMode.get() || mRingerMode.get() != AudioManager.RINGER_MODE_SILENT)) {
                        // if vibration is turned on and phone is not in silent mode or respect ringer mode option is disabled
                        long[] pattern = parseVibrationPattern(vibrationPattern.get());
                        vibrationCompletedAtLeastOnce = Single.fromCallable(() -> {
                            mVibrationWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                    "MissedNotificationsReminder:VIBRATOR_LOCK");
                            mVibrationWakeLock.acquire();
                            mVibrator.vibrate(pattern, 0);
                            long vibrationDuration = 0;
                            for (long step : pattern) {
                                vibrationDuration += step;
                            }
                            Timber.d("Minimum vibration duration: %d", vibrationDuration);
                            return vibrationDuration;
                        })
                                .flatMapCompletable(vibrationDuration -> Completable.timer(vibrationDuration, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread()))
                                .doOnError(t -> Timber.e(t))
                                .onErrorComplete()
                                .doOnCompleted(() -> Timber.d("Minimum vibration completed"))
                                .doOnUnsubscribe(() -> cancelVibrator());
                    } else {
                        vibrationCompletedAtLeastOnce = Completable.complete();
                    }
                    // await for both playback and minimum vibration duration to complete
                    mReminderSubscription.add(Completable.merge(
                            playbackCompleted,
                            vibrationCompletedAtLeastOnce)
                            .doOnCompleted(() -> cancelVibrator())
                            .subscribe(() -> Timber.d("Reminder completed")));
                }

                // notify listeners abot reminder completion
                mEventBus.send(RemindEvents.REMINDER_COMPLETED);
                scheduleNextWakeup();
            });
        }

        private void cancelVibrator() {
            mVibrator.cancel();
            if (mVibrationWakeLock != null) {
                try {
                    if (mVibrationWakeLock.isHeld()) {
                        mVibrationWakeLock.release();
                    }
                } catch (Exception ex) {
                    Timber.e(ex);
                }
                mWakeLock = null;
            }
        }

        private Completable playReminderCompletable() {
            return Observable.amb(
                    Completable.timer(5, TimeUnit.SECONDS)
                            .andThen(Observable.error(new Error("onReceive: media player initializes too long, didn't receive onComplete for 5 seconds."))),
                    Observable.create(emitter -> {
                        try {
                            mMediaPlayer.reset();
                            // use alternative stream if respect ringer mode is disabled
                            mMediaPlayer.setAudioStreamType(respectRingerMode.get() ? AudioManager.STREAM_NOTIFICATION : AudioManager.STREAM_ALARM);
                            if (respectRingerMode.get() && (mRingerMode.get() == AudioManager.RINGER_MODE_VIBRATE || mRingerMode.get() == AudioManager.RINGER_MODE_SILENT)) {
                                // mute sound explicitly for silent ringer modes because some user claims that sound is not muted on their devices in such cases
                                mMediaPlayer.setVolume(0f, 0f);
                            } else {
                                mMediaPlayer.setVolume(1f, 1f);
                            }
                            mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                                Timber.e("MediaPlayer error %1$d %2$d", what, extra);
                                emitter.onError(new Error(String.format("MediaPlayer error %1$d %2$d", what, extra)));
                                return false;
                            });
                            mMediaPlayer.setOnCompletionListener(__ -> {
                                Timber.d("completion");
                                emitter.onCompleted();
                            });
                            emitter.setCancellation(() -> {
                                Timber.d("cancellation 1");
                                if (mMediaPlayer.isPlaying()) {
                                    mMediaPlayer.stop();
                                }
                                mMediaPlayer.setOnCompletionListener(null);
                                mMediaPlayer.setOnErrorListener(null);
                                mMediaPlayer.setOnPreparedListener(null);
                            });
                            // get the selected notification sound URI
                            String ringtone = reminderRingtone.get();
                            if (TextUtils.isEmpty(ringtone)) {
                                Timber.w("The reminder ringtone is not specified. Skip playing");
                                emitter.onCompleted();
                            } else {
                                Timber.d("onReceive: ringtone %1$s", ringtone);
                                Uri notification = Uri.parse(ringtone);
                                mMediaPlayer.setOnPreparedListener(__ -> {
                                    Timber.d("MediaPlayer prepared");
                                    mMediaPlayer.start();
                                    emitter.onNext(notification);
                                });
                                mMediaPlayer.setDataSource(getApplicationContext(), notification);
                                mMediaPlayer.prepareAsync();
                            }
                        } catch (Exception ex) {
                            Timber.e(ex);
                            emitter.onError(ex);
                        }
                    }, Emitter.BackpressureMode.NONE))
                    .share()
                    .toCompletable()
                    .doOnError(t -> Timber.e(t))
                    .onErrorComplete()
                    .doOnCompleted(() -> Timber.d("Playback completed"));
        }

        private long[] parseVibrationPattern(String rawPattern) {
            // This code assumes the pattern string matches regexp \d+(\s*,\s*\d+)*
            String[] components = rawPattern.split("\\s*,\\s*");
            long[] parsedPattern = new long[components.length];
            for (int i = 0; i < components.length; i++) {
                parsedPattern[i] = Long.parseLong(components[i]);
            }
            return parsedPattern;
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
                //noinspection deprecation
                return mPowerManager.isScreenOn();
            }
        }

        /**
         * Interrupt previously started reminder if it is active
         */
        void interruptReminderIfActive() {
            mReminderSubscription.clear();
        }
    }

    /**
     * The broadcast receiver for the pending intent fired when the user wants to stop reminders by
     * cancelling the dismiss notification.
     */
    class StopRemindersReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mHandler.post(() -> {
                Timber.d("dismiss notification cancelled");
                ignoreAllCurrentNotifications();
                stopWaking();
            });
        }
    }
}
