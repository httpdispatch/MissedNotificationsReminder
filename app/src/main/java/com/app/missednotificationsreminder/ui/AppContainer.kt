package com.app.missednotificationsreminder.ui

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.app.missednotificationsreminder.R

/**
 * An indirection which allows controlling the root container used for each activity.
 */
interface AppContainer {
    /**
     * The root [ViewGroup] into which the activity should place its contents.
     */
    fun bind(activity: AppCompatActivity): ViewGroup

    companion object {
        /**
         * An [AppContainer] which returns the normal activity content view.
         */
        val DEFAULT: AppContainer = object : AppContainer {
            override fun bind(activity: AppCompatActivity): ViewGroup {
                return activity.findViewById(android.R.id.content)
            }
        }
    }
}