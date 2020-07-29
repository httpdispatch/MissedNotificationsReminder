package com.app.missednotificationsreminder.util

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.doOnApplyWindowInsets(f: (View, WindowInsetsCompat, InitialPadding, InitialMargins) -> Unit) {
    // Create a snapshot of the view's padding state
    val initialPadding = recordInitialPadding()
    val initialMargins = recordInitialMargins()
    // Set an actual OnApplyWindowInsetsListener which proxies to the given
    // lambda, also passing in the original padding state
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        f(v, insets, initialPadding, initialMargins)
        // Always return the insets, so that children can also use them
        insets
    }
    // request some insets
    requestApplyInsetsWhenAttached()
}

data class InitialPadding(val left: Int, val top: Int,
                          val right: Int, val bottom: Int)

data class InitialMargins(val left: Int, val top: Int,
                          val right: Int, val bottom: Int)

fun View.recordInitialPadding() = InitialPadding(
        paddingLeft, paddingTop, paddingRight, paddingBottom)

fun View.recordInitialMargins(): InitialMargins =
        with(layoutParams as ViewGroup.MarginLayoutParams) {
            return InitialMargins(leftMargin, topMargin, rightMargin, bottomMargin)
        }

fun View.requestApplyInsetsWhenAttached() {
    if (ViewCompat.isAttachedToWindow(this)) {
        ViewCompat.requestApplyInsets(this)
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                ViewCompat.requestApplyInsets(v)
            }

            override fun onViewDetachedFromWindow(v: View) {}
        })
    }
}