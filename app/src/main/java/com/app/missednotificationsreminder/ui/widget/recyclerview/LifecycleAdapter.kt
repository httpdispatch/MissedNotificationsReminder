package com.app.missednotificationsreminder.ui.widget.recyclerview

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import timber.log.Timber

abstract class LifecycleAdapter<VH : LifecycleViewHolder> : RecyclerView.Adapter<VH>(), LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    private val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View?) {
            Timber.d("onViewAttachedToWindow: $v")
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }

        override fun onViewDetachedFromWindow(v: View?) {
            Timber.d("onViewDetachedFromWindow: $v")
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
    }

    init {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        Timber.d("onAttachedToRecyclerView")
        if (recyclerView.isAttachedToWindow) {
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }
        recyclerView.addOnAttachStateChangeListener(attachListener)
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        Timber.d("onDetachedFromRecyclerView")
        recyclerView.removeOnAttachStateChangeListener(attachListener)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onViewAttachedToWindow(holder: VH) {
        super.onViewAttachedToWindow(holder)
        holder.onAttached()
    }

    override fun onViewDetachedFromWindow(holder: VH) {
        super.onViewDetachedFromWindow(holder)
        holder.onDetached()
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

}
