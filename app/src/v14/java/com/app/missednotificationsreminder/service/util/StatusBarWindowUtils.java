package com.app.missednotificationsreminder.service.util;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import timber.log.Timber;

/**
 * The utility class for the various functionality related to the status bar system UI window.
 */
public class StatusBarWindowUtils {

    /**
     * The package name used by system ui
     */
    public static final String SYSTEMUI_PACKAGE_NAME = "com.android.systemui";
    /**
     * The status bar window id used in accessibility events. Initialized and used in the
     * {@linkplain #isStatusBarWindowEvent(AccessibilityEvent) isStatusBarWindowEvent} method
     */
    int mStatusBarWindowId = -1;
    /**
     * The content description for the clear all notifications button. Initialized in the
     * {@linkplain #findClearAllButton() findClearAllButton} method
     */
    String mClearButtonName = null;
    /**
     * The package manager
     */
    PackageManager mPackageManager;

    /**
     * Construct instance of status bar window utils
     *
     * @param packageManager the package manager
     */
    public StatusBarWindowUtils(PackageManager packageManager) {
        mPackageManager = packageManager;
        findClearAllButton();
    }

    /**
     * Find "clear all notifications" button accessibility text used by the systemui application
     */
    private void findClearAllButton() {
        Timber.d("findClearAllButton: called");
        Resources res;
        try {
            res = mPackageManager.getResourcesForApplication(SYSTEMUI_PACKAGE_NAME);
            int i = res.getIdentifier("accessibility_clear_all", "string", "com.android.systemui");
            if (i != 0) {
                mClearButtonName = res.getString(i);
            }
        } catch (Exception exp) {
            Timber.e(exp, null);
        }
    }

    /**
     * Check whether accessibility event belongs to the status bar window by checking event package
     * name and window id
     *
     * @param accessibilityEvent
     * @return
     */
    public boolean isStatusBarWindowEvent(AccessibilityEvent accessibilityEvent) {
        boolean result = false;
        if (!SYSTEMUI_PACKAGE_NAME.equals(accessibilityEvent.getPackageName())) {
            Timber.v("isStatusBarWindowEvent: not system ui package");
        } else if (mStatusBarWindowId != -1) {
            // if status bar window id is already initialized
            result = accessibilityEvent.getWindowId() == mStatusBarWindowId;
            Timber.v("isStatusBarWindowEvent: comparing window ids %1$d %2$d, result %3$b", mStatusBarWindowId, accessibilityEvent.getWindowId(), result);
        } else {
            Timber.v("isStatusBarWindowEvent: status bar window id not initialized, starting detection");
            AccessibilityNodeInfo node = accessibilityEvent.getSource();
            node = getRootNode(node);

            if (hasClearButton(node)) {
                Timber.v("isStatusBarWindowEvent: the root node has clear text button in the view hierarchy. Remember window id for future use");
                mStatusBarWindowId = accessibilityEvent.getWindowId();
                result = isStatusBarWindowEvent(accessibilityEvent);
            }
            if (!result) {
                Timber.v("isStatusBarWindowEvent: can't initizlie status bar window id");
            }
        }
        return result;
    }

    /**
     * Get the root node for the specified node if it is not null
     *
     * @param node
     * @return the root node for the specified node in the view hierarchy
     */
    public AccessibilityNodeInfo getRootNode(AccessibilityNodeInfo node) {
        if (node != null) {
            // workaround for Android 4.0.3 to avoid NPE. Should to remember first call of the node.getParent() such
            // as second call may return null
            AccessibilityNodeInfo parent;
            while ((parent = node.getParent()) != null) {
                node = parent;
            }
        }
        return node;
    }

    /**
     * Check whether the node has clear notifications button in the view hierarchy
     *
     * @param node
     * @return
     */
    private boolean hasClearButton(AccessibilityNodeInfo node) {
        boolean result = false;
        if (node == null) {
            return result;
        }
        Timber.d("hasClearButton: %1$s %2$d %3$s", node.getClassName(), node.getWindowId(), node.getContentDescription());
        if (TextUtils.equals(mClearButtonName, node.getContentDescription())) {
            result = true;
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                if (hasClearButton(node.getChild(i))) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Check whether the accessibility event is generated by the clear all notifications button
     *
     * @param accessibilityEvent
     * @return
     */
    public boolean isClearNotificationsButtonEvent(AccessibilityEvent accessibilityEvent) {
        return TextUtils.equals(accessibilityEvent.getClassName(), android.widget.ImageView.class.getName())
                && TextUtils.equals(accessibilityEvent.getContentDescription(), mClearButtonName);
    }
}
