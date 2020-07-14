package com.app.missednotificationsreminder.ui.widget.recyclerview

import android.view.View
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import timber.log.Timber
import java.lang.ref.WeakReference

abstract class LifecycleViewHolder(itemView: View, parentLifecycle: Lifecycle) : RecyclerView.ViewHolder(itemView), LifecycleOwner {
    private var lifecycleRegistry = LifecycleRegistry(this)
    private val parentLifecycleReference = WeakReference(parentLifecycle)
    private val parentLifecycleEventObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            onDestroy()
        }
    }

    @CallSuper
    open fun onAttached() {
        Timber.d("onAttached")
        parentLifecycleReference.get()
                ?.takeIf { it.currentState != Lifecycle.State.DESTROYED }
                ?.apply {
                    addObserver(parentLifecycleEventObserver)
                    // use STARTED instead of CREATED so it may be properly used with databinding and LiveData
                    lifecycleRegistry.currentState = Lifecycle.State.STARTED
                }
    }

    @CallSuper
    fun onDetached() {
        Timber.d("onDetached")
        onDestroy()
    }

    private fun onDestroy() {
        Timber.d("onDestroy()")
        if (lifecycleRegistry.currentState != Lifecycle.State.INITIALIZED) {
            parentLifecycleReference.get()?.apply { removeObserver(parentLifecycleEventObserver) }
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            parentLifecycleReference.get()
                    ?.takeIf { it.currentState != Lifecycle.State.DESTROYED }
                    ?.run {
                        // lifecycle can't be fully reused after destroyed
                        lifecycleRegistry = LifecycleRegistry(this@LifecycleViewHolder)
                    }
        }
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

}