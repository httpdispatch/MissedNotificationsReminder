package com.app.missednotificationsreminder.util.resources

import android.content.Context
import android.content.res.TypedArray
import android.util.TypedValue

fun Context.resolveBooleanAttribute(attr: Int, defaultValue: Boolean): Boolean {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    val ta: TypedArray = obtainStyledAttributes(typedValue.resourceId, intArrayOf(attr))
    val result = ta.getBoolean(0, defaultValue)
    ta.recycle()
    return result
}