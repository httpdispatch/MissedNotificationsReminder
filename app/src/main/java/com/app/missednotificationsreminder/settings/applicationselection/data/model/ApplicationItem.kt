package com.app.missednotificationsreminder.settings.applicationselection.data.model

import android.net.Uri

/**
 * The class to store application item information used in the [ApplicationsSelectionViewModel]
 * @property checked Whether the application is already checked by user
 * @property applicationName The application name
 * @property packageName The application package name
 * @property iconUri The application icon URI
 * @property activeNotifications The number of active notifications
 */
data class ApplicationItem(
        val checked: Boolean,
        val applicationName: CharSequence,
        val packageName: String,
        val iconUri: Uri,
        val activeNotifications: Int)