package com.app.missednotificationsreminder.binding.util

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.util.doOnApplyWindowInsets
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import com.squareup.picasso.RequestCreator
import timber.log.Timber

/**
 * Bind the [ImageView] view with the [RequestCreator]
 *
 * @param view           the view to bind request creator with
 * @param requestCreator the request creator to bind the view with
 */
@BindingAdapter("request")
fun loadImage(view: ImageView, requestCreator: RequestCreator?) {
    if (requestCreator == null) {
        view.setImageBitmap(null)
    } else {
        // load
        try {
            // load
            requestCreator.into(view)
        } catch (e: Exception) {
            // catch unexpected IllegalArgumentException errors
            Timber.e(e)
        }
    }
}

@BindingAdapter("leftIndex", "rightIndex")
fun bindRangeBar(view: RangeSlider,
                 left: Int,
                 right: Int) {
    view.setValues(left.toFloat(), right.toFloat())
}

@BindingAdapter("onRangeChanged")
fun onRangeChanged(view: RangeSlider,
                   rangeChanged: RangeChangedListener) {
    var listener = view.getTag(R.id.binded) as RangeSlider.OnChangeListener?
    if (listener != null) {
        view.removeOnChangeListener(listener)
    }
    listener = RangeSlider.OnChangeListener { slider, _, fromUser ->
        rangeChanged.rangeChanged(slider.values[0].toInt(), slider.values[1].toInt(), fromUser)
    }
    view.addOnChangeListener(listener)
    view.setTag(R.id.binded, listener)
}

interface RangeChangedListener {
    fun rangeChanged(left: Int, right: Int, fromUser: Boolean)
}

@BindingAdapter("onProgressChanged")
fun onProgressChanged(view: Slider,
                      progressChanged: ProgressChangedListener) {
    var listener = view.getTag(R.id.binded) as Slider.OnChangeListener?
    if (listener != null) {
        view.removeOnChangeListener(listener)
    }
    listener = Slider.OnChangeListener { slider, _, fromUser ->
        progressChanged.onChanged(slider.value.toInt(), fromUser)
    }
    view.addOnChangeListener(listener)
    view.setTag(R.id.binded, listener)
}


interface ProgressChangedListener {
    fun onChanged(value: Int, fromUser: Boolean)
}

@BindingAdapter(value = [
    "paddingLeftSystemWindowInsets",
    "paddingTopSystemWindowInsets",
    "paddingRightSystemWindowInsets",
    "paddingBottomSystemWindowInsets",
    "marginLeftSystemWindowInsets",
    "marginTopSystemWindowInsets",
    "marginRightSystemWindowInsets",
    "marginBottomSystemWindowInsets"
], requireAll = false)
fun addSystemInsets(view: View,
                    applyLeftPadding: Boolean,
                    applyTopPadding: Boolean,
                    applyRightPadding: Boolean,
                    applyBottomPadding: Boolean,
                    applyLeftMargin: Boolean,
                    applyTopMargin: Boolean,
                    applyRightMargin: Boolean,
                    applyBottomMargin: Boolean) {
    if (view.getTag(R.id.system_insets_binded) != null) {
        // already binded;
        return
    }
    view.setTag(R.id.system_insets_binded, true)
    view.doOnApplyWindowInsets { _, insets, initialPadding, initialMargins ->
        if (applyTopPadding || applyBottomPadding || applyLeftPadding || applyRightPadding) {
            view.setPadding(initialPadding.left + if (applyLeftPadding) insets.systemWindowInsetLeft else 0,
                    initialPadding.top + if (applyTopPadding) insets.systemWindowInsetTop else 0,
                    initialPadding.right + if (applyRightPadding) insets.systemWindowInsetRight else 0,
                    initialPadding.bottom + if (applyBottomPadding) insets.systemWindowInsetBottom else 0)
        }
        if (applyTopMargin || applyBottomMargin || applyLeftMargin || applyRightMargin) {
            val layoutParams = view.layoutParams as MarginLayoutParams
            if (applyLeftMargin) {
                val newValue: Int = initialMargins.left + insets.systemWindowInsetLeft
                if (layoutParams.leftMargin != newValue) {
                    layoutParams.leftMargin = newValue
                }
            }
            if (applyTopMargin) {
                val newValue: Int = initialMargins.top + insets.systemWindowInsetTop
                if (layoutParams.topMargin != newValue) {
                    layoutParams.topMargin = newValue
                }
            }
            if (applyRightMargin) {
                val newValue: Int = initialMargins.right + insets.systemWindowInsetRight
                if (layoutParams.rightMargin != newValue) {
                    layoutParams.rightMargin = newValue
                }
            }
            if (applyBottomMargin) {
                val newValue: Int = initialMargins.bottom + insets.systemWindowInsetBottom
                if (layoutParams.bottomMargin != newValue) {
                    layoutParams.bottomMargin = newValue
                }
            }
            view.layoutParams = layoutParams
            // required for some android versions
            view.parent.requestLayout()
        }
    }
}