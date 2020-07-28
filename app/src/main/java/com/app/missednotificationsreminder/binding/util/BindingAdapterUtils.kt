package com.app.missednotificationsreminder.binding.util

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.app.missednotificationsreminder.R
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