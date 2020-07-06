package com.app.missednotificationsreminder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.os.PowerManager.WakeLock
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils
import android.util.Log
import android.view.Display
import androidx.annotation.CallSuper
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.lifecycleScope
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.binding.util.BindableBoolean
import com.app.missednotificationsreminder.binding.util.BindableObject
import com.app.missednotificationsreminder.binding.util.RxBindingUtils
import com.app.missednotificationsreminder.di.Injector.Companion.obtain
import com.app.missednotificationsreminder.di.qualifiers.*
import com.app.missednotificationsreminder.service.data.model.NotificationData
import com.app.missednotificationsreminder.service.event.NotificationsUpdatedEvent
import com.app.missednotificationsreminder.service.event.RemindEvents
import com.app.missednotificationsreminder.service.util.PhoneStateUtils
import com.app.missednotificationsreminder.util.TimeUtils
import com.app.missednotificationsreminder.util.event.Event
import com.app.missednotificationsreminder.util.event.RxEventBus
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.f2prateek.rx.preferences.Preference
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import rx.*
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * The service to monitor all status bar notifications. It performs periodical sound notification depend on whether
 * there are available notifications from applications which matches user selected applications. The notification interval
 * is also specified by the user in the corresponding window.
 *
 * @author Eugene Popovich
 */
class ReminderNotificationListenerService : AbstractReminderNotificationListenerService(), LifecycleOwner {
    private val mDispatcher = ServiceLifecycleDispatcher(this)

    @Inject
    @ReminderEnabled
    lateinit var reminderEnabled: Preference<Boolean>

    @JvmField
    @field:[Inject ReminderIntervalMin]
    var reminderIntervalMinimum: Int = 0

    @Inject
    @ReminderInterval
    lateinit var reminderInterval: Preference<Int>

    @Inject
    @ReminderRepeats
    lateinit var reminderRepeats: Preference<Int>

    @Inject
    @LimitReminderRepeats
    lateinit var limitReminderRepeats: Preference<Boolean>

    @Inject
    @CreateDismissNotification
    lateinit var createDismissNotification: Preference<Boolean>

    @Inject
    @CreateDismissNotificationImmediately
    lateinit var createDismissNotificationImmediately: Preference<Boolean>

    @Inject
    @ForceWakeLock
    lateinit var forceWakeLock: Preference<Boolean>

    @Inject
    @SelectedApplications
    lateinit var selectedApplications: Preference<Set<String>>

    @Inject
    @IgnorePersistentNotifications
    lateinit var ignorePersistentNotifications: Preference<Boolean>

    @Inject
    @RespectPhoneCalls
    lateinit var respectPhoneCalls: Preference<Boolean>

    @Inject
    @RespectRingerMode
    lateinit var respectRingerMode: Preference<Boolean>

    @Inject
    @RemindWhenScreenIsOn
    lateinit var remindWhenScreenIsOn: Preference<Boolean>

    @Inject
    @SchedulerEnabled
    lateinit var schedulerEnabled: Preference<Boolean>

    @Inject
    @SchedulerMode
    lateinit var schedulerMode: Preference<Boolean>

    @Inject
    @SchedulerRangeBegin
    lateinit var schedulerRangeBegin: Preference<Int>

    @Inject
    @SchedulerRangeEnd
    lateinit var schedulerRangeEnd: Preference<Int>

    @Inject
    @ReminderRingtone
    lateinit var reminderRingtone: Preference<String>

    @Inject
    @Vibrate
    lateinit var vibrate: Preference<Boolean>

    @Inject
    @VibrationPattern
    lateinit var vibrationPattern: Preference<String>

    @Inject
    lateinit var mEventBus: RxEventBus

    /**
     * List to store currently active notifications data
     */
    private val availableNotifications = ConcurrentLinkedQueue<NotificationData>()

    /**
     * List of notification data entries that are ignored. This list must contain same objects as
     * mAvailableNotifications above.
     */
    private val ignoredNotifications = ConcurrentLinkedQueue<NotificationData>()

    /**
     * The power manager to acquire wake locks for the reminder
     */
    private val powerManager by lazy { getSystemService(Context.POWER_SERVICE) as PowerManager }

    /**
     * Vibrator to perform vibration when the notification is playing
     */
    @Inject
    lateinit var vibrator: Vibrator

