package com.app.missednotificationsreminder.ui.activity.common

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.View
import com.app.missednotificationsreminder.util.resources.resolveBooleanAttribute
import timber.log.Timber

class CommonActivityLifecycleCallback : Application.ActivityLifecycleCallbacks {
    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Timber.d("onActivityCreated() called")
        with(activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val decorView: View = window.decorView
                decorView.systemUiVisibility = decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    val lightNavigationBar: Boolean = resolveBooleanAttribute(android.R.attr.windowLightNavigationBar, false)
                    if (!lightNavigationBar) {
                        decorView.systemUiVisibility = decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                    }
                    val lightStatusBar: Boolean = resolveBooleanAttribute(android.R.attr.windowLightNavigationBar, false)
                    if (!lightStatusBar) {
                        decorView.systemUiVisibility = decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                    }
                }
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {
    }
}