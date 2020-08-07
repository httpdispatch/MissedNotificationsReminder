package com.app.missednotificationsreminder.service

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.os.SystemClock
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.lifecycleScope
import com.app.missednotificationsreminder.service.data.model.NotificationData
import com.app.missednotificationsreminder.service.util.NotificationParser
import com.app.missednotificationsreminder.service.util.StatusBarWindowUtils
import com.app.missednotificationsreminder.util.coroutines.debounce
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

/**
 * The service to monitor all status bar notifications (compatibility version for Android 4.0-4.2).
 */
abstract class AbstractReminderNotificationListenerService : AccessibilityService(), ReminderNotificationListenerServiceInterface {
    /**
     * Notification parser used to retrieve notification information
     */
    private val notificationParser by lazy { NotificationParser(applicationContext) }

    /**
     * Utilities to work with status bar window
     */
    private val statusBarWindowUtils by lazy { StatusBarWindowUtils(packageManager) }

    val statusBarContentChangedRemovedNotification: (List<NotificationData>) -> Unit by lazy {
        debounce<List<NotificationData>>(
                1000L,
                lifecycleScope) { notificationData ->
            lifecycleScope.launch {
                for (data in notificationData) {
                    if (!createDismissNotification || ignoredNotificationsData.contains(data)) {
                        onNotificationRemoved(data)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        onReady()
    }

    override suspend fun actualizeNotificationData() {
        // do nothing
    }

    override fun onAccessibilityEvent(accessibilityEvent: AccessibilityEvent) {
        Timber.d("onAccessibilityEvent: received, windowid: %1\$d; type: %2\$s", accessibilityEvent.windowId, AccessibilityEvent.eventTypeToString(accessibilityEvent.eventType))
        when (accessibilityEvent.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                Timber.d("onAccessibilityEvent: notification state changed")
                if (accessibilityEvent.parcelableData != null &&
                        accessibilityEvent.parcelableData is Notification) {
                    val n = accessibilityEvent.parcelableData as Notification
                    val packageName = accessibilityEvent.packageName.toString()
                    Timber.d("onAccessibilityEvent: notification posted package: %1\$s; notification: %2\$s", packageName, n)
                    lifecycleScope.launch {
                        // fire event
                        onNotificationPosted(ExtendedNotificationData(
                                notificationParser.getNotificationTitle(n, packageName),
                                packageName,
                                SystemClock.elapsedRealtime(),
                                n.flags))
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // auto clear notifications for launched application (TYPE_WINDOW_CONTENT_CHANGED not always generated
                // when app is clicked or cleared)
                Timber.d("onAccessibilityEvent: window state changed")
                if (accessibilityEvent.packageName != null) {
                    val packageName = accessibilityEvent.packageName.toString()
                    Timber.d("onAccessibilityEvent: window state has been changed for package %1\$s", packageName)
                    removeNotificationsFor(packageName)
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ->                 // auto clear notifications when cleared from notifications bar (old api, Android < 4.3)
                if (statusBarWindowUtils.isStatusBarWindowEvent(accessibilityEvent)) {
                    Timber.d("onAccessibilityEvent: status bar content changed")
                    statusBarContentChangedRemovedNotification(getRemovedNotifications(accessibilityEvent))
                }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // auto clear notifications when clear all notifications button clicked (TYPE_WINDOW_CONTENT_CHANGED not always generated
                // when this event occurs so need to handle this manually
                //
                // also handle notification clicked event
                Timber.d("onAccessibilityEvent: view clicked")
                if (statusBarWindowUtils.isStatusBarWindowEvent(accessibilityEvent)) {
                    Timber.d("onAccessibilityEvent: status bar content clicked")
                    if (statusBarWindowUtils.isClearNotificationsButtonEvent(accessibilityEvent)) {
                        // if clicked image view element with the clear button name content description
                        lifecycleScope.launch {
                            Timber.d("onAccessibilityEvent: clear notifications button clicked")
                            for (data in notificationsData) {
                                onNotificationRemoved(data)
                            }
                        }
                    } else {
                        // update notifications if another view is clicked
                        statusBarContentChangedRemovedNotification(getRemovedNotifications(accessibilityEvent))
                    }
                }
            }
        }
    }

    override fun onInterrupt() {}

    /**
     * Get the removed notification information from the node information of the accessibility event
     * <br></br>
     * The algorithm is not exact. All the strings are recursively retrieved in the view hierarchy and then
     * titles are compared with the available notifications
     *
     * @param accessibilityEvent
     */
    private fun getRemovedNotifications(accessibilityEvent: AccessibilityEvent): List<NotificationData> {
        val result: MutableList<NotificationData> = ArrayList()
        var node = accessibilityEvent.source
        node = statusBarWindowUtils.getRootNode(node)
        val titles = node?.let { recursiveGetStrings(it) } ?: emptySet()
        for (data in notificationsData) {
            if (!titles.contains((data as ExtendedNotificationData).id)) {
                Timber.d("updateNotifications: removed %s", data)
                // if the title is absent in the view hierarchy remove notification from available notifications
                result.add(data)
            }
        }
        return result
    }

    /**
     * Remove all notifications from the available notifications with the specified package name
     *
     * @param packageName
     */
    private fun removeNotificationsFor(packageName: String) {
        lifecycleScope.launch {
            Timber.d("removeNotificationsFor: %1\$s", packageName)
            for (data in notificationsData) {
                if (TextUtils.equals(packageName, data.packageName)) {
                    onNotificationRemoved(data)
                }
            }
        }
    }

    /**
     * Get all the text information from the node view hierarchy
     *
     * @param node
     * @return
     */
    private fun recursiveGetStrings(node: AccessibilityNodeInfo?): Set<String> {
        val strings: MutableSet<String> = HashSet()
        if (node != null) {
            if (node.text != null) {
                strings.add(node.text.toString())
                Timber.d("recursiveGetStrings: %1\$s", node.text.toString())
            }
            for (i in 0 until node.childCount) {
                strings.addAll(recursiveGetStrings(node.getChild(i)))
            }
        }
        return strings
    }

    /**
     * Simple notification information holder
     */
    internal inner class ExtendedNotificationData(title: String, packageName: String, foundAtTime: Long, flags: Int) : NotificationData(title, packageName, foundAtTime, flags) {
        constructor(title: CharSequence, packageName: CharSequence, foundAtTime: Long, flags: Int) : this(title.toString(),
                packageName.toString(),
                foundAtTime,
                flags)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false
            return true
        }

    }
}