    /**
     * Audio manager to check current ringer mode
     */
    @Inject
    lateinit var audioManager: AudioManager

    /**
     * Current ringer mode value holder
     */
    private val ringerMode = BindableObject<Int>()

    /**
     * Current DND mode enabled value holder
     */
    private val dndEnabled = BindableBoolean(false)

    /**
     * Current ready state value holder
     */
    private val ready = BindableBoolean(false)

    /**
     * The pending intent sent when dismiss notification is cancelled.
     */
    private val stopRemindersIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(this.applicationContext, 0, Intent(STOP_REMINDERS_INTENT_ACTION), 0)
    }

    /**
     * Reference to the current device wake lock if exists
     */
    private var wakeLock: WakeLock? = null

    /**
     * Number of remaining reminder repetitions.
     */
    private var remainingRepeats = 0

    /**
     * Notification manager for creating/removing dismiss notification.
     */
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * The flag to indicate periodical notification active state
     */
    private val active = AtomicBoolean()

    /**
     * Composite subscription used to handle subscriptions added in this service
     */
    private val subscriptions = CompositeSubscription()

    /**
     * Field used to store reference to the timer subscription used when the wake lock option is specified
     */
    private var timerSubscription: Subscription? = null

    /**
     * Receiver used to handle actions from the pending intent used for periodical alarms
     */
    private val pendingIntentReceiver by lazy {
        ScheduledSoundNotificationReceiver()
    }

    /**
     * Receiver used to handle ringer mode changed events
     */
    private val ringerModeChangedReceiver by lazy {
        RingerModeChangedReceiver()
    }

    /**
     * Observer used to handle DND mode changes events
     */
    private val zenModeObserver by lazy {
        ZenModeObserver(handler)
    }

    /**
     * Receiver used to handle cancellation of the dismiss message.
     */
    private val stopRemindersReceiver by lazy {
        StopRemindersReceiver()
    }

    /**
     * The notification large icon cache
     */
    private val notificationLargeIcon: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
    }

    /**
     * Whether the application is initializing
     */
    @Volatile
    private var initializing = true

    /**
     * The handler used to process various service related messages
     */
    private val handler = Handler(Looper.getMainLooper())

    /**
     * The scheduler which runs jobs in the `mHandler`
     */
    private val scheduler = AndroidSchedulers.from(handler.looper)

    @CallSuper
    override fun onCreate() {
        mDispatcher.onServicePreSuperOnCreate()
        super.onCreate()
        Timber.d("onCreate")
    }

    @CallSuper
    override fun onBind(intent: Intent): IBinder? {
        mDispatcher.onServicePreSuperOnBind()
        Timber.d("onBind()")
        return super.onBind(intent)
    }

    @CallSuper
    override fun onStart(intent: Intent?, startId: Int) {
        mDispatcher.onServicePreSuperOnStart()
        Timber.d("onStart()")
        super.onStart(intent, startId)
    }

    override fun getLifecycle(): Lifecycle {
        return mDispatcher.lifecycle
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Timber.d("attachBaseContext")
        initialize()
    }

    private fun initialize() {
        // inject dependencies
        val appGraph: AndroidInjector<Any>? = obtain(applicationContext)
        if (appGraph == null) {
            Log.e("ReminderService", "application is not available")
            handler.postDelayed({
                Log.w("ReminderService", "Initialize: one more try")
                initialize()
            }, 1000)
            return
        }
        appGraph.inject(this)
        // TODO workaround for updated interval measurements
        if (reminderInterval.get()!! < reminderIntervalMinimum) {
            reminderInterval.set(TimeUtils.minutesToSeconds(reminderInterval.get()!!.toFloat()))
        }

        // initialize broadcast receiver
        var filter = IntentFilter()
        filter.addAction(PENDING_INTENT_ACTION)
        registerReceiver(pendingIntentReceiver, filter)
        filter = IntentFilter(
                AudioManager.RINGER_MODE_CHANGED_ACTION)
        registerReceiver(ringerModeChangedReceiver, filter)
        applicationContext.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, zenModeObserver)

        // initialize dismiss notification service and receiver
        registerReceiver(stopRemindersReceiver, IntentFilter(STOP_REMINDERS_INTENT_ACTION))

        // initialize preferences changes listeners
        subscriptions.add(
                reminderEnabled.asObservable()
                        .skip(1) // skip initial value emitted right after the subscription
                        .filter { enabled: Boolean? -> enabled } // if reminder enabled
                        .filter { ready.get() }
                        .observeOn(scheduler)
                        .subscribe { b: Boolean? -> checkWakingConditions() })
        subscriptions.add(
                reminderEnabled.asObservable()
                        .skip(1) // skip initial value emitted right after the subscription
                        .filter { enabled: Boolean? -> !enabled!! } // if reminder disabled
                        .observeOn(scheduler)
                        .subscribe { b: Boolean? -> stopWaking() })
        subscriptions.add(
                Observable.merge(
                        Arrays.asList(
                                reminderInterval.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { Timber.d("Reminder interval changed") }
                                        .map { true },
                                limitReminderRepeats.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { Timber.d("Limit reminder repeats changed") }
                                        .map { true },
                                createDismissNotification.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { Timber.d("Create dismiss notification changed") }
                                        .map { true },
                                createDismissNotificationImmediately.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { Timber.d("Create dismiss notification immediately changed") }
                                        .map { true },
                                reminderRepeats.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { Timber.d("Reminder repeats changed") }
                                        .map { true },
                                forceWakeLock.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { Timber.d("Force WakeLock changed") }
                                        .map { true },
                                selectedApplications.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { Timber.d("Selected applications changed") }
                                        .map { true },
                                ignorePersistentNotifications.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { Timber.d("Ignore persistent notifications changed") }
                                        .map { true },
                                respectPhoneCalls.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { Timber.d("Respect phone calls changed") },
                                respectRingerMode.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { Timber.d("Respect ringer mode changed") },
                                remindWhenScreenIsOn.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { Timber.d("Remind when screen is on changed") },
                                schedulerEnabled.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { Timber.d("Scheduler enabled changed") }
                                        .map { true },
                                schedulerMode.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { Timber.d("Scheduler mode changed") }
                                        .map { true },
                                schedulerRangeBegin.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { Timber.d("Scheduler range begin changed") }
                                        .map { true },
                                schedulerRangeEnd.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { Timber.d("Scheduler range end changed") }
                                        .map { true },
                                vibrate.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { Timber.d("Vibrate changed") },
                                vibrationPattern.asObservable()
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { Timber.d("Vibration pattern changed") },
                                RxBindingUtils
                                        .valueChanged(ringerMode)
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { v -> Timber.d("Ringer mode changed to %d", v) }
                                        .filter { respectRingerMode.get() }
                                        .map { true },
                                RxBindingUtils
                                        .valueChanged(dndEnabled)
                                        .skip(1) // skip initial value emitted right after the subscription
                                        .doOnNext { v -> Timber.d("DND mode changed to %b", v) }
                                        .filter { respectRingerMode.get() }))
                        .filter { ready.get() }
                        .observeOn(scheduler)
                        .subscribe {
                            // restart alarm with new conditions if necessary
                            stopWaking()
                            checkWakingConditions()
                        })
        // await for the service become ready event to send check waking conditions command
        subscriptions.add(RxBindingUtils.valueChanged(ready)
                .filter { it }
                .take(1)
                .observeOn(scheduler)
                .doOnNext { checkWakingConditions() }
                .subscribe { actualizeNotificationData() })
        // monitor for the remind events sent via event bus
        subscriptions.add(mEventBus.toObserverable()
                .filter { event -> event === RemindEvents.REMIND }
                .subscribe { pendingIntentReceiver.onReceive(applicationContext, Intent()) })
        subscriptions.add(mEventBus.toObserverable()
                .filter { event: Event -> event === RemindEvents.GET_CURRENT_NOTIFICATIONS_DATA }
                .observeOn(scheduler)
                .subscribe { mEventBus.send(NotificationsUpdatedEvent(notificationsData)) })
        initializing = false
    }

    /**
     * Check whether the waking alarm should be scheduled or no
     */
    private fun checkWakingConditions() {
        Timber.d("checkWakingConditions() called %s", Thread.currentThread().name)
        try {
            if (active.get()) {
                Timber.d("checkWakingConditions: already active, skipping")
                return
            }
            if (!reminderEnabled.get()!!) {
                Timber.d("checkWakingConditions: disabled, skipping")
                return
            }
            if (respectRingerMode.get()!!) {
                // if ringer mode should be respected
                if (ringerMode.get() == AudioManager.RINGER_MODE_SILENT) {
                    Timber.d("checkWakingConditions: respecting silent mode, skipping")
                    return
                }
                if (dndEnabled.get()) {
                    Timber.d("checkWakingConditions: respecting DND mode, skipping")
                    return
                }
                if (ringerMode.get() == AudioManager.RINGER_MODE_VIBRATE && !vibrate.get()!!) {
                    Timber.d("checkWakingConditions: respecting vibrate mode while vibration is not enabled, skipping")
                    return
                }
            }
            val schedule = checkNotificationForAtLeastOnePackageExists(selectedApplications.get()!!, ignorePersistentNotifications.get()!!)
            if (schedule) {
                Timber.d("checkWakingConditions: there are notifications from selected applications. Scheduling reminder")
                // remember active state
                active.set(true)
                if (limitReminderRepeats.get()!!) {
                    remainingRepeats = reminderRepeats.get()!!
                }
                scheduleNextWakeup(false)
            } else {
                Timber.d("checkWakingConditions: there are no notifications from selected applications to periodically remind")
            }
        } catch (t: Throwable) {
            Timber.e(t, "Unexpected failure")
        }
    }

    /**
     * Cancel dismiss notification if one is present.
     */
    private fun cancelDismissNotification() {
        Timber.d("cancelDismissNotification() called")
        // This will not send mStopRemindersIntent. Only user actions do.
        notificationManager.cancel(DISMISS_NOTIFICATION_ID)
    }

    /**
     * Create dismiss notification unless one is already present.
     */
    private fun createDismissNotification() {
        Timber.d("createDismissNotification() called")
        val channelId = "MNR dismiss notification"
        val builder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification) // this is custom icon, looks betetr
                .setLargeIcon(notificationLargeIcon)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentTitle(getText(R.string.dismiss_notification_title))
                .setContentText(getText(R.string.dismiss_notification_text)) // main color of the logo
                .setColor(ResourcesCompat.getColor(resources, R.color.logo_color, theme))
                .setDeleteIntent(stopRemindersIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                    getText(R.string.dismiss_notification_title),
                    NotificationManager.IMPORTANCE_DEFAULT)
            channel.setSound(null, null)
            channel.enableVibration(false)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(DISMISS_NOTIFICATION_ID, builder.build())
    }

    /**
     * Schedule wakeup alarm for the sound notification pending intent
     */
    private fun scheduleNextWakeup(repeating: Boolean) {
        var scheduledTime: Long = 0
        if (limitReminderRepeats.get()!! && remainingRepeats-- <= 0) {
            Timber.d("scheduleNextWakeup: ran out of reminder repeats, stopping")
            stopWaking()
            return
        }
        if (createDismissNotification.get()!! && (repeating || createDismissNotificationImmediately.get()!!)) {
            createDismissNotification()
        }
        if (schedulerEnabled.get()!!) {
            // if custom scheduler is enabled
            scheduledTime = TimeUtils.getScheduledTime(
                    if (schedulerMode.get()!!) TimeUtils.SchedulerMode.WORKING_PERIOD else TimeUtils.SchedulerMode.NON_WORKING_PERIOD,
                    schedulerRangeBegin.get()!!, schedulerRangeEnd.get()!!,
                    System.currentTimeMillis() + reminderInterval.get()!! * TimeUtils.MILLIS_IN_SECOND)
        }
        if (scheduledTime == 0L) {
            Timber.d("scheduleNextWakup: Schedule reminder for %1\$d seconds",
                    reminderInterval.get())
            if (forceWakeLock.get()!! && wakeLock == null) {
                // if wakelock workaround should be used
                Timber.d("scheduleNextWakup: force wake lock")
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        ReminderNotificationListenerService::class.java.simpleName)
                        .apply { acquire() }
            }
            scheduleNextWakeupForOffset(reminderInterval.get()!! * TimeUtils.MILLIS_IN_SECOND.toLong())
        } else {
            Timber.d("scheduleNextWakup: Schedule reminder for time %1\$tY-%1\$tm-%1\$td %1\$tH:%1\$tM:%1\$tS",
                    Date(scheduledTime))
            releaseWakeLockIfRequired()
            scheduleNextWakeupForOffset(scheduledTime - System.currentTimeMillis())
        }
    }

    /**
     * Schedule wakeup alarm for the sound notification pending intent
     *
     * @time the next wakeup time offset
     */
    private fun scheduleNextWakeupForOffset(timeOffset: Long) {
        Timber.d("scheduleNextWakup: called")
        if (wakeLock != null) {
            // use the manual timer action to trigger pending intent receiver instead instead of alarm manager
            timerSubscription = Observable
                    .just(true)
                    .delay(timeOffset, TimeUnit.MILLISECONDS)
                    .doOnNext { Timber.d("Wake from subscription") }
                    .subscribe { pendingIntentReceiver.onReceive(applicationContext, Intent()) }
        } else {
            JobRequest.Builder(RemindJob.TAG)
                    .setExact(timeOffset)
                    .build()
                    .schedule()
        }
    }

    /**
     * Stop scheduled wakeup alarm for the periodical sound notification
     */
    private fun stopWaking() {
        Timber.d("stopWaking() called")
        stopWaking(false)
        timerSubscription?.run {
            unsubscribe()
            timerSubscription = null
        }
        // cancel any pending remind jobs
        JobManager.instance().cancelAllForTag(RemindJob.TAG)
        pendingIntentReceiver.interruptReminderIfActive()
        releaseWakeLockIfRequired()
        cancelDismissNotification()
    }

    /**
     * Release a wakelock if exists
     */
    private fun releaseWakeLockIfRequired() {
        wakeLock?.let {
            Timber.d("releaseWakeLockIfRequired: release wake lock")
            try {
                if (it.isHeld) {
                    it.release()
                }
            } catch (ex: Exception) {
                Timber.e(ex)
            }
            wakeLock = null
        }
    }

    /**
     * Stop scheduled wakeup alarm for the periodical sound notification
     *
     * @param force whether to do force cancel independently of the active flag value. Needed for active development
     * when the pending intent may be changed or action scheduled by previous app run.
     */
    private fun stopWaking(force: Boolean) {
        Timber.d("stopWaking")
        if (active.compareAndSet(true, false) || force) {
            Timber.d("stopWaking: cancel reminder")
        }
    }


    @CallSuper
    override fun onDestroy() {
        mDispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
        Timber.d("onDestroy")
        // stop any scheduled alarms
        stopWaking()
        // unregister pending intent receiver
        unregisterReceiver(pendingIntentReceiver)
        // unregister ringer mode changed receiver
        unregisterReceiver(ringerModeChangedReceiver)
        // unregister zen mode changed observer
        applicationContext.contentResolver.unregisterContentObserver(zenModeObserver)
        // unregister dismiss notification receiver
        unregisterReceiver(stopRemindersReceiver)
        handler.removeCallbacksAndMessages(null)
        subscriptions.unsubscribe()
    }

    override fun onNotificationPosted(notificationData: NotificationData) {
        handler.post {
            Timber.d("onNotificationPosted: %s", notificationData)
            val existingElement = existingElement(notificationData)
            if (existingElement != null) {
                Timber.d("onNotificationPosted: removing previous %s", existingElement)
                availableNotifications.remove(existingElement)
            }
            availableNotifications.add(notificationData)
            if (!initializing) {
                mEventBus.send(NotificationsUpdatedEvent(notificationsData))
            }
            if (ready.get() && selectedApplications.get()!!.contains(notificationData.packageName)) {
                // check waking conditions only if notification has been posted for the monitored application to prevent
                // mRemainingRepeats overcome in case reminder is already stopped but new notification arrived from any not
                // monitored app
                if (limitReminderRepeats.get()!!) {
                    // reset reminder repeats such as new important notification has arrived
                    remainingRepeats = reminderRepeats.get()!!
                }
                if (!initializing) {
                    checkWakingConditions()
                }
            }
        }
    }

    private fun existingElement(notificationData: NotificationData): NotificationData? {
        var result: NotificationData? = null
        for (item in notificationsData) {
            if (TextUtils.equals(item.id, notificationData.id) &&
                    TextUtils.equals(item.packageName, notificationData.packageName)) {
                result = item
                break
            }
        }
        return result
    }

    override fun onNotificationRemoved(notificationData: NotificationData) {
        handler.post {
            Timber.d("onNotificationRemoved: %s", notificationData)
            if (!availableNotifications.remove(notificationData)) {
                Timber.w("onNotificationRemoved: removal failed")
            }
            if (!initializing) {
                mEventBus.send(NotificationsUpdatedEvent(notificationsData))
            }
            if (active.get() && !checkNotificationForAtLeastOnePackageExists(selectedApplications.get()!!, ignorePersistentNotifications.get()!!)) {
                // stop alarm if there are no more notifications to update
                stopWaking()
            }
        }
    }

    override fun onReady() {
        Timber.d("onReady")
        ready.set(true)
    }

    /**
     * Ignore all current notifications. The checkNotificationForAtLeastOnePackageExists will return
     * false unless there are new notifications created after this call.
     */
    fun ignoreAllCurrentNotifications() {
        ignoredNotifications.clear()
        ignoredNotifications.addAll(availableNotifications)
    }

    /**
     * Check whether the at least one notification for specified packages is present in the status bar
     *
     * @param packages      the collection of packages to check
     * @param ignoreOngoing whether the ongoing notifications should be ignored
     * @return true if notification for at least one package is found, false otherwise
     */
    fun checkNotificationForAtLeastOnePackageExists(packages: Collection<String>, ignoreOngoing: Boolean): Boolean {
        // Remove notifications that were already cancelled to avoid memory leaks.
        val copy: List<NotificationData> = ArrayList(ignoredNotifications)
        for (ignoredNotification in copy) {
            if (!notificationsData.contains(ignoredNotification)) {
                ignoredNotifications.remove(ignoredNotification)
            }
        }
        var result = false
        for (notificationData in notificationsData) {
            val packageName = notificationData.packageName
            Timber.d("checkNotificationForAtLeastOnePackageExists: checking package %1\$s", packageName)
            val contains = packages.contains(packageName)
            if (contains && ignoreOngoing && notificationData.flags and Notification.FLAG_ONGOING_EVENT == Notification.FLAG_ONGOING_EVENT) {
                Timber.d("checkNotificationForAtLeastOnePackageExists: found ongoing match which is requested to be skipped")
                continue
            }
            if (ignoredNotifications.contains(notificationData)) {
                Timber.d("checkNotificationForAtLeastOnePackageExists: notification ignored")
                continue
            }
            result = result or contains
            if (result) {
                Timber.d("checkNotificationForAtLeastOnePackageExists: found match for package %1\$s", packageName)
                break
            }
        }
        return result
    }

    override fun getNotificationsData(): List<NotificationData> {
        return Collections.unmodifiableList(ArrayList(availableNotifications))
    }

    override fun getIgnoredNotificationsData(): List<NotificationData> {
        return Collections.unmodifiableList(ArrayList(ignoredNotifications))
    }

    /**
     * The broadcast receiver for ringer mode changed events
     */
    internal inner class RingerModeChangedReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("onReceive: %s", intent)
            ringerModeUpdated()
        }

        /**
         * Called when ringer mode is updated
         */
        private fun ringerModeUpdated() {
            ringerMode.set(audioManager.ringerMode)
        }

        init {
            // update to initial value
            ringerModeUpdated()
        }
    }

    /**
     * The content observer for the DND mode changes
     */
    private inner class ZenModeObserver internal constructor(handler: Handler) : ContentObserver(handler) {
        val DND_OFF = 0
        override fun deliverSelfNotifications(): Boolean {
            return super.deliverSelfNotifications()
        }

        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            zenModeUpdated()
        }

        private fun zenModeUpdated() {
            Timber.d("zenModeUpdated() called")
            try {
                val zenMode = Settings.Global.getInt(contentResolver, "zen_mode")
                Timber.d("zenModeUpdated: %d", zenMode)
                dndEnabled.set(zenMode != DND_OFF)
            } catch (e: SettingNotFoundException) {
                Timber.e(e)
            }
        }

        init {
            zenModeUpdated()
        }
    }

    /**
     * The broadcast receiver for the pending intent action fired by alarm manager
     */
    internal inner class ScheduledSoundNotificationReceiver : BroadcastReceiver() {
        /**
         * Media player used to play notification sound
         */
        private val mediaPlayer: MediaPlayer by lazy {
            MediaPlayer()
                    .apply {
                        setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                    }
        }

        /**
         * The reminder subscription
         */
        private val reminderSubscription = CompositeSubscription()

        /**
         * Reference to the current device wake lock used while vibrator is active
         */
        var vibrationWakeLock: WakeLock? = null

        override fun onReceive(context: Context, intent: Intent) {
            handler.post {
                Timber.d("onReceive: current thread %1\$s", Thread.currentThread().name)
                if (!active.get()) {
                    Timber.w("onReceive: Invalid service activity state, stopping reminder")
                    stopWaking(true)
                    return@post
                }
                if (!remindWhenScreenIsOn.get()!! && isScreenOn(applicationContext)) {
                    Timber.d("onReceive: The screen is on and remind when screen is on is not specified, skip notification")
                } else if (PhoneStateUtils.isCallActive(applicationContext) && respectPhoneCalls.get()!!) {
                    Timber.d("onReceive: The phone call is active and respect phone calls setting is specified, skip notification")
                } else {
                    Timber.d("onReceive: The screen is off, notify")
                    interruptReminderIfActive()
                    val playbackCompleted = playReminderCompletable()
                    val vibrationCompletedAtLeastOnce: Completable
                    // Start without a delay
                    // Each element then alternates between vibrate, sleep, vibrate, sleep...
                    if (vibrate.get()!! && (!respectRingerMode.get()!! || ringerMode.get() != AudioManager.RINGER_MODE_SILENT)) {
                        // if vibration is turned on and phone is not in silent mode or respect ringer mode option is disabled
                        val pattern = parseVibrationPattern(vibrationPattern.get()!!)
                        vibrationCompletedAtLeastOnce = Single.fromCallable {
                            var vibrationDuration: Long = 0
                            for (step in pattern) {
                                vibrationDuration += step
                            }
                            vibrationWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                    "MissedNotificationsReminder:VIBRATOR_LOCK").apply { acquire(2 * vibrationDuration) }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(pattern, 0)
                            }
                            Timber.d("Minimum vibration duration: %d", vibrationDuration)
                            vibrationDuration
                        }
                                .flatMapCompletable { vibrationDuration -> Completable.timer(vibrationDuration, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread()) }
                                .doOnError { t -> Timber.e(t) }
                                .onErrorComplete()
                                .doOnCompleted { Timber.d("Minimum vibration completed") }
                    } else {
                        vibrationCompletedAtLeastOnce = Completable.complete()
                    }
                    // await for both playback and minimum vibration duration to complete
                    reminderSubscription.add(Completable.merge(
                            playbackCompleted,
                            vibrationCompletedAtLeastOnce)
                            .doOnCompleted { reminderCompleted() }
                            .doOnUnsubscribe { cancelVibrator() }
                            .subscribe { Timber.d("Reminder completed") })
                }
            }
        }

        private fun reminderCompleted() {
            scheduleNextWakeup(true)
            actualizeNotificationData()
            cancelVibrator()
            // notify listeners about reminder completion
            mEventBus.send(RemindEvents.REMINDER_COMPLETED)
        }

        private fun cancelVibrator() {
            Timber.d("cancelVibrator() called")
            vibrator.cancel()
            vibrationWakeLock?.run {
                try {
                    if (isHeld) {
                        release()
                    }
                } catch (ex: Exception) {
                    Timber.e(ex)
                }
                vibrationWakeLock = null
            }
        }

        private fun playReminderCompletable(): Completable {
            return Observable.amb(
                    Completable.timer(5, TimeUnit.SECONDS)
                            .andThen(Observable.error(Error("onReceive: media player initializes too long, didn't receive onComplete for 5 seconds."))),
                    Observable.create({ emitter: Emitter<Any> ->
                        try {
                            mediaPlayer.reset()
                            // use alternative stream if respect ringer mode is disabled
                            val streamType = if (respectRingerMode.get()!!) AudioManager.STREAM_NOTIFICATION else AudioManager.STREAM_ALARM
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                mediaPlayer.setAudioAttributes(AudioAttributes.Builder()
                                        .setLegacyStreamType(streamType)
                                        .build())
                            } else {
                                @Suppress("DEPRECATION")
                                mediaPlayer.setAudioStreamType(streamType)
                            }
                            if (respectRingerMode.get()!! && (ringerMode.get() == AudioManager.RINGER_MODE_VIBRATE || ringerMode.get() == AudioManager.RINGER_MODE_SILENT)) {
                                // mute sound explicitly for silent ringer modes because some user claims that sound is not muted on their devices in such cases
                                mediaPlayer.setVolume(0f, 0f)
                            } else {
                                mediaPlayer.setVolume(1f, 1f)
                            }
                            mediaPlayer.setOnErrorListener { mp, what, extra ->
                                Timber.e("MediaPlayer error %1\$d %2\$d", what, extra)
                                emitter.onError(Error(String.format("MediaPlayer error %1\$d %2\$d", what, extra)))
                                false
                            }
                            mediaPlayer.setOnCompletionListener {
                                Timber.d("completion")
                                emitter.onCompleted()
                            }
                            emitter.setCancellation {
                                Timber.d("cancellation 1")
                                if (mediaPlayer.isPlaying) {
                                    mediaPlayer.stop()
                                }
                                mediaPlayer.setOnCompletionListener(null)
                                mediaPlayer.setOnErrorListener(null)
                                mediaPlayer.setOnPreparedListener(null)
                            }
                            // get the selected notification sound URI
                            val ringtone = reminderRingtone.get()
                            if (TextUtils.isEmpty(ringtone)) {
                                Timber.w("The reminder ringtone is not specified. Skip playing")
                                emitter.onCompleted()
                            } else {
                                Timber.d("onReceive: ringtone %1\$s", ringtone)
                                val notification = Uri.parse(ringtone)
                                mediaPlayer.setOnPreparedListener {
                                    Timber.d("MediaPlayer prepared")
                                    mediaPlayer.start()
                                    emitter.onNext(notification)
                                }
                                mediaPlayer.setDataSource(applicationContext, notification)
                                mediaPlayer.prepareAsync()
                            }
                        } catch (ex: Exception) {
                            Timber.e(ex)
                            emitter.onError(ex)
                        }
                    }, Emitter.BackpressureMode.NONE))
                    .share()
                    .toCompletable()
                    .doOnError { t: Throwable? -> Timber.e(t) }
                    .onErrorComplete()
                    .doOnCompleted { Timber.d("Playback completed") }
        }

        private fun parseVibrationPattern(rawPattern: String): LongArray {
            // This code assumes the pattern string matches regexp \d+(\s*,\s*\d+)*
            val components = rawPattern.split("\\s*,\\s*".toRegex()).toTypedArray()
            val parsedPattern = LongArray(components.size)
            for (i in components.indices) {
                parsedPattern[i] = components[i].toLong()
            }
            return parsedPattern
        }

        /**
         * Is the screen of the device on.
         *
         * @param context the context
         * @return true when (at least one) screen is on
         */
        private fun isScreenOn(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                var screenOn = false
                for (display in dm.displays) {
                    if (display.state != Display.STATE_OFF) {
                        screenOn = true
                    }
                }
                screenOn
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                    powerManager.isInteractive
                } else {
                    @Suppress("DEPRECATION")
                    powerManager.isScreenOn
                }
            }
        }

        /**
         * Interrupt previously started reminder if it is active
         */
        fun interruptReminderIfActive() {
            reminderSubscription.clear()
        }
    }

    /**
     * The broadcast receiver for the pending intent fired when the user wants to stop reminders by
     * cancelling the dismiss notification.
     */
    internal inner class StopRemindersReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            handler.post {
                Timber.d("dismiss notification cancelled")
                ignoreAllCurrentNotifications()
                stopWaking()
            }
        }
    }

    @dagger.Module
    abstract class Module {
        @ContributesAndroidInjector
        abstract fun contribute(): ReminderNotificationListenerService?
    }

    companion object {
        /**
         * Action for the pending intent used by alarm manager to periodically wake the device and send broadcast with this
         * action
         */
        val PENDING_INTENT_ACTION = ReminderNotificationListenerService::class.qualifiedName

        /**
         * Action for the pending intent sent when dismiss notification has been cancelled.
         */
        val STOP_REMINDERS_INTENT_ACTION = ReminderNotificationListenerService::class.qualifiedName + ".STOP_REMINDERS_INTENT"

        /**
         * Notification id for the dismiss notification. It must be unique in an app, but since we only
         * generate this notification and there could be only one of them, it is a constant.
         */
        const val DISMISS_NOTIFICATION_ID = 42
    }
}