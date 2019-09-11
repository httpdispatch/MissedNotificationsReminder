package com.app.missednotificationsreminder.service;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.app.missednotificationsreminder.data.model.NotificationData;
import com.app.missednotificationsreminder.di.qualifiers.CreateDismissNotification;
import com.app.missednotificationsreminder.service.util.NotificationParser;
import com.app.missednotificationsreminder.service.util.StatusBarWindowUtils;
import com.f2prateek.rx.preferences.Preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * The service to monitor all status bar notifications (compatibility version for Android 4.0-4.2).
 *
 * @author Eugene Popovich
 */
public abstract class AbstractReminderNotificationListenerService extends AccessibilityService implements ReminderNotificationListenerServiceInterface {
    /**
     * Notification parser used to retrieve notification information
     */
    NotificationParser mNotificationParser;
    /**
     * Utilities to work with status bar window
     */
    StatusBarWindowUtils mStatusBarWindowUtils;

    @Inject @CreateDismissNotification Preference<Boolean> createDismissNotification;

    PublishSubject<List<NotificationData>> mStatusBarContentChangedRemovedNotificationsNotifier = PublishSubject.create();

    CompositeSubscription mSubscription = new CompositeSubscription();

    @Override public void onCreate() {
        super.onCreate();
        mNotificationParser = new NotificationParser(getApplicationContext());
        mStatusBarWindowUtils = new StatusBarWindowUtils(getPackageManager());
        mSubscription.add(mStatusBarContentChangedRemovedNotificationsNotifier
                .debounce(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(notificationData -> {
                    for (NotificationData data : notificationData) {
                        if(!createDismissNotification.get() || getIgnoredNotificationsData().contains(data)) {
                            onNotificationRemoved(data);
                        }
                    }
                }));
        onReady();
    }

    @Override public void onDestroy() {
        super.onDestroy();
        mSubscription.clear();
    }

    @Override public void actualizeNotificationData() {
        // do nothing
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        Timber.d("onAccessibilityEvent: received, windowid: %1$d; type: %2$s", accessibilityEvent.getWindowId(), AccessibilityEvent.eventTypeToString(accessibilityEvent.getEventType()));
        switch (accessibilityEvent.getEventType()) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                Timber.d("onAccessibilityEvent: notification state changed");
                if (accessibilityEvent.getParcelableData() != null &&
                        accessibilityEvent.getParcelableData() instanceof Notification) {
                    Notification n = (Notification) accessibilityEvent.getParcelableData();
                    String packageName = accessibilityEvent.getPackageName().toString();
                    Timber.d("onAccessibilityEvent: notification posted package: %1$s; notification: %2$s", packageName, n);
                    // fire event
                    onNotificationPosted(new ExtendedNotificationData(
                            mNotificationParser.getNotificationTitle(n, packageName),
                            packageName,
                            SystemClock.elapsedRealtime(),
                            n.flags));
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                // auto clear notifications for launched application (TYPE_WINDOW_CONTENT_CHANGED not always generated
                // when app is clicked or cleared)
                Timber.d("onAccessibilityEvent: window state changed");
                if (accessibilityEvent.getPackageName() != null) {
                    String packageName = accessibilityEvent.getPackageName().toString();
                    Timber.d("onAccessibilityEvent: window state has been changed for package %1$s", packageName);
                    removeNotificationsFor(packageName);
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                // auto clear notifications when cleared from notifications bar (old api, Android < 4.3)
                if (mStatusBarWindowUtils.isStatusBarWindowEvent(accessibilityEvent)) {
                    Timber.d("onAccessibilityEvent: status bar content changed");
                    mStatusBarContentChangedRemovedNotificationsNotifier.onNext(getRemovedNotifications(accessibilityEvent));
                }
                break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                // auto clear notifications when clear all notifications button clicked (TYPE_WINDOW_CONTENT_CHANGED not always generated
                // when this event occurs so need to handle this manually
                //
                // also handle notification clicked event
                Timber.d("onAccessibilityEvent: view clicked");
                if (mStatusBarWindowUtils.isStatusBarWindowEvent(accessibilityEvent)) {
                    Timber.d("onAccessibilityEvent: status bar content clicked");
                    if (mStatusBarWindowUtils.isClearNotificationsButtonEvent(accessibilityEvent)) {
                        // if clicked image view element with the clear button name content description
                        Timber.d("onAccessibilityEvent: clear notifications button clicked");
                        for (NotificationData data : getNotificationsData()) {
                            onNotificationRemoved(data);
                        }
                    } else {
                        // update notifications if another view is clicked
                        mStatusBarContentChangedRemovedNotificationsNotifier.onNext(getRemovedNotifications(accessibilityEvent));
                    }
                }
                break;
        }
    }

    @Override public void onInterrupt() {
    }


    /**
     * Get the removed notification information from the node information of the accessibility event
     * <br>
     * The algorithm is not exact. All the strings are recursively retrieved in the view hierarchy and then
     * titles are compared with the available notifications
     *
     * @param accessibilityEvent
     */
    private List<NotificationData> getRemovedNotifications(AccessibilityEvent accessibilityEvent) {
        List<NotificationData> result = new ArrayList<>();
        AccessibilityNodeInfo node = accessibilityEvent.getSource();
        node = mStatusBarWindowUtils.getRootNode(node);
        Set<String> titles = node == null ? Collections.emptySet() : recursiveGetStrings(node);
        for (NotificationData data : getNotificationsData()) {
            if (!titles.contains(((ExtendedNotificationData) data).id)) {
                Timber.d("updateNotifications: removed %s", data);
                // if the title is absent in the view hierarchy remove notification from available notifications
                result.add(data);
            }
        }
        return result;
    }

    /**
     * Remove all notifications from the available notifications with the specified package name
     *
     * @param packageName
     */
    private void removeNotificationsFor(String packageName) {
        Timber.d("removeNotificationsFor: %1$s", packageName);
        for (NotificationData data : getNotificationsData()) {
            if (TextUtils.equals(packageName, data.packageName)) {
                onNotificationRemoved(data);
            }
        }
    }

    /**
     * Get all the text information from the node view hierarchy
     *
     * @param node
     * @return
     */
    private Set<String> recursiveGetStrings(AccessibilityNodeInfo node) {
        Set<String> strings = new HashSet<>();
        if (node != null) {
            if (node.getText() != null) {
                strings.add(node.getText().toString());
                Timber.d("recursiveGetStrings: %1$s", node.getText().toString());
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                strings.addAll(recursiveGetStrings(node.getChild(i)));
            }
        }
        return strings;
    }

    /**
     * Simple notification information holder
     */
    class ExtendedNotificationData extends NotificationData {

        public ExtendedNotificationData(CharSequence title, CharSequence packageName, long foundAtTime, int flags) {
            this(title == null ? null : title.toString(),
                    packageName == null ? null : packageName.toString(),
                    foundAtTime,
                    flags);
        }

        public ExtendedNotificationData(String title, String packageName, long foundAtTime, int flags) {
            super(title, packageName, foundAtTime, flags);
        }
    }

}
