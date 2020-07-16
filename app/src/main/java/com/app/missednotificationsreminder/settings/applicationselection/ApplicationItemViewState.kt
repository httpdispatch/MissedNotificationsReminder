package com.app.missednotificationsreminder.settings.applicationselection

import android.net.Uri
import timber.log.Timber

/**
 * The class to store application item information used in the [ApplicationsSelectionViewModel]
 * @property checked Whether the application is already checked by user
 * @property applicationName The application name
 * @property packageName The application package name
 * @property iconUri The application icon URI
 * @property activeNotifications The number of active notifications
 */
data class ApplicationItemViewState(
        val checked: Boolean,
        val applicationName: CharSequence,
        val packageName: String,
        val iconUri: Uri,
        val activeNotifications: Int) {
    /**
     * Get the application name
     *
     * @return
     */
    val name: CharSequence
        get() {
            Timber.d("getName for %1\$s", toString())
            return applicationName
        }

    /**
     * Get the application description
     *
     * @return
     */
    val description: String
        get() {
            Timber.d("getDescription for %1\$s", toString())
            return packageName
        }

    val hasActiveNotifications: Boolean
        get() {
            return activeNotifications > 0
        }

    val activeNotificationsInfo: String
        get() {
            return activeNotifications.toString()
        }
}