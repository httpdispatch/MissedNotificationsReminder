package com.app.missednotificationsreminder.ui.widget.dialog

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import timber.log.Timber

open class LifecycleAlertDialog : AlertDialog, LifecycleOwner {
    private var lifecycleRegistry = LifecycleRegistry(this)

    protected constructor(context: Context) : super(context) {}
    protected constructor(context: Context, themeResId: Int) : super(context, themeResId) {}
    protected constructor(context: Context, cancelable: Boolean, cancelListener: DialogInterface.OnCancelListener?) : super(context, cancelable, cancelListener) {}

    override fun onAttachedToWindow() {
        Timber.d("onAttachedToWindow() called")
        super.onAttachedToWindow()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onDetachedFromWindow() {
        Timber.d("onDetachedFromWindow() called")
        if (lifecycleRegistry.currentState != Lifecycle.State.INITIALIZED) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            lifecycleRegistry = LifecycleRegistry(this)
        }
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }
}